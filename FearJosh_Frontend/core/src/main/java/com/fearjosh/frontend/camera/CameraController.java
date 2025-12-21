package com.fearjosh.frontend.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.MathUtils;
import com.fearjosh.frontend.entity.Player;

public class CameraController {

    private float worldWidth;
    private float worldHeight;

    public CameraController(float worldWidth, float worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }
    
    /**
     * Update world bounds (used when switching to rooms with different sizes)
     */
    public void setWorldBounds(float width, float height) {
        this.worldWidth = width;
        this.worldHeight = height;
    }
    
    public float getWorldWidth() {
        return worldWidth;
    }
    
    public float getWorldHeight() {
        return worldHeight;
    }

    public void update(OrthographicCamera camera, Viewport viewport, Player player) {
        float viewW = viewport.getWorldWidth() * camera.zoom;
        float viewH = viewport.getWorldHeight() * camera.zoom;
        float halfW = viewW / 2f;
        float halfH = viewH / 2f;

        float targetX = player.getCenterX();
        float targetY = player.getCenterY();

        // Margin to include walls rendered outside room bounds
        float wallMargin = 100f; // Same as WALL_THICKNESS in PlayScreen
        
        // Extended world bounds to include walls
        float extendedWidth = worldWidth;
        float extendedHeight = worldHeight + wallMargin; // Include top wall

        // If room is smaller than viewport, center the camera on the room
        float camX, camY;
        
        if (extendedWidth <= viewW) {
            // Room is smaller than view width - center horizontally
            camX = worldWidth / 2f;
        } else {
            // Normal clamping - allow camera to see edges
            camX = MathUtils.clamp(targetX, halfW, worldWidth - halfW);
        }
        
        if (extendedHeight <= viewH) {
            // Room is smaller than view height - center vertically (offset for top wall)
            camY = (worldHeight + wallMargin) / 2f - wallMargin / 2f;
        } else {
            // Normal clamping - allow camera to show top wall when at top
            float minY = halfH;
            float maxY = worldHeight + wallMargin - halfH;
            camY = MathUtils.clamp(targetY, minY, maxY);
        }

        camera.position.set(camX, camY, 0f);
        camera.update();
    }
}
