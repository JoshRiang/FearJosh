package com.fearjosh.frontend.difficulty;

/**
 * Hard difficulty - untuk pemain yang ingin tantangan.
 * - Speed sedikit lebih lambat
 * - Stamina drain lebih cepat
 * - Battery drain lebih cepat
 * - Vision radius lebih sempit
 * - Fog lebih tebal
 */
public class HardDifficulty implements DifficultyStrategy {
    @Override
    public float walkSpeedMultiplier() {
        return 0.95f;
    }

    @Override
    public float runSpeedMultiplier() {
        return 1.33f;  // Sprint gives 40% speed boost - CUSTOM: ubah angka ini (0.95-2.0)
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
