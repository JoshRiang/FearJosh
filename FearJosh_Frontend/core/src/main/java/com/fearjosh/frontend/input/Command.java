package com.fearjosh.frontend.input;

import com.fearjosh.frontend.entity.Player;

/**
 * Command Pattern interface untuk input handling.
 * Setiap command merepresentasikan satu aksi yang bisa dilakukan player.
 */
public interface Command {
    /**
     * Execute command pada player
     * @param player Target player
     * @param delta Delta time untuk frame-rate independent movement
     */
    void execute(Player player, float delta);
}
