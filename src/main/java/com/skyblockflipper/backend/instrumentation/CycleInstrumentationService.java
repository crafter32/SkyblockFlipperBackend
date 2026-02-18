package com.skyblockflipper.backend.instrumentation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class CycleInstrumentationService {

    private static final String METRIC_NAME = "skyblock.polling.phase";

    private final AtomicLong cycleCounter = new AtomicLong();
    private final MeterRegistry meterRegistry;
    private final BlockingTimeTracker blockingTimeTracker;

    /**
     * Creates a CycleInstrumentationService wired with the provided MeterRegistry and BlockingTimeTracker.
     *
     * @param meterRegistry registry used to create and record Micrometer metrics
     * @param blockingTimeTracker component that collects blocking-time samples for cycle summaries
     */
    public CycleInstrumentationService(MeterRegistry meterRegistry, BlockingTimeTracker blockingTimeTracker) {
        this.meterRegistry = meterRegistry;
        this.blockingTimeTracker = blockingTimeTracker;
    }

    /**
     * Start a new cycle by generating a unique cycleId, creating a CycleContext with the current timestamp,
     * and storing that context in the CycleContextHolder.
     *
     * @return the created CycleContext containing the generated cycleId and start timestamp
     */
    public CycleContext startCycle() {
        long next = cycleCounter.incrementAndGet();
        String cycleId = next + "-" + Instant.now().toEpochMilli();
        CycleContext context = new CycleContext(cycleId, Instant.now());
        CycleContextHolder.set(context);
        return context;
    }

    /**
     * Capture a monotonic timestamp to mark the start of a cycle phase.
     *
     * @return the current value of the JVM high-resolution time source (System.nanoTime()) in nanoseconds
     */
    public long startPhase() {
        return System.nanoTime();
    }

    /**
     * Record the end of a phase by saving its elapsed time and payload size to the current cycle context and emitting a timer metric.
     *
     * If no cycle context is present, this method does nothing.
     *
     * @param phase            the logical name of the phase that completed
     * @param phaseStartNanos  the value previously returned by {@code startPhase()} for this phase (nanoseconds)
     * @param success          whether the phase completed successfully
     * @param payloadBytes     the number of bytes processed during the phase
     */
    public void endPhase(String phase, long phaseStartNanos, boolean success, long payloadBytes) {
        CycleContext context = CycleContextHolder.get();
        if (context == null) {
            return;
        }
        long phaseMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - phaseStartNanos);
        context.getPhaseMillis().put(phase, phaseMillis);
        context.setPayloadBytes(payloadBytes);
        Timer.builder(METRIC_NAME)
                .tag("phase", phase)
                .tag("cycleId", bucketCycle(context.getCycleId()))
                .tag("outcome", success ? "success" : "failure")
                .tag("payload_size_bucket", bucketPayload(payloadBytes))
                .register(meterRegistry)
                .record(phaseMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Finalizes the current cycle's instrumentation, logs a summary of per-phase timings and top blocking points, and clears the cycle context.
     *
     * If no cycle context is present, this method does nothing.
     *
     * @param success whether the cycle completed successfully; recorded in the summary as "success" or "failure"
     */
    public void finishCycle(boolean success) {
        CycleContext context = CycleContextHolder.get();
        if (context == null) {
            return;
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("cycleId", context.getCycleId());
        summary.put("payloadBytes", context.getPayloadBytes());
        summary.put("totalCycleMillis", context.getPhaseMillis().getOrDefault("total_cycle", 0L));
        summary.put("perPhaseMillis", context.getPhaseMillis());
        summary.put("slowBlockingPoints", blockingTimeTracker.topBlockingPoints(context, 5));
        summary.put("outcome", success ? "success" : "failure");
        log.info("cycle_blocking_summary={}", summary);
        CycleContextHolder.clear();
    }

    /**
     * Produce a bucketed cycle range string from a cycle identifier.
     *
     * The method extracts the numeric counter prefix from the provided cycleId (expected in the form
     * "<counter>-<epochMillis>") and rounds it down to the nearest 100 to form a range.
     *
     * @param cycleId the cycle identifier with a numeric counter prefix (e.g. "257-1610000000000"); if the
     *                counter cannot be parsed the method treats it as 0
     * @return a 100-sized bucket range for the cycle counter, formatted as "start-end" (for example "200-299")
     */
    private String bucketCycle(String cycleId) {
        int dash = cycleId.indexOf('-');
        long counter = dash > 0 ? Long.parseLong(cycleId.substring(0, dash)) : 0;
        long bucket = (counter / 100) * 100;
        return bucket + "-" + (bucket + 99);
    }

    /**
     * Maps a payload size in bytes to a human-readable size bucket used for metrics.
     *
     * @param payloadBytes the payload size in bytes
     * @return `"lt_100kb"` for bytes < 100,000, `"100kb_1mb"` for bytes >= 100,000 and < 1,000,000, `"1mb_10mb"` for bytes >= 1,000,000 and < 10,000,000, or `"gte_10mb"` for bytes >= 10,000,000
     */
    private String bucketPayload(long payloadBytes) {
        if (payloadBytes < 100_000) return "lt_100kb";
        if (payloadBytes < 1_000_000) return "100kb_1mb";
        if (payloadBytes < 10_000_000) return "1mb_10mb";
        return "gte_10mb";
    }
}