package com.fearjosh.frontend.world;

import com.fearjosh.frontend.entity.BatteryItem;
import com.fearjosh.frontend.entity.ChocolateItem;
import com.fearjosh.frontend.entity.Item;

import java.util.Random;

/**
 * Loot table for lockers.
 * Provides random drops of chocolate or battery items.
 */
public class LockerLootTable {

    private static final Random random = new Random();

    // Loot probabilities (should sum to <= 1.0)
    private static final float BATTERY_CHANCE = 0.35f;      // 35% chance for battery
    private static final float CHOCOLATE_CHANCE = 0.35f;    // 35% chance for chocolate
    // 30% chance for nothing (empty locker)

    /**
     * Roll for loot from a locker.
     * 
     * @return Item if locker contains something, null if empty
     */
    public static Item rollLoot() {
        float roll = random.nextFloat();

        if (roll < BATTERY_CHANCE) {
            System.out.println("[LockerLootTable] Rolled: Battery!");
            return new BatteryItem();
        } else if (roll < BATTERY_CHANCE + CHOCOLATE_CHANCE) {
            System.out.println("[LockerLootTable] Rolled: Chocolate!");
            return new ChocolateItem();
        } else {
            System.out.println("[LockerLootTable] Rolled: Empty locker");
            return null;
        }
    }

    /**
     * Get a specific loot type by name (for debugging/testing)
     * 
     * @param lootType "battery", "chocolate", or anything else for random
     * @return The requested item or random roll
     */
    public static Item getLoot(String lootType) {
        if ("battery".equalsIgnoreCase(lootType)) {
            return new BatteryItem();
        } else if ("chocolate".equalsIgnoreCase(lootType)) {
            return new ChocolateItem();
        }
        return rollLoot();
    }

    /**
     * Roll with custom probabilities
     * 
     * @param batteryChance Probability for battery (0.0 to 1.0)
     * @param chocolateChance Probability for chocolate (0.0 to 1.0)
     * @return Item if locker contains something, null if empty
     */
    public static Item rollLoot(float batteryChance, float chocolateChance) {
        float roll = random.nextFloat();

        if (roll < batteryChance) {
            return new BatteryItem();
        } else if (roll < batteryChance + chocolateChance) {
            return new ChocolateItem();
        }
        return null;
    }
}
