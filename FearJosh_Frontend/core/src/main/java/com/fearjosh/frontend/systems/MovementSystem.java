package com.fearjosh.frontend.systems;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.input.InputHandler;

/**
 * MovementSystem - Handles player movement and collision resolution.
 * NOTE: This class is currently UNUSED. PlayScreen handles player movement directly.
 * Room parameter removed for compatibility with TMX-based collision system.
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
     * Room parameter removed; collision should be handled via TiledMapManager.
     * 
     * @param player The player to move
     * @param walkSpeed Base walking speed
     * @param delta Delta time
     */
    public void update(Player player, float walkSpeed, float delta) {
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
            applyMovementWithCollision(player, dx * speed, dy * speed);
        }
    }
    
    /**
     * Apply movement with slide-along-edges collision resolution.
     * Room parameter removed; collision checking now handled via TiledMapManager.
     */
    private void applyMovementWithCollision(Player player, float mx, float my) {
        float oldX = player.getX();
        float oldY = player.getY();
        
        // Move and update facing direction
        player.move(mx, my);
        
        // Collision resolution per-axis (slide along edges)
        // NOTE: Collision checking now done in PlayScreen via TiledMapManager
        // This system is kept for compatibility but not actively used
        if (collidesWithFurniture(player)) {
            // Test X-only
            player.setX(oldX + mx);
            player.setY(oldY);
            boolean collideX = collidesWithFurniture(player);
            
            // Test Y-only
            player.setX(oldX);
            player.setY(oldY + my);
            boolean collideY = collidesWithFurniture(player);
            
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
     * NOTE: Returns false - TMX collision is handled in PlayScreen via TiledMapManager
     * This method is kept for compatibility but should not be used.
     */
    private boolean collidesWithFurniture(Player player) {
        // TMX collision is now handled via TiledMapManager in PlayScreen
        // This system is obsolete but kept for compilation compatibility
        return false;
    }
    
    public boolean isMoving() {
        return isMoving;
    }
}
