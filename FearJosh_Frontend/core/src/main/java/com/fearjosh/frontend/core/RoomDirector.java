package com.fearjosh.frontend.core;

import com.fearjosh.frontend.world.RoomId;
import com.fearjosh.frontend.systems.AudioManager;

public class RoomDirector {

    // LISTENER
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

    // TRACKING
    private RoomId playerRoom;
    private RoomId enemyRoom;
    private RoomId lastEnemyRoom;

    // TIMING
    private static final float ENEMY_MOVE_COOLDOWN_MIN = 6f;
    private static final float ENEMY_MOVE_COOLDOWN_MAX = 10f;
    private static final float PLAYER_GRACE_PERIOD_MIN = 3f;
    private static final float PLAYER_GRACE_PERIOD_MAX = 5f;
    private static final float DOOR_ENTRY_DELAY = 1.5f;
    private static final float TRANSITION_BUFFER_DURATION = 2.0f;
    private static final int MAX_ROOM_DISTANCE = 3;

    // TIMERS
    private float enemyMoveCooldown;
    private float enemyMoveTimer;
    private float playerGracePeriod;
    private float graceTimer;
    private float doorEntryTimer;

    // STATE
    private boolean enemyReadyToEnterRoom;
    private boolean enemyPhysicallyPresent;
    private boolean doorEntryTriggered;

    // TRANSITION
    private boolean inTransitionBuffer;
    private float transitionBufferTimer;
    private RoomId transitionTargetRoom;
    private RoomId frozenAtRoom;
    private DoorDirection entryDirection;
    private boolean debugMode = false;

    public enum DoorDirection {
        TOP, BOTTOM, LEFT, RIGHT, NONE
    }

