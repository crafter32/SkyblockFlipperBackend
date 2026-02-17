package com.skyblockflipper.backend.service.market;

import com.skyblockflipper.backend.model.market.BazaarMarketRecord;
import com.skyblockflipper.backend.model.market.MarketSnapshot;
import com.skyblockflipper.backend.service.flipping.FlipScoreFeatureSet;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketTimescaleFeatureServiceTest {

    @Test
    void dailyFeaturesUseFirstSnapshotInEachEpochDayBucket() {
        MarketSnapshotPersistenceService persistenceService = mock(MarketSnapshotPersistenceService.class);
        MarketTimescaleFeatureService featureService = new MarketTimescaleFeatureService(persistenceService);

        MarketSnapshot latest = snapshot("2026-02-18T12:00:00Z", 300D);
        List<MarketSnapshot> microWindow = List.of(
                snapshot("2026-02-18T11:59:00Z", 295D),
                latest
        );
        List<MarketSnapshot> dailyHistory = List.of(
                snapshot("2026-02-17T00:00:05Z", 100D),
                snapshot("2026-02-17T23:59:59Z", 200D),
                snapshot("2026-02-18T00:00:10Z", 120D),
                latest
        );
        when(persistenceService.between(any(Instant.class), any(Instant.class)))
                .thenReturn(microWindow)
                .thenReturn(dailyHistory);

        FlipScoreFeatureSet featureSet = featureService.computeFor(latest);
        FlipScoreFeatureSet.ItemTimescaleFeatures itemFeatures = featureSet.get("ENCHANTED_DIAMOND");

        assertNotNull(itemFeatures);
        assertEquals(Math.log(120D / 100D), itemFeatures.macroReturn1d(), 1e-9);
    }

    private MarketSnapshot snapshot(String timestamp, double midPrice) {
        double buyPrice = midPrice + 2D;
        double sellPrice = midPrice - 2D;
        BazaarMarketRecord record = new BazaarMarketRecord(
                "ENCHANTED_DIAMOND",
                buyPrice,
                sellPrice,
                10_000L,
                10_000L,
                840_000L,
                840_000L,
                80,
                80
        );
        return new MarketSnapshot(
                Instant.parse(timestamp),
                List.of(),
                Map.of("ENCHANTED_DIAMOND", record)
        );
    }
}
