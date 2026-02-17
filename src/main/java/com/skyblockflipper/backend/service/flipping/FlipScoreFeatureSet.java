package com.skyblockflipper.backend.service.flipping;

import java.util.Map;

public record FlipScoreFeatureSet(
        Map<String, ItemTimescaleFeatures> byItemId
) {
    public FlipScoreFeatureSet {
        byItemId = byItemId == null ? Map.of() : Map.copyOf(byItemId);
    }

    public static FlipScoreFeatureSet empty() {
        return new FlipScoreFeatureSet(Map.of());
    }

    public ItemTimescaleFeatures get(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        return byItemId.get(itemId);
    }

    public enum ConfidenceLevel {
        LOW(0D),
        MEDIUM(0.5D),
        HIGH(1D);

        private final double weightFactor;

        ConfidenceLevel(double weightFactor) {
            this.weightFactor = weightFactor;
        }

        public double weightFactor() {
            return weightFactor;
        }
    }

    public record ItemTimescaleFeatures(
            Double microVolatility1m,
            Double microReturn1m,
            ConfidenceLevel microConfidence,
            Double macroVolatility1d,
            Double macroReturn1d,
            ConfidenceLevel macroConfidence,
            boolean structurallyIlliquid
    ) {
        public ItemTimescaleFeatures {
            microConfidence = microConfidence == null ? ConfidenceLevel.LOW : microConfidence;
            macroConfidence = macroConfidence == null ? ConfidenceLevel.LOW : macroConfidence;
        }
    }
}
