package com.fearjosh.frontend.config;

/**
 * Game-wide constants and configuration values.
 * Centralized location for all magic numbers used across the game.
 */
public final class Constants {
    
    private Constants() {} // Prevent instantiation
    
    // ==================== DISPLAY ====================
    
    /** Virtual screen width (camera resolution) */
    public static final float VIRTUAL_WIDTH = 800f;
    
    /** Virtual screen height (camera resolution) */
    public static final float VIRTUAL_HEIGHT = 600f;
    
    /** Camera zoom level (<1 = zoom in, >1 = zoom out) */
    public static final float CAMERA_ZOOM = 0.7f;
    
    // ==================== WORLD GEOMETRY ====================
    
    /** Standard door opening width */
    public static final float DOOR_WIDTH = 80f;
    
    /** Door zone thickness for transition */
    public static final float DOOR_THICKNESS = 20f;
    
    /** Wall boundary thickness */
    public static final float WALL_THICKNESS = 6f;
    
    /** Minimum margin from wall for furniture placement */
    public static final float WALL_MARGIN = 40f;
    
    /** Offset when player enters new room */
    public static final float ENTRY_OFFSET = 20f;
    
    // ==================== PLAYER ====================
    
    /** Default player width */
    public static final float PLAYER_WIDTH = 64f;
    
    /** Default player height */
    public static final float PLAYER_HEIGHT = 64f;
    
    /** Normal walking speed */
    public static final float PLAYER_WALK_SPEED = 120f;
    
    /** Sprint speed multiplier */
    public static final float PLAYER_SPRINT_MULTIPLIER = 1.8f;
    
    /** Maximum stamina value */
    public static final float MAX_STAMINA = 100f;
    
    /** Stamina regeneration rate per second (in NormalState) */
    public static final float STAMINA_REGEN_RATE = 15f;
    
    /** Stamina drain rate per second (in SprintingState) */
    public static final float STAMINA_DRAIN_RATE = 25f;
    
    // ==================== FLASHLIGHT ====================
    
    /** Maximum battery percentage */
    public static final float MAX_BATTERY = 100f;
    
    /** Base battery drain rate per second when flashlight is on */
    public static final float BATTERY_DRAIN_RATE = 3f;
    
    /** Battery pickup restoration percentage */
    public static final float BATTERY_PICKUP_AMOUNT = 0.25f;
    
    // ==================== ENEMY ====================
    
    /** Enemy base chase speed */
    public static final float ENEMY_CHASE_SPEED = 90f;
    
    /** Enemy stun duration when flashlight hits */
    public static final float ENEMY_STUN_DURATION = 3f;
    
    // ==================== INTERACTION ====================
    
    /** Maximum distance for player-interactable interaction */
    public static final float INTERACT_RANGE = 60f;
    
    // ==================== FURNITURE DIMENSIONS ====================
    
    /** Standard locker width */
    public static final float LOCKER_WIDTH = 40f;
    
    /** Standard locker height */
    public static final float LOCKER_HEIGHT = 80f;
    
    /** Standard table width */
    public static final float TABLE_WIDTH = 75f;
    
    /** Standard table height */
    public static final float TABLE_HEIGHT = 45f;
    
    // ==================== TIMING ====================
    
    /** Room transition cooldown duration */
    public static final float TRANSITION_COOLDOWN = 0.2f;
    
    /** Floor tile size for rendering */
    public static final int FLOOR_TILE_SIZE = 64;
    
    // ==================== DEBUG ====================
    
    /** Enable/disable hitbox debug rendering */
    public static final boolean DEBUG_HITBOX = false;
    
    /** Enable/disable collision debug rendering */
    public static final boolean DEBUG_COLLISION = false;
}
