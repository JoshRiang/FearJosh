package com.fearjosh.frontend.world;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.fearjosh.frontend.entity.Player;

public interface Interactable {
    void render(ShapeRenderer renderer);
    boolean isActive();
    boolean canInteract(Player player);
    InteractionResult interact();
    float getCenterX();
    float getCenterY();
}
