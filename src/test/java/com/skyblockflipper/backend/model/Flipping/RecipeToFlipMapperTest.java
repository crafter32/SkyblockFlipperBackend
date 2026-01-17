package com.skyblockflipper.backend.model.Flipping;

import com.skyblockflipper.backend.NEU.model.Item;
import com.skyblockflipper.backend.model.Flipping.Enums.FlipType;
import com.skyblockflipper.backend.model.Flipping.Enums.StepType;
import com.skyblockflipper.backend.model.Flipping.Recipe.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeToFlipMapperTest {

    @Test
    void mapsForgeRecipeToFlipWithConstraints() {
        Recipe recipe = new Recipe(
                "forge_recipe_1",
                item("REFINED_DIAMOND"),
                RecipeProcessType.FORGE,
                3600L,
                List.of(
                        new RecipeIngredient("DIAMOND", 160),
                        new RecipeIngredient("COAL", 1)
                )
        );

        RecipeToFlipMapper mapper = new RecipeToFlipMapper();
        Flip flip = mapper.fromRecipe(recipe);

        assertEquals(FlipType.FORGE, flip.getFlipType());
        assertEquals("REFINED_DIAMOND", flip.getResultItemId());
        assertEquals(3, flip.getSteps().size());
        assertEquals(StepType.FORGE, flip.getSteps().getLast().getType());
        assertTrue(flip.getConstraints().isEmpty());
    }

    private Item item(String id) {
        return Item.builder().id(id).displayName("name").build();
    }
}
