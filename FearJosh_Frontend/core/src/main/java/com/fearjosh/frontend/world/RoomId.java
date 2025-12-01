package com.fearjosh.frontend.world;

public enum RoomId {
    R1(0, 0),
    R2(1, 0),
    R3(2, 0),
    R4(0, 1),
    R5(1, 1),
    R6(2, 1),
    R7(0, 2),
    R8(1, 2),
    R9(2, 2);

    private final int col;
    private final int row;

    RoomId(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public RoomId up() {
        return from(col, row - 1);
    }

    public RoomId down() {
        return from(col, row + 1);
    }

    public RoomId left() {
        return from(col - 1, row);
    }

    public RoomId right() {
        return from(col + 1, row);
    }

    public static RoomId from(int col, int row) {
        for (RoomId id : values()) {
            if (id.col == col && id.row == row) {
                return id;
            }
        }
        return null;
    }

    public boolean hasUp() {
        return up() != null;
    }

    public boolean hasDown() {
        return down() != null;
    }

    public boolean hasLeft() {
        return left() != null;
    }

    public boolean hasRight() {
        return right() != null;
    }
}
