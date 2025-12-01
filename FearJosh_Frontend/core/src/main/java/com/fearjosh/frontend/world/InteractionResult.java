package com.fearjosh.frontend.world;

public class InteractionResult {

    public static final InteractionResult NONE = new InteractionResult(0f);

    private final float batteryDelta;

    public InteractionResult(float batteryDelta) {
        this.batteryDelta = batteryDelta;
    }

    public float getBatteryDelta() {
        return batteryDelta;
    }
}
