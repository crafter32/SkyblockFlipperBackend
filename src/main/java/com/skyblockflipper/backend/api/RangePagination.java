package com.skyblockflipper.backend.api;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class RangePagination {

    private RangePagination() {
    }

    public static Pageable pageable(Integer min, Integer max, int defaultSize, Sort sort) {
        int safeDefaultSize = Math.max(1, defaultSize);
        int safeMin = min == null ? 0 : Math.max(0, min);
        int safeMax = max == null ? safeMin + safeDefaultSize - 1 : Math.max(safeMin, max);
        int size = safeMax - safeMin + 1;
        return OffsetLimitPageRequest.of(safeMin, size, sort);
    }
}
