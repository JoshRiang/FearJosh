package com.fearjosh.frontend.state.player;

import com.fearjosh.frontend.entity.Player;

/**
 * InjuredState - Player is injured and doing the bandaging minigame.
 * Player cannot move during this state and must complete the minigame.
 * Uses jonatan_injured.png sprite to show injured appearance.
 */
public class InjuredState implements PlayerState {

    private static InjuredState instance;

    private InjuredState() {
    }

    public static InjuredState getInstance() {
        if (instance == null) {
            instance = new InjuredState();
        }
        return instance;
    }

    @Override
    public void enter(Player player) {
        System.out.println("[InjuredState] Player is injured! Starting bandage minigame...");
        // Stop all movement
        player.setMoving(false);
        player.setSprintIntent(false);
    }

    @Override
    public PlayerState update(Player player, float delta) {
        // Player is injured, no stamina drain/regen
        // State will be changed from PlayScreen after minigame completes
        return this;
    }

    @Override
    public void exit(Player player) {
        System.out.println("[InjuredState] Player successfully bandaged themselves!");
    }

    @Override
    public float getSpeedMultiplier() {
        return 0.0f; // Cannot move while injured/bandaging
    }

    @Override
    public boolean canSprint() {
        return false; // Cannot sprint while injured
    }

    @Override
    public String getName() {
        return "INJURED";
    }
}
