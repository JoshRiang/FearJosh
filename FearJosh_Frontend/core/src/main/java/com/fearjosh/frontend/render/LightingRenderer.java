package com.fearjosh.frontend.render;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.fearjosh.frontend.entity.Player;

public class LightingRenderer {

    // FOV config
    private final float povRadius = 150f;
    private final float povAlpha = 0.20f;

    // Cone config
    private final float minConeLength = 140f;
    private final float maxConeLength = 280f;

    private final float minConeWidth = 40f;
    private final float maxConeWidth = 80f;

    private final float minConeAlpha = 0.40f;
    private final float maxConeAlpha = 0.90f;
    
    private TiledMapManager mapManager;
    
    public void setMapManager(TiledMapManager mapManager) {
        this.mapManager = mapManager;
    }
    
    // Raycast
    private float raycastToWall(float startX, float startY, float dirX, float dirY, float maxDist) {
        if (mapManager == null) {
            return maxDist;
        }
        
        float stepSize = 8f;
        int maxSteps = (int)(maxDist / stepSize) + 1;
        
        for (int i = 1; i <= maxSteps; i++) {
            float dist = i * stepSize;
            if (dist > maxDist) {
                return maxDist;
            }
            
            float checkX = startX + dirX * dist;
            float checkY = startY + dirY * dist;
            
            if (mapManager.isWallTile(checkX, checkY)) {
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

        // Blending
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        float cx = player.getCenterX();
        float cy = player.getCenterY();

        // Ambient POV
        int ambientLayers = 10;
        for (int i = ambientLayers; i >= 0; i--) {
            float t = (float) i / ambientLayers;
            float layerRadius = povRadius * t;
            float layerAlpha = povAlpha * 0.05f * t * t;
            
            renderer.setColor(0.9f, 0.9f, 0.85f, layerAlpha);
            renderer.circle(cx, cy, layerRadius, 32);
        }

        // Flashlight cone
        if (flashlightOn && batteryFrac > 0f) {
            float baseConeLength = MathUtils.lerp(minConeLength, maxConeLength, batteryFrac);
            float coneHalfWidth = MathUtils.lerp(minConeWidth, maxConeWidth, batteryFrac);
            float coneAlpha = MathUtils.lerp(minConeAlpha, maxConeAlpha, batteryFrac);
            
            float dirX = 0, dirY = 0;
            switch (player.getDirection()) {
                case UP:    dirX = 0;  dirY = 1;  break;
                case DOWN:  dirX = 0;  dirY = -1; break;
                case LEFT:  dirX = -1; dirY = 0;  break;
                case RIGHT: dirX = 1;  dirY = 0;  break;
            }
            
            // Raycast edges
            float centerDist = raycastToWall(cx, cy, dirX, dirY, baseConeLength);
            
            float edgeAngleRad = 0.5f;
            float leftDirX, leftDirY, rightDirX, rightDirY;
            if (dirX == 0) {
                leftDirX = -MathUtils.sin(edgeAngleRad);
                leftDirY = dirY * MathUtils.cos(edgeAngleRad);
                rightDirX = MathUtils.sin(edgeAngleRad);
                rightDirY = dirY * MathUtils.cos(edgeAngleRad);
            } else {
                leftDirX = dirX * MathUtils.cos(edgeAngleRad);
                leftDirY = MathUtils.sin(edgeAngleRad);
                rightDirX = dirX * MathUtils.cos(edgeAngleRad);
                rightDirY = -MathUtils.sin(edgeAngleRad);
            }
            
            float leftDist = raycastToWall(cx, cy, leftDirX, leftDirY, baseConeLength);
            float rightDist = raycastToWall(cx, cy, rightDirX, rightDirY, baseConeLength);
            
            float effectiveConeLength = Math.min(centerDist, Math.min(leftDist, rightDist));

            // Gradient layers
            int layers = 20;
            for (int i = layers; i >= 0; i--) {
                float t = (float) i / layers;

                float layerLength = effectiveConeLength * (t * 1.1f);
                float layerWidth = coneHalfWidth * (t * 1.1f);

                float alphaMultiplier = t * t * t;
                float layerAlpha = coneAlpha * 0.08f * alphaMultiplier;

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
