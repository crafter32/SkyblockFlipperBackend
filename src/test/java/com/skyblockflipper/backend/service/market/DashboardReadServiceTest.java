package com.skyblockflipper.backend.service.market;

import com.skyblockflipper.backend.NEU.model.Item;
import com.skyblockflipper.backend.NEU.repository.ItemRepository;
import com.skyblockflipper.backend.api.DashboardOverviewDto;
import com.skyblockflipper.backend.api.MarketplaceType;
import com.skyblockflipper.backend.api.TrendingItemDto;
import com.skyblockflipper.backend.api.UnifiedFlipDto;
import com.skyblockflipper.backend.model.Flipping.Enums.FlipType;
import com.skyblockflipper.backend.model.Flipping.Flip;
import com.skyblockflipper.backend.model.market.BazaarMarketRecord;
import com.skyblockflipper.backend.model.market.MarketSnapshot;
import com.skyblockflipper.backend.repository.FlipRepository;
import com.skyblockflipper.backend.service.flipping.FlipCalculationContext;
import com.skyblockflipper.backend.service.flipping.FlipCalculationContextService;
import com.skyblockflipper.backend.service.flipping.FlipScoreFeatureSet;
import com.skyblockflipper.backend.service.flipping.UnifiedFlipDtoMapper;
import com.skyblockflipper.backend.service.item.ItemMarketplaceService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardReadServiceTest {

    @Test
    void overviewReturnsUnknownWhenNoSnapshotExists() {
        MarketSnapshotPersistenceService snapshotService = mock(MarketSnapshotPersistenceService.class);
        ItemRepository itemRepository = mock(ItemRepository.class);
        FlipRepository flipRepository = mock(FlipRepository.class);
        UnifiedFlipDtoMapper mapper = mock(UnifiedFlipDtoMapper.class);
        FlipCalculationContextService contextService = mock(FlipCalculationContextService.class);
        ItemMarketplaceService marketplaceService = mock(ItemMarketplaceService.class);
        DashboardReadService service = new DashboardReadService(
                snapshotService, itemRepository, flipRepository, mapper, contextService, marketplaceService
        );

        when(itemRepository.count()).thenReturn(1247L);
        when(snapshotService.latest()).thenReturn(Optional.empty());

        DashboardOverviewDto dto = service.overview();

        assertEquals(1247L, dto.totalItems());
        assertEquals("UNKNOWN", dto.marketTrend());
        assertNull(dto.topFlip());
    }

    @Test
    void overviewReturnsTopFlipAndBullishTrendFromLatestSnapshot() {
        MarketSnapshotPersistenceService snapshotService = mock(MarketSnapshotPersistenceService.class);
        ItemRepository itemRepository = mock(ItemRepository.class);
        FlipRepository flipRepository = mock(FlipRepository.class);
        UnifiedFlipDtoMapper mapper = mock(UnifiedFlipDtoMapper.class);
        FlipCalculationContextService contextService = mock(FlipCalculationContextService.class);
        ItemMarketplaceService marketplaceService = mock(ItemMarketplaceService.class);
        DashboardReadService service = new DashboardReadService(
                snapshotService, itemRepository, flipRepository, mapper, contextService, marketplaceService
        );

        Instant ts = Instant.parse("2026-02-21T12:00:00Z");
        MarketSnapshot snapshot = new MarketSnapshot(
                ts,
                List.of(
                        new com.skyblockflipper.backend.model.market.AuctionMarketRecord("a", "H", "W", "L", 1L, 0L, 0L, 0L, false),
                        new com.skyblockflipper.backend.model.market.AuctionMarketRecord("b", "T", "W", "L", 1L, 0L, 0L, 0L, false)
                ),
                Map.of(
                        "A", new BazaarMarketRecord("A", 100, 90, 10, 10, 0, 0, 1, 1),
                        "B", new BazaarMarketRecord("B", 200, 180, 10, 10, 0, 0, 1, 1)
                )
        );
        when(itemRepository.count()).thenReturn(10L);
        when(snapshotService.latest()).thenReturn(Optional.of(snapshot));
        when(flipRepository.findMaxSnapshotTimestampEpochMillis()).thenReturn(Optional.of(ts.toEpochMilli()));

        Flip f1 = new Flip(UUID.randomUUID(), FlipType.AUCTION, List.of(), "A", List.of());
        Flip f2 = new Flip(UUID.randomUUID(), FlipType.BAZAAR, List.of(), "B", List.of());
        when(flipRepository.findAllBySnapshotTimestampEpochMillis(ts.toEpochMilli())).thenReturn(List.of(f1, f2));

        FlipCalculationContext context = new FlipCalculationContext(null, 0.0125D, 1D, false, FlipScoreFeatureSet.empty());
        when(contextService.loadContextAsOf(ts)).thenReturn(context);
        when(mapper.toDto(f1, context)).thenReturn(flipDto(f1.getId(), "A_OUTPUT", 1_000L));
        when(mapper.toDto(f2, context)).thenReturn(flipDto(f2.getId(), "B_OUTPUT", 5_000L));

        DashboardOverviewDto dto = service.overview();

        assertEquals(10L, dto.totalItems());
        assertEquals(2L, dto.totalActiveFlips());
        assertEquals(2L, dto.totalAHListings());
        assertEquals(2L, dto.bazaarProducts());
        assertEquals("BULLISH", dto.marketTrend());
        assertEquals(f2.getId().toString(), dto.topFlip().id());
        assertEquals("B_OUTPUT", dto.topFlip().outputName());
        assertEquals(5_000L, dto.topFlip().expectedProfit());
    }

    @Test
    void trendingComputesChangesSortsAndRespectsLimit() {
        MarketSnapshotPersistenceService snapshotService = mock(MarketSnapshotPersistenceService.class);
        ItemRepository itemRepository = mock(ItemRepository.class);
        FlipRepository flipRepository = mock(FlipRepository.class);
        UnifiedFlipDtoMapper mapper = mock(UnifiedFlipDtoMapper.class);
        FlipCalculationContextService contextService = mock(FlipCalculationContextService.class);
        ItemMarketplaceService marketplaceService = mock(ItemMarketplaceService.class);
        DashboardReadService service = new DashboardReadService(
                snapshotService, itemRepository, flipRepository, mapper, contextService, marketplaceService
        );

        Instant end = Instant.parse("2026-02-21T12:00:00Z");
        MarketSnapshot first = new MarketSnapshot(
                end.minusSeconds(24L * 60L * 60L),
                List.of(),
                Map.of(
                        "A", new BazaarMarketRecord("A", 100, 99, 100, 100, 0, 0, 1, 1),
                        "B", new BazaarMarketRecord("B", 100, 99, 200, 200, 0, 0, 1, 1)
                )
        );
        MarketSnapshot latest = new MarketSnapshot(
                end,
                List.of(),
                Map.of(
                        "A", new BazaarMarketRecord("A", 150, 140, 300, 300, 0, 0, 1, 1),
                        "B", new BazaarMarketRecord("B", 110, 105, 210, 210, 0, 0, 1, 1)
                )
        );

        when(snapshotService.latest()).thenReturn(Optional.of(latest));
        when(snapshotService.between(end.minusSeconds(24L * 60L * 60L), end)).thenReturn(List.of(first, latest));
        when(itemRepository.findAll()).thenReturn(List.of(
                Item.builder().id("A").displayName("Alpha").build(),
                Item.builder().id("B").displayName("Bravo").build()
        ));
        when(marketplaceService.resolveMarketplaces(any())).thenReturn(Map.of(
                "A", MarketplaceType.BAZAAR,
                "B", MarketplaceType.BOTH
        ));

        List<TrendingItemDto> trending = service.trending(1);

        assertEquals(1, trending.size());
        assertEquals("A", trending.getFirst().itemId());
        assertEquals("Alpha", trending.getFirst().displayName());
        assertEquals(50.0, trending.getFirst().priceChange24h());
        assertTrue(trending.getFirst().volumeChange24h() > 0);
        assertEquals(MarketplaceType.BAZAAR, trending.getFirst().marketplace());
    }

    private UnifiedFlipDto flipDto(UUID id, String outputItem, long expectedProfit) {
        return new UnifiedFlipDto(
                id,
                FlipType.AUCTION,
                List.of(),
                List.of(new UnifiedFlipDto.ItemStackDto(outputItem, 1)),
                null,
                expectedProfit,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                false,
                List.of(),
                List.of(),
                List.of()
        );
    }
}
