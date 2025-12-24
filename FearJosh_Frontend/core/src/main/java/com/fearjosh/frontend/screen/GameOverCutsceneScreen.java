package com.fearjosh.frontend.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.systems.AudioManager;
import com.fearjosh.frontend.systems.KeyManager;

public class GameOverCutsceneScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;

    // Scene timing
    private static final float SCENE_G1_DURATION = 2.0f;
    private static final float SCENE_G2_DURATION = 2.0f;
    private static final float CAUGHT_SCREEN_DURATION = 1.5f;
    private static final float GAME_OVER_SCREEN_DURATION = 1.5f;
    
    // Fade timing
    private static final float FADE_IN_DURATION = 0.3f;
    private static final float FADE_OUT_DURATION = 1.0f;

    private final FearJosh game;

    private OrthographicCamera camera;
    private FitViewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Texture sceneG1Texture;
    private Texture sceneG2Texture;
    private Texture caughtTexture;
    private Texture gameOverTexture;

    private int currentScene = 0;
    private float sceneTimer = 0f;
    private float fadeAlpha = 1f;

    private boolean canSkip = false;
    private float skipCooldown = 0.5f;

    public GameOverCutsceneScreen(FearJosh game) {
        this.game = game;
        
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        
        loadTextures();
    }

    private void loadTextures() {
        try {
            if (Gdx.files.internal("Cutscene/1/1_g1.png").exists()) {
                sceneG1Texture = new Texture(Gdx.files.internal("Cutscene/1/1_g1.png"));
                System.out.println("[GameOverCutscene] Loaded 1_g1.png");
            }
            if (Gdx.files.internal("Cutscene/1/1_g2.png").exists()) {
                sceneG2Texture = new Texture(Gdx.files.internal("Cutscene/1/1_g2.png"));
                System.out.println("[GameOverCutscene] Loaded 1_g2.png");
            }
            if (Gdx.files.internal("General/josh_caught_you.png").exists()) {
                caughtTexture = new Texture(Gdx.files.internal("General/josh_caught_you.png"));
            } else if (Gdx.files.internal("Cutscene/1/josh_caught_you.png").exists()) {
                caughtTexture = new Texture(Gdx.files.internal("Cutscene/1/josh_caught_you.png"));
            }
            if (Gdx.files.internal("General/game_over.png").exists()) {
                gameOverTexture = new Texture(Gdx.files.internal("General/game_over.png"));
            } else if (Gdx.files.internal("Cutscene/1/game_over.png").exists()) {
                gameOverTexture = new Texture(Gdx.files.internal("Cutscene/1/game_over.png"));
            }
            
            System.out.println("[GameOverCutscene] Textures loaded");
        } catch (Exception e) {
            System.err.println("[GameOverCutscene] Failed to load some textures: " + e.getMessage());
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);
        
        AudioManager.getInstance().stopMusic();
        
        GameManager.getInstance().setCurrentState(GameManager.GameState.GAME_OVER);
        
        currentScene = 0;
        sceneTimer = 0f;
        fadeAlpha = 1f;
        
        if (Gdx.files.internal("Audio/Music/game_over_music.wav").exists()) {
            AudioManager.getInstance().playMusic("Audio/Music/game_over_music.wav", false);
        }
        
        System.out.println("[GameOverCutscene] Game over sequence started");
    }

    @Override
    public void render(float delta) {
        if (skipCooldown > 0) {
            skipCooldown -= delta;
        } else {
            canSkip = true;
        }

        if (canSkip && (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || 
                        Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))) {
            skipToEnd();
            return;
        }

        updateScene(delta);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        batch.begin();
        Texture currentTexture = getCurrentSceneTexture();
        if (currentTexture != null) {
            batch.draw(currentTexture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        } else {
            drawFallbackScreen();
        }
        batch.end();

        if (fadeAlpha > 0) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, fadeAlpha);
            shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        if (canSkip && currentScene < 5) {
            batch.begin();
            font.draw(batch, "Press SPACE to skip", 20, 30);
            batch.end();
        }
    }

    private void drawFallbackScreen() {
        String text;
        switch (currentScene) {
            case 1:
                text = "Josh menangkapmu...";
                break;
            case 2:
                text = "Kamu tidak bisa melarikan diri...";
                break;
            case 3:
                text = "TERTANGKAP";
                break;
            case 4:
            case 5:
                text = "GAME OVER";
                break;
            default:
                text = "";
        }
        
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, text);
        float x = (VIRTUAL_WIDTH - layout.width) / 2f;
        float y = VIRTUAL_HEIGHT / 2f;
        font.draw(batch, text, x, y);
    }

    private void updateScene(float delta) {
        sceneTimer += delta;

        switch (currentScene) {
            case 0:
                fadeAlpha = 1f - (sceneTimer / FADE_IN_DURATION);
                if (sceneTimer >= FADE_IN_DURATION) {
                    fadeAlpha = 0f;
                    currentScene = 1;
                    sceneTimer = 0f;
                }
                break;

            case 1:
                if (sceneTimer >= SCENE_G1_DURATION) {
                    currentScene = 2;
                    sceneTimer = 0f;
                }
                break;

            case 2:
                if (sceneTimer >= SCENE_G2_DURATION) {
                    currentScene = 3;
                    sceneTimer = 0f;
                }
                break;

            case 3:
                if (sceneTimer >= CAUGHT_SCREEN_DURATION) {
                    currentScene = 4;
                    sceneTimer = 0f;
                }
                break;

            case 4:
                if (sceneTimer >= GAME_OVER_SCREEN_DURATION) {
                    currentScene = 5;
                    sceneTimer = 0f;
                }
                break;

            case 5:
                fadeAlpha = sceneTimer / FADE_OUT_DURATION;
                if (sceneTimer >= FADE_OUT_DURATION) {
                    fadeAlpha = 1f;
                    currentScene = 6;
                    finishGameOver();
                }
                break;
        }
    }

    private Texture getCurrentSceneTexture() {
        switch (currentScene) {
            case 0:
            case 1:
                return sceneG1Texture;
            case 2:
                return sceneG2Texture;
            case 3:
                return caughtTexture;
            case 4:
            case 5:
                return gameOverTexture;
            default:
                return null;
        }
    }

    private void skipToEnd() {
        System.out.println("[GameOverCutscene] Skipped by player");
        finishGameOver();
    }

    private void finishGameOver() {
        AudioManager.getInstance().stopMusic();
        
        resetGameState();
        
        System.out.println("[GameOverCutscene] Returning to main menu");
        game.setScreen(new MainMenuScreen(game));
    }

    private void resetGameState() {
        GameManager gm = GameManager.getInstance();
        
        if (gm.getCurrentSession() != null) {
            gm.getCurrentSession().setActive(false);
        }
        
        gm.resetInventory();
        KeyManager.getInstance().reset();
        
        gm.setCurrentState(GameManager.GameState.MAIN_MENU);
        
        System.out.println("[GameOverCutscene] Game state reset for new game");
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        
        if (sceneG1Texture != null) sceneG1Texture.dispose();
        if (sceneG2Texture != null) sceneG2Texture.dispose();
        if (caughtTexture != null) caughtTexture.dispose();
        if (gameOverTexture != null) gameOverTexture.dispose();
    }
}
