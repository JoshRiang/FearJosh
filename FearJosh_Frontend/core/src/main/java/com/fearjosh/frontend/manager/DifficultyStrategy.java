package com.fearjosh.frontend.manager;

public interface DifficultyStrategy {
    float walkSpeedMultiplier();

    float runSpeedMultiplier();

    float staminaDrainMultiplier();

    float batteryDrainMultiplier();

    float itemSpawnRateMultiplier();

    float visionRadius();

    float fogDarkness();
}
