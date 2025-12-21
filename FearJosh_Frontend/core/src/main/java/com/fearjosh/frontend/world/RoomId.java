package com.fearjosh.frontend.world;

import java.util.HashMap;
import java.util.Map;

/**
 * Room layout matching the School Floor Plan Template.
 * 
 * SIMPLIFIED LAYOUT:
 * 
 *                              ┌─────────────────┐
 *                              │       GYM       │ (large court area)
 *                              └────────┬────────┘
 *                                       │
 *    ┌─────────┐   ┌───────────────────────────────────────────────────────────────┐
 *    │         │   │ CLASS  CLASS  CLASS  CLASS │ CLASS  CLASS  CLASS  CLASS       │
 *    │ PARKING │   │  1A     2A     3A     4A   │  5A     6A     7A     8A         │ (top row)
 *    │         │   ├───────────────────────────────────────────────────────────────┤
 *    │         ├───┤                      HALLWAY                                  │
 *    │         │   ├───────────────────────────────────────────────────────────────┤
 *    │         │   │ CLASS  CLASS  CLASS  CLASS │ CLASS  CLASS  CLASS  CLASS       │
 *    └─────────┘   │  1B     2B     3B     4B   │  5B     6B     7B     8B         │ (bottom row)
 *        ↑         └───────────────────────────────────────────────────────────────┘
 *      LOBBY
 *    (entrance)
 */
public enum RoomId {
    // Entrance & Parking
    PARKING("Parking Lot", 800, 700),
    LOBBY("Main Lobby", 1440, 1440),
    
    // Main hallway - long horizontal corridor (1920x960 to match TMX map)
    HALLWAY("Main Hallway", 1920, 960),
    
    // Gym - large court at top (1440x1440 to match TMX map: 30x30 tiles * 48px)
    GYM("Gymnasium", 1440, 1440),
    
    // Special rooms
    JANITOR("Janitor Room", 400, 300),
    RESTROOM("Restroom", 400, 350),
    TEACHERS_ROOM("Teachers Room", 600, 400),
    
    // Top row classrooms (1A-8A) - above hallway (1440x1440 to match TMX: 30x30 tiles * 48px)
    CLASS_1A("Classroom 1A", 1440, 1440),
    CLASS_2A("Classroom 2A", 1440, 1440),
    CLASS_3A("Classroom 3A", 500, 450),
    CLASS_4A("Classroom 4A", 500, 450),
    CLASS_5A("Classroom 5A", 500, 450),
    CLASS_6A("Classroom 6A", 500, 450),
    CLASS_7A("Classroom 7A", 500, 450),
    CLASS_8A("Classroom 8A", 500, 450),
    
    // Bottom row classrooms (1B-8B) - below hallway
    CLASS_1B("Classroom 1B", 500, 450),
    CLASS_2B("Classroom 2B", 500, 450),
    CLASS_3B("Classroom 3B", 500, 450),
    CLASS_4B("Classroom 4B", 500, 450),
    CLASS_5B("Classroom 5B", 500, 450),
    CLASS_6B("Classroom 6B", 500, 450),
    CLASS_7B("Classroom 7B", 500, 450),
    CLASS_8B("Classroom 8B", 500, 450);

    private final String displayName;
    private final float width;
    private final float height;
    
    // Door connections - initialized in static block
    private static final Map<RoomId, Map<DoorPosition, RoomId>> doorConnections = new HashMap<>();

    RoomId(String displayName, float width, float height) {
        this.displayName = displayName;
        this.width = width;
        this.height = height;
    }
    
    /**
     * Door positions on a room's walls
     */
    public enum DoorPosition {
        TOP, BOTTOM, LEFT, RIGHT,
        // For rooms with multiple doors on same wall (hallway)
        TOP_1, TOP_2, TOP_3, TOP_4, TOP_5, TOP_6, TOP_7, TOP_8,
        BOTTOM_1, BOTTOM_2, BOTTOM_3, BOTTOM_4, BOTTOM_5, BOTTOM_6, BOTTOM_7, BOTTOM_8
    }
    
