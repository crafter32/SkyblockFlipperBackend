package com.skyblockflipper.backend.model.Flipping.Recipe;

import com.skyblockflipper.backend.model.Flipping.Enums.FlipType;
import com.skyblockflipper.backend.model.Flipping.Enums.StepType;
import com.skyblockflipper.backend.model.Flipping.Flip;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeToFlipMapperTest {

    private final RecipeToFlipMapper mapper = new RecipeToFlipMapper();

    @Test
    void mapsForgeRecipeWithRequirements() {
        Recipe recipe = new Recipe(
                "r1",
                "out",
                RecipeProcessType.FORGE,
                -5L,
                List.of(new RecipeIngredient("item", 2)),
                List.of(RecipeRequirement.minForgeSlots(2), RecipeRequirement.minCapital(100L))
        );

        Flip flip = mapper.fromRecipe(recipe);

        assertEquals(FlipType.FORGE, flip.getFlipType());
        assertEquals("out", flip.getResultItemId());
        assertEquals(2, flip.getConstraints().size());
        assertEquals(StepType.BUY, flip.getSteps().get(0).getType());
        assertEquals(StepType.FORGE, flip.getSteps().get(1).getType());
        assertTrue(flip.getSteps().get(1).getBaseDurationSeconds() >= 0);
    }
}
