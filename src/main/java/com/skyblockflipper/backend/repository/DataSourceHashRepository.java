package com.skyblockflipper.backend.repository;

import com.skyblockflipper.backend.model.DataSourceHash;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DataSourceHashRepository extends JpaRepository<DataSourceHash, UUID> {
}
