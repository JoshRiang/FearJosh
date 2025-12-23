package com.fearjosh.frontend.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * JumpscareManager - Handles two types of jumpscares:
 * 
 * 1. AMBIENT JUMPSCARE (joshInvertedFace.png)
 *    - Random psychological pressure
 *    - Non-lethal, purely for tension
 *    - Quick flash (0.15-0.3s duration)
 *    - Has cooldown to prevent spam
 * 
 * 2. CAPTURE JUMPSCARE (jumpscareJosh.png)
 *    - Guaranteed when Josh catches player
 *    - Plays BEFORE injured/captured state
 *    - Longer duration with flicker effect (0.4-0.6s)
 *    - No cooldown (always plays on capture)
 * 
 * BLOCKED DURING: pause menu, cutscenes, minigames, tutorials
 */
public class JumpscareManager {
    
    private static JumpscareManager INSTANCE;
    
    // ==================== ASSETS ====================
    
    private Texture ambientJumpscareTexture;   // joshInvertedFace.png
    private Texture captureJumpscareTexture;   // jumpscareJosh.png
    
    // ==================== JUMPSCARE STATE ====================
    
    public enum JumpscareType {
        NONE,
        AMBIENT,    // Random psychological jumpscare
        CAPTURE     // Guaranteed capture jumpscare
    }
    
    private JumpscareType currentJumpscare = JumpscareType.NONE;
    private float jumpscareTimer = 0f;
    private float jumpscareDuration = 0f;
    private boolean jumpscareActive = false;
    
    // Callback for when capture jumpscare finishes
    private Runnable onCaptureJumpscareComplete;
    
    // ==================== AMBIENT JUMPSCARE CONFIG ====================
    
    /** Minimum time between ambient jumpscares (seconds) */
    private static final float AMBIENT_COOLDOWN_MIN = 45f;
    
    /** Maximum time between ambient jumpscares (seconds) */
    private static final float AMBIENT_COOLDOWN_MAX = 120f;
    
    /** Duration of ambient jumpscare (seconds) */
    private static final float AMBIENT_DURATION_MIN = 0.15f;
    private static final float AMBIENT_DURATION_MAX = 0.30f;
    
    /** Base chance for ambient jumpscare per check (0.0 - 1.0) */
    private static final float AMBIENT_CHANCE = 0.02f;
    
    /** Check interval for ambient jumpscare (seconds) */
    private static final float AMBIENT_CHECK_INTERVAL = 5f;
    
    // ==================== CAPTURE JUMPSCARE CONFIG ====================
    
    /** Duration of capture jumpscare (seconds) */
    private static final float CAPTURE_DURATION_MIN = 0.4f;
    private static final float CAPTURE_DURATION_MAX = 0.6f;
    
    /** Flicker frequency for capture jumpscare */
    private static final float CAPTURE_FLICKER_SPEED = 0.05f;
    
    // ==================== TIMERS ====================
    
    private float ambientCooldownTimer = 0f;
    private float currentAmbientCooldown;
    private float ambientCheckTimer = 0f;
    private float globalTimer = 0f;
    
    // Flicker state for capture jumpscare
    private boolean flickerOn = true;
    private float flickerTimer = 0f;
    
    // ==================== HALLWAY BOOST ====================
    
    /** Extra chance for ambient jumpscare in hallways */
    private static final float HALLWAY_AMBIENT_BOOST = 0.03f;
    private boolean inHallway = false;
    
    // ==================== BLOCKED STATES ====================
    
    private boolean blocked = false;  // True during pause/cutscene/minigame
    
    // ==================== DEBUG ====================
    
    private boolean debugMode = false;
    
    // ==================== CONSTRUCTOR ====================
    
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
    
    // ==================== ASSET LOADING ====================
    
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
    
    // ==================== UPDATE ====================
    
    /**
     * Update jumpscare system - call every frame
     * 
     * @param delta Time since last frame
     * @param canTrigger False if blocked (pause/cutscene/minigame/tutorial)
     */
    public void update(float delta, boolean canTrigger) {
        globalTimer += delta;
        blocked = !canTrigger;
        
        // Update active jumpscare
        if (jumpscareActive) {
            jumpscareTimer += delta;
            
            // Update flicker for capture jumpscare
            if (currentJumpscare == JumpscareType.CAPTURE) {
                flickerTimer += delta;
                if (flickerTimer >= CAPTURE_FLICKER_SPEED) {
                    flickerOn = !flickerOn;
                    flickerTimer = 0f;
                }
            }
            
            // Check if jumpscare should end
            if (jumpscareTimer >= jumpscareDuration) {
                endJumpscare();
            }
            return; // Don't check for new jumpscares while one is active
        }
        
        // Don't trigger ambient jumpscares when blocked
        if (blocked) {
            return;
        }
        
        // Update ambient cooldown
        ambientCooldownTimer += delta;
        ambientCheckTimer += delta;
        
        // Periodic check for ambient jumpscare
        if (ambientCheckTimer >= AMBIENT_CHECK_INTERVAL && 
            ambientCooldownTimer >= currentAmbientCooldown) {
            
            ambientCheckTimer = 0f;
            checkAmbientJumpscare();
        }
    }
    
