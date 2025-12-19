package com.fearjosh.frontend.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.camera.CameraController;
import com.fearjosh.frontend.entity.Enemy;
import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.factory.RoomFactory;
import com.fearjosh.frontend.render.HudRenderer;
import com.fearjosh.frontend.render.LightingRenderer;
import com.fearjosh.frontend.world.*;
import com.fearjosh.frontend.world.objects.Table;
import com.fearjosh.frontend.world.objects.Locker;
import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.input.InputHandler;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class PlayScreen implements Screen {

    // Transition antar room
    private float transitionCooldown = 0f;
    private static final float TRANSITION_COOLDOWN_DURATION = 0.2f;

    // Resolusi virtual (view camera)
    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;

    // Zoom camera ( <1 = zoom in )
    private static final float CAMERA_ZOOM = 0.7f;

    private static final float DOOR_WIDTH = 80f;
    private static final float WALL_THICKNESS = 6f;
    private static final float ENTRY_OFFSET = 20f;

    private static final float INTERACT_RANGE = 60f;

    private final FearJosh game;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;

    private Player player;
    private Enemy josh;
    private Texture playerTexture; // hanya dipakai untuk ukuran awal
    // overlay ambience gelap (vignette)
    private Texture vignetteTexture;

    // Fog of War system
    private FrameBuffer darknessFrameBuffer;
    private Texture lightTexture; // White circle for punch-through
    // Vision radius and fog darkness now driven by DifficultyStrategy

    // Rooms
    private final Map<RoomId, Room> rooms = new EnumMap<>(RoomId.class);
    private RoomId currentRoomId = RoomId.R5;
    private Room currentRoom;

    // Movement speed constants
    private static final float WALK_SPEED = 150f;
    
    // NOTE: Stamina sekarang di-manage di Player + PlayerState (NormalState/SprintingState)
    // Constants berikut hanya untuk reference/documentation:
    // - STAMINA_MAX = 100f (di Player)
    // - Drain rate = 25f/sec (di SprintingState)
    // - Regen rate = 15f/sec (di NormalState)

    // Flashlight / battery
    private static final float BATTERY_MAX = 1f;
    private static final float BATTERY_DRAIN_RATE = 0.08f;
    private float battery = BATTERY_MAX;
    // NOTE: flashlightOn state sekarang di Player

    // Interaction
    private Interactable currentInteractable = null;

    // Flag movement (untuk animasi)
    private boolean isMoving = false;

    // Cameras
    private OrthographicCamera worldCamera;
    private Viewport worldViewport;
    private OrthographicCamera uiCamera;

    // Sistem terpisah
    private CameraController cameraController;
    private LightingRenderer lightingRenderer;
    private HudRenderer hudRenderer;
    private InputHandler inputHandler;
    
    // DEBUG MODE - untuk visual hitbox
    // Set ke true untuk render hitbox visual (RED=body, GREEN=foot)
    // Non-final agar tidak menjadi dead code warning saat false
    private static boolean debugHitbox = false;
    private boolean paused = false;
    private com.badlogic.gdx.math.Rectangle resumeButtonBounds = new com.badlogic.gdx.math.Rectangle();

    // ------------ FLOOR TILES ------------
    // ukuran world untuk 1 tile lantai (lebih kecil -> lebih rapat)
    private static final float FLOOR_TILE_SIZE = 32f;

    private final int FLOOR_COLS;
    private final int FLOOR_ROWS;

    private Texture floorTex1;
    private Texture floorTex2;
    private Texture floorTex3;

    // masing-masing tileset berisi 16 tile kecil (4x4)
    private TextureRegion[] floorTiles1;
    private TextureRegion[] floorTiles2;
    private TextureRegion[] floorTiles3;

    // tiap room punya grid sendiri (FLOOR_ROWS x FLOOR_COLS)
    private final Map<RoomId, TextureRegion[][]> roomFloors = new EnumMap<>(RoomId.class);

    private final Random rng = new Random();

    public PlayScreen(FearJosh game) {
        this.game = game;

        // Hitung jumlah kolom/baris floor berdasarkan ukuran tile
        FLOOR_COLS = (int) Math.ceil(VIRTUAL_WIDTH / FLOOR_TILE_SIZE);
        FLOOR_ROWS = (int) Math.ceil(VIRTUAL_HEIGHT / FLOOR_TILE_SIZE);

        // Renderer
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        // Cameras
        worldCamera = new OrthographicCamera();
        worldViewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, worldCamera);
        worldCamera.zoom = CAMERA_ZOOM;

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        // Sistem eksternal
        cameraController = new CameraController(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        lightingRenderer = new LightingRenderer();
        hudRenderer = new HudRenderer();
        inputHandler = new InputHandler();

        // --------- LOAD FLOOR TEXTURES ---------
        // Pastikan file-file ini ada di assets/
        floorTex1 = new Texture("floor_tiles_1.png");
        floorTex2 = new Texture("floor_tiles_2.png");
        floorTex3 = new Texture("floor_tiles_3.png");

        floorTiles1 = splitFloorTiles(floorTex1);
        floorTiles2 = splitFloorTiles(floorTex2);
        floorTiles3 = splitFloorTiles(floorTex3);

        vignetteTexture = new Texture("vignette.png");

        // Initialize FrameBuffer for darkness layer
        darknessFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, (int) VIRTUAL_WIDTH, (int) VIRTUAL_HEIGHT, false);

        // Create light texture for vision circle
        lightTexture = createLightTexture(512);

        // Initialize game manager (singleton)
        GameManager gm = GameManager.getInstance();
        gm.initIfNeeded(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        // Use singleton player
        player = gm.getPlayer();

        // Texture sementara hanya untuk ukuran awal player (placeholder)
        playerTexture = new Texture("white.png");

        // Room via GameManager
        currentRoomId = gm.getCurrentRoomId();
        switchToRoom(currentRoomId);

        // === NEW: spawn Josh (Enemy) - CONDITIONAL BASED ON TESTING MODE ===
        if (!gm.isTestingMode()) {
            float enemyW = 24f;
            float enemyH = 36f;
            josh = new Enemy(
                    player.getX() + 120f,
                    player.getY() + 60f,
                    enemyW,
                    enemyH
            );
        }
        // ====================================================================

        // Set kamera awal di player
        cameraController.update(worldCamera, worldViewport, player);
    }

    private void switchToRoom(RoomId id) {
        currentRoomId = id;
        GameManager.getInstance().setCurrentRoomId(id);
        currentRoom = rooms.computeIfAbsent(id,
                rid -> RoomFactory.createRoom(rid, VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        currentInteractable = null;

        // === NEW: Handle Josh saat room transition - CONDITIONAL ===
        if (!GameManager.getInstance().isTestingMode()) {
            if (josh != null && josh.getCurrentStateType() == com.fearjosh.frontend.state.enemy.EnemyStateType.CHASING) {
                // Josh akan despawn dan respawn dekat player nanti
            }
        }
        // ============================================================
    }

    // ======================
    // GAME LOOP
    // ======================

    @Override
    public void render(float delta) {
        // STATE CHECK: Hanya update jika PLAYING (tidak paused)
        if (!paused && GameManager.getInstance().isPlaying()) {
            update(delta);
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // ---------- WORLD RENDER ----------
        worldViewport.apply();
        worldCamera.update();
        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        batch.setProjectionMatrix(worldCamera.combined);

        // 1) Gambar lantai + meja + locker (pakai batch)
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        drawFloor(); // lantai tile

        for (Table t : currentRoom.getTables())
            t.render(batch); // <-- sekarang pakai sprite meja

        for (Locker l : currentRoom.getLockers())
            l.render(batch); // <-- sekarang pakai sprite locker

        batch.end();

        // 2) Shapes: lighting, walls, item (masih pakai ShapeRenderer)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        lightingRenderer.render(shapeRenderer, player, player.isFlashlightOn(), battery / BATTERY_MAX);
        drawWalls();

        for (Interactable i : currentRoom.getInteractables())
            if (i.isActive())
                i.render(shapeRenderer);

        // === NEW: render Josh (sementara kotak merah pakai ShapeRenderer) - CONDITIONAL ===
        if (!GameManager.getInstance().isTestingMode()) {
            if (josh != null) {
                josh.render(shapeRenderer);
            }
        }
        // ===================================================================================

        shapeRenderer.end();

        // 3) Texture: player + prompt 'E'
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        // player
        TextureRegion frame = player.getCurrentFrame(isMoving);
        batch.draw(frame,
                player.getX(),
                player.getY(),
                player.getWidth(),
                player.getHeight());

        // prompt E
        if (currentInteractable != null && currentInteractable.isActive()) {
            font.draw(batch,
                    "E",
                    currentInteractable.getCenterX() - 4,
                    currentInteractable.getCenterY() + 30);
        }

        batch.end();

        // 4) Fog of War - Render to darkness buffer, then composite
        renderDarknessLayer();

        // ---------- HUD RENDER (kamera UI) ----------
        uiCamera.update();
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        batch.setProjectionMatrix(uiCamera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        hudRenderer.render(shapeRenderer,
                VIRTUAL_WIDTH,
                VIRTUAL_HEIGHT,
                player.getStamina(),
                player.getMaxStamina(),
                battery,
                BATTERY_MAX);
        
        // DEBUG: Render hitboxes jika debugHitbox aktif
        if (debugHitbox && GameManager.getInstance().isPlaying()) {
            player.debugRenderHitboxes(shapeRenderer);
        }
        
        shapeRenderer.end();

        // Draw HUD text (difficulty)
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        hudRenderer.renderText(batch, font, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        batch.end();

        // Handle pause button click in UI space - HANYA jika state PLAYING
        if (GameManager.getInstance().isPlaying() && Gdx.input.justTouched()) {
            float screenX = Gdx.input.getX();
            float screenY = Gdx.input.getY();
            // Convert to UI world coords
            com.badlogic.gdx.math.Vector3 uiCoords = uiCamera
                    .unproject(new com.badlogic.gdx.math.Vector3(screenX, screenY, 0));
            com.badlogic.gdx.math.Rectangle r = hudRenderer.getPauseButtonBounds();
            if (r.contains(uiCoords.x, uiCoords.y)) {
                paused = !paused;
                // Update state
                if (paused) {
                    GameManager.getInstance().setCurrentState(GameManager.GameState.PAUSED);
                } else {
                    GameManager.getInstance().setCurrentState(GameManager.GameState.PLAYING);
                }
            }
        }

        // If paused, draw overlay with Resume button and handle inputs
        if (paused) {
            // Draw dim backdrop and panel
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 0.35f);
            shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            shapeRenderer.setColor(0.12f, 0.12f, 0.15f, 0.9f);
            float panelW = 360f, panelH = 140f;
            float panelX = (VIRTUAL_WIDTH - panelW) / 2f;
            float panelY = (VIRTUAL_HEIGHT - panelH) / 2f;
            shapeRenderer.rect(panelX, panelY, panelW, panelH);

            // Resume button inside panel
            float btnW = 160f, btnH = 36f;
            float btnX = panelX + (panelW - btnW) / 2f;
            float btnY = panelY + 18f;
            resumeButtonBounds.set(btnX, btnY, btnW, btnH);
            shapeRenderer.setColor(0.18f, 0.18f, 0.22f, 1f);
            shapeRenderer.rect(btnX, btnY, btnW, btnH);
            // Main Menu button
            float menuBtnW = 160f, menuBtnH = 36f;
            float menuBtnX = panelX + (panelW - menuBtnW) / 2f;
            float menuBtnY = btnY + btnH + 10f;
            shapeRenderer.setColor(0.18f, 0.18f, 0.22f, 1f);
            shapeRenderer.rect(menuBtnX, menuBtnY, menuBtnW, menuBtnH);
            shapeRenderer.end();

            // Text
            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            font.draw(batch, "Paused", VIRTUAL_WIDTH / 2f - 36f, panelY + panelH - 18f);
            font.draw(batch, "Resume", btnX + 52f, btnY + 24f);
            font.draw(batch, "Main Menu", menuBtnX + 40f, menuBtnY + 24f);
            batch.end();

            // Keyboard shortcuts to resume - HANYA jika state PAUSED
            if (GameManager.getInstance().isPaused()) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                    paused = false;
                    GameManager.getInstance().setCurrentState(GameManager.GameState.PLAYING);
                }
            }

            // Mouse click on Resume or Main Menu - HANYA jika state PAUSED
            if (GameManager.getInstance().isPaused() && Gdx.input.justTouched()) {
                float sx = Gdx.input.getX();
                float sy = Gdx.input.getY();
                com.badlogic.gdx.math.Vector3 ui = uiCamera.unproject(new com.badlogic.gdx.math.Vector3(sx, sy, 0));
                
                if (resumeButtonBounds.contains(ui.x, ui.y)) {
                    paused = false;
                    GameManager.getInstance().setCurrentState(GameManager.GameState.PLAYING);
                } else {
                    // Test main menu button area
                    float testW = 160f, testH = 36f;
                    float testX = panelX + (panelW - testW) / 2f;
                    float testY = btnY + btnH + 10f;
                    if (ui.x >= testX && ui.x <= testX + testW && ui.y >= testY && ui.y <= testY + testH) {
                        // Go back to main menu - UBAH STATE ke MAIN_MENU
                        GameManager.getInstance().setCurrentState(GameManager.GameState.MAIN_MENU);
                        com.fearjosh.frontend.FearJosh app = this.game;
                        app.setScreen(new com.fearjosh.frontend.screen.MainMenuScreen(app));
                    }
                }
            }
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void update(float delta) {

        if (transitionCooldown > 0) {
            transitionCooldown -= delta;
            if (transitionCooldown < 0)
                transitionCooldown = 0;
        }

        handleInput(delta);
        player.update(delta, isMoving);

        if (transitionCooldown <= 0) {
            checkRoomTransition();
        }

        updateBattery(delta);
        findCurrentInteractable();
        handleInteractInput();
        currentRoom.cleanupInactive();

        // === NEW: update musuh (Josh) dengan collision handling - CONDITIONAL ===
        if (!GameManager.getInstance().isTestingMode()) {
            if (josh != null) {
                josh.update(player, currentRoom, delta);

                // Check apakah Josh menabrak player (GAME OVER)
                if (!josh.isDespawned() && checkEnemyPlayerCollision(josh, player)) {
                    // [PLANNED] Trigger game over / death sequence - redirect to GameOverScreen
                    System.out.println("GAME OVER: Josh caught you!");
                }

                // Clamp Josh ke dalam ruangan (wall collision)
                clampEnemyToRoom(josh);
            }
        }
        // ==========================================================================

        // Kamera update di akhir logic
        cameraController.update(worldCamera, worldViewport, player);
    }

    // ======================
    // INPUT & MOVEMENT (via InputHandler + Command Pattern)
    // ======================

    private void handleInput(float delta) {
        // 1. Poll input dan execute commands via InputHandler
        inputHandler.update(player, delta);
        
        // 2. Get movement direction dari InputHandler
        float dx = inputHandler.getMoveDirX();
        float dy = inputHandler.getMoveDirY();
        isMoving = inputHandler.isMoving();
        
        // 3. Calculate speed berdasarkan Player state (Normal/Sprinting)
        float baseSpeed = WALK_SPEED;
        float speed = baseSpeed * player.getSpeedMultiplier() * delta;
        
        // 4. Apply movement dengan collision resolution
        if (isMoving) {
            float mx = dx * speed;
            float my = dy * speed;
            float oldX = player.getX();
            float oldY = player.getY();

            // Move dan update facing direction
            player.move(mx, my);

            // Collision resolution per-axis (slide along edges)
            if (collidesWithFurniture(player)) {
                // Test X-only
                player.setX(oldX + mx);
                player.setY(oldY);
                boolean collideX = collidesWithFurniture(player);

                // Test Y-only
                player.setX(oldX);
                player.setY(oldY + my);
                boolean collideY = collidesWithFurniture(player);

                // Apply results
                if (!collideX) {
                    player.setX(oldX + mx);
                } else {
                    player.setX(oldX);
                }

                if (!collideY) {
                    player.setY(oldY + my);
                } else {
                    player.setY(oldY);
                }
            }
        }
        
        // NOTE: Stamina drain/regen sekarang di-handle oleh PlayerState (NormalState/SprintingState)
        // NOTE: Flashlight toggle sekarang di-handle oleh InputHandler -> Player.toggleFlashlight()
    }

    // ======================
    // FURNITURE COLLISIONS
    // ======================

    /**
     * COLLISION FURNITURE: Pakai FOOT HITBOX player
     * Player hanya collision dengan kaki, bukan full body
     */
    private boolean collidesWithFurniture(Player p) {
        // GUNAKAN FOOT HITBOX (bukan body bounds!)
        com.badlogic.gdx.math.Rectangle footBounds = p.getFootBounds();

        // Check tables - full rectangle furniture
        for (Table t : currentRoom.getTables()) {
            if (footBounds.overlaps(t.getCollisionBounds()))
                return true;
        }
        // Check lockers - full rectangle furniture
        for (Locker l : currentRoom.getLockers()) {
            if (footBounds.overlaps(l.getCollisionBounds()))
                return true;
        }
        return false;
    }

    // ========================
    // ENEMY COLLISION HELPERS
    // ========================

    /**
     * COLLISION ENEMY: Pakai BODY HITBOX player (full body)
     * Enemy menangkap player jika kena bagian mana pun dari body
     */
    private boolean checkEnemyPlayerCollision(Enemy enemy, Player player) {
        // GUNAKAN BODY HITBOX (full body, bukan foot!)
        com.badlogic.gdx.math.Rectangle playerBody = player.getBodyBounds();
        com.badlogic.gdx.math.Rectangle enemyBounds = new com.badlogic.gdx.math.Rectangle(
            enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight()
        );
        
        return playerBody.overlaps(enemyBounds);
    }

    private void clampEnemyToRoom(Enemy enemy) {
        float ex = enemy.getX();
        float ey = enemy.getY();
        float ew = enemy.getWidth();
        float eh = enemy.getHeight();

        // Clamp ke dalam ruangan (dengan wall thickness)
        if (ex < WALL_THICKNESS) {
            enemy.setX(WALL_THICKNESS);
        }
        if (ex + ew > VIRTUAL_WIDTH - WALL_THICKNESS) {
            enemy.setX(VIRTUAL_WIDTH - ew - WALL_THICKNESS);
        }
        if (ey < WALL_THICKNESS) {
            enemy.setY(WALL_THICKNESS);
        }
        if (ey + eh > VIRTUAL_HEIGHT - WALL_THICKNESS) {
            enemy.setY(VIRTUAL_HEIGHT - eh - WALL_THICKNESS);
        }
    }

    private void checkRoomTransition() {

        float cx = player.getCenterX();
        float cy = player.getCenterY();

        float doorMinX = VIRTUAL_WIDTH / 2f - DOOR_WIDTH / 2f;
        float doorMaxX = VIRTUAL_WIDTH / 2f + DOOR_WIDTH / 2f;
        float doorMinY = VIRTUAL_HEIGHT / 2f - DOOR_WIDTH / 2f;
        float doorMaxY = VIRTUAL_HEIGHT / 2f + DOOR_WIDTH / 2f;

        boolean moved = false;

        // ATAS
        if (player.getY() + player.getHeight() >= VIRTUAL_HEIGHT - WALL_THICKNESS) {
            RoomId up = currentRoomId.up();
            if (up != null && cx >= doorMinX && cx <= doorMaxX) {
                switchToRoom(up);
                float newY = WALL_THICKNESS + ENTRY_OFFSET;
                player.setY(newY);
                moved = true;
            } else {
                player.setY(VIRTUAL_HEIGHT - player.getHeight() - WALL_THICKNESS);
            }
        }

        // BAWAH
        if (!moved && player.getY() <= WALL_THICKNESS) {
            RoomId down = currentRoomId.down();
            if (down != null && cx >= doorMinX && cx <= doorMaxX) {
                switchToRoom(down);
                float newY = VIRTUAL_HEIGHT - WALL_THICKNESS - player.getHeight() - ENTRY_OFFSET;
                player.setY(newY);
                moved = true;
            } else {
                player.setY(WALL_THICKNESS);
            }
        }

        // KANAN
        if (!moved && player.getX() + player.getWidth() >= VIRTUAL_WIDTH - WALL_THICKNESS) {
            RoomId right = currentRoomId.right();
            if (right != null && cy >= doorMinY && cy <= doorMaxY) {
                switchToRoom(right);
                float newX = WALL_THICKNESS + ENTRY_OFFSET;
                player.setX(newX);
                moved = true;
            } else {
                player.setX(VIRTUAL_WIDTH - player.getWidth() - WALL_THICKNESS);
            }
        }

        // KIRI
        if (!moved && player.getX() <= WALL_THICKNESS) {
            RoomId left = currentRoomId.left();
            if (left != null && cy >= doorMinY && cy <= doorMaxY) {
                switchToRoom(left);
                float newX = VIRTUAL_WIDTH - WALL_THICKNESS - player.getWidth() - ENTRY_OFFSET;
                player.setX(newX);
                moved = true;
            } else {
                player.setX(WALL_THICKNESS);
            }
        }

        if (moved) {
            transitionCooldown = TRANSITION_COOLDOWN_DURATION;
        }
    }

    private void updateBattery(float delta) {
        if (player.isFlashlightOn() && battery > 0) {
            com.fearjosh.frontend.difficulty.DifficultyStrategy ds = com.fearjosh.frontend.core.GameManager
                    .getInstance().getDifficultyStrategy();
            battery -= BATTERY_DRAIN_RATE * ds.batteryDrainMultiplier() * delta;
            if (battery <= 0) {
                battery = 0;
                player.setFlashlightOn(false);
            }
        }
    }

    // ======================
    // INTERACTION
    // ======================

    private void findCurrentInteractable() {
        currentInteractable = null;
        float bestDist2 = Float.MAX_VALUE;

        float px = player.getCenterX();
        float py = player.getCenterY();

        for (Interactable inter : currentRoom.getInteractables()) {
            if (!inter.isActive() || !inter.canInteract(player))
                continue;

            float dx = inter.getCenterX() - px;
            float dy = inter.getCenterY() - py;
            float dist2 = dx * dx + dy * dy;

            if (dist2 < bestDist2 && dist2 <= INTERACT_RANGE * INTERACT_RANGE) {
                bestDist2 = dist2;
                currentInteractable = inter;
            }
        }

        for (Locker locker : currentRoom.getLockers()) {
            if (!locker.isActive() || !locker.canInteract(player))
                continue;

            float dx = locker.getCenterX() - px;
            float dy = locker.getCenterY() - py;
            float dist2 = dx * dx + dy * dy;

            if (dist2 < bestDist2) {
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
                battery = Math.min(BATTERY_MAX, battery + result.getBatteryDelta());
            }
        }
    }

    // ======================
    // WALLS
    // ======================

    private void drawWalls() {
        shapeRenderer.setColor(0.4f, 0.4f, 0.4f, 1);

        float doorMinX = VIRTUAL_WIDTH / 2f - DOOR_WIDTH / 2f;
        float doorMaxX = VIRTUAL_WIDTH / 2f + DOOR_WIDTH / 2f;
        float doorMinY = VIRTUAL_HEIGHT / 2f - DOOR_WIDTH / 2f;
        float doorMaxY = VIRTUAL_HEIGHT / 2f + DOOR_WIDTH / 2f;

        // Top
        if (currentRoomId.hasUp()) {
            shapeRenderer.rect(0, VIRTUAL_HEIGHT - WALL_THICKNESS,
                    doorMinX, WALL_THICKNESS);
            shapeRenderer.rect(doorMaxX, VIRTUAL_HEIGHT - WALL_THICKNESS,
                    VIRTUAL_WIDTH - doorMaxX, WALL_THICKNESS);
        } else {
            shapeRenderer.rect(0, VIRTUAL_HEIGHT - WALL_THICKNESS,
                    VIRTUAL_WIDTH, WALL_THICKNESS);
        }

        // Bottom
        if (currentRoomId.hasDown()) {
            shapeRenderer.rect(0, 0, doorMinX, WALL_THICKNESS);
            shapeRenderer.rect(doorMaxX, 0,
                    VIRTUAL_WIDTH - doorMaxX, WALL_THICKNESS);
        } else {
            shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, WALL_THICKNESS);
        }

        // Left
        if (currentRoomId.hasLeft()) {
            shapeRenderer.rect(0, 0, WALL_THICKNESS, doorMinY);
            shapeRenderer.rect(0, doorMaxY,
                    WALL_THICKNESS, VIRTUAL_HEIGHT - doorMaxY);
        } else {
            shapeRenderer.rect(0, 0, WALL_THICKNESS, VIRTUAL_HEIGHT);
        }

        // Right
        if (currentRoomId.hasRight()) {
            shapeRenderer.rect(VIRTUAL_WIDTH - WALL_THICKNESS, 0,
                    WALL_THICKNESS, doorMinY);
            shapeRenderer.rect(VIRTUAL_WIDTH - WALL_THICKNESS, doorMaxY,
                    WALL_THICKNESS, VIRTUAL_HEIGHT - doorMaxY);
        } else {
            shapeRenderer.rect(VIRTUAL_WIDTH - WALL_THICKNESS, 0,
                    WALL_THICKNESS, VIRTUAL_HEIGHT);
        }
    }

    // ======================
    // FLOOR HELPERS
    // ======================

    // Memecah 1 texture 4x4 jadi array 16 tile
    private TextureRegion[] splitFloorTiles(Texture tex) {
        int COLS = 4;
        int ROWS = 4;

        TextureRegion[][] tmp = TextureRegion.split(
                tex,
                tex.getWidth() / COLS,
                tex.getHeight() / ROWS);

        TextureRegion[] flat = new TextureRegion[COLS * ROWS];
        int idx = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                flat[idx++] = tmp[r][c];
            }
        }
        return flat;
    }

    // Ambil / generate grid lantai untuk room saat ini
    private TextureRegion[][] getFloorForCurrentRoom() {
        TextureRegion[][] grid = roomFloors.get(currentRoomId);
        if (grid == null) {
            grid = generateFloorForRoom(currentRoomId);
            roomFloors.put(currentRoomId, grid);
        }
        return grid;
    }

    private TextureRegion[][] generateFloorForRoom(RoomId id) {
        TextureRegion[] pool;

        // pilih salah satu tileset sebagai "tema" room ini
        int choice = rng.nextInt(3);
        switch (choice) {
            default:
            case 0:
                pool = floorTiles1;
                break;
            case 1:
                pool = floorTiles2;
                break;
            case 2:
                pool = floorTiles3;
                break;
        }

        TextureRegion[][] grid = new TextureRegion[FLOOR_ROWS][FLOOR_COLS];

        // supaya pattern konsisten tiap kali balik ke room yang sama, seed based on
        // RoomId
        long seed = id.ordinal() * 99991L + 12345L;
        Random r = new Random(seed);

        for (int row = 0; row < FLOOR_ROWS; row++) {
            for (int col = 0; col < FLOOR_COLS; col++) {
                grid[row][col] = pool[r.nextInt(pool.length)];
            }
        }
        return grid;
    }

    private void drawFloor() {
        TextureRegion[][] grid = getFloorForCurrentRoom();

        float tileW = FLOOR_TILE_SIZE;
        float tileH = FLOOR_TILE_SIZE;

        // gelapkan semua lantai → ambience ruangannya gelap
        Color oldColor = batch.getColor();
        batch.setColor(0.35f, 0.35f, 0.35f, 1f);

        for (int row = 0; row < FLOOR_ROWS; row++) {
            for (int col = 0; col < FLOOR_COLS; col++) {
                TextureRegion region = grid[row][col];
                if (region == null)
                    continue;

                float x = col * tileW;
                float y = row * tileH;
                batch.draw(region, x, y, tileW, tileH);
            }
        }

        batch.setColor(oldColor);
    }

    // ======================
    // FOG OF WAR
    // ======================

    private void renderDarknessLayer() {
        com.fearjosh.frontend.difficulty.DifficultyStrategy ds = com.fearjosh.frontend.core.GameManager.getInstance()
                .getDifficultyStrategy();
        float visionRadius = ds.visionRadius();
        float fogDarkness = ds.fogDarkness();
        // Step 1: Begin FrameBuffer
        darknessFrameBuffer.begin();

        // Step 2: Clear Background with dark color
        Gdx.gl.glClearColor(0f, 0f, 0f, fogDarkness);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Step 3: Use world camera projection directly
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        // Step 4: Punch the Hole with gradient - Set erase blend function
        batch.setBlendFunction(GL20.GL_ZERO, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float cx = player.getCenterX();
        float cy = player.getCenterY();

        // Draw player vision circle (always visible)
        float baseSize = visionRadius * 2f;

        // Outer soft layer (reduced size)
        batch.setColor(1f, 1f, 1f, 0.3f);
        float outerSize = baseSize * 1.2f;
        batch.draw(lightTexture, cx - outerSize / 2f, cy - outerSize / 2f, outerSize, outerSize);

        // Middle layer
        batch.setColor(1f, 1f, 1f, 0.6f);
        float midSize = baseSize * 1.1f;
        batch.draw(lightTexture, cx - midSize / 2f, cy - midSize / 2f, midSize, midSize);

        // Inner bright layer (full erase)
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(lightTexture, cx - baseSize / 2f, cy - baseSize / 2f, baseSize, baseSize);

        // Flashlight cone - adds extended vision when active
        if (player.isFlashlightOn() && battery > 0f) {
            float batteryFrac = battery / BATTERY_MAX;

            // Flashlight parameters based on battery
            float minConeLength = 140f;
            float maxConeLength = 280f;
            float minConeWidth = 40f;
            float maxConeWidth = 80f;

            float coneLength = minConeLength + (maxConeLength - minConeLength) * batteryFrac;
            float coneWidth = minConeWidth + (maxConeWidth - minConeWidth) * batteryFrac;

            // Calculate cone tip position based on player direction
            float tipX = cx, tipY = cy;

            switch (player.getDirection()) {
                case UP:
                    tipY = cy + coneLength;
                    break;
                case DOWN:
                    tipY = cy - coneLength;
                    break;
                case LEFT:
                    tipX = cx - coneLength;
                    break;
                case RIGHT:
                    tipX = cx + coneLength;
                    break;
            }

            // Draw circles along the cone path to create elongated light
            int steps = 12; // More steps for smoother gradient
            for (int i = 0; i <= steps; i++) {
                float t = (float) i / steps;
                float lx = cx + (tipX - cx) * t;
                float ly = cy + (tipY - cy) * t;

                // Size increases towards the tip
                float circleSize = (visionRadius + coneWidth * t) * 2f;

                // Alpha decreases more gradually for smoother, more transparent effect
                float alpha = 0.5f * (1f - t * 0.7f); // Starts at 0.5, fades to ~0.15
                batch.setColor(1f, 1f, 0.85f, alpha); // Subtle yellowish tint

                batch.draw(lightTexture, lx - circleSize / 2f, ly - circleSize / 2f, circleSize, circleSize);
            }
        }

        // Step 5: Reset Blending
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        batch.end();

        // Step 6: End FrameBuffer
        darknessFrameBuffer.end();

        // Step 7: Draw FrameBuffer texture as UI overlay
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f);

        batch.draw(darknessFrameBuffer.getColorBufferTexture(), 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, 0, 0, 1, 1);

        batch.end();
    }

    private Texture createLightTexture(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f); // White color

        int centerX = size / 2;
        int centerY = size / 2;
        int radius = size / 2;

        // Draw filled white circle
        pixmap.fillCircle(centerX, centerY, radius);

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        return texture;
    }

    // ======================
    // LIFECYCLE
    // ======================

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height);
        uiCamera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
    }

    @Override
    public void show() {
        // CRITICAL: Disable UI menu input - set InputProcessor to null
        // Ini mencegah tombol menu (termasuk Quit) diklik saat game berjalan
        com.badlogic.gdx.Gdx.input.setInputProcessor(null);
        
        // SET STATE ke PLAYING saat screen ditampilkan
        // (Kecuali jika dari Main Menu yang sudah set PLAYING)
        if (!GameManager.getInstance().isPaused()) {
            GameManager.getInstance().setCurrentState(GameManager.GameState.PLAYING);
        }
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
        playerTexture.dispose();

        floorTex1.dispose();
        floorTex2.dispose();
        floorTex3.dispose();

        vignetteTexture.dispose();
        Table.disposeTexture();
        lightTexture.dispose();
        darknessFrameBuffer.dispose();
    }

}

