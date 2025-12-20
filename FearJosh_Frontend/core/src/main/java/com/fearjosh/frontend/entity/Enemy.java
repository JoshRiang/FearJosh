package com.fearjosh.frontend.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.fearjosh.frontend.state.enemy.*;
import com.fearjosh.frontend.world.Room;
import com.fearjosh.frontend.world.RoomId;
import com.fearjosh.frontend.world.objects.Table;
import com.fearjosh.frontend.world.objects.Locker;
import com.fearjosh.frontend.systems.PathfindingSystem;
import java.util.List;
import java.util.ArrayList;

public class Enemy {

    private float x, y;
    private float width, height;

    // speed dasar
    private float chaseSpeed = 90f;

    // state pattern
    private EnemyState currentState;
    private EnemyStateType currentStateType;

    private final EnemyState searchingState;
    private final EnemyState chasingState;
    private final EnemyStunnedState stunnedState;

    // ==================== SPRITE ANIMATION SYSTEM ====================
    private Texture joshSpriteSheet;
    private Animation<TextureRegion> walkUpAnimation;
    private Animation<TextureRegion> walkDownAnimation;
    private Animation<TextureRegion> walkLeftAnimation;
    private Animation<TextureRegion> walkRightAnimation;
    private Animation<TextureRegion> idleAnimation;
    private float animationTime = 0f;

    // Josh idle sprites
    private Texture joshIdle1;
    private Texture joshIdle2;

    // Josh caught sprite (for capture animation)
    private Texture joshCaughtTexture;

    // Track last movement direction for sprite facing
    private float lastDx = 0f;
    private float lastDy = 0f;
    // Old system: Enemy despawned after losing sight, respawned near player
    // New system: RoomDirector handles abstract/physical presence via room
    // adjacency
    private float lastSeenX, lastSeenY;
    private RoomId lastSeenRoomId;
    private float despawnTimer = 0f;
    private static final float DESPAWN_DELAY = 999999f; // Effectively disabled
    private boolean isDespawned = false;
    private float respawnCheckTimer = 0f;
    private static final float RESPAWN_CHECK_INTERVAL = 999999f; // Effectively disabled

    // [DEPRECATED] Old respawn distance - now handled by RoomDirector door spawning
    public static final float RESPAWN_DISTANCE = 200f;

    // Detection radii untuk debug visual
    public static final float DETECTION_RADIUS = 220f; // hearing range (hijau)
    public static final float VISION_RADIUS = 350f; // vision range (biru)

    // ==================== PATHFINDING ====================

    private List<float[]> currentPath = new ArrayList<>();
    private int currentWaypointIndex = 0;
    private float pathRecalculateTimer = 0f;
    private static final float PATH_RECALCULATE_INTERVAL = 0.5f; // Recalculate every 0.5s
    private static final float WAYPOINT_REACH_DISTANCE = 12f; // Distance to consider waypoint reached

    // Current pathfinding target
    private float pathTargetX = 0f;
    private float pathTargetY = 0f;

    public Enemy(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.lastSeenX = x;
        this.lastSeenY = y;
        this.lastSeenRoomId = null; // akan di-set saat update pertama

        searchingState = new EnemySearchingState();
        chasingState = new EnemyChasingState();
        stunnedState = new EnemyStunnedState(1.5f);

        changeState(searchingState);

        // Load Josh animations
        loadAnimations();
    }

