package com.fearjosh.frontend.input.commands;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.input.Command;

/**
 * Command untuk Sprint (SHIFT hold)
 * Ini adalah "intent" sprint - PlayerState yang memutuskan boleh atau tidak
 */
public class SprintCommand implements Command {
    @Override
    public void execute(Player player, float delta) {
        // Sprint intent - actual sprint decision ada di PlayerState
        player.setSprintIntent(true);
    }
}
