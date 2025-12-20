package com.fearjosh.frontend.entity;

/**
 * Battery item - restores flashlight power
 * This is a USABLE item (consumed on use)
 */
public class BatteryItem extends Item {

    private static final float RECHARGE_AMOUNT = 0.25f; // 25% battery per item (1 bar = quarter charge)

    public BatteryItem() {
        super("Battery", "Restores flashlight power", true);
        loadIcon("Items/battery.png"); // Assuming battery icon exists
    }

    /**
     * Get the amount of battery this item restores
     */
    public float getRechargeAmount() {
        return RECHARGE_AMOUNT;
    }

    @Override
    public boolean useItem() {
        // Battery usage is handled by PlayScreen
        // This just signals that the item was successfully used
        System.out.println("[Battery] Item used - will recharge flashlight");
        return true;
    }
}
