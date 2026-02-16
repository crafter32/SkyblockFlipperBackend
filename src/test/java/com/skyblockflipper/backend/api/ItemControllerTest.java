package com.skyblockflipper.backend.api;

import com.skyblockflipper.backend.service.item.NpcShopReadService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemControllerTest {

    @Test
    void listNpcBuyableItemsDelegatesToService() {
        NpcShopReadService service = mock(NpcShopReadService.class);
        ItemController controller = new ItemController(service);
        Pageable pageable = PageRequest.of(0, 100);
        NpcShopOfferDto dto = new NpcShopOfferDto(
                "FARM_MERCHANT_NPC",
                "Farm Merchant",
                "WHEAT",
                3,
                List.of(new NpcShopOfferDto.CostDto("SKYBLOCK_COIN", 7)),
                7L,
                7D / 3D
        );
        Page<NpcShopOfferDto> expected = new PageImpl<>(List.of(dto), pageable, 1);

        when(service.listNpcBuyableOffers("WHEAT", pageable)).thenReturn(expected);

        Page<NpcShopOfferDto> response = controller.listNpcBuyableItems("WHEAT", pageable);

        assertEquals(expected, response);
        verify(service).listNpcBuyableOffers("WHEAT", pageable);
    }
}
