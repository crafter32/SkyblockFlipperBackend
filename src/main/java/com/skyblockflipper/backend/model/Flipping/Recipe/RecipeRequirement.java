package com.skyblockflipper.backend.model.Flipping.Recipe;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@Table(name = "recipe_requirements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RecipeRequirementType type;

    @Column(name = "string_value")
    private String stringValue;

    @Column(name = "int_value")
    private Integer intValue;

    @Column(name = "long_value")
    private Long longValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    private RecipeRequirement(RecipeRequirementType type, String stringValue, Integer intValue, Long longValue) {
        this.type = Objects.requireNonNull(type, "type");
        this.stringValue = stringValue;
        this.intValue = intValue;
        this.longValue = longValue;
    }

    public static RecipeRequirement minForgeSlots(int slots) {
        return new RecipeRequirement(RecipeRequirementType.MIN_FORGE_SLOTS, null, slots, null);
    }

    public static RecipeRequirement recipeUnlocked(String recipeId) {
        return new RecipeRequirement(RecipeRequirementType.RECIPE_UNLOCKED, recipeId, null, null);
    }

    public static RecipeRequirement minCapital(long coins) {
        return new RecipeRequirement(RecipeRequirementType.MIN_CAPITAL, null, null, coins);
    }

    void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }
}
