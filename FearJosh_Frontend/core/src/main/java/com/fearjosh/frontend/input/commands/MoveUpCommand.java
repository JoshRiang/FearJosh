package com.fearjosh.frontend.input.commands;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.input.Command;

/**
 * Command untuk move UP (W key)
 */
public class MoveUpCommand implements Command {
    @Override
    public void execute(Player player, float delta) {
        // Movement direction diakumulasi di InputHandler
        // Command ini hanya menandai intent
    }
}
