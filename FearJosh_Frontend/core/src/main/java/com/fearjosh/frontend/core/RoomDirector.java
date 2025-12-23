package com.fearjosh.frontend.core;

import com.fearjosh.frontend.world.RoomId;
import com.fearjosh.frontend.systems.AudioManager;

/**
 * RoomDirector - Manages enemy presence and stalking behavior across rooms.
 * 
 * HYBRID STALKER SYSTEM:
 * - ABSTRACT MODE: Enemy "exists" in different room, moves via adjacency
 * - PHYSICAL MODE: Enemy enters player's room through door, becomes visible
 * 
 * Key Features:
 * - No instant teleport to player
 * - Grace period after room transition
 * - Door-based entry system
 * - Adjacency-based navigation
 */
public class RoomDirector {

    // ==================== EVENT LISTENER ====================
    
    /**
     * Listener interface for RoomDirector events (camera shake, etc.)
     * PlayScreen should implement this to handle visual effects
     */
    public interface RoomDirectorEventListener {
        void onCameraShake(float duration, float intensity);
        void onDoorEntry(DoorDirection direction);
    }
    
    private RoomDirectorEventListener eventListener;
    
    public void setEventListener(RoomDirectorEventListener listener) {
        this.eventListener = listener;
    }
    
    public RoomDirectorEventListener getEventListener() {
        return this.eventListener;
    }

    // ==================== TRACKING ====================

    private RoomId playerRoom;
    private RoomId enemyRoom;
    private RoomId lastEnemyRoom; // Track where enemy came from (for spawn direction)

    // ==================== TIMING CONSTANTS ====================

    /** How often enemy moves between rooms in abstract mode (seconds) */
    private static final float ENEMY_MOVE_COOLDOWN_MIN = 6f;
    private static final float ENEMY_MOVE_COOLDOWN_MAX = 10f;

    /** Grace period after player enters new room (seconds) */
    private static final float PLAYER_GRACE_PERIOD_MIN = 3f;
    private static final float PLAYER_GRACE_PERIOD_MAX = 5f;

    /** Delay before enemy physically spawns at door (seconds) */
    private static final float DOOR_ENTRY_DELAY = 1.5f;

    /** Transition buffer duration when enemy "freezes" at door (seconds) */
    private static final float TRANSITION_BUFFER_DURATION = 2.0f;

    /** Maximum room distance before teleporting closer (tension mechanic) */
    private static final int MAX_ROOM_DISTANCE = 3;

    // ==================== TIMERS ====================

    private float enemyMoveCooldown;
    private float enemyMoveTimer;

    private float playerGracePeriod;
    private float graceTimer;

    private float doorEntryTimer;

    // ==================== STATE FLAGS ====================

    private boolean enemyReadyToEnterRoom;
    private boolean enemyPhysicallyPresent;
    private boolean doorEntryTriggered;

    // Transition buffer state
    private boolean inTransitionBuffer;
    private float transitionBufferTimer;
    private RoomId transitionTargetRoom;
    private RoomId frozenAtRoom; // Room where enemy is "frozen" at door

    // Door direction for entry
    private DoorDirection entryDirection;

    // ==================== DEBUG ====================

    private boolean debugMode = false;

    public enum DoorDirection {
        TOP, BOTTOM, LEFT, RIGHT, NONE
    }

    // ==================== CONSTRUCTOR ====================

