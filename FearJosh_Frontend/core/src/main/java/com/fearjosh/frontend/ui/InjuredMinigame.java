package com.fearjosh.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.fearjosh.frontend.systems.AudioManager;

public class InjuredMinigame {
    
    // State
    private boolean active = false;
    private boolean isTutorialMode = false;
    private float progress = 0f;
    private int spacebarPresses = 0;
    private float timer = 0f;
    
    // Position
    private float worldX = 0f;
    private float worldY = 0f;
    
    // Constants
    private static final float PROGRESS_PER_PRESS = 0.04f;
    private static final float PROGRESS_DECAY = 0.02f;
    private static final float TIME_LIMIT = 15.0f;
    private static final float TUTORIAL_TIME_LIMIT = 30.0f;
    private static final int REQUIRED_PRESSES = 25;
    
    // World visuals
    private static final float WORLD_BAR_WIDTH = 80f;
    private static final float WORLD_BAR_HEIGHT = 8f;
    private static final float INJURED_SPRITE_SIZE = 48f;
    
    // UI visuals
    private static final float BAR_WIDTH = 400f;
    private static final float BAR_HEIGHT = 40f;
    private static final float SPRITE_SIZE = 90f;
    
    // Textures
    private Texture injuredSprite;
    private Texture bandageIcon;
    
    // Callbacks
    private Runnable onComplete;
    private Runnable onFail;
    
    // Animation
    private float shakeTimer = 0f;
    private float pulseTimer = 0f;
    
    public InjuredMinigame() {
        try {
            if (Gdx.files.internal("Sprite/Player/jonatan_injured.png").exists()) {
                injuredSprite = new Texture("Sprite/Player/jonatan_injured.png");
            }
        } catch (Exception e) {
            System.err.println("[InjuredMinigame] Could not load injured sprite: " + e.getMessage());
        }
    }
    
    public void startAtPosition(Runnable onComplete, Runnable onFail, float playerX, float playerY) {
        startAtPosition(onComplete, onFail, playerX, playerY, false);
    }
    
    public void startAtPosition(Runnable onComplete, Runnable onFail, float playerX, float playerY, boolean tutorialMode) {
        this.worldX = playerX;
        this.worldY = playerY;
        start(onComplete, onFail, tutorialMode);
        System.out.println("[InjuredMinigame] Started at world position (" + playerX + ", " + playerY + ")");
    }
    
    public void start(Runnable onComplete, Runnable onFail) {
        start(onComplete, onFail, false);
    }
    
    public void start(Runnable onComplete, Runnable onFail, boolean tutorialMode) {
        this.active = true;
        this.isTutorialMode = tutorialMode;
        this.progress = 0f;
        this.spacebarPresses = 0;
        this.timer = 0f;
        this.onComplete = onComplete;
        this.onFail = onFail;
        this.shakeTimer = 0f;
        this.pulseTimer = 0f;
        
        if (tutorialMode) {
            System.out.println("[InjuredMinigame] TUTORIAL MODE - Mash SPACEBAR to bandage yourself!");
        } else {
            System.out.println("[InjuredMinigame] Started! Mash SPACEBAR to bandage yourself!");
        }
    }
    
    public void update(float delta) {
        if (!active) return;
        
        timer += delta;
        shakeTimer += delta * 10f;
        pulseTimer += delta * 3f;
        
        // Decay
        float decayRate = isTutorialMode ? PROGRESS_DECAY * 0.5f : PROGRESS_DECAY;
        progress -= decayRate * delta;
        if (progress < 0f) progress = 0f;
        
        // Time limit
        float timeLimit = isTutorialMode ? TUTORIAL_TIME_LIMIT : TIME_LIMIT;
        if (timer >= timeLimit) {
            fail();
        }
    }
    
