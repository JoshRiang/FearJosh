package com.fearjosh.frontend.world;

import com.fearjosh.frontend.entity.Item;

public class InteractionResult {

    public static final InteractionResult NONE = new InteractionResult(0f, null);

    private final float batteryDelta; // Legacy: direct battery recharge
    private final Item item; // NEW: item to add to inventory

    // Legacy constructor (for backward compatibility)
    public InteractionResult(float batteryDelta) {
        this(batteryDelta, null);
    }

    // NEW constructor with item support
    public InteractionResult(float batteryDelta, Item item) {
        this.batteryDelta = batteryDelta;
        this.item = item;
    }

    public float getBatteryDelta() {
        return batteryDelta;
    }

    public Item getItem() {
        return item;
    }

    public boolean hasItem() {
        return item != null;
    }
}
