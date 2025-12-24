package com.fearjosh.frontend.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.systems.AudioManager;

public class BlackTransitionScreen implements Screen {

    private final FearJosh game;
    private final Screen nextScreen;
    private final float duration;
    private final String musicPath;
    private float elapsed;

    public BlackTransitionScreen(FearJosh game, Screen nextScreen, float duration) {
        this(game, nextScreen, duration, null);
    }

    public BlackTransitionScreen(FearJosh game, Screen nextScreen, float duration, String musicPath) {
        this.game = game;
        this.nextScreen = nextScreen;
        this.duration = duration;
        this.musicPath = musicPath;
        this.elapsed = 0f;
    }

    @Override
    public void show() {
        System.out.println("[BlackTransition] Started - duration: " + duration + "s");

        if (musicPath != null && !musicPath.isEmpty()) {
            String currentMusic = AudioManager.getInstance().getCurrentMusicPath();
            if (!musicPath.equals(currentMusic)) {
                AudioManager.getInstance().stopMusic();
                AudioManager.getInstance().playMusic(musicPath, true);
                System.out.println("[BlackTransition] Playing music: " + musicPath);
            }
        }
    }

    @Override
    public void render(float delta) {
        elapsed += delta;

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
