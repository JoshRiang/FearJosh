package com.fearjosh.frontend.state.enemy;

import com.fearjosh.frontend.entity.Enemy;
import com.fearjosh.frontend.entity.Player;

/**
 * Stunned state - enemy is temporarily incapacitated.
 * Room parameter removed; not used since stunned enemy doesn't move.
 */
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
            // balik ke chasing (karena sudah dalam sight radius saat di-stun)
            enemy.changeState(enemy.getChasingState());
        }
        // musuh diam selama stun (tidak move)
    }

    public void reset(float newDuration) {
        this.timer = newDuration;
        this.duration = newDuration;
    }

    public float getTimeRemaining() {
        return timer;
    }
}
