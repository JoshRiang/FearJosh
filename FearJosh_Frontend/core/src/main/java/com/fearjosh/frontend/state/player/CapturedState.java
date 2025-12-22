package com.fearjosh.frontend.state.player;

import com.fearjosh.frontend.entity.Player;

/**
 * Captured state - player has been caught by the enemy.
 * In this state, player cannot move or sprint.
 * Waiting for escape minigame or game over.
 */
public class CapturedState implements PlayerState {
    
    // Singleton instance to avoid allocation
    private static final CapturedState INSTANCE = new CapturedState();
    
    public static CapturedState getInstance() {
        return INSTANCE;
    }
    
    private CapturedState() {}
    
    @Override
    public void enter(Player player) {
        // Player is captured - stop all movement
        player.setMoving(false);
        System.out.println("[CapturedState] Player captured!");
    }
    
    @Override
    public PlayerState update(Player player, float delta) {
        // Player cannot move in captured state
        // Transition back to NormalState is handled externally when escape succeeds
        return this;
    }
    
    @Override
    public void exit(Player player) {
        System.out.println("[CapturedState] Player escaped or released!");
    }
    
    @Override
    public float getSpeedMultiplier() {
        return 0f; // Cannot move when captured
    }
    
    @Override
    public boolean canSprint() {
        return false; // Cannot sprint when captured
    }
    
    @Override
    public String getName() {
        return "Captured";
    }
}