    /**
     * Load Josh sprite animations from assets
     * Spritesheet layout: 4x4 grid (64x64 per frame)
     * Row 0: UP facing (4 frames)
     * Row 1: DOWN facing (4 frames)
     * Row 2: LEFT facing (4 frames)
     * Row 3: RIGHT facing (4 frames)
     */
    private void loadAnimations() {
        // Load general animation spritesheet (4x4 = 16 frames total)
        // Total size: 519x480 pixels
        joshSpriteSheet = new Texture("Sprite/Enemy/josh_general_animation.png");

        // Calculate frame size: 519÷4 = 129.75, 480÷4 = 120
        // Using 129x120 per frame (slightly less to fit 4 columns)
        TextureRegion[][] tmp = TextureRegion.split(joshSpriteSheet, 129, 120);

        // Create UP animation (row 0, 4 frames)
        TextureRegion[] upFrames = new TextureRegion[4];
        for (int i = 0; i < 4; i++) {
            upFrames[i] = tmp[0][i];
        }
        walkUpAnimation = new Animation<>(0.15f, upFrames);
        walkUpAnimation.setPlayMode(Animation.PlayMode.LOOP);

        // Create DOWN animation (row 1, 4 frames)
        TextureRegion[] downFrames = new TextureRegion[4];
        for (int i = 0; i < 4; i++) {
            downFrames[i] = tmp[1][i];
        }
        walkDownAnimation = new Animation<>(0.15f, downFrames);
        walkDownAnimation.setPlayMode(Animation.PlayMode.LOOP);

        // Create LEFT animation (row 2, 4 frames)
        TextureRegion[] leftFrames = new TextureRegion[4];
        for (int i = 0; i < 4; i++) {
            leftFrames[i] = tmp[2][i];
        }
        walkLeftAnimation = new Animation<>(0.15f, leftFrames);
        walkLeftAnimation.setPlayMode(Animation.PlayMode.LOOP);

        // Create RIGHT animation (row 3, 4 frames)
        TextureRegion[] rightFrames = new TextureRegion[4];
        for (int i = 0; i < 4; i++) {
            rightFrames[i] = tmp[3][i];
        }
        walkRightAnimation = new Animation<>(0.15f, rightFrames);
        walkRightAnimation.setPlayMode(Animation.PlayMode.LOOP);

        // Load idle sprites
        joshIdle1 = new Texture("Sprite/Enemy/josh_idle_1.png");
        joshIdle2 = new Texture("Sprite/Enemy/josh_idle_2.png");

        // Create idle animation (2 frames, slower)
        TextureRegion[] idleFrames = new TextureRegion[2];
        idleFrames[0] = new TextureRegion(joshIdle1);
        idleFrames[1] = new TextureRegion(joshIdle2);
        idleAnimation = new Animation<>(0.5f, idleFrames);
        idleAnimation.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);

        // Load caught sprite
        joshCaughtTexture = new Texture("Sprite/Enemy/josh_caught.png");

