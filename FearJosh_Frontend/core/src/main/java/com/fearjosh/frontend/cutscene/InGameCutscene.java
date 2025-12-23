package com.fearjosh.frontend.cutscene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.systems.AudioManager;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game cutscene system for events that happen within the game world.
 * Unlike storyboard cutscenes, these keep the player in the world but lock controls.
 * 
 * Used for:
 * - First Josh encounter in the gym
 * - Special story moments
 */
@SuppressWarnings("unused") // Some fields reserved for future cutscene features (Josh appearance animation, phases)
public class InGameCutscene {

    public enum CutsceneType {
        GYM_ENCOUNTER,      // First meeting with Josh
        OBJECTIVE_CHANGE,   // After waking up - objective changes
        ESCAPE_SUCCESS,     // Player escapes successfully
        CUSTOM              // Custom cutscene
    }

    // Cutscene state
    private boolean isActive;
    private CutsceneType currentType;
    private int currentPhase;
    private float phaseTimer;
    private boolean isComplete;

    // Dialog system
    private List<DialogLine> dialogLines;
    private int currentDialogIndex;
    private String currentDisplayText;
    private float textRevealTimer;
    private static final float TEXT_REVEAL_SPEED = 0.03f; // Seconds per character
    private boolean textFullyRevealed;

    // Visual effects
    private float fadeAlpha;
    private float shakeAmount;
    private float shakeTimer;
    
    // Josh appearance animation for GYM_ENCOUNTER
    private float joshX, joshY;
    private float joshTargetX, joshTargetY;
    private float joshAlpha;
    private boolean joshVisible;
    private float joshAnimTimer;
    private static final float JOSH_SPRITE_SIZE = 150f; // Fixed render size (not raw file size)
    private static final float JOSH_FACE_SIZE = 200f; // Josh face during scary reveal

    // Textures (lazy loaded)
    private Texture injuredTexture;
    private Texture joshFaceTexture;
    private Texture joshSpriteTexture; // Josh sprite sheet for animation
    
    // Josh sprite sheet animation (4 frames)
    private Animation<TextureRegion> joshAnimation;
    private static final int JOSH_SPRITE_FRAMES = 4;
    private static final float JOSH_ANIM_FRAME_DURATION = 0.15f;

    // Callbacks
    private Runnable onComplete;

    public InGameCutscene() {
        isActive = false;
        isComplete = false;
        dialogLines = new ArrayList<>();
        currentDialogIndex = 0;
        currentDisplayText = "";
        textRevealTimer = 0f;
        textFullyRevealed = false;
        fadeAlpha = 0f;
        shakeAmount = 0f;
        shakeTimer = 0f;
        joshAlpha = 0f;
        joshVisible = false;
    }

    /**
     * Start a predefined cutscene
     */
    public void startCutscene(CutsceneType type, Runnable onComplete) {
        this.currentType = type;
        this.onComplete = onComplete;
        this.isActive = true;
        this.isComplete = false;
        this.currentPhase = 0;
        this.phaseTimer = 0f;
        this.currentDialogIndex = 0;
        this.fadeAlpha = 0f;
        this.textFullyRevealed = false;
        this.currentDisplayText = "";

        dialogLines.clear();
        setupCutscene(type);

        // Set game state to CUTSCENE
        GameManager.getInstance().setCurrentState(GameManager.GameState.CUTSCENE);

        System.out.println("[InGameCutscene] Started: " + type);
    }

    private void setupCutscene(CutsceneType type) {
        switch (type) {
            case GYM_ENCOUNTER:
                setupGymEncounter();
                break;
            case OBJECTIVE_CHANGE:
                setupObjectiveChange();
                break;
            case ESCAPE_SUCCESS:
                setupEscapeSuccess();
                break;
            default:
                break;
        }
    }

