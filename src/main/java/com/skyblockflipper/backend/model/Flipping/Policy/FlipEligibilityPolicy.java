package com.skyblockflipper.backend.model.Flipping.Policy;

import com.skyblockflipper.backend.model.market.UnifiedFlipInputSnapshot;
import org.springframework.stereotype.Component;

@Component
public class FlipEligibilityPolicy {

    private static final double MIN_BAZAAR_EDGE_RATIO = 1.015D;
    private static final double MIN_AUCTION_EDGE_RATIO = 1.05D;
    private static final int MIN_AUCTION_SAMPLE_SIZE = 3;

    public boolean isBazaarFlipEligible(UnifiedFlipInputSnapshot.BazaarQuote quote) {
        if (quote == null) {
            return false;
        }
        if (quote.buyPrice() <= 0D || quote.sellPrice() <= 0D) {
            return false;
        }
        return (quote.sellPrice() / quote.buyPrice()) >= MIN_BAZAAR_EDGE_RATIO;
    }

    public boolean isAuctionFlipEligible(UnifiedFlipInputSnapshot.AuctionQuote quote) {
        if (quote == null) {
            return false;
        }
        if (quote.lowestStartingBid() <= 0L || quote.averageObservedPrice() <= 0D) {
            return false;
        }
        if (quote.sampleSize() < MIN_AUCTION_SAMPLE_SIZE) {
            return false;
        }
        return (quote.averageObservedPrice() / quote.lowestStartingBid()) >= MIN_AUCTION_EDGE_RATIO;
    }
}
