package com.skyblockflipper.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan("com.skyblockflipper.backend.NEU.model")
public class SkyblockFlipperBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkyblockFlipperBackendApplication.class, args);
	}
}
