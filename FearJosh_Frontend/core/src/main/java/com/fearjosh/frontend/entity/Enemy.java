package com.fearjosh.frontend.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.fearjosh.frontend.state.enemy.*;
import com.fearjosh.frontend.world.RoomId;
import com.fearjosh.frontend.systems.PathfindingSystem;
import com.fearjosh.frontend.render.TiledMapManager;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings("unused") // Some fields are kept for future features or legacy compatibility
public class Enemy {

    private float x, y;
    private float width, height;

    // DUAL HITBOX SYSTEM (same concept as Player)
    // 1) BODY HITBOX - for capture/damage detection (full body)
    private Rectangle bodyBounds;
    // 2) FOOT HITBOX - for world collision (walls, furniture)
    private Rectangle footBounds;

    // TMX collision detection
    private TiledMapManager tiledMapManager;

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

    // Searching state animations (separate sprites)
    private Texture searchingUpTexture;
    private Texture searchingDownTexture;
    private Texture searchingLeftTexture;
    private Texture searchingRightTexture;
    private Animation<TextureRegion> searchingUpAnimation;
    private Animation<TextureRegion> searchingDownAnimation;
    private Animation<TextureRegion> searchingLeftAnimation;
    private Animation<TextureRegion> searchingRightAnimation;

    // Chasing state animations (separate sprites)
    private Texture chasingUpTexture;
    private Texture chasingDownTexture;
    private Texture chasingLeftTexture;
    private Texture chasingRightTexture;
    private Animation<TextureRegion> chasingUpAnimation;
    private Animation<TextureRegion> chasingDownAnimation;
    private Animation<TextureRegion> chasingLeftAnimation;
    private Animation<TextureRegion> chasingRightAnimation;

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
        this.bodyBounds = new Rectangle();
        this.footBounds = new Rectangle();
        updateHitboxes(); // Initialize both hitboxes

        searchingState = new EnemySearchingState();
        chasingState = new EnemyChasingState();
        stunnedState = new EnemyStunnedState(1.5f);

        changeState(searchingState);

