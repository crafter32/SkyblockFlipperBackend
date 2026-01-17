package com.skyblockflipper.backend.model.Flipping.Recipe;

import com.skyblockflipper.backend.NEU.model.Item;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecipeTest {

    @Test
    void setIngredientsWiresBackReference() {
        Recipe recipe = new Recipe("r1", item("out"), RecipeProcessType.CRAFT, 5L, List.of());
        RecipeIngredient ingredient = new RecipeIngredient("item", 2);

        recipe.setIngredients(List.of(ingredient));

        assertEquals(recipe, ingredient.getRecipe());
        recipe.removeIngredient(ingredient);
        assertNull(ingredient.getRecipe());
    }

    private Item item(String id) {
        return Item.builder().id(id).displayName("name").build();
    }
}
