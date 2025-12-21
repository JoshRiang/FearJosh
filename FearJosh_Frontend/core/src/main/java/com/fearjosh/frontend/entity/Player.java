package com.fearjosh.frontend.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.fearjosh.frontend.state.player.PlayerState;
import com.fearjosh.frontend.state.player.NormalState;

public class Player {

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private float x;
    private float y;

    // RENDER SIZE - Visual only (untuk draw sprite)
    private float renderWidth;
    private float renderHeight;

    private Direction direction;

    // DUAL HITBOX SYSTEM
    // 1) BODY HITBOX - untuk collision dengan ENEMY (full body)
    private Rectangle bodyBounds;

    // 2) FOOT HITBOX - untuk collision dengan FURNITURE (hanya kaki)
    private Rectangle footBounds;

    // Animations
    private Animation<TextureRegion> walkUp, walkDown, walkLeft, walkRight;
    private TextureRegion idleUp, idleDown, idleLeft, idleRight;
    private float animationTimer = 0;

    // ------------ PLAYER STATE SYSTEM ------------
    private PlayerState currentState;
    private boolean sprintIntent = false; // Intent dari input
    private boolean moving = false; // Track apakah player bergerak

    // ------------ STAMINA SYSTEM ------------
    private float stamina = 100f;
    private static final float MAX_STAMINA = 100f;

    // ------------ FLASHLIGHT ------------
    private boolean flashlightOn = false;

    // ------------ CAPTURED/TIED SPRITE ------------
    private Texture tiedTexture;
    private boolean isCaptured = false;

    public Player(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        // Use Constants for proper sizing (width/height params ignored)
        this.renderWidth = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_WIDTH;
        this.renderHeight = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_HEIGHT;
        this.direction = Direction.DOWN;
        this.bodyBounds = new Rectangle();
        this.footBounds = new Rectangle();
        this.currentState = NormalState.getInstance();
        updateHitboxes();
    }

    /**
     * Update player state dan animasi setiap frame
     */
    public void update(float delta, boolean isMoving) {
        this.moving = isMoving;

        // Update animation
        if (isMoving) {
            animationTimer += delta;
        } else {
            animationTimer = 0f;
        }

        // Update state (handles stamina drain/regen)
        PlayerState nextState = currentState.update(this, delta);
        if (nextState != currentState) {
            setState(nextState);
        }
    }

    // ------------ STATE MANAGEMENT ------------

    public void setState(PlayerState newState) {
        if (currentState != null) {
            currentState.exit(this);
        }
        currentState = newState;
        if (currentState != null) {
            currentState.enter(this);
        }
    }

    public PlayerState getCurrentState() {
        return currentState;
    }

    /**
     * Get speed multiplier dari current state
     */
    public float getSpeedMultiplier() {
        return currentState != null ? currentState.getSpeedMultiplier() : 1.0f;
    }

    // ------------ SPRINT INTENT ------------

    public void setSprintIntent(boolean intent) {
        this.sprintIntent = intent;
    }

    public boolean hasSprintIntent() {
        return sprintIntent;
    }

    // ------------ STAMINA ------------

    public float getStamina() {
        return stamina;
    }

    public void setStamina(float stamina) {
        this.stamina = Math.max(0, Math.min(stamina, MAX_STAMINA));
    }

    public float getMaxStamina() {
        return MAX_STAMINA;
    }

    // ------------ FLASHLIGHT ------------

    public void toggleFlashlight() {
        flashlightOn = !flashlightOn;
    }

    public boolean isFlashlightOn() {
        return flashlightOn;
    }

    public void setFlashlightOn(boolean on) {
        this.flashlightOn = on;
    }

    // ------------ MOVEMENT STATE ------------

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    // Gerak + update arah hadap
    public void move(float dx, float dy) {
        // Only update direction if there's actual movement
        if (Math.abs(dx) > 0.0001f || Math.abs(dy) > 0.0001f) {
            // Prioritize the larger movement to avoid flickering
            if (Math.abs(dx) > Math.abs(dy)) {
                // Horizontal movement is dominant
                if (dx > 0)
                    direction = Direction.RIGHT;
                else
                    direction = Direction.LEFT;
            } else {
                // Vertical movement is dominant
                if (dy > 0)
                    direction = Direction.UP;
                else
                    direction = Direction.DOWN;
            }
        }

        x += dx;
        y += dy;
        updateHitboxes();
    }

    // ------------ posisi ------------

    public float getCenterX() {
        return x + renderWidth / 2f;
    }

