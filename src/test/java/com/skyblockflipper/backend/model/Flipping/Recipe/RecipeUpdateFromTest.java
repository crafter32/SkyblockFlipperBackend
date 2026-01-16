package com.skyblockflipper.backend.model.Flipping.Recipe;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecipeUpdateFromTest {

    @Test
    void updateFromReplacesCollectionsAndBackReferences() {
        Recipe recipe = new Recipe("r1", "out", RecipeProcessType.CRAFT, 5L, List.of(), List.of());
        RecipeIngredient ingredient = new RecipeIngredient("item", 1);
        RecipeRequirement requirement = RecipeRequirement.recipeUnlocked("abc");

        recipe.updateFrom("out2", RecipeProcessType.FORGE, 7L, List.of(ingredient), List.of(requirement));

        assertEquals("out2", recipe.getOutputItemId());
        assertEquals(RecipeProcessType.FORGE, recipe.getProcessType());
        assertEquals(recipe, ingredient.getRecipe());
        assertEquals(recipe, requirement.getRecipe());

        recipe.setIngredients(List.of());
        recipe.setRequirements(List.of());

        assertNull(ingredient.getRecipe());
        assertNull(requirement.getRecipe());
    }
}
