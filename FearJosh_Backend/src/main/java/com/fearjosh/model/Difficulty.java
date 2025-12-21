package com.fearjosh.model;

public enum Difficulty {
    EASY("Easy"),
    NORMAL("Normal"),
    HARD("Hard"),
    NIGHTMARE("Nightmare");

    private final String displayName;

    Difficulty(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Difficulty fromString(String text) {
        for (Difficulty d : Difficulty.values()) {
            if (d.name().equalsIgnoreCase(text) || d.displayName.equalsIgnoreCase(text)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown difficulty: " + text);
    }
}
