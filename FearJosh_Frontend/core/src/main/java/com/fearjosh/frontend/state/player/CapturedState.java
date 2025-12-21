package com.fearjosh.frontend.state.player;

import com.fearjosh.frontend.entity.Player;

/**
 * CapturedState - Player tertangkap Josh dan terikat
 * Player tidak bisa bergerak dan hanya bisa menekan F untuk memulai minigame
 */
public class CapturedState implements PlayerState {

    private static CapturedState instance;

    private CapturedState() {
    }

    public static CapturedState getInstance() {
        if (instance == null) {
            instance = new CapturedState();
        }
        return instance;
    }

    @Override
    public void enter(Player player) {
        System.out.println("[CapturedState] Player terikat! Tidak bisa bergerak.");
        // Stop all movement
        player.setMoving(false);
        player.setSprintIntent(false);
    }

    @Override
    public PlayerState update(Player player, float delta) {
        // Player terikat, tidak ada stamina drain/regen
        // State ini akan diubah dari luar (PlayScreen) setelah minigame
        return this;
    }

    @Override
    public void exit(Player player) {
        System.out.println("[CapturedState] Player berhasil lepas dari ikatan!");
    }

    @Override
    public float getSpeedMultiplier() {
        return 0.0f; // Tidak bisa bergerak sama sekali
    }

    @Override
    public boolean canSprint() {
        return false; // Tidak bisa sprint saat terikat
    }

    @Override
    public String getName() {
        return "CAPTURED";
    }
}
