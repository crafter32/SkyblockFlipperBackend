package com.skyblockflipper.backend.service.market.polling;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdaptivePollerState {
    private PollerMode mode = PollerMode.WARMUP;
    private long warmupStartedAtMillis = -1L;
    private long burstStartedAtMillis = -1L;
    private long lastPollAtMillis = -1L;
    private long lastChangeAtMillis = -1L;
    private long estimatedPeriodMillis;
    private long expectedChangeAtMillis = -1L;
    private long missCount;
    private long updateCount;
    private long consecutiveErrors;

    public AdaptivePollerState() {
    }

    public AdaptivePollerState(AdaptivePollerState other) {
        this.mode = other.mode;
        this.warmupStartedAtMillis = other.warmupStartedAtMillis;
        this.burstStartedAtMillis = other.burstStartedAtMillis;
        this.lastPollAtMillis = other.lastPollAtMillis;
        this.lastChangeAtMillis = other.lastChangeAtMillis;
        this.estimatedPeriodMillis = other.estimatedPeriodMillis;
        this.expectedChangeAtMillis = other.expectedChangeAtMillis;
        this.missCount = other.missCount;
        this.updateCount = other.updateCount;
        this.consecutiveErrors = other.consecutiveErrors;
    }

    public AdaptivePollerState copy() {
        return new AdaptivePollerState(this);
    }
}
