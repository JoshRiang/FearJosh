package com.fearjosh.frontend.world.items;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.world.Interactable;
import com.fearjosh.frontend.world.InteractionResult;

/**
 * Battery collectible item - restores flashlight battery when collected.
 */
public class Battery implements Interactable {

    private final float x, y, width, height;
    private boolean collected = false;

    public Battery(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void render(ShapeRenderer renderer) {
        if (!collected) {
            renderer.setColor(Color.YELLOW);
            renderer.rect(x, y, width, height);
        }
    }

    @Override
    public boolean isActive() {
        return !collected;
    }

    @Override
    public boolean canInteract(Player player) {
        float px = player.getCenterX();
        float py = player.getCenterY();
        float dx = getCenterX() - px;
        float dy = getCenterY() - py;
        float dist2 = dx * dx + dy * dy;
        float range = 60f;
        return dist2 <= range * range;
    }

    @Override
    public InteractionResult interact() {
        if (!collected) {
            collected = true;
            // +25% battery
            return new InteractionResult(0.25f);
        }
        return InteractionResult.NONE;
    }

    @Override
    public float getCenterX() {
        return x + width / 2f;
    }

    @Override
    public float getCenterY() {
        return y + height / 2f;
    }
}
