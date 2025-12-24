package com.fearjosh.frontend.core;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.difficulty.DifficultyStrategy;
import com.fearjosh.frontend.difficulty.GameDifficulty;
import com.fearjosh.frontend.difficulty.EasyDifficulty;
import com.fearjosh.frontend.difficulty.MediumDifficulty;
import com.fearjosh.frontend.difficulty.HardDifficulty;
import com.fearjosh.frontend.world.RoomId;
import com.fearjosh.frontend.systems.Inventory;

public class GameManager {

    // STATE
    public enum GameState {
        MAIN_MENU,
        CUTSCENE,
        STORY,
        PLAYING,
        PAUSED,
        GAME_OVER,
        ENDING
    }

    // STORY
    private boolean hasMetJosh = false;
    private boolean storyObjectiveIsEscape = false;

    private static GameManager INSTANCE;

    // SESSION
    private GameSession currentSession;

    // ROOM
    private RoomDirector roomDirector;
    private JoshSpawnController joshSpawnController;

    private Player player;
    private RoomId currentRoomId;

    private float virtualWidth = 800f;
    private float virtualHeight = 600f;
    private GameDifficulty difficulty = GameDifficulty.MEDIUM;
    private DifficultyStrategy difficultyStrategy = new MediumDifficulty();

    private boolean testingMode = false;
    private GameState currentState = GameState.MAIN_MENU;

    // LIVES
    private int maxLives = 2;
    private int currentLives = 2;

    // INVENTORY
    private Inventory inventory;

    private GameManager() {
        inventory = new Inventory();
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
            // sizing
            float pw = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_WIDTH;
            float ph = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_HEIGHT;
            player = new Player(virtualWidth / 2f - pw / 2f,
                    virtualHeight / 2f - ph / 2f,
                    pw, ph);
            player.loadAnimations();
        }
        if (currentRoomId == null) {
            currentRoomId = RoomId.LOBBY; // Start in LOBBY
        }

