package com.skyblockflipper.backend.api;

public record FlipGoodnessDto(
        UnifiedFlipDto flip,
        double goodnessScore,
        GoodnessBreakdown breakdown
) {
    public record GoodnessBreakdown(
            double roiPerHourScore,
            double profitScore,
            double liquidityScore,
            double inverseRiskScore,
            boolean partialPenaltyApplied
    ) {
    }
}
