package com.fearjosh.frontend.systems;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.world.RoomId;

/**
 * RoomTransitionSystem - Handles room-to-room transitions.
 * 
 * Responsibilities:
 * - Check if player is at door boundaries
 * - Handle transition cooldown
 * - Return transition info for room switching
 */
public class RoomTransitionSystem {
    
    private final float doorWidth;
    private final float wallThickness;
    private final float entryOffset;
    private final float virtualWidth;
    private final float virtualHeight;
    
    private float transitionCooldown = 0f;
    private static final float TRANSITION_COOLDOWN_DURATION = 0.2f;
    
    /**
     * Result of a transition check.
     */
    public static class TransitionResult {
        public final RoomId targetRoom;
        public final float newX;
        public final float newY;
        
        public TransitionResult(RoomId targetRoom, float newX, float newY) {
            this.targetRoom = targetRoom;
            this.newX = newX;
            this.newY = newY;
        }
        
        public static final TransitionResult NONE = null;
    }
    
    public RoomTransitionSystem(float virtualWidth, float virtualHeight, 
                                 float doorWidth, float wallThickness, float entryOffset) {
        this.virtualWidth = virtualWidth;
        this.virtualHeight = virtualHeight;
        this.doorWidth = doorWidth;
        this.wallThickness = wallThickness;
        this.entryOffset = entryOffset;
    }
    
    /**
     * Update cooldown timer.
     */
    public void update(float delta) {
        if (transitionCooldown > 0) {
            transitionCooldown -= delta;
            if (transitionCooldown < 0) {
                transitionCooldown = 0;
            }
        }
    }
    
    /**
     * Check if player is at a door and should transition.
     * Also clamps player to walls if not at door.
     * 
     * @param player The player
     * @param currentRoomId Current room ID
     * @return TransitionResult with target room and new position, or null if no transition
     */
    public TransitionResult checkTransition(Player player, RoomId currentRoomId) {
        if (transitionCooldown > 0) {
            clampToWalls(player, currentRoomId);
            return null;
        }
        
        float cx = player.getCenterX();
        float cy = player.getCenterY();
        
        float doorMinX = virtualWidth / 2f - doorWidth / 2f;
        float doorMaxX = virtualWidth / 2f + doorWidth / 2f;
        float doorMinY = virtualHeight / 2f - doorWidth / 2f;
        float doorMaxY = virtualHeight / 2f + doorWidth / 2f;
        
        // UP
        if (player.getY() + player.getHeight() >= virtualHeight - wallThickness) {
            RoomId up = currentRoomId.up();
            if (up != null && cx >= doorMinX && cx <= doorMaxX) {
                transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                float newY = wallThickness + entryOffset;
                return new TransitionResult(up, player.getX(), newY);
            } else {
                player.setY(virtualHeight - player.getHeight() - wallThickness);
            }
        }
        
        // DOWN
        if (player.getY() <= wallThickness) {
            RoomId down = currentRoomId.down();
            if (down != null && cx >= doorMinX && cx <= doorMaxX) {
                transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                float newY = virtualHeight - wallThickness - player.getHeight() - entryOffset;
                return new TransitionResult(down, player.getX(), newY);
            } else {
                player.setY(wallThickness);
            }
        }
        
        // RIGHT
        if (player.getX() + player.getWidth() >= virtualWidth - wallThickness) {
            RoomId right = currentRoomId.right();
            if (right != null && cy >= doorMinY && cy <= doorMaxY) {
                transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                float newX = wallThickness + entryOffset;
                return new TransitionResult(right, newX, player.getY());
            } else {
                player.setX(virtualWidth - player.getWidth() - wallThickness);
            }
        }
        
        // LEFT
        if (player.getX() <= wallThickness) {
            RoomId left = currentRoomId.left();
            if (left != null && cy >= doorMinY && cy <= doorMaxY) {
                transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                float newX = virtualWidth - wallThickness - player.getWidth() - entryOffset;
                return new TransitionResult(left, newX, player.getY());
            } else {
                player.setX(wallThickness);
            }
        }
        
        return null;
    }
    
    /**
     * Clamp player position to room walls (respecting doors).
     */
    private void clampToWalls(Player player, RoomId currentRoomId) {
        float doorMinX = virtualWidth / 2f - doorWidth / 2f;
        float doorMaxX = virtualWidth / 2f + doorWidth / 2f;
        float doorMinY = virtualHeight / 2f - doorWidth / 2f;
        float doorMaxY = virtualHeight / 2f + doorWidth / 2f;
        
        float cx = player.getCenterX();
        float cy = player.getCenterY();
        
        // Clamp each direction
        if (player.getY() + player.getHeight() > virtualHeight - wallThickness) {
            RoomId up = currentRoomId.up();
            if (up == null || cx < doorMinX || cx > doorMaxX) {
                player.setY(virtualHeight - player.getHeight() - wallThickness);
            }
        }
        if (player.getY() < wallThickness) {
            RoomId down = currentRoomId.down();
            if (down == null || cx < doorMinX || cx > doorMaxX) {
                player.setY(wallThickness);
            }
        }
        if (player.getX() + player.getWidth() > virtualWidth - wallThickness) {
            RoomId right = currentRoomId.right();
            if (right == null || cy < doorMinY || cy > doorMaxY) {
                player.setX(virtualWidth - player.getWidth() - wallThickness);
            }
        }
        if (player.getX() < wallThickness) {
            RoomId left = currentRoomId.left();
            if (left == null || cy < doorMinY || cy > doorMaxY) {
                player.setX(wallThickness);
            }
        }
    }
    
    public boolean isInCooldown() {
        return transitionCooldown > 0;
    }
}
