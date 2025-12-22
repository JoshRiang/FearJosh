package com.fearjosh.frontend.factory;

import com.fearjosh.frontend.world.*;
import com.fearjosh.frontend.world.objects.Table;
import com.fearjosh.frontend.world.objects.Locker;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating Room instances.
 * NOTE: Procedural table/locker generation has been removed.
 * Furniture and items are now defined in TMX maps.
 */
public class RoomFactory {

    /**
     * Create a room with empty furniture lists.
     * Furniture and collision is now handled by TMX maps via TiledMapManager.
     */
    public static Room createRoom(RoomId id, float worldWidth, float worldHeight) {
        List<Table> tables = new ArrayList<>();
        List<Locker> lockers = new ArrayList<>();
        List<Interactable> interactables = new ArrayList<>();

        // NOTE: Procedural furniture generation removed.
        // All furniture, items, and collision are now defined in TMX maps.
        // TiledMapManager handles rendering and collision for TMX-based rooms.

        return new Room(id, tables, lockers, interactables);
    }
}
