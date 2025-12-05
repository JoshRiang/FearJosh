package com.fearjosh.frontend.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fearjosh.frontend.FearJosh;

/**
 * Minimal placeholder Settings screen with a Back button.
 */
public class SettingsScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;

    private final FearJosh game;
    private final Stage stage;
    private final FitViewport viewport;
    private final OrthographicCamera uiCamera;
    private final Skin skin;

    public SettingsScreen(FearJosh game) {
        this.game = game;
        uiCamera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, uiCamera);
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        // Reuse Skin from MainMenu if available; otherwise create a basic one
        // For simplicity, create a temporary Skin using Scene2D default font
        skin = new Skin();
        skin.add("default-font", new com.badlogic.gdx.graphics.g2d.BitmapFont());
        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle ls = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
        ls.font = (com.badlogic.gdx.graphics.g2d.BitmapFont) skin.get("default-font",
                com.badlogic.gdx.graphics.g2d.BitmapFont.class);
        skin.add("default", ls);
        com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle tbs = new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle();
        tbs.font = (com.badlogic.gdx.graphics.g2d.BitmapFont) skin.get("default-font",
                com.badlogic.gdx.graphics.g2d.BitmapFont.class);
        skin.add("default", tbs);

        Table root = new Table();
        root.setFillParent(true);
        root.center();
        stage.addActor(root);

        Label title = new Label("Settings", skin);
        TextButton back = new TextButton("Back", skin);

        root.add(title).padBottom(20f).row();
        root.add(back).width(200f).height(48f).pad(8f).row();

        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
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
        stage.dispose();
        skin.dispose();
    }
}
