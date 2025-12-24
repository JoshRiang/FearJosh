package com.fearjosh.frontend.input;

import com.fearjosh.frontend.entity.Player;

public interface Command {
    void execute(Player player, float delta);
}
