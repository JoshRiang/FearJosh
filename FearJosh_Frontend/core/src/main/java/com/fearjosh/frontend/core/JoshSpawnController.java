package com.fearjosh.frontend.core;

import com.fearjosh.frontend.world.RoomId;
import com.fearjosh.frontend.render.TiledMapManager;
import com.badlogic.gdx.utils.Array;

public class JoshSpawnController {

    // INTERACTION
    public enum JoshInteraction {
        NONE,
        CHASED,
        CAUGHT,
        RETREATED
    }

    // SPAWN RESULT
    public static class SpawnDecision {
        public boolean shouldSpawn;
        public float spawnX;
        public float spawnY;
        public SpawnContext context;
        public String reason;
        
        public SpawnDecision(boolean shouldSpawn, float x, float y, SpawnContext context, String reason) {
            this.shouldSpawn = shouldSpawn;
            this.spawnX = x;
            this.spawnY = y;
            this.context = context;
            this.reason = reason;
        }
        
        public static SpawnDecision noSpawn(String reason) {
            return new SpawnDecision(false, 0, 0, SpawnContext.BLOCKED, reason);
        }
        
        public static SpawnDecision spawn(float x, float y, SpawnContext context, String reason) {
            return new SpawnDecision(true, x, y, context, reason);
        }
    }

    public enum SpawnContext {
        BLOCKED,
        FROM_CHASE,
        FRESH_ENCOUNTER,
        AMBUSH,
        WAITING
    }

    // STATE
    private RoomId currentRoomId;
    private RoomId lastRoomId;
    private boolean joshPresent;
    private RoomId lastJoshRoomId;
    private float lastJoshSpawnTime;
    private JoshInteraction lastInteraction;
    private int roomEntryCounter;

    // CONFIG
    private static final float SPAWN_COOLDOWN_MIN = 15f;
    private static final float SPAWN_COOLDOWN_MAX = 30f;
    private static final float ROOM_ENTRY_DELAY = 2.5f;
    private static final float BASE_SPAWN_CHANCE = 0.35f;
    private static final float CHANCE_BONUS_PER_ENTRY = 0.08f;
    private static final float MAX_SPAWN_CHANCE = 0.70f;
    private static final float MIN_SPAWN_DISTANCE = 150f;

    // HALLWAY
    private static final float HALLWAY_SPAWN_BONUS = 0.20f;
    private static final float HALLWAY_COOLDOWN_MULTIPLIER = 0.7f;

    // TIMERS
    private float roomEntryTimer;
    private float globalTimer;
    private float currentSpawnCooldown;
    private boolean debugMode = false;

    // INIT
    public JoshSpawnController() {
        this.currentRoomId = null;
        this.lastRoomId = null;
        this.joshPresent = false;
        this.lastJoshRoomId = null;
        this.lastJoshSpawnTime = -SPAWN_COOLDOWN_MAX;
        this.lastInteraction = JoshInteraction.NONE;
        this.roomEntryCounter = 0;
        this.roomEntryTimer = 0f;
        this.globalTimer = 0f;
        this.currentSpawnCooldown = randomRange(SPAWN_COOLDOWN_MIN, SPAWN_COOLDOWN_MAX);
        
        log("Initialized with cooldown: " + currentSpawnCooldown);
    }

    // UPDATE
    public void update(float delta) {
        globalTimer += delta;
        roomEntryTimer += delta;
    }

    // EVENTS
    public void onPlayerEnterRoom(RoomId newRoom, RoomId previousRoom) {
        lastRoomId = previousRoom;
        currentRoomId = newRoom;
        roomEntryTimer = 0f;
        roomEntryCounter++;

        if (!isJoshChasingFromPreviousRoom(previousRoom)) {
            joshPresent = false;
        }
        
        log("Player entered " + newRoom + " from " + previousRoom + 
            " (entry #" + roomEntryCounter + ", joshPresent=" + joshPresent + ")");
    }

    public void onJoshSpawned(RoomId room) {
        joshPresent = true;
        lastJoshRoomId = room;
        lastJoshSpawnTime = globalTimer;
        roomEntryCounter = 0;

        float baseCooldown = randomRange(SPAWN_COOLDOWN_MIN, SPAWN_COOLDOWN_MAX);
        if (room == RoomId.HALLWAY) {
            currentSpawnCooldown = baseCooldown * HALLWAY_COOLDOWN_MULTIPLIER;
            log("HALLWAY: Reduced cooldown to " + String.format("%.1f", currentSpawnCooldown) + "s");
        } else {
            currentSpawnCooldown = baseCooldown;
        }
        
        log("Josh spawned in " + room + ", next cooldown: " + String.format("%.1f", currentSpawnCooldown) + "s");
    }

