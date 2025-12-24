package com.fearjosh.frontend.state.player;

import com.fearjosh.frontend.entity.Player;

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
        player.setMoving(false);
        player.setSprintIntent(false);
    }

    @Override
    public PlayerState update(Player player, float delta) {
        return this;
    }

    @Override
    public void exit(Player player) {
        System.out.println("[InjuredState] Player successfully bandaged themselves!");
    }

    @Override
    public float getSpeedMultiplier() {
        return 0.0f;
    }

    @Override
    public boolean canSprint() {
        return false;
    }

    @Override
    public String getName() {
        return "INJURED";
    }
}
