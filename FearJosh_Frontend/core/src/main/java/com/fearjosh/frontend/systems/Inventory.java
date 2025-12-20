package com.fearjosh.frontend.systems;

import com.fearjosh.frontend.entity.Item;
import java.util.ArrayList;
import java.util.List;

/**
 * Inventory system managing 7 item slots (Minecraft-style).
 * Handles adding, removing, using items.
 */
public class Inventory {

    public static final int MAX_SLOTS = 7;

    private Item[] slots;
    private int selectedSlot; // Currently selected slot (0-6)

    public Inventory() {
        slots = new Item[MAX_SLOTS];
        selectedSlot = 0;
    }

    /**
     * Add item to first empty slot
     * 
     * @return true if added successfully, false if inventory full
     */
    public boolean addItem(Item item) {
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] == null) {
                slots[i] = item;
                System.out.println("[Inventory] Added " + item.getName() + " to slot " + i);
                return true;
            }
        }
        System.out.println("[Inventory] Full! Cannot add " + item.getName());
        return false; // Inventory full
    }

    /**
     * Add item to specific slot
     * 
     * @return true if added, false if slot occupied
     */
    public boolean addItemToSlot(Item item, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) {
            return false;
        }

        if (slots[slotIndex] == null) {
            slots[slotIndex] = item;
            System.out.println("[Inventory] Added " + item.getName() + " to slot " + slotIndex);
            return true;
        }

        return false; // Slot occupied
    }

    /**
     * Remove item from specific slot
     */
    public void removeItem(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < MAX_SLOTS && slots[slotIndex] != null) {
            Item item = slots[slotIndex];
            item.removeItem();
            slots[slotIndex] = null;
            System.out.println("[Inventory] Removed item from slot " + slotIndex);
        }
    }

    /**
     * Use item in currently selected slot
     * 
     * @return true if item was used, false otherwise
     */
    public boolean useSelectedItem() {
        return useItem(selectedSlot);
    }

    /**
     * Use item in specific slot
     * 
     * @return true if item was used successfully
     */
    public boolean useItem(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) {
            return false;
        }

        Item item = slots[slotIndex];
        if (item == null) {
            System.out.println("[Inventory] No item in slot " + slotIndex);
            return false;
        }

        if (!item.isUsable()) {
            System.out.println("[Inventory] " + item.getName() + " cannot be used");
            return false;
        }

        // Use the item
        boolean success = item.useItem();
        if (success) {
            // If item is consumed, remove it
            // (Subclasses can decide if they're consumed or not)
            System.out.println("[Inventory] Used " + item.getName());
        }

        return success;
    }

    /**
     * Get item in specific slot
     */
    public Item getItem(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < MAX_SLOTS) {
            return slots[slotIndex];
        }
        return null;
    }

    /**
     * Get currently selected item
     */
    public Item getSelectedItem() {
        return slots[selectedSlot];
    }

    /**
     * Select slot by index (0-6)
     */
    public void setSelectedSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < MAX_SLOTS) {
            this.selectedSlot = slotIndex;
        }
    }

    /**
     * Get currently selected slot index
     */
    public int getSelectedSlot() {
        return selectedSlot;
    }

    /**
     * Check if inventory is full
     */
    public boolean isFull() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if inventory is empty
     */
    public boolean isEmpty() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get all items (including nulls)
     */
    public Item[] getAllSlots() {
        return slots;
    }

    /**
     * Get list of non-null items
     */
    public List<Item> getItems() {
        List<Item> items = new ArrayList<>();
        for (Item item : slots) {
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    /**
     * Clear all items
     */
    public void clear() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] != null) {
                slots[i].removeItem();
                slots[i] = null;
            }
        }
        selectedSlot = 0;
    }

    /**
     * Dispose resources
     */
    public void dispose() {
        for (Item item : slots) {
            if (item != null) {
                item.dispose();
            }
        }
    }
}
