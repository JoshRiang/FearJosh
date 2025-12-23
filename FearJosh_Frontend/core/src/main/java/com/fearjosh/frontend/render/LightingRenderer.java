package com.fearjosh.frontend.render;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.fearjosh.frontend.entity.Player;

/**
 * LightingRenderer - Renders fog of war and flashlight effects.
 * 
 * Renamed from LightingSystem to clarify that this is a RENDERER,
 * not a game logic system.
 * 
 * Now supports wall collision for flashlight via ray-casting.
 */
public class LightingRenderer {

    // FOV dasar (tanpa senter) – "mata Jonatan"
    private final float povRadius = 150f;
    private final float povAlpha = 0.20f;

    // Konfigurasi cone senter (ini yang tergantung baterai)
    private final float minConeLength = 140f;
    private final float maxConeLength = 280f;

    private final float minConeWidth = 40f;
    private final float maxConeWidth = 80f;

    private final float minConeAlpha = 0.40f;
    private final float maxConeAlpha = 0.90f;
    
    // Reference to TiledMapManager for collision checking
    private TiledMapManager mapManager;
    
    /**
     * Set the map manager for wall collision detection.
     * Must be called before render() to enable wall collision.
     */
    public void setMapManager(TiledMapManager mapManager) {
        this.mapManager = mapManager;
    }
    
    /**
     * Ray-cast from origin to target, checking for WALL collision only.
     * Uses isWallTile() to check ONLY wall tiles (ID 80-84, 96-100),
     * NOT furniture or other collision objects.
     * 
     * @param startX Starting X position
     * @param startY Starting Y position
     * @param dirX Direction X (normalized)
     * @param dirY Direction Y (normalized)
     * @param maxDist Maximum distance to check
     * @return Distance to first wall hit, or maxDist if none
     */
    private float raycastToWall(float startX, float startY, float dirX, float dirY, float maxDist) {
        if (mapManager == null) {
            return maxDist; // No collision checking available
        }
        
        // Step size for ray marching (smaller = more accurate but slower)
        float stepSize = 8f;
        int maxSteps = (int)(maxDist / stepSize) + 1;
        
        for (int i = 1; i <= maxSteps; i++) {
            float dist = i * stepSize;
            if (dist > maxDist) {
                return maxDist;
            }
            
            float checkX = startX + dirX * dist;
            float checkY = startY + dirY * dist;
            
            // Check if this point is a WALL tile (NOT all collision objects)
            // This uses the new isWallTile() method that only checks wall tile IDs
            if (mapManager.isWallTile(checkX, checkY)) {
                // Return slightly before the wall for smoother appearance
                return Math.max(0, dist - stepSize * 0.5f);
            }
        }
        
        return maxDist;
    }

    public void render(ShapeRenderer renderer,
            Player player,
            boolean flashlightOn,
            float batteryFrac) {

        batteryFrac = MathUtils.clamp(batteryFrac, 0f, 1f);

        // Enable blending for alpha transparency
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        float cx = player.getCenterX();
        float cy = player.getCenterY();

        // =========================
        // AMBIENT POV (player's natural vision - always active)
        // =========================
        // Render soft ambient circle around player regardless of flashlight
        int ambientLayers = 10;
        for (int i = ambientLayers; i >= 0; i--) {
            float t = (float) i / ambientLayers;
            float layerRadius = povRadius * t;
            float layerAlpha = povAlpha * 0.05f * t * t;
            
            renderer.setColor(0.9f, 0.9f, 0.85f, layerAlpha);
            renderer.circle(cx, cy, layerRadius, 32);
        }

        // =========================
        // FLASHLIGHT CONE (LIGHT HOLE) WITH WALL COLLISION
        // =========================
        if (flashlightOn && batteryFrac > 0f) {
            float baseConeLength = MathUtils.lerp(minConeLength, maxConeLength, batteryFrac);
            float coneHalfWidth = MathUtils.lerp(minConeWidth, maxConeWidth, batteryFrac);
            float coneAlpha = MathUtils.lerp(minConeAlpha, maxConeAlpha, batteryFrac);
            
            // Determine direction vector based on player facing
            float dirX = 0, dirY = 0;
            switch (player.getDirection()) {
                case UP:    dirX = 0;  dirY = 1;  break;
                case DOWN:  dirX = 0;  dirY = -1; break;
                case LEFT:  dirX = -1; dirY = 0;  break;
                case RIGHT: dirX = 1;  dirY = 0;  break;
            }
            
            // Ray-cast to find actual cone length (limited by walls)
            // Cast 3 rays: center, left edge, right edge
            float centerDist = raycastToWall(cx, cy, dirX, dirY, baseConeLength);
            
            // Cast edge rays at ~30 degree angle from center
            float edgeAngleRad = 0.5f; // about 30 degrees
            float leftDirX, leftDirY, rightDirX, rightDirY;
            if (dirX == 0) {
                // Facing up or down
                leftDirX = -MathUtils.sin(edgeAngleRad);
                leftDirY = dirY * MathUtils.cos(edgeAngleRad);
                rightDirX = MathUtils.sin(edgeAngleRad);
                rightDirY = dirY * MathUtils.cos(edgeAngleRad);
            } else {
                // Facing left or right
                leftDirX = dirX * MathUtils.cos(edgeAngleRad);
                leftDirY = MathUtils.sin(edgeAngleRad);
                rightDirX = dirX * MathUtils.cos(edgeAngleRad);
                rightDirY = -MathUtils.sin(edgeAngleRad);
            }
            
            float leftDist = raycastToWall(cx, cy, leftDirX, leftDirY, baseConeLength);
            float rightDist = raycastToWall(cx, cy, rightDirX, rightDirY, baseConeLength);
            
            // Use minimum of all 3 distances for a cleaner cut-off
            float effectiveConeLength = Math.min(centerDist, Math.min(leftDist, rightDist));

            // Draw multiple triangles with decreasing size and alpha for smooth gradient
            int layers = 20; // More layers for smoother edge
            for (int i = layers; i >= 0; i--) {
                float t = (float) i / layers;

                // Extend slightly beyond original size for softer edges
                float layerLength = effectiveConeLength * (t * 1.1f);
                float layerWidth = coneHalfWidth * (t * 1.1f);

                // Cubic falloff for very smooth natural light dissipation
                float alphaMultiplier = t * t * t; // Cubic for smoother fade
                float layerAlpha = coneAlpha * 0.08f * alphaMultiplier; // Lower base alpha

                renderer.setColor(1f, 1f, 0.7f, layerAlpha);

                float x1 = cx, y1 = cy;
                float x2 = cx, y2 = cy;
                float x3 = cx, y3 = cy;

                switch (player.getDirection()) {
                    case UP:
                        x2 = cx - layerWidth;
                        y2 = cy + layerLength;
                        x3 = cx + layerWidth;
                        y3 = cy + layerLength;
                        break;
                    case DOWN:
                        x2 = cx - layerWidth;
                        y2 = cy - layerLength;
                        x3 = cx + layerWidth;
                        y3 = cy - layerLength;
                        break;
                    case LEFT:
                        x2 = cx - layerLength;
                        y2 = cy - layerWidth;
                        x3 = cx - layerLength;
                        y3 = cy + layerWidth;
                        break;
                    case RIGHT:
                        x2 = cx + layerLength;
                        y2 = cy - layerWidth;
                        x3 = cx + layerLength;
                        y3 = cy + layerWidth;
                        break;
                }

                renderer.triangle(x1, y1, x2, y2, x3, y3);
            }
        }
    }
}
