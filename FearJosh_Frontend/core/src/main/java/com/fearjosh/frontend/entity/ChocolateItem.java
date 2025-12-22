package com.fearjosh.frontend.entity;

/**
 * Chocolate item - restores stamina
 * This is a USABLE item (consumed on use)
 */
public class ChocolateItem extends Item {

    private static final float STAMINA_RESTORE_AMOUNT = 0.30f; // 30% stamina restore

    public ChocolateItem() {
        super("Chocolate", "Restores stamina", true);
        loadIcon("Items/chocolate.png");
    }

    /**
     * Get the amount of stamina this item restores
     */
    public float getStaminaRestoreAmount() {
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
