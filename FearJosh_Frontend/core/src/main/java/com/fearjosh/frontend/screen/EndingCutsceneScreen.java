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

public class EndingCutsceneScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;

    // Scene timing
    private static final float SCENE_1_DURATION = 2.5f;
    private static final float SCENE_2_DURATION = 2.5f;
    private static final float SCENE_3_DURATION = 2.5f;
    private static final float DOOR_OPEN_DELAY = 1.0f;
    private static final float RUNNING_SOUND_DELAY = 1.0f;
    
    // Fade timing
    private static final float FADE_IN_DURATION = 0.5f;
    private static final float FADE_OUT_DURATION = 1.5f;

    private final FearJosh game;

    private OrthographicCamera camera;
    private FitViewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Texture scene1Texture;
    private Texture scene2Texture;
    private Texture scene3Texture;

    private int currentScene = 0;
    private float sceneTimer = 0f;
    private float fadeAlpha = 1f;
    
    private long breathingSoundId = -1;
    private long rainSoundId = -1;
    private long runningSoundId = -1;
    private boolean doorOpenPlayed = false;
    private boolean runningStarted = false;

    private boolean canSkip = false;
    private float skipCooldown = 0.5f;

    public EndingCutsceneScreen(FearJosh game) {
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
            if (Gdx.files.internal("Cutscene/1/1_1.png").exists()) {
                scene1Texture = new Texture(Gdx.files.internal("Cutscene/1/1_1.png"));
            }
            if (Gdx.files.internal("Cutscene/1/1_2.png").exists()) {
                scene2Texture = new Texture(Gdx.files.internal("Cutscene/1/1_2.png"));
            }
            if (Gdx.files.internal("Cutscene/1/1_3.png").exists()) {
                scene3Texture = new Texture(Gdx.files.internal("Cutscene/1/1_3.png"));
            }
            System.out.println("[EndingCutscene] Textures loaded successfully");
        } catch (Exception e) {
            System.err.println("[EndingCutscene] Failed to load some textures: " + e.getMessage());
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);
        
        AudioManager.getInstance().stopMusic();
        
        GameManager.getInstance().setCurrentState(GameManager.GameState.ENDING);
        
        currentScene = 0;
        sceneTimer = 0f;
        fadeAlpha = 1f;
        
        System.out.println("[EndingCutscene] Escape ending started");
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

        if (canSkip) {
            batch.begin();
            font.draw(batch, "Press SPACE to skip", 20, 30);
            batch.end();
        }
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
                    startScene1Audio();
                }
                break;

            case 1:
                updateScene1Audio(sceneTimer);
                if (sceneTimer >= SCENE_1_DURATION) {
                    currentScene = 2;
                    sceneTimer = 0f;
                    startScene2Audio();
                }
                break;

            case 2:
                updateScene2Audio(sceneTimer);
                if (sceneTimer >= SCENE_2_DURATION) {
                    currentScene = 3;
                    sceneTimer = 0f;
                }
                break;

            case 3:
                if (sceneTimer >= SCENE_3_DURATION) {
                    currentScene = 4;
                    sceneTimer = 0f;
                }
                break;

            case 4:
                fadeAlpha = sceneTimer / FADE_OUT_DURATION;
                if (sceneTimer >= FADE_OUT_DURATION) {
                    fadeAlpha = 1f;
                    currentScene = 5;
                    finishEnding();
                }
                break;
        }
    }

    private Texture getCurrentSceneTexture() {
        switch (currentScene) {
            case 0:
            case 1:
                return scene1Texture;
            case 2:
                return scene2Texture;
            case 3:
            case 4:
                return scene3Texture;
            default:
                return null;
        }
    }

    private void startScene1Audio() {
        AudioManager audio = AudioManager.getInstance();
        
        breathingSoundId = audio.loopSound("Audio/Effect/monster_grunt_sound_effect.wav");
        System.out.println("[EndingCutscene] Started breathing sound");
    }

    private void updateScene1Audio(float elapsed) {
        if (!doorOpenPlayed && elapsed >= DOOR_OPEN_DELAY) {
            AudioManager.getInstance().playSound("Audio/Effect/cut_rope_sound_effect.wav");
            doorOpenPlayed = true;
            System.out.println("[EndingCutscene] Door opening sound played");
        }
    }

    private void startScene2Audio() {
        AudioManager audio = AudioManager.getInstance();
        
        rainSoundId = audio.loopSound("Audio/Effect/rain_sound_effect.wav");
        System.out.println("[EndingCutscene] Started rain sound");
    }

    private void updateScene2Audio(float elapsed) {
        if (!runningStarted && elapsed >= RUNNING_SOUND_DELAY) {
            runningSoundId = AudioManager.getInstance().loopSound("Audio/Effect/footstep_sound_effect.wav");
            runningStarted = true;
            System.out.println("[EndingCutscene] Started running sound");
        }
    }

    private void stopAllAudio() {
        AudioManager audio = AudioManager.getInstance();
        
        if (breathingSoundId != -1) {
            audio.stopSound("Audio/Effect/monster_grunt_sound_effect.wav", breathingSoundId);
            breathingSoundId = -1;
        }
        if (rainSoundId != -1) {
            audio.stopSound("Audio/Effect/rain_sound_effect.wav", rainSoundId);
            rainSoundId = -1;
        }
        if (runningSoundId != -1) {
            audio.stopSound("Audio/Effect/footstep_sound_effect.wav", runningSoundId);
            runningSoundId = -1;
        }
        
        System.out.println("[EndingCutscene] All audio stopped");
    }

    private void skipToEnd() {
        System.out.println("[EndingCutscene] Skipped by player");
        finishEnding();
    }

    private void finishEnding() {
        stopAllAudio();
        
        // Calculate elapsed time
        long completionTimeSeconds = GameManager.getInstance().getElapsedTimeSeconds();
        
        System.out.println("[EndingCutscene] Going to leaderboard - Time: " + completionTimeSeconds + "s");
        
        // Go to leaderboard screen with ESCAPE ending type (score will be submitted)
        game.setScreen(new LeaderboardScreen(game, LeaderboardScreen.EndingType.ESCAPE, completionTimeSeconds));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        stopAllAudio();
    }

    @Override
    public void dispose() {
        stopAllAudio();
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        
        if (scene1Texture != null) scene1Texture.dispose();
        if (scene2Texture != null) scene2Texture.dispose();
        if (scene3Texture != null) scene3Texture.dispose();
    }
}
