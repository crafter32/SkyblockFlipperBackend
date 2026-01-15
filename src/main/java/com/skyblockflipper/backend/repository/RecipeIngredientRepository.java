package com.skyblockflipper.backend.repository;

import com.skyblockflipper.backend.model.Flipping.Recipe.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
}
