package com.skyblockflipper.backend.repository;

import com.skyblockflipper.backend.model.Flipping.Recipe.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, String> {
}
