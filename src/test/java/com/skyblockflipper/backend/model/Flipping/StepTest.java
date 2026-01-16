package com.skyblockflipper.backend.model.Flipping;

import com.skyblockflipper.backend.model.Flipping.Enums.DurationType;
import com.skyblockflipper.backend.model.Flipping.Enums.SchedulingPolicy;
import com.skyblockflipper.backend.model.Flipping.Enums.StepResource;
import com.skyblockflipper.backend.model.Flipping.Enums.StepType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StepTest {

    @Test
    void validateDefaultsMarketBasedBaseSeconds() {
        Step step = new Step(null, StepType.BUY, DurationType.MARKET_BASED, null, null,
                StepResource.NONE, 0, SchedulingPolicy.BEST_EFFORT, null);

        ReflectionTestUtils.invokeMethod(step, "validate");

        assertEquals(30L, step.getBaseDurationSeconds());
    }

    @Test
    void validateRejectsInvalidForgeStep() {
        Step step = new Step(null, StepType.FORGE, DurationType.INSTANT, 5L, null,
                StepResource.NONE, 0, SchedulingPolicy.NONE, null);

        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(step, "validate"));
    }

    @Test
    void factoryMethodsSetExpectedDefaults() {
        Step buy = Step.forBuyMarketBased(25L, "{}");
        Step sell = Step.forSellMarketBased(25L, "{}");
        Step forge = Step.forForgeFixed(120L);
        Step craft = Step.forCraftInstant(2L);

        assertEquals(StepType.BUY, buy.getType());
        assertEquals(DurationType.MARKET_BASED, buy.getDurationType());
        assertEquals(StepType.SELL, sell.getType());
        assertEquals(StepType.FORGE, forge.getType());
        assertEquals(StepType.CRAFT, craft.getType());
    }
}
