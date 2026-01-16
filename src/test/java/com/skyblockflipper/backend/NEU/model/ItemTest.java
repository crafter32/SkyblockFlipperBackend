package com.skyblockflipper.backend.NEU.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ItemTest {

    @Test
    void builderDefaultsHaveMaps() {
        Item item = Item.builder().id("id").name("name").build();

        assertNotNull(item.getStats());
        assertNotNull(item.getMetadata());
    }
}
