package com.skyblockflipper.backend.instrumentation;

import org.springframework.stereotype.Service;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JfrBlockingReportService {

    private static final int STACK_DEPTH = 12;

    /**
     * Produces a summary report of blocking, I/O wait, and CPU metrics from a Java Flight Recorder (JFR) file.
     *
     * The returned map contains either a single `"error"` entry with a message on failure, or the following keys:
     * - `"recordingFile"`: the input path as a string
     * - `"topBlockingStacks"`: a map of stack-key to duration in milliseconds for top blocking stacks
     * - `"topIoWaitStacks"`: a map of stack-key to duration in milliseconds for top I/O wait stacks
     * - `"blockedMillis"`: total time spent blocked in milliseconds
     * - `"cpuMillis"`: total CPU-sampled time in milliseconds
     * - `"blockedToCpuRatio"`: the ratio `blockedNanos / cpuNanos` as a `double`, or the string `"n/a"` if CPU time is zero
     *
     * @param recordingFile the path to the JFR recording file to analyze; may be null
     * @return a map containing the summary report or an `"error"` entry describing a failure
     */
    public Map<String, Object> summarize(Path recordingFile) {
        if (recordingFile == null) {
            return Map.of("error", "No JFR recording file available");
        }
        Map<String, Long> blockingStacks = new LinkedHashMap<>();
        Map<String, Long> ioStacks = new LinkedHashMap<>();
        long blockedNanos = 0L;
        long cpuNanos = 0L;

        try (RecordingFile file = new RecordingFile(recordingFile)) {
            while (file.hasMoreEvents()) {
                RecordedEvent event = file.readEvent();
                String eventName = event.getEventType().getName();
                long durationNanos = durationNanos(event);
                switch (eventName) {
                    case "jdk.ThreadPark", "jdk.JavaMonitorBlocked", "jdk.JavaMonitorWait" -> {
                        blockedNanos += durationNanos;
                        blockingStacks.merge(stackKey(event.getStackTrace()), durationNanos, Long::sum);
                    }
                    case "jdk.SocketRead", "jdk.SocketWrite", "jdk.FileRead", "jdk.FileWrite" ->
                            ioStacks.merge(stackKey(event.getStackTrace()), durationNanos, Long::sum);
                    case "jdk.ExecutionSample" -> cpuNanos += durationNanos;
                    default -> {
                    }
                }
            }
        } catch (IOException exception) {
            return Map.of("error", "Failed to parse JFR: " + exception.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordingFile", recordingFile.toString());
        result.put("topBlockingStacks", topN(blockingStacks, 10));
        result.put("topIoWaitStacks", topN(ioStacks, 10));
        result.put("blockedMillis", blockedNanos / 1_000_000L);
        result.put("cpuMillis", cpuNanos / 1_000_000L);
        result.put("blockedToCpuRatio", cpuNanos == 0L ? "n/a" : (double) blockedNanos / (double) cpuNanos);
        return result;
    }

    /**
     * Get the duration of a RecordedEvent expressed in nanoseconds.
     *
     * @param event the RecordedEvent whose duration to read
     * @return `0` if the event has no duration, otherwise the duration in nanoseconds
     */
    private long durationNanos(RecordedEvent event) {
        Duration duration = event.getDuration();
        return duration == null ? 0L : duration.toNanos();
    }

    /**
     * Constructs a compact string key representing the top frames of a recorded stack trace.
     *
     * If the provided trace is null or contains no frames, returns "<no-stack>". Otherwise,
     * formats up to {@code STACK_DEPTH} frames as {@code TypeName.MethodName} joined with
     * " <- " in call order.
     *
     * @param stackTrace the recorded stack trace to format
     * @return "<no-stack>" if {@code stackTrace} is null or has no frames, otherwise a concatenation
     *         of up to {@code STACK_DEPTH} frames in "TypeName.MethodName" order separated by " <- "
     */
    private String stackKey(RecordedStackTrace stackTrace) {
        if (stackTrace == null || stackTrace.getFrames().isEmpty()) {
            return "<no-stack>";
        }
        return stackTrace.getFrames().stream()
                .limit(STACK_DEPTH)
                .map(frame -> frame.getMethod().getType().getName() + "." + frame.getMethod().getName())
                .reduce((left, right) -> left + " <- " + right)
                .orElse("<unknown>");
    }

    /**
     * Produce a LinkedHashMap of up to `n` entries from `source` with the largest values, sorted in descending order and converted from nanoseconds to milliseconds.
     *
     * @param source a map whose values are durations in nanoseconds
     * @param n the maximum number of entries to include
     * @return a LinkedHashMap preserving order of the top entries with values expressed in milliseconds
     */
    private Map<String, Long> topN(Map<String, Long> source, int n) {
        return source.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(n)
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue() / 1_000_000L), Map::putAll);
    }
}