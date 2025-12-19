package com.fearjosh.frontend.core;

import com.fearjosh.frontend.difficulty.GameDifficulty;
import com.fearjosh.frontend.world.RoomId;

/**
 * Represents an active game session/run with all progress data.
 * This is kept in memory during gameplay and can be saved/loaded.
 * 
 * TERMINOLOGY:
 * - "Session" = one playthrough from New Game until death/completion
 * - "Active Run" = session currently in progress (can be resumed)
 * - "No Run" = no active session (Resume should be disabled)
 */
public class GameSession {
    
    // Session metadata
    private final long sessionId;           // Unique ID for this run
    private final long startTimeMs;         // When session was created
    private boolean active;                 // Is this run still active?
    
    // Game progress
    private RoomId currentRoomId;           // Which room player is in
    private float playerX;                  // Player X position
    private float playerY;                  // Player Y position
    
    // Player state
    private float stamina;                  // Current stamina value
    private float battery;                  // Current flashlight battery
    private int batterySegments;            // Number of battery pickups collected
    private boolean flashlightOn;           // Is flashlight currently on?
    
    // Difficulty (locked once session starts)
    private final GameDifficulty difficulty;
    
    // Additional progress (expandable)
    // private int itemsCollected;
    // private Set<String> unlockedRooms;
    // private float playTimeSeconds;
    
    /**
     * Create a new game session
     */
    public GameSession(GameDifficulty difficulty, RoomId startingRoom, float playerX, float playerY) {
        this.sessionId = System.currentTimeMillis();
        this.startTimeMs = System.currentTimeMillis();
        this.active = true;
        
        this.difficulty = difficulty;
        this.currentRoomId = startingRoom;
        this.playerX = playerX;
        this.playerY = playerY;
        
        // Default starting values
        this.stamina = 100f;
        this.battery = 1f;
        this.batterySegments = 0;
        this.flashlightOn = false;
    }
    
    // ==================== GETTERS & SETTERS ====================
    
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
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Update session with current player state
     */
    public void updateFromPlayer(com.fearjosh.frontend.entity.Player player, RoomId roomId) {
        this.playerX = player.getX();
        this.playerY = player.getY();
        this.stamina = player.getStamina();
        this.flashlightOn = player.isFlashlightOn();
        this.currentRoomId = roomId;
    }
    
    /**
     * Restore player state from session
     */
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
