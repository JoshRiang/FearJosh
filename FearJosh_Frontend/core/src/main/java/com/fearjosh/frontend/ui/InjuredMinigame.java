package com.fearjosh.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.fearjosh.frontend.systems.AudioManager;

/**
 * Injured Minigame - Spacebar mashing to bandage yourself after being caught by Josh.
 * 
 * When Jonatan is caught by Josh:
 * 1. He gets knocked unconscious (not tied up)
 * 2. Player must mash SPACEBAR to "bandage" themselves
 * 3. Progress bar fills up from red to green
 * 4. After completing, player loses 1 heart but can continue
 * 
 * This minigame renders IN-PLACE where the player was caught (world space),
 * NOT in a separate fullscreen overlay.
 */
public class InjuredMinigame {
    
    // Minigame state
    private boolean active = false;
    private boolean isTutorialMode = false; // Tutorial mode - no time limit penalty
    private float progress = 0f; // 0.0 to 1.0
    private int spacebarPresses = 0;
    private float timer = 0f;
    
    // World position where player was caught
    private float worldX = 0f;
    private float worldY = 0f;
    
    // Constants
    private static final float PROGRESS_PER_PRESS = 0.04f; // 4% per press (25 presses to complete)
    private static final float PROGRESS_DECAY = 0.02f; // 2% decay per second
    private static final float TIME_LIMIT = 15.0f; // 15 seconds to complete
    private static final float TUTORIAL_TIME_LIMIT = 30.0f; // More time in tutorial
    private static final int REQUIRED_PRESSES = 25; // Target presses
    
    // Visual constants for in-world rendering
    private static final float WORLD_BAR_WIDTH = 80f;    // Progress bar width in world units
    private static final float WORLD_BAR_HEIGHT = 8f;    // Progress bar height in world units
    private static final float INJURED_SPRITE_SIZE = 48f; // Player sprite size in world
    
    // Visual constants for UI overlay
    private static final float BAR_WIDTH = 400f;
    private static final float BAR_HEIGHT = 40f;
    private static final float SPRITE_SIZE = 90f; // Injured sprite render size (smaller for better visibility)
    
    // Textures
    private Texture injuredSprite;
    private Texture bandageIcon;
    
    // Completion callback
    private Runnable onComplete;
    private Runnable onFail;
    
    // Animation
    private float shakeTimer = 0f;
    private float pulseTimer = 0f;
    
    public InjuredMinigame() {
        // Load textures
        try {
            if (Gdx.files.internal("Sprite/Player/jonatan_injured.png").exists()) {
                injuredSprite = new Texture("Sprite/Player/jonatan_injured.png");
            }
        } catch (Exception e) {
            System.err.println("[InjuredMinigame] Could not load injured sprite: " + e.getMessage());
        }
    }
    
    /**
     * Start the minigame at player's current position (for when caught)
     * @param onComplete Called when player successfully completes the minigame
     * @param onFail Called when player fails (time runs out)
     * @param playerX World X position where player was caught
     * @param playerY World Y position where player was caught
     */
    public void startAtPosition(Runnable onComplete, Runnable onFail, float playerX, float playerY) {
        startAtPosition(onComplete, onFail, playerX, playerY, false);
    }
    
    /**
     * Start the minigame at player's current position with optional tutorial mode
     * @param onComplete Called when player successfully completes the minigame
     * @param onFail Called when player fails (time runs out)
     * @param playerX World X position where player was caught
     * @param playerY World Y position where player was caught
     * @param tutorialMode If true, more time and shows tutorial text
     */
    public void startAtPosition(Runnable onComplete, Runnable onFail, float playerX, float playerY, boolean tutorialMode) {
        this.worldX = playerX;
        this.worldY = playerY;
        start(onComplete, onFail, tutorialMode);
        System.out.println("[InjuredMinigame] Started at world position (" + playerX + ", " + playerY + ")");
    }
    
    /**
     * Start the minigame (UI overlay mode - for tutorial)
     * @param onComplete Called when player successfully completes the minigame
     * @param onFail Called when player fails (time runs out)
     */
    public void start(Runnable onComplete, Runnable onFail) {
        start(onComplete, onFail, false);
    }
    
    /**
     * Start the minigame with optional tutorial mode (UI overlay mode)
     * @param onComplete Called when player successfully completes the minigame
     * @param onFail Called when player fails (time runs out)
     * @param tutorialMode If true, more time and shows tutorial text
     */
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
    
