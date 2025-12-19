package com.fearjosh.frontend.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.fearjosh.frontend.entity.Player;

import java.util.List;

public class Locker implements Interactable {

    private final float x, y, width, height;
    private boolean opened = false;
    private boolean active = true;
    private Battery containedBattery;
    private final List<Interactable> roomInteractables;
    private Texture closedTexture;
    private Texture openTexture;

    public Locker(float x, float y, float width, float height, List<Interactable> roomInteractables) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.roomInteractables = roomInteractables;
        this.closedTexture = new Texture("locker_closed.png");
        this.openTexture = new Texture("locker_open.png");
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
        // This method is kept for interface compatibility but textures are rendered in PlayScreen
    }

    public void render(SpriteBatch batch) {
        Texture texture = opened ? openTexture : closedTexture;
        batch.draw(texture, x, y, width, height);
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

    public void dispose() {
        if (closedTexture != null) closedTexture.dispose();
        if (openTexture != null) openTexture.dispose();
    }
}
