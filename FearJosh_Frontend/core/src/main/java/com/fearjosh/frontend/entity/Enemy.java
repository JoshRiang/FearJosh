package com.fearjosh.frontend.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.fearjosh.frontend.state.enemy.*;
import com.fearjosh.frontend.world.Room;
import com.fearjosh.frontend.world.RoomId;
import com.fearjosh.frontend.world.objects.Table;
import com.fearjosh.frontend.world.objects.Locker;

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

    // Despawn/Respawn mechanics
    private float lastSeenX, lastSeenY;
    private RoomId lastSeenRoomId; // track room terakhir kali Josh terlihat
    private float despawnTimer = 0f;
    private static final float DESPAWN_DELAY = 3f;
    private boolean isDespawned = false;
    private float respawnCheckTimer = 0f;
    private static final float RESPAWN_CHECK_INTERVAL = 2f;
    
    // [PLANNED] Min distance from player when respawning - will be used in respawn logic
    public static final float RESPAWN_DISTANCE = 200f;

    // Detection radii untuk debug visual
    public static final float DETECTION_RADIUS = 220f;  // hearing range (hijau)
    public static final float VISION_RADIUS = 350f;     // vision range (biru)

    public Enemy(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width  = width;
        this.height = height;
        this.lastSeenX = x;
        this.lastSeenY = y;
        this.lastSeenRoomId = null; // akan di-set saat update pertama

        searchingState = new EnemySearchingState();
        chasingState   = new EnemyChasingState();
        stunnedState   = new EnemyStunnedState(1.5f);

        changeState(searchingState);
    }

    public void changeState(EnemyState newState) {
        if (currentState == newState) return;
        if (currentState != null) currentState.onExit(this);
        currentState = newState;
        if (currentState != null) currentState.onEnter(this);
    }

    public void update(Player player,
                       Room room,
                       float delta) {
        // Set last seen room jika null
        if (lastSeenRoomId == null) {
            lastSeenRoomId = room.getId();
        }

        // Jika sudah despawned, cek apakah perlu respawn
        if (isDespawned) {
            respawnCheckTimer += delta;
            if (respawnCheckTimer >= RESPAWN_CHECK_INTERVAL) {
                respawnCheckTimer = 0f;
                // Respawn hanya jika player masuk ruangan yang adjacent dengan last seen
                if (isAdjacentRoom(room.getId(), lastSeenRoomId)) {
                    respawnNearPlayer(player);
                }
            }
            return;
        }

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
        } else if (currentStateType == EnemyStateType.SEARCHING) {
            despawnTimer += delta;
            if (despawnTimer >= DESPAWN_DELAY) {
                despawn();
            }
        }
    }

    // Check apakah currentRoom adjacent dengan lastSeenRoom
    private boolean isAdjacentRoom(RoomId currentRoom, RoomId lastSeen) {
        if (currentRoom == lastSeen) return true; // same room
        
        // Check 4 adjacent directions
        return currentRoom == lastSeen.up() ||
               currentRoom == lastSeen.down() ||
               currentRoom == lastSeen.left() ||
               currentRoom == lastSeen.right();
    }

    private void despawn() {
        isDespawned = true;
        despawnTimer = 0f;
        respawnCheckTimer = 0f;
    }

    private void respawnNearPlayer(Player player) {
        // Spawn di sekitar player dengan offset random
        float offsetDist = 100f;
        double angle = Math.random() * 2 * Math.PI;
        float offsetX = (float)(Math.cos(angle) * offsetDist);
        float offsetY = (float)(Math.sin(angle) * offsetDist);

        x = player.getCenterX() + offsetX - width / 2f;
        y = player.getCenterY() + offsetY - height / 2f;
        
        isDespawned = false;
        changeState(searchingState);
    }

    // Render dengan visual debug indicators
    public void render(ShapeRenderer renderer) {
        if (isDespawned) return;
        
        // Draw detection circles (hijau = hearing, biru = vision)
        renderer.setColor(0f, 1f, 0f, 0.3f); // hijau semi-transparent
        renderer.circle(getCenterX(), getCenterY(), DETECTION_RADIUS);
        
        renderer.setColor(0f, 0f, 1f, 0.2f); // biru semi-transparent
        renderer.circle(getCenterX(), getCenterY(), VISION_RADIUS);

        // Draw Josh dengan warna berdasarkan state
        Color stateColor = getStateColor();
        renderer.setColor(stateColor);
        renderer.rect(x, y, width, height);
    }

    private Color getStateColor() {
        switch (currentStateType) {
            case SEARCHING:
                return Color.YELLOW; // kuning
            case CHASING:
                return Color.RED;    // merah
            case STUNNED:
                return Color.WHITE;  // putih
            default:
                return Color.RED;
        }
    }

    public void render(SpriteBatch batch) {
        // [PLANNED] Render Josh sprite texture here when assets are ready
        // Currently using ShapeRenderer in PlayScreen for placeholder rendering
    }

    // Movement dengan collision detection
    public void move(float dx, float dy, Room room) {
        float newX = x + dx;
        float newY = y + dy;

        float oldX = x;
        float oldY = y;
        x = newX;
        y = newY;

        if (collidesWithFurniture(room)) {
            x = oldX + dx;
            y = oldY;
            if (collidesWithFurniture(room)) {
                x = oldX;
            } else {
                oldX = x;
            }

            x = oldX;
            y = oldY + dy;
            if (collidesWithFurniture(room)) {
                y = oldY;
            }
        }
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

    public float getCenterX() { return x + width / 2f; }
    public float getCenterY() { return y + height / 2f; }

    public float getChaseSpeed() { return chaseSpeed; }

    public EnemyState getSearchingState() { return searchingState; }
    public EnemyState getChasingState()   { return chasingState; }
    public EnemyStunnedState getStunnedState() { return stunnedState; }

    public void setCurrentStateType(EnemyStateType type) {
        this.currentStateType = type;
    }

    public EnemyStateType getCurrentStateType() {
        return currentStateType;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public void setX(float newX) { this.x = newX; }
    public void setY(float newY) { this.y = newY; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }

    public float getLastSeenX() { return lastSeenX; }
    public float getLastSeenY() { return lastSeenY; }
    public RoomId getLastSeenRoomId() { return lastSeenRoomId; }

    public boolean isDespawned() { return isDespawned; }
}
