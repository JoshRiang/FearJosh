package com.fearjosh.frontend.entity;

public class Player {

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private float x;
    private float y;
    private float width;
    private float height;
    private Direction direction;

    public Player(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.direction = Direction.DOWN;
    }

    public void move(float dx, float dy) {
        if (dx > 0) direction = Direction.RIGHT;
        if (dx < 0) direction = Direction.LEFT;
        if (dy > 0) direction = Direction.UP;
        if (dy < 0) direction = Direction.DOWN;

        this.x += dx;
        this.y += dy;
    }

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
}
