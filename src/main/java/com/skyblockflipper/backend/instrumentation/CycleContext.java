package com.skyblockflipper.backend.instrumentation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CycleContext {
    private final String cycleId;
    private final Instant startedAt;
    private final Map<String, Long> phaseMillis = new LinkedHashMap<>();
    private final List<BlockingTimeTracker.BlockingPoint> blockingPoints = new ArrayList<>();
    private long payloadBytes;

    /**
     * Create a CycleContext for a single instrumentation cycle.
     *
     * Initializes the context with the provided cycle identifier and start timestamp;
     * the phase and blocking-point collections are left empty and payload size defaults to zero.
     *
     * @param cycleId   unique identifier for the cycle
     * @param startedAt timestamp when the cycle began
     */
    public CycleContext(String cycleId, Instant startedAt) {
        this.cycleId = cycleId;
        this.startedAt = startedAt;
    }

    /**
 * Retrieves the identifier for this cycle.
 *
 * @return the cycle identifier
 */
public String getCycleId() { return cycleId; }
    /**
 * Timestamp indicating when the cycle began.
 *
 * @return the Instant representing when the cycle started
 */
public Instant getStartedAt() { return startedAt; }
    /**
 * Map of phase names to their durations in milliseconds, with insertion order preserved.
 *
 * The returned map is the live, modifiable internal map; modifications to it will be reflected in this CycleContext.
 *
 * @return a map from phase name to duration in milliseconds with insertion order preserved
 */
public Map<String, Long> getPhaseMillis() { return phaseMillis; }
    /**
 * The recorded blocking points observed during this cycle.
 *
 * @return the list of recorded blocking points for the cycle, in insertion order
 */
public List<BlockingTimeTracker.BlockingPoint> getBlockingPoints() { return blockingPoints; }
    /**
 * The payload size for this cycle, in bytes.
 *
 * @return the payload size in bytes.
 */
public long getPayloadBytes() { return payloadBytes; }
    /**
 * Set the payload size, in bytes, associated with this cycle.
 *
 * @param payloadBytes the payload size in bytes
 */
public void setPayloadBytes(long payloadBytes) { this.payloadBytes = payloadBytes; }
}