        // Load Josh animations
        loadAnimations();
    }

    /**
     * Update BOTH hitboxes position (call after moving)
     * DUAL HITBOX SYSTEM - same concept as Player, but sized for Josh
     * 
     * IMPORTANT: Uses Josh's own dimensions, NOT copied from Player!
     * Josh is larger than Player (intimidation factor), so hitboxes scale proportionally.
     */
    private void updateHitboxes() {
        // 1) BODY HITBOX - full sprite for capture/damage detection
        // Uses full width/height of Josh sprite
        bodyBounds.set(x, y, width, height);
        
        // 2) FOOT HITBOX - bottom portion for world collision
        // Proportional to Josh's own size (NOT Player's values!)
        // Width: 50% of Josh sprite width
        // Height: 25% of Josh sprite height
        float footW = width * 0.5f;
        float footH = height * 0.25f;
        float footX = x + (width - footW) / 2f; // Center horizontally
        float footY = y; // Bottom of sprite
        
        footBounds.set(footX, footY, footW, footH);
    }

    /**
     * Get BODY hitbox for capture/damage detection
     * Use this when checking if Josh has caught the player
     */
    public Rectangle getBodyBounds() {
        return bodyBounds;
    }

    /**
     * Get FOOT hitbox for world collision detection
     * Use this when checking collision with walls, furniture, obstacles
     * (NOT for capture detection - use getBodyBounds() for that)
     */
    public Rectangle getFootBounds() {
        return footBounds;
    }

    /**
     * Load Josh sprite animations from assets
     * Using individual sprite sheets for chasing and searching states
     */
    private void loadAnimations() {
        // Load chasing sprite sheets (fast aggressive movement)
        chasingUpTexture = new Texture("Sprite/Enemy/josh_chasing_up.png");
        chasingDownTexture = new Texture("Sprite/Enemy/josh_chasing_down.png");
        chasingLeftTexture = new Texture("Sprite/Enemy/josh_chasing_left.png");
        chasingRightTexture = new Texture("Sprite/Enemy/josh_chasing_right.png");

        // Split each texture into 4 frames (1 row x 4 columns)
        TextureRegion[][] chasingUpFrames = TextureRegion.split(chasingUpTexture,
                chasingUpTexture.getWidth() / 4, chasingUpTexture.getHeight());
        TextureRegion[][] chasingDownFrames = TextureRegion.split(chasingDownTexture,
                chasingDownTexture.getWidth() / 4, chasingDownTexture.getHeight());
        TextureRegion[][] chasingLeftFrames = TextureRegion.split(chasingLeftTexture,
                chasingLeftTexture.getWidth() / 4, chasingLeftTexture.getHeight());
        TextureRegion[][] chasingRightFrames = TextureRegion.split(chasingRightTexture,
                chasingRightTexture.getWidth() / 4, chasingRightTexture.getHeight());

        // Create chasing animations (medium pace)
        chasingUpAnimation = new Animation<>(0.2f, chasingUpFrames[0]);
        chasingUpAnimation.setPlayMode(Animation.PlayMode.LOOP);

        chasingDownAnimation = new Animation<>(0.2f, chasingDownFrames[0]);
        chasingDownAnimation.setPlayMode(Animation.PlayMode.LOOP);

        chasingLeftAnimation = new Animation<>(0.2f, chasingLeftFrames[0]);
        chasingLeftAnimation.setPlayMode(Animation.PlayMode.LOOP);

        chasingRightAnimation = new Animation<>(0.2f, chasingRightFrames[0]);
        chasingRightAnimation.setPlayMode(Animation.PlayMode.LOOP);

        // Set walk animations to use chasing animations (for compatibility)
        walkUpAnimation = chasingUpAnimation;
        walkDownAnimation = chasingDownAnimation;
        walkLeftAnimation = chasingLeftAnimation;
        walkRightAnimation = chasingRightAnimation;

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

        // Load searching animations (separate sprite sheets)
        loadSearchingAnimations();

        System.out.println("[Enemy] Josh animations loaded - Chasing and Searching sprites");
    }

    /**
     * Load searching state animations from separate sprite sheets
     */
    private void loadSearchingAnimations() {
        // Load searching sprite sheets (each has 4 frames in a row)
        searchingUpTexture = new Texture("Sprite/Enemy/josh_searching_up.png");
        searchingDownTexture = new Texture("Sprite/Enemy/josh_searching_down.png");
        searchingLeftTexture = new Texture("Sprite/Enemy/josh_searching_left.png");
        searchingRightTexture = new Texture("Sprite/Enemy/josh_searching_right.png");

        // Split each texture into 4 frames (1 row x 4 columns)
        // Assuming each sprite is around 64x64 pixels per frame
        TextureRegion[][] searchUpFrames = TextureRegion.split(searchingUpTexture,
                searchingUpTexture.getWidth() / 4, searchingUpTexture.getHeight());
        TextureRegion[][] searchDownFrames = TextureRegion.split(searchingDownTexture,
                searchingDownTexture.getWidth() / 4, searchingDownTexture.getHeight());
        TextureRegion[][] searchLeftFrames = TextureRegion.split(searchingLeftTexture,
                searchingLeftTexture.getWidth() / 4, searchingLeftTexture.getHeight());
        TextureRegion[][] searchRightFrames = TextureRegion.split(searchingRightTexture,
                searchingRightTexture.getWidth() / 4, searchingRightTexture.getHeight());

        // Create searching animations (slower pace for searching behavior)
        searchingUpAnimation = new Animation<>(0.3f, searchUpFrames[0]);
        searchingUpAnimation.setPlayMode(Animation.PlayMode.LOOP);

        searchingDownAnimation = new Animation<>(0.3f, searchDownFrames[0]);
        searchingDownAnimation.setPlayMode(Animation.PlayMode.LOOP);

        searchingLeftAnimation = new Animation<>(0.3f, searchLeftFrames[0]);
        searchingLeftAnimation.setPlayMode(Animation.PlayMode.LOOP);

        searchingRightAnimation = new Animation<>(0.3f, searchRightFrames[0]);
        searchingRightAnimation.setPlayMode(Animation.PlayMode.LOOP);

        System.out.println("[Enemy] Josh searching animations loaded successfully");
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

    public void update(Player player, float delta) {
        // lastSeenRoomId is now set externally via setLastSeenRoomId() by RoomDirector
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

        // Update state normal (TMX-based collision via move() method)
        if (currentState != null) {
            currentState.update(this, player, delta);
        }

        // Update last seen position saat dalam chasing state
        if (currentStateType == EnemyStateType.CHASING) {
            lastSeenX = x;
            lastSeenY = y;
            // lastSeenRoomId updated externally by RoomDirector
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
        } else if (currentStateType == EnemyStateType.SEARCHING) {
            // Searching animation - slower, more cautious movement
            Animation<TextureRegion> searchAnimation = getSearchingAnimationForDirection();
            currentFrame = searchAnimation.getKeyFrame(animationTime, true);
        } else {
            // Idle animation as fallback
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
     * Get appropriate searching animation based on movement direction
     */
    private Animation<TextureRegion> getSearchingAnimationForDirection() {
        // Determine primary direction based on last movement
        if (Math.abs(lastDy) > Math.abs(lastDx)) {
            // Vertical movement is dominant
            if (lastDy > 0) {
                return searchingUpAnimation; // Moving up
            } else {
                return searchingDownAnimation; // Moving down
            }
        } else {
            // Horizontal movement is dominant (or equal)
            if (lastDx > 0) {
                return searchingRightAnimation; // Moving right
            } else if (lastDx < 0) {
                return searchingLeftAnimation; // Moving left
            } else {
                // No movement, default to down
                return searchingDownAnimation;
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

        // Draw body hitbox (for capture detection) - RED
        Color stateColor = getStateColor();
        renderer.setColor(stateColor.r, stateColor.g, stateColor.b, 0.5f);
        renderer.rect(bodyBounds.x, bodyBounds.y, bodyBounds.width, bodyBounds.height);
        
        // Draw foot hitbox (for world collision) - GREEN
        renderer.setColor(0f, 1f, 0f, 0.8f);
        renderer.rect(footBounds.x, footBounds.y, footBounds.width, footBounds.height);
    }
    
    /**
     * DEBUG: Render hitboxes for visual debugging (similar to Player)
     * Call from PlayScreen with ShapeRenderer
     */
    public void debugRenderHitboxes(ShapeRenderer sr) {
        // Body hitbox - RED (untuk capture detection)
        sr.setColor(Color.RED);
        sr.rect(bodyBounds.x, bodyBounds.y, bodyBounds.width, bodyBounds.height);

        // Foot hitbox - GREEN (untuk world collision)
        sr.setColor(Color.GREEN);
        sr.rect(footBounds.x, footBounds.y, footBounds.width, footBounds.height);
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
    // Room parameter removed; uses TMX collision via TiledMapManager
    public void move(float dx, float dy) {
        // Track movement direction for sprite animation
        lastDx = dx;
        lastDy = dy;

        float oldX = x;
        float oldY = y;

        // Try full diagonal movement first
        x = oldX + dx;
        y = oldY + dy;
        updateHitboxes(); // Update both hitboxes after position change

        if (collidesWithFurniture()) {
            // Full diagonal blocked, try sliding along obstacles

            // Try X-only movement (slide horizontally)
            x = oldX + dx;
            y = oldY;
            updateHitboxes();
            boolean xBlocked = collidesWithFurniture();

            // Try Y-only movement (slide vertically)
            x = oldX;
            y = oldY + dy;
            updateHitboxes();
            boolean yBlocked = collidesWithFurniture();

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
            updateHitboxes(); // Final update after position resolved
        }
        // If no collision, position already set to (oldX+dx, oldY+dy)
    }

    /**
     * Collision detection using FOOT HITBOX (same as Player)
     * Checks all 4 corners and center of foot hitbox for walkability
     * Uses TMX collision via TiledMapManager (no Room parameter needed)
     */
    private boolean collidesWithFurniture() {
        // Check TMX collision using foot hitbox (same as Player)
        if (tiledMapManager != null && tiledMapManager.hasCurrentMap()) {
            // Check all 4 corners and center of foot hitbox
            if (!tiledMapManager.isWalkable(footBounds.x, footBounds.y) ||
                    !tiledMapManager.isWalkable(footBounds.x + footBounds.width, footBounds.y) ||
                    !tiledMapManager.isWalkable(footBounds.x, footBounds.y + footBounds.height) ||
                    !tiledMapManager.isWalkable(footBounds.x + footBounds.width, footBounds.y + footBounds.height) ||
                    !tiledMapManager.isWalkable(footBounds.x + footBounds.width / 2,
                            footBounds.y + footBounds.height / 2)) {
                return true;
            }
        }

        // No procedural furniture collision - TMX maps only
        return false;
    }

    /**
     * Set TiledMapManager for TMX collision detection
     */
    public void setTiledMapManager(TiledMapManager manager) {
        this.tiledMapManager = manager;
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

    /**
     * Set last seen room (called by RoomDirector when enemy enters a room)
     */
    public void setLastSeenRoomId(RoomId roomId) {
        this.lastSeenRoomId = roomId;
    }

    public boolean isDespawned() {
        return isDespawned;
    }

    // ==================== PATHFINDING METHODS ====================

    /**
     * Calculate path to target using A* pathfinding
     * Room parameter removed; pathfinding uses TMX via TiledMapManager
     * 
     * @param targetX     Target X coordinate
     * @param targetY     Target Y coordinate
     * @param worldWidth  World width
     * @param worldHeight World height
     */
    public void calculatePathTo(float targetX, float targetY, float worldWidth, float worldHeight) {
        pathTargetX = targetX;
        pathTargetY = targetY;

        List<float[]> rawPath = PathfindingSystem.findPath(
                getCenterX(), getCenterY(),
                targetX, targetY,
                tiledMapManager, worldWidth, worldHeight);

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
     * Room parameter removed; uses TMX collision via move()
     * 
     * @param delta       Delta time
     * @param worldWidth  World width
     * @param worldHeight World height
     * @return true if moving along path, false if path exhausted
     */
    public boolean followPath(float delta, float worldWidth, float worldHeight) {
        // No path
        if (currentPath.isEmpty()) {
            return false;
        }

        // Recalculate path periodically
        pathRecalculateTimer += delta;
        if (pathRecalculateTimer >= PATH_RECALCULATE_INTERVAL) {
            calculatePathTo(pathTargetX, pathTargetY, worldWidth, worldHeight);
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

        // Move with BOTH X and Y applied together (true diagonal, TMX collision)
        move(dx, dy);
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
        // Dispose idle and caught textures
        if (joshIdle1 != null)
            joshIdle1.dispose();
        if (joshIdle2 != null)
            joshIdle2.dispose();
        if (joshCaughtTexture != null)
            joshCaughtTexture.dispose();

        // Dispose searching textures
        if (searchingUpTexture != null)
            searchingUpTexture.dispose();
        if (searchingDownTexture != null)
            searchingDownTexture.dispose();
        if (searchingLeftTexture != null)
            searchingLeftTexture.dispose();
        if (searchingRightTexture != null)
            searchingRightTexture.dispose();

        // Dispose chasing textures
        if (chasingUpTexture != null)
            chasingUpTexture.dispose();
        if (chasingDownTexture != null)
            chasingDownTexture.dispose();
        if (chasingLeftTexture != null)
            chasingLeftTexture.dispose();
        if (chasingRightTexture != null)
            chasingRightTexture.dispose();
    }
}
