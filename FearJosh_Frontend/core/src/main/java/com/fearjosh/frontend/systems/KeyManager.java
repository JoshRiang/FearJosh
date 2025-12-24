package com.fearjosh.frontend.systems;

import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.entity.Item;
import com.fearjosh.frontend.entity.KeyItem;
import com.fearjosh.frontend.world.RoomId;

import java.util.HashSet;
import java.util.Set;

public class KeyManager {

    private static KeyManager instance;

    private boolean specialLockerOpened;
    private boolean janitorRoomOpened;
    private boolean gymBackDoorOpened;
    
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

    public void reset() {
        specialLockerOpened = false;
        janitorRoomOpened = false;
        gymBackDoorOpened = false;
        consumedExitKeyTriggers.clear();
        System.out.println("[KeyManager] Reset - all keys, locks, and triggers cleared");
    }
    
    public void markExitKeyTriggerConsumed(RoomId roomId, int objectId) {
        String key = roomId.name() + "_" + objectId;
        consumedExitKeyTriggers.add(key);
        System.out.println("[KeyManager] Marked exit_key trigger consumed: " + key);
    }
    
    public boolean isExitKeyTriggerConsumed(RoomId roomId, int objectId) {
        String key = roomId.name() + "_" + objectId;
        return consumedExitKeyTriggers.contains(key);
    }
    
    public Set<Integer> getConsumedTriggerIdsForRoom(RoomId roomId) {
        Set<Integer> ids = new HashSet<>();
        String prefix = roomId.name() + "_";
        for (String key : consumedExitKeyTriggers) {
            if (key.startsWith(prefix)) {
                try {
                    int objectId = Integer.parseInt(key.substring(prefix.length()));
                    ids.add(objectId);
                } catch (NumberFormatException e) {
                }
            }
        }
        return ids;
    }

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

    public boolean canOpenSpecialLocker() {
        return hasKey(KeyItem.KeyType.LOCKER_KEY) && !specialLockerOpened;
    }

    public boolean openSpecialLocker() {
        if (!canOpenSpecialLocker()) {
            return false;
        }

        if (tryUnlock(KeyItem.KeyType.LOCKER_KEY, true)) {
            specialLockerOpened = true;
            
            GameManager.getInstance().getInventory().addItem(
                new KeyItem(KeyItem.KeyType.JANITOR_KEY));
            
            System.out.println("[KeyManager] Special locker opened! Got JANITOR_KEY");
            return true;
        }
        return false;
    }

    public boolean canOpenJanitorRoom() {
        return hasKey(KeyItem.KeyType.JANITOR_KEY) && !janitorRoomOpened;
    }

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

    public boolean canOpenGymBackDoor() {
        return hasKey(KeyItem.KeyType.GYM_KEY) && !gymBackDoorOpened;
    }

    public boolean openGymBackDoor() {
        if (!canOpenGymBackDoor()) {
            return false;
        }

        if (tryUnlock(KeyItem.KeyType.GYM_KEY, false)) {
            gymBackDoorOpened = true;
            System.out.println("[KeyManager] GYM BACK DOOR OPENED - ESCAPE!");
            return true;
        }
        return false;
    }

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

    public boolean isRoomLocked(RoomId roomId) {
        if (roomId == RoomId.JANITOR) {
            return !janitorRoomOpened;
        }
        return false;
    }

    public KeyItem.KeyType getRequiredKey(RoomId roomId) {
        if (roomId == RoomId.JANITOR) {
            return KeyItem.KeyType.JANITOR_KEY;
        }
        return null;
    }

    public boolean isSpecialLockerOpened() { return specialLockerOpened; }
    public boolean isJanitorRoomOpened() { return janitorRoomOpened; }
    public boolean isGymBackDoorOpened() { return gymBackDoorOpened; }
    
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
