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

@SuppressWarnings("unused")
public class Enemy {

    private float x, y;
    private float width, height;

    // HITBOX
    private Rectangle bodyBounds;
    private Rectangle footBounds;

    private TiledMapManager tiledMapManager;

    private float chaseSpeed = 90f;

    // STATE
    private EnemyState currentState;
    private EnemyStateType currentStateType;

    private final EnemyState searchingState;
    private final EnemyState chasingState;
    private final EnemyStunnedState stunnedState;

    // ANIMATION
    private Texture joshSpriteSheet;
    private Animation<TextureRegion> walkUpAnimation;
    private Animation<TextureRegion> walkDownAnimation;
    private Animation<TextureRegion> walkLeftAnimation;
    private Animation<TextureRegion> walkRightAnimation;
    private Animation<TextureRegion> idleAnimation;

    // SEARCHING ANIMATION
    private Texture searchingUpTexture;
    private Texture searchingDownTexture;
    private Texture searchingLeftTexture;
    private Texture searchingRightTexture;
    private Animation<TextureRegion> searchingUpAnimation;
    private Animation<TextureRegion> searchingDownAnimation;
    private Animation<TextureRegion> searchingLeftAnimation;
    private Animation<TextureRegion> searchingRightAnimation;

    // CHASING ANIMATION
    private Texture chasingUpTexture;
    private Texture chasingDownTexture;
    private Texture chasingLeftTexture;
    private Texture chasingRightTexture;
    private Animation<TextureRegion> chasingUpAnimation;
    private Animation<TextureRegion> chasingDownAnimation;
    private Animation<TextureRegion> chasingLeftAnimation;
    private Animation<TextureRegion> chasingRightAnimation;

    private float animationTime = 0f;

    // IDLE
    private Texture joshIdle1;
    private Texture joshIdle2;

    // CAUGHT
    private Texture joshCaughtTexture;

    // DIRECTION
    private float lastDx = 0f;
    private float lastDy = 0f;

    // SPAWN
    private float lastSeenX, lastSeenY;
    private RoomId lastSeenRoomId;
    private float despawnTimer = 0f;
    private static final float DESPAWN_DELAY = 999999f;
    private boolean isDespawned = false;
    private float respawnCheckTimer = 0f;
    private static final float RESPAWN_CHECK_INTERVAL = 999999f;

    // DISABLED
    public static final float RESPAWN_DISTANCE = 200f;

    // DETECTION
    public static final float DETECTION_RADIUS = 220f;
    public static final float VISION_RADIUS = 350f;

    // PATHFINDING

    private List<float[]> currentPath = new ArrayList<>();
    private int currentWaypointIndex = 0;
    private float pathRecalculateTimer = 0f;
    private static final float PATH_RECALCULATE_INTERVAL = 0.5f;
    private static final float WAYPOINT_REACH_DISTANCE = 12f;

    private float pathTargetX = 0f;
    private float pathTargetY = 0f;

    public Enemy(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.lastSeenX = x;
        this.lastSeenY = y;
        this.lastSeenRoomId = null;
        this.bodyBounds = new Rectangle();
        this.footBounds = new Rectangle();
        updateHitboxes();

        searchingState = new EnemySearchingState();
        chasingState = new EnemyChasingState();
        stunnedState = new EnemyStunnedState(1.5f);

        changeState(searchingState);

        loadAnimations();
    }

    private void updateHitboxes() {
        // BODY
        bodyBounds.set(x, y, width, height);
        
        // FOOT
        float footW = width * 0.5f;
        float footH = height * 0.25f;
        float footX = x + (width - footW) / 2f;
        float footY = y;
        
        footBounds.set(footX, footY, footW, footH);
    }

    public Rectangle getBodyBounds() {
        return bodyBounds;
    }

    public Rectangle getFootBounds() {
        return footBounds;
    }

