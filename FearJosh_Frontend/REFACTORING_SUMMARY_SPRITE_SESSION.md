# FearJosh Refactoring: Player Sprite & Session Management

## ✅ PART A: Player Sprite Rendering & Hitbox Fix

### Problem Identified
**Root Cause**: Player sprite was being **squashed** because rendering used fixed 64x64 dimensions regardless of actual sprite aspect ratio.
- **Location**: [PlayScreen.java:289-294](../core/src/main/java/com/fearjosh/frontend/screen/PlayScreen.java#L289-L294)
- **Issue**: `batch.draw(frame, x, y, player.getWidth(), player.getHeight())` forced all sprites (375x800 up/down, ~450x? left/right) into 64x64 square
- **Player Init**: [GameManager.java:59](../core/src/main/java/com/fearjosh/frontend/core/GameManager.java#L59) hardcoded 64x64

### Solution Implemented

#### 1. **Separated Render Size from Collision Hitboxes**
Added tunable constants in [Constants.java](../core/src/main/java/com/fearjosh/frontend/config/Constants.java):

```java
// VISUAL SIZE (adjust for appearance - does NOT affect gameplay)
PLAYER_RENDER_WIDTH = 48f   // Narrower than old 64f
PLAYER_RENDER_HEIGHT = 80f  // Taller for better proportions

// FOOT COLLISION (affects furniture collision only)
PLAYER_COLLISION_WIDTH = 32f   // 50% of render width
PLAYER_COLLISION_HEIGHT = 20f  // 25% of render height
PLAYER_COLLISION_OFFSET_Y = 0f // At bottom

// ENEMY COLLISION (affects enemy damage)
PLAYER_ENEMY_HITBOX_WIDTH = 48f   // Match render width
PLAYER_ENEMY_HITBOX_HEIGHT = 80f  // Match render height
```

#### 2. **Updated Player.java Architecture**
- Added `renderWidth` and `renderHeight` fields separate from collision bounds
- `getWidth()` / `getHeight()` now return render size (deprecated but kept for compatibility)
- Added `getRenderWidth()` / `getRenderHeight()` (new recommended methods)
- Updated `updateHitboxes()` to use Constants instead of ratios

#### 3. **Fixed All Rendering Calls**
- **PlayScreen**: Uses `getRenderWidth/Height()` for sprite draw
- **RoomTransitionSystem**: Uses render size for wall collision
- **GameManager**: Uses Constants for player initialization

### How to Adjust Player Appearance

**TO MAKE PLAYER THINNER/WIDER:**
Edit [Constants.java:47](../core/src/main/java/com/fearjosh/frontend/config/Constants.java#L47):
```java
public static final float PLAYER_RENDER_WIDTH = 40f;  // Change this!
```

**TO MAKE PLAYER TALLER/SHORTER:**
Edit [Constants.java:50](../core/src/main/java/com/fearjosh/frontend/config/Constants.java#L50):
```java
public static final float PLAYER_RENDER_HEIGHT = 90f;  // Change this!
```

**TO ADJUST COLLISION (furniture):**
Edit [Constants.java:53-56](../core/src/main/java/com/fearjosh/frontend/config/Constants.java#L53-L56):
```java
public static final float PLAYER_COLLISION_WIDTH = 32f;   // Foot hitbox width
public static final float PLAYER_COLLISION_HEIGHT = 20f;  // Foot hitbox height
```

**TO ADJUST ENEMY HITBOX:**
Edit [Constants.java:61-64](../core/src/main/java/com/fearjosh/frontend/config/Constants.java#L61-L64):
```java
public static final float PLAYER_ENEMY_HITBOX_WIDTH = 48f;  // Body collision
public static final float PLAYER_ENEMY_HITBOX_HEIGHT = 80f; // Body collision
```

---

## ✅ PART B: Resume/New Game Flow & Difficulty Lock

### Problem Identified
**Flow was BROKEN:**
1. "Resume" from Main Menu actually called `resetNewGame()` → **WRONG**, should continue existing session
2. Pause → Main Menu → Resume → **Lost progress** (session not saved)
3. Difficulty could be changed mid-run without warning → **Inconsistent state**

### Solution: GameSession Architecture

#### 1. **Created GameSession.java** ([view file](../core/src/main/java/com/fearjosh/frontend/core/GameSession.java))
Represents ONE playthrough with all progress:
```java
class GameSession {
    - sessionId (unique)
    - currentRoomId
    - playerX, playerY
    - stamina, battery, batterySegments
    - difficulty (LOCKED once session starts)
    - active (boolean)
    
    + updateFromPlayer()  // Save progress
    + restoreToPlayer()   // Load progress
}
```

#### 2. **Updated GameManager** ([view file](../core/src/main/java/com/fearjosh/frontend/core/GameManager.java))
New session-aware methods:
```java
+ startNewGame()           // Creates FRESH session (resets progress)
+ resumeSession()          // Restores EXISTING session (NO reset)
+ saveProgressToSession()  // Save current state (call on pause/room change)
+ hasActiveSession()       // Check if Resume should be enabled
+ canChangeDifficultyFreely()          // false if active run
+ difficultyChangeRequiresNewGame()    // true if active run
+ changeDifficultyAndStartNewGame()    // Force reset with new difficulty
```

#### 3. **Fixed Menu Flow** ([MainMenuScreen.java](../core/src/main/java/com/fearjosh/frontend/screen/MainMenuScreen.java))
**BEFORE (BROKEN):**
```java
// New Game button
onClick: resetNewGame() → creates player

// Resume button  
onClick: (just switches screen) → USES SAME PLAYER (wrong!)
```

**AFTER (CORRECT):**
```java
// New Game button
onClick: startNewGame() → creates FRESH session + resets player

// Resume button
onClick: resumeSession() → restores session state WITHOUT reset
```

#### 4. **Fixed Pause Menu** ([PlayScreen.java:415-421](../core/src/main/java/com/fearjosh/frontend/screen/PlayScreen.java#L415-L421))
**CRITICAL FIX:**
```java
// Back to Main Menu button
onClick: {
    saveProgressToSession()  // ← NEW: Save before leaving!
    setCurrentState(MAIN_MENU)
    setScreen(MainMenuScreen)
}
```
Now session remains active when returning to menu → **Resume works correctly**

#### 5. **Difficulty Lock with Confirmation Popup** ([SettingsScreen.java](../core/src/main/java/com/fearjosh/frontend/screen/SettingsScreen.java))

**Logic:**
```
User clicks difficulty button
    ↓
IF no active session:
    → Change immediately (no popup)
    
IF active session exists:
    → Show MODAL POPUP:
        Title: "Warning"
        Message: "Changing difficulty will start NEW GAME 
                 and delete current progress. Continue?"
        Buttons:
            [Cancel] → Dismiss popup, revert selection
            [New Game] → Change difficulty + startNewGame()
```

**UI Design:**
- Modal overlay (dark semi-transparent background)
- Apple-style rounded card dialog
- Orange-red warning title
- Two buttons: Cancel (safe) / New Game (destructive)
- Input blocked to popup only (proper modal behavior)

---

## 📊 Flow Diagrams

### New Game Flow
```
Main Menu: Click "New Game"
    ↓
GameManager.startNewGame()
    ↓
Creates new GameSession(difficulty, startRoom, playerPos)
    ↓
Resets player state
    ↓
setCurrentState(PLAYING)
    ↓
PlayScreen (fresh run)
```

### Resume Flow
```
Main Menu: Click "Resume" (only if hasActiveSession())
    ↓
GameManager.resumeSession()
    ↓
session.restoreToPlayer(player)  ← Restore position, stamina, etc.
    ↓
currentRoomId = session.getCurrentRoomId()
    ↓
setCurrentState(PLAYING)
    ↓
PlayScreen (continues where left off)
```

### Pause → Main Menu → Resume Flow
```
Playing → Pause → Click "Main Menu"
    ↓
saveProgressToSession()  ← CRITICAL: Save before leaving
    ↓
setCurrentState(MAIN_MENU)
    ↓
MainMenuScreen (shows "Resume" button)
    ↓
Click "Resume"
    ↓
resumeSession() → Loads saved state
    ↓
PlayScreen (same run, not reset!)
```

### Difficulty Change with Active Run
```
Settings: Click different difficulty
    ↓
IF hasActiveSession():
    ↓
    Show POPUP:
    "Changing difficulty will start NEW GAME
     and delete current progress. Continue?"
    ↓
    User clicks [Cancel]:
        → Dismiss popup
        → Revert to current difficulty
    ↓
    User clicks [New Game]:
        → changeDifficultyAndStartNewGame()
        → Creates fresh session with new difficulty
        → Enters PlayScreen (progress reset)
```

---

## 🎮 Testing Checklist

### Part A: Sprite Rendering
- [ ] **Build & Run**: Game compiles without errors
- [ ] **Visual Check**: Player sprite NOT squashed (aspect ratio correct)
- [ ] **Adjust Width**: Change `PLAYER_RENDER_WIDTH` to 40f → Player thinner
- [ ] **Adjust Height**: Change `PLAYER_RENDER_HEIGHT` to 90f → Player taller
- [ ] **Hitbox Test**: Walk near furniture → foot hitbox collision works
- [ ] **Enemy Test**: Enemy touches player → body hitbox collision works
- [ ] **4 Directions**: Walk up/down/left/right → all animations render correctly

### Part B: Session Management
- [ ] **New Game**: Click "New Game" → Fresh run starts
- [ ] **Pause → Resume**: Pause → Resume → Game continues (no reset)
- [ ] **Pause → Menu → Resume**:
  1. New Game → Play for a bit (move around, drain stamina)
  2. Pause → Main Menu
  3. Click "Resume"
  4. **VERIFY**: Player is at SAME position, SAME stamina (not reset!)
- [ ] **Difficulty Lock**:
  1. New Game → Play for a bit
  2. Pause → Settings → Click different difficulty
  3. **VERIFY**: Popup appears with warning
  4. Click "Cancel" → **VERIFY**: Difficulty unchanged
  5. Click different difficulty again → Click "New Game"
  6. **VERIFY**: New run starts, difficulty changed
- [ ] **No Active Run**:
  1. Quit to menu (before New Game)
  2. Settings → Click difficulty
  3. **VERIFY**: Changes immediately (no popup)

---

## 📝 Why Old Flow Was Wrong

**Problem**: Resume Button Confusion
```java
// OLD CODE (MainMenuScreen)
resumeBtn.onClick() {
    gm.setCurrentState(PLAYING);
    game.setScreen(new PlayScreen(game));  // ← No restore!
}

// PlayScreen constructor
public PlayScreen(FearJosh game) {
    player = gm.getPlayer();  // ← Uses existing player
    // But player was NEVER restored from saved state!
    // Position/stamina/battery NOT loaded
}
```

**Result**: "Resume" looked like it worked (same player object) but:
- Player position NOT restored (always center)
- Room NOT restored (always R5)
- Stamina/battery NOT restored (always defaults)
- **Effectively a "New Game" but without full reset**

**Fix**: Explicit save/load with GameSession
```java
// NEW CODE
resumeBtn.onClick() {
    gm.resumeSession();  // ← Explicitly restore from session
    gm.setCurrentState(PLAYING);
    game.setScreen(new PlayScreen(game));
}

// resumeSession() implementation
public void resumeSession() {
    currentSession.restoreToPlayer(player);  // ← Load position
    currentRoomId = currentSession.getCurrentRoomId();  // ← Load room
    // Now player ACTUALLY at saved state
}
```

---

## 🔧 Files Modified

### Part A (Sprite/Hitbox)
- ✅ [Constants.java](../core/src/main/java/com/fearjosh/frontend/config/Constants.java) - Added render/collision constants
- ✅ [Player.java](../core/src/main/java/com/fearjosh/frontend/entity/Player.java) - Separated render from hitbox
- ✅ [PlayScreen.java](../core/src/main/java/com/fearjosh/frontend/screen/PlayScreen.java) - Uses getRenderWidth/Height
- ✅ [GameManager.java](../core/src/main/java/com/fearjosh/frontend/core/GameManager.java) - Uses Constants for init
- ✅ [RoomTransitionSystem.java](../core/src/main/java/com/fearjosh/frontend/systems/RoomTransitionSystem.java) - Uses render size

### Part B (Session)
- ✅ [GameSession.java](../core/src/main/java/com/fearjosh/frontend/core/GameSession.java) - **NEW FILE** (session model)
- ✅ [GameManager.java](../core/src/main/java/com/fearjosh/frontend/core/GameManager.java) - Session management methods
- ✅ [MainMenuScreen.java](../core/src/main/java/com/fearjosh/frontend/screen/MainMenuScreen.java) - Fixed New Game/Resume
- ✅ [PlayScreen.java](../core/src/main/java/com/fearjosh/frontend/screen/PlayScreen.java) - Save on pause menu exit
- ✅ [SettingsScreen.java](../core/src/main/java/com/fearjosh/frontend/screen/SettingsScreen.java) - Difficulty lock popup

---

## 🎯 Summary

**Part A - Player Sprite:**
- ✅ Fixed squashed sprite rendering (48x80 instead of forced 64x64)
- ✅ Separated visual render size from collision hitboxes
- ✅ All sizes now tunable via Constants.java
- ✅ Can adjust appearance WITHOUT breaking collision

**Part B - Session Management:**
- ✅ Created GameSession model for proper save/load
- ✅ Fixed Resume to actually continue progress (not reset)
- ✅ Pause → Menu → Resume now works correctly (saves state)
- ✅ Difficulty locked during active run with confirmation popup
- ✅ Clear separation: New Game = fresh session, Resume = continue session

**Impact:**
- 🎨 Player visual now customizable (adjust width/height independently)
- 🎮 Gameplay mechanics preserved (hitboxes separate from visuals)
- 💾 Progress actually saved (Resume works as expected)
- 🔒 Difficulty consistency enforced (no mid-run changes without warning)
