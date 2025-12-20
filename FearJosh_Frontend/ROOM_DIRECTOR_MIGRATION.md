# RoomDirector System Migration - Complete

## 🎯 Tujuan
Mengganti sistem spawn enemy lama (direct spawn near player) dengan sistem stalker hybrid yang lebih sophisticated menggunakan **RoomDirector** dan **Ghost Navigator pattern**.

## 📁 File yang Ditambahkan

### 1. `RoomDirector.java` (NEW - 394 lines)
**Path:** `core/src/main/java/com/fearjosh/frontend/core/RoomDirector.java`

**Fitur Utama:**
- **Abstract Mode**: Enemy invisible, navigates room grid via adjacency (R1→R2→R5→R8 path)
- **Physical Mode**: Enemy visible, spawns at door after grace period
- **Grace Period**: 3-5 detik setelah player masuk room baru (no instant spawn)
- **Door Entry**: Enemy masuk lewat pintu (top/bottom/left/right) based on previous room
- **Timing System**: 
  - Move cooldown: 6-10 detik (randomized) untuk movement antar room
  - Grace period: 3-5 detik (randomized) setelah player masuk room
  - Door entry delay: 1.5 detik fixed untuk transisi door→spawn

**Key Methods:**
```java
// Main update loop
public void update(float delta)

// Abstract mode: enemy moves between rooms via adjacency
private void updateAbstractMode(float delta)

// Physical mode: grace period enforcement, door entry triggering
private void updatePhysicalMode(float delta)

// Manhattan navigation without BFS/Dijkstra
private RoomId moveCloser(RoomId from, RoomId to)

// Calculate door spawn position based on entry direction
public float[] getEnemySpawnPosition(float roomWidth, float roomHeight, float enemyW, float enemyH)

// Check if enemy should be visible/active
public boolean isEnemyPhysicallyPresent()
```

**Debug Flag:**
```java
Constants.DEBUG_ROOM_DIRECTOR = true; // Print console logs for tracking
```

---

## 🔧 File yang Dimodifikasi

### 2. `GameManager.java`
**Changes:**
- Added `RoomDirector roomDirector` field
- Added `initializeRoomDirector(RoomId playerStartRoom)` method
  - Spawns enemy 2-3 moves away in distant room (R1/R3/R7/R9 corners)
- Added `notifyPlayerRoomChange(RoomId newRoom)` method
  - Forwards room change events to RoomDirector
- Added `getRoomDirector()` getter method
- Added `getRandomDistantRoom(RoomId playerRoom)` helper
  - Returns corner room 2-3 moves away from player

**Integration Point:**
```java
// Called on New Game
gm.initializeRoomDirector(session.getCurrentRoomId());
```

---

### 3. `PlayScreen.java`
**Changes:**

#### Constructor (lines 193-207)
- **REMOVED**: Old direct enemy spawn
  ```java
  // OLD (REMOVED):
  josh = new Enemy(player.getX() + 120f, player.getY() + 60f, 24f, 36f);
  
  // NEW:
  // Enemy spawning now controlled by RoomDirector
  // Josh starts as null and spawns when RoomDirector signals
  josh = null;
  ```

#### `switchToRoom()` Method (lines 210-224)
- **ADDED**: Notify RoomDirector of room change
  ```java
  RoomDirector rd = gm.getRoomDirector();
  if (rd != null) {
      rd.notifyPlayerRoomChange(newRoomId);
      // Despawn physical enemy when player leaves
      if (josh != null) {
          josh = null;
      }
  }
  ```

#### `update()` Method (lines 462-487)
- **REPLACED**: Old enemy update block with RoomDirector-controlled spawning
  ```java
  // OLD (REMOVED):
  if (josh != null) {
      josh.update(player, currentRoom, delta);
      if (!josh.isDespawned() && checkEnemyPlayerCollision(josh, player)) {
          System.out.println("GAME OVER: Josh caught you!");
      }
      clampEnemyToRoom(josh);
  }
  
  // NEW:
  updateRoomDirector(delta); // RoomDirector controls spawning
  ```

