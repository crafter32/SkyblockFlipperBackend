package com.skyblockflipper.backend.service.market;

import com.skyblockflipper.backend.hypixel.HypixelClient;
import com.skyblockflipper.backend.hypixel.HypixelMarketSnapshotMapper;
import com.skyblockflipper.backend.hypixel.model.AuctionResponse;
import com.skyblockflipper.backend.hypixel.model.BazaarResponse;
import com.skyblockflipper.backend.instrumentation.CycleInstrumentationService;
import com.skyblockflipper.backend.model.market.MarketSnapshot;
import com.skyblockflipper.backend.service.flipping.UnifiedFlipInputMapper;
import com.skyblockflipper.backend.model.market.UnifiedFlipInputSnapshot;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class MarketDataProcessingService {

    private final HypixelClient hypixelClient;
    private final HypixelMarketSnapshotMapper marketSnapshotMapper;
    private final MarketSnapshotPersistenceService marketSnapshotPersistenceService;
    private final UnifiedFlipInputMapper unifiedFlipInputMapper;
    private final CycleInstrumentationService cycleInstrumentationService;

    /**
     * Constructs a MarketDataProcessingService with the provided clients and mappers and a default
     * CycleInstrumentationService configured with a SimpleMeterRegistry and InstrumentationProperties.
     *
     * @param hypixelClient client used to fetch auction and bazaar data
     * @param marketSnapshotMapper maps external API responses into MarketSnapshot instances
     * @param marketSnapshotPersistenceService persists and retrieves MarketSnapshot data
     * @param unifiedFlipInputMapper converts MarketSnapshot into UnifiedFlipInputSnapshot
     */
    public MarketDataProcessingService(HypixelClient hypixelClient,
                                       HypixelMarketSnapshotMapper marketSnapshotMapper,
                                       MarketSnapshotPersistenceService marketSnapshotPersistenceService,
                                       UnifiedFlipInputMapper unifiedFlipInputMapper) {
        this(hypixelClient,
                marketSnapshotMapper,
                marketSnapshotPersistenceService,
                unifiedFlipInputMapper,
                new CycleInstrumentationService(
                        new SimpleMeterRegistry(),
                        new com.skyblockflipper.backend.instrumentation.BlockingTimeTracker(
                                new com.skyblockflipper.backend.instrumentation.InstrumentationProperties())));
    }

    /**
     * Constructs a MarketDataProcessingService with the provided collaborators.
     *
     * @param hypixelClient client used to fetch auction and bazaar data
     * @param marketSnapshotMapper mapper that converts external API responses into MarketSnapshot instances
     * @param marketSnapshotPersistenceService persistence service for saving and retrieving MarketSnapshot objects
     * @param unifiedFlipInputMapper mapper that transforms a MarketSnapshot into a UnifiedFlipInputSnapshot
     * @param cycleInstrumentationService instrumentation service used to record and report cycle phases and payload metrics
     */
    @Autowired
    public MarketDataProcessingService(HypixelClient hypixelClient,
                                       HypixelMarketSnapshotMapper marketSnapshotMapper,
                                       MarketSnapshotPersistenceService marketSnapshotPersistenceService,
                                       UnifiedFlipInputMapper unifiedFlipInputMapper,
                                       CycleInstrumentationService cycleInstrumentationService) {
        this.hypixelClient = hypixelClient;
        this.marketSnapshotMapper = marketSnapshotMapper;
        this.marketSnapshotPersistenceService = marketSnapshotPersistenceService;
        this.unifiedFlipInputMapper = unifiedFlipInputMapper;
        this.cycleInstrumentationService = cycleInstrumentationService;
    }

    /**
     * Capture the current market snapshot and prepare a UnifiedFlipInputSnapshot using the default cycle identifier "manual".
     *
     * @return `Optional` containing the prepared UnifiedFlipInputSnapshot when market data was captured successfully, `Optional.empty()` when no data was available.
     */
    public Optional<UnifiedFlipInputSnapshot> captureCurrentSnapshotAndPrepareInput() {
        return captureCurrentSnapshotAndPrepareInput("manual");
    }

    /**
     * Captures current auction and bazaar data, maps and persists a MarketSnapshot, and produces a UnifiedFlipInputSnapshot.
     *
     * Fetches auction and bazaar payloads, converts them into a MarketSnapshot which is saved, and then maps that snapshot
     * into a UnifiedFlipInputSnapshot. If neither payload is available the method returns an empty Optional. If only one
     * payload is available a partial-data informational log is emitted that includes the provided cycleId.
     *
     * @param cycleId an identifier for this capture run; included in logs and instrumentation to correlate phases
     * @return an Optional containing the prepared UnifiedFlipInputSnapshot when at least one payload was available, `Optional.empty()` otherwise
     */
    public Optional<UnifiedFlipInputSnapshot> captureCurrentSnapshotAndPrepareInput(String cycleId) {
        long pullHttpStart = cycleInstrumentationService.startPhase();
        AuctionResponse auctionResponse = hypixelClient.fetchAllAuctionPages();
        BazaarResponse bazaarResponse = hypixelClient.fetchBazaar();
        long payloadBytes = estimatePayload(auctionResponse, bazaarResponse);
        cycleInstrumentationService.endPhase("pull_http", pullHttpStart, true, payloadBytes);

        long deserializeStart = cycleInstrumentationService.startPhase();
        boolean hasAnyPayload = auctionResponse != null || bazaarResponse != null;
        cycleInstrumentationService.endPhase("deserialize", deserializeStart, hasAnyPayload, payloadBytes);

        if (!hasAnyPayload) {
            log.warn("Both auction and bazaar responses are null, returning empty");
            return Optional.empty();
        }
        if (auctionResponse == null || bazaarResponse == null) {
            log.info("Partial data available: auctions={}, bazaar={} cycleId={}", auctionResponse != null, bazaarResponse != null, cycleId);
        }

        long normalizeStart = cycleInstrumentationService.startPhase();
        MarketSnapshot snapshot = marketSnapshotMapper.map(auctionResponse, bazaarResponse);
        cycleInstrumentationService.endPhase("normalize", normalizeStart, true, payloadBytes);

        long computeStart = cycleInstrumentationService.startPhase();
        UnifiedFlipInputSnapshot inputSnapshot = unifiedFlipInputMapper.map(snapshot);
        cycleInstrumentationService.endPhase("compute_flips", computeStart, true, payloadBytes);

        long persistStart = cycleInstrumentationService.startPhase();
        marketSnapshotPersistenceService.save(snapshot);
        cycleInstrumentationService.endPhase("persist/cache_update", persistStart, true, payloadBytes);

        return Optional.of(inputSnapshot);
    }

    /**
     * Retrieve the most recently persisted market snapshot.
     *
     * @return the latest MarketSnapshot wrapped in an Optional, or Optional.empty() if none is available
     */
    public Optional<MarketSnapshot> latestMarketSnapshot() {
        return marketSnapshotPersistenceService.latest();
    }

    public Optional<MarketSnapshot> marketSnapshotAsOfSecondsAgo(long secondsAgo) {
        long boundedSecondsAgo = Math.max(0L, secondsAgo);
        return marketSnapshotPersistenceService.asOf(java.time.Instant.now().minusSeconds(boundedSecondsAgo));
    }

    /**
     * Compacts persisted market snapshots to reclaim storage and remove redundant or aged entries.
     *
     * @return the compaction result containing statistics about snapshots removed and snapshots retained
     */
    public MarketSnapshotPersistenceService.SnapshotCompactionResult compactSnapshots() {
        return marketSnapshotPersistenceService.compactSnapshots();
    }

    /**
     * Estimate the rough payload size in bytes for the provided auction and bazaar responses.
     *
     * This method treats null responses or null lists as empty. It applies heuristics of
     * 300 bytes per auction and 200 bytes per bazaar product to compute the estimate.
     *
     * @param auctionResponse the auction API response, may be null
     * @param bazaarResponse  the bazaar API response, may be null
     * @return the estimated payload size in bytes based on per-item heuristics
     */
    private long estimatePayload(AuctionResponse auctionResponse, BazaarResponse bazaarResponse) {
        long auctionCount = auctionResponse == null || auctionResponse.getAuctions() == null ? 0L : auctionResponse.getAuctions().size();
        long bazaarCount = bazaarResponse == null || bazaarResponse.getProducts() == null ? 0L : bazaarResponse.getProducts().size();
        return (auctionCount * 300L) + (bazaarCount * 200L);
    }
}