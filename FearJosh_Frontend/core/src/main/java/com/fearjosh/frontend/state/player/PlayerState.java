package com.fearjosh.frontend.state.player;

import com.fearjosh.frontend.entity.Player;

public interface PlayerState {
    
    void enter(Player player);
    
    PlayerState update(Player player, float delta);
    
    void exit(Player player);
    
    float getSpeedMultiplier();
    
    boolean canSprint();
    
    String getName();
}
