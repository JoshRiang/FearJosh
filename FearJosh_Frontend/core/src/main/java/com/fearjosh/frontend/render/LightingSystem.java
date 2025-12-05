package com.fearjosh.frontend.render;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.fearjosh.frontend.entity.Player;

public class LightingSystem {

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
        // FLASHLIGHT CONE (LIGHT HOLE)
        // =========================
        if (flashlightOn && batteryFrac > 0f) {
            float coneLength = MathUtils.lerp(minConeLength, maxConeLength, batteryFrac);
            float coneHalfWidth = MathUtils.lerp(minConeWidth, maxConeWidth, batteryFrac);
            float coneAlpha = MathUtils.lerp(minConeAlpha, maxConeAlpha, batteryFrac);

            // Draw multiple triangles with decreasing size and alpha for smooth gradient
            int layers = 20; // More layers for smoother edge
            for (int i = layers; i >= 0; i--) {
                float t = (float) i / layers;

                // Extend slightly beyond original size for softer edges
                float layerLength = coneLength * (t * 1.1f);
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