    public RoomDirector(RoomId startingPlayerRoom, RoomId startingEnemyRoom) {
        this.playerRoom = startingPlayerRoom;

        // Testing mode: spawn enemy in same room as player for instant spawn
        if (GameManager.getInstance().isTestingMode()) {
            this.enemyRoom = startingPlayerRoom; // Same room as player
            this.lastEnemyRoom = startingPlayerRoom;
        } else {
            this.enemyRoom = startingEnemyRoom;
            this.lastEnemyRoom = startingEnemyRoom;
        }

        // Randomize cooldowns for unpredictability
        this.enemyMoveCooldown = randomRange(ENEMY_MOVE_COOLDOWN_MIN, ENEMY_MOVE_COOLDOWN_MAX);
        this.playerGracePeriod = randomRange(PLAYER_GRACE_PERIOD_MIN, PLAYER_GRACE_PERIOD_MAX);

        this.enemyMoveTimer = 0f;
        this.graceTimer = playerGracePeriod; // Start with grace expired
        this.doorEntryTimer = 0f;

        this.enemyReadyToEnterRoom = false;
        this.enemyPhysicallyPresent = (enemyRoom == playerRoom); // Will be true in testing mode
        this.doorEntryTriggered = false;
        this.entryDirection = DoorDirection.NONE;

        this.inTransitionBuffer = false;
        this.transitionBufferTimer = 0f;
        this.transitionTargetRoom = null;
        this.frozenAtRoom = null;

        System.out.println("[RoomDirector] Constructor - Testing Mode: " + GameManager.getInstance().isTestingMode());
        System.out.println("[RoomDirector] Player Room: " + playerRoom + ", Enemy Room: " + enemyRoom);
        System.out.println("[RoomDirector] Enemy Physically Present: " + enemyPhysicallyPresent);

        if (debugMode) {
            System.out.println("[RoomDirector] Initialized: player=" + playerRoom + ", enemy=" + enemyRoom);
        }
    }

    // ==================== UPDATE LOOP ====================

    /**
     * Main update loop - call this every frame
     */
    public void update(float delta) {
        // === STORY STATE CHECK ===
        // Don't update enemy behavior during STORY state (before meeting Josh)
        GameManager gm = GameManager.getInstance();
        if (gm.getCurrentState() == GameManager.GameState.STORY && !gm.hasMetJosh()) {
            // Player hasn't met Josh yet, don't let enemy spawn
            return;
        }
        
        // === ENDING STATE CHECK ===
        // Don't update during ending sequence
        if (gm.getCurrentState() == GameManager.GameState.ENDING) {
            return;
        }
        
        // Update grace timer
        if (graceTimer < playerGracePeriod) {
            graceTimer += delta;
        }

        // TRANSITION BUFFER: Enemy is "frozen" at door
        if (inTransitionBuffer) {
            updateTransitionBuffer(delta);
            return;
        }

        // ABSTRACT MODE: Enemy in different room
        if (!isEnemyInSameRoom()) {
            updateAbstractMode(delta);
        }
        // PHYSICAL MODE: Enemy in same room
        else {
            updatePhysicalMode(delta);
        }
    }

    /**
     * ABSTRACT MODE: Enemy navigates between rooms
     */
    private void updateAbstractMode(float delta) {
        enemyMoveTimer += delta;

        // Time to move enemy closer
        if (enemyMoveTimer >= enemyMoveCooldown) {
            enemyMoveTimer = 0f;
            enemyMoveCooldown = randomRange(ENEMY_MOVE_COOLDOWN_MIN, ENEMY_MOVE_COOLDOWN_MAX);

            // Save current room BEFORE moving (for entry direction calculation)
            RoomId roomBeforeMove = enemyRoom;

            // TENSION MECHANIC: If enemy is too far, teleport to adjacent room
            int distance = calculateRoomDistance(enemyRoom, playerRoom);
            if (distance > MAX_ROOM_DISTANCE) {
                enemyRoom = teleportToAdjacentRoom(playerRoom);
                // When teleporting, set lastEnemyRoom to the teleport destination's neighbor
                // (enemy appears to come from adjacent room)
                lastEnemyRoom = roomBeforeMove;
                if (debugMode) {
                    System.out.println("[RoomDirector] TENSION: Enemy teleported to adjacent room " + enemyRoom);
                }
            } else {
                // Normal movement: one room closer
                lastEnemyRoom = enemyRoom; // Save BEFORE moving
                enemyRoom = moveCloser(enemyRoom, playerRoom);

                if (debugMode) {
                    System.out.println("[RoomDirector] Enemy moved: " + lastEnemyRoom + " -> " + enemyRoom);
                }
            }

            // Check if enemy is now adjacent to player
            if (isEnemyAdjacentToPlayer()) {
                triggerAudioCue();
            }

            // Check if enemy reached player's room
            if (isEnemyInSameRoom()) {
                enemyReadyToEnterRoom = true;
                doorEntryTriggered = false;
                // Use lastEnemyRoom (where enemy came FROM) for correct door direction
                entryDirection = calculateEntryDirection(lastEnemyRoom);

                if (debugMode) {
                    System.out.println("[RoomDirector] Enemy reached player room from " + lastEnemyRoom + " via "
                            + entryDirection);
                }
            }
        }
    }

