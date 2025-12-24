package com.fearjosh.frontend.systems;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.input.InputHandler;

public class MovementSystem {
    
    private final InputHandler inputHandler;
    private boolean isMoving = false;
    
    public MovementSystem(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }
    
    public void update(Player player, float walkSpeed, float delta) {
        inputHandler.update(player, delta);
        
        float dx = inputHandler.getMoveDirX();
        float dy = inputHandler.getMoveDirY();
        isMoving = inputHandler.isMoving();
        
        float speed = walkSpeed * player.getSpeedMultiplier() * delta;
        
        if (isMoving) {
            applyMovementWithCollision(player, dx * speed, dy * speed);
        }
    }
    
    private void applyMovementWithCollision(Player player, float mx, float my) {
        float oldX = player.getX();
        float oldY = player.getY();
        
        player.move(mx, my);
        
        if (collidesWithFurniture(player)) {
            player.setX(oldX + mx);
            player.setY(oldY);
            boolean collideX = collidesWithFurniture(player);
            
            player.setX(oldX);
            player.setY(oldY + my);
            boolean collideY = collidesWithFurniture(player);
            
            if (!collideX) {
                player.setX(oldX + mx);
            } else {
                player.setX(oldX);
            }
            
            if (!collideY) {
                player.setY(oldY + my);
            } else {
                player.setY(oldY);
            }
        }
    }
    
    private boolean collidesWithFurniture(Player player) {
        return false;
    }
    
    public boolean isMoving() {
        return isMoving;
    }
}