    /**
     * Check if ambient jumpscare should trigger
     */
    private void checkAmbientJumpscare() {
        float chance = AMBIENT_CHANCE;
        
        // Boost chance in hallways
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
    
    // ==================== TRIGGER METHODS ====================
    
    /**
     * Trigger ambient jumpscare (random psychological pressure)
     */
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
        
        // Reset ambient cooldown
        ambientCooldownTimer = 0f;
        currentAmbientCooldown = randomRange(AMBIENT_COOLDOWN_MIN, AMBIENT_COOLDOWN_MAX);
        
        // Play sound effect
        AudioManager.getInstance().playSound("Audio/Effect/jumpscare_ambient.wav", 0.8f);
        
        log("AMBIENT JUMPSCARE triggered! Duration: " + String.format("%.2f", jumpscareDuration) + "s");
    }
    
    /**
     * Trigger capture jumpscare (guaranteed before injured state)
     * 
     * @param onComplete Callback to run after jumpscare finishes (e.g., trigger injured state)
     */
    public void triggerCaptureJumpscare(Runnable onComplete) {
        // Capture jumpscare bypasses blocked state - it's mandatory
        if (jumpscareActive) {
            // If already in a jumpscare, immediately call the callback
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
        
        // Play capture sound effect
        AudioManager.getInstance().playSound("Audio/Effect/jumpscare_capture.wav", 1.0f);
        
        log("CAPTURE JUMPSCARE triggered! Duration: " + String.format("%.2f", jumpscareDuration) + "s");
    }
    
    /**
     * End current jumpscare
     */
    private void endJumpscare() {
        JumpscareType endedType = currentJumpscare;
        
        jumpscareActive = false;
        jumpscareTimer = 0f;
        currentJumpscare = JumpscareType.NONE;
        
        log("Jumpscare ended: " + endedType);
        
        // Call capture callback if it was a capture jumpscare
        if (endedType == JumpscareType.CAPTURE && onCaptureJumpscareComplete != null) {
            Runnable callback = onCaptureJumpscareComplete;
            onCaptureJumpscareComplete = null;
            callback.run();
        }
    }
    
    // ==================== RENDER ====================
    
    /**
     * Render jumpscare overlay - call LAST in render pipeline (on top of everything)
     * 
     * @param batch SpriteBatch to use (must be begun)
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     */
    public void render(SpriteBatch batch, float screenWidth, float screenHeight) {
        if (!jumpscareActive) {
            return;
        }
        
        Texture texture = null;
        float alpha = 1f;
        
        switch (currentJumpscare) {
            case AMBIENT:
                texture = ambientJumpscareTexture;
                // Quick fade in/out for ambient
                if (jumpscareTimer < 0.05f) {
                    alpha = jumpscareTimer / 0.05f;
                } else if (jumpscareTimer > jumpscareDuration - 0.05f) {
                    alpha = (jumpscareDuration - jumpscareTimer) / 0.05f;
                }
                break;
                
            case CAPTURE:
                texture = captureJumpscareTexture;
                // Flicker effect for capture
                alpha = flickerOn ? 1f : 0.7f;
                break;
                
            default:
                return;
        }
        
        if (texture == null) {
            return;
        }
        
        // Draw fullscreen with transparency
        Color prevColor = batch.getColor().cpy();
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(texture, 0, 0, screenWidth, screenHeight);
        batch.setColor(prevColor);
    }
    
    /**
     * Render black flash for capture jumpscare (optional extra effect)
     */
    public void renderBlackFlash(ShapeRenderer shapeRenderer, float screenWidth, float screenHeight) {
        if (!jumpscareActive || currentJumpscare != JumpscareType.CAPTURE) {
            return;
        }
        
        // Brief black flash at start and end
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
    
    // ==================== STATE METHODS ====================
    
    /**
     * Set whether player is in hallway (affects ambient jumpscare chance)
     */
    public void setInHallway(boolean inHallway) {
        this.inHallway = inHallway;
        log("Hallway state: " + inHallway);
    }
    
    /**
     * Check if any jumpscare is currently active
     */
    public boolean isJumpscareActive() {
        return jumpscareActive;
    }
    
    /**
     * Get current jumpscare type
     */
    public JumpscareType getCurrentJumpscare() {
        return currentJumpscare;
    }
    
    /**
     * Force stop any active jumpscare (emergency use only)
     */
    public void forceStopJumpscare() {
        if (jumpscareActive) {
            log("Force stopping jumpscare: " + currentJumpscare);
            
            // If it was a capture jumpscare, still call the callback
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
    
    /**
     * Reset manager state (call on game restart)
     */
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
    
    // ==================== DEBUG ====================
    
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }
    
    private void log(String message) {
        if (debugMode) {
            System.out.println("[JumpscareManager] " + message);
        }
    }
    
    // ==================== UTILITY ====================
    
    private float randomRange(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }
    
    // ==================== DISPOSE ====================
    
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