    /**
     * PHYSICAL MODE: Handle door entry and physical presence
     */
    private void updatePhysicalMode(float delta) {
        // During grace period, enemy can't enter yet (skip in testing mode)
        if (!GameManager.getInstance().isTestingMode() && graceTimer < playerGracePeriod) {
            if (debugMode && !doorEntryTriggered) {
                System.out.println("[RoomDirector] Grace period active: " +
                        String.format("%.1f", (playerGracePeriod - graceTimer)) + "s remaining");
            }
            return;
        }

        // Grace period expired, enemy can enter
        if (enemyReadyToEnterRoom && !doorEntryTriggered) {
            triggerDoorEntry();
            doorEntryTriggered = true;
            doorEntryTimer = 0f;
        }

        // Wait for door entry animation/sound
        if (doorEntryTriggered && !enemyPhysicallyPresent) {
            doorEntryTimer += delta;

            if (doorEntryTimer >= DOOR_ENTRY_DELAY) {
                spawnEnemyAtDoor();
                enemyPhysicallyPresent = true;
                enemyReadyToEnterRoom = false;

                if (debugMode) {
                    System.out.println("[RoomDirector] Enemy physically spawned at door");
                }
            }
        }
    }

    // ==================== NAVIGATION ====================

    /**
     * Move enemy one room closer to player using Manhattan adjacency
     * Grid layout:
     * R1 R2 R3
     * R4 R5 R6
     * R7 R8 R9
     */
    private RoomId moveCloser(RoomId from, RoomId to) {
        if (from == to)
            return from;

        int fromRow = getRow(from);
        int fromCol = getCol(from);
        int toRow = getRow(to);
        int toCol = getCol(to);

        // Prioritize vertical movement if different rows
        if (fromRow != toRow) {
            if (fromRow < toRow && from.down() != null) {
                return from.down(); // Move down
            } else if (fromRow > toRow && from.up() != null) {
                return from.up(); // Move up
            }
        }

        // Otherwise move horizontally
        if (fromCol != toCol) {
            if (fromCol < toCol && from.right() != null) {
                return from.right(); // Move right
            } else if (fromCol > toCol && from.left() != null) {
                return from.left(); // Move left
            }
        }

        // Fallback: stay in place
        return from;
    }

    /**
     * Get row index based on room type (simplified for school layout)
     * Row 0 = Bottom (Parking, Bottom classrooms)
     * Row 1 = Middle (Lobby, Hallway, Special rooms)
     * Row 2 = Top (Top classrooms, Gym)
     */
    private int getRow(RoomId room) {
        if (room == null)
            return 1;
        String name = room.name();

        // Top row - classrooms ending with A and gym
        if (name.endsWith("A") || name.equals("GYM")) {
            return 2;
        }
        // Bottom row - classrooms ending with B and parking
        if (name.endsWith("B") || name.equals("PARKING")) {
            return 0;
        }
        // Middle row - hallway, lobby, special rooms
        return 1;
    }

    /**
     * Get column index based on room type (simplified for school layout)
     * Returns relative position 0-2 for approximate location
     */
    private int getCol(RoomId room) {
        if (room == null)
            return 1;
        String name = room.name();

        // Left side - parking, lobby, class 1-3
        if (name.equals("PARKING") || name.equals("LOBBY") ||
                name.contains("_1") || name.contains("_2") || name.contains("_3")) {
            return 0;
        }
        // Middle - hallway, class 4-5, janitor, restroom
        if (name.equals("HALLWAY") || name.equals("GYM") ||
                name.contains("_4") || name.contains("_5") ||
                name.equals("JANITOR") || name.equals("RESTROOM")) {
            return 1;
        }
        // Right side - class 6-8, teachers room
        return 2;
    }

    /**
     * Calculate which door enemy should enter from
     */
    private DoorDirection calculateEntryDirection(RoomId previousRoom) {
        if (previousRoom == null)
            return DoorDirection.NONE;

        if (previousRoom == playerRoom.up())
            return DoorDirection.TOP;
        if (previousRoom == playerRoom.down())
            return DoorDirection.BOTTOM;
        if (previousRoom == playerRoom.left())
            return DoorDirection.LEFT;
        if (previousRoom == playerRoom.right())
            return DoorDirection.RIGHT;

        return DoorDirection.NONE;
    }

