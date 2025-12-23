package com.fearjosh.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * Image-based menu button using PNG assets with proper scaling.
 * Supports hover state, click detection, and consistent sizing.
 * 
 * SCALING CONSTANTS - Use these to control button sizes:
 * - MENU_BUTTON_WIDTH / MENU_BUTTON_HEIGHT for main menu buttons
 * - DIALOG_BUTTON_WIDTH / DIALOG_BUTTON_HEIGHT for dialog buttons
 * - MENU_BUTTON_SPACING for vertical spacing between buttons
 */
public class MenuButton {

    // ==================== SCALING CONSTANTS ====================
    /** Standard menu button width */
    public static final float MENU_BUTTON_WIDTH = 280f;
    /** Standard menu button height */
    public static final float MENU_BUTTON_HEIGHT = 64f;
    /** Spacing between stacked buttons */
    public static final float MENU_BUTTON_SPACING = 18f;
    
    /** Dialog button width (smaller) */
    public static final float DIALOG_BUTTON_WIDTH = 140f;
    /** Dialog button height */
    public static final float DIALOG_BUTTON_HEIGHT = 48f;
    /** Dialog button spacing */
    public static final float DIALOG_BUTTON_SPACING = 30f;
    
    /** Title/logo max width for scaling */
    public static final float TITLE_MAX_WIDTH = 500f;
    
    // ==================== INSTANCE FIELDS ====================
    private Texture normalTexture;
    private Texture hoverTexture;
    private Rectangle bounds;
    private boolean isHovered;
    private float alpha = 1f;
    
    // Visual scale on hover
    private static final float HOVER_SCALE = 1.05f;
    private float currentScale = 1f;
    
    // Fixed render dimensions (not raw texture size)
    private float renderWidth;
    private float renderHeight;

    /**
     * Create a menu button with PNG texture and DEFAULT size.
     * Uses MENU_BUTTON_WIDTH x MENU_BUTTON_HEIGHT.
     * 
     * @param texturePath Path to the normal state texture
     * @param x           X position (center)
     * @param y           Y position (center)
     */
    public MenuButton(String texturePath, float x, float y) {
        this(texturePath, null, x, y, MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT);
    }

    /**
     * Create a menu button with PNG textures for normal and hover states.
     * Uses MENU_BUTTON_WIDTH x MENU_BUTTON_HEIGHT.
     * 
     * @param normalPath Path to normal state texture
     * @param hoverPath  Path to hover state texture (optional)
     * @param x          X position (center)
     * @param y          Y position (center)
     */
    public MenuButton(String normalPath, String hoverPath, float x, float y) {
        this(normalPath, hoverPath, x, y, MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT);
    }

    /**
     * Create a menu button with CUSTOM size (for dialog buttons, etc).
     * 
     * @param normalPath Path to normal state texture
     * @param hoverPath  Path to hover state texture (optional)
     * @param x          X position (center)
     * @param y          Y position (center)
     * @param width      Fixed render width
     * @param height     Fixed render height
     */
    public MenuButton(String normalPath, String hoverPath, float x, float y, float width, float height) {
        // Load textures
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

        // Use fixed dimensions (NOT raw texture size)
        this.renderWidth = width;
        this.renderHeight = height;
        
        // Bounds for hit testing - centered at (x, y)
        bounds = new Rectangle(x - width / 2f, y - height / 2f, width, height);
        isHovered = false;
    }

    /**
     * Set position (center-aligned)
     */
    public void setPosition(float centerX, float centerY) {
        if (bounds != null) {
            bounds.setPosition(centerX - renderWidth / 2f, centerY - renderHeight / 2f);
        }
    }

    /**
     * Set fixed render size (overrides constructor size)
     */
    public void setSize(float width, float height) {
        this.renderWidth = width;
        this.renderHeight = height;
        if (bounds != null) {
            float cx = bounds.x + bounds.width / 2f;
            float cy = bounds.y + bounds.height / 2f;
            bounds.set(cx - width / 2f, cy - height / 2f, width, height);
        }
    }

    /**
     * Update button state based on mouse position
     * 
     * @param mouseX Mouse X in UI coordinates (unprojected)
     * @param mouseY Mouse Y in UI coordinates (unprojected)
     */
    public void update(float mouseX, float mouseY) {
        if (bounds == null) return;
        
        isHovered = bounds.contains(mouseX, mouseY);
        
        // Smooth scale transition
        float targetScale = isHovered ? HOVER_SCALE : 1f;
        currentScale += (targetScale - currentScale) * 0.2f;
    }

    /**
     * Check if button was just clicked
     * 
     * @param mouseX Mouse X in UI coordinates (unprojected)
     * @param mouseY Mouse Y in UI coordinates (unprojected)
     * @return true if clicked
     */
    public boolean isClicked(float mouseX, float mouseY) {
        if (bounds == null) return false;
        return bounds.contains(mouseX, mouseY) && Gdx.input.justTouched();
    }

    /**
     * Render the button with proper scaling
     */
    public void render(SpriteBatch batch) {
        if (normalTexture == null) return;

        Texture tex = (isHovered && hoverTexture != null) ? hoverTexture : normalTexture;
        
        // Apply tint on hover if no hover texture
        if (isHovered && hoverTexture == null) {
            batch.setColor(1.2f, 1.2f, 1.2f, alpha);
        } else {
            batch.setColor(1f, 1f, 1f, alpha);
        }

        // Calculate scaled dimensions (hover effect around fixed size)
        float w = renderWidth * currentScale;
        float h = renderHeight * currentScale;
        
        // Center the scaled button around the bounds center
        float cx = bounds.x + bounds.width / 2f;
        float cy = bounds.y + bounds.height / 2f;
        float x = cx - w / 2f;
        float y = cy - h / 2f;

        // Draw with FIXED size (scaled from our constants, NOT raw texture)
        batch.draw(tex, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    /**
     * Set button alpha
     */
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
    
    // ==================== STATIC LAYOUT HELPERS ====================
    
    /**
     * Calculate Y position for a button in a vertical stack.
     * Index 0 is topmost button.
     * 
     * @param startY    Y position of first (top) button center
     * @param index     Button index (0 = top)
     * @param height    Button height
     * @param spacing   Spacing between buttons
     * @return Y position for button center
     */
    public static float getStackedY(float startY, int index, float height, float spacing) {
        return startY - index * (height + spacing);
    }
    
    /**
     * Calculate Y position using default menu button dimensions.
     */
    public static float getStackedY(float startY, int index) {
        return getStackedY(startY, index, MENU_BUTTON_HEIGHT, MENU_BUTTON_SPACING);
    }
}
