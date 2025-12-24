package com.fearjosh.frontend.world;

import java.util.HashMap;
import java.util.Map;

public enum RoomId {
    // Entrance & Parking
    PARKING("Area Parkir", 800, 700),
    LOBBY("Lobi Utama", 1440, 1440),
    
    HALLWAY("Koridor Utama", 1920, 960),
    
    GYM("Ruang Olahraga", 1440, 1440),
    
    JANITOR("Ruang Penjaga", 1440, 1440),
    RESTROOM("Toilet", 400, 350),
    TEACHERS_ROOM("Ruang Guru", 600, 400),
    
    CLASS_1A("Kelas 1A", 1440, 1440),
    CLASS_2A("Kelas 2A", 1440, 1440),
    CLASS_3A("Kelas 3A", 500, 450),
    CLASS_4A("Kelas 4A", 500, 450),
    CLASS_5A("Kelas 5A", 500, 450),
    CLASS_6A("Kelas 6A", 500, 450),
    CLASS_7A("Kelas 7A", 500, 450),
    CLASS_8A("Kelas 8A", 500, 450),
    
    CLASS_1B("Kelas 1B", 500, 450),
    CLASS_2B("Kelas 2B", 500, 450),
    CLASS_3B("Kelas 3B", 500, 450),
    CLASS_4B("Kelas 4B", 500, 450),
    CLASS_5B("Kelas 5B", 500, 450),
    CLASS_6B("Kelas 6B", 500, 450),
    CLASS_7B("Kelas 7B", 500, 450),
    CLASS_8B("Kelas 8B", 500, 450);

    private final String displayName;
    private final float width;
    private final float height;
    
    private static final Map<RoomId, Map<DoorPosition, RoomId>> doorConnections = new HashMap<>();

    RoomId(String displayName, float width, float height) {
        this.displayName = displayName;
        this.width = width;
        this.height = height;
    }
    
    public enum DoorPosition {
        TOP, BOTTOM, LEFT, RIGHT,
        TOP_1, TOP_2, TOP_3, TOP_4, TOP_5, TOP_6, TOP_7, TOP_8,
        BOTTOM_1, BOTTOM_2, BOTTOM_3, BOTTOM_4, BOTTOM_5, BOTTOM_6, BOTTOM_7, BOTTOM_8
    }
    
    static {
        addDoor(PARKING, DoorPosition.BOTTOM, LOBBY);
        
        addDoor(LOBBY, DoorPosition.TOP, PARKING);
        addDoor(LOBBY, DoorPosition.RIGHT, HALLWAY);
        
        addDoor(HALLWAY, DoorPosition.LEFT, LOBBY);
        addDoor(HALLWAY, DoorPosition.TOP, GYM);
        
        addDoor(HALLWAY, DoorPosition.TOP_1, CLASS_1A);
        addDoor(HALLWAY, DoorPosition.TOP_2, CLASS_2A);
        addDoor(HALLWAY, DoorPosition.TOP_3, CLASS_3A);
        addDoor(HALLWAY, DoorPosition.TOP_4, CLASS_4A);
        addDoor(HALLWAY, DoorPosition.TOP_5, CLASS_5A);
        addDoor(HALLWAY, DoorPosition.TOP_6, CLASS_6A);
        addDoor(HALLWAY, DoorPosition.TOP_7, CLASS_7A);
        addDoor(HALLWAY, DoorPosition.TOP_8, CLASS_8A);
        
        addDoor(HALLWAY, DoorPosition.BOTTOM_1, CLASS_1B);
        addDoor(HALLWAY, DoorPosition.BOTTOM_2, CLASS_2B);
        addDoor(HALLWAY, DoorPosition.BOTTOM_3, CLASS_3B);
        addDoor(HALLWAY, DoorPosition.BOTTOM_4, CLASS_4B);
        addDoor(HALLWAY, DoorPosition.BOTTOM_5, CLASS_5B);
        addDoor(HALLWAY, DoorPosition.BOTTOM_6, CLASS_6B);
        addDoor(HALLWAY, DoorPosition.BOTTOM_7, CLASS_7B);
        addDoor(HALLWAY, DoorPosition.BOTTOM_8, CLASS_8B);
        
        addDoor(GYM, DoorPosition.BOTTOM, HALLWAY);
        
        addDoor(CLASS_1A, DoorPosition.BOTTOM, HALLWAY);
        addDoor(CLASS_2A, DoorPosition.BOTTOM, HALLWAY);
        addDoor(CLASS_3A, DoorPosition.BOTTOM, HALLWAY);
        addDoor(CLASS_4A, DoorPosition.BOTTOM, HALLWAY);
        addDoor(CLASS_5A, DoorPosition.BOTTOM, HALLWAY);
        addDoor(CLASS_6A, DoorPosition.BOTTOM, HALLWAY);
        addDoor(CLASS_7A, DoorPosition.BOTTOM, HALLWAY);
        addDoor(CLASS_8A, DoorPosition.BOTTOM, HALLWAY);
        
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
    
    public RoomId getDoorConnection(DoorPosition position) {
        Map<DoorPosition, RoomId> doors = doorConnections.get(this);
        return doors != null ? doors.get(position) : null;
    }
    
    public boolean hasDoorAt(DoorPosition position) {
        return getDoorConnection(position) != null;
    }
    
    public Map<DoorPosition, RoomId> getAllDoors() {
        return doorConnections.getOrDefault(this, new HashMap<>());
    }

    public RoomId up() {
        RoomId result = getDoorConnection(DoorPosition.TOP);
        if (result != null) return result;
        // top doors
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
    
    public static RoomId getStartingRoom() {
        return LOBBY;
    }
    
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
    
    @Deprecated
    public int getCol() { return 0; }
    
    @Deprecated
    public int getRow() { return 0; }
}
