package com.fearjosh.frontend.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.fearjosh.frontend.state.player.PlayerState;
import com.fearjosh.frontend.state.player.NormalState;
import com.fearjosh.frontend.systems.AudioManager;

public class Player {

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private float x;
    private float y;

    // RENDER SIZE
    private float renderWidth;
    private float renderHeight;

    private Direction direction;

    // HITBOX
    private Rectangle bodyBounds;
    private Rectangle footBounds;

    // Animations
    private Animation<TextureRegion> walkUp, walkDown, walkLeft, walkRight;
    private TextureRegion idleUp, idleDown, idleLeft, idleRight;
    private float animationTimer = 0;

    // STATE
    private PlayerState currentState;
    private boolean sprintIntent = false;
    private boolean moving = false;

    // STAMINA
    private float stamina = 100f;
    private static final float MAX_STAMINA = 100f;

    // FLASHLIGHT
    private boolean flashlightOn = false;

    // INJURED
    private Texture injuredTexture;
    private boolean isInjured = false;

    public Player(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        // sizing
        this.renderWidth = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_WIDTH;
        this.renderHeight = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_HEIGHT;
        this.direction = Direction.DOWN;
        this.bodyBounds = new Rectangle();
        this.footBounds = new Rectangle();
        this.currentState = NormalState.getInstance();
        updateHitboxes();
    }

    // UPDATE
    public void update(float delta, boolean isMoving) {
        this.moving = isMoving;

        // Update animation
        if (isMoving) {
            animationTimer += delta;
        } else {
            animationTimer = 0f;
        }

        // state update
        PlayerState nextState = currentState.update(this, delta);
        if (nextState != currentState) {
            setState(nextState);
        }
    }

    // STATE

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

    public float getSpeedMultiplier() {
        return currentState != null ? currentState.getSpeedMultiplier() : 1.0f;
    }

    // SPRINT

    public void setSprintIntent(boolean intent) {
        this.sprintIntent = intent;
    }

    public boolean hasSprintIntent() {
        return sprintIntent;
    }

    // STAMINA

    public float getStamina() {
        return stamina;
    }

    public void setStamina(float stamina) {
        this.stamina = Math.max(0, Math.min(stamina, MAX_STAMINA));
    }

    public float getMaxStamina() {
        return MAX_STAMINA;
    }

    // FLASHLIGHT

    public void toggleFlashlight() {
        flashlightOn = !flashlightOn;
        AudioManager.getInstance().playSound("Audio/Effect/flashlight_click_sound_effect.wav");
    }

    public boolean isFlashlightOn() {
        return flashlightOn;
    }

    public void setFlashlightOn(boolean on) {
        this.flashlightOn = on;
    }

    // MOVEMENT

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public void move(float dx, float dy) {
        if (Math.abs(dx) > 0.0001f || Math.abs(dy) > 0.0001f) {
            if (Math.abs(dx) > Math.abs(dy)) {
                if (dx > 0)
                    direction = Direction.RIGHT;
                else
                    direction = Direction.LEFT;
            } else {
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

    // POSITION

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

    public float getRenderWidth() {
        return renderWidth;
    }

    public float getRenderHeight() {
        return renderHeight;
    }

    @Deprecated
    public float getWidth() {
        return renderWidth;
    }

    @Deprecated
    public float getHeight() {
        return renderHeight;
    }

    // HITBOX

    private void updateHitboxes() {
        float bodyW = com.fearjosh.frontend.config.Constants.PLAYER_ENEMY_HITBOX_WIDTH;
        float bodyH = com.fearjosh.frontend.config.Constants.PLAYER_ENEMY_HITBOX_HEIGHT;
        bodyBounds.set(x, y, bodyW, bodyH);

        float footW = com.fearjosh.frontend.config.Constants.PLAYER_COLLISION_WIDTH;
        float footH = com.fearjosh.frontend.config.Constants.PLAYER_COLLISION_HEIGHT;
        float footOffsetY = com.fearjosh.frontend.config.Constants.PLAYER_COLLISION_OFFSET_Y;
        float footX = x + (renderWidth - footW) / 2f;
        float footY = y + footOffsetY;

        footBounds.set(footX, footY, footW, footH);
    }

    public Rectangle getBodyBounds() {
        return bodyBounds;
    }

    public Rectangle getFootBounds() {
        return footBounds;
    }

    // DEBUG
    public void debugRenderHitboxes(com.badlogic.gdx.graphics.glutils.ShapeRenderer sr) {
        sr.setColor(com.badlogic.gdx.graphics.Color.RED);
        sr.rect(bodyBounds.x, bodyBounds.y, bodyBounds.width, bodyBounds.height);

        sr.setColor(com.badlogic.gdx.graphics.Color.GREEN);
        sr.rect(footBounds.x, footBounds.y, footBounds.width, footBounds.height);
    }

    // INJURED

    public void setInjured(boolean injured) {
        this.isInjured = injured;
    }

    public boolean isInjured() {
        return isInjured;
    }

    // ANIMATION

    public void loadAnimations() {
        int frameCols = 4; // jumlah frame per arah

        Texture rightTex = new Texture("Sprite/Player/jonatan_right.png");
        Texture leftTex = new Texture("Sprite/Player/jonatan_left.png");
        Texture upTex = new Texture("Sprite/Player/jonatan_up.png");
        Texture downTex = new Texture("Sprite/Player/jonatan_down.png");

        injuredTexture = new Texture("Sprite/Player/jonatan_injured.png");
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

        idleRight = rightSplit[0][0];
        idleLeft = leftSplit[0][0];
        idleUp = upSplit[0][0];
        idleDown = downSplit[0][0];
    }

    public TextureRegion getCurrentFrame(boolean isMoving) {
        if (isInjured && injuredTexture != null) {
            return new TextureRegion(injuredTexture);
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
        if (injuredTexture != null) {
            injuredTexture.dispose();
        }
    }
}
