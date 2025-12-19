package com.fearjosh.frontend.state.enemy;

import com.fearjosh.frontend.entity.Enemy;
import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.world.Room;

public class EnemySearchingState implements EnemyState {

    private static final float WANDER_SPEED = 40f;
    private static final float WANDER_CHANGE_TIME = 3f;

    private float wanderTimer = 0f;
    private float wanderDx = 0f;
    private float wanderDy = 0f;
    private java.util.Random rng = new java.util.Random();

    @Override
    public void onEnter(Enemy enemy) {
        enemy.setCurrentStateType(EnemyStateType.SEARCHING);
        wanderTimer = 0f;
        generateNewWanderDirection();
    }

    @Override
    public void update(Enemy enemy, Player player, Room currentRoom, float delta) {
        wanderTimer += delta;
        if (wanderTimer >= WANDER_CHANGE_TIME) {
            wanderTimer = 0f;
            generateNewWanderDirection();
        }

        float moveX = wanderDx * WANDER_SPEED * delta;
        float moveY = wanderDy * WANDER_SPEED * delta;
        enemy.move(moveX, moveY, currentRoom);

        float dx = player.getCenterX() - enemy.getCenterX();
        float dy = player.getCenterY() - enemy.getCenterY();
        float dist2 = dx*dx + dy*dy;

        if (dist2 <= Enemy.DETECTION_RADIUS * Enemy.DETECTION_RADIUS) {
            enemy.changeState(enemy.getChasingState());
        }
    }

    private void generateNewWanderDirection() {
        double angle = rng.nextDouble() * 2 * Math.PI;
        wanderDx = (float) Math.cos(angle);
        wanderDy = (float) Math.sin(angle);
    }
}
