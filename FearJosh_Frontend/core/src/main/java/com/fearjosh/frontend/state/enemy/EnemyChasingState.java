package com.fearjosh.frontend.state.enemy;

import com.fearjosh.frontend.entity.Enemy;
import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.world.Room;
import com.fearjosh.frontend.config.Constants;

public class EnemyChasingState implements EnemyState {

    private static final float COLLISION_DISTANCE = 30f;
    private static final float PATH_UPDATE_DISTANCE = 50f; // Recalculate if player moves this far

    @Override
    public void onEnter(Enemy enemy) {
        enemy.setCurrentStateType(EnemyStateType.CHASING);
        // Clear any existing path
        enemy.clearPath();
    }

    @Override
    public void update(Enemy enemy, Player player, Room currentRoom, float delta) {

        float dx = player.getCenterX() - enemy.getCenterX();
        float dy = player.getCenterY() - enemy.getCenterY();
        float len2 = dx*dx + dy*dy;

        if (len2 == 0) return;
        float len = (float)Math.sqrt(len2);

        // === USE A* PATHFINDING FOR INTELLIGENT NAVIGATION ===
        
        // Check if we need new path (no path OR target moved significantly)
        boolean needsNewPath = !enemy.hasPath();
        
        if (!needsNewPath) {
            float[] currentTarget = enemy.getPathTarget();
            float targetDx = player.getCenterX() - currentTarget[0];
            float targetDy = player.getCenterY() - currentTarget[1];
            float targetDist = (float)Math.sqrt(targetDx*targetDx + targetDy*targetDy);
            
            if (targetDist > PATH_UPDATE_DISTANCE) {
                needsNewPath = true;
            }
        }
        
        // Calculate new path if needed
        if (needsNewPath) {
            enemy.calculatePathTo(
                player.getCenterX(), 
                player.getCenterY(),
                currentRoom,
                Constants.VIRTUAL_WIDTH,
                Constants.VIRTUAL_HEIGHT
            );
        }
        
        // Follow path (will recalculate periodically)
        boolean moving = enemy.followPath(delta, currentRoom, 
            Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT);
        
        // If no path, fall back to direct movement (SMOOTH DIAGONAL)
        if (!moving) {
            // Normalize direction vector for unit vector
            dx /= len; // Now dx and dy form a normalized direction
            dy /= len;
            
            // Scale by speed and delta for smooth movement
            float speed = enemy.getChaseSpeed();
            float moveX = dx * speed * delta;
            float moveY = dy * speed * delta;
            
            // Apply BOTH X and Y together for true diagonal movement
            enemy.move(moveX, moveY, currentRoom);
        }
        
        // ======================================================

        // Check collision with player
        float collisionDist2 = COLLISION_DISTANCE * COLLISION_DISTANCE;
        if (len2 <= collisionDist2) {
            // [PLANNED] Collision detected - game over handled in PlayScreen.checkEnemyPlayerCollision()
        }

        // Lost sight of player - switch to searching
        if (len2 > Enemy.VISION_RADIUS * Enemy.VISION_RADIUS) {
            enemy.changeState(enemy.getSearchingState());
        }
    }
}