    // ==================== STATE CHECKS ====================

    public boolean isEnemyInSameRoom() {
        return enemyRoom == playerRoom;
    }

    public boolean isEnemyAdjacentToPlayer() {
        return enemyRoom == playerRoom.up() ||
                enemyRoom == playerRoom.down() ||
                enemyRoom == playerRoom.left() ||
                enemyRoom == playerRoom.right();
    }

    public boolean isEnemyPhysicallyPresent() {
        return enemyPhysicallyPresent;
    }

    public boolean isDoorEntryPending() {
        return doorEntryTriggered && !enemyPhysicallyPresent;
    }

    // ==================== EVENTS ====================

    /**
     * Called when player enters a new room
     */
    public void onPlayerEnterRoom(RoomId newRoom) {
        RoomId oldRoom = playerRoom;
        playerRoom = newRoom;
        graceTimer = 0f; // Reset grace period

        // IMMEDIATE RETURN ("Cilukba") CHECK
        // If enemy was in transition buffer heading to oldRoom, and player returns
        if (inTransitionBuffer && frozenAtRoom == newRoom) {
            // JUMP SCARE: Enemy is still at door, instantly spawn!
            cancelTransitionBuffer();
            enemyRoom = newRoom;
            enemyPhysicallyPresent = true;
            enemyReadyToEnterRoom = false;
            doorEntryTriggered = true;
            // Entry direction is from where enemy was frozen (previous room)
            // lastEnemyRoom already set when transition started
            entryDirection = calculateEntryDirection(lastEnemyRoom);

            if (debugMode) {
                System.out.println("[RoomDirector] JUMP SCARE: Player returned! Enemy at " + entryDirection
                        + " door (from " + lastEnemyRoom + ")");
            }
            return;
        }

        // If enemy was physically present in old room, enter transition buffer
        if (enemyPhysicallyPresent && enemyRoom == oldRoom) {
            // Update lastEnemyRoom to track where enemy is coming FROM
            lastEnemyRoom = oldRoom;
            enterTransitionBuffer(oldRoom, newRoom);
        }

        if (debugMode) {
            System.out.println("[RoomDirector] Player entered room: " + newRoom +
                    " (grace period: " + playerGracePeriod + "s)");
        }
    }

    /**
     * Called when enemy is physically despawned/killed
     */
    public void onEnemyDespawn() {
        enemyPhysicallyPresent = false;
        enemyReadyToEnterRoom = false;
        doorEntryTriggered = false;
        enemyMoveTimer = 0f;

        if (debugMode) {
            System.out.println("[RoomDirector] Enemy despawned");
        }
    }

    /**
     * Get all adjacent rooms for a given room
     * 
     * @param room The room to get adjacents for
     * @return Array of adjacent RoomIds (may contain nulls if no adjacent room
     *         exists)
     */
    private RoomId[] getAdjacentRooms(RoomId room) {
        return new RoomId[] {
                room.up(),
                room.down(),
                room.left(),
                room.right()
        };
    }

    /**
     * Force enemy to retreat to a different room (used when player is captured)
     * Enemy will move to a random adjacent room that is NOT the player's room
     */
    public void forceEnemyRetreat() {
        // Get adjacent rooms to player
        RoomId[] adjacentRooms = getAdjacentRooms(playerRoom);

        // Filter out player's current room
        java.util.List<RoomId> validRooms = new java.util.ArrayList<>();
        for (RoomId room : adjacentRooms) {
            if (room != playerRoom) {
                validRooms.add(room);
            }
        }

        // If no adjacent rooms available, use any other room
        if (validRooms.isEmpty()) {
            // Fallback: pick a random room from all rooms
            RoomId[] allRooms = RoomId.values();
            for (RoomId room : allRooms) {
                if (room != playerRoom) {
                    validRooms.add(room);
                }
            }
        }

        // Move enemy to random retreat room
        if (!validRooms.isEmpty()) {
            int randomIndex = (int) (Math.random() * validRooms.size());
            lastEnemyRoom = enemyRoom;
            enemyRoom = validRooms.get(randomIndex);

            // Reset enemy state
            enemyPhysicallyPresent = false;
            enemyReadyToEnterRoom = false;
            doorEntryTriggered = false;
            entryDirection = DoorDirection.NONE;

            // Reset timers to give player breathing room
            enemyMoveTimer = 0f;
            enemyMoveCooldown = randomRange(ENEMY_MOVE_COOLDOWN_MIN * 2f, ENEMY_MOVE_COOLDOWN_MAX * 2f); // Longer
                                                                                                         // cooldown

            System.out.println("[RoomDirector] Enemy retreated from " + lastEnemyRoom + " to " + enemyRoom
                    + " (player in " + playerRoom + ")");
        }
    }

