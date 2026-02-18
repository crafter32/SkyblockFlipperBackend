package com.skyblockflipper.backend.hypixel;

import com.skyblockflipper.backend.hypixel.model.Auction;
import com.skyblockflipper.backend.hypixel.model.AuctionResponse;
import com.skyblockflipper.backend.hypixel.model.BazaarResponse;
import com.skyblockflipper.backend.instrumentation.BlockingTimeTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class HypixelClient {
    // Base URL already contains /v2 (see config.hypixel.api-url), so this stays resource-relative.
    private static final String ELECTION_RESOURCE_PATH = "/resources/skyblock/election";

    private final RestClient restClient;
    private final String apiKey;
    private final BlockingTimeTracker blockingTimeTracker;


    /**
     * Convenience constructor that creates a HypixelClient using a default BlockingTimeTracker.
     *
     * @param apiUrl the base URL for the Hypixel API
     * @param apiKey the API key to include in requests; may be null or blank to perform unauthenticated requests
     */
    public HypixelClient(String apiUrl, String apiKey) {
        this(apiUrl, apiKey, new BlockingTimeTracker(new com.skyblockflipper.backend.instrumentation.InstrumentationProperties()));
    }

    /**
     * Create a HypixelClient configured with the given API base URL, API key, and blocking time tracker.
     *
     * Initializes the internal HTTP client and stores the API key and blocking time tracker for instrumented requests.
     *
     * @param apiUrl              the base URL of the Hypixel API
     * @param apiKey              the API key to send with requests; may be empty if unauthenticated access is desired
     * @param blockingTimeTracker instrumentation utility used to record and control blocking request execution time
     */
    @Autowired
    public HypixelClient(
            @Value("${config.hypixel.api-url}") String apiUrl,
            @Value("${config.hypixel.api-key:}") String apiKey,
            BlockingTimeTracker blockingTimeTracker
    ) {
        this.restClient = RestClient.builder().baseUrl(apiUrl).build();
        this.apiKey = apiKey;
        this.blockingTimeTracker = blockingTimeTracker;
    }

    /**
     * Retrieves the auctions for a specific page.
     *
     * Fetches the auctions at the specified zero-based page index and returns the parsed response when the request succeeds.
     *
     * @param page zero-based page index to fetch (0 is the first page)
     * @return the AuctionResponse for the requested page if the request succeeded and the response indicates success, `null` otherwise
     */
    public AuctionResponse fetchAuctionPage(int page) {
        AuctionResponse result = request(
                "/skyblock/auctions?page=" + page,
                new ParameterizedTypeReference<>() {}
        );
        if (result == null || !result.isSuccess()) {
            return null;
        }
        return result;
    }

    public List<Auction> fetchAllAuctions() {
        return fetchAllAuctionPages().getAuctions();
    }

    public AuctionResponse fetchAllAuctionPages() {
        AuctionResponse firstPage = fetchAuctionPage(0);
        if (firstPage == null) {
            throw new IllegalStateException("Failed to fetch auctions page 0 from Hypixel API.");
        }

        List<Auction> allAuctions = new ArrayList<>();
        if (firstPage.getAuctions() != null) {
            allAuctions.addAll(firstPage.getAuctions());
        }
        for (int page = 1; page < firstPage.getTotalPages(); page++) {
            AuctionResponse nextPage = fetchAuctionPage(page);
            if (nextPage == null) {
                throw new IllegalStateException("Failed to fetch auctions page " + page + " from Hypixel API.");
            }
            if (nextPage.getAuctions() != null) {
                allAuctions.addAll(nextPage.getAuctions());
            }
        }
        return new AuctionResponse(
                true,
                0,
                firstPage.getTotalPages(),
                firstPage.getTotalAuctions(),
                firstPage.getLastUpdated(),
                allAuctions
        );
    }

    public BazaarResponse fetchBazaar() {
        BazaarResponse result = request(
                "/skyblock/bazaar",
                new ParameterizedTypeReference<>() {}
        );
        if (result == null || !result.isSuccess()) {
            return null;
        }
        return result;
    }

    public JsonNode fetchElection() {
        JsonNode result = request(
                ELECTION_RESOURCE_PATH,
                new ParameterizedTypeReference<>() {}
        );
        if (result == null || !result.path("success").asBoolean(false)) {
            return null;
        }
        return result;
    }

    public void fetchAuctions() {
        AuctionResponse result = fetchAuctionPage(0);
        log.info("-----[FETCH AUCTIONS]-----");
        if (result == null) {
            return;
        }
        log.info(new ObjectMapper().writeValueAsString(result.getTotalAuctions()));
        if (result.getAuctions() != null && !result.getAuctions().isEmpty()) {
            log.info(new ObjectMapper().writeValueAsString(result.getAuctions().getFirst().getAuctioneer()));
        }
        log.info(new ObjectMapper().writeValueAsString(result.getLastUpdated()));
    }

    /**
     * Perform a GET request to the given URI and parse the response into the specified type.
     *
     * @param uri the request URI (path relative to the client's base URL)
     * @param responseType a ParameterizedTypeReference describing the expected response type
     * @return the parsed response of type T, or `null` if the request failed (for example due to HTTP errors or other client errors)
     */
    private <T> T request(String uri, ParameterizedTypeReference<T> responseType) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get().uri(uri);
            if (!apiKey.isBlank()) {
                request = request.header("API-Key", apiKey);
            }
            RestClient.RequestHeadersSpec<?> finalRequest = request;
            return blockingTimeTracker.record("http.hypixel" + uri, "http", () -> finalRequest.retrieve().body(responseType));
        } catch (RestClientResponseException e) {
            log.warn("Hypixel request failed for {} with status {}: {}", uri, e.getStatusCode(), e.getStatusText());
            return null;
        } catch (RestClientException e) {
            log.warn("Hypixel request failed for {}: {}", uri, e.getMessage());
            return null;
        }
    }
}