    /**
     * Update the minigame state
     * Note: Input handling is done externally via onSpacebarPressed()
     */
    public void update(float delta) {
        if (!active) return;
        
        timer += delta;
        shakeTimer += delta * 10f;
        pulseTimer += delta * 3f;
        
        // Progress decay (makes it harder) - slower in tutorial mode
        float decayRate = isTutorialMode ? PROGRESS_DECAY * 0.5f : PROGRESS_DECAY;
        progress -= decayRate * delta;
        if (progress < 0f) progress = 0f;
        
        // Check time limit (longer in tutorial mode)
        float timeLimit = isTutorialMode ? TUTORIAL_TIME_LIMIT : TIME_LIMIT;
        if (timer >= timeLimit) {
            fail();
        }
    }
    
    /**
     * Called when player presses spacebar (from PlayScreen input handler)
     */
    public void onSpacebarPressed() {
        if (!active) return;
        
        spacebarPresses++;
        progress += PROGRESS_PER_PRESS;
        
        // Add shake effect
        shakeTimer = 0f;
        
        System.out.println("[InjuredMinigame] Progress: " + (int)(progress * 100) + "% (" + spacebarPresses + "/" + REQUIRED_PRESSES + ")");
        
        // Check completion
        if (progress >= 1f) {
            complete();
        }
    }
    
