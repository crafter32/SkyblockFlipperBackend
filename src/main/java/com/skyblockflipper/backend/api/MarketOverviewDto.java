package com.skyblockflipper.backend.api;

import java.time.Instant;

public record MarketOverviewDto(
        String productId,
        Instant snapshotTimestamp,
        Double buy,
        Double buyChangePercent,
        Double sell,
        Double sellChangePercent,
        Double spread,
        Double spreadPercent,
        Double sevenDayHigh,
        Double sevenDayLow,
        Long volume,
        Double averageVolume,
        Long activeFlips,
        Long bestProfit
) {
}

