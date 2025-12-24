package com.fearjosh.frontend.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class JumpscareManager {
    
    private static JumpscareManager INSTANCE;
    
    // ASSETS
    private Texture ambientJumpscareTexture;
    private Texture captureJumpscareTexture;
    
    // JUMPSCARE STATE
    public enum JumpscareType {
        NONE,
        AMBIENT,
        CAPTURE
    }
    
    private JumpscareType currentJumpscare = JumpscareType.NONE;
    private float jumpscareTimer = 0f;
    private float jumpscareDuration = 0f;
    private boolean jumpscareActive = false;
    
    private Runnable onCaptureJumpscareComplete;
    
    // AMBIENT CONFIG
    private static final float AMBIENT_COOLDOWN_MIN = 45f;
    private static final float AMBIENT_COOLDOWN_MAX = 120f;
    private static final float AMBIENT_DURATION_MIN = 0.15f;
    private static final float AMBIENT_DURATION_MAX = 0.30f;
    private static final float AMBIENT_CHANCE = 0.02f;
    private static final float AMBIENT_CHECK_INTERVAL = 5f;
    
    // CAPTURE CONFIG
    private static final float CAPTURE_DURATION_MIN = 0.4f;
    private static final float CAPTURE_DURATION_MAX = 0.6f;
    private static final float CAPTURE_FLICKER_SPEED = 0.05f;
    
    // TIMERS
    private float ambientCooldownTimer = 0f;
    private float currentAmbientCooldown;
    private float ambientCheckTimer = 0f;
    
    // Flicker state
    private boolean flickerOn = true;
    private float flickerTimer = 0f;
    
    // HALLWAY BOOST
    private static final float HALLWAY_AMBIENT_BOOST = 0.03f;
    private boolean inHallway = false;
    
    // BLOCKED STATE
    private boolean blocked = false;
    
    // DEBUG
    private boolean debugMode = false;
    
    // CONSTRUCTOR
    
    private JumpscareManager() {
        currentAmbientCooldown = randomRange(AMBIENT_COOLDOWN_MIN, AMBIENT_COOLDOWN_MAX);
        loadAssets();
        log("Initialized with ambient cooldown: " + String.format("%.1f", currentAmbientCooldown) + "s");
    }
    
    public static synchronized JumpscareManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JumpscareManager();
        }
        return INSTANCE;
    }
    
    // ASSET LOADING
    private void loadAssets() {
        try {
            if (Gdx.files.internal("joshInvertedFace.png").exists()) {
                ambientJumpscareTexture = new Texture(Gdx.files.internal("joshInvertedFace.png"));
                log("Loaded ambient jumpscare: joshInvertedFace.png");
            } else {
                System.err.println("[JumpscareManager] WARNING: joshInvertedFace.png not found!");
            }
            
            if (Gdx.files.internal("jumpscareJosh.png").exists()) {
                captureJumpscareTexture = new Texture(Gdx.files.internal("jumpscareJosh.png"));
                log("Loaded capture jumpscare: jumpscareJosh.png");
            } else {
                System.err.println("[JumpscareManager] WARNING: jumpscareJosh.png not found!");
            }
        } catch (Exception e) {
            System.err.println("[JumpscareManager] Error loading jumpscare textures: " + e.getMessage());
        }
    }
    
    // UPDATE
    public void update(float delta, boolean canTrigger) {
        blocked = !canTrigger;
        
        if (jumpscareActive) {
            jumpscareTimer += delta;
            
            if (currentJumpscare == JumpscareType.CAPTURE) {
                flickerTimer += delta;
                if (flickerTimer >= CAPTURE_FLICKER_SPEED) {
                    flickerOn = !flickerOn;
                    flickerTimer = 0f;
                }
            }
            
            if (jumpscareTimer >= jumpscareDuration) {
                endJumpscare();
            }
            return;
        }
        
        if (blocked) {
            return;
        }
        
        ambientCooldownTimer += delta;
        ambientCheckTimer += delta;
        
        // ambient check
        if (ambientCheckTimer >= AMBIENT_CHECK_INTERVAL && 
            ambientCooldownTimer >= currentAmbientCooldown) {
            
            ambientCheckTimer = 0f;
            checkAmbientJumpscare();
        }
    }
    
    private void checkAmbientJumpscare() {
        float chance = AMBIENT_CHANCE;
        
        if (inHallway) {
            chance += HALLWAY_AMBIENT_BOOST;
        }
        
        float roll = (float) Math.random();
        
        if (roll < chance) {
            triggerAmbientJumpscare();
        } else {
            log("Ambient check failed: " + String.format("%.0f%%", roll * 100) + 
                " >= " + String.format("%.0f%%", chance * 100));
        }
    }
    
    // TRIGGER METHODS
    public void triggerAmbientJumpscare() {
        if (jumpscareActive || blocked) {
            log("Ambient jumpscare blocked (active=" + jumpscareActive + ", blocked=" + blocked + ")");
            return;
        }
        
        if (ambientJumpscareTexture == null) {
            log("Cannot trigger ambient - texture not loaded");
            return;
        }
        
        currentJumpscare = JumpscareType.AMBIENT;
        jumpscareActive = true;
        jumpscareTimer = 0f;
        jumpscareDuration = randomRange(AMBIENT_DURATION_MIN, AMBIENT_DURATION_MAX);
        
        ambientCooldownTimer = 0f;
        currentAmbientCooldown = randomRange(AMBIENT_COOLDOWN_MIN, AMBIENT_COOLDOWN_MAX);
        
        // Play sound effect
        AudioManager.getInstance().playSound("Audio/Effect/jumpscare_ambient.wav", 0.8f);
        
        log("AMBIENT JUMPSCARE triggered! Duration: " + String.format("%.2f", jumpscareDuration) + "s");
    }
    
    public void triggerCaptureJumpscare(Runnable onComplete) {
        if (jumpscareActive) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        
        if (captureJumpscareTexture == null) {
            log("Cannot trigger capture - texture not loaded, proceeding to callback");
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        
        currentJumpscare = JumpscareType.CAPTURE;
        jumpscareActive = true;
        jumpscareTimer = 0f;
        jumpscareDuration = randomRange(CAPTURE_DURATION_MIN, CAPTURE_DURATION_MAX);
        flickerOn = true;
        flickerTimer = 0f;
        onCaptureJumpscareComplete = onComplete;
        
        AudioManager.getInstance().playSound("Audio/Effect/jumpscare_capture.wav", 1.0f);
        
        log("CAPTURE JUMPSCARE triggered! Duration: " + String.format("%.2f", jumpscareDuration) + "s");
    }
    
    private void endJumpscare() {
        JumpscareType endedType = currentJumpscare;
        
        jumpscareActive = false;
        jumpscareTimer = 0f;
        currentJumpscare = JumpscareType.NONE;
        
        log("Jumpscare ended: " + endedType);
        
        if (endedType == JumpscareType.CAPTURE && onCaptureJumpscareComplete != null) {
            Runnable callback = onCaptureJumpscareComplete;
            onCaptureJumpscareComplete = null;
            callback.run();
        }
    }
    
    // RENDER
    public void render(SpriteBatch batch, float screenWidth, float screenHeight) {
        if (!jumpscareActive) {
            return;
        }
        
        Texture texture = null;
        float alpha = 1f;
        
        switch (currentJumpscare) {
            case AMBIENT:
                texture = ambientJumpscareTexture;
                // Fade in/out
                if (jumpscareTimer < 0.05f) {
                    alpha = jumpscareTimer / 0.05f;
                } else if (jumpscareTimer > jumpscareDuration - 0.05f) {
                    alpha = (jumpscareDuration - jumpscareTimer) / 0.05f;
                }
                break;
                
            case CAPTURE:
                texture = captureJumpscareTexture;
                alpha = flickerOn ? 1f : 0.7f;
                break;
                
            default:
                return;
        }
        
        if (texture == null) {
            return;
        }
        
        Color prevColor = batch.getColor().cpy();
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(texture, 0, 0, screenWidth, screenHeight);
        batch.setColor(prevColor);
    }
    
    public void renderBlackFlash(ShapeRenderer shapeRenderer, float screenWidth, float screenHeight) {
        if (!jumpscareActive || currentJumpscare != JumpscareType.CAPTURE) {
            return;
        }
        
        float alpha = 0f;
        if (jumpscareTimer < 0.05f) {
            alpha = 1f - (jumpscareTimer / 0.05f);
        } else if (jumpscareTimer > jumpscareDuration - 0.05f) {
            alpha = (jumpscareTimer - (jumpscareDuration - 0.05f)) / 0.05f;
        }
        
        if (alpha > 0f) {
            shapeRenderer.setColor(0, 0, 0, alpha);
            shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        }
    }
    
    // STATE METHODS
    public void setInHallway(boolean inHallway) {
        this.inHallway = inHallway;
        log("Hallway state: " + inHallway);
    }
    
    public boolean isJumpscareActive() {
        return jumpscareActive;
    }
    
    public JumpscareType getCurrentJumpscare() {
        return currentJumpscare;
    }
    
    public void forceStopJumpscare() {
        if (jumpscareActive) {
            log("Force stopping jumpscare: " + currentJumpscare);
            
            if (currentJumpscare == JumpscareType.CAPTURE && onCaptureJumpscareComplete != null) {
                Runnable callback = onCaptureJumpscareComplete;
                onCaptureJumpscareComplete = null;
                callback.run();
            }
            
            jumpscareActive = false;
            jumpscareTimer = 0f;
            currentJumpscare = JumpscareType.NONE;
        }
    }
    
    public void reset() {
        jumpscareActive = false;
        jumpscareTimer = 0f;
        currentJumpscare = JumpscareType.NONE;
        onCaptureJumpscareComplete = null;
        ambientCooldownTimer = 0f;
        ambientCheckTimer = 0f;
        currentAmbientCooldown = randomRange(AMBIENT_COOLDOWN_MIN, AMBIENT_COOLDOWN_MAX);
        inHallway = false;
        blocked = false;
        
        log("Reset complete, next ambient cooldown: " + String.format("%.1f", currentAmbientCooldown) + "s");
    }
    
    // DEBUG
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }
    
    private void log(String message) {
        if (debugMode) {
            System.out.println("[JumpscareManager] " + message);
        }
    }
    
    // UTILITY
    private float randomRange(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }
    
    // DISPOSE
    public void dispose() {
        if (ambientJumpscareTexture != null) {
            ambientJumpscareTexture.dispose();
            ambientJumpscareTexture = null;
        }
        if (captureJumpscareTexture != null) {
            captureJumpscareTexture.dispose();
            captureJumpscareTexture = null;
        }
        log("Disposed textures");
    }
}