    /**
     * Render the minigame overlay
     */
    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font,
                       float screenWidth, float screenHeight) {
        if (!active) return;
        
        // Calculate shake offset
        float shakeX = (float) Math.sin(shakeTimer) * 3f * Math.max(0, 1f - shakeTimer * 0.5f);
        float shakeY = (float) Math.cos(shakeTimer * 1.3f) * 2f * Math.max(0, 1f - shakeTimer * 0.5f);
        
        // Dark overlay background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.85f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();
        
        // Draw injured sprite (centered, upper area)
        if (injuredSprite != null) {
            batch.begin();
            float spriteX = (screenWidth - SPRITE_SIZE) / 2f + shakeX;
            float spriteY = screenHeight * 0.55f + shakeY;
            batch.draw(injuredSprite, spriteX, spriteY, SPRITE_SIZE, SPRITE_SIZE);
            batch.end();
        }
        
        // Progress bar background
        float barX = (screenWidth - BAR_WIDTH) / 2f;
        float barY = screenHeight * 0.35f;
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);
        shapeRenderer.rect(barX - 4f, barY - 4f, BAR_WIDTH + 8f, BAR_HEIGHT + 8f);
        shapeRenderer.end();
        
        // Progress bar fill (red to green gradient based on progress)
        float fillWidth = BAR_WIDTH * Math.min(progress, 1f);
        Color barColor = getProgressColor(progress);
        
        // Pulse effect when pressing
        float pulse = (float) Math.sin(pulseTimer) * 0.1f + 1f;
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(barColor);
        shapeRenderer.rect(barX, barY, fillWidth * pulse, BAR_HEIGHT);
        shapeRenderer.end();
        
        // Progress bar border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3f);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(barX, barY, BAR_WIDTH, BAR_HEIGHT);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1f);
        
        // Text instructions
        batch.begin();
        
        // Title - different for tutorial
        font.setColor(isTutorialMode ? Color.CYAN : Color.RED);
        font.getData().setScale(1.5f);
        String title = isTutorialMode ? "TUTORIAL" : "TERLUKA!";
        GlyphLayout layout = new GlyphLayout(font, title);
        font.draw(batch, title, (screenWidth - layout.width) / 2f, screenHeight * 0.85f);
        
        // Tutorial subtitle
        if (isTutorialMode) {
            font.setColor(Color.YELLOW);
            font.getData().setScale(1f);
            String tutorialHint = "Kamu terluka! Pelajari cara membalut diri...";
            layout.setText(font, tutorialHint);
            font.draw(batch, tutorialHint, (screenWidth - layout.width) / 2f, screenHeight * 0.78f);
        }
        
        // Instruction
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
        String instruction = "Tekan SPACEBAR untuk membalut luka!";
        layout.setText(font, instruction);
        font.draw(batch, instruction, (screenWidth - layout.width) / 2f, screenHeight * 0.48f);
        
        // Progress percentage
        font.getData().setScale(1.3f);
        font.setColor(barColor);
        String progressText = (int)(progress * 100) + "%";
        layout.setText(font, progressText);
        font.draw(batch, progressText, (screenWidth - layout.width) / 2f, barY - 15f);
        
        // Timer
        float timeLimit = isTutorialMode ? TUTORIAL_TIME_LIMIT : TIME_LIMIT;
        font.getData().setScale(1f);
        font.setColor(timer > timeLimit * 0.7f ? Color.RED : Color.YELLOW);
        String timerText = "Waktu: " + String.format("%.1f", timeLimit - timer) + "s";
        layout.setText(font, timerText);
        font.draw(batch, timerText, (screenWidth - layout.width) / 2f, screenHeight * 0.25f);
        
        // Press count
        font.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
        font.getData().setScale(0.9f);
        String pressText = "(" + spacebarPresses + " kali tekan)";
        layout.setText(font, pressText);
        font.draw(batch, pressText, (screenWidth - layout.width) / 2f, screenHeight * 0.2f);
        
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
    
    /**
     * Render the minigame IN WORLD SPACE - player stays in position
     * The progress bar is rendered at the player's world position
     * Note: The injured sprite is now rendered by the Player class via InjuredState
     * 
     * @param shapeRenderer ShapeRenderer for progress bar
     * @param batch SpriteBatch for sprites and text
     * @param font BitmapFont for text
     */
    public void renderInWorld(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        if (!active) return;
        
        // NOTE: The injured sprite is now rendered by the Player class via InjuredState
        // We only render the progress bar and UI elements here
        
        // Progress bar above player
        float barX = worldX + (INJURED_SPRITE_SIZE - WORLD_BAR_WIDTH) / 2f;
        float barY = worldY + INJURED_SPRITE_SIZE + 10f;
        
        // Progress bar background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.9f);
        shapeRenderer.rect(barX - 2f, barY - 2f, WORLD_BAR_WIDTH + 4f, WORLD_BAR_HEIGHT + 4f);
        shapeRenderer.end();
        
        // Progress bar fill
        float fillWidth = WORLD_BAR_WIDTH * Math.min(progress, 1f);
        Color barColor = getProgressColor(progress);
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(barColor);
        shapeRenderer.rect(barX, barY, fillWidth, WORLD_BAR_HEIGHT);
        shapeRenderer.end();
        
        // Progress bar border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(barX, barY, WORLD_BAR_WIDTH, WORLD_BAR_HEIGHT);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1f);
        
        // Small text above bar showing progress
        batch.begin();
        font.getData().setScale(0.5f);
        font.setColor(barColor);
        String progressText = (int)(progress * 100) + "%";
        GlyphLayout layout = new GlyphLayout(font, progressText);
        font.draw(batch, progressText, barX + (WORLD_BAR_WIDTH - layout.width) / 2f, barY + WORLD_BAR_HEIGHT + 15f);
        
        // Timer above progress
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
    
    /**
     * Render UI overlay for instructions (call with UI camera projection)
     */
    public void renderUIOverlay(SpriteBatch batch, BitmapFont font, float screenWidth, float screenHeight) {
        if (!active) return;
        
        batch.begin();
        
        // Instruction text at top of screen
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
    
    /**
     * Get the world X position
     */
    public float getWorldX() {
        return worldX;
    }
    
    /**
     * Get the world Y position
     */
    public float getWorldY() {
        return worldY;
    }
    
    /**
     * Get color interpolated between red and green based on progress
     */
    private Color getProgressColor(float progress) {
        // Red (0%) -> Yellow (50%) -> Green (100%)
        if (progress < 0.5f) {
            float t = progress * 2f;
            return new Color(1f, t, 0f, 1f); // Red to Yellow
        } else {
            float t = (progress - 0.5f) * 2f;
            return new Color(1f - t, 1f, 0f, 1f); // Yellow to Green
        }
    }
    
    private void complete() {
        active = false;
        System.out.println("[InjuredMinigame] SUCCESS! Player bandaged themselves.");
        
        // Play success sound
        try {
            AudioManager.getInstance().playSound("Audio/Effect/heal_sound.wav");
        } catch (Exception e) {
            // Fallback
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
    
    /**
     * Force skip the minigame (for testing)
     */
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
