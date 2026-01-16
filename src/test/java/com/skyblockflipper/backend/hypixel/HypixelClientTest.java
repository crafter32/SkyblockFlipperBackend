package com.skyblockflipper.backend.hypixel;

import com.skyblockflipper.backend.hypixel.model.Auction;
import com.skyblockflipper.backend.hypixel.model.AuctionResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HypixelClientTest {

    @Test
    void fetchAuctionsHandlesNullResponse() {
        RestClient restClient = mock(RestClient.class, Answers.RETURNS_DEEP_STUBS);
        when(restClient.get().uri(anyString()).retrieve().body(any(ParameterizedTypeReference.class)))
                .thenReturn(null);

        HypixelClient client = new HypixelClient("http://localhost");
        ReflectionTestUtils.setField(client, "restClient", restClient);

        client.fetchAuctions();

        verify(restClient, atLeastOnce()).get();
    }

    @Test
    void fetchAuctionsHandlesSuccess() {
        RestClient restClient = mock(RestClient.class, Answers.RETURNS_DEEP_STUBS);
        Auction auction = new Auction(
                "uuid",
                "auctioneer",
                "profile",
                List.of(),
                1L,
                2L,
                "item",
                "lore",
                "extra",
                "category",
                "tier",
                100L,
                false,
                List.of(),
                150L,
                List.of()
        );
        AuctionResponse response = new AuctionResponse(true, 0, 1, 1, 3L, List.of(auction));
        when(restClient.get().uri(anyString()).retrieve().body(any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        HypixelClient client = new HypixelClient("http://localhost");
        ReflectionTestUtils.setField(client, "restClient", restClient);

        client.fetchAuctions();

        verify(restClient, atLeastOnce()).get();
    }
}
