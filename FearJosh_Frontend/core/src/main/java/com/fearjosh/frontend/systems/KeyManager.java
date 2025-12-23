package com.fearjosh.frontend.systems;

import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.entity.Item;
import com.fearjosh.frontend.entity.KeyItem;
import com.fearjosh.frontend.world.RoomId;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages the key-based escape system.
 * 
 * ESCAPE SEQUENCE:
 * 1. Classroom 1A: Find LOCKER_KEY (exit_key trigger zone)
 * 2. Hallway: Trigger janitor_key zone (requires locker_key) → get JANITOR_KEY
 * 3. Janitor Room: Trigger gym_key zone (requires janitor_key) → get GYM_KEY
 * 4. Gym Back Door: Use GYM_KEY → ESCAPE!
 */
public class KeyManager {

    private static KeyManager instance;

    // Track which locked objects have been opened
    private boolean specialLockerOpened;
    private boolean janitorRoomOpened;
    private boolean gymBackDoorOpened;
    
    // Track consumed exit_key triggers by "roomId_objectId" key
    // This persists across room transitions
    private Set<String> consumedExitKeyTriggers = new HashSet<>();

    public static KeyManager getInstance() {
        if (instance == null) {
            instance = new KeyManager();
        }
        return instance;
    }

    private KeyManager() {
        reset();
    }

    /**
     * Reset all key progress (for new game)
     */
    public void reset() {
        specialLockerOpened = false;
        janitorRoomOpened = false;
        gymBackDoorOpened = false;
        consumedExitKeyTriggers.clear();
        System.out.println("[KeyManager] Reset - all keys, locks, and triggers cleared");
    }
    
    // =====================================================
    // EXIT_KEY TRIGGER TRACKING (persistent across rooms)
    // =====================================================
    
    /**
     * Mark an exit_key trigger as consumed.
     * @param roomId The room containing the trigger
     * @param objectId The TMX object ID of the trigger
     */
    public void markExitKeyTriggerConsumed(RoomId roomId, int objectId) {
        String key = roomId.name() + "_" + objectId;
        consumedExitKeyTriggers.add(key);
        System.out.println("[KeyManager] Marked exit_key trigger consumed: " + key);
    }
    
    /**
     * Check if an exit_key trigger has been consumed.
     * @param roomId The room containing the trigger
     * @param objectId The TMX object ID of the trigger
     * @return true if already consumed
     */
    public boolean isExitKeyTriggerConsumed(RoomId roomId, int objectId) {
        String key = roomId.name() + "_" + objectId;
        return consumedExitKeyTriggers.contains(key);
    }
    
    /**
     * Get all consumed trigger keys for a room (for restoring state on room load).
     * Returns object IDs that were consumed in the specified room.
     */
    public Set<Integer> getConsumedTriggerIdsForRoom(RoomId roomId) {
        Set<Integer> ids = new HashSet<>();
        String prefix = roomId.name() + "_";
        for (String key : consumedExitKeyTriggers) {
            if (key.startsWith(prefix)) {
                try {
                    int objectId = Integer.parseInt(key.substring(prefix.length()));
                    ids.add(objectId);
                } catch (NumberFormatException e) {
                    // Ignore malformed keys
                }
            }
        }
        return ids;
    }

