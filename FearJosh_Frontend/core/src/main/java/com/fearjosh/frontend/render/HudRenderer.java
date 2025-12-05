package com.fearjosh.frontend.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class HudRenderer {

    private final float margin = 10f;
    private final float barHeight = 10f;
    private final float segmentWidth = 30f;
    private final float gap = 4f;

    private final Rectangle pauseButtonBounds = new Rectangle();

    public void render(ShapeRenderer renderer,
            float virtualWidth,
            float virtualHeight,
            float stamina,
            float staminaMax,
            float battery,
            float batteryMax) {

        float startX = margin;
        float startY = virtualHeight - margin - barHeight;
        float totalWidth = 4 * segmentWidth + 3 * gap;

        // Battery segments
        float batteryFrac = battery / batteryMax;
        if (batteryFrac < 0f)
            batteryFrac = 0f;
        if (batteryFrac > 1f)
            batteryFrac = 1f;

        int activeSegments = (int) Math.ceil(batteryFrac * 4f);
        if (activeSegments < 0)
            activeSegments = 0;
        if (activeSegments > 4)
            activeSegments = 4;

        for (int i = 0; i < 4; i++) {
            float x = startX + i * (segmentWidth + gap);
            if (i < activeSegments)
                renderer.setColor(Color.YELLOW);
            else
                renderer.setColor(0.3f, 0.3f, 0.1f, 1f);

            renderer.rect(x, startY, segmentWidth, barHeight);
        }

        // Stamina bar
        float staminaMaxWidth = totalWidth;
        float staminaX = margin;
        float staminaY = startY - barHeight - 6f;

        renderer.setColor(0.2f, 0.2f, 0.2f, 1f);
        renderer.rect(staminaX, staminaY, staminaMaxWidth, barHeight);

        float staminaFrac = stamina / staminaMax;
        if (staminaFrac < 0f)
            staminaFrac = 0f;
        if (staminaFrac > 1f)
            staminaFrac = 1f;

        float staminaWidth = staminaMaxWidth * staminaFrac;
        renderer.setColor(Color.CYAN);
        renderer.rect(staminaX, staminaY, staminaWidth, barHeight);

        // Pause button (top-right corner)
        float btnSize = 28f;
        float btnX = virtualWidth - margin - btnSize;
        float btnY = virtualHeight - margin - btnSize;
        pauseButtonBounds.set(btnX, btnY, btnSize, btnSize);

        // Button background
        renderer.setColor(0.15f, 0.15f, 0.18f, 1f);
        renderer.rect(btnX, btnY, btnSize, btnSize);

        // Pause icon: two vertical bars
        float barW = 6f;
        float barH = btnSize - 10f;
        float barY = btnY + 5f;
        float leftBarX = btnX + 6f;
        float rightBarX = btnX + btnSize - 6f - barW;
        renderer.setColor(Color.LIGHT_GRAY);
        renderer.rect(leftBarX, barY, barW, barH);
        renderer.rect(rightBarX, barY, barW, barH);
    }

    public Rectangle getPauseButtonBounds() {
        return pauseButtonBounds;
    }
}
