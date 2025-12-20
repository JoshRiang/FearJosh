package com.fearjosh.frontend.world.objects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.world.Interactable;
import com.fearjosh.frontend.world.InteractionResult;
import com.fearjosh.frontend.world.items.Battery;

import java.util.List;

/**
 * Locker furniture object - can contain Battery items.
 * Implements Interactable for player interaction.
 */
public class Locker implements Interactable {

    private final float x, y, width, height;
    
    // Collision box - FULL RECTANGLE
    private final Rectangle collisionBounds;
    
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
        
        // Full rectangle collision
        this.collisionBounds = new Rectangle(x, y, width, height);
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
        // This method is kept for interface compatibility but textures are rendered via SpriteBatch
    }

    public void render(SpriteBatch batch) {
        Texture texture = opened ? openTexture : closedTexture;
        batch.draw(texture, x, y, width, height);
    }

    @Override
    public boolean isActive() {
        // Can only interact if still closed
        return active && !opened;
    }

    @Override
    public boolean canInteract(Player player) {
        float px = player.getCenterX();
        float py = player.getCenterY();
        float dx = getCenterX() - px;
        float dy = getCenterY() - py;
        float dist2 = dx * dx + dy * dy;
        float range = 60f; // same as INTERACT_RANGE in PlayScreen
        return dist2 <= range * range;
    }

    @Override
    public InteractionResult interact() {
        if (!opened) {
            opened = true;
            // If battery inside, spawn to room as new interactable
            if (containedBattery != null) {
                roomInteractables.add(containedBattery);
                containedBattery = null;
            }
        }
        return InteractionResult.NONE;
    }
    
    /**
     * Get collision bounds for collision detection with player's footBounds.
     */
    public Rectangle getCollisionBounds() {
        return collisionBounds;
    }

    public void dispose() {
        if (closedTexture != null) {
            closedTexture.dispose();
        }
        if (openTexture != null) {
            openTexture.dispose();
        }
    }
}