    // ANIMATION
    private void loadAnimations() {
        // CHASING
        chasingUpTexture = new Texture("Sprite/Enemy/josh_chasing_up.png");
        chasingDownTexture = new Texture("Sprite/Enemy/josh_chasing_down.png");
        chasingLeftTexture = new Texture("Sprite/Enemy/josh_chasing_left.png");
        chasingRightTexture = new Texture("Sprite/Enemy/josh_chasing_right.png");

        TextureRegion[][] chasingUpFrames = TextureRegion.split(chasingUpTexture,
                chasingUpTexture.getWidth() / 4, chasingUpTexture.getHeight());
        TextureRegion[][] chasingDownFrames = TextureRegion.split(chasingDownTexture,
                chasingDownTexture.getWidth() / 4, chasingDownTexture.getHeight());
        TextureRegion[][] chasingLeftFrames = TextureRegion.split(chasingLeftTexture,
                chasingLeftTexture.getWidth() / 4, chasingLeftTexture.getHeight());
        TextureRegion[][] chasingRightFrames = TextureRegion.split(chasingRightTexture,
                chasingRightTexture.getWidth() / 4, chasingRightTexture.getHeight());

        chasingUpAnimation = new Animation<>(0.2f, chasingUpFrames[0]);
        chasingUpAnimation.setPlayMode(Animation.PlayMode.LOOP);

        chasingDownAnimation = new Animation<>(0.2f, chasingDownFrames[0]);
        chasingDownAnimation.setPlayMode(Animation.PlayMode.LOOP);

        chasingLeftAnimation = new Animation<>(0.2f, chasingLeftFrames[0]);
        chasingLeftAnimation.setPlayMode(Animation.PlayMode.LOOP);

        chasingRightAnimation = new Animation<>(0.2f, chasingRightFrames[0]);
        chasingRightAnimation.setPlayMode(Animation.PlayMode.LOOP);

        walkUpAnimation = chasingUpAnimation;
        walkDownAnimation = chasingDownAnimation;
        walkLeftAnimation = chasingLeftAnimation;
        walkRightAnimation = chasingRightAnimation;

        // IDLE
        joshIdle1 = new Texture("Sprite/Enemy/josh_idle_1.png");
        joshIdle2 = new Texture("Sprite/Enemy/josh_idle_2.png");

        TextureRegion[] idleFrames = new TextureRegion[2];
        idleFrames[0] = new TextureRegion(joshIdle1);
        idleFrames[1] = new TextureRegion(joshIdle2);
        idleAnimation = new Animation<>(0.5f, idleFrames);
        idleAnimation.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);

        // CAUGHT
        joshCaughtTexture = new Texture("Sprite/Enemy/josh_caught.png");

        // SEARCHING
        loadSearchingAnimations();

