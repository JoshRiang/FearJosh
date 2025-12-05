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
        Texture leftTex = new Texture("jonatan_left.png");
        Texture upTex = new Texture("jonatan_up.png");
        Texture downTex = new Texture("jonatan_down.png");

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
}
