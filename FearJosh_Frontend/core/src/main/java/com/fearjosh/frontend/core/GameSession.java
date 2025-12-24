package com.fearjosh.frontend.core;

import com.fearjosh.frontend.difficulty.GameDifficulty;
import com.fearjosh.frontend.world.RoomId;

public class GameSession {

    // META
    private final long sessionId;
    private final long startTimeMs;
    private boolean active;

    // PROGRESS
    private RoomId currentRoomId;
    private float playerX;
    private float playerY;

    // PLAYER STATE
    private float stamina;
    private float battery;
    private int batterySegments;
    private boolean flashlightOn;

    private final GameDifficulty difficulty;

    public GameSession(GameDifficulty difficulty, RoomId startingRoom, float playerX, float playerY) {
        this.sessionId = System.currentTimeMillis();
        this.startTimeMs = System.currentTimeMillis();
        this.active = true;
        
        this.difficulty = difficulty;
        this.currentRoomId = startingRoom;
        this.playerX = playerX;
        this.playerY = playerY;

        // DEFAULTS
        this.stamina = 100f;
        this.battery = 1f;
        this.batterySegments = 0;
        this.flashlightOn = false;
    }

    public long getSessionId() {
        return sessionId;
    }
    
    public long getStartTimeMs() {
        return startTimeMs;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }

    public void endSession() {
        this.active = false;
        System.out.println("[GameSession] Session " + sessionId + " ended");
    }
    
    public RoomId getCurrentRoomId() {
        return currentRoomId;
    }
    
    public void setCurrentRoomId(RoomId currentRoomId) {
        this.currentRoomId = currentRoomId;
    }
    
    public float getPlayerX() {
        return playerX;
    }
    
    public void setPlayerX(float playerX) {
        this.playerX = playerX;
    }
    
    public float getPlayerY() {
        return playerY;
    }
    
    public void setPlayerY(float playerY) {
        this.playerY = playerY;
    }
    
    public float getStamina() {
        return stamina;
    }
    
    public void setStamina(float stamina) {
        this.stamina = stamina;
    }
    
    public float getBattery() {
        return battery;
    }
    
    public void setBattery(float battery) {
        this.battery = battery;
    }
    
    public int getBatterySegments() {
        return batterySegments;
    }
    
    public void setBatterySegments(int batterySegments) {
        this.batterySegments = batterySegments;
    }
    
    public boolean isFlashlightOn() {
        return flashlightOn;
    }
    
    public void setFlashlightOn(boolean flashlightOn) {
        this.flashlightOn = flashlightOn;
    }
    
    public GameDifficulty getDifficulty() {
        return difficulty;
    }

    // UPDATE
    public void updateFromPlayer(com.fearjosh.frontend.entity.Player player, RoomId roomId) {
        this.playerX = player.getX();
        this.playerY = player.getY();
        this.stamina = player.getStamina();
        this.flashlightOn = player.isFlashlightOn();
        this.currentRoomId = roomId;
    }

    public void restoreToPlayer(com.fearjosh.frontend.entity.Player player) {
        player.setX(this.playerX);
        player.setY(this.playerY);
        player.setStamina(this.stamina);
        player.setFlashlightOn(this.flashlightOn);
    }
    
    @Override
    public String toString() {
        return String.format("GameSession[id=%d, room=%s, difficulty=%s, active=%b]",
                sessionId, currentRoomId, difficulty, active);
    }
}
