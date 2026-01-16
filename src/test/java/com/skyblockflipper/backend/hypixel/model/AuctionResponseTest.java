package com.skyblockflipper.backend.hypixel.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionResponseTest {

    @Test
    void constructorSetsFields() {
        AuctionResponse response = new AuctionResponse(true, 1, 2, 3, 4L, List.of());

        assertTrue(response.isSuccess());
        assertEquals(1, response.getPage());
        assertEquals(2, response.getTotalPages());
        assertEquals(3, response.getTotalAuctions());
        assertEquals(4L, response.getLastUpdated());
        assertEquals(List.of(), response.getAuctions());
    }
}
