package com.fearjosh.frontend.render;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.fearjosh.frontend.entity.Player;

public class LightingSystem {

    // FOV dasar (tanpa senter) – “mata Jonatan”
    private final float povRadius = 90f;   // radius lingkaran sekitar player
    private final float povAlpha = 0.20f; // seberapa kuat mencerahkan area sekitar

    // Konfigurasi cone senter (ini yang tergantung baterai)
    private final float minConeLength = 140f;
    private final float maxConeLength = 280f;

    private final float minConeWidth = 40f;
    private final float maxConeWidth = 80f;

    // Cahaya senter harus lebih terang dari FOV dasar
    private final float minConeAlpha = 0.40f;
    private final float maxConeAlpha = 0.90f;

    public void render(ShapeRenderer renderer,
                       Player player,
                       boolean flashlightOn,
                       float batteryFrac) {

        batteryFrac = MathUtils.clamp(batteryFrac, 0f, 1f);

        float cx = player.getCenterX();
        float cy = player.getCenterY();

        // =========================
        //  FOV DASAR (TANPA SENTER)
        // =========================
        // ini “mata Jonatan”: area di sekitar dia yang otomatis lebih terang
        // nilai 0.20f kurang lebih seperti opacity 20%
        renderer.setColor(1f, 1f, 1f, 0.20f);
        renderer.circle(cx, cy, povRadius);

        // =========================
        //  FLASHLIGHT CONE
        // =========================
        if (!flashlightOn || batteryFrac <= 0f) return;

        float coneLength = MathUtils.lerp(minConeLength, maxConeLength, batteryFrac);
        float coneHalfWidth = MathUtils.lerp(minConeWidth, maxConeWidth, batteryFrac);

        // flashlight lebih “menerangkan” daripada FOV:
        // alpha min 0.4 (40%), max 0.8 (80%)
        float coneAlpha = MathUtils.lerp(0.4f, 0.8f, batteryFrac);

        renderer.setColor(1f, 1f, 0.6f, coneAlpha);

        float x1 = cx, y1 = cy;
        float x2 = cx, y2 = cy;
        float x3 = cx, y3 = cy;

        switch (player.getDirection()) {
            case UP:
                x2 = cx - coneHalfWidth;
                y2 = cy + coneLength;
                x3 = cx + coneHalfWidth;
                y3 = cy + coneLength;
                break;
            case DOWN:
                x2 = cx - coneHalfWidth;
                y2 = cy - coneLength;
                x3 = cx + coneHalfWidth;
                y3 = cy - coneLength;
                break;
            case LEFT:
                x2 = cx - coneLength;
                y2 = cy - coneHalfWidth;
                x3 = cx - coneLength;
                y3 = cy + coneHalfWidth;
                break;
            case RIGHT:
                x2 = cx + coneLength;
                y2 = cy - coneHalfWidth;
                x3 = cx + coneLength;
                y3 = cy + coneHalfWidth;
                break;
        }

        renderer.triangle(x1, y1, x2, y2, x3, y3);
    }
}
