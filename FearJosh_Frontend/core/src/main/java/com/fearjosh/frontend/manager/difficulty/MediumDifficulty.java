package com.fearjosh.frontend.manager.difficulty;

import com.fearjosh.frontend.manager.DifficultyStrategy;

public class MediumDifficulty implements DifficultyStrategy {
    @Override
    public float walkSpeedMultiplier() {
        return 1.0f;
    }

    @Override
    public float runSpeedMultiplier() {
        return 1.0f;
    }

    @Override
    public float staminaDrainMultiplier() {
        return 1.0f;
    }

    @Override
    public float batteryDrainMultiplier() {
        return 1.0f;
    }

    @Override
    public float itemSpawnRateMultiplier() {
        return 1.0f;
    }

    @Override
    public float visionRadius() {
        return 90f;
    }

    @Override
    public float fogDarkness() {
        return 0.8f;
    }
}
