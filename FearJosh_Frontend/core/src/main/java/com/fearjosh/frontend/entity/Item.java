package com.fearjosh.frontend.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Base interface for all items in the game inventory.
 * Items can be usable (batteries, medkit) or non-usable (keys, collectibles).
 */
public abstract class Item {

    protected String name;
    protected String description;
    protected Texture icon;
    protected boolean usable;

    public Item(String name, String description, boolean usable) {
        this.name = name;
        this.description = description;
        this.usable = usable;
    }

    /**
     * Check if this item can be used
     * 
     * @return true if item has a use action (battery, medkit), false otherwise
     *         (keys)
     */
    public boolean isUsable() {
        return usable;
    }

    /**
     * Use this item (consume battery, use medkit, etc.)
     * Override in subclasses to implement specific behavior
     * 
     * @return true if item was used successfully, false otherwise
     */
    public abstract boolean useItem();

    /**
     * Remove this item from inventory
     * Called when item is consumed or manually dropped
     */
    public void removeItem() {
        if (icon != null) {
            icon.dispose();
            icon = null;
        }
    }

    /**
     * Load item icon texture
     * 
     * @param iconPath Path to icon texture file
     */
    public void loadIcon(String iconPath) {
        try {
            this.icon = new Texture(iconPath);
        } catch (Exception e) {
            System.err.println("[Item] Failed to load icon: " + iconPath);
            e.printStackTrace();
        }
    }

    /**
     * Render item icon at specified position
     */
    public void renderIcon(SpriteBatch batch, float x, float y, float width, float height) {
        if (icon != null) {
            batch.draw(icon, x, y, width, height);
        }
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Texture getIcon() {
        return icon;
    }

    public void dispose() {
        removeItem();
    }
}
