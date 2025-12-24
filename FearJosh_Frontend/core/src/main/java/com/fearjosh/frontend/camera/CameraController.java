package com.fearjosh.frontend.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.MathUtils;
import com.fearjosh.frontend.entity.Player;

public class CameraController {

    private float worldWidth;
    private float worldHeight;
    
    // Camera shake parameters
    private float shakeDuration = 0f;
    private float shakeIntensity = 0f;
    private float shakeTimer = 0f;
    private float shakeOffsetX = 0f;
    private float shakeOffsetY = 0f;

    public CameraController(float worldWidth, float worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }
    
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
    
    public void shake(float duration, float intensity) {
        this.shakeDuration = duration;
        this.shakeIntensity = intensity;
        this.shakeTimer = 0f;
    }
    
    public boolean isShaking() {
        return shakeTimer < shakeDuration;
    }
    
    private void updateShake(float delta) {
        if (shakeTimer < shakeDuration) {
            shakeTimer += delta;
            // Decay intensity over time
            float progress = shakeTimer / shakeDuration;
            float currentIntensity = shakeIntensity * (1f - progress);
            
            // Random offset
            shakeOffsetX = MathUtils.random(-currentIntensity, currentIntensity);
            shakeOffsetY = MathUtils.random(-currentIntensity, currentIntensity);
        } else {
            shakeOffsetX = 0f;
            shakeOffsetY = 0f;
        }
    }

    public void update(OrthographicCamera camera, Viewport viewport, Player player) {
        update(camera, viewport, player, 0.016f); // Default delta ~60fps
    }
    
    public void update(OrthographicCamera camera, Viewport viewport, Player player, float delta) {
        // Update shake effect
        updateShake(delta);
        
        float viewW = viewport.getWorldWidth() * camera.zoom;
        float viewH = viewport.getWorldHeight() * camera.zoom;
        float halfW = viewW / 2f;
        float halfH = viewH / 2f;

        float targetX = player.getCenterX();
        float targetY = player.getCenterY();

        // wall margin
        float wallMargin = 100f;
        
        // bounds
        float extendedWidth = worldWidth;
        float extendedHeight = worldHeight + wallMargin; // Include top wall

        float camX, camY;
        
        if (extendedWidth <= viewW) {
            // center X
            camX = worldWidth / 2f;
        } else {
            // clamp X
            camX = MathUtils.clamp(targetX, halfW, worldWidth - halfW);
        }
        
        if (extendedHeight <= viewH) {
            // center Y
            camY = (worldHeight + wallMargin) / 2f - wallMargin / 2f;
        } else {
            // clamp Y
            float minY = halfH;
            float maxY = worldHeight + wallMargin - halfH;
            camY = MathUtils.clamp(targetY, minY, maxY);
        }

        // Apply shake offset
        camX += shakeOffsetX;
        camY += shakeOffsetY;

        camera.position.set(camX, camY, 0f);
        camera.update();
    }
}
