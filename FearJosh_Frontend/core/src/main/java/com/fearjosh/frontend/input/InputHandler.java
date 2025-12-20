package com.fearjosh.frontend.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.input.commands.SprintCommand;
import com.fearjosh.frontend.input.commands.ToggleFlashlightCommand;

/**
 * InputHandler - Central input management menggunakan Command Pattern.
 * 
 * Tanggung jawab:
 * - Mapping key -> Command
 * - Track pressed state untuk movement & sprint
 * - Handle single-trigger untuk flashlight (execute sekali per keyDown)
 * 
 * TIDAK boleh: render, akses UI menu
 */
public class InputHandler {
    
    // Movement state - diakumulasi tiap frame
    private boolean moveUp = false;
    private boolean moveDown = false;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean sprintHeld = false;
    
    // Flashlight toggle state - untuk single trigger
    private boolean flashlightKeyWasPressed = false;
    
    // Commands (bisa di-inject untuk flexibility)
    private final Command toggleFlashlightCommand = new ToggleFlashlightCommand();
    private final Command sprintCommand = new SprintCommand();
    
    // Output dari input processing
    private float moveDirX = 0f;
    private float moveDirY = 0f;
    private boolean isMoving = false;
    
    /**
     * Poll semua input dan update state.
     * Dipanggil setiap frame dari PlayScreen.
     */
    public void pollInput() {
        // Movement keys (hold-based)
        moveUp = Gdx.input.isKeyPressed(Input.Keys.W);
        moveDown = Gdx.input.isKeyPressed(Input.Keys.S);
        moveLeft = Gdx.input.isKeyPressed(Input.Keys.A);
        moveRight = Gdx.input.isKeyPressed(Input.Keys.D);
        
        // Sprint key (hold-based)
        sprintHeld = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) 
                  || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        
        // Calculate movement direction
        moveDirX = 0f;
        moveDirY = 0f;
        
        if (moveUp) moveDirY += 1f;
        if (moveDown) moveDirY -= 1f;
        if (moveLeft) moveDirX -= 1f;
        if (moveRight) moveDirX += 1f;
        
        isMoving = (moveDirX != 0f || moveDirY != 0f);
        
        // Normalize movement vector
        if (isMoving) {
            float len = (float) Math.sqrt(moveDirX * moveDirX + moveDirY * moveDirY);
            moveDirX /= len;
            moveDirY /= len;
        }
    }
    
    /**
     * Execute commands pada player.
     * Dipanggil setelah pollInput().
     * 
     * @param player Target player
     * @param delta Delta time
     */
    public void executeCommands(Player player, float delta) {
        // Reset sprint intent setiap frame
        player.setSprintIntent(false);
        
        // Sprint command (hold-based)
        if (sprintHeld && isMoving) {
            sprintCommand.execute(player, delta);
        }
        
        // Flashlight toggle - SINGLE TRIGGER
        // Hanya execute sekali saat key BARU ditekan
        boolean flashlightKeyPressed = Gdx.input.isKeyPressed(Input.Keys.F);
        if (flashlightKeyPressed && !flashlightKeyWasPressed) {
            // Key baru ditekan frame ini -> toggle
            toggleFlashlightCommand.execute(player, delta);
        }
        flashlightKeyWasPressed = flashlightKeyPressed;
    }
    
    /**
     * Update player berdasarkan input.
     * Kombinasi pollInput() + executeCommands() untuk convenience.
     */
    public void update(Player player, float delta) {
        pollInput();
        executeCommands(player, delta);
    }
    
    // ------------ Getters untuk PlayScreen ------------
    
    public float getMoveDirX() {
        return moveDirX;
    }
    
    public float getMoveDirY() {
        return moveDirY;
    }
    
    public boolean isMoving() {
        return isMoving;
    }
    
    public boolean isSprintHeld() {
        return sprintHeld;
    }
}
