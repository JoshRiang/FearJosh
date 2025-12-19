package com.fearjosh.frontend.state.player;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.core.GameManager;

/**
 * Sprinting state - player berlari cepat.
 * Stamina drains saat sprint.
 * Auto-transition ke NormalState jika stamina habis atau berhenti sprint.
 */
public class SprintingState implements PlayerState {
    
    // Stamina drain rate (per second)
    private static final float STAMINA_DRAIN_RATE = 25f;
    
    // Singleton instance
    private static final SprintingState INSTANCE = new SprintingState();
    
    public static SprintingState getInstance() {
        return INSTANCE;
    }
    
    private SprintingState() {}
    
    @Override
    public void enter(Player player) {
        // Bisa tambah visual/sound effect
    }
    
    @Override
    public PlayerState update(Player player, float delta) {
        // Drain stamina
        float drainMultiplier = GameManager.getInstance().getDifficultyStrategy().staminaDrainMultiplier();
        float newStamina = player.getStamina() - STAMINA_DRAIN_RATE * drainMultiplier * delta;
        player.setStamina(Math.max(newStamina, 0f));
        
        // Check transitions ke NormalState:
        // 1. Stamina habis
        // 2. Tidak lagi punya sprint intent
        // 3. Berhenti bergerak
        if (player.getStamina() <= 0 || !player.hasSprintIntent() || !player.isMoving()) {
            return NormalState.getInstance();
        }
        
        return this;
    }
    
    @Override
    public void exit(Player player) {
        // Nothing special
    }
    
    @Override
    public float getSpeedMultiplier() {
        return GameManager.getInstance().getDifficultyStrategy().runSpeedMultiplier();
    }
    
    @Override
    public boolean canSprint() {
        return false; // Sudah sprint
    }
    
    @Override
    public String getName() {
        return "Sprinting";
    }
}