        System.out.println("[Enemy] Josh 4-directional animations loaded successfully");
    }

    public void changeState(EnemyState newState) {
        if (currentState == newState)
            return;
        if (currentState != null)
            currentState.onExit(this);
        currentState = newState;
        if (currentState != null)
            currentState.onEnter(this);
    }

    public void update(Player player, Room room, float delta) {
        // Set last seen room jika null
        if (lastSeenRoomId == null) {
            lastSeenRoomId = room.getId();
        }

        // OLD RESPAWN SYSTEM DISABLED - Now handled by RoomDirector
        // Despawn/respawn logic replaced with abstract/physical presence in
        // RoomDirector
        // Enemy spawning is controlled by RoomDirector.isEnemyPhysicallyPresent()

        if (isDespawned) {
            // Enemy is despawned, no update needed
            // RoomDirector will handle re-spawning via door entry
            return;
        }

        // Update animation time
        animationTime += delta;

        // Update state normal
        if (currentState != null) {
            currentState.update(this, player, room, delta);
        }

        // Update last seen position saat dalam chasing state
        if (currentStateType == EnemyStateType.CHASING) {
            lastSeenX = x;
            lastSeenY = y;
            lastSeenRoomId = room.getId();
            despawnTimer = 0f;
        }
        // OLD DESPAWN TIMER DISABLED
        // Despawning now handled by room transitions in RoomDirector
    }

    // [DEPRECATED] Old adjacency check - now in RoomDirector.moveCloser()
    @Deprecated
    private boolean isAdjacentRoom(RoomId currentRoom, RoomId lastSeen) {
        if (currentRoom == lastSeen)
            return true;
        return currentRoom == lastSeen.up() ||
                currentRoom == lastSeen.down() ||
                currentRoom == lastSeen.left() ||
                currentRoom == lastSeen.right();
    }

    // [DEPRECATED] Old despawn - now controlled by RoomDirector
    @Deprecated
    private void despawn() {
        isDespawned = true;
        despawnTimer = 0f;
        respawnCheckTimer = 0f;
    }

    // [DEPRECATED] Old respawn logic - replaced by RoomDirector door spawning
    @Deprecated
    private void respawnNearPlayer(Player player) {
        // OLD SYSTEM: Random offset spawn near player
        // NEW SYSTEM: RoomDirector spawns at door position
        // This method is no longer called
    }

    /**
     * MAIN RENDER WITH SPRITE - Gambar Josh dengan animasi 4-directional
     * 
     * @param batch SpriteBatch untuk render sprite
     */
    public void render(SpriteBatch batch) {
        TextureRegion currentFrame;

        // Choose animation based on state and movement direction
        if (currentStateType == EnemyStateType.CHASING) {
            // Walking animation when chasing - choose direction based on last movement
            Animation<TextureRegion> currentAnimation = getWalkAnimationForDirection();
            currentFrame = currentAnimation.getKeyFrame(animationTime, true);
        } else if (currentStateType == EnemyStateType.STUNNED) {
            // Show caught sprite when stunned
            currentFrame = new TextureRegion(joshCaughtTexture);
        } else {
            // Idle animation when searching
            currentFrame = idleAnimation.getKeyFrame(animationTime, true);
        }

        // Draw sprite
        batch.draw(currentFrame, x, y, width, height);
    }

    /**
     * Get appropriate walk animation based on movement direction
     */
    private Animation<TextureRegion> getWalkAnimationForDirection() {
        // Determine primary direction based on last movement
        if (Math.abs(lastDy) > Math.abs(lastDx)) {
            // Vertical movement is dominant
            if (lastDy > 0) {
                return walkUpAnimation; // Moving up
            } else {
                return walkDownAnimation; // Moving down
            }
        } else {
            // Horizontal movement is dominant (or equal)
            if (lastDx > 0) {
                return walkRightAnimation; // Moving right
            } else if (lastDx < 0) {
                return walkLeftAnimation; // Moving left
            } else {
                // No movement, default to down
                return walkDownAnimation;
            }
        }
    }

    /**
     * LEGACY RENDER (ShapeRenderer) - untuk debug atau fallback
     * WARNA = INDIKATOR STATE
     */
    @Deprecated
    public void renderShape(ShapeRenderer renderer) {
        // Draw Josh dengan warna berdasarkan state
        Color stateColor = getStateColor();
        renderer.setColor(stateColor);
        renderer.rect(x, y, width, height);

        // Optional: outline putih untuk visibility
        renderer.setColor(Color.WHITE);
        renderer.rectLine(x, y, x + width, y, 2f);
        renderer.rectLine(x + width, y, x + width, y + height, 2f);
        renderer.rectLine(x + width, y + height, x, y + height, 2f);
        renderer.rectLine(x, y + height, x, y, 2f);
    }

    /**
     * DEBUG RENDER - Detection circles + hitbox
     */
    public void renderDebug(ShapeRenderer renderer) {
        // Draw detection circles (hijau = hearing, biru = vision)
        renderer.setColor(0f, 1f, 0f, 0.3f); // hijau semi-transparent
        renderer.circle(getCenterX(), getCenterY(), DETECTION_RADIUS);

        renderer.setColor(0f, 0f, 1f, 0.2f); // biru semi-transparent
        renderer.circle(getCenterX(), getCenterY(), VISION_RADIUS);

        // Draw main hitbox
        Color stateColor = getStateColor();
        renderer.setColor(stateColor.r, stateColor.g, stateColor.b, 0.5f);
        renderer.rect(x, y, width, height);
    }

    private Color getStateColor() {
        switch (currentStateType) {
            case SEARCHING:
                return Color.YELLOW; // kuning
            case CHASING:
                return Color.RED; // merah
            case STUNNED:
                return Color.CYAN; // cyan (lebih jelas dari putih)
            default:
                return Color.RED;
        }
    }

    // Movement dengan collision detection - SMOOTH DIAGONAL MOVEMENT
    public void move(float dx, float dy, Room room) {
        // Track movement direction for sprite animation
        lastDx = dx;
        lastDy = dy;

        float oldX = x;
        float oldY = y;

        // Try full diagonal movement first
        x = oldX + dx;
        y = oldY + dy;

        if (collidesWithFurniture(room)) {
            // Full diagonal blocked, try sliding along obstacles

            // Try X-only movement (slide horizontally)
            x = oldX + dx;
            y = oldY;
            boolean xBlocked = collidesWithFurniture(room);

            // Try Y-only movement (slide vertically)
            x = oldX;
            y = oldY + dy;
            boolean yBlocked = collidesWithFurniture(room);

            // Apply best available movement
            if (!xBlocked && !yBlocked) {
                // Both axes free, prefer original direction
                // Use the axis with larger delta for sliding
                if (Math.abs(dx) > Math.abs(dy)) {
                    x = oldX + dx; // Slide horizontally
                    y = oldY;
                } else {
                    x = oldX; // Slide vertically
                    y = oldY + dy;
                }
            } else if (!xBlocked) {
                x = oldX + dx; // Only X is free
                y = oldY;
            } else if (!yBlocked) {
                x = oldX; // Only Y is free
                y = oldY + dy;
            } else {
                // Both blocked, stay in place
                x = oldX;
                y = oldY;
            }
        }
        // If no collision, position already set to (oldX+dx, oldY+dy)
    }

    private boolean collidesWithFurniture(Room room) {
        for (Table t : room.getTables()) {
            if (overlapsRect(x, y, width, height, t.getX(), t.getY(), t.getWidth(), t.getHeight()))
                return true;
        }
        for (Locker l : room.getLockers()) {
            if (overlapsRect(x, y, width, height, l.getX(), l.getY(), l.getWidth(), l.getHeight()))
                return true;
        }
        return false;
    }

    private boolean overlapsRect(float x, float y, float w, float h,
            float x2, float y2, float w2, float h2) {
        return x < x2 + w2 &&
                x + w > x2 &&
                y < y2 + h2 &&
                y + h > y2;
    }

    public float getCenterX() {
        return x + width / 2f;
    }

    public float getCenterY() {
        return y + height / 2f;
    }

    public float getChaseSpeed() {
        return chaseSpeed;
    }

    public EnemyState getSearchingState() {
        return searchingState;
    }

    public EnemyState getChasingState() {
        return chasingState;
    }

    public EnemyStunnedState getStunnedState() {
        return stunnedState;
    }

    public void setCurrentStateType(EnemyStateType type) {
        this.currentStateType = type;
    }

    public EnemyStateType getCurrentStateType() {
        return currentStateType;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setX(float newX) {
        this.x = newX;
    }

    public void setY(float newY) {
        this.y = newY;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getLastSeenX() {
        return lastSeenX;
    }

    public float getLastSeenY() {
        return lastSeenY;
    }

    public RoomId getLastSeenRoomId() {
        return lastSeenRoomId;
    }

    public boolean isDespawned() {
        return isDespawned;
    }

    // ==================== PATHFINDING METHODS ====================

    /**
     * Calculate path to target using A* pathfinding
     * 
     * @param targetX     Target X coordinate
     * @param targetY     Target Y coordinate
     * @param room        Current room
     * @param worldWidth  World width
     * @param worldHeight World height
     */
    public void calculatePathTo(float targetX, float targetY, Room room, float worldWidth, float worldHeight) {
        pathTargetX = targetX;
        pathTargetY = targetY;

        List<float[]> rawPath = PathfindingSystem.findPath(
                getCenterX(), getCenterY(),
                targetX, targetY,
                room, worldWidth, worldHeight);

        // Simplify path to reduce waypoints
        currentPath = PathfindingSystem.simplifyPath(rawPath);
        currentWaypointIndex = 0;
        pathRecalculateTimer = 0f;

        if (com.fearjosh.frontend.config.Constants.DEBUG_ROOM_DIRECTOR) {
            System.out.println("[Enemy] Path calculated: " + currentPath.size() + " waypoints");
        }
    }

    /**
     * Move along current path towards target
     * Call this from update loop when in chasing state
     * 
     * @param delta       Delta time
     * @param room        Current room
     * @param worldWidth  World width
     * @param worldHeight World height
     * @return true if moving along path, false if path exhausted
     */
    public boolean followPath(float delta, Room room, float worldWidth, float worldHeight) {
        // No path
        if (currentPath.isEmpty()) {
            return false;
        }

        // Recalculate path periodically
        pathRecalculateTimer += delta;
        if (pathRecalculateTimer >= PATH_RECALCULATE_INTERVAL) {
            calculatePathTo(pathTargetX, pathTargetY, room, worldWidth, worldHeight);
        }

        // Get current waypoint
        if (currentWaypointIndex >= currentPath.size()) {
            return false; // Path exhausted
        }

        float[] waypoint = currentPath.get(currentWaypointIndex);
        float waypointX = waypoint[0];
        float waypointY = waypoint[1];

        // Calculate direction to waypoint (SMOOTH DIAGONAL MOVEMENT)
        float dx = waypointX - getCenterX();
        float dy = waypointY - getCenterY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        // Reached waypoint
        if (distance <= WAYPOINT_REACH_DISTANCE) {
            currentWaypointIndex++;
            return true;
        }

        // Normalize direction vector for smooth diagonal movement
        dx = dx / distance; // Unit vector
        dy = dy / distance;

        // Scale by speed
        float speed = chaseSpeed * delta;
        dx = dx * speed;
        dy = dy * speed;

        // Move with BOTH X and Y applied together (true diagonal)
        move(dx, dy, room);
        return true;
    }

    /**
     * Check if enemy has active path
     */
    public boolean hasPath() {
        return !currentPath.isEmpty() && currentWaypointIndex < currentPath.size();
    }

    /**
     * Clear current path
     */
    public void clearPath() {
        currentPath.clear();
        currentWaypointIndex = 0;
    }

    /**
     * Get current pathfinding target
     */
    public float[] getPathTarget() {
        return new float[] { pathTargetX, pathTargetY };
    }

    // ==================== ENHANCED DEBUG RENDERING ====================

    /**
     * ENHANCED DEBUG RENDER with pathfinding visualization
     * Shows: hearing/vision circles, hitbox, path line
     */
    public void renderDebugEnhanced(ShapeRenderer renderer) {
        // Draw hearing circle (YELLOW - larger)
        renderer.setColor(1f, 1f, 0f, 0.25f); // Yellow semi-transparent
        renderer.circle(getCenterX(), getCenterY(), DETECTION_RADIUS);

        // Draw vision circle (RED - smaller)
        renderer.setColor(1f, 0f, 0f, 0.35f); // Red semi-transparent
        renderer.circle(getCenterX(), getCenterY(), VISION_RADIUS);

        // Draw main hitbox with state color
        Color stateColor = getStateColor();
        renderer.setColor(stateColor.r, stateColor.g, stateColor.b, 0.5f);
        renderer.rect(x, y, width, height);

        // Draw pathfinding visualization
        if (!currentPath.isEmpty()) {
            // Draw line from enemy to first waypoint
            if (currentWaypointIndex < currentPath.size()) {
                float[] waypoint = currentPath.get(currentWaypointIndex);
                renderer.setColor(0f, 1f, 1f, 1f); // Cyan
                renderer.rectLine(getCenterX(), getCenterY(), waypoint[0], waypoint[1], 2f);
            }

            // Draw all waypoints
            renderer.setColor(1f, 1f, 1f, 0.8f); // White
            for (float[] point : currentPath) {
                renderer.circle(point[0], point[1], 3f);
            }

            // Draw line to final target
            if (!currentPath.isEmpty()) {
                renderer.setColor(1f, 0f, 1f, 0.7f); // Magenta
                renderer.rectLine(getCenterX(), getCenterY(), pathTargetX, pathTargetY, 1f);
            }
        }
    }

    /**
     * Dispose textures when enemy is destroyed
     */
    public void dispose() {
        if (joshSpriteSheet != null)
            joshSpriteSheet.dispose();
        if (joshIdle1 != null)
            joshIdle1.dispose();
        if (joshIdle2 != null)
            joshIdle2.dispose();
        if (joshCaughtTexture != null)
            joshCaughtTexture.dispose();
    }
}