    public void onJoshDespawned() {
        joshPresent = false;
        log("Josh despawned");
    }

    public void onPlayerChased() {
        lastInteraction = JoshInteraction.CHASED;
        log("Player is being chased!");
    }

    public void onPlayerCaught() {
        lastInteraction = JoshInteraction.CAUGHT;
        log("Player was caught!");
    }

    public void onJoshRetreated() {
        lastInteraction = JoshInteraction.RETREATED;
        joshPresent = false;
        lastJoshSpawnTime = globalTimer + SPAWN_COOLDOWN_MIN;
        log("Josh retreated, extra cooldown applied");
    }

    // SPAWN
    public SpawnDecision shouldSpawnJosh(TiledMapManager tiledMapManager,
                                          float playerX, float playerY,
                                          boolean canSpawn) {
        // BLOCK
        if (!canSpawn) {
            return SpawnDecision.noSpawn("Spawn blocked by game state");
        }

        if (joshPresent) {
            return SpawnDecision.noSpawn("Josh already present in room");
        }

        if (roomEntryTimer < ROOM_ENTRY_DELAY) {
            return SpawnDecision.noSpawn("Room entry delay not passed");
        }

        float timeSinceLastSpawn = globalTimer - lastJoshSpawnTime;
        if (timeSinceLastSpawn < currentSpawnCooldown) {
            return SpawnDecision.noSpawn("Spawn cooldown: " + 
                String.format("%.1f", currentSpawnCooldown - timeSinceLastSpawn) + "s remaining");
        }

        // CHANCE
        float spawnChance = calculateSpawnChance();
        float roll = (float) Math.random();
        
        if (roll > spawnChance) {
            return SpawnDecision.noSpawn("Chance roll failed: " + 
                String.format("%.0f%%", roll * 100) + " > " + 
                String.format("%.0f%%", spawnChance * 100));
        }

        return selectSpawnPoint(tiledMapManager, playerX, playerY);
    }

    private float calculateSpawnChance() {
        float chance = BASE_SPAWN_CHANCE;
        chance += roomEntryCounter * CHANCE_BONUS_PER_ENTRY;

        if (currentRoomId == RoomId.HALLWAY) {
            chance += HALLWAY_SPAWN_BONUS;
            log("HALLWAY DANGER BOOST: +" + String.format("%.0f%%", HALLWAY_SPAWN_BONUS * 100));
        }

        return Math.min(chance, MAX_SPAWN_CHANCE);
    }

    public boolean isInHallway() {
        return currentRoomId == RoomId.HALLWAY;
    }

    private SpawnDecision selectSpawnPoint(TiledMapManager tiledMapManager,
                                            float playerX, float playerY) {
        if (tiledMapManager == null || !tiledMapManager.hasCurrentMap()) {
            return SpawnDecision.noSpawn("No TMX map loaded");
        }

        Array<TiledMapManager.SpawnInfo> spawnPoints = tiledMapManager.getAllJoshSpawnPoints();
        
        if (spawnPoints == null || spawnPoints.size == 0) {
            TiledMapManager.SpawnInfo singleSpawn = tiledMapManager.getJoshSpawnPoint();
            if (singleSpawn != null) {
                spawnPoints = new Array<>();
                spawnPoints.add(singleSpawn);
            } else {
                return SpawnDecision.noSpawn("No josh_spawn points in current map");
            }
        }

        // CONTEXT
        SpawnContext context;
        TiledMapManager.SpawnInfo selectedSpawn = null;

        // CHASE
        if (lastInteraction == JoshInteraction.CHASED &&
            lastJoshRoomId == lastRoomId) {
            
            context = SpawnContext.FROM_CHASE;
            selectedSpawn = findSpawnNearDoor(spawnPoints, tiledMapManager, playerX, playerY, lastRoomId);
            
            if (selectedSpawn != null) {
                return SpawnDecision.spawn(
                    selectedSpawn.x, selectedSpawn.y,
                    context,
                    "Josh chasing from " + lastRoomId + " - spawn near entry door"
                );
            }
        }

        // FRESH
        if (lastInteraction == JoshInteraction.NONE ||
            lastInteraction == JoshInteraction.RETREATED ||
            lastJoshRoomId == null ||
            lastJoshRoomId != lastRoomId) {

            context = SpawnContext.FRESH_ENCOUNTER;

            Array<TiledMapManager.SpawnInfo> validSpawns = filterByDistance(spawnPoints, playerX, playerY);
            
            if (validSpawns.size > 0) {
                int randomIndex = (int) (Math.random() * validSpawns.size);
                selectedSpawn = validSpawns.get(randomIndex);
                
                return SpawnDecision.spawn(
                    selectedSpawn.x, selectedSpawn.y,
                    context,
                    "Fresh encounter - random valid spawn point"
                );
            }
        }

        // FALLBACK
        Array<TiledMapManager.SpawnInfo> validSpawns = filterByDistance(spawnPoints, playerX, playerY);
        if (validSpawns.size > 0) {
            int randomIndex = (int) (Math.random() * validSpawns.size);
            selectedSpawn = validSpawns.get(randomIndex);
            
            return SpawnDecision.spawn(
                selectedSpawn.x, selectedSpawn.y,
                SpawnContext.AMBUSH,
                "Fallback spawn point selection"
            );
        }

        if (spawnPoints.size > 0) {
            selectedSpawn = spawnPoints.get(0);
            log("WARNING: All spawn points too close to player, using first available");
            
            return SpawnDecision.spawn(
                selectedSpawn.x, selectedSpawn.y,
                SpawnContext.AMBUSH,
                "No ideal spawn point, using first available"
            );
        }
        
        return SpawnDecision.noSpawn("No valid spawn points found");
    }

