package com.fearjosh.frontend.state.enemy;

import com.fearjosh.frontend.entity.Enemy;
import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.world.Room;

public class EnemyChasingState implements EnemyState {

    private static final float COLLISION_DISTANCE = 30f;

    @Override
    public void onEnter(Enemy enemy) {
        enemy.setCurrentStateType(EnemyStateType.CHASING);
    }

    @Override
    public void update(Enemy enemy, Player player, Room currentRoom, float delta) {

        float dx = player.getCenterX() - enemy.getCenterX();
        float dy = player.getCenterY() - enemy.getCenterY();
        float len2 = dx*dx + dy*dy;

        if (len2 == 0) return;

        float len = (float)Math.sqrt(len2);
        dx /= len;
        dy /= len;

        float speed = enemy.getChaseSpeed();
        float moveX = dx * speed * delta;
        float moveY = dy * speed * delta;

        enemy.move(moveX, moveY, currentRoom);

        float collisionDist2 = COLLISION_DISTANCE * COLLISION_DISTANCE;
        if (len2 <= collisionDist2) {
            // TODO: Trigger game over / player death
        }

        if (len2 > Enemy.VISION_RADIUS * Enemy.VISION_RADIUS) {
            enemy.changeState(enemy.getSearchingState());
        }
    }
}
