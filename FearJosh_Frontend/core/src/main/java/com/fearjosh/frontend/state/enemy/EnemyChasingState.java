package com.fearjosh.frontend.state.enemy;

import com.fearjosh.frontend.entity.Enemy;
import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.config.Constants;

public class EnemyChasingState implements EnemyState {

    private static final float COLLISION_DISTANCE = 30f;
    private static final float PATH_UPDATE_DISTANCE = 50f;

    @Override
    public void onEnter(Enemy enemy) {
        enemy.setCurrentStateType(EnemyStateType.CHASING);
        enemy.clearPath();
    }

    @Override
    public void update(Enemy enemy, Player player, float delta) {

        float dx = player.getCenterX() - enemy.getCenterX();
        float dy = player.getCenterY() - enemy.getCenterY();
        float len2 = dx*dx + dy*dy;

        if (len2 == 0) return;
        float len = (float)Math.sqrt(len2);

        // PATHFINDING
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
        
        if (needsNewPath) {
            enemy.calculatePathTo(
                player.getCenterX(), 
                player.getCenterY(),
                Constants.VIRTUAL_WIDTH,
                Constants.VIRTUAL_HEIGHT
            );
        }
        
        boolean moving = enemy.followPath(delta, 
            Constants.VIRTUAL_WIDTH, Constants.VIRTUAL_HEIGHT);
        
        if (!moving) {
            dx /= len;
            dy /= len;
            
            float speed = enemy.getChaseSpeed();
            float moveX = dx * speed * delta;
            float moveY = dy * speed * delta;
            
            enemy.move(moveX, moveY);
        }

        float collisionDist2 = COLLISION_DISTANCE * COLLISION_DISTANCE;
        if (len2 <= collisionDist2) {
        }

        if (len2 > Enemy.VISION_RADIUS * Enemy.VISION_RADIUS) {
            enemy.changeState(enemy.getSearchingState());
        }
    }
}