    private TiledMapManager.SpawnInfo findSpawnNearDoor(
            Array<TiledMapManager.SpawnInfo> spawnPoints,
            TiledMapManager tiledMapManager,
            float playerX, float playerY,
            RoomId fromRoom) {

        Array<TiledMapManager.DoorInfo> doors = tiledMapManager.getDoors();
        TiledMapManager.DoorInfo targetDoor = null;

        for (TiledMapManager.DoorInfo door : doors) {
            if (door.targetRoom != null &&
                door.targetRoom.equalsIgnoreCase(fromRoom.name())) {
                targetDoor = door;
                break;
            }
        }

        if (targetDoor == null) {
            return null;
        }

        float doorCenterX = targetDoor.bounds.x + targetDoor.bounds.width / 2f;
        float doorCenterY = targetDoor.bounds.y + targetDoor.bounds.height / 2f;

        TiledMapManager.SpawnInfo nearestSpawn = null;
        float nearestDistance = Float.MAX_VALUE;
        
        for (TiledMapManager.SpawnInfo spawn : spawnPoints) {
            float dx = spawn.x - doorCenterX;
            float dy = spawn.y - doorCenterY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            float pDx = spawn.x - playerX;
            float pDy = spawn.y - playerY;
            float playerDistance = (float) Math.sqrt(pDx * pDx + pDy * pDy);
            
            if (distance < nearestDistance && playerDistance >= MIN_SPAWN_DISTANCE * 0.5f) {
                nearestDistance = distance;
                nearestSpawn = spawn;
            }
        }
        
        return nearestSpawn;
    }

    private Array<TiledMapManager.SpawnInfo> filterByDistance(
            Array<TiledMapManager.SpawnInfo> spawnPoints,
            float playerX, float playerY) {
        
        Array<TiledMapManager.SpawnInfo> filtered = new Array<>();
        
        for (TiledMapManager.SpawnInfo spawn : spawnPoints) {
            float dx = spawn.x - playerX;
            float dy = spawn.y - playerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            
            if (distance >= MIN_SPAWN_DISTANCE) {
                filtered.add(spawn);
            }
        }
        
        return filtered;
    }

    private boolean isJoshChasingFromPreviousRoom(RoomId previousRoom) {
        return joshPresent && 
               lastJoshRoomId == previousRoom &&
               lastInteraction == JoshInteraction.CHASED;
    }

    public RoomId getCurrentRoomId() { return currentRoomId; }
    public RoomId getLastRoomId() { return lastRoomId; }
    public boolean isJoshPresent() { return joshPresent; }
    public RoomId getLastJoshRoomId() { return lastJoshRoomId; }
    public JoshInteraction getLastInteraction() { return lastInteraction; }
    public int getRoomEntryCounter() { return roomEntryCounter; }

    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }
    
    private void log(String message) {
        if (debugMode) {
            System.out.println("[JoshSpawnController] " + message);
        }
    }
    
    public String getDebugInfo() {
        return String.format(
            "[JoshSpawn] Room: %s (from %s) | Josh: %s (in %s) | Interaction: %s | Entries: %d | Timer: %.1f/%.1f",
            currentRoomId, lastRoomId,
            joshPresent ? "PRESENT" : "absent",
            lastJoshRoomId,
            lastInteraction,
            roomEntryCounter,
            globalTimer - lastJoshSpawnTime,
            currentSpawnCooldown
        );
    }
    
    // UTILITY
    
    private float randomRange(float min, float max) {
        return min + (float) (Math.random() * (max - min));
    }
}
