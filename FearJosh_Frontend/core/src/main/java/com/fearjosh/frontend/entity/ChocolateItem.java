package com.fearjosh.frontend.entity;

/**
 * Chocolate item - restores player stamina
 * This is a USABLE item (consumed on use)
 */
public class ChocolateItem extends Item {

    private static final float STAMINA_RESTORE_AMOUNT = 0.50f; // 50% stamina per item

    public ChocolateItem() {
        super("Chocolate", "Restores stamina when consumed", true);
        loadIcon("Items/chocolate.png"); // Assuming chocolate icon exists
    }

    /**
     * Get the amount of stamina this item restores (as percentage of max stamina)
     */
    public float getStaminaRestore() {
        return STAMINA_RESTORE_AMOUNT;
    }

    @Override
    public boolean useItem() {
        // Chocolate usage is handled by PlayScreen
        // This just signals that the item was successfully used
        System.out.println("[Chocolate] Item used - will restore stamina");
        return true;
    }
}
