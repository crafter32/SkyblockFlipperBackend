package com.skyblockflipper.backend.repository;

import com.skyblockflipper.backend.model.Flipping.Recipe.RecipeRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRequirementRepository extends JpaRepository<RecipeRequirement, Long> {
}
