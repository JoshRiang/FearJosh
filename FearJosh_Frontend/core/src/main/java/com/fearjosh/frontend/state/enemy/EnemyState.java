package com.fearjosh.frontend.state.enemy;

import com.fearjosh.frontend.entity.Enemy;
import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.world.Room;

public interface EnemyState {

    // dipanggil sekali saat state ini dipasang
    default void onEnter(Enemy enemy) {}

    // dipanggil sekali saat state ini dilepas
    default void onExit(Enemy enemy) {}

    // dipanggil tiap frame
    void update(Enemy enemy, Player player, Room currentRoom, float delta);
}
