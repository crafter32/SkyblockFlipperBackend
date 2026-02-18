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

    public JfrRecordingManager(InstrumentationProperties properties) {
        this.properties = properties;
    }

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

    private Path ensureOutputDir() throws IOException {
        Path outputDir = properties.getJfr().getOutputDir();
        Files.createDirectories(outputDir);
        return outputDir;
    }

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
