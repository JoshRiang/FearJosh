package com.fearjosh.frontend.systems;

import com.badlogic.gdx.math.Rectangle;
import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.world.Room;
import com.fearjosh.frontend.world.objects.Table;
import com.fearjosh.frontend.world.objects.Locker;
import com.fearjosh.frontend.input.InputHandler;

/**
 * MovementSystem - Handles player movement and collision resolution.
 * 
 * Responsibilities:
 * - Process movement input via InputHandler
 * - Apply movement with furniture collision resolution
 * - Handle room boundary clamping
 */
public class MovementSystem {
    
    private final InputHandler inputHandler;
    private boolean isMoving = false;
    
    public MovementSystem(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }
    
    /**
     * Process input and move player with collision resolution.
     * 
     * @param player The player to move
     * @param room Current room for collision checking
     * @param walkSpeed Base walking speed
     * @param delta Delta time
     */
    public void update(Player player, Room room, float walkSpeed, float delta) {
        // 1. Poll input and execute commands
        inputHandler.update(player, delta);
        
        // 2. Get movement direction from InputHandler
        float dx = inputHandler.getMoveDirX();
        float dy = inputHandler.getMoveDirY();
        isMoving = inputHandler.isMoving();
        
        // 3. Calculate speed based on Player state (Normal/Sprinting)
        float speed = walkSpeed * player.getSpeedMultiplier() * delta;
        
        // 4. Apply movement with collision resolution
        if (isMoving) {
            applyMovementWithCollision(player, room, dx * speed, dy * speed);
        }
    }
    
    /**
     * Apply movement with slide-along-edges collision resolution.
     */
    private void applyMovementWithCollision(Player player, Room room, float mx, float my) {
        float oldX = player.getX();
        float oldY = player.getY();
        
        // Move and update facing direction
        player.move(mx, my);
        
        // Collision resolution per-axis (slide along edges)
        if (collidesWithFurniture(player, room)) {
            // Test X-only
            player.setX(oldX + mx);
            player.setY(oldY);
            boolean collideX = collidesWithFurniture(player, room);
            
            // Test Y-only
            player.setX(oldX);
            player.setY(oldY + my);
            boolean collideY = collidesWithFurniture(player, room);
            
            // Apply results
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
    
    /**
     * COLLISION FURNITURE: Uses FOOT HITBOX of player.
     * Player only collides with feet, not full body.
     */
    private boolean collidesWithFurniture(Player player, Room room) {
        Rectangle footBounds = player.getFootBounds();
        
        // Check tables
        for (Table t : room.getTables()) {
            if (footBounds.overlaps(t.getCollisionBounds())) {
                return true;
            }
        }
        
        // Check lockers
        for (Locker l : room.getLockers()) {
            if (footBounds.overlaps(l.getCollisionBounds())) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isMoving() {
        return isMoving;
    }
}
