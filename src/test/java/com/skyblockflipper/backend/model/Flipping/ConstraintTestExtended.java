package com.skyblockflipper.backend.model.Flipping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstraintTestExtended {

    @Test
    void toStringIncludesType() {
        Constraint constraint = Constraint.minCapital(500L);

        String text = constraint.toString();

        assertEquals(true, text.contains("MIN_CAPITAL"));
    }
}