    // INIT
    public RoomDirector(RoomId startingPlayerRoom, RoomId startingEnemyRoom) {
        this.playerRoom = startingPlayerRoom;

        if (GameManager.getInstance().isTestingMode()) {
            this.enemyRoom = startingPlayerRoom;
            this.lastEnemyRoom = startingPlayerRoom;
        } else {
            this.enemyRoom = startingEnemyRoom;
            this.lastEnemyRoom = startingEnemyRoom;
        }

        this.enemyMoveCooldown = randomRange(ENEMY_MOVE_COOLDOWN_MIN, ENEMY_MOVE_COOLDOWN_MAX);
        this.playerGracePeriod = randomRange(PLAYER_GRACE_PERIOD_MIN, PLAYER_GRACE_PERIOD_MAX);

        this.enemyMoveTimer = 0f;
        this.graceTimer = playerGracePeriod;
        this.doorEntryTimer = 0f;

        this.enemyReadyToEnterRoom = false;
        this.enemyPhysicallyPresent = (enemyRoom == playerRoom);
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

    // UPDATE
    public void update(float delta) {
        // STORY CHECK
        GameManager gm = GameManager.getInstance();
        if (gm.getCurrentState() == GameManager.GameState.STORY && !gm.hasMetJosh()) {
            return;
        }

        // ENDING CHECK
        if (gm.getCurrentState() == GameManager.GameState.ENDING) {
            return;
        }

        if (graceTimer < playerGracePeriod) {
            graceTimer += delta;
        }

        // BUFFER
        if (inTransitionBuffer) {
            updateTransitionBuffer(delta);
            return;
        }

        // ABSTRACT
        if (!isEnemyInSameRoom()) {
            updateAbstractMode(delta);
        }
        // PHYSICAL
        else {
            updatePhysicalMode(delta);
        }
    }

    private void updateAbstractMode(float delta) {
        enemyMoveTimer += delta;

        if (enemyMoveTimer >= enemyMoveCooldown) {
            enemyMoveTimer = 0f;
            enemyMoveCooldown = randomRange(ENEMY_MOVE_COOLDOWN_MIN, ENEMY_MOVE_COOLDOWN_MAX);

            RoomId roomBeforeMove = enemyRoom;

            // TENSION
            int distance = calculateRoomDistance(enemyRoom, playerRoom);
            if (distance > MAX_ROOM_DISTANCE) {
                enemyRoom = teleportToAdjacentRoom(playerRoom);
                lastEnemyRoom = roomBeforeMove;
                if (debugMode) {
                    System.out.println("[RoomDirector] TENSION: Enemy teleported to adjacent room " + enemyRoom);
                }
            } else {
                lastEnemyRoom = enemyRoom;
                enemyRoom = moveCloser(enemyRoom, playerRoom);

                if (debugMode) {
                    System.out.println("[RoomDirector] Enemy moved: " + lastEnemyRoom + " -> " + enemyRoom);
                }
            }

            if (isEnemyAdjacentToPlayer()) {
                triggerAudioCue();
            }

            if (isEnemyInSameRoom()) {
                enemyReadyToEnterRoom = true;
                doorEntryTriggered = false;
                entryDirection = calculateEntryDirection(lastEnemyRoom);

                if (debugMode) {
                    System.out.println("[RoomDirector] Enemy reached player room from " + lastEnemyRoom + " via "
                            + entryDirection);
                }
            }
        }
    }

    private void updatePhysicalMode(float delta) {
        if (!GameManager.getInstance().isTestingMode() && graceTimer < playerGracePeriod) {
            if (debugMode && !doorEntryTriggered) {
                System.out.println("[RoomDirector] Grace period active: " +
                        String.format("%.1f", (playerGracePeriod - graceTimer)) + "s remaining");
            }
            return;
        }

        if (enemyReadyToEnterRoom && !doorEntryTriggered) {
            triggerDoorEntry();
            doorEntryTriggered = true;
            doorEntryTimer = 0f;
        }

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

    // NAV
    private RoomId moveCloser(RoomId from, RoomId to) {
        if (from == to)
            return from;

        int fromRow = getRow(from);
        int fromCol = getCol(from);
        int toRow = getRow(to);
        int toCol = getCol(to);

        if (fromRow != toRow) {
            if (fromRow < toRow && from.down() != null) {
                return from.down();
            } else if (fromRow > toRow && from.up() != null) {
                return from.up();
            }
        }

        if (fromCol != toCol) {
            if (fromCol < toCol && from.right() != null) {
                return from.right();
            } else if (fromCol > toCol && from.left() != null) {
                return from.left();
            }
        }

        return from;
    }

    private int getRow(RoomId room) {
        if (room == null)
            return 1;
        String name = room.name();

        if (name.endsWith("A") || name.equals("GYM")) {
            return 2;
        }
        if (name.endsWith("B") || name.equals("PARKING")) {
            return 0;
        }
        return 1;
    }

    private int getCol(RoomId room) {
        if (room == null)
            return 1;
        String name = room.name();

        if (name.equals("PARKING") || name.equals("LOBBY") ||
                name.contains("_1") || name.contains("_2") || name.contains("_3")) {
            return 0;
        }
        if (name.equals("HALLWAY") || name.equals("GYM") ||
                name.contains("_4") || name.contains("_5") ||
                name.equals("JANITOR") || name.equals("RESTROOM")) {
            return 1;
        }
        return 2;
    }

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

    // EVENTS
    public void onPlayerEnterRoom(RoomId newRoom) {
        RoomId oldRoom = playerRoom;
        playerRoom = newRoom;
        graceTimer = 0f;

        // CILUKBA
        if (inTransitionBuffer && frozenAtRoom == newRoom) {
            cancelTransitionBuffer();
            enemyRoom = newRoom;
            enemyPhysicallyPresent = true;
            enemyReadyToEnterRoom = false;
            doorEntryTriggered = true;
            entryDirection = calculateEntryDirection(lastEnemyRoom);

            if (debugMode) {
                System.out.println("[RoomDirector] JUMP SCARE: Player returned! Enemy at " + entryDirection
                        + " door (from " + lastEnemyRoom + ")");
            }
            return;
        }

        if (enemyPhysicallyPresent && enemyRoom == oldRoom) {
            lastEnemyRoom = oldRoom;
            enterTransitionBuffer(oldRoom, newRoom);
        }

        if (debugMode) {
            System.out.println("[RoomDirector] Player entered room: " + newRoom +
                    " (grace period: " + playerGracePeriod + "s)");
        }
    }

    public void onEnemyDespawn() {
        enemyPhysicallyPresent = false;
        enemyReadyToEnterRoom = false;
        doorEntryTriggered = false;
        enemyMoveTimer = 0f;

        if (debugMode) {
            System.out.println("[RoomDirector] Enemy despawned");
        }
    }

    private RoomId[] getAdjacentRooms(RoomId room) {
        return new RoomId[] {
                room.up(),
                room.down(),
                room.left(),
                room.right()
        };
    }

    public void forceEnemyRetreat() {
        RoomId[] adjacentRooms = getAdjacentRooms(playerRoom);

        java.util.List<RoomId> validRooms = new java.util.ArrayList<>();
        for (RoomId room : adjacentRooms) {
            if (room != playerRoom) {
                validRooms.add(room);
            }
        }

        if (validRooms.isEmpty()) {
            RoomId[] allRooms = RoomId.values();
            for (RoomId room : allRooms) {
                if (room != playerRoom) {
                    validRooms.add(room);
                }
            }
        }

        if (!validRooms.isEmpty()) {
            int randomIndex = (int) (Math.random() * validRooms.size());
            lastEnemyRoom = enemyRoom;
            enemyRoom = validRooms.get(randomIndex);

            enemyPhysicallyPresent = false;
            enemyReadyToEnterRoom = false;
            doorEntryTriggered = false;
            entryDirection = DoorDirection.NONE;

            enemyMoveTimer = 0f;
            enemyMoveCooldown = randomRange(ENEMY_MOVE_COOLDOWN_MIN * 2f, ENEMY_MOVE_COOLDOWN_MAX * 2f);

            System.out.println("[RoomDirector] Enemy retreated from " + lastEnemyRoom + " to " + enemyRoom
                    + " (player in " + playerRoom + ")");
        }
    }

    private void triggerAudioCue() {
        AudioManager audio = AudioManager.getInstance();
        String audioPath = "Audio/Effect/monster_grunt_sound_effect.wav";
        audio.playSound(audioPath, 0.3f);

        if (debugMode) {
            System.out.println("[RoomDirector] AUDIO CUE: Enemy nearby from direction " + entryDirection);
        }
    }

    private void triggerDoorEntry() {
        AudioManager audio = AudioManager.getInstance();
        audio.playSound("Audio/Effect/footstep_sound_effect.wav", 0.8f);

        if (eventListener != null) {
            eventListener.onCameraShake(0.3f, 5f);
            eventListener.onDoorEntry(entryDirection);
        }

        audio.playSound("Audio/Effect/monster_roar_sound_effect.wav", 0.6f);

        if (debugMode) {
            System.out.println("[RoomDirector] DOOR ENTRY triggered from " + entryDirection);
        }
    }

    private void spawnEnemyAtDoor() {
    }

    // TRANSITION
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

    private void updateTransitionBuffer(float delta) {
        transitionBufferTimer += delta;

        if (transitionBufferTimer >= TRANSITION_BUFFER_DURATION) {
            exitTransitionBuffer();
        }
    }

    private void exitTransitionBuffer() {
        if (!inTransitionBuffer)
            return;

        inTransitionBuffer = false;
        enemyRoom = transitionTargetRoom;

        if (debugMode) {
            System.out.println(
                    "[RoomDirector] TRANSITION COMPLETE: Enemy moved from " + lastEnemyRoom + " to " + enemyRoom);
        }

        if (isEnemyInSameRoom()) {
            enemyReadyToEnterRoom = true;
            entryDirection = calculateEntryDirection(lastEnemyRoom);

            if (debugMode) {
                System.out.println(
                        "[RoomDirector] Enemy entering via " + entryDirection + " door (from " + lastEnemyRoom + ")");
            }
        }

        transitionTargetRoom = null;
        frozenAtRoom = null;
    }

    private void cancelTransitionBuffer() {
        inTransitionBuffer = false;
        transitionBufferTimer = 0f;
        transitionTargetRoom = null;
        frozenAtRoom = null;

        if (debugMode) {
            System.out.println("[RoomDirector] Transition buffer CANCELLED");
        }
    }

    // DISTANCE
    private int calculateRoomDistance(RoomId from, RoomId to) {
        int fromRow = getRow(from);
        int fromCol = getCol(from);
        int toRow = getRow(to);
        int toCol = getCol(to);

        return Math.abs(fromRow - toRow) + Math.abs(fromCol - toCol);
    }

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
            return target;

        int randomIndex = (int) (Math.random() * adjacentRooms.size());
        return adjacentRooms.get(randomIndex);
    }

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

    public float[] getEnemySpawnPosition(float worldWidth, float worldHeight, float enemyWidth, float enemyHeight,
            float playerX, float playerY) {
        float x, y;
        float margin = 20f;

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
                if (GameManager.getInstance().isTestingMode()) {
                    x = playerX + 30f;
                    y = playerY + 30f;
                } else {
                    x = worldWidth / 2f - enemyWidth / 2f;
                    y = worldHeight / 2f - enemyHeight / 2f;
                }
                break;
        }

        return new float[] { x, y };
    }

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

    private float randomRange(float min, float max) {
        return min + (float) (Math.random() * (max - min));
    }
}
