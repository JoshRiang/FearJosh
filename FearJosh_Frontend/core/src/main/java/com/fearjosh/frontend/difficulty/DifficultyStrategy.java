package com.fearjosh.frontend.difficulty;

/**
 * Strategy pattern untuk difficulty settings.
 * Mengontrol multiplier untuk berbagai aspek gameplay.
 */
public interface DifficultyStrategy {
    float walkSpeedMultiplier();
    float runSpeedMultiplier();
    float staminaDrainMultiplier();
    float batteryDrainMultiplier();
    float itemSpawnRateMultiplier();
    float visionRadius();
    float fogDarkness();
}