    /**
     * Trigger audio cue when enemy is adjacent (breathing, footsteps)
     * Plays directional audio hint based on where the enemy is relative to player
     */
    private void triggerAudioCue() {
        AudioManager audio = AudioManager.getInstance();
        
        // Play ambient breathing/presence sound based on entry direction
        // Using existing grunt sound as presence indicator
        String audioPath = "Audio/Effect/monster_grunt_sound_effect.wav";
        
        // Play at reduced volume for distant presence hint
        audio.playSound(audioPath, 0.3f);

        if (debugMode) {
            System.out.println("[RoomDirector] AUDIO CUE: Enemy nearby from direction " + entryDirection);
        }
    }

    /**
     * Trigger door entry event (sound, camera shake)
     * Called when enemy physically enters player's room through a door
     */
    private void triggerDoorEntry() {
        AudioManager audio = AudioManager.getInstance();
        
        // Play door opening/slam sound (using existing footstep as door sound placeholder)
        audio.playSound("Audio/Effect/footstep_sound_effect.wav", 0.8f);
        
        // Trigger camera shake via listener (PlayScreen handles actual shake)
        if (eventListener != null) {
            eventListener.onCameraShake(0.3f, 5f); // 0.3 second, 5 pixel intensity
            eventListener.onDoorEntry(entryDirection);
        }
        
        // Play heavy footsteps/monster entering sound
        audio.playSound("Audio/Effect/monster_roar_sound_effect.wav", 0.6f);

        if (debugMode) {
            System.out.println("[RoomDirector] DOOR ENTRY triggered from " + entryDirection);
        }
    }

    /**
     * Spawn enemy at the appropriate door position
     * Returns position where enemy should spawn
     */
    private void spawnEnemyAtDoor() {
        // This will be called by game to get spawn position
        // See getEnemySpawnPosition() for coordinates
    }

    // ==================== TRANSITION BUFFER SYSTEM ====================

    /**
     * Enter transition buffer state (enemy freezes at door)
     */
    private void enterTransitionBuffer(RoomId currentRoom, RoomId targetRoom) {
        inTransitionBuffer = true;
        transitionBufferTimer = 0f;
        transitionTargetRoom = targetRoom;
        frozenAtRoom = currentRoom;

        enemyPhysicallyPresent = false;
        enemyReadyToEnterRoom = false;
        doorEntryTriggered = false;

        if (debugMode) {
            System.out.println("[RoomDirector] TRANSITION BUFFER: Enemy frozen at door in " + frozenAtRoom);
        }
    }

    /**
     * Update transition buffer timer
     */
    private void updateTransitionBuffer(float delta) {
        transitionBufferTimer += delta;

        // Buffer expired, enemy can now move to target room
        if (transitionBufferTimer >= TRANSITION_BUFFER_DURATION) {
            exitTransitionBuffer();
        }
    }

    /**
     * Exit transition buffer and move enemy to target room
     */
    private void exitTransitionBuffer() {
        if (!inTransitionBuffer)
            return;

        inTransitionBuffer = false;

        // lastEnemyRoom was set when transition started (= frozenAtRoom)
        // This is where enemy is coming FROM
        enemyRoom = transitionTargetRoom;

        if (debugMode) {
            System.out.println(
                    "[RoomDirector] TRANSITION COMPLETE: Enemy moved from " + lastEnemyRoom + " to " + enemyRoom);
        }

        // Check if enemy is now in player's room
        if (isEnemyInSameRoom()) {
            enemyReadyToEnterRoom = true;
            // Use lastEnemyRoom for correct door direction
            entryDirection = calculateEntryDirection(lastEnemyRoom);

            if (debugMode) {
                System.out.println(
                        "[RoomDirector] Enemy entering via " + entryDirection + " door (from " + lastEnemyRoom + ")");
            }
        }

        transitionTargetRoom = null;
        frozenAtRoom = null;
    }

