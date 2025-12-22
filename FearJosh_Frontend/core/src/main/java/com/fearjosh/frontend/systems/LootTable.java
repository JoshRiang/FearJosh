package com.fearjosh.frontend.systems;

import com.fearjosh.frontend.entity.Item;
import com.fearjosh.frontend.entity.BatteryItem;
import com.fearjosh.frontend.entity.ChocolateItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * LootTable - Defines loot drops from containers
 * Uses weighted random selection for item drops
 */
public class LootTable {
    
    private static final Random random = new Random();
    
    private final List<LootEntry> entries;
    private final float emptyChance; // Chance of dropping nothing
    
    public LootTable(float emptyChance) {
        this.entries = new ArrayList<>();
        this.emptyChance = emptyChance;
    }
    
    /**
     * Add an item to the loot table with a weight
     * Higher weight = more likely to drop
     */
    public void addEntry(ItemSupplier supplier, float weight) {
        entries.add(new LootEntry(supplier, weight));
    }
    
    /**
     * Roll for loot from this table
     * @return Item if successful, null if empty roll
     */
    public Item rollLoot() {
        // First check for empty roll
        if (random.nextFloat() < emptyChance) {
            return null;
        }
        
        if (entries.isEmpty()) {
            return null;
        }
        
        // Calculate total weight
        float totalWeight = 0f;
        for (LootEntry entry : entries) {
            totalWeight += entry.weight;
        }
        
        // Roll a random value
        float roll = random.nextFloat() * totalWeight;
        
        // Find which entry the roll lands on
        float cumulative = 0f;
        for (LootEntry entry : entries) {
            cumulative += entry.weight;
            if (roll <= cumulative) {
                return entry.supplier.create();
            }
        }
        
        // Fallback to last entry
        return entries.get(entries.size() - 1).supplier.create();
    }
    
    /**
     * Get the appropriate loot table for a container type
     */
    public static LootTable getTableForType(String containerType) {
        if (containerType == null) {
            return createDefaultTable();
        }
        
        switch (containerType.toLowerCase()) {
            case "locker":
                return createLockerTable();
            case "chest":
                return createChestTable();
            case "desk":
            case "drawer":
                return createDeskTable();
            default:
                return createDefaultTable();
        }
    }
    
    private static LootTable createLockerTable() {
        LootTable table = new LootTable(0.3f); // 30% chance of nothing
        table.addEntry(BatteryItem::new, 40f);     // 40% battery
        table.addEntry(ChocolateItem::new, 30f);   // 30% chocolate
        return table;
    }
    
    private static LootTable createChestTable() {
        LootTable table = new LootTable(0.1f); // 10% chance of nothing (chests are valuable)
        table.addEntry(BatteryItem::new, 35f);
        table.addEntry(ChocolateItem::new, 35f);
        return table;
    }
    
    private static LootTable createDeskTable() {
        LootTable table = new LootTable(0.5f); // 50% chance of nothing
        table.addEntry(BatteryItem::new, 30f);
        table.addEntry(ChocolateItem::new, 20f);
        return table;
    }
    
    private static LootTable createDefaultTable() {
        LootTable table = new LootTable(0.4f); // 40% chance of nothing
        table.addEntry(BatteryItem::new, 35f);
        table.addEntry(ChocolateItem::new, 25f);
        return table;
    }
    
    /**
     * Functional interface for creating items
     */
    @FunctionalInterface
    public interface ItemSupplier {
        Item create();
    }
    
    /**
     * Internal class for loot entries
     */
    private static class LootEntry {
        final ItemSupplier supplier;
        final float weight;
        
        LootEntry(ItemSupplier supplier, float weight) {
            this.supplier = supplier;
            this.weight = weight;
        }
    }
}
