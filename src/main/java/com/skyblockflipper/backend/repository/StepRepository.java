package com.skyblockflipper.backend.repository;

import com.skyblockflipper.backend.model.Flipping.Step;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StepRepository extends JpaRepository<Step, UUID> {
}