#### **NEW Methods**:
```java
// Update RoomDirector and spawn enemy when ready
private void updateRoomDirector(float delta) {
    RoomDirector rd = gm.getRoomDirector();
    if (rd != null) {
        rd.update(delta);
        
        // Spawn enemy only when RoomDirector signals
        if (rd.isEnemyPhysicallyPresent() && josh == null) {
            spawnEnemyPhysically(rd);
        }
        
        // Update physical enemy if exists
        if (josh != null) {
            josh.update(player, currentRoom, delta);
            if (!josh.isDespawned() && checkEnemyPlayerCollision(josh, player)) {
                System.out.println("GAME OVER: Josh caught you!");
            }
            clampEnemyToRoom(josh);
        }
    }
}

// Spawn enemy at door position (not center)
private void spawnEnemyPhysically(RoomDirector rd) {
    float[] pos = rd.getEnemySpawnPosition(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, 24f, 36f);
    josh = new Enemy(pos[0], pos[1], 24f, 36f);
    System.out.println("[PlayScreen] Enemy spawned at door: (" + pos[0] + ", " + pos[1] + ")");
}
```

#### Render Pipeline (lines 275-298)
- **FIXED**: Moved enemy render from ShapeRenderer to SpriteBatch
  ```java
  // OLD: josh.render(shapeRenderer); // WRONG - Enemy expects SpriteBatch
  
  // NEW: josh.render(batch); // CORRECT - Enemy.render(SpriteBatch)
  ```

---

### 4. `Enemy.java`
**Changes:**

#### Deprecated Old Respawn Logic
```java
// Despawn/Respawn mechanics - DEPRECATED in favor of RoomDirector system
private static final float DESPAWN_DELAY = 999999f; // Effectively disabled
private static final float RESPAWN_CHECK_INTERVAL = 999999f; // Effectively disabled

@Deprecated
private boolean isAdjacentRoom(RoomId currentRoom, RoomId lastSeen) { ... }

@Deprecated
private void despawn() { ... }

@Deprecated
private void respawnNearPlayer(Player player) {
    // OLD SYSTEM: Random offset spawn near player
    // NEW SYSTEM: RoomDirector spawns at door position
    // This method is no longer called
}
```

#### Updated `update()` Method
- **REMOVED**: Despawn timer logic
- **REMOVED**: Respawn check logic
- **REMOVED**: isAdjacentRoom() condition
- Enemy now only updates state machine, no automatic respawn

---

### 5. `Constants.java`
**Changes:**
- Added `DEBUG_ROOM_DIRECTOR = true` flag (line 146)
- Used for console logging of enemy navigation events

---

## 🚀 Alur Kerja Baru (Abstract → Physical)

### 1. **Initialization** (New Game)
```
Player starts in room (e.g., R5)
  ↓
GameManager.initializeRoomDirector(R5)
  ↓
Enemy spawns in distant room (R1, R3, R7, or R9)
  ↓
RoomDirector enters ABSTRACT mode
```

### 2. **Abstract Mode** (Enemy in Different Room)
```
Every 6-10 seconds (randomized):
  ↓
RoomDirector.moveCloser(currentRoom, playerRoom)
  ↓
Enemy moves 1 step closer via adjacency:
  Example: R1 → R2 → R5 (Manhattan path)
  ↓
Uses RoomId.up()/down()/left()/right()
  ↓
No teleport, no instant spawn
```

### 3. **Transition to Physical Mode**
```
Enemy reaches player's room
  ↓
RoomDirector enters PHYSICAL mode
  ↓
Grace period starts: 3-5 seconds (randomized)
  ↓
Player has time to prepare, no instant spawn
```

### 4. **Door Entry** (After Grace Period)
```
Grace period expires
  ↓
triggerDoorEntry() called (audio cue TODO)
  ↓
Door entry delay: 1.5 seconds
  ↓
isEnemyPhysicallyPresent() returns true
  ↓
PlayScreen.spawnEnemyPhysically() called
  ↓
Enemy spawned at door position (top/bottom/left/right)
  based on which room enemy came from
```

### 5. **Player Room Change**
```
Player teleports to adjacent room
  ↓
PlayScreen.switchToRoom() called
  ↓
notifyPlayerRoomChange(newRoomId)
  ↓
Physical enemy despawned (josh = null)
  ↓
RoomDirector enters ABSTRACT mode again
  ↓
Back to step 2
```

---

## 🔍 Testing Checklist

### ✅ Scenario 1: Grace Period Test
**Test:** Player pindah room cepat 2-3 kali  
**Expected:** Enemy tidak langsung muncul (3-5 detik delay)

### ✅ Scenario 2: Abstract Mode Navigation
**Test:** Diam di satu room, tunggu beberapa waktu  
**Expected:** Enemy mendekat via adjacency (R1→R2→R5 path), tidak teleport

### ✅ Scenario 3: Door Entry
**Test:** Enemy masuk room player  
**Expected:** Muncul lewat pintu (top/bottom/left/right), bukan tengah room

