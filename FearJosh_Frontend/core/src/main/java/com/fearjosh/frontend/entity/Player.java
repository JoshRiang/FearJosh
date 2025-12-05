package com.fearjosh.frontend.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Player {

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private float x;
    private float y;
    private float width;
    private float height;
    private Direction direction;

    // Animations
    private Animation<TextureRegion> walkUp, walkDown, walkLeft, walkRight;
    private TextureRegion idleUp, idleDown, idleLeft, idleRight;
    private float animationTimer = 0;

    public Player(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.direction = Direction.DOWN;
    }

    // Dipanggil dari PlayScreen setiap frame
    public void update(float delta, boolean isMoving) {
        if (isMoving) {
            animationTimer += delta;
        } else {
            animationTimer = 0f; // idle balik ke frame pertama
        }
    }

    // Gerak + update arah hadap
    public void move(float dx, float dy) {
        if (dx > 0) direction = Direction.RIGHT;
        if (dx < 0) direction = Direction.LEFT;
        if (dy > 0) direction = Direction.UP;
        if (dy < 0) direction = Direction.DOWN;

        x += dx;
        y += dy;
    }

    // ------------ posisi ------------

    public float getCenterX() {
        return x + width / 2f;
    }

    public float getCenterY() {
        return y + height / 2f;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    // ------------ animasi ------------

    public void loadAnimations() {
        int frameCols = 4; // jumlah frame per arah

        Texture rightTex = new Texture("jonatan_right.png");
        Texture leftTex  = new Texture("jonatan_left.png");
        Texture upTex    = new Texture("jonatan_up.png");
        Texture downTex  = new Texture("jonatan_down.png");

        int frameWidth  = rightTex.getWidth() / frameCols;
        int frameHeight = rightTex.getHeight(); // tinggi 1 frame

        TextureRegion[][] rightSplit = TextureRegion.split(rightTex, frameWidth, frameHeight);
        TextureRegion[][] leftSplit  = TextureRegion.split(leftTex,  frameWidth, frameHeight);
        TextureRegion[][] upSplit    = TextureRegion.split(upTex,    frameWidth, frameHeight);
        TextureRegion[][] downSplit  = TextureRegion.split(downTex,  frameWidth, frameHeight);

        walkRight = new Animation<>(0.15f, rightSplit[0]);
        walkLeft  = new Animation<>(0.15f, leftSplit[0]);
        walkUp    = new Animation<>(0.15f, upSplit[0]);
        walkDown  = new Animation<>(0.15f, downSplit[0]);

        walkRight.setPlayMode(Animation.PlayMode.LOOP);
        walkLeft.setPlayMode(Animation.PlayMode.LOOP);
        walkUp.setPlayMode(Animation.PlayMode.LOOP);
        walkDown.setPlayMode(Animation.PlayMode.LOOP);

        // idle = frame ke-0 tiap arah
        idleRight = rightSplit[0][0];
        idleLeft  = leftSplit[0][0];
        idleUp    = upSplit[0][0];
        idleDown  = downSplit[0][0];
    }

    public TextureRegion getCurrentFrame(boolean isMoving) {
        if (!isMoving) {
            switch (direction) {
                case LEFT:  return idleLeft;
                case RIGHT: return idleRight;
                case UP:    return idleUp;
                default:    return idleDown;
            }
        }

        switch (direction) {
            case LEFT:  return walkLeft.getKeyFrame(animationTimer);
            case RIGHT: return walkRight.getKeyFrame(animationTimer);
            case UP:    return walkUp.getKeyFrame(animationTimer);
            default:    return walkDown.getKeyFrame(animationTimer);
        }
    }
}