    private void setupGymEncounter() {
        System.out.println("[InGameCutscene] Setting up GYM_ENCOUNTER cutscene...");
        
        // Load textures safely
        try {
            
            
            // Load Josh chasing sprite sheet for cutscene animation (appears from top)
            // This is a 4-frame sprite sheet
            if (joshSpriteTexture == null) {
                if (Gdx.files.internal("Sprite/Enemy/josh_chasing_down.png").exists()) {
                    joshSpriteTexture = new Texture("Sprite/Enemy/josh_chasing_down.png");
                    
                    // Split the sprite sheet into 4 frames
                    int frameWidth = joshSpriteTexture.getWidth() / JOSH_SPRITE_FRAMES;
                    int frameHeight = joshSpriteTexture.getHeight();
                    TextureRegion[][] frames = TextureRegion.split(joshSpriteTexture, frameWidth, frameHeight);
                    joshAnimation = new Animation<>(JOSH_ANIM_FRAME_DURATION, frames[0]);
                    joshAnimation.setPlayMode(Animation.PlayMode.LOOP);
                    
                    System.out.println("[InGameCutscene] Loaded josh_chasing_down.png as 4-frame sprite sheet");
                } else if (Gdx.files.internal("josh.png").exists()) {
                    joshSpriteTexture = new Texture("josh.png");
                    // Fallback - no animation
                    joshAnimation = null;
                    System.out.println("[InGameCutscene] Loaded josh.png fallback for cutscene (no animation)");
                }
            }
            
            // Load Josh face for jumpscare
            if (joshFaceTexture == null) {
                if (Gdx.files.internal("joshInvertedFace.png").exists()) {
                    joshFaceTexture = new Texture("joshInvertedFace.png");
                    System.out.println("[InGameCutscene] Loaded joshInvertedFace.png for jumpscare");
                }
            }
        } catch (Exception e) {
            System.err.println("[InGameCutscene] Could not load textures: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Initialize Josh animation state - start from top of screen
        joshVisible = false;
        joshAlpha = 0f;
        joshAnimTimer = 0f;
        joshY = 600f; // Start above screen (will animate down)
        joshTargetY = 200f; // Target position (center-ish)

        // Dialog sequence with Josh appearance
        dialogLines.add(new DialogLine(null, "*Suara langkah kaki menggema di gym...*", 2.5f));
        dialogLines.add(new DialogLine(null, "*Kamu merasakan hawa dingin yang aneh...*", 2.0f));
        dialogLines.add(new DialogLine(null, "GRRRRAAAAAHHH!!!", 1.5f, true)); // Screen shake + Josh appears from top
        dialogLines.add(new DialogLine("Josh", "...", 1.0f));
        dialogLines.add(new DialogLine(null, "*Josh menerkammu dengan kecepatan luar biasa!*", 2.0f));
        dialogLines.add(new DialogLine(null, "*Kamu pingsan...*", 3.0f, true, true)); // Fade to black

        // Play scary sound - safely
        try {
            String soundPath = "Audio/Effect/monster_roar_sound_effect.wav";
            if (Gdx.files.internal(soundPath).exists()) {
                AudioManager.getInstance().playSound(soundPath);
                System.out.println("[InGameCutscene] Playing monster roar sound");
            } else {
                System.err.println("[InGameCutscene] Sound file not found: " + soundPath);
            }
        } catch (Exception e) {
            System.err.println("[InGameCutscene] Could not play sound: " + e.getMessage());
        }
        
        System.out.println("[InGameCutscene] GYM_ENCOUNTER setup complete, " + dialogLines.size() + " dialog lines");
    }

    private void setupObjectiveChange() {
        dialogLines.add(new DialogLine(null, "*Kamu terbangun di lantai yang dingin...*", 2.5f));
        dialogLines.add(new DialogLine("Jonatan", "Ugh... kepala ku...", 2.0f));
        dialogLines.add(new DialogLine("Jonatan", "Josh... dia sudah tidak bisa diselamatkan.", 3.0f));
        dialogLines.add(new DialogLine("Jonatan", "Aku harus keluar dari sini!", 2.5f));
        dialogLines.add(new DialogLine(null, "Objektif baru: KABUR dari sekolah!", 3.0f));
        dialogLines.add(new DialogLine(null, "Temukan kunci untuk membuka pintu keluar.", 3.0f));
    }

    private void setupEscapeSuccess() {
        dialogLines.add(new DialogLine(null, "*Pintu terbuka...*", 2.0f));
        dialogLines.add(new DialogLine("Jonatan", "Akhirnya... aku bebas!", 2.5f));
        dialogLines.add(new DialogLine(null, "Kamu berhasil kabur dari sekolah.", 3.0f));
    }

    /**
     * Update cutscene state
     */
    public void update(float delta) {
        if (!isActive || isComplete)
            return;

        phaseTimer += delta;
        
        // Update Josh animation for GYM_ENCOUNTER
        if (currentType == CutsceneType.GYM_ENCOUNTER) {
            joshAnimTimer += delta;
            
            // Josh appears during dialog index 2 (the roar) and stays visible
            if (currentDialogIndex >= 2) {
                joshVisible = true;
                // Fade in Josh
                if (joshAlpha < 1f) {
                    joshAlpha += delta * 2f; // Fade in over 0.5 seconds
                    if (joshAlpha > 1f) joshAlpha = 1f;
                }
            }
        }

        // Handle dialog progression
        if (currentDialogIndex < dialogLines.size()) {
            DialogLine currentLine = dialogLines.get(currentDialogIndex);
            
            // Text reveal animation
            if (!textFullyRevealed) {
                textRevealTimer += delta;
                int charsToShow = (int) (textRevealTimer / TEXT_REVEAL_SPEED);
                if (charsToShow >= currentLine.text.length()) {
                    currentDisplayText = currentLine.text;
                    textFullyRevealed = true;
                } else {
                    currentDisplayText = currentLine.text.substring(0, charsToShow);
                }
            }

            // Screen shake effect
            if (currentLine.screenShake && phaseTimer < 0.5f) {
                shakeAmount = 5f * (1f - phaseTimer * 2f);
                shakeTimer += delta * 30f;
            } else {
                shakeAmount = 0f;
            }

            // Fade effect
            if (currentLine.fadeToBlack) {
                fadeAlpha = Math.min(1f, phaseTimer / 2f);
            }

            // Auto-advance or input to continue
            boolean shouldAdvance = false;
            if (currentLine.duration > 0 && phaseTimer >= currentLine.duration && textFullyRevealed) {
                shouldAdvance = true;
            } else if (textFullyRevealed && 
                    (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || 
                     Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                     Gdx.input.justTouched())) {
                shouldAdvance = true;
            }

            if (shouldAdvance) {
                nextDialog();
            }
        } else {
            // Cutscene complete
            completeCutscene();
        }
    }

    private void nextDialog() {
        currentDialogIndex++;
        phaseTimer = 0f;
        textRevealTimer = 0f;
        textFullyRevealed = false;
        currentDisplayText = "";
        shakeAmount = 0f;

        if (currentDialogIndex >= dialogLines.size()) {
            completeCutscene();
        }
    }

    private void completeCutscene() {
        isComplete = true;
        isActive = false;
        fadeAlpha = 0f;
        shakeAmount = 0f;

        System.out.println("[InGameCutscene] Complete: " + currentType);
        
        // Store onComplete before clearing currentType
        Runnable completionCallback = onComplete;
        CutsceneType completedType = currentType;

        // Call the completion callback first
        if (completionCallback != null) {
            completionCallback.run();
        }
        
        // After callback, if no new cutscene was started, restore game state
        // (if callback started a new cutscene, isActive will be true again)
        if (!isActive) {
            // Restore state to PLAYING or STORY based on game progress
            GameManager gm = GameManager.getInstance();
            if (gm.getCurrentState() == GameManager.GameState.CUTSCENE) {
                if (gm.hasMetJosh()) {
                    gm.setCurrentState(GameManager.GameState.PLAYING);
                    System.out.println("[InGameCutscene] State restored to PLAYING");
                } else {
                    gm.setCurrentState(GameManager.GameState.STORY);
                    System.out.println("[InGameCutscene] State restored to STORY");
                }
            }
        }
    }

    /**
     * Render cutscene overlay
     */
    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font,
            float screenWidth, float screenHeight) {
        if (!isActive)
            return;

        // Calculate shake offset
        float shakeX = shakeAmount > 0 ? (float) Math.sin(shakeTimer) * shakeAmount : 0f;
        float shakeY = shakeAmount > 0 ? (float) Math.cos(shakeTimer * 1.3f) * shakeAmount : 0f;

        // Apply shake (translate camera or UI)
        
        // Draw fade overlay
        if (fadeAlpha > 0) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, fadeAlpha);
            shapeRenderer.rect(0, 0, screenWidth, screenHeight);
            shapeRenderer.end();
        }

