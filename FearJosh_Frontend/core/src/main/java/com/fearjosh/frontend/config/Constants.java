package com.fearjosh.frontend.config;

public final class Constants {
    
    private Constants() {}
    
    // DISPLAY
    public static final float VIRTUAL_WIDTH = 800f;
    public static final float VIRTUAL_HEIGHT = 600f;
    public static final float CAMERA_ZOOM = 0.7f;
    
    // WORLD GEOMETRY
    public static final float DOOR_WIDTH = 80f;
    public static final float DOOR_THICKNESS = 20f;
    public static final float WALL_THICKNESS = 6f;
    public static final float WALL_MARGIN = 40f;
    public static final float DOOR_CLEARANCE_WIDTH = DOOR_WIDTH + 2 * 30f;
    public static final float DOOR_CLEARANCE_DEPTH = 120f;
    public static final float ENTRY_OFFSET = 20f;
    
    // PLAYER
    public static final float PLAYER_RENDER_WIDTH = 38f;
    public static final float PLAYER_RENDER_HEIGHT = 64f;
    public static final float PLAYER_COLLISION_WIDTH = 25f;
    public static final float PLAYER_COLLISION_HEIGHT = 16f;
    public static final float PLAYER_COLLISION_OFFSET_Y = 0f;
    public static final float PLAYER_ENEMY_HITBOX_WIDTH = 38f;
    public static final float PLAYER_ENEMY_HITBOX_HEIGHT = 64f;
    @Deprecated
    public static final float PLAYER_WIDTH = PLAYER_RENDER_WIDTH;
    @Deprecated
    public static final float PLAYER_HEIGHT = PLAYER_RENDER_HEIGHT;
    public static final float PLAYER_WALK_SPEED = 120f;
    public static final float PLAYER_SPRINT_MULTIPLIER = 1.8f;
    public static final float MAX_STAMINA = 100f;
    public static final float STAMINA_REGEN_RATE = 15f;
    public static final float STAMINA_DRAIN_RATE = 25f;
    
    // FLASHLIGHT
    public static final float MAX_BATTERY = 100f;
    public static final float BATTERY_DRAIN_RATE = 3f;
    public static final float BATTERY_PICKUP_AMOUNT = 0.25f;
    
    // ENEMY
    public static final float ENEMY_CHASE_SPEED = 90f;
    public static final float ENEMY_STUN_DURATION = 3f;
    
    // INTERACTION
    public static final float INTERACT_RANGE = 60f;
    
    // FURNITURE
    public static final float LOCKER_WIDTH = 40f;
    public static final float LOCKER_HEIGHT = 80f;
    public static final float TABLE_WIDTH = 75f;
    public static final float TABLE_HEIGHT = 45f;
    
    // TIMING
    public static final float TRANSITION_COOLDOWN = 0.2f;
    public static final int FLOOR_TILE_SIZE = 64;
    
    // KEY ITEMS
    public static final float KEY_RENDER_SIZE = 18f;
    public static final float KEY_PICKUP_RADIUS = 28f;
    public static final float KEY_BOB_AMPLITUDE = 3f;
    public static final float KEY_BOB_SPEED = 2.5f;
    
    // DEBUG
    public static final boolean DEBUG_HITBOX = false;
    public static final boolean DEBUG_COLLISION = false;
    public static final boolean DEBUG_ROOM_DIRECTOR = false;
}
