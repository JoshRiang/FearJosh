package com.fearjosh.frontend.world;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Table {

    private final float x, y, width, height;

    public Table(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        ensureTextureLoaded();
    }

    private static Texture TABLE_TEXTURE;

    private static void ensureTextureLoaded() {
        if (TABLE_TEXTURE == null) {
        TABLE_TEXTURE = new Texture("table.png"); // pastikan ada di assets/
        }
    }

    public static void disposeTexture() {
        if (TABLE_TEXTURE != null) {
        TABLE_TEXTURE.dispose();
        TABLE_TEXTURE = null;
        }
    }   

    public void render(SpriteBatch batch) {
    if (TABLE_TEXTURE == null) return;
    batch.draw(TABLE_TEXTURE, x, y, width, height);
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
