package com.skyblockflipper.backend.model.Flipping.Policy;

import com.skyblockflipper.backend.model.market.UnifiedFlipInputSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlipEligibilityPolicyTest {

    private final FlipEligibilityPolicy policy = new FlipEligibilityPolicy();

    @Test
    void bazaarEligibilityUsesConfiguredEdgeThreshold() {
        assertTrue(policy.isBazaarFlipEligible(new UnifiedFlipInputSnapshot.BazaarQuote(
                100D, 103D, 0L, 0L, 0L, 0L, 0, 0
        )));
        assertFalse(policy.isBazaarFlipEligible(new UnifiedFlipInputSnapshot.BazaarQuote(
                100D, 101D, 0L, 0L, 0L, 0L, 0, 0
        )));
    }

    @Test
    void auctionEligibilityChecksSampleSizeAndEdge() {
        assertTrue(policy.isAuctionFlipEligible(new UnifiedFlipInputSnapshot.AuctionQuote(
                1_000_000L, 1_500_000L, 1_100_000D, 3
        )));
        assertFalse(policy.isAuctionFlipEligible(new UnifiedFlipInputSnapshot.AuctionQuote(
                1_000_000L, 1_500_000L, 1_100_000D, 2
        )));
        assertFalse(policy.isAuctionFlipEligible(new UnifiedFlipInputSnapshot.AuctionQuote(
                1_000_000L, 1_500_000L, 1_020_000D, 5
        )));
    }
}