    /**
     * Cancel transition buffer (for immediate return case)
     */
    private void cancelTransitionBuffer() {
        inTransitionBuffer = false;
        transitionBufferTimer = 0f;
        transitionTargetRoom = null;
        frozenAtRoom = null;

        if (debugMode) {
            System.out.println("[RoomDirector] Transition buffer CANCELLED");
        }
    }

    // ==================== DISTANCE & TELEPORT MECHANICS ====================

    /**
     * Calculate Manhattan distance between two rooms
     */
    private int calculateRoomDistance(RoomId from, RoomId to) {
        int fromRow = getRow(from);
        int fromCol = getCol(from);
        int toRow = getRow(to);
        int toCol = getCol(to);

        return Math.abs(fromRow - toRow) + Math.abs(fromCol - toCol);
    }

    /**
     * Teleport enemy to random adjacent room of target (tension mechanic)
     */
    private RoomId teleportToAdjacentRoom(RoomId target) {
        java.util.List<RoomId> adjacentRooms = new java.util.ArrayList<>();

        if (target.up() != null)
            adjacentRooms.add(target.up());
        if (target.down() != null)
            adjacentRooms.add(target.down());
        if (target.left() != null)
            adjacentRooms.add(target.left());
        if (target.right() != null)
            adjacentRooms.add(target.right());

        if (adjacentRooms.isEmpty())
            return target; // Fallback

        int randomIndex = (int) (Math.random() * adjacentRooms.size());
        return adjacentRooms.get(randomIndex);
    }

    // ==================== GETTERS ====================

    public RoomId getPlayerRoom() {
        return playerRoom;
    }

    public RoomId getEnemyRoom() {
        return enemyRoom;
    }

    public RoomId getLastEnemyRoom() {
        return lastEnemyRoom;
    }

    public DoorDirection getEntryDirection() {
        return entryDirection;
    }

    public float getGraceTimeRemaining() {
        return Math.max(0, playerGracePeriod - graceTimer);
    }

    /**
     * Get spawn position for enemy based on entry direction
     * Call this when isEnemyPhysicallyPresent() becomes true
     * 
     * @param playerX Player X position (used in testing mode)
     * @param playerY Player Y position (used in testing mode)
     */
    public float[] getEnemySpawnPosition(float worldWidth, float worldHeight, float enemyWidth, float enemyHeight,
            float playerX, float playerY) {
        float x, y;
        float margin = 20f; // Distance from wall

        switch (entryDirection) {
            case TOP:
                x = worldWidth / 2f - enemyWidth / 2f;
                y = worldHeight - margin - enemyHeight;
                break;
            case BOTTOM:
                x = worldWidth / 2f - enemyWidth / 2f;
                y = margin;
                break;
            case LEFT:
                x = margin;
                y = worldHeight / 2f - enemyHeight / 2f;
                break;
            case RIGHT:
                x = worldWidth - margin - enemyWidth;
                y = worldHeight / 2f - enemyHeight / 2f;
                break;
            default:
                // Testing mode: spawn 30px away from player
                if (GameManager.getInstance().isTestingMode()) {
                    x = playerX + 30f;
                    y = playerY + 30f;
                } else {
                    // Fallback to center if no direction
                    x = worldWidth / 2f - enemyWidth / 2f;
                    y = worldHeight / 2f - enemyHeight / 2f;
                }
                break;
        }

        return new float[] { x, y };
    }

    // ==================== DEBUG ====================

    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public String getDebugInfo() {
        String baseInfo = String.format(
                "RoomDirector [Player: %s | Enemy: %s (from %s) | Physical: %b | Grace: %.1fs | Entry: %s",
                playerRoom, enemyRoom, lastEnemyRoom, enemyPhysicallyPresent, getGraceTimeRemaining(), entryDirection);

        if (inTransitionBuffer) {
            baseInfo += String.format(" | TRANSITION: %.1fs at %s->%s",
                    TRANSITION_BUFFER_DURATION - transitionBufferTimer, frozenAtRoom, transitionTargetRoom);
        }

        return baseInfo + "]";
    }

    // ==================== UTILITY ====================

    private float randomRange(float min, float max) {
        return min + (float) (Math.random() * (max - min));
    }
}
