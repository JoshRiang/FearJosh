package com.fearjosh.frontend.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.factory.RoomFactory;
import com.fearjosh.frontend.world.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PlayScreen implements Screen {

    // Room transition
    private float transitionCooldown = 0f;
    private static final float TRANSITION_COOLDOWN_DURATION = 0.2f; // detik

    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;

    private static final float DOOR_WIDTH = 80f;
    private static final float DOOR_THICKNESS = 20f;
    private static final float WALL_THICKNESS = 6f;

    private static final float INTERACT_RANGE = 60f;

    private final FearJosh game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;

    private Player player;
    private Texture playerTexture;

    // Rooms
    private final Map<RoomId, Room> rooms = new EnumMap<>(RoomId.class);
    private RoomId currentRoomId = RoomId.R5;
    private Room currentRoom;

    // Movement & stamina
    private static final float WALK_SPEED = 150f;
    private static final float RUN_SPEED = 260f;
    private static final float STAMINA_MAX = 1f;
    private static final float STAMINA_DRAIN_RATE = 0.4f;   // per detik saat sprint
    private static final float STAMINA_REGEN_RATE = 0.25f;  // per detik saat diam

    private float stamina = STAMINA_MAX;

    // Flashlight / battery
    private static final float BATTERY_MAX = 1f;
    private static final float BATTERY_DRAIN_RATE = 0.08f; // per detik saat nyala
    private float battery = BATTERY_MAX;
    private boolean flashlightOn = false;

    // Interaksi
    private Interactable currentInteractable = null;

    public PlayScreen(FearJosh game) {
        this.game = game;
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        // Load sprite player
        playerTexture = new Texture("jonatan.png");
        float scale = 0.4f;
        float pw = playerTexture.getWidth() * scale;
        float ph = playerTexture.getHeight() * scale;

        // Player di tengah ruangan
        player = new Player(
            VIRTUAL_WIDTH / 2f - pw / 2f,
            VIRTUAL_HEIGHT / 2f - ph / 2f,
            pw,
            ph
        );

        switchToRoom(RoomId.R5);
    }

    private void switchToRoom(RoomId id) {
        currentRoomId = id;
        currentRoom = rooms.computeIfAbsent(id,
            rid -> RoomFactory.createRoom(rid, VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        currentInteractable = null;
    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Lighting
        drawLighting();

        // Walls
        drawWalls();

        // Tables & lockers
        for (Table table : currentRoom.getTables()) {
            table.render(shapeRenderer);
        }
        for (Locker locker : currentRoom.getLockers()) {
            locker.render(shapeRenderer);
        }

        // Interactable items (baterai)
        for (Interactable inter : currentRoom.getInteractables()) {
            if (inter.isActive()) {
                inter.render(shapeRenderer);
            }
        }

        // UI bar
        drawUIBars();

        shapeRenderer.end();

        // Sprite + E prompt
        batch.begin();

        // Player sprite
        batch.draw(
            playerTexture,
            player.getX(),
            player.getY(),
            player.getWidth(),
            player.getHeight()
        );

        if (currentInteractable != null && currentInteractable.isActive()) {
            float tx = currentInteractable.getCenterX();
            float ty = currentInteractable.getCenterY() + 30f;
            font.draw(batch, "E", tx - 4f, ty);
        }

        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void update(float delta) {
        // kurangi cooldown dulu
        if (transitionCooldown > 0f) {
            transitionCooldown -= delta;
            if (transitionCooldown < 0f) transitionCooldown = 0f;
        }

        handleInput(delta);

        if (transitionCooldown <= 0f) {
            checkRoomTransition();
        }

        updateBattery(delta);
        findCurrentInteractable();
        handleInteractInput();
        currentRoom.cleanupInactive();
    }


    private void handleInput(float delta) {
        float dx = 0f;
        float dy = 0f;

        boolean up = Gdx.input.isKeyPressed(Input.Keys.W);
        boolean down = Gdx.input.isKeyPressed(Input.Keys.S);
        boolean left = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D);

        if (up) dy += 1f;
        if (down) dy -= 1f;
        if (left) dx -= 1f;
        if (right) dx += 1f;

        boolean isMoving = (dx != 0 || dy != 0);

        // Toggle flashlight dengan F
        if (Gdx.input.isKeyJustPressed(Input.Keys.F) && battery > 0f) {
            flashlightOn = !flashlightOn;
        }

        // Normalisasi arah gerak
        if (isMoving) {
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len != 0) {
                dx /= len;
                dy /= len;
            }
        }

        // Sprint dengan Shift + butuh stamina
        boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ||
            Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        float speed = WALK_SPEED;
        boolean sprinting = false;

        if (shift && stamina > 0f && isMoving) {
            speed = RUN_SPEED;
            sprinting = true;
        }

        // Gerakkan player
        if (isMoving) {
            player.move(dx * speed * delta, dy * speed * delta);
        }

        // Update stamina
        if (sprinting) {
            stamina -= STAMINA_DRAIN_RATE * delta;
            if (stamina < 0f) stamina = 0f;
        } else if (!isMoving) {
            stamina += STAMINA_REGEN_RATE * delta;
            if (stamina > STAMINA_MAX) stamina = STAMINA_MAX;
        }
    }

    private void checkRoomTransition() {
        float centerX = player.getCenterX();
        float centerY = player.getCenterY();

        float doorMinX = VIRTUAL_WIDTH / 2f - DOOR_WIDTH / 2f;
        float doorMaxX = VIRTUAL_WIDTH / 2f + DOOR_WIDTH / 2f;
        float doorMinY = VIRTUAL_HEIGHT / 2f - DOOR_WIDTH / 2f;
        float doorMaxY = VIRTUAL_HEIGHT / 2f + DOOR_WIDTH / 2f;

        transitionCooldown = TRANSITION_COOLDOWN_DURATION;

        boolean moved = false;

        // TOP
        if (player.getY() + player.getHeight() >= VIRTUAL_HEIGHT - WALL_THICKNESS) {
            RoomId up = currentRoomId.up();
            if (up != null && centerX >= doorMinX && centerX <= doorMaxX) {
                switchToRoom(up);
                player.setY(1f);
                moved = true;
                transitionCooldown = TRANSITION_COOLDOWN_DURATION; // ← TAMBAH INI
            } else {
                player.setY(VIRTUAL_HEIGHT - player.getHeight() - WALL_THICKNESS);
            }
        }


        // BOTTOM
        if (!moved && player.getY() <= WALL_THICKNESS) {
            RoomId down = currentRoomId.down();
            if (down != null && centerX >= doorMinX && centerX <= doorMaxX) {
                switchToRoom(down);
                player.setY(VIRTUAL_HEIGHT - player.getHeight() - 1f);
                moved = true;
                transitionCooldown = TRANSITION_COOLDOWN_DURATION; // ←
            } else {
                player.setY(WALL_THICKNESS);
            }
        }

        // RIGHT
        if (!moved && player.getX() + player.getWidth() >= VIRTUAL_WIDTH - WALL_THICKNESS) {
            RoomId right = currentRoomId.right();
            if (right != null && centerY >= doorMinY && centerY <= doorMaxY) {
                switchToRoom(right);
                player.setX(1f);
                moved = true;
                transitionCooldown = TRANSITION_COOLDOWN_DURATION; // ←
            } else {
                player.setX(VIRTUAL_WIDTH - player.getWidth() - WALL_THICKNESS);
            }
        }

        // LEFT
        if (!moved && player.getX() <= WALL_THICKNESS) {
            RoomId left = currentRoomId.left();
            if (left != null && centerY >= doorMinY && centerY <= doorMaxY) {
                switchToRoom(left);
                player.setX(VIRTUAL_WIDTH - player.getWidth() - 1f);
                moved = true;
                transitionCooldown = TRANSITION_COOLDOWN_DURATION; // ←
            } else {
                player.setX(WALL_THICKNESS);
            }
        }
    }

    private void updateBattery(float delta) {
        if (flashlightOn && battery > 0f) {
            battery -= BATTERY_DRAIN_RATE * delta;
            if (battery <= 0f) {
                battery = 0f;
                flashlightOn = false;
            }
        }
    }

    private void findCurrentInteractable() {
        currentInteractable = null;
        float bestDist2 = Float.MAX_VALUE;

        float px = player.getCenterX();
        float py = player.getCenterY();

        // Items
        List<Interactable> interactables = currentRoom.getInteractables();
        for (Interactable inter : interactables) {
            if (!inter.isActive()) continue;
            if (!inter.canInteract(player)) continue;

            float dx = inter.getCenterX() - px;
            float dy = inter.getCenterY() - py;
            float dist2 = dx * dx + dy * dy;
            if (dist2 < bestDist2 && dist2 <= INTERACT_RANGE * INTERACT_RANGE) {
                bestDist2 = dist2;
                currentInteractable = inter;
            }
        }

        // Lockers (juga interactable)
        for (Locker locker : currentRoom.getLockers()) {
            if (!locker.isActive()) continue;
            if (!locker.canInteract(player)) continue;

            float dx = locker.getCenterX() - px;
            float dy = locker.getCenterY() - py;
            float dist2 = dx * dx + dy * dy;
            if (dist2 < bestDist2 && dist2 <= INTERACT_RANGE * INTERACT_RANGE) {
                bestDist2 = dist2;
                currentInteractable = locker;
            }
        }
    }

    private void handleInteractInput() {
        if (currentInteractable != null &&
            currentInteractable.isActive() &&
            Gdx.input.isKeyJustPressed(Input.Keys.E)) {

            InteractionResult result = currentInteractable.interact();
            if (result != null) {
                float deltaBattery = result.getBatteryDelta();
                if (deltaBattery != 0f) {
                    battery = Math.min(BATTERY_MAX, battery + deltaBattery);
                }
            }
        }
    }

    private void drawLighting() {
        float cx = player.getCenterX();
        float cy = player.getCenterY();

        // Lingkaran vision default: abu-abu
        Color softGray = new Color(0.3f, 0.3f, 0.3f, 1f);
        shapeRenderer.setColor(softGray);
        float baseRadius = 80f;
        shapeRenderer.circle(cx, cy, baseRadius);

        // Flashlight cone kuning
        if (flashlightOn && battery > 0f) {
            Color coneColor = new Color(1f, 1f, 0.6f, 0.85f);
            shapeRenderer.setColor(coneColor);

            float coneLength = 200f;
            float coneHalfWidth = 60f;

            float x1 = cx;
            float y1 = cy;

            float x2 = cx;
            float y2 = cy;
            float x3 = cx;
            float y3 = cy;

            switch (player.getDirection()) {
                case UP:
                    x2 = cx - coneHalfWidth;
                    y2 = cy + coneLength;
                    x3 = cx + coneHalfWidth;
                    y3 = cy + coneLength;
                    break;
                case DOWN:
                    x2 = cx - coneHalfWidth;
                    y2 = cy - coneLength;
                    x3 = cx + coneHalfWidth;
                    y3 = cy - coneLength;
                    break;
                case LEFT:
                    x2 = cx - coneLength;
                    y2 = cy - coneHalfWidth;
                    x3 = cx - coneLength;
                    y3 = cy + coneHalfWidth;
                    break;
                case RIGHT:
                    x2 = cx + coneLength;
                    y2 = cy - coneHalfWidth;
                    x3 = cx + coneLength;
                    y3 = cy + coneHalfWidth;
                    break;
            }

            shapeRenderer.triangle(x1, y1, x2, y2, x3, y3);
        }
    }

    private void drawWalls() {
        shapeRenderer.setColor(0.4f, 0.4f, 0.4f, 1f);

        float doorMinX = VIRTUAL_WIDTH / 2f - DOOR_WIDTH / 2f;
        float doorMaxX = VIRTUAL_WIDTH / 2f + DOOR_WIDTH / 2f;
        float doorMinY = VIRTUAL_HEIGHT / 2f - DOOR_WIDTH / 2f;
        float doorMaxY = VIRTUAL_HEIGHT / 2f + DOOR_WIDTH / 2f;

        // TOP
        if (currentRoomId.hasUp()) {
            // kiri segmen
            shapeRenderer.rect(0f, VIRTUAL_HEIGHT - WALL_THICKNESS,
                doorMinX, WALL_THICKNESS);
            // kanan segmen
            shapeRenderer.rect(doorMaxX, VIRTUAL_HEIGHT - WALL_THICKNESS,
                VIRTUAL_WIDTH - doorMaxX, WALL_THICKNESS);
        } else {
            shapeRenderer.rect(0f, VIRTUAL_HEIGHT - WALL_THICKNESS,
                VIRTUAL_WIDTH, WALL_THICKNESS);
        }

        // BOTTOM
        if (currentRoomId.hasDown()) {
            shapeRenderer.rect(0f, 0f,
                doorMinX, WALL_THICKNESS);
            shapeRenderer.rect(doorMaxX, 0f,
                VIRTUAL_WIDTH - doorMaxX, WALL_THICKNESS);
        } else {
            shapeRenderer.rect(0f, 0f,
                VIRTUAL_WIDTH, WALL_THICKNESS);
        }

        // LEFT
        if (currentRoomId.hasLeft()) {
            shapeRenderer.rect(0f, 0f,
                WALL_THICKNESS, doorMinY);
            shapeRenderer.rect(0f, doorMaxY,
                WALL_THICKNESS, VIRTUAL_HEIGHT - doorMaxY);
        } else {
            shapeRenderer.rect(0f, 0f,
                WALL_THICKNESS, VIRTUAL_HEIGHT);
        }

        // RIGHT
        if (currentRoomId.hasRight()) {
            shapeRenderer.rect(VIRTUAL_WIDTH - WALL_THICKNESS, 0f,
                WALL_THICKNESS, doorMinY);
            shapeRenderer.rect(VIRTUAL_WIDTH - WALL_THICKNESS, doorMaxY,
                WALL_THICKNESS, VIRTUAL_HEIGHT - doorMaxY);
        } else {
            shapeRenderer.rect(VIRTUAL_WIDTH - WALL_THICKNESS, 0f,
                WALL_THICKNESS, VIRTUAL_HEIGHT);
        }
    }

    private void drawUIBars() {
        float margin = 10f;
        float barHeight = 10f;

        // Flashlight bar 4 segmen kuning
        float segmentWidth = 30f;
        float gap = 4f;
        float totalWidth = 4 * segmentWidth + 3 * gap;

        float startX = margin;
        float startY = VIRTUAL_HEIGHT - margin - barHeight;

        int activeSegments = (int) Math.ceil(battery * 4f);
        if (activeSegments < 0) activeSegments = 0;
        if (activeSegments > 4) activeSegments = 4;

        for (int i = 0; i < 4; i++) {
            float x = startX + i * (segmentWidth + gap);
            if (i < activeSegments) {
                shapeRenderer.setColor(Color.YELLOW);
            } else {
                shapeRenderer.setColor(0.3f, 0.3f, 0.1f, 1f);
            }
            shapeRenderer.rect(x, startY, segmentWidth, barHeight);
        }

        // Stamina bar biru di bawahnya
        float staminaMaxWidth = totalWidth;
        float staminaX = margin;
        float staminaY = startY - barHeight - 6f;

        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);
        shapeRenderer.rect(staminaX, staminaY, staminaMaxWidth, barHeight);

        float staminaWidth = staminaMaxWidth * stamina / STAMINA_MAX;
        shapeRenderer.setColor(Color.CYAN);
        shapeRenderer.rect(staminaX, staminaY, staminaWidth, barHeight);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
    }

    @Override
    public void show() { }

    @Override
    public void hide() { }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
        playerTexture.dispose();
    }
}
