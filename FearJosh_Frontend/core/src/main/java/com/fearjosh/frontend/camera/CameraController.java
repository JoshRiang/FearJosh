package com.fearjosh.frontend.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.MathUtils;
import com.fearjosh.frontend.entity.Player;

public class CameraController {

    private final float worldWidth;
    private final float worldHeight;

    public CameraController(float worldWidth, float worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    public void update(OrthographicCamera camera, Viewport viewport, Player player) {
        float halfW = (viewport.getWorldWidth()  * camera.zoom) / 2f;
        float halfH = (viewport.getWorldHeight() * camera.zoom) / 2f;

        float targetX = player.getCenterX();
        float targetY = player.getCenterY();

        float camX = MathUtils.clamp(targetX, halfW, worldWidth  - halfW);
        float camY = MathUtils.clamp(targetY, halfH, worldHeight - halfH);

        camera.position.set(camX, camY, 0f);
        camera.update();
    }
}
