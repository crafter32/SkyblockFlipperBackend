package com.skyblockflipper.backend.instrumentation;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
@Slf4j
public class JfrRecordingManager {

    private Recording continuousRecording;
    private Recording snapshotRingRecording;
    private final InstrumentationProperties properties;

    /**
     * Creates a JfrRecordingManager configured with the provided instrumentation properties.
     *
     * @param properties configuration used to control JFR behavior (output directory, retention window,
     *                   snapshot window, max size and enablement)
     */
    public JfrRecordingManager(InstrumentationProperties properties) {
        this.properties = properties;
    }

    /**
     * Initializes and starts the JFR continuous and snapshot-ring recordings using the configured
     * InstrumentationProperties.
     *
     * <p>Sets the JFR stack depth system property, ensures the configured output directory exists,
     * creates and starts a long-running continuous recording and a snapshot-ring recording with
     * configured retention windows and max sizes, and enables the blocking-related JFR events
     * used by the manager.</p>
     *
     * <p>On success this method leaves {@code continuousRecording} and {@code snapshotRingRecording}
     * running; on failure it logs a warning and leaves recordings unset or unchanged.</p>
     */
    @PostConstruct
    public void start() {
        if (!properties.getJfr().isEnabled()) {
            return;
        }
        try {
            System.setProperty("jdk.jfr.stackdepth", Integer.toString(properties.getJfr().getStackDepth()));
            Path outputDir = ensureOutputDir();
            Configuration profile = Configuration.getConfiguration("profile");

            continuousRecording = new Recording(profile);
            continuousRecording.setName("skyblock-blocking-continuous");
            continuousRecording.setToDisk(true);
            continuousRecording.setDumpOnExit(true);
            continuousRecording.setDestination(outputDir.resolve("continuous.jfr"));
            continuousRecording.setMaxAge(properties.getJfr().getRetention());
            continuousRecording.setMaxSize(properties.getJfr().getMaxSizeMb() * 1024L * 1024L);
            configureBlockingEvents(continuousRecording);
            continuousRecording.start();

            snapshotRingRecording = new Recording(profile);
            snapshotRingRecording.setName("skyblock-blocking-snapshot-ring");
            snapshotRingRecording.setToDisk(true);
            snapshotRingRecording.setDumpOnExit(false);
            snapshotRingRecording.setMaxAge(properties.getJfr().getSnapshotWindow());
            snapshotRingRecording.setMaxSize(Math.max(32L, properties.getJfr().getMaxSizeMb() / 4) * 1024L * 1024L);
            configureBlockingEvents(snapshotRingRecording);
            snapshotRingRecording.start();

            log.info("JFR started. dir={} retention={} snapshotWindow={}",
                    outputDir,
                    properties.getJfr().getRetention(),
                    properties.getJfr().getSnapshotWindow());
        } catch (Exception exception) {
            log.warn("Failed to start JFR instrumentation: {}", exception.getMessage(), exception);
        }
    }

    /**
     * Dump the current snapshot-ring recording to a timestamped .jfr file and return its path.
     *
     * @return the path to the written .jfr snapshot file
     * @throws IllegalStateException if the snapshot-ring recording is not running or if an I/O error prevents writing the file
     */
    public synchronized Path dumpSnapshot() {
        if (snapshotRingRecording == null) {
            throw new IllegalStateException("JFR snapshot recording is not running");
        }
        try {
            System.setProperty("jdk.jfr.stackdepth", Integer.toString(properties.getJfr().getStackDepth()));
            Path outputDir = ensureOutputDir();
            Recording copy = snapshotRingRecording.copy(true);
            Path output = outputDir.resolve("snapshot-" + Instant.now().toEpochMilli() + ".jfr");
            copy.dump(output);
            copy.close();
            return output;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to dump JFR snapshot", exception);
        }
    }

    /**
     * Locate the most recently modified `.jfr` file in the configured JFR output directory.
     *
     * @return the path to the most recently modified `.jfr` file, or `null` if no `.jfr` files are present or an I/O error occurs
     */
    public synchronized Path latestRecordingFile() {
        try {
            System.setProperty("jdk.jfr.stackdepth", Integer.toString(properties.getJfr().getStackDepth()));
            Path outputDir = ensureOutputDir();
            List<Path> files = Files.list(outputDir)
                    .filter(path -> path.getFileName().toString().endsWith(".jfr"))
                    .sorted(Comparator.comparingLong((Path path) -> path.toFile().lastModified()).reversed())
                    .toList();
            return files.isEmpty() ? null : files.getFirst();
        } catch (IOException exception) {
            return null;
        }
    }

    /**
     * Deletes `.jfr` files in the configured JFR output directory that are older than the configured retention period.
     *
     * The method does nothing if JFR is disabled in the configuration. IO errors encountered while listing or
     * deleting files are ignored. This method is intended to be invoked periodically (scheduled).
     */
    @Scheduled(fixedDelayString = "PT10M")
    public void cleanupOldRecordings() {
        if (!properties.getJfr().isEnabled()) {
            return;
        }
        try {
            System.setProperty("jdk.jfr.stackdepth", Integer.toString(properties.getJfr().getStackDepth()));
            Path outputDir = ensureOutputDir();
            Instant threshold = Instant.now().minus(properties.getJfr().getRetention());
            Files.list(outputDir)
                    .filter(path -> path.getFileName().toString().endsWith(".jfr"))
                    .filter(path -> path.toFile().lastModified() < threshold.toEpochMilli())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    /**
     * Enables a predefined set of JFR events on the given recording for capturing blocking, I/O,
     * garbage collection, CPU load, and execution-sampling diagnostics.
     *
     * Most enabled events will include stack traces; `jdk.CPULoad` is enabled without a stack trace.
     *
     * @param recording the JFR Recording to configure
     */
    private void configureBlockingEvents(Recording recording) {
        recording.enable("jdk.JavaMonitorBlocked").withStackTrace();
        recording.enable("jdk.ThreadPark").withStackTrace();
        recording.enable("jdk.JavaMonitorWait").withStackTrace();
        recording.enable("jdk.SocketRead").withStackTrace();
        recording.enable("jdk.SocketWrite").withStackTrace();
        recording.enable("jdk.FileRead").withStackTrace();
        recording.enable("jdk.FileWrite").withStackTrace();
        recording.enable("jdk.GarbageCollection").withStackTrace();
        recording.enable("jdk.CPULoad");
        recording.enable("jdk.ExecutionSample").withStackTrace();
    }

    /**
     * Ensures the configured JFR output directory exists and returns its path.
     *
     * @return the path to the JFR output directory
     * @throws IOException if the directory cannot be created
     */
    private Path ensureOutputDir() throws IOException {
        Path outputDir = properties.getJfr().getOutputDir();
        Files.createDirectories(outputDir);
        return outputDir;
    }

    /**
     * Stops and closes any active JFR recordings managed by this component.
     *
     * If a continuous or snapshot-ring recording exists, it will be stopped and closed to release resources.
     */
    @PreDestroy
    public void stop() {
        if (continuousRecording != null) {
            continuousRecording.stop();
            continuousRecording.close();
        }
        if (snapshotRingRecording != null) {
            snapshotRingRecording.stop();
            snapshotRingRecording.close();
        }
    }
}