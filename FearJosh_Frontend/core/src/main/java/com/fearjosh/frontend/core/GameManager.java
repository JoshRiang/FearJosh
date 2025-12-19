package com.fearjosh.frontend.core;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.difficulty.DifficultyStrategy;
import com.fearjosh.frontend.difficulty.GameDifficulty;
import com.fearjosh.frontend.difficulty.EasyDifficulty;
import com.fearjosh.frontend.difficulty.MediumDifficulty;
import com.fearjosh.frontend.difficulty.HardDifficulty;
import com.fearjosh.frontend.world.RoomId;

/**
 * Singleton GameManager to hold persistent game state
 * (player instance, current room, difficulty, and game state).
 */
public class GameManager {
    
    /**
     * GAME STATE SYSTEM
     * Mengontrol input, update, dan render berdasarkan state aktif
     */
    public enum GameState {
        MAIN_MENU,   // Di main menu, hanya tombol menu aktif
        PLAYING,     // In-game, world update + player bisa gerak
        PAUSED       // Game paused, overlay pause + tombol pause aktif
    }

    private static GameManager INSTANCE;

    private Player player;
    private RoomId currentRoomId;

    private float virtualWidth = 800f;
    private float virtualHeight = 600f;
    private GameDifficulty difficulty = GameDifficulty.MEDIUM;
    private DifficultyStrategy difficultyStrategy = new MediumDifficulty();
    
    // TESTING MODE - untuk testing gameplay tanpa enemy mengganggu
    private boolean testingMode = false;
    
    // GAME STATE - kontrol input/update/render per state
    private GameState currentState = GameState.MAIN_MENU;

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
            float pw = 64f;
            float ph = 64f;
            player = new Player(virtualWidth / 2f - pw / 2f,
                    virtualHeight / 2f - ph / 2f,
                    pw, ph);
            player.loadAnimations();
        }
        if (currentRoomId == null) {
            currentRoomId = RoomId.R5;
        }
    }

    public boolean hasActiveSession() {
        return player != null && currentRoomId != null;
    }

    public void resetNewGame(float virtualWidth, float virtualHeight) {
        this.virtualWidth = virtualWidth;
        this.virtualHeight = virtualHeight;
        this.player = null;
        this.currentRoomId = null;
        initIfNeeded(virtualWidth, virtualHeight);
    }

    public GameDifficulty getDifficulty() {
        return difficulty;
    }

    public DifficultyStrategy getDifficultyStrategy() {
        return difficultyStrategy;
    }

    public void setDifficulty(GameDifficulty diff) {
        this.difficulty = diff;
        switch (diff) {
            case EASY:
                this.difficultyStrategy = new EasyDifficulty();
                break;
            case MEDIUM:
                this.difficultyStrategy = new MediumDifficulty();
                break;
            case HARD:
                this.difficultyStrategy = new HardDifficulty();
                break;
        }
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
    
    // ------------ TESTING MODE ------------
    
    public boolean isTestingMode() {
        return testingMode;
    }
    
    public void setTestingMode(boolean testingMode) {
        this.testingMode = testingMode;
    }
    
    // ------------ GAME STATE SYSTEM ------------
    
    public GameState getCurrentState() {
        return currentState;
    }
    
    public void setCurrentState(GameState state) {
        this.currentState = state;
        System.out.println("[GameStateManager] State changed to: " + state);
    }
    
    public boolean isInMenu() {
        return currentState == GameState.MAIN_MENU;
    }
    
    public boolean isPlaying() {
        return currentState == GameState.PLAYING;
    }
    
    public boolean isPaused() {
        return currentState == GameState.PAUSED;
    }
}
