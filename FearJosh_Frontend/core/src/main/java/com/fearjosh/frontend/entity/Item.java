package com.fearjosh.frontend.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

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

    public boolean isUsable() {
        return usable;
    }

    public abstract boolean useItem();

    public void removeItem() {
        if (icon != null) {
            icon.dispose();
            icon = null;
        }
    }

    public void loadIcon(String iconPath) {
        try {
            this.icon = new Texture(iconPath);
        } catch (Exception e) {
            System.err.println("[Item] Failed to load icon: " + iconPath);
            e.printStackTrace();
        }
    }

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
