package com.fearjosh.frontend.difficulty;

public interface DifficultyStrategy {
    float walkSpeedMultiplier();
    float runSpeedMultiplier();
    float staminaDrainMultiplier();
    float batteryDrainMultiplier();
    float itemSpawnRateMultiplier();
    float visionRadius();
    float fogDarkness();
}