    public float getCenterY() {
        return y + renderHeight / 2f;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setX(float x) {
        this.x = x;
        updateHitboxes();
    }

    public void setY(float y) {
        this.y = y;
        updateHitboxes();
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    /** @return Render width for drawing sprite */
    public float getRenderWidth() {
        return renderWidth;
    }

    /** @return Render height for drawing sprite */
    public float getRenderHeight() {
        return renderHeight;
    }

    /** @deprecated Use getRenderWidth() - kept for compatibility */
    @Deprecated
    public float getWidth() {
        return renderWidth; // Return render width instead
    }

    /** @deprecated Use getRenderHeight() - kept for compatibility */
    @Deprecated
    public float getHeight() {
        return renderHeight; // Return render height instead
    }

    // ------------ DUAL HITBOX SYSTEM ------------

    /**
     * Update posisi SEMUA hitbox setiap kali player bergerak
     * Uses Constants for consistent, tunable hitbox sizing
     */
    private void updateHitboxes() {
        // 1) BODY HITBOX - full sprite untuk enemy collision
        float bodyW = com.fearjosh.frontend.config.Constants.PLAYER_ENEMY_HITBOX_WIDTH;
        float bodyH = com.fearjosh.frontend.config.Constants.PLAYER_ENEMY_HITBOX_HEIGHT;
        bodyBounds.set(x, y, bodyW, bodyH);

        // 2) FOOT HITBOX - bagian bawah untuk furniture collision
        float footW = com.fearjosh.frontend.config.Constants.PLAYER_COLLISION_WIDTH;
        float footH = com.fearjosh.frontend.config.Constants.PLAYER_COLLISION_HEIGHT;
        float footOffsetY = com.fearjosh.frontend.config.Constants.PLAYER_COLLISION_OFFSET_Y;
        float footX = x + (renderWidth - footW) / 2f; // Center horizontally
        float footY = y + footOffsetY; // Bottom of sprite + offset

        footBounds.set(footX, footY, footW, footH);
    }

    /**
     * BODY BOUNDS - untuk collision dengan ENEMY
     * Full-body rectangle dari kepala sampai kaki
     */
    public Rectangle getBodyBounds() {
        return bodyBounds;
    }

    /**
     * FOOT BOUNDS - untuk collision dengan FURNITURE
     * Hanya bagian kaki/bawah sprite
     */
    public Rectangle getFootBounds() {
        return footBounds;
    }

    /**
     * DEBUG: Render hitbox untuk visual debugging
     * Call dari PlayScreen dengan ShapeRenderer
     */
    public void debugRenderHitboxes(com.badlogic.gdx.graphics.glutils.ShapeRenderer sr) {
        // Body hitbox - MERAH (untuk enemy)
        sr.setColor(com.badlogic.gdx.graphics.Color.RED);
        sr.rect(bodyBounds.x, bodyBounds.y, bodyBounds.width, bodyBounds.height);

        // Foot hitbox - HIJAU (untuk furniture)
        sr.setColor(com.badlogic.gdx.graphics.Color.GREEN);
        sr.rect(footBounds.x, footBounds.y, footBounds.width, footBounds.height);
    }

    // ------------ CAPTURED STATE ------------

    public void setCaptured(boolean captured) {
        this.isCaptured = captured;
    }

    public boolean isCaptured() {
        return isCaptured;
    }

    // ------------ animasi ------------

    public void loadAnimations() {
        int frameCols = 4; // jumlah frame per arah

        Texture rightTex = new Texture("Sprite/Player/jonatan_right.png");
        Texture leftTex = new Texture("Sprite/Player/jonatan_left.png");
        Texture upTex = new Texture("Sprite/Player/jonatan_up.png");
        Texture downTex = new Texture("Sprite/Player/jonatan_down.png");

        // Load tied sprite
        tiedTexture = new Texture("Sprite/Player/jonatan_terikat.png");

        // Hitung frameWidth dan frameHeight untuk setiap texture
        int frameWidthRight = rightTex.getWidth() / frameCols;
        int frameHeightRight = rightTex.getHeight();

        int frameWidthLeft = leftTex.getWidth() / frameCols;
        int frameHeightLeft = leftTex.getHeight();

        int frameWidthUp = upTex.getWidth() / frameCols;
        int frameHeightUp = upTex.getHeight();

        int frameWidthDown = downTex.getWidth() / frameCols;
        int frameHeightDown = downTex.getHeight();

        TextureRegion[][] rightSplit = TextureRegion.split(rightTex, frameWidthRight, frameHeightRight);
        TextureRegion[][] leftSplit = TextureRegion.split(leftTex, frameWidthLeft, frameHeightLeft);
        TextureRegion[][] upSplit = TextureRegion.split(upTex, frameWidthUp, frameHeightUp);
        TextureRegion[][] downSplit = TextureRegion.split(downTex, frameWidthDown, frameHeightDown);

        walkRight = new Animation<>(0.15f, rightSplit[0]);
        walkLeft = new Animation<>(0.15f, leftSplit[0]);
        walkUp = new Animation<>(0.15f, upSplit[0]);
        walkDown = new Animation<>(0.15f, downSplit[0]);

        walkRight.setPlayMode(Animation.PlayMode.LOOP);
        walkLeft.setPlayMode(Animation.PlayMode.LOOP);
        walkUp.setPlayMode(Animation.PlayMode.LOOP);
        walkDown.setPlayMode(Animation.PlayMode.LOOP);

        // idle = frame ke-0 tiap arah
        idleRight = rightSplit[0][0];
        idleLeft = leftSplit[0][0];
        idleUp = upSplit[0][0];
        idleDown = downSplit[0][0];
    }

    public TextureRegion getCurrentFrame(boolean isMoving) {
        // If captured, always show tied sprite
        if (isCaptured && tiedTexture != null) {
            return new TextureRegion(tiedTexture);
        }

        if (!isMoving) {
            switch (direction) {
                case LEFT:
                    return idleLeft;
                case RIGHT:
                    return idleRight;
                case UP:
                    return idleUp;
                default:
                    return idleDown;
            }
        }

        switch (direction) {
            case LEFT:
                return walkLeft.getKeyFrame(animationTimer);
            case RIGHT:
                return walkRight.getKeyFrame(animationTimer);
            case UP:
                return walkUp.getKeyFrame(animationTimer);
            default:
                return walkDown.getKeyFrame(animationTimer);
        }
    }

    public void dispose() {
        if (tiedTexture != null) {
            tiedTexture.dispose();
        }
    }
}
