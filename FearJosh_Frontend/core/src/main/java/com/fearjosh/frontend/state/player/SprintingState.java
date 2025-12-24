package com.fearjosh.frontend.state.player;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.core.GameManager;

public class SprintingState implements PlayerState {
    
    private static final float STAMINA_DRAIN_RATE = 25f;
    
    private static final SprintingState INSTANCE = new SprintingState();
    
    public static SprintingState getInstance() {
        return INSTANCE;
    }
    
    private SprintingState() {}
    
    @Override
    public void enter(Player player) {
    }
    
    @Override
    public PlayerState update(Player player, float delta) {
        float drainMultiplier = GameManager.getInstance().getDifficultyStrategy().staminaDrainMultiplier();
        float newStamina = player.getStamina() - STAMINA_DRAIN_RATE * drainMultiplier * delta;
        player.setStamina(Math.max(newStamina, 0f));
        
        if (player.getStamina() <= 0 || !player.hasSprintIntent() || !player.isMoving()) {
            return NormalState.getInstance();
        }
        
        return this;
    }
    
    @Override
    public void exit(Player player) {
    }
    
    @Override
    public float getSpeedMultiplier() {
        return GameManager.getInstance().getDifficultyStrategy().runSpeedMultiplier();
    }
    
    @Override
    public boolean canSprint() {
        return false;
    }
    
    @Override
    public String getName() {
        return "Sprinting";
    }
}
