package com.skyblockflipper.backend.model.Flipping.Recipe;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Entity
@Table(name = "recipes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipe {

    @Id
    @Column(name = "recipe_id", nullable = false, updatable = false)
    private String recipeId;

    @Column(name = "output_item_id", nullable = false)
    private String outputItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_type", nullable = false)
    private RecipeProcessType processType;

    @Column(name = "process_duration_seconds", nullable = false)
    private long processDurationSeconds;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeRequirement> requirements = new ArrayList<>();

    public Recipe(String recipeId, String outputItemId, RecipeProcessType processType, long processDurationSeconds,
                  List<RecipeIngredient> ingredients, List<RecipeRequirement> requirements) {
        this.recipeId = Objects.requireNonNull(recipeId, "recipeId");
        updateFrom(outputItemId, processType, processDurationSeconds, ingredients, requirements);
    }

    public void updateFrom(String outputItemId, RecipeProcessType processType, long processDurationSeconds,
                           List<RecipeIngredient> ingredients, List<RecipeRequirement> requirements) {
        this.outputItemId = Objects.requireNonNull(outputItemId, "outputItemId");
        this.processType = Objects.requireNonNull(processType, "processType");
        this.processDurationSeconds = processDurationSeconds;
        setIngredients(ingredients);
        setRequirements(requirements);
    }

    public void setIngredients(List<RecipeIngredient> newIngredients) {
        clearIngredients();
        if (newIngredients != null) {
            newIngredients.forEach(this::addIngredient);
        }
    }

    public void addIngredient(RecipeIngredient ingredient) {
        Objects.requireNonNull(ingredient, "ingredient");
        ingredient.setRecipe(this);
        this.ingredients.add(ingredient);
    }

    public void removeIngredient(RecipeIngredient ingredient) {
        if (ingredient != null && this.ingredients.remove(ingredient)) {
            ingredient.setRecipe(null);
        }
    }

    public void setRequirements(List<RecipeRequirement> newRequirements) {
        clearRequirements();
        if (newRequirements != null) {
            newRequirements.forEach(this::addRequirement);
        }
    }

    public void addRequirement(RecipeRequirement requirement) {
        Objects.requireNonNull(requirement, "requirement");
        requirement.setRecipe(this);
        this.requirements.add(requirement);
    }

    public void removeRequirement(RecipeRequirement requirement) {
        if (requirement != null && this.requirements.remove(requirement)) {
            requirement.setRecipe(null);
        }
    }

    private void clearIngredients() {
        this.ingredients.forEach(ingredient -> ingredient.setRecipe(null));
        this.ingredients.clear();
    }

    private void clearRequirements() {
        this.requirements.forEach(requirement -> requirement.setRecipe(null));
        this.requirements.clear();
    }
}
