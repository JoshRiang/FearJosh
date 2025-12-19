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

    // SESSION MANAGEMENT - NEW SYSTEM
    private GameSession currentSession;     // Active game session (null = no run)
    
    // Legacy fields (kept for backward compatibility during transition)
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
            // Use Constants for consistent sizing
            float pw = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_WIDTH;
            float ph = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_HEIGHT;
            player = new Player(virtualWidth / 2f - pw / 2f,
                    virtualHeight / 2f - ph / 2f,
                    pw, ph);
            player.loadAnimations();
        }
        if (currentRoomId == null) {
            currentRoomId = RoomId.R5;
        }
    }

    /**
     * Check if there's an active game session that can be resumed
     * @return true if currentSession exists and is active
     */
    public boolean hasActiveSession() {
        return currentSession != null && currentSession.isActive();
    }
    
    /**
     * Get current active session (may be null)
     */
    public GameSession getCurrentSession() {
        return currentSession;
    }
    
    /**
     * Start a NEW GAME - creates fresh session and resets all progress
     * This should be called when user clicks "New Game"
     */
    public void startNewGame(float virtualWidth, float virtualHeight) {
        this.virtualWidth = virtualWidth;
        this.virtualHeight = virtualHeight;
        
        // Reset player
        this.player = null;
        this.currentRoomId = null;
        initIfNeeded(virtualWidth, virtualHeight);
        
        // Create new session with current difficulty
        RoomId startRoom = RoomId.R5;  // Starting room
        currentSession = new GameSession(
            difficulty,
            startRoom,
            player.getX(),
            player.getY()
        );
        
        System.out.println("[GameManager] NEW GAME started: " + currentSession);
    }
    
    /**
     * RESUME existing session - restores progress without reset
     * This should be called when user clicks "Resume" from menu
     */
    public void resumeSession() {
        if (!hasActiveSession()) {
            System.err.println("[GameManager] ERROR: No active session to resume!");
            return;
        }
        
        // Restore player state from session
        currentSession.restoreToPlayer(player);
        currentRoomId = currentSession.getCurrentRoomId();
        
        System.out.println("[GameManager] RESUMED session: " + currentSession);
    }
    
    /**
     * Save current progress to session
     * Call this when pausing or changing rooms
     */
    public void saveProgressToSession() {
        if (currentSession != null && currentSession.isActive()) {
            currentSession.updateFromPlayer(player, currentRoomId);
        }
    }
    
    /**
     * @deprecated Use startNewGame() instead
     */
    @Deprecated
    public void resetNewGame(float virtualWidth, float virtualHeight) {
        startNewGame(virtualWidth, virtualHeight);
    }

    public GameDifficulty getDifficulty() {
        return difficulty;
    }

    public DifficultyStrategy getDifficultyStrategy() {
        return difficultyStrategy;
    }

    /**
     * Change difficulty setting.
     * NOTE: If active session exists, this will NOT take effect until New Game.
     * Use canChangeDifficulty() and requiresNewGame() to check before calling.
     */
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
    
    /**
     * Check if difficulty can be changed freely
     * @return false if active session exists (difficulty is locked)
     */
    public boolean canChangeDifficultyFreely() {
        return !hasActiveSession();
    }
    
    /**
     * Check if changing difficulty requires starting a new game
     * @return true if active session exists
     */
    public boolean difficultyChangeRequiresNewGame() {
        return hasActiveSession();
    }
    
    /**
     * Force difficulty change AND start new game
     * Use this after user confirms difficulty change popup
     */
    public void changeDifficultyAndStartNewGame(GameDifficulty newDiff, float virtualWidth, float virtualHeight) {
        setDifficulty(newDiff);
        startNewGame(virtualWidth, virtualHeight);
        System.out.println("[GameManager] Difficulty changed to " + newDiff + " with NEW GAME");
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
