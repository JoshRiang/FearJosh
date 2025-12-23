package com.fearjosh.frontend.core;

import com.fearjosh.frontend.world.RoomId;
import com.fearjosh.frontend.render.TiledMapManager;
import com.badlogic.gdx.utils.Array;

/**
 * JoshSpawnController - Context-aware Josh spawn system for TMX-based maps.
 * 
 * PRINSIP UTAMA:
 * 1. Josh HANYA spawn di ruangan yang SAMA dengan player
 * 2. Tidak ada teleport aneh - spawn harus masuk akal berdasarkan konteks
 * 3. Semua spawn point dari TMX object layer (josh_spawn)
 * 4. Spawn bersifat kondisional (cooldown + chance)
 * 
 * KONTEKS TRACKING:
 * - Melacak di mana Josh terakhir terlihat
 * - Melacak interaksi terakhir player-Josh (chase/caught)
 * - Menggunakan histori untuk menentukan spawn point yang logis
 */
public class JoshSpawnController {
    
    // ==================== INTERACTION TYPE ====================
    
    public enum JoshInteraction {
        NONE,           // Belum pernah interaksi
        CHASED,         // Player dikejar Josh
        CAUGHT,         // Player tertangkap Josh
        RETREATED       // Josh mundur setelah capture
    }
    
