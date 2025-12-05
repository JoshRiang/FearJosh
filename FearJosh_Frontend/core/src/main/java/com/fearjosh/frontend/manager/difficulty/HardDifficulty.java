package com.fearjosh.frontend.manager.difficulty;

import com.fearjosh.frontend.manager.DifficultyStrategy;

public class HardDifficulty implements DifficultyStrategy {
    @Override
    public float walkSpeedMultiplier() {
        return 0.95f;
    }

    @Override
    public float runSpeedMultiplier() {
        return 0.95f;
    }

    @Override
    public float staminaDrainMultiplier() {
        return 1.3f;
    }

    @Override
    public float batteryDrainMultiplier() {
        return 1.2f;
    }

    @Override
    public float itemSpawnRateMultiplier() {
        return 0.6f;
    }

    @Override
    public float visionRadius() {
        return 55f;
    }

    @Override
    public float fogDarkness() {
        return 0.95f;
    }
}
