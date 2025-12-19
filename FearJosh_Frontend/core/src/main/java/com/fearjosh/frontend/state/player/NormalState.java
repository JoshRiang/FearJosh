package com.fearjosh.frontend.state.player;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.core.GameManager;

/**
 * Normal state - player berjalan biasa.
 * Stamina regenerates saat tidak bergerak.
 */
public class NormalState implements PlayerState {
    
    // Stamina regen rate (per second)
    private static final float STAMINA_REGEN_RATE = 15f;
    
    // Singleton instance untuk avoid allocation
    private static final NormalState INSTANCE = new NormalState();
    
    public static NormalState getInstance() {
        return INSTANCE;
    }
    
    private NormalState() {}
    
    @Override
    public void enter(Player player) {
        // Nothing special saat masuk normal state
    }
    
    @Override
    public PlayerState update(Player player, float delta) {
        // Regen stamina saat tidak bergerak
        if (!player.isMoving()) {
            float newStamina = player.getStamina() + STAMINA_REGEN_RATE * delta;
            player.setStamina(Math.min(newStamina, player.getMaxStamina()));
        }
        
        // Check transition ke SprintingState
        if (player.hasSprintIntent() && player.isMoving() && player.getStamina() > 0) {
            return SprintingState.getInstance();
        }
        
        return this;
    }
    
    @Override
    public void exit(Player player) {
        // Nothing special
    }
    
    @Override
    public float getSpeedMultiplier() {
        return GameManager.getInstance().getDifficultyStrategy().walkSpeedMultiplier();
    }
    
    @Override
    public boolean canSprint() {
        return true;
    }
    
    @Override
    public String getName() {
        return "Normal";
    }
}
