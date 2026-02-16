package com.skyblockflipper.backend.NEU;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class NEUItemFilterHandler {

    private static final List<String> CRAFT_KEYS = List.of(
            "A1", "A2", "A3",
            "B1", "B2", "B3",
            "C1", "C2", "C3"
    );

    public List<JsonNode> filter(List<JsonNode> items) {
        return items.stream()
                .filter(this::isUsefulItem)
                .toList();
    }

    private boolean isUsefulItem(JsonNode node) {
        return isCraftItem(node) || isForgeItem(node) || isPetItem(node) || isShardItem(node);
    }

    private boolean isCraftItem(JsonNode node) {
        if (hasCraftingGrid(node.path("recipe"))) {
            return true;
        }
        JsonNode recipes = node.path("recipes");
        if (recipes.isArray()) {
            for (JsonNode recipe : recipes) {
                if (isCraftOrKatgradeRecipeType(getString(recipe.path("type")))) {
                    return true;
                }
                if (hasCraftingGrid(recipe.path("slots"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCraftOrKatgradeRecipeType(String type) {
        String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT);
        return "crafting".equals(normalized) || "craft".equals(normalized) || "katgrade".equals(normalized);
    }

    private boolean hasCraftingGrid(JsonNode recipe) {
        if (!recipe.isObject()) {
            return false;
        }
        for (String key : CRAFT_KEYS) {
            if (recipe.has(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean isForgeItem(JsonNode node) {
        JsonNode recipes = node.path("recipes");
        if (!recipes.isArray()) {
            return false;
        }
        for (JsonNode recipe : recipes) {
            if ("forge".equals(getString(recipe.path("type")))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPetItem(JsonNode node) {
        String id = getItemKey(node);
        if (id.matches(".*;[0-4]$")) {
            return true;
        }
        String nbttag = getString(node.path("nbttag"));
        return nbttag.contains("petInfo");
    }

    private boolean isShardItem(JsonNode node) {
        String id = getItemKey(node);
        return id.contains("SHARD");
    }

    private String getItemKey(JsonNode node) {
        String id = getString(node.path("id"));
        if (!id.isEmpty()) {
            return id;
        }
        return getString(node.path("internalname"));
    }

    private String getString(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        String value = node.asString();
        return value == null ? "" : value;
    }

}