    public void onSpacebarPressed() {
        if (!active) return;
        
        spacebarPresses++;
        progress += PROGRESS_PER_PRESS;
        shakeTimer = 0f;
        
        System.out.println("[InjuredMinigame] Progress: " + (int)(progress * 100) + "% (" + spacebarPresses + "/" + REQUIRED_PRESSES + ")");
        
        if (progress >= 1f) {
            complete();
        }
    }
    
    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font,
                       float screenWidth, float screenHeight) {
        if (!active) return;
        
        // Shake
        float shakeX = (float) Math.sin(shakeTimer) * 3f * Math.max(0, 1f - shakeTimer * 0.5f);
        float shakeY = (float) Math.cos(shakeTimer * 1.3f) * 2f * Math.max(0, 1f - shakeTimer * 0.5f);
        
        // Overlay
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.85f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();
        
        // Sprite
        if (injuredSprite != null) {
            batch.begin();
            float spriteX = (screenWidth - SPRITE_SIZE) / 2f + shakeX;
            float spriteY = screenHeight * 0.55f + shakeY;
            batch.draw(injuredSprite, spriteX, spriteY, SPRITE_SIZE, SPRITE_SIZE);
            batch.end();
        }
        
        // Progress bar
        float barX = (screenWidth - BAR_WIDTH) / 2f;
        float barY = screenHeight * 0.35f;
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);
        shapeRenderer.rect(barX - 4f, barY - 4f, BAR_WIDTH + 8f, BAR_HEIGHT + 8f);
        shapeRenderer.end();
        
        // Fill
        float fillWidth = BAR_WIDTH * Math.min(progress, 1f);
        Color barColor = getProgressColor(progress);
        
        // Pulse
        float pulse = (float) Math.sin(pulseTimer) * 0.1f + 1f;
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(barColor);
        shapeRenderer.rect(barX, barY, fillWidth * pulse, BAR_HEIGHT);
        shapeRenderer.end();
        
        // Border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3f);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(barX, barY, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1f);
        
        // Text
        batch.begin();
        
        // Title
        font.setColor(isTutorialMode ? Color.CYAN : Color.RED);
        font.getData().setScale(1.5f);
        String title = isTutorialMode ? "TUTORIAL" : "TERLUKA!";
        GlyphLayout layout = new GlyphLayout(font, title);
        font.draw(batch, title, (screenWidth - layout.width) / 2f, screenHeight * 0.85f);
        
        if (isTutorialMode) {
            font.setColor(Color.YELLOW);
            font.getData().setScale(1f);
            String tutorialHint = "Kamu terluka! Pelajari cara membalut diri...";
            layout.setText(font, tutorialHint);
            font.draw(batch, tutorialHint, (screenWidth - layout.width) / 2f, screenHeight * 0.78f);
        }
        
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
        String instruction = "Tekan SPACEBAR untuk membalut luka!";
        layout.setText(font, instruction);
        font.draw(batch, instruction, (screenWidth - layout.width) / 2f, screenHeight * 0.48f);
        
        font.getData().setScale(1.3f);
        font.setColor(barColor);
        String progressText = (int)(progress * 100) + "%";
        layout.setText(font, progressText);
        font.draw(batch, progressText, (screenWidth - layout.width) / 2f, barY - 15f);
        
        float timeLimit = isTutorialMode ? TUTORIAL_TIME_LIMIT : TIME_LIMIT;
        font.getData().setScale(1f);
        font.setColor(timer > timeLimit * 0.7f ? Color.RED : Color.YELLOW);
        String timerText = "Waktu: " + String.format("%.1f", timeLimit - timer) + "s";
        layout.setText(font, timerText);
        font.draw(batch, timerText, (screenWidth - layout.width) / 2f, screenHeight * 0.25f);
        
        font.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
        font.getData().setScale(0.9f);
        String pressText = "(" + spacebarPresses + " kali tekan)";
        layout.setText(font, pressText);
        font.draw(batch, pressText, (screenWidth - layout.width) / 2f, screenHeight * 0.2f);
        
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
    
    public void renderInWorld(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        if (!active) return;
        
        // Progress bar
        float barX = worldX + (INJURED_SPRITE_SIZE - WORLD_BAR_WIDTH) / 2f;
        float barY = worldY + INJURED_SPRITE_SIZE + 10f;
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.9f);
        shapeRenderer.rect(barX - 2f, barY - 2f, WORLD_BAR_WIDTH + 4f, WORLD_BAR_HEIGHT + 4f);
        shapeRenderer.end();
        
        // Fill
        float fillWidth = WORLD_BAR_WIDTH * Math.min(progress, 1f);
        Color barColor = getProgressColor(progress);
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(barColor);
        shapeRenderer.rect(barX, barY, fillWidth, WORLD_BAR_HEIGHT);
        shapeRenderer.end();
        
        // Border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(barX, barY, WORLD_BAR_WIDTH, WORLD_BAR_HEIGHT);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1f);
        
        batch.begin();
        font.getData().setScale(0.5f);
        font.setColor(barColor);
        String progressText = (int)(progress * 100) + "%";
        GlyphLayout layout = new GlyphLayout(font, progressText);
        font.draw(batch, progressText, barX + (WORLD_BAR_WIDTH - layout.width) / 2f, barY + WORLD_BAR_HEIGHT + 15f);
        
        float timeLimit = isTutorialMode ? TUTORIAL_TIME_LIMIT : TIME_LIMIT;
        font.setColor(timer > timeLimit * 0.7f ? Color.RED : Color.YELLOW);
        font.getData().setScale(0.4f);
        String timerText = String.format("%.1fs", timeLimit - timer);
        layout.setText(font, timerText);
        font.draw(batch, timerText, barX + (WORLD_BAR_WIDTH - layout.width) / 2f, barY + WORLD_BAR_HEIGHT + 28f);
        
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
    
    public void renderUIOverlay(SpriteBatch batch, BitmapFont font, float screenWidth, float screenHeight) {
        if (!active) return;
        
        batch.begin();
        
        font.setColor(isTutorialMode ? Color.CYAN : Color.RED);
        font.getData().setScale(1.2f);
        String title = isTutorialMode ? "TUTORIAL - TERLUKA!" : "TERLUKA!";
        GlyphLayout layout = new GlyphLayout(font, title);
        font.draw(batch, title, (screenWidth - layout.width) / 2f, screenHeight - 30f);
        
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
        String instruction = "Tekan SPACEBAR untuk membalut luka!";
        layout.setText(font, instruction);
        font.draw(batch, instruction, (screenWidth - layout.width) / 2f, screenHeight - 60f);
        
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
    
    public float getWorldX() {
        return worldX;
    }
    
    public float getWorldY() {
        return worldY;
    }
    
    private Color getProgressColor(float progress) {
        if (progress < 0.5f) {
            float t = progress * 2f;
            return new Color(1f, t, 0f, 1f);
        } else {
            float t = (progress - 0.5f) * 2f;
            return new Color(1f - t, 1f, 0f, 1f);
        }
    }
    
    private void complete() {
        active = false;
        System.out.println("[InjuredMinigame] SUCCESS! Player bandaged themselves.");
        
        try {
            AudioManager.getInstance().playSound("Audio/Effect/heal_sound.wav");
        } catch (Exception e) {
        }
        
        if (onComplete != null) {
            onComplete.run();
        }
    }
    
    private void fail() {
        active = false;
        System.out.println("[InjuredMinigame] FAILED! Time ran out.");
        
        if (onFail != null) {
            onFail.run();
        }
    }
    
    public boolean isActive() {
        return active;
    }
    
    public float getProgress() {
        return progress;
    }
    
    public void skip() {
        if (active) {
            progress = 1f;
            complete();
        }
    }
    
    public void dispose() {
        if (injuredSprite != null) {
            injuredSprite.dispose();
            injuredSprite = null;
        }
        if (bandageIcon != null) {
            bandageIcon.dispose();
            bandageIcon = null;
        }
    }
}
