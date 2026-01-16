package com.skyblockflipper.backend.model.Flipping.Recipe;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecipeTest {

    @Test
    void setIngredientsWiresBackReference() {
        Recipe recipe = new Recipe("r1", "out", RecipeProcessType.CRAFT, 5L, List.of(), List.of());
        RecipeIngredient ingredient = new RecipeIngredient("item", 2);

        recipe.setIngredients(List.of(ingredient));

        assertEquals(recipe, ingredient.getRecipe());
        recipe.removeIngredient(ingredient);
        assertNull(ingredient.getRecipe());
    }

    @Test
    void setRequirementsWiresBackReference() {
        Recipe recipe = new Recipe("r1", "out", RecipeProcessType.FORGE, 5L, List.of(), List.of());
        RecipeRequirement requirement = RecipeRequirement.minCapital(100L);

        recipe.setRequirements(List.of(requirement));

        assertEquals(recipe, requirement.getRecipe());
        recipe.removeRequirement(requirement);
        assertNull(requirement.getRecipe());
    }
}
