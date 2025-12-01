package com.fearjosh.frontend.world;

import java.util.List;

public class Room {

    private final RoomId id;
    private final List<Table> tables;
    private final List<Locker> lockers;
    private final List<Interactable> interactables;

    public Room(RoomId id,
                List<Table> tables,
                List<Locker> lockers,
                List<Interactable> interactables) {
        this.id = id;
        this.tables = tables;
        this.lockers = lockers;
        this.interactables = interactables;
    }

    public RoomId getId() {
        return id;
    }

    public List<Table> getTables() {
        return tables;
    }

    public List<Locker> getLockers() {
        return lockers;
    }

    public List<Interactable> getInteractables() {
        return interactables;
    }

    public void cleanupInactive() {
        interactables.removeIf(inter -> !inter.isActive());
        // lockers tetap dipertahankan meskipun sudah dibuka
    }
}
