package com.skyblockflipper.backend.hypixel.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionTest {

    @Test
    void constructorSetsFields() {
        Auction.Bid bid = new Auction.Bid("auction-1", "bidder", "profile", 150L, 42L);
        Auction auction = new Auction(
                "uuid",
                "auctioneer",
                "profile",
                List.of("coop"),
                10L,
                20L,
                "item",
                "lore",
                "extra",
                "category",
                "tier",
                100L,
                false,
                List.of("bidder"),
                150L,
                List.of(bid)
        );

        assertEquals("uuid", auction.getUuid());
        assertEquals("auctioneer", auction.getAuctioneer());
        assertEquals("profile", auction.getProfileId());
        assertEquals("item", auction.getItemName());
        assertEquals(100L, auction.getStartingBid());
        assertEquals(List.of(bid), auction.getBids());
        assertEquals("auction-1", bid.getAuctionId());
        assertEquals(150L, bid.getAmount());
    }
}
