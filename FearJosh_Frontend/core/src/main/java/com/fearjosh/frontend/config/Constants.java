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
    
    /** Clearance width around door (adds margin on both sides) */
    public static final float DOOR_CLEARANCE_WIDTH = DOOR_WIDTH + 2 * 30f; // 80 + 60 = 140px
    
    /** Clearance depth into room from door (prevents furniture blocking entry) */
    public static final float DOOR_CLEARANCE_DEPTH = 120f; // Locker height (80) + margin (40)
    
    /** Offset when player enters new room */
    public static final float ENTRY_OFFSET = 20f;
    
    // ==================== PLAYER ====================
    
    // --- RENDER SIZE (Visual only - can be adjusted for appearance) ---
    /** Player render width (visual) - adjust this to make player thinner/wider */
    public static final float PLAYER_RENDER_WIDTH = 38f;  // Slim proportions
    
    /** Player render height (visual) - adjust this for taller/shorter appearance */
    public static final float PLAYER_RENDER_HEIGHT = 64f;  // Proportional to width
    
    // --- COLLISION HITBOX (Foot collision with furniture) ---
    /** Player collision width (foot hitbox) - affects furniture collision */
    public static final float PLAYER_COLLISION_WIDTH = 25f;  // ~66% of render width
    
    /** Player collision height (foot hitbox) - affects furniture collision */
    public static final float PLAYER_COLLISION_HEIGHT = 16f;  // 25% of render height
    
    /** Y-offset for foot hitbox from player bottom */
    public static final float PLAYER_COLLISION_OFFSET_Y = 0f;  // At bottom
    
    // --- ENEMY HITBOX (Full body collision with enemy) ---
    /** Player enemy hitbox width (full body) - affects enemy collision */
    public static final float PLAYER_ENEMY_HITBOX_WIDTH = 38f;  // Match render width
    
    /** Player enemy hitbox height (full body) - affects enemy collision */
    public static final float PLAYER_ENEMY_HITBOX_HEIGHT = 64f;  // Match render height
    
    // --- LEGACY (for backward compatibility) ---
    /** @deprecated Use PLAYER_RENDER_WIDTH instead */
    @Deprecated
    public static final float PLAYER_WIDTH = PLAYER_RENDER_WIDTH;
    
    /** @deprecated Use PLAYER_RENDER_HEIGHT instead */
    @Deprecated
    public static final float PLAYER_HEIGHT = PLAYER_RENDER_HEIGHT;
    
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
    
    // ==================== KEY ITEMS (World Render) ====================
    
    /** Key sprite render size (small, consistent) */
    public static final float KEY_RENDER_SIZE = 18f;
    
    /** Key pickup radius (how close player needs to be) */
    public static final float KEY_PICKUP_RADIUS = 28f;
    
    /** Key bob animation amplitude (up/down motion) */
    public static final float KEY_BOB_AMPLITUDE = 3f;
    
    /** Key bob animation speed */
    public static final float KEY_BOB_SPEED = 2.5f;
    
    // ==================== DEBUG ====================
    
    /** Enable/disable hitbox debug rendering */
    public static final boolean DEBUG_HITBOX = false;
    
    /** Enable/disable collision debug rendering */
    public static final boolean DEBUG_COLLISION = false;
    
    /** Enable/disable RoomDirector debug logging */
    public static final boolean DEBUG_ROOM_DIRECTOR = false;
}
