package com.fearjosh.frontend.difficulty;

public class EasyDifficulty implements DifficultyStrategy {
    @Override
    public float walkSpeedMultiplier() {
        return 1.0f;
    }

    @Override
    public float runSpeedMultiplier() {
        return 1.4f;
    }

    @Override
    public float staminaDrainMultiplier() {
        return 0.7f;
    }

    @Override
    public float batteryDrainMultiplier() {
        return 0.8f;
    }

    @Override
    public float itemSpawnRateMultiplier() {
        return 1.4f;
    }

    @Override
    public float visionRadius() {
        return 130f;
    }

    @Override
    public float fogDarkness() {
        return 0.7f;
    }
}
