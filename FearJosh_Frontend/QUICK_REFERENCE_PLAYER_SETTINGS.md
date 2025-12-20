# 🎮 Quick Reference: Player Customization & Game Flow

## 🎨 Adjust Player Appearance (No Gameplay Impact)

**File**: [Constants.java](core/src/main/java/com/fearjosh/frontend/config/Constants.java)

### Make Player THINNER
```java
// Line 47
public static final float PLAYER_RENDER_WIDTH = 40f;  // Current: 48f
```

### Make Player WIDER
```java
// Line 47
public static final float PLAYER_RENDER_WIDTH = 56f;  // Current: 48f
```

### Make Player TALLER
```java
// Line 50
public static final float PLAYER_RENDER_HEIGHT = 90f;  // Current: 80f
```

### Make Player SHORTER
```java
// Line 50
public static final float PLAYER_RENDER_HEIGHT = 70f;  // Current: 80f
```

---

## ⚙️ Adjust Hitboxes (Affects Gameplay)

### Foot Collision (Furniture)
**Smaller = easier to squeeze through tight spaces**
```java
// Lines 53-56
public static final float PLAYER_COLLISION_WIDTH = 24f;   // Current: 32f (smaller)
public static final float PLAYER_COLLISION_HEIGHT = 16f;  // Current: 20f (smaller)
```

### Enemy Collision (Body)
**Smaller = harder for enemy to hit you**
```java
// Lines 61-64
public static final float PLAYER_ENEMY_HITBOX_WIDTH = 40f;   // Current: 48f (smaller)
public static final float PLAYER_ENEMY_HITBOX_HEIGHT = 72f;  // Current: 80f (smaller)
```

---

## 🎯 Game Flow States

### STATE 1: No Active Run
**Main Menu Shows:**
- ✅ New Game (enabled)
- ❌ Resume (hidden/disabled)

**Settings:**
- Difficulty changes immediately (no popup)

### STATE 2: Active Run
**Main Menu Shows:**
- ✅ New Game (enabled) → Starts fresh run
- ✅ Resume (enabled) → Continues where left off

**Settings:**
- Difficulty change → Shows popup warning
- Cancel → No change
- Confirm → New Game with new difficulty

### STATE 3: Playing
**Pause Menu:**
- Resume → Unpause (continues game)
- Main Menu → **Saves progress**, returns to menu (Resume still works)

---

## 🔍 Debug: Check Current Session

**In code**, check if session is active:
```java
GameManager gm = GameManager.getInstance();
boolean hasRun = gm.hasActiveSession();  // true if Resume should work
GameSession session = gm.getCurrentSession();  // null if no run
```

**Print session info**:
```java
if (gm.hasActiveSession()) {
    System.out.println("Session: " + gm.getCurrentSession());
    // Output: GameSession[id=123, room=R5, difficulty=MEDIUM, active=true]
}
```

---

## ⚠️ Common Issues

### Issue: Resume button does NEW GAME instead of continuing
**Cause**: Session not saved when going to menu
**Fix**: Already implemented in PlayScreen line 416-421 (`saveProgressToSession()`)

### Issue: Player sprite stretched/squashed
**Cause**: Render size doesn't match sprite aspect ratio
**Fix**: Adjust `PLAYER_RENDER_WIDTH` and `PLAYER_RENDER_HEIGHT` in Constants.java

### Issue: Player walks through furniture
**Cause**: Foot collision hitbox too small or disabled
**Fix**: Increase `PLAYER_COLLISION_WIDTH/HEIGHT` in Constants.java

### Issue: Enemy hits from too far away
**Cause**: Enemy hitbox too large
**Fix**: Decrease `PLAYER_ENEMY_HITBOX_WIDTH/HEIGHT` in Constants.java

### Issue: Difficulty change popup doesn't appear
**Cause**: No active session (game thinks no run started)
**Fix**: Must click "New Game" first, then Settings will show popup

---

## 📋 Test Sequence (After Changes)

1. **Build**: `.\gradlew.bat build`
2. **Run**: `.\gradlew.bat lwjgl3:run`
3. **Visual Test**:
   - Check player sprite proportions (not squashed)
   - Walk in all 4 directions
4. **Hitbox Test**:
   - Walk near furniture → foot collision works
   - Enemy touches player → body collision works
5. **Session Test**:
   - New Game → Play → Pause → Main Menu
   - Click Resume → Should continue (not reset)
6. **Difficulty Lock Test**:
   - New Game → Settings → Change difficulty
   - Popup should appear → Test Cancel and Confirm

---

## 🎨 Recommended Settings for "Thin" Player

```java
// Visual (line 47-50)
PLAYER_RENDER_WIDTH = 40f   // Thinner
PLAYER_RENDER_HEIGHT = 88f  // Taller to compensate

// Collision (line 53-56)
PLAYER_COLLISION_WIDTH = 28f   // Keep reasonable
PLAYER_COLLISION_HEIGHT = 20f  // Same as before

// Enemy Hitbox (line 61-64)
PLAYER_ENEMY_HITBOX_WIDTH = 40f   // Match render
PLAYER_ENEMY_HITBOX_HEIGHT = 88f  // Match render
```

This gives a "slim anime protagonist" look while keeping collision fair.

---

## 🎨 Recommended Settings for "Wide" Player

```java
// Visual (line 47-50)
PLAYER_RENDER_WIDTH = 60f   // Wider
PLAYER_RENDER_HEIGHT = 75f  // Shorter to compensate

// Collision (line 53-56)
PLAYER_COLLISION_WIDTH = 36f   // Wider
PLAYER_COLLISION_HEIGHT = 22f  // Slightly taller

// Enemy Hitbox (line 61-64)
PLAYER_ENEMY_HITBOX_WIDTH = 60f   // Match render
PLAYER_ENEMY_HITBOX_HEIGHT = 75f  // Match render
```

This gives a "stocky hero" look with proportionally larger hitboxes.
