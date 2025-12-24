package com.fearjosh.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class MenuButton {

    // Scaling constants
    public static final float MENU_BUTTON_WIDTH = 280f;
    public static final float MENU_BUTTON_HEIGHT = 64f;
    public static final float MENU_BUTTON_SPACING = 18f;
    
    public static final float DIALOG_BUTTON_WIDTH = 140f;
    public static final float DIALOG_BUTTON_HEIGHT = 48f;
    public static final float DIALOG_BUTTON_SPACING = 30f;
    
    public static final float TITLE_MAX_WIDTH = 500f;
    
    // Instance fields
    private Texture normalTexture;
    private Texture hoverTexture;
    private Rectangle bounds;
    private boolean isHovered;
    private float alpha = 1f;
    
    private static final float HOVER_SCALE = 1.05f;
    private float currentScale = 1f;
    
    private float renderWidth;
    private float renderHeight;

    public MenuButton(String texturePath, float x, float y) {
        this(texturePath, null, x, y, MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT);
    }

    public MenuButton(String normalPath, String hoverPath, float x, float y) {
        this(normalPath, hoverPath, x, y, MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT);
    }

    public MenuButton(String normalPath, String hoverPath, float x, float y, float width, float height) {
        try {
            normalTexture = new Texture(normalPath);
            normalTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            System.err.println("[MenuButton] Failed to load texture: " + normalPath);
            normalTexture = null;
        }

        if (hoverPath != null) {
            try {
                hoverTexture = new Texture(hoverPath);
                hoverTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } catch (Exception e) {
                hoverTexture = null;
            }
        }

        this.renderWidth = width;
        this.renderHeight = height;
        bounds = new Rectangle(x - width / 2f, y - height / 2f, width, height);
        isHovered = false;
    }

    public void setPosition(float centerX, float centerY) {
        if (bounds != null) {
            bounds.setPosition(centerX - renderWidth / 2f, centerY - renderHeight / 2f);
        }
    }

    public void setSize(float width, float height) {
        this.renderWidth = width;
        this.renderHeight = height;
        if (bounds != null) {
            float cx = bounds.x + bounds.width / 2f;
            float cy = bounds.y + bounds.height / 2f;
            bounds.set(cx - width / 2f, cy - height / 2f, width, height);
        }
    }

    public void update(float mouseX, float mouseY) {
        if (bounds == null) return;
        
        isHovered = bounds.contains(mouseX, mouseY);
        
        float targetScale = isHovered ? HOVER_SCALE : 1f;
        currentScale += (targetScale - currentScale) * 0.2f;
    }

    public boolean isClicked(float mouseX, float mouseY) {
        if (bounds == null) return false;
        return bounds.contains(mouseX, mouseY) && Gdx.input.justTouched();
    }

    public void render(SpriteBatch batch) {
        if (normalTexture == null) return;

        Texture tex = (isHovered && hoverTexture != null) ? hoverTexture : normalTexture;
        
        if (isHovered && hoverTexture == null) {
            batch.setColor(1.2f, 1.2f, 1.2f, alpha);
        } else {
            batch.setColor(1f, 1f, 1f, alpha);
        }

        float w = renderWidth * currentScale;
        float h = renderHeight * currentScale;
        
        float cx = bounds.x + bounds.width / 2f;
        float cy = bounds.y + bounds.height / 2f;
        float x = cx - w / 2f;
        float y = cy - h / 2f;

        batch.draw(tex, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public boolean isHovered() {
        return isHovered;
    }

    public Rectangle getBounds() {
        return bounds;
    }
    
    public float getRenderWidth() {
        return renderWidth;
    }
    
    public float getRenderHeight() {
        return renderHeight;
    }

    public void dispose() {
        if (normalTexture != null) {
            normalTexture.dispose();
            normalTexture = null;
        }
        if (hoverTexture != null) {
            hoverTexture.dispose();
            hoverTexture = null;
        }
    }
    
    // Layout helpers
    
    public static float getStackedY(float startY, int index, float height, float spacing) {
        return startY - index * (height + spacing);
    }
    
    public static float getStackedY(float startY, int index) {
        return getStackedY(startY, index, MENU_BUTTON_HEIGHT, MENU_BUTTON_SPACING);
    }
}