        initializeLives();
    }

    // INIT
    private void initializeLives() {
        switch (difficulty) {
            case EASY:
                maxLives = 3;
                break;
            case MEDIUM:
                maxLives = 2;
                break;
            case HARD:
                maxLives = 1;
                break;
        }
        currentLives = maxLives;
    }

    public boolean hasActiveSession() {
        return currentSession != null && currentSession.isActive();
    }

    public GameSession getCurrentSession() {
        return currentSession;
    }

    public void startNewGame(float virtualWidth, float virtualHeight) {
        this.virtualWidth = virtualWidth;
        this.virtualHeight = virtualHeight;

        // RESET
        this.hasMetJosh = false;
        this.storyObjectiveIsEscape = false;
        resetInventory();
        com.fearjosh.frontend.systems.KeyManager.getInstance().reset();
        this.player = null;
        this.currentRoomId = null;
        initIfNeeded(virtualWidth, virtualHeight);
        initializeLives();

        RoomId startRoom = RoomId.LOBBY;
        currentSession = new GameSession(
                difficulty,
                startRoom,
                player.getX(),
                player.getY());

        initializeRoomDirector(startRoom);

        System.out.println("[GameManager] NEW GAME started: " + currentSession);
        System.out.println("[GameManager] Story flags reset - hasMetJosh: " + hasMetJosh + ", objectiveIsEscape: " + storyObjectiveIsEscape);
    }

    public void resumeSession() {
        if (!hasActiveSession()) {
            System.err.println("[GameManager] ERROR: No active session to resume!");
            return;
        }

        currentSession.restoreToPlayer(player);
        currentRoomId = currentSession.getCurrentRoomId();

        System.out.println("[GameManager] RESUMED session: " + currentSession);
    }

    public void saveProgressToSession() {
        if (currentSession != null && currentSession.isActive()) {
            currentSession.updateFromPlayer(player, currentRoomId);
        }
    }

    public void clearSession() {
        if (currentSession != null) {
            currentSession.endSession();
            currentSession = null;
            System.out.println("[GameManager] Session cleared - no Resume available");
        }
    }

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

    public boolean canChangeDifficultyFreely() {
        return !hasActiveSession();
    }

    public boolean difficultyChangeRequiresNewGame() {
        return hasActiveSession();
    }

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

    public boolean isTestingMode() {
        return testingMode;
    }

    public void setTestingMode(boolean testingMode) {
        this.testingMode = testingMode;
    }

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
        return currentState == GameState.PLAYING || currentState == GameState.STORY;
    }

    public boolean isPaused() {
        return currentState == GameState.PAUSED;
    }
    
    public boolean isInStoryMode() {
        return currentState == GameState.STORY;
    }
    
    public boolean isInFullPlayMode() {
        return currentState == GameState.PLAYING;
    }

    public boolean isInCutscene() {
        return currentState == GameState.CUTSCENE;
    }

    public boolean isInTutorialState() {
        return currentState == GameState.STORY && !hasMetJosh;
    }

    public boolean isInteractionAllowed(String interactionType, String targetId) {
        if (currentState == GameState.PLAYING) {
            return true;
        }

        if (isInTutorialState()) {
            if ("door".equals(interactionType) && targetId != null) {
                return targetId.toLowerCase().contains("gym") ||
                       targetId.toLowerCase().contains("gymnasium");
            }
            return false;
        }

        if (currentState == GameState.CUTSCENE) {
            return false;
        }

        return true;
    }

    public boolean hasMetJosh() {
        return hasMetJosh;
    }

    public void setHasMetJosh(boolean met) {
        this.hasMetJosh = met;
        if (met) {
            this.storyObjectiveIsEscape = true;
        }
        System.out.println("[Story] Josh encounter: " + met + ", Objective is now ESCAPE: " + storyObjectiveIsEscape);
    }

    public boolean isObjectiveEscape() {
        return storyObjectiveIsEscape;
    }

    public void transitionToFullPlayMode() {
        if (currentState == GameState.STORY) {
            setCurrentState(GameState.PLAYING);
            setHasMetJosh(true);
            System.out.println("[Story] Transitioned to FULL PLAY mode - all interactions enabled");
        }
    }

    // INIT
    private void initializeRoomDirector(RoomId playerStartRoom) {
        RoomId enemyStartRoom = getRandomDistantRoom(playerStartRoom);
        roomDirector = new RoomDirector(playerStartRoom, enemyStartRoom);
        roomDirector.setDebugMode(com.fearjosh.frontend.config.Constants.DEBUG_ROOM_DIRECTOR);

        System.out.println("[GameManager] RoomDirector initialized: enemy starts in " + enemyStartRoom);

        joshSpawnController = new JoshSpawnController();
        joshSpawnController.setDebugMode(com.fearjosh.frontend.config.Constants.DEBUG_ROOM_DIRECTOR);
        System.out.println("[GameManager] JoshSpawnController initialized");
    }

    public RoomDirector getRoomDirector() {
        return roomDirector;
    }

    public JoshSpawnController getJoshSpawnController() {
        return joshSpawnController;
    }

    public void notifyPlayerRoomChange(RoomId newRoom) {
        RoomId previousRoom = currentRoomId;
        if (roomDirector != null) {
            roomDirector.onPlayerEnterRoom(newRoom);
        }
        if (joshSpawnController != null) {
            joshSpawnController.onPlayerEnterRoom(newRoom, previousRoom);
        }
    }

    private RoomId getRandomDistantRoom(RoomId start) {
        RoomId[] distantRooms = { RoomId.CLASS_1A, RoomId.CLASS_8A, RoomId.CLASS_1B, RoomId.CLASS_8B, RoomId.GYM };

        // FILTER
        java.util.List<RoomId> validRooms = new java.util.ArrayList<>();
        for (RoomId room : distantRooms) {
            if (room != start) {
                validRooms.add(room);
            }
        }

        if (validRooms.isEmpty()) {
            return RoomId.HALLWAY;
        }

        int randomIndex = (int) (Math.random() * validRooms.size());
        return validRooms.get(randomIndex);
    }

    public int getCurrentLives() {
        return currentLives;
    }

    public int getMaxLives() {
        return maxLives;
    }

    public boolean loseLife() {
        currentLives--;
        System.out.println("[Health] Life lost! Remaining: " + currentLives);

        if (currentLives <= 0) {
            System.out.println("[Game Over] No more lives!");
            return true; // Game over
        }

        return false;
    }

    public void gainLife() {
        if (currentLives < maxLives) {
            currentLives++;
            System.out.println("[Health] Life gained! Current: " + currentLives);
        }
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void resetInventory() {
        if (inventory != null) {
            inventory.clear();
        } else {
            inventory = new Inventory();
        }
    }

    public boolean isGameOver() {
        return currentLives <= 0;
    }
}
