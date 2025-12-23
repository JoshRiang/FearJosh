package com.fearjosh.frontend.state.enemy;

import com.fearjosh.frontend.entity.Enemy;
import com.fearjosh.frontend.entity.Player;

/**
 * State interface for Enemy behavior (State Pattern).
 * Room parameter removed - collision/pathing now uses TMX via TiledMapManager.
 */
public interface EnemyState {

    // dipanggil sekali saat state ini dipasang
    default void onEnter(Enemy enemy) {}

    // dipanggil sekali saat state ini dilepas
    default void onExit(Enemy enemy) {}

    // dipanggil tiap frame (TMX-based collision via Enemy's TiledMapManager)
    void update(Enemy enemy, Player player, float delta);
}
