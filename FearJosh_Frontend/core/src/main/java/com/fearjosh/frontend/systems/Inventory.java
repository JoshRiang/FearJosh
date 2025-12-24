package com.fearjosh.frontend.systems;

import com.fearjosh.frontend.entity.Item;
import java.util.ArrayList;
import java.util.List;

public class Inventory {

    public static final int MAX_SLOTS = 7;

    private Item[] slots;
    private int selectedSlot;

    public Inventory() {
        slots = new Item[MAX_SLOTS];
        selectedSlot = 0;
    }

    public boolean addItem(Item item) {
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] == null) {
                slots[i] = item;
                System.out.println("[Inventory] Added " + item.getName() + " to slot " + i);
                return true;
            }
        }
        System.out.println("[Inventory] Full! Cannot add " + item.getName());
        return false;
    }

    public boolean addItemToSlot(Item item, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) {
            return false;
        }

        if (slots[slotIndex] == null) {
            slots[slotIndex] = item;
            System.out.println("[Inventory] Added " + item.getName() + " to slot " + slotIndex);
            return true;
        }

        return false;
    }

    public void removeItem(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < MAX_SLOTS && slots[slotIndex] != null) {
            Item item = slots[slotIndex];
            item.removeItem();
            slots[slotIndex] = null;
            System.out.println("[Inventory] Removed item from slot " + slotIndex);
        }
    }

    public boolean useSelectedItem() {
        return useItem(selectedSlot);
    }

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

        boolean success = item.useItem();
        if (success) {
            System.out.println("[Inventory] Used " + item.getName());
        }

        return success;
    }

    public Item getItem(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < MAX_SLOTS) {
            return slots[slotIndex];
        }
        return null;
    }

    public Item getSelectedItem() {
        return slots[selectedSlot];
    }

    public void setSelectedSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < MAX_SLOTS) {
            this.selectedSlot = slotIndex;
        }
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public boolean isFull() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] == null) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] != null) {
                return false;
            }
        }
        return true;
    }

    public Item[] getAllSlots() {
        return slots;
    }

    public List<Item> getItems() {
        List<Item> items = new ArrayList<>();
        for (Item item : slots) {
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    public void clear() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] != null) {
                slots[i].removeItem();
                slots[i] = null;
            }
        }
        selectedSlot = 0;
    }

    public void dispose() {
        for (Item item : slots) {
            if (item != null) {
                item.dispose();
            }
        }
    }
}
