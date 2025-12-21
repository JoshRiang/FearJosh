# Death System Implementation Summary

## Overview
Implemented a complete capture/death system where Josh catches and ties up the player.

## Features Implemented

### 1. **CapturedState** (New Player State)
- **File**: `CapturedState.java`
- Player cannot move (speed multiplier = 0)
- Cannot sprint
- Stamina doesn't drain/regen

### 2. **Capture Timer System**
- Josh must hold player for **2 seconds** to trigger capture
- If player escapes before 2 seconds, capture is cancelled
- Timer starts when Josh collides with player

### 3. **Capture Sequence**
When player is caught for 2 seconds:
1. **Transition**: `josh_caught_you.jpg` fades in over 1.5 seconds
2. **Player Sprite**: Changes to `jonatan_terikat.png` (tied up sprite)
3. **Movement**: Player cannot move (CapturedState active)
4. **Inventory**: Hidden/disabled
5. **Life**: Reduced by 1
6. **Hint Box**: Shows "Press F to get free"

### 4. **UI Changes**
- Capture transition overlay with fade-in effect
- Hint box with black background and white border
- Inventory automatically hidden when captured
- Hearts/lives update automatically

### 5. **Input Handling**
- All movement input disabled when captured
- Inventory input disabled when captured
- Only **F key** is active (for future minigame trigger)

## Files Modified

1. **Player.java**
   - Added `isCaptured` boolean flag
   - Added `tiedTexture` for tied sprite
   - Modified `getCurrentFrame()` to show tied sprite when captured
   - Added `dispose()` method for texture cleanup

2. **PlayScreen.java**
   - Added capture timer logic (2-second delay)
   - Added capture transition texture and fade-in effect
   - Added hint box rendering
   - Modified input handling to block all input except F when captured
   - Modified inventory rendering to hide when captured
   - Added `triggerPlayerCaptured()` method
   - Life reduction integration

3. **CapturedState.java** (New)
   - Implements PlayerState interface
   - Prevents all movement and stamina changes
   - Singleton pattern for efficient state management

## Assets Required
- ✅ `Sprite/Player/jonatan_terikat.png` - Player tied up sprite
- ✅ `UI/josh_caught_you.jpg` - Capture transition image

## Next Steps (TODO)
- [ ] Implement minigame when F key is pressed
- [ ] Add escape mechanic (successful minigame = player freed)
- [ ] Add failure mechanic (failed minigame = game over?)
- [ ] Add sound effects for capture sequence
- [ ] Consider adding struggle animation while being caught

## How It Works

1. **Detection**: `checkEnemyPlayerCollision()` runs every frame
2. **Timer Start**: First collision starts 2-second timer
3. **Escape Window**: Player can escape if collision breaks before 2 seconds
4. **Capture Trigger**: After 2 seconds, `triggerPlayerCaptured()` is called
5. **State Change**: Player switches to CapturedState
6. **Visual Feedback**: Transition fades in, sprite changes, hint appears
7. **Waiting**: Player sees "Press F to get free" prompt
8. **Minigame**: (Not yet implemented) F key will trigger escape minigame

## Testing Notes
- Test capture by letting Josh catch player and waiting 2 seconds
- Test escape by breaking away before 2 seconds
- Verify life count decreases when captured
- Verify inventory is hidden
- Verify player cannot move when captured
- Verify F key prompt appears after transition completes
