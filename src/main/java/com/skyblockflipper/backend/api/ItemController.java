package com.skyblockflipper.backend.api;

import com.skyblockflipper.backend.service.item.NpcShopReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final NpcShopReadService npcShopReadService;

    @GetMapping("/npc-buyable")
    public Page<NpcShopOfferDto> listNpcBuyableItems(
            @RequestParam(required = false) String itemId,
            @PageableDefault(size = 100) Pageable pageable
    ) {
        return npcShopReadService.listNpcBuyableOffers(itemId, pageable);
    }
}
