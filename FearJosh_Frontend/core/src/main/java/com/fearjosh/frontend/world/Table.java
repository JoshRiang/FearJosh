package com.fearjosh.frontend.world;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Table {

    private final float x, y, width, height;

    public Table(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(ShapeRenderer renderer) {
        // Top table
        renderer.setColor(0.55f, 0.27f, 0.07f, 1f);
        renderer.rect(x, y, width, height);

        // Legs (2 kaki)
        float legWidth = width * 0.15f;
        float legHeight = 15f;
        renderer.setColor(0.4f, 0.2f, 0.05f, 1f);
        renderer.rect(x + width * 0.15f, y - legHeight, legWidth, legHeight);
        renderer.rect(x + width * 0.7f, y - legHeight, legWidth, legHeight);
    }

    public float getCenterX() {
        return x + width / 2f;
    }

    public float getY() {
        return y;
    }

    public float getHeight() {
        return height;
    }

    public float getX() {
        return x;
    }

    public float getWidth() {
        return width;
    }
}
