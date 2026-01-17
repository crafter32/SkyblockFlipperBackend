package com.skyblockflipper.backend.model.Flipping.Recipe;

import com.skyblockflipper.backend.NEU.model.Item;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecipeUpdateFromTest {

    @Test
    void updateFromReplacesCollectionsAndBackReferences() {
        Item output1 = item("out");
        Item output2 = item("out2");
        Recipe recipe = new Recipe("r1", output1, RecipeProcessType.CRAFT, 5L, List.of());
        RecipeIngredient ingredient = new RecipeIngredient("item", 1);

        recipe.updateFrom(output2, RecipeProcessType.FORGE, 7L, List.of(ingredient));

        assertEquals("out2", recipe.getOutputItem().getId());
        assertEquals(RecipeProcessType.FORGE, recipe.getProcessType());
        assertEquals(recipe, ingredient.getRecipe());

        recipe.setIngredients(List.of());

        assertNull(ingredient.getRecipe());
    }

    private Item item(String id) {
        return Item.builder().id(id).displayName("name").build();
    }
}
