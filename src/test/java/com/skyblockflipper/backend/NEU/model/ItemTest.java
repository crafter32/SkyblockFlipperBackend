package com.skyblockflipper.backend.NEU.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ItemTest {

    @Test
    void builderDefaultsHaveCollections() {
        Item item = Item.builder().id("id").displayName("name").build();

        assertNotNull(item.getInfoLinks());
        assertNotNull(item.getRecipes());
    }
}
