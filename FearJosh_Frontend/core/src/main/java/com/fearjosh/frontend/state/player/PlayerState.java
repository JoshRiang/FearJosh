package com.fearjosh.frontend.state.player;

import com.fearjosh.frontend.entity.Player;

/**
 * Interface untuk Player State Pattern.
 * Mengontrol behavior player berdasarkan state (Normal, Sprinting, dll)
 */
public interface PlayerState {
    
    /**
     * Dipanggil saat masuk ke state ini
     */
    void enter(Player player);
    
    /**
     * Update logic state setiap frame
     * @return State berikutnya (bisa return this jika tidak berubah)
     */
    PlayerState update(Player player, float delta);
    
    /**
     * Dipanggil saat keluar dari state ini
     */
    void exit(Player player);
    
    /**
     * Speed multiplier untuk movement
     * 1.0 = normal, >1.0 = faster (sprint)
     */
    float getSpeedMultiplier();
    
    /**
     * Apakah state ini bisa transition ke sprint
     */
    boolean canSprint();
    
    /**
     * Nama state untuk debugging
     */
    String getName();
}
