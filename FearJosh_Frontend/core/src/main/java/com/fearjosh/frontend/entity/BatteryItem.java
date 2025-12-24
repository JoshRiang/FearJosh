package com.fearjosh.frontend.entity;

public class BatteryItem extends Item {

    private static final float RECHARGE_AMOUNT = 0.25f; // 25%

    public BatteryItem() {
        super("Baterai", "Mengisi ulang daya senter", true);
        loadIcon("Items/battery.png");
    }

    public float getRechargeAmount() {
        return RECHARGE_AMOUNT;
    }

    @Override
    public boolean useItem() {
        System.out.println("[Battery] Item used - will recharge flashlight");
        return true;
    }
}
