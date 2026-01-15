package com.skyblockflipper.backend.repository;

import com.skyblockflipper.backend.model.Flipping.Flip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FlipRepository extends JpaRepository<Flip, UUID> {
}