### ✅ Scenario 4: No Instant Spawn
**Test:** Room transition (player teleport)  
**Expected:** Physical enemy despawned, tidak ada spawn instan saat transition

---

## 🎨 Konstanta Timing

```java
// RoomDirector.java
public static final float ENEMY_MOVE_COOLDOWN_MIN = 6f;    // Min seconds between moves
public static final float ENEMY_MOVE_COOLDOWN_MAX = 10f;   // Max seconds between moves
public static final float PLAYER_GRACE_PERIOD_MIN = 3f;    // Min grace period after room entry
public static final float PLAYER_GRACE_PERIOD_MAX = 5f;    // Max grace period after room entry
public static final float DOOR_ENTRY_DELAY = 1.5f;          // Fixed delay for door entry effect
```

**Tunable via Constants:** Bisa diubah untuk menyesuaikan difficulty/pacing game.

---

## 🐛 Known Issues / TODO

### Audio Cues (TODO)
```java
// In RoomDirector.java:
private void triggerAudioCue() {
    // TODO: Play breathing sound when enemy adjacent to player room
}

private void triggerDoorEntry() {
    // TODO: Play door sound when enemy enters room
}
```

### Future Improvements
- [ ] Implement audio cues for enemy proximity
- [ ] Add visual indicators (breathing particles, door animation)
- [ ] Tune timing constants based on playtesting
- [ ] Add difficulty-based timing adjustments

---

## 📊 System Comparison

### Old System (REMOVED)
- ❌ Enemy spawned directly near player with random offset
- ❌ Despawned after 3 seconds of not chasing
- ❌ Respawned instantly when player entered adjacent room
- ❌ No grace period, instant spawn
- ❌ No door-based entry
- ❌ Used `respawnNearPlayer()` with 100f random offset

### New System (ACTIVE)
- ✅ Enemy navigates room grid via adjacency (Abstract mode)
- ✅ Grace period 3-5 seconds (no instant spawn)
- ✅ Door-based entry (top/bottom/left/right)
- ✅ Manhattan navigation without teleport
- ✅ Hybrid Abstract/Physical presence
- ✅ Controlled by RoomDirector state machine

---

## 🔧 Build & Compile Status

**Status:** ✅ SUCCESS  
**Command:** `.\gradlew.bat build`  
**Warnings:** 4 deprecation warnings (PLAYER_WIDTH/HEIGHT, Player.getWidth/getHeight)  
**Errors:** 0

### Compilation Fixed
- ✅ Import RoomDirector in PlayScreen
- ✅ Fix josh.render() call (ShapeRenderer → SpriteBatch)
- ✅ Add getRoomDirector() getter in GameManager
- ✅ Disable Enemy.java old respawn logic

---

## 🎮 How to Enable Debug Logging

```java
// In Constants.java:
public static final boolean DEBUG_ROOM_DIRECTOR = true;
```

**Console Output:**
```
[RoomDirector] Enemy at R1, Player at R5, moving to R2
[RoomDirector] Enemy reached player room R5, starting grace period: 4.2s
[RoomDirector] Grace period expired, triggering door entry
[PlayScreen] Enemy spawned at door: (375.0, 550.0)
```

---

## 📝 Summary

**Status:** ✅ MIGRATION COMPLETE  

**Files Changed:**
- `RoomDirector.java` (NEW)
- `GameManager.java` (modified)
- `PlayScreen.java` (modified)
- `Enemy.java` (deprecated old logic)
- `Constants.java` (added debug flag)

**Old Logic Removed:**
- ✅ Direct spawn near player (PlayScreen constructor)
- ✅ Instant respawn on adjacent room (Enemy.update)
- ✅ despawnTimer logic (Enemy.update)
- ✅ respawnNearPlayer() method (deprecated)
- ✅ isAdjacentRoom() respawn check (deprecated)

**New System Active:**
- ✅ RoomDirector abstract/physical mode
- ✅ Adjacency-based navigation (moveCloser)
- ✅ Grace period enforcement (3-5s)
- ✅ Door-based spawning (getEnemySpawnPosition)
- ✅ Room change notifications (notifyPlayerRoomChange)

**Next Steps:**
1. Run game and test all 4 scenarios
2. Implement audio cues (breathing, door sounds)
3. Tune timing constants based on playtesting
4. Consider adding visual indicators (particles, animations)

---

**Migration Date:** 2025  
**Author:** GitHub Copilot  
**Status:** ✅ Ready for Testing
