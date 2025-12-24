package com.fearjosh.frontend.state.player;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.core.GameManager;

public class NormalState implements PlayerState {
    
    private static final float STAMINA_REGEN_RATE = 15f;
    
    private static final NormalState INSTANCE = new NormalState();
    
    public static NormalState getInstance() {
        return INSTANCE;
    }
    
    private NormalState() {}
    
    @Override
    public void enter(Player player) {
    }
    
    @Override
    public PlayerState update(Player player, float delta) {
        if (!player.isMoving()) {
            float newStamina = player.getStamina() + STAMINA_REGEN_RATE * delta;
            player.setStamina(Math.min(newStamina, player.getMaxStamina()));
        }
        
        if (player.hasSprintIntent() && player.isMoving() && player.getStamina() > 0) {
            return SprintingState.getInstance();
        }
        
        return this;
    }
    
    @Override
    public void exit(Player player) {
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
