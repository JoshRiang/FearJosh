package com.fearjosh.frontend.state.enemy;

import com.fearjosh.frontend.entity.Enemy;
import com.fearjosh.frontend.entity.Player;

public class EnemyStunnedState implements EnemyState {

    private float timer;
    private float duration;

    public EnemyStunnedState(float duration) {
        this.duration = duration;
        this.timer = duration;
    }

    @Override
    public void onEnter(Enemy enemy) {
        enemy.setCurrentStateType(EnemyStateType.STUNNED);
        this.timer = this.duration;
    }

    @Override
    public void update(Enemy enemy, Player player, float delta) {
        timer -= delta;
        if (timer <= 0f) {
            enemy.changeState(enemy.getChasingState());
        }
    }

    public void reset(float newDuration) {
        this.timer = newDuration;
        this.duration = newDuration;
    }

    public float getTimeRemaining() {
        return timer;
    }
}
