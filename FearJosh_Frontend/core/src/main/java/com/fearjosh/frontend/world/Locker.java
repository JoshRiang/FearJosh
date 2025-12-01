package com.fearjosh.frontend.world;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.fearjosh.frontend.entity.Player;

import java.util.List;

public class Locker implements Interactable {

    private final float x, y, width, height;
    private boolean opened = false;
    private boolean active = true;
    private Battery containedBattery;
    private final List<Interactable> roomInteractables;

    public Locker(float x, float y, float width, float height, List<Interactable> roomInteractables) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.roomInteractables = roomInteractables;
    }

    public void setContainedBattery(Battery battery) {
        this.containedBattery = battery;
    }

    public float getCenterX() {
        return x + width / 2f;
    }

    public float getCenterY() {
        return y + height / 2f;
    }

    public float getY() {
        return y;
    }

    public float getX() {
        return x;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    @Override
    public void render(ShapeRenderer renderer) {
        if (!opened) {
            // Loker tertutup (abu-abu agak gelap)
            renderer.setColor(0.6f, 0.6f, 0.6f, 1f);
            renderer.rect(x, y, width, height);
        } else {
            // Loker terbuka (abu-abu lebih terang + garis pintu)
            renderer.setColor(0.8f, 0.8f, 0.8f, 1f);
            renderer.rect(x, y, width, height);

            renderer.setColor(0.5f, 0.5f, 0.5f, 1f);
            renderer.rectLine(x + width * 0.25f, y, x + width * 0.25f, y + height, 2f);
        }
    }

    @Override
    public boolean isActive() {
        // hanya bisa di-interact kalau masih tertutup
        return active && !opened;
    }

    @Override
    public boolean canInteract(Player player) {
        float px = player.getCenterX();
        float py = player.getCenterY();
        float dx = getCenterX() - px;
        float dy = getCenterY() - py;
        float dist2 = dx * dx + dy * dy;
        float range = 60f; // sama seperti INTERACT_RANGE di PlayScreen
        return dist2 <= range * range;
    }

    @Override
    public InteractionResult interact() {
        if (!opened) {
            opened = true;
            // kalau ada baterai di dalam, spawn ke ruangan sebagai interactable baru
            if (containedBattery != null) {
                roomInteractables.add(containedBattery);
                containedBattery = null;
            }
            // setelah dibuka, loker jadi tidak aktif untuk interaksi E
            active = false;
        }
        return InteractionResult.NONE;
    }
}
