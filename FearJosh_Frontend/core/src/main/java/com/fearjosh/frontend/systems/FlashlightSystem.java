package com.fearjosh.frontend.systems;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.core.GameManager;

/**
 * FlashlightSystem - Manages flashlight state and battery consumption.
 * 
 * Responsibilities:
 * - Update battery drain when flashlight is on
 * - Apply difficulty multiplier for drain rate
 * - Track flashlight active state
 */
public class FlashlightSystem {
    
    private float battery;
    private final float maxBattery;
    private final float baseDrainRate;
    
    public FlashlightSystem(float maxBattery, float baseDrainRate) {
        this.maxBattery = maxBattery;
        this.battery = maxBattery;
        this.baseDrainRate = baseDrainRate;
    }
    
    /**
     * Update battery consumption.
     * 
     * @param player Player to check flashlight state
     * @param delta Delta time
     */
    public void update(Player player, float delta) {
        if (player.isFlashlightOn()) {
            float drainMultiplier = GameManager.getInstance()
                    .getDifficultyStrategy()
                    .batteryDrainMultiplier();
            
            battery -= baseDrainRate * drainMultiplier * delta;
            
            if (battery < 0f) {
                battery = 0f;
            }
            
            // Auto turn off flashlight if battery is depleted
            if (battery <= 0f && player.isFlashlightOn()) {
                player.setFlashlightOn(false);
            }
        }
    }
    
    /**
     * Add battery (from pickup).
     * 
     * @param amount Percentage to add (0.0 - 1.0)
     */
    public void addBattery(float amount) {
        battery += maxBattery * amount;
        if (battery > maxBattery) {
            battery = maxBattery;
        }
    }
    
    /**
     * Set battery directly.
     * 
     * @param value New battery value
     */
    public void setBattery(float value) {
        battery = Math.max(0f, Math.min(value, maxBattery));
    }
    
    public float getBattery() {
        return battery;
    }
    
    public float getMaxBattery() {
        return maxBattery;
    }
    
    public float getBatteryPercentage() {
        return battery / maxBattery;
    }
    
    public boolean isDepleted() {
        return battery <= 0f;
    }
}