    // ==================== SPAWN RESULT ====================
    
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
        BLOCKED,            // Spawn diblokir (cutscene, minigame, dll)
        FROM_CHASE,         // Josh mengejar dari room sebelumnya
        FRESH_ENCOUNTER,    // Encounter baru (player tidak tahu Josh di mana)
        AMBUSH,             // Josh muncul dari sudut tidak terduga
        WAITING             // Cooldown belum selesai
    }
    
    // ==================== STATE TRACKING ====================
    
    private RoomId currentRoomId;           // Ruangan player saat ini
    private RoomId lastRoomId;              // Ruangan player sebelumnya
    private boolean joshPresent;            // Apakah Josh sedang aktif di ruangan ini
    private RoomId lastJoshRoomId;          // Ruangan terakhir Josh terlihat
    private float lastJoshSpawnTime;        // Waktu spawn terakhir (dalam detik sejak start)
    private JoshInteraction lastInteraction; // Interaksi terakhir player-Josh
    private int roomEntryCounter;           // Berapa kali player masuk room sejak spawn terakhir
    
    // ==================== SPAWN CONFIGURATION ====================
    
    /** Cooldown minimum antara spawn (detik) */
    private static final float SPAWN_COOLDOWN_MIN = 15f;
    
    /** Cooldown maksimum antara spawn (detik) */
    private static final float SPAWN_COOLDOWN_MAX = 30f;
    
    /** Delay setelah masuk ruangan sebelum Josh bisa spawn (detik) */
    private static final float ROOM_ENTRY_DELAY = 2.5f;
    
    /** Base chance untuk spawn (0.0 - 1.0) */
    private static final float BASE_SPAWN_CHANCE = 0.35f;
    
    /** Chance bonus per room entry tanpa Josh */
    private static final float CHANCE_BONUS_PER_ENTRY = 0.08f;
    
    /** Maximum spawn chance */
    private static final float MAX_SPAWN_CHANCE = 0.70f;
    
    /** Jarak minimum dari player untuk spawn (pixels) */
    private static final float MIN_SPAWN_DISTANCE = 150f;
    
    // ==================== HALLWAY DANGER CONFIG ====================
    
    /** Extra spawn chance bonus for hallway (makes hallway feel dangerous) */
    private static final float HALLWAY_SPAWN_BONUS = 0.20f;
    
    /** Reduced cooldown modifier for hallway spawns (faster respawn) */
    private static final float HALLWAY_COOLDOWN_MULTIPLIER = 0.7f;
    
    // ==================== TIMERS ====================
    
    private float roomEntryTimer;           // Timer sejak masuk ruangan
    private float globalTimer;              // Timer global sejak start
    private float currentSpawnCooldown;     // Cooldown saat ini
    
    // ==================== DEBUG ====================
    
    private boolean debugMode = false;
    
    // ==================== CONSTRUCTOR ====================
    
    public JoshSpawnController() {
        this.currentRoomId = null;
        this.lastRoomId = null;
        this.joshPresent = false;
        this.lastJoshRoomId = null;
        this.lastJoshSpawnTime = -SPAWN_COOLDOWN_MAX; // Allow immediate spawn
        this.lastInteraction = JoshInteraction.NONE;
        this.roomEntryCounter = 0;
        this.roomEntryTimer = 0f;
        this.globalTimer = 0f;
        this.currentSpawnCooldown = randomRange(SPAWN_COOLDOWN_MIN, SPAWN_COOLDOWN_MAX);
        
        log("Initialized with cooldown: " + currentSpawnCooldown);
    }
    
    // ==================== UPDATE ====================
    
    /**
     * Update timers - panggil setiap frame
     */
    public void update(float delta) {
        globalTimer += delta;
        roomEntryTimer += delta;
    }
    
    // ==================== PLAYER EVENTS ====================
    
    /**
     * Dipanggil ketika player masuk ruangan baru
     */
    public void onPlayerEnterRoom(RoomId newRoom, RoomId previousRoom) {
        lastRoomId = previousRoom;
        currentRoomId = newRoom;
        roomEntryTimer = 0f;
        roomEntryCounter++;
        
        // Jika Josh tidak sedang mengejar dari room sebelumnya, reset presence
        if (!isJoshChasingFromPreviousRoom(previousRoom)) {
            joshPresent = false;
        }
        
        log("Player entered " + newRoom + " from " + previousRoom + 
            " (entry #" + roomEntryCounter + ", joshPresent=" + joshPresent + ")");
    }
    
    /**
     * Dipanggil ketika Josh berhasil di-spawn
     */
    public void onJoshSpawned(RoomId room) {
        joshPresent = true;
        lastJoshRoomId = room;
        lastJoshSpawnTime = globalTimer;
        roomEntryCounter = 0;
        
        // Calculate cooldown with hallway modifier
        float baseCooldown = randomRange(SPAWN_COOLDOWN_MIN, SPAWN_COOLDOWN_MAX);
        if (room == RoomId.HALLWAY) {
            // Hallway has reduced cooldown - Josh comes back faster!
            currentSpawnCooldown = baseCooldown * HALLWAY_COOLDOWN_MULTIPLIER;
            log("HALLWAY: Reduced cooldown to " + String.format("%.1f", currentSpawnCooldown) + "s");
        } else {
            currentSpawnCooldown = baseCooldown;
        }
        
        log("Josh spawned in " + room + ", next cooldown: " + String.format("%.1f", currentSpawnCooldown) + "s");
    }
    
    /**
     * Dipanggil ketika Josh despawn (kabur / player keluar room)
     */
    public void onJoshDespawned() {
        joshPresent = false;
        log("Josh despawned");
    }
    
    /**
     * Dipanggil ketika player dikejar Josh
     */
    public void onPlayerChased() {
        lastInteraction = JoshInteraction.CHASED;
        log("Player is being chased!");
    }
    
    /**
     * Dipanggil ketika player tertangkap Josh
     */
    public void onPlayerCaught() {
        lastInteraction = JoshInteraction.CAUGHT;
        log("Player was caught!");
    }
    
    /**
     * Dipanggil ketika Josh mundur setelah capture
     */
    public void onJoshRetreated() {
        lastInteraction = JoshInteraction.RETREATED;
        joshPresent = false;
        // Tambah cooldown setelah retreat
        lastJoshSpawnTime = globalTimer + SPAWN_COOLDOWN_MIN;
        log("Josh retreated, extra cooldown applied");
    }
    
    // ==================== SPAWN DECISION ====================
    
    /**
     * Tentukan apakah Josh harus spawn dan di mana.
     * INI ADALAH METODE UTAMA yang mengimplementasikan logika konteks.
     * 
     * @param tiledMapManager Untuk mengambil spawn points dari TMX
     * @param playerX Posisi X player
     * @param playerY Posisi Y player
     * @param canSpawn False jika sedang cutscene/minigame/tutorial
     * @return SpawnDecision dengan keputusan spawn
     */
    public SpawnDecision shouldSpawnJosh(TiledMapManager tiledMapManager, 
                                          float playerX, float playerY,
                                          boolean canSpawn) {
        // === BLOCK CONDITIONS ===
        
        // 1. Jika tidak boleh spawn (cutscene, minigame, tutorial)
        if (!canSpawn) {
            return SpawnDecision.noSpawn("Spawn blocked by game state");
        }
        
        // 2. Jika Josh sudah ada di ruangan ini
        if (joshPresent) {
            return SpawnDecision.noSpawn("Josh already present in room");
        }
        
        // 3. Jika belum cukup waktu sejak masuk ruangan
        if (roomEntryTimer < ROOM_ENTRY_DELAY) {
            return SpawnDecision.noSpawn("Room entry delay not passed");
        }
        
        // 4. Jika cooldown spawn belum selesai
        float timeSinceLastSpawn = globalTimer - lastJoshSpawnTime;
        if (timeSinceLastSpawn < currentSpawnCooldown) {
            return SpawnDecision.noSpawn("Spawn cooldown: " + 
                String.format("%.1f", currentSpawnCooldown - timeSinceLastSpawn) + "s remaining");
        }
        
        // === CHANCE CALCULATION ===
        
        float spawnChance = calculateSpawnChance();
        float roll = (float) Math.random();
        
        if (roll > spawnChance) {
            return SpawnDecision.noSpawn("Chance roll failed: " + 
                String.format("%.0f%%", roll * 100) + " > " + 
                String.format("%.0f%%", spawnChance * 100));
        }
        
        // === SPAWN POINT SELECTION ===
        
        return selectSpawnPoint(tiledMapManager, playerX, playerY);
    }
    
    /**
     * Hitung chance spawn berdasarkan konteks
     * HALLWAY gets significant bonus to make it feel dangerous
     */
    private float calculateSpawnChance() {
        float chance = BASE_SPAWN_CHANCE;
        
        // Bonus per room entry tanpa Josh
        chance += roomEntryCounter * CHANCE_BONUS_PER_ENTRY;
        
        // HALLWAY DANGER ZONE: Add significant spawn chance boost
        if (currentRoomId == RoomId.HALLWAY) {
            chance += HALLWAY_SPAWN_BONUS;
            log("HALLWAY DANGER BOOST: +" + String.format("%.0f%%", HALLWAY_SPAWN_BONUS * 100));
        }
        
        // Cap maximum
        return Math.min(chance, MAX_SPAWN_CHANCE);
    }
    
    /**
     * Check if current room is the hallway
     */
    public boolean isInHallway() {
        return currentRoomId == RoomId.HALLWAY;
    }
    
    /**
     * Pilih spawn point berdasarkan konteks
     */
    private SpawnDecision selectSpawnPoint(TiledMapManager tiledMapManager,
                                            float playerX, float playerY) {
        if (tiledMapManager == null || !tiledMapManager.hasCurrentMap()) {
            return SpawnDecision.noSpawn("No TMX map loaded");
        }
        
        // Ambil semua josh spawn points dari TMX
        Array<TiledMapManager.SpawnInfo> spawnPoints = tiledMapManager.getAllJoshSpawnPoints();
        
        if (spawnPoints == null || spawnPoints.size == 0) {
            // Fallback: gunakan single spawn point jika ada
            TiledMapManager.SpawnInfo singleSpawn = tiledMapManager.getJoshSpawnPoint();
            if (singleSpawn != null) {
                spawnPoints = new Array<>();
                spawnPoints.add(singleSpawn);
            } else {
                return SpawnDecision.noSpawn("No josh_spawn points in current map");
            }
        }
        
        // === KONTEKS-BASED SELECTION ===
        
        SpawnContext context;
        TiledMapManager.SpawnInfo selectedSpawn = null;
        
        // KONTEKS 3: Player baru kabur dari Josh
        // Josh harus spawn di titik DEKAT pintu transisi (seolah mengejar)
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
        
        // KONTEKS 1 & 2: Fresh encounter
        // Player tidak tahu Josh di mana - bisa spawn di mana saja yang valid
        if (lastInteraction == JoshInteraction.NONE || 
            lastInteraction == JoshInteraction.RETREATED ||
            lastJoshRoomId == null ||
            lastJoshRoomId != lastRoomId) {
            
            context = SpawnContext.FRESH_ENCOUNTER;
            
            // Filter spawn points yang cukup jauh dari player
            Array<TiledMapManager.SpawnInfo> validSpawns = filterByDistance(spawnPoints, playerX, playerY);
            
            if (validSpawns.size > 0) {
                // Random selection dari spawn points yang valid
                int randomIndex = (int) (Math.random() * validSpawns.size);
                selectedSpawn = validSpawns.get(randomIndex);
                
                return SpawnDecision.spawn(
                    selectedSpawn.x, selectedSpawn.y,
                    context,
                    "Fresh encounter - random valid spawn point"
                );
            }
        }
        
        // FALLBACK: Ambil spawn point mana saja yang cukup jauh
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
        
        // Jika semua spawn terlalu dekat, tetap spawn tapi log warning
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
    
    /**
     * Cari spawn point yang dekat dengan pintu dari room tertentu
     */
    private TiledMapManager.SpawnInfo findSpawnNearDoor(
            Array<TiledMapManager.SpawnInfo> spawnPoints,
            TiledMapManager tiledMapManager,
            float playerX, float playerY,
            RoomId fromRoom) {
        
        // Ambil posisi pintu yang menghubungkan ke fromRoom
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
            // Tidak ada pintu ke fromRoom, return null
            return null;
        }
        
        // Cari spawn point terdekat dengan pintu tersebut
        float doorCenterX = targetDoor.bounds.x + targetDoor.bounds.width / 2f;
        float doorCenterY = targetDoor.bounds.y + targetDoor.bounds.height / 2f;
        
        TiledMapManager.SpawnInfo nearestSpawn = null;
        float nearestDistance = Float.MAX_VALUE;
        
        for (TiledMapManager.SpawnInfo spawn : spawnPoints) {
            float dx = spawn.x - doorCenterX;
            float dy = spawn.y - doorCenterY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            
            // Juga pastikan tidak terlalu dekat dengan player
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
    
    /**
     * Filter spawn points yang cukup jauh dari player
     */
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
    
    // ==================== CONTEXT CHECKS ====================
    
    /**
     * Cek apakah Josh sedang mengejar dari room sebelumnya
     */
    private boolean isJoshChasingFromPreviousRoom(RoomId previousRoom) {
        return joshPresent && 
               lastJoshRoomId == previousRoom &&
               lastInteraction == JoshInteraction.CHASED;
    }
    
    // ==================== GETTERS ====================
    
    public RoomId getCurrentRoomId() { return currentRoomId; }
    public RoomId getLastRoomId() { return lastRoomId; }
    public boolean isJoshPresent() { return joshPresent; }
    public RoomId getLastJoshRoomId() { return lastJoshRoomId; }
    public JoshInteraction getLastInteraction() { return lastInteraction; }
    public int getRoomEntryCounter() { return roomEntryCounter; }
    
    // ==================== DEBUG ====================
    
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
    
    // ==================== UTILITY ====================
    
    private float randomRange(float min, float max) {
        return min + (float) (Math.random() * (max - min));
    }
}
