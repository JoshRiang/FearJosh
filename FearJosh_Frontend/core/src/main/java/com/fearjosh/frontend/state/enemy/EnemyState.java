package com.fearjosh.frontend.state.enemy;

import com.fearjosh.frontend.entity.Enemy;
import com.fearjosh.frontend.entity.Player;

public interface EnemyState {

    default void onEnter(Enemy enemy) {}

    default void onExit(Enemy enemy) {}

    void update(Enemy enemy, Player player, float delta);
}
