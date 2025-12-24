package com.fearjosh.frontend.input.commands;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.input.Command;

public class SprintCommand implements Command {
    @Override
    public void execute(Player player, float delta) {
        player.setSprintIntent(true);
    }
}