        // Draw dialog box if we have current dialog
        if (currentDialogIndex < dialogLines.size() && fadeAlpha < 0.9f) {
            DialogLine currentLine = dialogLines.get(currentDialogIndex);
            
            // === DRAW JOSH SPRITE DURING GYM_ENCOUNTER ===
            // Josh appears suddenly from TOP of screen and moves down
            if (currentType == CutsceneType.GYM_ENCOUNTER && joshVisible && joshSpriteTexture != null) {
                batch.begin();
                batch.setColor(1f, 1f, 1f, joshAlpha);
                
                // Draw Josh face during the roar (dialog index 2) - JUMPSCARE!
                if (currentDialogIndex == 2 && joshFaceTexture != null) {
                    float faceX = (screenWidth - JOSH_FACE_SIZE) / 2f + shakeX * 2f;
                    float faceY = screenHeight * 0.4f + shakeY * 2f;
                    batch.draw(joshFaceTexture, faceX, faceY, JOSH_FACE_SIZE, JOSH_FACE_SIZE);
                }
                // Draw Josh sprite entering from TOP (dialog index 2+)
                else if (currentDialogIndex >= 2) {
                    // Animate Josh moving down from top of screen
                    // joshY starts at 600 (above screen) and moves to joshTargetY (200)
                    float animSpeed = 800f; // pixels per second
                    if (joshY > joshTargetY) {
                        joshY -= animSpeed * Gdx.graphics.getDeltaTime();
                        if (joshY < joshTargetY) joshY = joshTargetY;
                    }
                    
                    // Scale up as Josh gets closer
                    float distanceProgress = 1f - (joshY - joshTargetY) / 400f;
                    float scale = 1f + distanceProgress * 0.8f; // Scale from 1x to 1.8x
                    scale = Math.max(1f, Math.min(scale, 2f)); // Clamp between 1x and 2x
                    
                    float spriteW = JOSH_SPRITE_SIZE * scale;
                    float spriteH = JOSH_SPRITE_SIZE * scale;
                    float spriteX = (screenWidth - spriteW) / 2f + shakeX;
                    float spriteY = joshY + shakeY;
                    
                    // Use animated sprite if available, otherwise fallback to static texture
                    if (joshAnimation != null) {
                        TextureRegion frame = joshAnimation.getKeyFrame(joshAnimTimer);
                        batch.draw(frame, spriteX, spriteY, spriteW, spriteH);
                    } else {
                        batch.draw(joshSpriteTexture, spriteX, spriteY, spriteW, spriteH);
                    }
                }
                
                batch.setColor(Color.WHITE);
                batch.end();
            }
            
            // Dialog box at bottom
            float boxHeight = 120f;
            float boxY = 30f + shakeY;
            float boxPadding = 30f;
            float boxX = boxPadding + shakeX;
            float boxWidth = screenWidth - boxPadding * 2;

            // Box background
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.05f, 0.05f, 0.08f, 0.9f);
            shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);
            shapeRenderer.end();

            // Box border (red horror theme)
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            Gdx.gl.glLineWidth(2f);
            shapeRenderer.setColor(0.7f, 0.1f, 0.1f, 1f);
            shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);
            shapeRenderer.end();
            Gdx.gl.glLineWidth(1f);

            // Draw text
            batch.begin();
            
            // Speaker name (if any)
            float textY = boxY + boxHeight - 25f;
            if (currentLine.speaker != null && !currentLine.speaker.isEmpty()) {
                font.setColor(0.9f, 0.3f, 0.3f, 1f);
                font.draw(batch, currentLine.speaker + ":", boxX + 20f, textY);
                textY -= 30f;
            }

            // Dialog text
            font.setColor(Color.WHITE);
            font.draw(batch, currentDisplayText, boxX + 20f, textY);

            // Continue hint
            if (textFullyRevealed) {
                font.setColor(0.5f, 0.5f, 0.5f, 0.7f);
                String hint = "[Tekan SPACE untuk lanjut]";
                GlyphLayout layout = new GlyphLayout(font, hint);
                font.draw(batch, hint, boxX + boxWidth - layout.width - 20f, boxY + 25f);
            }

            batch.end();
        }

        // Draw injured texture during specific phases
        if (currentType == CutsceneType.GYM_ENCOUNTER && injuredTexture != null && 
            currentDialogIndex >= 4 && fadeAlpha > 0.3f) {
            batch.begin();
            batch.setColor(1f, 1f, 1f, Math.min(1f, fadeAlpha * 1.5f));
            float imgW = 300f;
            float imgH = 300f;
            batch.draw(injuredTexture, 
                    (screenWidth - imgW) / 2f + shakeX, 
                    (screenHeight - imgH) / 2f + shakeY, 
                    imgW, imgH);
            batch.setColor(Color.WHITE);
            batch.end();
        }
    }

    /**
     * Get screen shake offset for camera
     */
    public float getShakeX() {
        return shakeAmount > 0 ? (float) Math.sin(shakeTimer) * shakeAmount : 0f;
    }

    public float getShakeY() {
        return shakeAmount > 0 ? (float) Math.cos(shakeTimer * 1.3f) * shakeAmount : 0f;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isComplete() {
        return isComplete;
    }

    /**
     * Force skip cutscene (for testing)
     */
    public void skip() {
        completeCutscene();
    }

    public void dispose() {
        if (injuredTexture != null) {
            injuredTexture.dispose();
            injuredTexture = null;
        }
        if (joshFaceTexture != null) {
            joshFaceTexture.dispose();
            joshFaceTexture = null;
        }
        if (joshSpriteTexture != null) {
            joshSpriteTexture.dispose();
            joshSpriteTexture = null;
        }
    }

    /**
     * Dialog line data class
     */
    private static class DialogLine {
        String speaker;
        String text;
        float duration;
        boolean screenShake;
        boolean fadeToBlack;

        DialogLine(String speaker, String text, float duration) {
            this(speaker, text, duration, false, false);
        }

        DialogLine(String speaker, String text, float duration, boolean screenShake) {
            this(speaker, text, duration, screenShake, false);
        }

        DialogLine(String speaker, String text, float duration, boolean screenShake, boolean fadeToBlack) {
            this.speaker = speaker;
            this.text = text;
            this.duration = duration;
            this.screenShake = screenShake;
            this.fadeToBlack = fadeToBlack;
        }
    }
}