    /**
     * Check if player has a specific key type in inventory
     */
    public boolean hasKey(KeyItem.KeyType keyType) {
        Inventory inventory = GameManager.getInstance().getInventory();
        for (Item item : inventory.getItems()) {
            if (item instanceof KeyItem) {
                KeyItem key = (KeyItem) item;
                if (key.matches(keyType)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Remove a key from inventory (after using it)
     */
    public boolean useKey(KeyItem.KeyType keyType) {
        Inventory inventory = GameManager.getInstance().getInventory();
        Item[] slots = inventory.getAllSlots();
        
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] instanceof KeyItem) {
                KeyItem key = (KeyItem) slots[i];
                if (key.matches(keyType)) {
                    inventory.removeItem(i);
                    System.out.println("[KeyManager] Used key: " + keyType);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Attempt to unlock something with required key
     * @param requiredKey The key type needed
     * @param consumeKey Whether to remove the key after use
     * @return true if unlocked successfully
     */
    public boolean tryUnlock(KeyItem.KeyType requiredKey, boolean consumeKey) {
        if (!hasKey(requiredKey)) {
            System.out.println("[KeyManager] Cannot unlock - missing " + requiredKey);
            return false;
        }

        if (consumeKey) {
            useKey(requiredKey);
        }

        System.out.println("[KeyManager] Unlocked with " + requiredKey);
        return true;
    }

    /**
     * Check if special locker in hallway can be opened
     */
    public boolean canOpenSpecialLocker() {
        return hasKey(KeyItem.KeyType.LOCKER_KEY) && !specialLockerOpened;
    }

    /**
     * Open the special locker (gives janitor key)
     */
    public boolean openSpecialLocker() {
        if (!canOpenSpecialLocker()) {
            return false;
        }

        if (tryUnlock(KeyItem.KeyType.LOCKER_KEY, true)) {
            specialLockerOpened = true;
            
            // Add janitor key to inventory
            GameManager.getInstance().getInventory().addItem(
                new KeyItem(KeyItem.KeyType.JANITOR_KEY));
            
            System.out.println("[KeyManager] Special locker opened! Got JANITOR_KEY");
            return true;
        }
        return false;
    }

    /**
     * Check if janitor room door can be opened
     */
    public boolean canOpenJanitorRoom() {
        return hasKey(KeyItem.KeyType.JANITOR_KEY) && !janitorRoomOpened;
    }

    /**
     * Open janitor room door
     */
    public boolean openJanitorRoom() {
        if (!canOpenJanitorRoom()) {
            return false;
        }

        if (tryUnlock(KeyItem.KeyType.JANITOR_KEY, true)) {
            janitorRoomOpened = true;
            System.out.println("[KeyManager] Janitor room unlocked!");
            return true;
        }
        return false;
    }

    /**
     * Check if gym back door can be opened
     */
    public boolean canOpenGymBackDoor() {
        return hasKey(KeyItem.KeyType.GYM_KEY) && !gymBackDoorOpened;
    }

    /**
     * Open gym back door (triggers escape/ending)
     */
    public boolean openGymBackDoor() {
        if (!canOpenGymBackDoor()) {
            return false;
        }

        if (tryUnlock(KeyItem.KeyType.GYM_KEY, false)) { // Don't consume - keep for story
            gymBackDoorOpened = true;
            System.out.println("[KeyManager] GYM BACK DOOR OPENED - ESCAPE!");
            return true;
        }
        return false;
    }

    /**
     * Get hint message for player based on current progress
     */
    public String getProgressHint() {
        if (!GameManager.getInstance().hasMetJosh()) {
            return "Temukan Josh di dalam sekolah...";
        }
        
        if (!hasKey(KeyItem.KeyType.LOCKER_KEY) && !specialLockerOpened) {
            return "Cari kunci locker di Classroom 1A...";
        }
        
        if (hasKey(KeyItem.KeyType.LOCKER_KEY) && !specialLockerOpened) {
            return "Buka locker khusus di hallway...";
        }
        
        if (!hasKey(KeyItem.KeyType.JANITOR_KEY) && !janitorRoomOpened && specialLockerOpened) {
            return "Gunakan kunci janitor untuk masuk ke ruang janitor...";
        }
        
        if (janitorRoomOpened && !hasKey(KeyItem.KeyType.GYM_KEY)) {
            return "Cari kunci gym di ruang janitor...";
        }
        
        if (hasKey(KeyItem.KeyType.GYM_KEY)) {
            return "Pergi ke pintu belakang gym untuk KABUR!";
        }
        
        return "Temukan jalan keluar...";
    }

    /**
     * Check door lock status for a room
     */
    public boolean isRoomLocked(RoomId roomId) {
        if (roomId == RoomId.JANITOR) {
            return !janitorRoomOpened;
        }
        // Add more locked rooms as needed
        return false;
    }

    /**
     * Get the key required to enter a room
     */
    public KeyItem.KeyType getRequiredKey(RoomId roomId) {
        if (roomId == RoomId.JANITOR) {
            return KeyItem.KeyType.JANITOR_KEY;
        }
        return null;
    }

    // Getters for state
    public boolean isSpecialLockerOpened() { return specialLockerOpened; }
    public boolean isJanitorRoomOpened() { return janitorRoomOpened; }
    public boolean isGymBackDoorOpened() { return gymBackDoorOpened; }
    
    // Convenience methods for PlayScreen checks
    public boolean hasLockerKey() { return hasKey(KeyItem.KeyType.LOCKER_KEY); }
    public boolean hasJanitorKey() { return hasKey(KeyItem.KeyType.JANITOR_KEY); }
    public boolean hasGymKey() { return hasKey(KeyItem.KeyType.GYM_KEY); }
    
    public boolean isSpecialLockerUnlocked() { return specialLockerOpened; }
    public boolean isJanitorRoomUnlocked() { return janitorRoomOpened; }
    public boolean isGymBackDoorUnlocked() { return gymBackDoorOpened; }
    
    public void unlockSpecialLocker() {
        specialLockerOpened = true;
        useKey(KeyItem.KeyType.LOCKER_KEY);
        System.out.println("[KeyManager] Special locker manually unlocked");
    }
    
    public void unlockJanitorRoom() {
        janitorRoomOpened = true;
        useKey(KeyItem.KeyType.JANITOR_KEY);
        System.out.println("[KeyManager] Janitor room manually unlocked");
    }
    
    public void unlockGymBackDoor() {
        gymBackDoorOpened = true;
        System.out.println("[KeyManager] Gym back door manually unlocked");
    }
}
