package com.skyblockflipper.backend.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DataSourceHashTest {

    @Test
    void equalityUsesIdOnly() {
        UUID id = UUID.randomUUID();
        DataSourceHash first = new DataSourceHash(id, "key", "hash1", Instant.EPOCH);
        DataSourceHash second = new DataSourceHash(id, "key", "hash2", Instant.EPOCH.plusSeconds(1));
        DataSourceHash different = new DataSourceHash(UUID.randomUUID(), "key", "hash1", Instant.EPOCH);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, different);
    }
}