        System.out.println("[Enemy] Josh animations loaded - Chasing and Searching sprites");
    }

    private void loadSearchingAnimations() {
        searchingUpTexture = new Texture("Sprite/Enemy/josh_searching_up.png");
        searchingDownTexture = new Texture("Sprite/Enemy/josh_searching_down.png");
        searchingLeftTexture = new Texture("Sprite/Enemy/josh_searching_left.png");
        searchingRightTexture = new Texture("Sprite/Enemy/josh_searching_right.png");

        TextureRegion[][] searchUpFrames = TextureRegion.split(searchingUpTexture,
                searchingUpTexture.getWidth() / 4, searchingUpTexture.getHeight());
        TextureRegion[][] searchDownFrames = TextureRegion.split(searchingDownTexture,
                searchingDownTexture.getWidth() / 4, searchingDownTexture.getHeight());
        TextureRegion[][] searchLeftFrames = TextureRegion.split(searchingLeftTexture,
                searchingLeftTexture.getWidth() / 4, searchingLeftTexture.getHeight());
        TextureRegion[][] searchRightFrames = TextureRegion.split(searchingRightTexture,
                searchingRightTexture.getWidth() / 4, searchingRightTexture.getHeight());

        searchingUpAnimation = new Animation<>(0.3f, searchUpFrames[0]);
        searchingUpAnimation.setPlayMode(Animation.PlayMode.LOOP);

        searchingDownAnimation = new Animation<>(0.3f, searchDownFrames[0]);
        searchingDownAnimation.setPlayMode(Animation.PlayMode.LOOP);

        searchingLeftAnimation = new Animation<>(0.3f, searchLeftFrames[0]);
        searchingLeftAnimation.setPlayMode(Animation.PlayMode.LOOP);

        searchingRightAnimation = new Animation<>(0.3f, searchRightFrames[0]);
        searchingRightAnimation.setPlayMode(Animation.PlayMode.LOOP);
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
        if (isDespawned) {
            return;
        }

        animationTime += delta;

        if (currentState != null) {
            currentState.update(this, player, delta);
        }

        if (currentStateType == EnemyStateType.CHASING) {
            lastSeenX = x;
            lastSeenY = y;
            despawnTimer = 0f;
        }
    }

    // DISABLED
    @Deprecated
    private boolean isAdjacentRoom(RoomId currentRoom, RoomId lastSeen) {
        if (currentRoom == lastSeen)
            return true;
        return currentRoom == lastSeen.up() ||
                currentRoom == lastSeen.down() ||
                currentRoom == lastSeen.left() ||
                currentRoom == lastSeen.right();
    }

    // DISABLED
    @Deprecated
    private void despawn() {
        isDespawned = true;
        despawnTimer = 0f;
        respawnCheckTimer = 0f;
    }

    // DISABLED
    @Deprecated
    private void respawnNearPlayer(Player player) {
    }

    // RENDER
    public void render(SpriteBatch batch) {
        TextureRegion currentFrame;

        if (currentStateType == EnemyStateType.CHASING) {
            Animation<TextureRegion> currentAnimation = getWalkAnimationForDirection();
            currentFrame = currentAnimation.getKeyFrame(animationTime, true);
        } else if (currentStateType == EnemyStateType.STUNNED) {
            currentFrame = new TextureRegion(joshCaughtTexture);
        } else if (currentStateType == EnemyStateType.SEARCHING) {
            Animation<TextureRegion> searchAnimation = getSearchingAnimationForDirection();
            currentFrame = searchAnimation.getKeyFrame(animationTime, true);
        } else {
            currentFrame = idleAnimation.getKeyFrame(animationTime, true);
        }

        batch.draw(currentFrame, x, y, width, height);
    }

    private Animation<TextureRegion> getWalkAnimationForDirection() {
        if (Math.abs(lastDy) > Math.abs(lastDx)) {
            if (lastDy > 0) {
                return walkUpAnimation;
            } else {
                return walkDownAnimation;
            }
        } else {
            if (lastDx > 0) {
                return walkRightAnimation;
            } else if (lastDx < 0) {
                return walkLeftAnimation;
            } else {
                return walkDownAnimation;
            }
        }
    }

    private Animation<TextureRegion> getSearchingAnimationForDirection() {
        if (Math.abs(lastDy) > Math.abs(lastDx)) {
            if (lastDy > 0) {
                return searchingUpAnimation;
            } else {
                return searchingDownAnimation;
            }
        } else {
            if (lastDx > 0) {
                return searchingRightAnimation;
            } else if (lastDx < 0) {
                return searchingLeftAnimation;
            } else {
                return searchingDownAnimation;
            }
        }
    }

    // DISABLED
    @Deprecated
    public void renderShape(ShapeRenderer renderer) {
        Color stateColor = getStateColor();
        renderer.setColor(stateColor);
        renderer.rect(x, y, width, height);

        renderer.setColor(Color.WHITE);
        renderer.rectLine(x, y, x + width, y, 2f);
        renderer.rectLine(x + width, y, x + width, y + height, 2f);
        renderer.rectLine(x + width, y + height, x, y + height, 2f);
        renderer.rectLine(x, y + height, x, y, 2f);
    }

    // DEBUG
    public void renderDebug(ShapeRenderer renderer) {
        renderer.setColor(0f, 1f, 0f, 0.3f);
        renderer.circle(getCenterX(), getCenterY(), DETECTION_RADIUS);

        renderer.setColor(0f, 0f, 1f, 0.2f);
        renderer.circle(getCenterX(), getCenterY(), VISION_RADIUS);

        Color stateColor = getStateColor();
        renderer.setColor(stateColor.r, stateColor.g, stateColor.b, 0.5f);
        renderer.rect(bodyBounds.x, bodyBounds.y, bodyBounds.width, bodyBounds.height);
        
        renderer.setColor(0f, 1f, 0f, 0.8f);
        renderer.rect(footBounds.x, footBounds.y, footBounds.width, footBounds.height);
    }
    
    public void debugRenderHitboxes(ShapeRenderer sr) {
        sr.setColor(Color.RED);
        sr.rect(bodyBounds.x, bodyBounds.y, bodyBounds.width, bodyBounds.height);

        sr.setColor(Color.GREEN);
        sr.rect(footBounds.x, footBounds.y, footBounds.width, footBounds.height);
    }

    private Color getStateColor() {
        switch (currentStateType) {
            case SEARCHING:
                return Color.YELLOW;
            case CHASING:
                return Color.RED;
            case STUNNED:
                return Color.CYAN;
            default:
                return Color.RED;
        }
    }

    // MOVEMENT
    public void move(float dx, float dy) {
        lastDx = dx;
        lastDy = dy;

        float oldX = x;
        float oldY = y;

        x = oldX + dx;
        y = oldY + dy;
        updateHitboxes();

        if (collidesWithFurniture()) {
            x = oldX + dx;
            y = oldY;
            updateHitboxes();
            boolean xBlocked = collidesWithFurniture();

            x = oldX;
            y = oldY + dy;
            updateHitboxes();
            boolean yBlocked = collidesWithFurniture();

            if (!xBlocked && !yBlocked) {
                if (Math.abs(dx) > Math.abs(dy)) {
                    x = oldX + dx;
                    y = oldY;
                } else {
                    x = oldX;
                    y = oldY + dy;
                }
            } else if (!xBlocked) {
                x = oldX + dx;
                y = oldY;
            } else if (!yBlocked) {
                x = oldX;
                y = oldY + dy;
            } else {
                x = oldX;
                y = oldY;
            }
            updateHitboxes();
        }
    }

    private boolean collidesWithFurniture() {
        // TMX collision
        if (tiledMapManager != null && tiledMapManager.hasCurrentMap()) {
            // corners check
            if (!tiledMapManager.isWalkable(footBounds.x, footBounds.y) ||
                    !tiledMapManager.isWalkable(footBounds.x + footBounds.width, footBounds.y) ||
                    !tiledMapManager.isWalkable(footBounds.x, footBounds.y + footBounds.height) ||
                    !tiledMapManager.isWalkable(footBounds.x + footBounds.width, footBounds.y + footBounds.height) ||
                    !tiledMapManager.isWalkable(footBounds.x + footBounds.width / 2,
                            footBounds.y + footBounds.height / 2)) {
                return true;
            }
        }

        return false;
    }

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

    public void setLastSeenRoomId(RoomId roomId) {
        this.lastSeenRoomId = roomId;
    }

    public boolean isDespawned() {
        return isDespawned;
    }

    // PATHFINDING

    public void calculatePathTo(float targetX, float targetY, float worldWidth, float worldHeight) {
        pathTargetX = targetX;
        pathTargetY = targetY;

        List<float[]> rawPath = PathfindingSystem.findPath(
                getCenterX(), getCenterY(),
                targetX, targetY,
                tiledMapManager, worldWidth, worldHeight);

        currentPath = PathfindingSystem.simplifyPath(rawPath);
        currentWaypointIndex = 0;
        pathRecalculateTimer = 0f;

        if (com.fearjosh.frontend.config.Constants.DEBUG_ROOM_DIRECTOR) {
        }
    }

    public boolean followPath(float delta, float worldWidth, float worldHeight) {
        if (currentPath.isEmpty()) {
            return false;
        }

        pathRecalculateTimer += delta;
        if (pathRecalculateTimer >= PATH_RECALCULATE_INTERVAL) {
            calculatePathTo(pathTargetX, pathTargetY, worldWidth, worldHeight);
        }

        if (currentWaypointIndex >= currentPath.size()) {
            return false;
        }

        float[] waypoint = currentPath.get(currentWaypointIndex);
        float waypointX = waypoint[0];
        float waypointY = waypoint[1];

        float dx = waypointX - getCenterX();
        float dy = waypointY - getCenterY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance <= WAYPOINT_REACH_DISTANCE) {
            currentWaypointIndex++;
            return true;
        }

        dx = dx / distance;
        dy = dy / distance;

        float speed = chaseSpeed * delta;
        dx = dx * speed;
        dy = dy * speed;

        move(dx, dy);
        return true;
    }

    public boolean hasPath() {
        return !currentPath.isEmpty() && currentWaypointIndex < currentPath.size();
    }

    public void clearPath() {
        currentPath.clear();
        currentWaypointIndex = 0;
    }

    public float[] getPathTarget() {
        return new float[] { pathTargetX, pathTargetY };
    }

    // DEBUG

    public void renderDebugEnhanced(ShapeRenderer renderer) {
        renderer.setColor(1f, 1f, 0f, 0.25f);
        renderer.circle(getCenterX(), getCenterY(), DETECTION_RADIUS);

        renderer.setColor(1f, 0f, 0f, 0.35f);
        renderer.circle(getCenterX(), getCenterY(), VISION_RADIUS);
        Color stateColor = getStateColor();
        renderer.setColor(stateColor.r, stateColor.g, stateColor.b, 0.5f);
        renderer.rect(x, y, width, height);

        if (!currentPath.isEmpty()) {
            if (currentWaypointIndex < currentPath.size()) {
                float[] waypoint = currentPath.get(currentWaypointIndex);
                renderer.setColor(0f, 1f, 1f, 1f);
                renderer.rectLine(getCenterX(), getCenterY(), waypoint[0], waypoint[1], 2f);
            }

            renderer.setColor(1f, 1f, 1f, 0.8f);
            for (float[] point : currentPath) {
                renderer.circle(point[0], point[1], 3f);
            }

            if (!currentPath.isEmpty()) {
                renderer.setColor(1f, 0f, 1f, 0.7f);
                renderer.rectLine(getCenterX(), getCenterY(), pathTargetX, pathTargetY, 1f);
            }
        }
    }

    public void dispose() {
        if (joshIdle1 != null)
            joshIdle1.dispose();
        if (joshIdle2 != null)
            joshIdle2.dispose();
        if (joshCaughtTexture != null)
            joshCaughtTexture.dispose();

        if (searchingUpTexture != null)
            searchingUpTexture.dispose();
        if (searchingDownTexture != null)
            searchingDownTexture.dispose();
        if (searchingLeftTexture != null)
            searchingLeftTexture.dispose();
        if (searchingRightTexture != null)
            searchingRightTexture.dispose();

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
