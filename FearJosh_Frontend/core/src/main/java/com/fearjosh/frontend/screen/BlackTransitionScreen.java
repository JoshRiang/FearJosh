package com.fearjosh.frontend.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.fearjosh.frontend.FearJosh;

/**
 * Simple black screen transition that waits for specified duration
 * before transitioning to next screen.
 */
public class BlackTransitionScreen implements Screen {

    private final FearJosh game;
    private final Screen nextScreen;
    private final float duration;
    private float elapsed;

    /**
     * Create a black transition screen.
     * 
     * @param game       The game instance
     * @param nextScreen Screen to transition to after duration
     * @param duration   How long to show black screen in seconds
     */
    public BlackTransitionScreen(FearJosh game, Screen nextScreen, float duration) {
        this.game = game;
        this.nextScreen = nextScreen;
        this.duration = duration;
        this.elapsed = 0f;
    }

    @Override
    public void show() {
        System.out.println("[BlackTransition] Started - duration: " + duration + "s");
    }

    @Override
    public void render(float delta) {
        elapsed += delta;

        // Clear to black
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Transition when duration elapsed
        if (elapsed >= duration) {
            System.out.println("[BlackTransition] Transitioning to next screen");
            game.setScreen(nextScreen);
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
    }
}
