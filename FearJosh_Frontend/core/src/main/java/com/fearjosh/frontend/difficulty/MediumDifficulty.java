package com.fearjosh.frontend.difficulty;

/**
 * Medium difficulty - balanced untuk kebanyakan pemain.
 * Semua multiplier di nilai default (1.0).
 */
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
