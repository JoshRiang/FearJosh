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
import com.fearjosh.frontend.core.RoomDirector;
import com.fearjosh.frontend.input.InputHandler;
import com.fearjosh.frontend.systems.Inventory;
import com.fearjosh.frontend.entity.Item;
import com.fearjosh.frontend.entity.BatteryItem;
import com.fearjosh.frontend.systems.AudioManager;
import com.fearjosh.frontend.systems.Inventory;
import com.fearjosh.frontend.entity.Item;

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

    // ------------ CAPTURE/DEATH SYSTEM ------------
    private boolean playerBeingCaught = false;
    private float captureTimer = 0f;
    private static final float CAPTURE_DELAY = 2.0f; // 2 detik sebelum benar-benar tertangkap
    private boolean playerFullyCaptured = false;
    private Texture captureTransitionTexture;
    private float captureTransitionAlpha = 0f;
    private static final float CAPTURE_TRANSITION_DURATION = 1.5f; // Durasi fade in transition
    private float capturePhaseTimer = 0f; // Timer untuk track fase capture (5 detik)
    private static final float CAPTURE_PHASE_DURATION = 3.0f; // 5 detik untuk overlay

    // ------------ ESCAPE MINIGAME ------------
    private boolean escapeMinigameActive = false;
    private float escapeProgress = 0f; // 0.0 to 1.0 (red to green)
    private float escapeTimer = 0f;
    private static final float ESCAPE_TIME_LIMIT = 10.0f; // 10 detik untuk escape
    private static final float ESCAPE_PROGRESS_PER_PRESS = 0.05f; // 5% per press
    private static final float ESCAPE_PROGRESS_DECAY = 0.03f; // 3% decay per second
    private int escapeSpacebarPresses = 0;

    // ------------ GAME OVER SYSTEM ------------
    private boolean isGameOver = false;
    private float gameOverTimer = 0f;
    private static final float GAME_OVER_DURATION = 3.0f; // 3 detik game over screen

    // Health UI
    private Texture heartTexture;
    private static final float HEART_SIZE = 32f;
    private static final float HEART_SPACING = 8f;

    // Inventory UI (Minecraft-style)
    private Texture inventorySlotTexture;
    private Texture inventorySlotSelectedTexture;
    private static final float SLOT_SIZE = 50f;
    private static final float SLOT_SPACING = 4f;
    private static final float INVENTORY_MARGIN_BOTTOM = 20f;
    private static final int INVENTORY_SLOTS = 7;

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

    // NOTE: Stamina sekarang di-manage di Player + PlayerState
    // (NormalState/SprintingState)
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

    // DEBUG MODE - untuk visual hitbox dan AI debug
    // Set ke true untuk render hitbox visual (RED=body, GREEN=foot)
    // Set debugEnemy ke true untuk render hearing/vision circles + pathfinding
    // Non-final agar tidak menjadi dead code warning saat false
    private static boolean debugHitbox = false;
    private static boolean debugEnemy = false; // AI stalker debug visualization
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
        floorTex1 = new Texture("Map/floor_tiles_1.png");
        floorTex2 = new Texture("Map/floor_tiles_2.png");
        floorTex3 = new Texture("Map/floor_tiles_3.png");

        floorTiles1 = splitFloorTiles(floorTex1);
        floorTiles2 = splitFloorTiles(floorTex2);
        floorTiles3 = splitFloorTiles(floorTex3);

        vignetteTexture = new Texture("General/vignette.png");

        // Load capture transition texture
        captureTransitionTexture = new Texture("UI/josh_caught_you.jpg");

        // Initialize capture state
        playerBeingCaught = false;
        playerFullyCaptured = false;
        captureTimer = 0f;
        captureTransitionAlpha = 0f;
        capturePhaseTimer = 0f;
        escapeMinigameActive = false;
        escapeProgress = 0f;
        escapeTimer = 0f;
        escapeSpacebarPresses = 0;

        // Initialize game over state
        isGameOver = false;
        gameOverTimer = 0f;
        // gameOverTexture = new Texture("UI/game_over.png"); // Optional: load game
        // over image

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
        playerTexture = new Texture("General/white.png");

        // Load health UI
        heartTexture = new Texture("UI/HUD/heart.png");

        // Load inventory slot textures
        inventorySlotTexture = createInventorySlotTexture(false);
        inventorySlotSelectedTexture = createInventorySlotTexture(true);

        // Room via GameManager
        currentRoomId = gm.getCurrentRoomId();
        switchToRoom(currentRoomId);

        // === NEW ENEMY SYSTEM: RoomDirector-based spawning ===
        // Enemy spawn is now handled by RoomDirector (abstract/physical mode)
        // Old direct spawn removed - see RoomDirector for stalking behavior
        // Enemy will be created lazily when RoomDirector signals physical presence
        // Enemy entity will be created on-demand via spawnEnemyPhysically()
        josh = null; // Start with no physical enemy
        // ====================================================

        // Set kamera awal di player
        cameraController.update(worldCamera, worldViewport, player);

        // Debug: Log testing mode status
        System.out.println("[PlayScreen] Constructor - Testing Mode: " + gm.isTestingMode());
        System.out.println("[PlayScreen] Battery initialized: " + battery);
    }

    private void switchToRoom(RoomId id) {
        currentRoomId = id;
        GameManager gm = GameManager.getInstance();
        gm.setCurrentRoomId(id);
        currentRoom = rooms.computeIfAbsent(id,
                rid -> RoomFactory.createRoom(rid, VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        currentInteractable = null;

        // === NOTIFY ROOMDIRECTOR: Player changed room ===
        gm.notifyPlayerRoomChange(id);

        // Remove physical enemy when changing rooms (becomes abstract)
        if (josh != null) {
            RoomDirector rd = gm.getRoomDirector();
            if (rd != null) {
                rd.onEnemyDespawn();
            }
            josh = null; // Will respawn via RoomDirector if needed
        }
        // ===============================================
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

        // If game over, show game over screen and handle timer
        if (isGameOver) {
            gameOverTimer += delta;
            if (gameOverTimer >= GAME_OVER_DURATION) {
                handleGameOverComplete();
            }
            renderGameOverScreen();
            return;
        }

        // If captured and in phase 1 (first 5 seconds): show black screen + overlay
        if (playerFullyCaptured && capturePhaseTimer < CAPTURE_PHASE_DURATION) {
            renderCaptureTransitionPhase();
            return;
        }

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

        // 2) Shapes: lighting, walls, enemy, item (masih pakai ShapeRenderer)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        lightingRenderer.render(shapeRenderer, player, player.isFlashlightOn(), battery / BATTERY_MAX);
        drawWalls();

        // === RENDER ENEMY (JOSH) - KOTAK BERWARNA ===
        // Enemy HARUS di-render di world layer SEBELUM fog-of-war
        // Animated sprite untuk Josh dengan state-based animation
        if (josh != null && !josh.isDespawned()) {
            // Switch to SpriteBatch for sprite rendering
            shapeRenderer.end();
            batch.setProjectionMatrix(worldCamera.combined);
            batch.begin();

            josh.render(batch); // <-- RENDER ENEMY SPRITE

            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            // DEBUG: Enhanced visualization (hearing/vision circles + pathfinding)
            if (debugEnemy) {
                josh.renderDebugEnhanced(shapeRenderer);
            }

            // DEBUG: Verify enemy render (optional - can be removed later)
            // System.out.println("[DEBUG] Enemy rendered at (" + josh.getX() + ", " +
            // josh.getY() + "), state=" + josh.getCurrentStateType());
        }
        // ============================================

        for (Interactable i : currentRoom.getInteractables())
            if (i.isActive())
                i.render(shapeRenderer);

        shapeRenderer.end();

        // 3) Texture: player + prompt 'E'
        // Enemy sprite sudah di-render sebelumnya menggunakan batch
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        // player - use render size (separate from hitbox)
        TextureRegion frame = player.getCurrentFrame(isMoving);
        batch.draw(frame,
                player.getX(),
                player.getY(),
                player.getRenderWidth(),
                player.getRenderHeight());

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

        // Only render HUD and inventory if NOT captured (or still in phase 1)
        if (!playerFullyCaptured || capturePhaseTimer < CAPTURE_PHASE_DURATION) {
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

            // Render health bar (hearts)
            batch.setProjectionMatrix(uiCamera.combined);
            renderHealthBar();

            // Render inventory bar (7 slots, Minecraft-style)
            batch.setProjectionMatrix(uiCamera.combined);
            renderInventory();
        }

        // If captured and after 5 seconds, show hint text
        if (playerFullyCaptured && capturePhaseTimer >= CAPTURE_PHASE_DURATION && !escapeMinigameActive) {
            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            String hintText = "Press F to release yourself";
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font,
                    hintText);
            float textX = (VIRTUAL_WIDTH - layout.width) / 2f;
            float textY = 60f; // Bottom center
            font.draw(batch, hintText, textX, textY);
            batch.end();
        }

        // Render escape minigame UI
        if (escapeMinigameActive) {
            renderEscapeMinigame();
        }

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
                        // Go back to main menu - SAVE PROGRESS first, then return to menu
                        GameManager gm = GameManager.getInstance();
                        gm.saveProgressToSession(); // CRITICAL: Save before leaving
                        gm.setCurrentState(GameManager.GameState.MAIN_MENU);
                        com.fearjosh.frontend.FearJosh app = this.game;
                        app.setScreen(new com.fearjosh.frontend.screen.MainMenuScreen(app));
                        System.out.println("[PlayScreen] Pause -> Main Menu: Progress saved, session remains active");
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
        handleInventoryInput();
        currentRoom.cleanupInactive();

        // === ROOM DIRECTOR SYSTEM: Abstract/Physical enemy control ===
        updateRoomDirector(delta);
        // ==============================================================

        // Kamera update di akhir logic
        cameraController.update(worldCamera, worldViewport, player);
    }

    /**
     * Update RoomDirector and handle enemy spawning
     */
    private void updateRoomDirector(float delta) {
        GameManager gm = GameManager.getInstance();
        RoomDirector rd = gm.getRoomDirector();

        if (rd == null)
            return;

        // Don't update RoomDirector if player is captured (freeze enemy behavior)
        if (!playerFullyCaptured) {
            // Update RoomDirector logic (abstract mode movement, grace period, etc.)
            rd.update(delta);

            // Check if enemy should spawn physically
            if (rd.isEnemyPhysicallyPresent() && josh == null) {
                // Spawn enemy at door position
                spawnEnemyPhysically(rd);
            }
        }

        // Update physical enemy if present (but not if player is captured)
        if (josh != null && !josh.isDespawned() && !playerFullyCaptured) {
            josh.update(player, currentRoom, delta);

            // Check collision with player (CAPTURE SYSTEM)
            if (checkEnemyPlayerCollision(josh, player)) {
                if (!playerBeingCaught && !playerFullyCaptured) {
                    // Mulai capture timer
                    playerBeingCaught = true;
                    captureTimer = 0f;
                    System.out.println("[CAPTURE] Josh menangkap player! Timer dimulai...");
                } else if (playerBeingCaught) {
                    // Update capture timer
                    captureTimer += delta;

                    if (captureTimer >= CAPTURE_DELAY) {
                        // Player benar-benar tertangkap setelah 2 detik
                        triggerPlayerCaptured();
                    }
                }
            } else {
                // Player berhasil lepas sebelum 2 detik
                if (playerBeingCaught && !playerFullyCaptured) {
                    System.out.println("[CAPTURE] Player berhasil lepas dari Josh!");
                    playerBeingCaught = false;
                    captureTimer = 0f;
                }
            }

            // Clamp to room bounds (only if enemy still exists)
            if (josh != null) {
                clampEnemyToRoom(josh);
            }
        }

        // Update capture phase timer and transition
        if (playerFullyCaptured) {
            capturePhaseTimer += delta;

            // Fade in overlay during first 1.5 seconds
            if (captureTransitionAlpha < 1f && capturePhaseTimer < CAPTURE_TRANSITION_DURATION) {
                captureTransitionAlpha += delta / CAPTURE_TRANSITION_DURATION;
                if (captureTransitionAlpha > 1f) {
                    captureTransitionAlpha = 1f;
                }
            }
        }

        // Update escape minigame
        if (escapeMinigameActive) {
            updateEscapeMinigame(delta);
        }
    }

    /**
     * Triggered when player has been caught for 2 seconds
     */
    private void triggerPlayerCaptured() {
        if (playerFullyCaptured)
            return; // Already captured

        playerFullyCaptured = true;
        playerBeingCaught = false;

        System.out.println("[GAME OVER] Josh caught you! Player terikat.");

        // Set player state to captured
        player.setState(com.fearjosh.frontend.state.player.CapturedState.getInstance());
        player.setCaptured(true);

        // Kurangi nyawa
        GameManager gm = GameManager.getInstance();
        gm.loseLife();

        System.out.println("[LIVES] Remaining lives: " + gm.getCurrentLives());

        // Check if game over (no lives left)
        if (gm.getCurrentLives() <= 0) {
            triggerGameOver();
            return; // Don't do retreat if game over
        }

        // Make Josh retreat to another room (give player time to escape)
        if (josh != null) {
            RoomDirector rd = gm.getRoomDirector();
            if (rd != null) {
                rd.onEnemyDespawn();
                rd.forceEnemyRetreat();
            }
            josh = null; // Despawn physical enemy
            System.out.println("[CAPTURE] Josh retreated to another room. Player has time to escape!");
        }
    }

    /**
     * Spawn enemy physically at door based on RoomDirector
     */
    private void spawnEnemyPhysically(RoomDirector rd) {
        // Josh size = 3x player size (menakutkan!)
        float playerW = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_WIDTH;
        float playerH = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_HEIGHT;

        float enemyW = playerW * 2f; // 38 * 3 = 114
        float enemyH = playerH * 2f; // 64 * 3 = 192

        // Get spawn position from RoomDirector (at door, or 30px from player in testing
        // mode)
        float[] pos = rd.getEnemySpawnPosition(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, enemyW, enemyH, player.getX(),
                player.getY());

        josh = new Enemy(pos[0], pos[1], enemyW, enemyH);

        System.out.println("[PlayScreen] ✅ Enemy spawned physically via " +
                rd.getEntryDirection() + " door at (" + pos[0] + ", " + pos[1] + ")");
        System.out.println("[PlayScreen] Enemy size: " + enemyW + "x" + enemyH +
                ", state: " + josh.getCurrentStateType());
    }

    // ======================
    // INPUT & MOVEMENT (via InputHandler + Command Pattern)
    // ======================

    private void handleInput(float delta) {
        // If minigame is active, only handle spacebar
        if (escapeMinigameActive) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                escapeProgress += ESCAPE_PROGRESS_PER_PRESS;
                escapeSpacebarPresses++;
                if (escapeProgress > 1f) {
                    escapeProgress = 1f;
                }
                System.out.println("[MINIGAME] Space pressed! Progress: " + (escapeProgress * 100) + "% ("
                        + escapeSpacebarPresses + " presses)");
            }
            return; // Don't process other input during minigame
        }

        // If player is captured (but minigame not started), only handle F key
        if (playerFullyCaptured && capturePhaseTimer >= CAPTURE_PHASE_DURATION) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                System.out.println("[CAPTURE] Player pressed F! Starting escape minigame...");
                startEscapeMinigame();
            }
            return; // Don't process any other input when captured
        }

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

        // NOTE: Stamina drain/regen sekarang di-handle oleh PlayerState
        // (NormalState/SprintingState)
        // NOTE: Flashlight toggle sekarang di-handle oleh InputHandler ->
        // Player.toggleFlashlight()
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
                enemy.getX(), enemy.getY(), enemy.getWidth(), enemy.getHeight());

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
        if (player.getY() + player.getRenderHeight() >= VIRTUAL_HEIGHT - WALL_THICKNESS) {
            RoomId up = currentRoomId.up();
            if (up != null && cx >= doorMinX && cx <= doorMaxX) {
                switchToRoom(up);
                float newY = WALL_THICKNESS + ENTRY_OFFSET;
                player.setY(newY);
                moved = true;
            } else {
                player.setY(VIRTUAL_HEIGHT - player.getRenderHeight() - WALL_THICKNESS);
            }
        }

        // BAWAH
        if (!moved && player.getY() <= WALL_THICKNESS) {
            RoomId down = currentRoomId.down();
            if (down != null && cx >= doorMinX && cx <= doorMaxX) {
                switchToRoom(down);
                float newY = VIRTUAL_HEIGHT - WALL_THICKNESS - player.getRenderHeight() - ENTRY_OFFSET;
                player.setY(newY);
                moved = true;
            } else {
                player.setY(WALL_THICKNESS);
            }
        }

        // KANAN
        if (!moved && player.getX() + player.getRenderWidth() >= VIRTUAL_WIDTH - WALL_THICKNESS) {
            RoomId right = currentRoomId.right();
            if (right != null && cy >= doorMinY && cy <= doorMaxY) {
                switchToRoom(right);
                float newX = WALL_THICKNESS + ENTRY_OFFSET;
                player.setX(newX);
                moved = true;
            } else {
                player.setX(VIRTUAL_WIDTH - player.getRenderWidth() - WALL_THICKNESS);
            }
        }

        // KIRI
        if (!moved && player.getX() <= WALL_THICKNESS) {
            RoomId left = currentRoomId.left();
            if (left != null && cy >= doorMinY && cy <= doorMaxY) {
                switchToRoom(left);
                float newX = VIRTUAL_WIDTH - WALL_THICKNESS - player.getRenderWidth() - ENTRY_OFFSET;
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
        // Testing mode: unlimited battery
        if (GameManager.getInstance().isTestingMode()) {
            battery = BATTERY_MAX;
            if (player.isFlashlightOn()) {
                player.setFlashlightOn(true); // Keep flashlight on
            }
            return;
        }

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

    /**
     * Recharge battery (used by battery items)
     */
    public void rechargeBattery(float amount) {
        battery = Math.min(BATTERY_MAX, battery + amount);
        System.out.println("[PlayScreen] Battery recharged to: " + battery);
    }

    // ======================
    // INVENTORY INPUT
    // ======================

    /**
     * Handle inventory-related input:
     * - Number keys (1-7) to select slots
     * - Q key to use selected item
     * Disabled when player is captured
     */
    private void handleInventoryInput() {
        // Don't allow inventory input when captured
        if (playerFullyCaptured) {
            return;
        }

        GameManager gm = GameManager.getInstance();
        Inventory inventory = gm.getInventory();

        // Select slot with number keys (1-7)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            inventory.setSelectedSlot(0);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            inventory.setSelectedSlot(1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            inventory.setSelectedSlot(2);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            inventory.setSelectedSlot(3);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
            inventory.setSelectedSlot(4);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {
            inventory.setSelectedSlot(5);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)) {
            inventory.setSelectedSlot(6);
        }

        // Use selected item with Q key
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            Item selectedItem = inventory.getSelectedItem();
            System.out.println("[Inventory] Q pressed - Selected slot: " + inventory.getSelectedSlot() + ", Item: "
                    + (selectedItem != null ? selectedItem.getName() : "null"));

            if (selectedItem != null) {
                if (selectedItem.isUsable()) {
                    boolean success = selectedItem.useItem();
                    System.out.println("[Inventory] Item use result: " + success);
                    if (success) {
                        // Handle specific item effects
                        if (selectedItem instanceof BatteryItem) {
                            BatteryItem batteryItem = (BatteryItem) selectedItem;
                            rechargeBattery(batteryItem.getRechargeAmount());
                            // Remove battery after use (consumed)
                            inventory.removeItem(inventory.getSelectedSlot());
                            System.out.println("[Inventory] Battery consumed and removed");
                        }
                        // Add more item types here as needed
                    }
                } else {
                    System.out.println("[Inventory] " + selectedItem.getName() + " cannot be used");
                }
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

            System.out.println("[Interact] Interacting with: " + currentInteractable.getClass().getSimpleName());

            InteractionResult result = currentInteractable.interact();
            if (result != null) {
                System.out.println("[Interact] Result - BatteryDelta: " + result.getBatteryDelta() + ", HasItem: "
                        + result.hasItem());

                // Legacy: direct battery recharge (for backward compatibility)
                if (result.getBatteryDelta() > 0) {
                    battery = Math.min(BATTERY_MAX, battery + result.getBatteryDelta());
                    System.out.println("[Interact] Direct battery recharge: " + result.getBatteryDelta());
                }

                // NEW: Add item to inventory
                if (result.hasItem()) {
                    GameManager gm = GameManager.getInstance();
                    Inventory inventory = gm.getInventory();
                    System.out.println("[Interact] Attempting to add item: " + result.getItem().getName());
                    boolean added = inventory.addItem(result.getItem());
                    if (added) {
                        System.out.println("[Inventory] Picked up " + result.getItem().getName());
                    } else {
                        System.out.println("[Inventory] Full! Cannot pick up " + result.getItem().getName());
                    }
                }
            } else {
                System.out.println("[Interact] Result is null");
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

    /**
     * Render health bar (hearts) di atas inventory (Minecraft style)
     */
    private void renderHealthBar() {
        GameManager gm = GameManager.getInstance();
        int currentLives = gm.getCurrentLives();
        int maxLives = gm.getMaxLives();

        // Position hearts above inventory bar (center-left)
        // Calculate inventory bar position first
        float totalInventoryWidth = (SLOT_SIZE * INVENTORY_SLOTS) + (SLOT_SPACING * (INVENTORY_SLOTS - 1));
        float inventoryStartX = (VIRTUAL_WIDTH - totalInventoryWidth) / 2f;
        float inventoryY = INVENTORY_MARGIN_BOTTOM;

        // Hearts positioned above inventory, slightly to the left
        float startX = inventoryStartX + 8f; // Small offset from inventory edge
        float startY = inventoryY + SLOT_SIZE + 8f; // 8px gap above inventory

        batch.begin();

        // Draw hearts from left to right
        for (int i = 0; i < maxLives; i++) {
            float x = startX + (i * (HEART_SIZE + HEART_SPACING));
            float y = startY;

            // Only draw if player still has this heart
            if (i < currentLives) {
                batch.draw(heartTexture, x, y, HEART_SIZE, HEART_SIZE);
            } else {
                // Optional: draw empty/gray heart for lost lives
                // For now, just don't draw
            }
        }

        batch.end();
    }

    /**
     * Render inventory bar (7 slots) di bawah tengah screen (Minecraft-style)
     * Hidden when player is captured
     */
    private void renderInventory() {
        // Don't render inventory when captured
        if (playerFullyCaptured) {
            return;
        }

        GameManager gm = GameManager.getInstance();
        Inventory inventory = gm.getInventory();

        // Calculate total width of inventory bar
        float totalWidth = (SLOT_SIZE * INVENTORY_SLOTS) + (SLOT_SPACING * (INVENTORY_SLOTS - 1));
        float startX = (VIRTUAL_WIDTH - totalWidth) / 2f;
        float startY = INVENTORY_MARGIN_BOTTOM;

        batch.begin();

        // Draw slots
        for (int i = 0; i < INVENTORY_SLOTS; i++) {
            float x = startX + (i * (SLOT_SIZE + SLOT_SPACING));
            float y = startY;

            // Draw slot background (selected or normal)
            Texture slotTex = (i == inventory.getSelectedSlot()) ? inventorySlotSelectedTexture : inventorySlotTexture;
            batch.draw(slotTex, x, y, SLOT_SIZE, SLOT_SIZE);

            // Draw item icon if slot has item
            Item item = inventory.getItem(i);
            if (item != null && item.getIcon() != null) {
                // Draw item icon with small padding
                float iconPadding = 4f;
                float iconSize = SLOT_SIZE - (iconPadding * 2);
                batch.draw(item.getIcon(), x + iconPadding, y + iconPadding, iconSize, iconSize);
            }
        }

        batch.end();
    }

    /**
     * Create inventory slot texture (normal or selected)
     */
    private Texture createInventorySlotTexture(boolean selected) {
        int size = 64; // Higher res for cleaner look
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // Background color
        if (selected) {
            pixmap.setColor(0.3f, 0.3f, 0.3f, 0.9f); // Lighter for selected
        } else {
            pixmap.setColor(0.15f, 0.15f, 0.15f, 0.8f); // Darker for normal
        }
        pixmap.fill();

        // Border
        if (selected) {
            pixmap.setColor(1f, 1f, 1f, 1f); // White border when selected
        } else {
            pixmap.setColor(0.4f, 0.4f, 0.4f, 1f); // Gray border for normal
        }

        // Draw border (4 rectangles for each edge)
        int borderWidth = 2;
        pixmap.fillRectangle(0, 0, size, borderWidth); // Top
        pixmap.fillRectangle(0, size - borderWidth, size, borderWidth); // Bottom
        pixmap.fillRectangle(0, 0, borderWidth, size); // Left
        pixmap.fillRectangle(size - borderWidth, 0, borderWidth, size); // Right

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        return texture;
    }

    /**
     * Render capture transition phase (first 5 seconds) - black background +
     * overlay
     */
    private void renderCaptureTransitionPhase() {
        uiCamera.update();
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        batch.setProjectionMatrix(uiCamera.combined);

        // Draw black background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 1f);
        shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        shapeRenderer.end();

        // Draw transition overlay (josh_caught_you.jpg)
        if (captureTransitionAlpha > 0f && captureTransitionTexture != null) {
            batch.begin();
            Color c = batch.getColor();
            batch.setColor(c.r, c.g, c.b, captureTransitionAlpha);
            batch.draw(captureTransitionTexture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            batch.setColor(c.r, c.g, c.b, 1f);
            batch.end();
        }
    }

    /**
     * Render capture transition overlay (josh_caught_you.jpg fade in)
     */
    private void renderCaptureTransition() {
        if (captureTransitionTexture == null)
            return;

        batch.begin();

        // Set alpha for fade in effect
        Color c = batch.getColor();
        batch.setColor(c.r, c.g, c.b, captureTransitionAlpha);

        // Draw fullscreen transition image
        batch.draw(captureTransitionTexture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        // Reset color
        batch.setColor(c.r, c.g, c.b, 1f);

        batch.end();
    }

    /**
     * Render hint box for captured state ("Press F to get free")
     */
    private void renderCaptureHintBox() {
        // Only show after transition is complete
        if (captureTransitionAlpha < 1f)
            return;

        // Draw hint box at bottom center of screen
        float boxWidth = 300f;
        float boxHeight = 60f;
        float boxX = (VIRTUAL_WIDTH - boxWidth) / 2f;
        float boxY = VIRTUAL_HEIGHT / 2f - 100f; // Below center

        // Draw background box
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.8f);
        shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);

        // Draw border
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 1f, 1f, 1f);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1f);

        // Draw text
        batch.begin();
        font.setColor(Color.WHITE);

        // Calculate text centering
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
        layout.setText(font, "Press F to get free");

        float textX = boxX + (boxWidth - layout.width) / 2f;
        float textY = boxY + (boxHeight + layout.height) / 2f;

        font.draw(batch, "Press F to get free", textX, textY);
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

        // Stop menu music when entering gameplay
        AudioManager.getInstance().stopMusic();

        // Reset capture state for new game
        playerBeingCaught = false;
        playerFullyCaptured = false;
        captureTimer = 0f;
        captureTransitionAlpha = 0f;

        System.out.println("[PlayScreen.show()] Capture state reset - playerFullyCaptured: " + playerFullyCaptured);

        // Ensure player is not in captured state
        if (player != null) {
            player.setCaptured(false);
            // Reset to normal state if currently captured
            if (player.getCurrentState() instanceof com.fearjosh.frontend.state.player.CapturedState) {
                player.setState(com.fearjosh.frontend.state.player.NormalState.getInstance());
                System.out.println("[PlayScreen.show()] Player state reset to NormalState");
            }
        }

        // SET STATE ke PLAYING saat screen ditampilkan
        // (Kecuali jika dari Main Menu yang sudah set PLAYING)
        if (!GameManager.getInstance().isPaused()) {
            GameManager.getInstance().setCurrentState(GameManager.GameState.PLAYING);
        }

        System.out.println(
                "[PlayScreen.show()] Screen shown - GameState: " + GameManager.getInstance().getCurrentState());
    }

    @Override
    public void pause() {
        paused = true;
        GameManager.getInstance().setCurrentState(GameManager.GameState.PAUSED);
    }

    @Override
    public void resume() {
        paused = false;
        GameManager.getInstance().setCurrentState(GameManager.GameState.PLAYING);
    }

    @Override
    public void hide() {
        // Clean up resources when screen is hidden
        paused = false;
    }

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

        if (heartTexture != null) {
            heartTexture.dispose();
        }
        if (inventorySlotTexture != null) {
            inventorySlotTexture.dispose();
        }
        if (inventorySlotSelectedTexture != null) {
            inventorySlotSelectedTexture.dispose();
        }
        if (captureTransitionTexture != null) {
            captureTransitionTexture.dispose();
        }
    }

    // ======================
    // ESCAPE MINIGAME
    // ======================

    /**
     * Start the escape minigame (triggered by pressing F when captured)
     */
    private void startEscapeMinigame() {
        escapeMinigameActive = true;
        escapeProgress = 0f;
        escapeTimer = 0f;
        escapeSpacebarPresses = 0;
        System.out.println("[MINIGAME] Escape minigame started! Press SPACE to fill the bar!");
    }

    /**
     * Update escape minigame logic
     */
    private void updateEscapeMinigame(float delta) {
        escapeTimer += delta;

        // Check win condition FIRST (before decay)
        if (escapeProgress >= 1f) {
            System.out.println("[MINIGAME] SUCCESS! Player escaped! Total presses: " + escapeSpacebarPresses);
            escapeSuccessful();
            return;
        }

        // Check lose condition (time ran out)
        if (escapeTimer >= ESCAPE_TIME_LIMIT) {
            System.out.println("[MINIGAME] FAILED! Time ran out. Presses: " + escapeSpacebarPresses);
            escapeFailed();
            return;
        }

        // Progress decay over time (makes it harder)
        escapeProgress -= ESCAPE_PROGRESS_DECAY * delta;
        if (escapeProgress < 0f) {
            escapeProgress = 0f;
        }
    }

    /**
     * Called when player successfully escapes
     */
    private void escapeSuccessful() {
        escapeMinigameActive = false;
        playerFullyCaptured = false;
        capturePhaseTimer = 0f;

        // Reset player state to normal
        player.setState(com.fearjosh.frontend.state.player.NormalState.getInstance());
        player.setCaptured(false);

        System.out.println("[ESCAPE] Player berhasil lepas dari tali!");
    }

    /**
     * Called when player fails to escape (time ran out)
     */
    private void escapeFailed() {
        escapeMinigameActive = false;
        // Player tetap terikat, harus coba lagi
        System.out.println("[ESCAPE] Player gagal lepas! Press F to try again.");
    }

    /**
     * Render escape minigame UI
     */
    private void renderEscapeMinigame() {
        uiCamera.update();
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        batch.setProjectionMatrix(uiCamera.combined);

        // Bar dimensions
        float barWidth = 400f;
        float barHeight = 40f;
        float barX = (VIRTUAL_WIDTH - barWidth) / 2f;
        float barY = VIRTUAL_HEIGHT / 2f;

        // Draw bar background (dark gray)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);

        // Draw bar fill (red to green gradient based on progress)
        float fillWidth = barWidth * escapeProgress;
        float red = 1f - escapeProgress;
        float green = escapeProgress;
        shapeRenderer.setColor(red, green, 0f, 1f);
        shapeRenderer.rect(barX, barY, fillWidth, barHeight);

        // Draw bar outline
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 1f, 1f, 1f);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);
        shapeRenderer.end();

        // Draw text instructions and timer
        batch.begin();
        String instruction = "PRESS SPACE TO ESCAPE!";
        com.badlogic.gdx.graphics.g2d.GlyphLayout instructionLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(
                font, instruction);
        font.draw(batch, instruction, (VIRTUAL_WIDTH - instructionLayout.width) / 2f, barY + barHeight + 40f);

        // Timer
        float timeRemaining = ESCAPE_TIME_LIMIT - escapeTimer;
        String timerText = String.format("Time: %.1fs", timeRemaining);
        com.badlogic.gdx.graphics.g2d.GlyphLayout timerLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font,
                timerText);
        font.draw(batch, timerText, (VIRTUAL_WIDTH - timerLayout.width) / 2f, barY - 20f);

        // Progress percentage
        String progressText = String.format("Progress: %d%%", (int) (escapeProgress * 100));
        com.badlogic.gdx.graphics.g2d.GlyphLayout progressLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font,
                progressText);
        font.draw(batch, progressText, (VIRTUAL_WIDTH - progressLayout.width) / 2f, barY + barHeight / 2f + 5f);

        batch.end();
    }

    // ======================
    // GAME OVER SYSTEM
    // ======================

    /**
     * Trigger game over (called when lives reach 0)
     */
    private void triggerGameOver() {
        isGameOver = true;
        gameOverTimer = 0f;

        System.out.println("[GAME OVER] All lives lost! Game Over triggered.");

        // Change game state to prevent resume/pause functionality
        GameManager gm = GameManager.getInstance();
        gm.setCurrentState(GameManager.GameState.GAME_OVER);

        // Stop all gameplay
        playerFullyCaptured = false; // Exit capture state
        escapeMinigameActive = false;
        paused = false; // Exit pause state

        // Despawn enemy completely
        if (josh != null) {
            RoomDirector rd = gm.getRoomDirector();
            if (rd != null) {
                rd.onEnemyDespawn();
            }
            josh = null;
            System.out.println("[GAME OVER] Enemy despawned.");
        }
    }

    /**
     * Called when game over screen finishes (after 3 seconds)
     */
    private void handleGameOverComplete() {
        System.out.println("[GAME OVER] Returning to main menu...");

        // End the current session (disable Resume button)
        GameManager gm = GameManager.getInstance();
        if (gm.getCurrentSession() != null) {
            gm.getCurrentSession().setActive(false);
            System.out.println("[GAME OVER] Session ended - Resume button will be disabled");
        }

        // Return to main menu (LibGDX will automatically call dispose() on this screen)
        game.setScreen(new com.fearjosh.frontend.screen.MainMenuScreen(game));
    }

    /**
     * Render game over screen
     */
    private void renderGameOverScreen() {
        uiCamera.update();
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        batch.setProjectionMatrix(uiCamera.combined);

        // Draw dark red background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0f, 0f, 1f); // Dark red
        shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        shapeRenderer.end();

        // Draw game over text
        batch.begin();

        // Main "GAME OVER" text
        String gameOverText = "GAME OVER";
        com.badlogic.gdx.graphics.g2d.GlyphLayout gameOverLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font,
                gameOverText);
        float gameOverX = (VIRTUAL_WIDTH - gameOverLayout.width) / 2f;
        float gameOverY = VIRTUAL_HEIGHT / 2f + 50f;
        font.draw(batch, gameOverText, gameOverX, gameOverY);

        // Subtitle text
        String subtitleText = "Josh got you...";
        com.badlogic.gdx.graphics.g2d.GlyphLayout subtitleLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font,
                subtitleText);
        float subtitleX = (VIRTUAL_WIDTH - subtitleLayout.width) / 2f;
        float subtitleY = VIRTUAL_HEIGHT / 2f;
        font.draw(batch, subtitleText, subtitleX, subtitleY);

        // Timer countdown text
        float timeRemaining = GAME_OVER_DURATION - gameOverTimer;
        String timerText = String.format("Returning to menu in %.0fs...", timeRemaining);
        com.badlogic.gdx.graphics.g2d.GlyphLayout timerLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font,
                timerText);
        float timerX = (VIRTUAL_WIDTH - timerLayout.width) / 2f;
        float timerY = VIRTUAL_HEIGHT / 2f - 50f;
        font.draw(batch, timerText, timerX, timerY);

        batch.end();
    }

}
