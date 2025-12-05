package com.fearjosh.frontend.manager;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.screen.PlayScreen;
import com.fearjosh.frontend.world.RoomId;

/**
 * Singleton GameManager to hold persistent game state
 * (player instance, current room, and future global attributes).
 */
public class GameManager {

    private static GameManager INSTANCE;

    private Player player;
    private RoomId currentRoomId;
    private PlayScreen playScreen;

    private float virtualWidth = 800f;
    private float virtualHeight = 600f;

    private GameManager() {
    }

    public static synchronized GameManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GameManager();
        }
        return INSTANCE;
    }

    public void initIfNeeded(float virtualWidth, float virtualHeight) {
        this.virtualWidth = virtualWidth;
        this.virtualHeight = virtualHeight;
        if (player == null) {
            // Create a default player larger for better visibility.
            // Adjust size here to scale the player sprite.
            float pw = 64f;
            float ph = 64f;
            player = new Player(virtualWidth / 2f - pw / 2f,
                    virtualHeight / 2f - ph / 2f,
                    pw, ph);
            player.loadAnimations();
        }
        if (currentRoomId == null) {
            currentRoomId = RoomId.R5; // default starting room
        }
    }

    public boolean hasActiveSession() {
        return player != null && currentRoomId != null;
    }

    public void resetNewGame(float virtualWidth, float virtualHeight) {
        // Discard existing player and room and start fresh
        this.virtualWidth = virtualWidth;
        this.virtualHeight = virtualHeight;
        this.player = null;
        this.currentRoomId = null;
        // Dispose cached screen if any
        if (playScreen != null) {
            try {
                playScreen.dispose();
            } catch (Exception ignored) {
            }
        }
        playScreen = null;
        initIfNeeded(virtualWidth, virtualHeight);
    }

    public PlayScreen getPlayScreen() {
        return playScreen;
    }

    public void setPlayScreen(PlayScreen screen) {
        this.playScreen = screen;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public RoomId getCurrentRoomId() {
        return currentRoomId;
    }

    public void setCurrentRoomId(RoomId id) {
        this.currentRoomId = id;
    }

    public float getVirtualWidth() {
        return virtualWidth;
    }

    public float getVirtualHeight() {
        return virtualHeight;
    }
}