    // Static initializer to set up all door connections
    static {
        // PARKING - connects to LOBBY
        addDoor(PARKING, DoorPosition.BOTTOM, LOBBY);
        
        // LOBBY - connects to PARKING and HALLWAY
        addDoor(LOBBY, DoorPosition.TOP, PARKING);
        addDoor(LOBBY, DoorPosition.RIGHT, HALLWAY);
        
        // HALLWAY - main corridor connecting everything
        addDoor(HALLWAY, DoorPosition.LEFT, LOBBY);
        addDoor(HALLWAY, DoorPosition.TOP, GYM);  // GYM entrance in center-top
        
        // Top row classrooms connect to HALLWAY
        addDoor(HALLWAY, DoorPosition.TOP_1, CLASS_1A);
        addDoor(HALLWAY, DoorPosition.TOP_2, CLASS_2A);
        addDoor(HALLWAY, DoorPosition.TOP_3, CLASS_3A);
        addDoor(HALLWAY, DoorPosition.TOP_4, CLASS_4A);
        addDoor(HALLWAY, DoorPosition.TOP_5, CLASS_5A);
        addDoor(HALLWAY, DoorPosition.TOP_6, CLASS_6A);
        addDoor(HALLWAY, DoorPosition.TOP_7, CLASS_7A);
        addDoor(HALLWAY, DoorPosition.TOP_8, CLASS_8A);
        
        // Bottom row classrooms connect to HALLWAY
        addDoor(HALLWAY, DoorPosition.BOTTOM_1, CLASS_1B);
        addDoor(HALLWAY, DoorPosition.BOTTOM_2, CLASS_2B);
        addDoor(HALLWAY, DoorPosition.BOTTOM_3, CLASS_3B);
        addDoor(HALLWAY, DoorPosition.BOTTOM_4, CLASS_4B);
        addDoor(HALLWAY, DoorPosition.BOTTOM_5, CLASS_5B);
        addDoor(HALLWAY, DoorPosition.BOTTOM_6, CLASS_6B);
        addDoor(HALLWAY, DoorPosition.BOTTOM_7, CLASS_7B);
        addDoor(HALLWAY, DoorPosition.BOTTOM_8, CLASS_8B);
        
        // GYM - connects back to HALLWAY
        addDoor(GYM, DoorPosition.BOTTOM, HALLWAY);
        
        // Top row classrooms - each connects back to HALLWAY
        addDoor(CLASS_1A, DoorPosition.BOTTOM, HALLWAY);
        addDoor(CLASS_2A, DoorPosition.BOTTOM, HALLWAY);
        addDoor(CLASS_3A, DoorPosition.BOTTOM, HALLWAY);
        addDoor(CLASS_4A, DoorPosition.BOTTOM, HALLWAY);
        addDoor(CLASS_5A, DoorPosition.BOTTOM, HALLWAY);
        addDoor(CLASS_6A, DoorPosition.BOTTOM, HALLWAY);
        addDoor(CLASS_7A, DoorPosition.BOTTOM, HALLWAY);
        addDoor(CLASS_8A, DoorPosition.BOTTOM, HALLWAY);
        
        // Bottom row classrooms - each connects back to HALLWAY
        addDoor(CLASS_1B, DoorPosition.TOP, HALLWAY);
        addDoor(CLASS_2B, DoorPosition.TOP, HALLWAY);
        addDoor(CLASS_3B, DoorPosition.TOP, HALLWAY);
        addDoor(CLASS_4B, DoorPosition.TOP, HALLWAY);
        addDoor(CLASS_5B, DoorPosition.TOP, HALLWAY);
        addDoor(CLASS_6B, DoorPosition.TOP, HALLWAY);
        addDoor(CLASS_7B, DoorPosition.TOP, HALLWAY);
        addDoor(CLASS_8B, DoorPosition.TOP, HALLWAY);
    }
    
    private static void addDoor(RoomId room, DoorPosition position, RoomId destination) {
        doorConnections.computeIfAbsent(room, k -> new HashMap<>()).put(position, destination);
    }

    public String getDisplayName() {
        return displayName;
    }
    
    public float getWidth() {
        return width;
    }
    
    public float getHeight() {
        return height;
    }
    
    /**
     * Get room connected via a door at the given position
     */
    public RoomId getDoorConnection(DoorPosition position) {
        Map<DoorPosition, RoomId> doors = doorConnections.get(this);
        return doors != null ? doors.get(position) : null;
    }
    
    /**
     * Check if this room has a door at the given position
     */
    public boolean hasDoorAt(DoorPosition position) {
        return getDoorConnection(position) != null;
    }
    
    /**
     * Get all door connections for this room
     */
    public Map<DoorPosition, RoomId> getAllDoors() {
        return doorConnections.getOrDefault(this, new HashMap<>());
    }

    // Legacy methods for backward compatibility with PlayScreen
    // These check for any door on the given wall
    
    public RoomId up() {
        // Check TOP door first, then numbered TOP doors
        RoomId result = getDoorConnection(DoorPosition.TOP);
        if (result != null) return result;
        // Check numbered top doors
        for (DoorPosition pos : DoorPosition.values()) {
            if (pos.name().startsWith("TOP_")) {
                result = getDoorConnection(pos);
                if (result != null) return result;
            }
        }
        return null;
    }
    
    public RoomId down() {
        RoomId result = getDoorConnection(DoorPosition.BOTTOM);
        if (result != null) return result;
        for (DoorPosition pos : DoorPosition.values()) {
            if (pos.name().startsWith("BOTTOM_")) {
                result = getDoorConnection(pos);
                if (result != null) return result;
            }
        }
        return null;
    }
    
    public RoomId left() {
        return getDoorConnection(DoorPosition.LEFT);
    }
    
    public RoomId right() {
        return getDoorConnection(DoorPosition.RIGHT);
    }

    public boolean hasUp() {
        return up() != null;
    }

    public boolean hasDown() {
        return down() != null;
    }

    public boolean hasLeft() {
        return left() != null;
    }

    public boolean hasRight() {
        return right() != null;
    }
    
    /**
     * Get the starting room (Entrance)
     */
    public static RoomId getStartingRoom() {
        return LOBBY;
    }
    
    // Room type checks
    
    public boolean isHallway() {
        return this == HALLWAY;
    }
    
    public boolean isClassroom() {
        return this.name().startsWith("CLASS_");
    }
    
    public boolean isGym() {
        return this == GYM;
    }
    
    public boolean isParking() {
        return this == PARKING;
    }
    
    public boolean isLobby() {
        return this == LOBBY;
    }
    
    public boolean isTopRowClass() {
        return this.name().endsWith("A") && isClassroom();
    }
    
    public boolean isBottomRowClass() {
        return this.name().endsWith("B") && isClassroom();
    }
    
    // Legacy grid methods (deprecated but kept for compatibility)
    @Deprecated
    public int getCol() { return 0; }
    
    @Deprecated
    public int getRow() { return 0; }
}
