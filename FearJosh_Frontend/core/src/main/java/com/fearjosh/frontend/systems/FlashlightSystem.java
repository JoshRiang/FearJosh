package com.fearjosh.frontend.systems;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.core.GameManager;

public class FlashlightSystem {
    
    private float battery;
    private final float maxBattery;
    private final float baseDrainRate;
    
    public FlashlightSystem(float maxBattery, float baseDrainRate) {
        this.maxBattery = maxBattery;
        this.battery = maxBattery;
        this.baseDrainRate = baseDrainRate;
    }
    
    public void update(Player player, float delta) {
        if (player.isFlashlightOn()) {
            float drainMultiplier = GameManager.getInstance()
                    .getDifficultyStrategy()
                    .batteryDrainMultiplier();
            
            battery -= baseDrainRate * drainMultiplier * delta;
            
            if (battery < 0f) {
                battery = 0f;
            }
            
            if (battery <= 0f && player.isFlashlightOn()) {
                player.setFlashlightOn(false);
            }
        }
    }
    
    public void addBattery(float amount) {
        battery += maxBattery * amount;
        if (battery > maxBattery) {
            battery = maxBattery;
        }
    }
    
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
