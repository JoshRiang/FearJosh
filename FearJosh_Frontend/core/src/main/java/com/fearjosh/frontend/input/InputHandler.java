package com.fearjosh.frontend.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.input.commands.SprintCommand;
import com.fearjosh.frontend.input.commands.ToggleFlashlightCommand;

public class InputHandler {
    
    // MOVEMENT
    private boolean moveUp = false;
    private boolean moveDown = false;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean sprintHeld = false;
    
    // FLASHLIGHT
    private boolean flashlightKeyWasPressed = false;
    
    // COMMANDS
    private final Command toggleFlashlightCommand = new ToggleFlashlightCommand();
    private final Command sprintCommand = new SprintCommand();
    
    // OUTPUT
    private float moveDirX = 0f;
    private float moveDirY = 0f;
    private boolean isMoving = false;
    
    public void pollInput() {
        moveUp = Gdx.input.isKeyPressed(Input.Keys.W);
        moveDown = Gdx.input.isKeyPressed(Input.Keys.S);
        moveLeft = Gdx.input.isKeyPressed(Input.Keys.A);
        moveRight = Gdx.input.isKeyPressed(Input.Keys.D);
        
        sprintHeld = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) 
                  || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        
        moveDirX = 0f;
        moveDirY = 0f;
        
        if (moveUp) moveDirY += 1f;
        if (moveDown) moveDirY -= 1f;
        if (moveLeft) moveDirX -= 1f;
        if (moveRight) moveDirX += 1f;
        
        isMoving = (moveDirX != 0f || moveDirY != 0f);
        
        if (isMoving) {
            float len = (float) Math.sqrt(moveDirX * moveDirX + moveDirY * moveDirY);
            moveDirX /= len;
            moveDirY /= len;
        }
    }
    
    public void executeCommands(Player player, float delta) {
        player.setSprintIntent(false);
        
        if (sprintHeld && isMoving) {
            sprintCommand.execute(player, delta);
        }
        
        boolean flashlightKeyPressed = Gdx.input.isKeyPressed(Input.Keys.F);
        if (flashlightKeyPressed && !flashlightKeyWasPressed) {
            toggleFlashlightCommand.execute(player, delta);
        }
        flashlightKeyWasPressed = flashlightKeyPressed;
    }
    
    public void update(Player player, float delta) {
        pollInput();
        executeCommands(player, delta);
    }
    
    // GETTERS
    
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
