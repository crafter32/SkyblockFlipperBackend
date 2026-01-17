package com.skyblockflipper.backend.NEU;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NEUItemFilterHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NEUItemFilterHandler handler = new NEUItemFilterHandler();

    @Test
    void filterKeepsExpectedItemTypes() {
        JsonNode craftRecipe = readJson("{\"id\":\"CRAFT_A\",\"recipe\":{\"A1\":\"ITEM:1\"}}");
        JsonNode craftRecipesType = readJson("{\"id\":\"CRAFT_B\",\"recipes\":[{\"type\":\"crafting\"}]}");
        JsonNode craftSlots = readJson("{\"id\":\"CRAFT_C\",\"recipes\":[{\"slots\":{\"A1\":\"ITEM:1\"}}]}");
        JsonNode forge = readJson("{\"id\":\"FORGE_A\",\"recipes\":[{\"type\":\"forge\"}]}");
        JsonNode petId = readJson("{\"id\":\"BEE;4\"}");
        JsonNode petNbt = readJson("{\"id\":\"PET\",\"nbttag\":\"{petInfo:\\\"{}\\\"}\"}");
        JsonNode shard = readJson("{\"internalname\":\"ATTRIBUTE_SHARD_ALMIGHTY\"}");
        JsonNode ignored = readJson("{\"id\":\"RANDOM\"}");

        List<JsonNode> filtered = handler.filter(List.of(
                craftRecipe, craftRecipesType, craftSlots, forge, petId, petNbt, shard, ignored
        ));

        assertEquals(7, filtered.size());
    }

    @Test
    void filterUsesInternalnameWhenIdMissing() {
        JsonNode shard = readJson("{\"internalname\":\"ATTRIBUTE_SHARD_ALMIGHTY\"}");
        JsonNode ignored = readJson("{\"internalname\":\"RANDOM\"}");

        List<JsonNode> filtered = handler.filter(List.of(shard, ignored));

        assertEquals(1, filtered.size());
    }

    private JsonNode readJson(String json) {
        return objectMapper.readTree(json);
    }
}
