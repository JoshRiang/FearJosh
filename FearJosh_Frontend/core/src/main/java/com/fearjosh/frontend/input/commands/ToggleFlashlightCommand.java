package com.fearjosh.frontend.input.commands;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.input.Command;

/**
 * Command untuk Toggle Flashlight (F key - single trigger)
 * Hanya execute SEKALI saat key ditekan, bukan terus-menerus
 */
public class ToggleFlashlightCommand implements Command {
    @Override
    public void execute(Player player, float delta) {
        player.toggleFlashlight();
    }
}
