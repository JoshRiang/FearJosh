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
import com.fearjosh.frontend.render.TiledMapManager;
import com.fearjosh.frontend.world.*;
import com.fearjosh.frontend.world.RoomId.DoorPosition;
import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.core.RoomDirector;
import com.fearjosh.frontend.input.InputHandler;
import com.fearjosh.frontend.systems.Inventory;
import com.fearjosh.frontend.entity.Item;
import com.fearjosh.frontend.entity.BatteryItem;
import com.fearjosh.frontend.systems.AudioManager;

import java.util.EnumMap;
import java.util.Map;

public class PlayScreen implements Screen {

    // Transition antar room
    private float transitionCooldown = 0f;
    private static final float TRANSITION_COOLDOWN_DURATION = 0.2f;

    // Resolusi virtual (view camera)
    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;

    // Zoom camera ( <1 = zoom in )
    private static final float CAMERA_ZOOM = 0.7f;

    private static final float ROOM_BORDER = 5f;     // Collision boundary at room edges (fallback)
    private static final float ENTRY_OFFSET = 20f;   // Offset for door entry positioning

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

    // Health UI
    private Texture heartTexture;
    private static final float HEART_SIZE = 32f;
    private static final float HEART_SPACING = 8f;
    private static final float HEART_MARGIN_RIGHT = 20f;
    private static final float HEART_MARGIN_TOP = 40f;

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
    private RoomId currentRoomId = RoomId.getStartingRoom();
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
    private com.fearjosh.frontend.render.TiledMapManager.TileInteractable currentTileInteractable = null;

    // Floating message above player (e.g., "It's locked...")
    private String floatingMessage = null;
    private float floatingMessageTimer = 0f;
    private static final float FLOATING_MESSAGE_DURATION = 2f;

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
    private TiledMapManager tiledMapManager;

    // DEBUG MODE - untuk visual hitbox dan AI debug
    // Set ke true untuk render hitbox visual (RED=body, GREEN=foot)
    // Set debugEnemy ke true untuk render hearing/vision circles + pathfinding
    // Non-final agar tidak menjadi dead code warning saat false
    private static boolean debugHitbox = false;
    private static boolean debugEnemy = false; // AI stalker debug visualization
    private boolean paused = false;
    private com.badlogic.gdx.math.Rectangle resumeButtonBounds = new com.badlogic.gdx.math.Rectangle();


    
    // ======================
    // ROOM DIMENSION HELPERS
    // ======================
    
    /**
     * Get the width of the current room
     */
    private float getRoomWidth() {
        return currentRoomId != null ? currentRoomId.getWidth() : VIRTUAL_WIDTH;
    }
    
    /**
     * Get the height of the current room
     */
    private float getRoomHeight() {
        return currentRoomId != null ? currentRoomId.getHeight() : VIRTUAL_HEIGHT;
    }

    public PlayScreen(FearJosh game) {
        this.game = game;

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
        tiledMapManager = new TiledMapManager();

        vignetteTexture = new Texture("General/vignette.png");

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
        
        // Position player at first spawn point if available
        if (tiledMapManager.hasCurrentMap()) {
            com.fearjosh.frontend.render.TiledMapManager.SpawnInfo firstSpawn = tiledMapManager.getFirstSpawnPoint();
            if (firstSpawn != null) {
                player.setX(firstSpawn.x - player.getRenderWidth() / 2f);
                player.setY(firstSpawn.y - player.getRenderHeight() / 2f);
                System.out.println("[PlayScreen] Positioned player at first spawn: (" + firstSpawn.x + ", " + firstSpawn.y + ")");
            }
        }

        // === NEW ENEMY SYSTEM: RoomDirector-based spawning ===
        // Enemy spawn is now handled by RoomDirector (abstract/physical mode)
        // Old direct spawn removed - see RoomDirector for stalking behavior
        // Enemy will be created lazily when RoomDirector signals physical presence
        if (!gm.isTestingMode()) {
            // Enemy entity will be created on-demand via spawnEnemyPhysically()
            josh = null; // Start with no physical enemy
        }
        // ====================================================

        // Set kamera awal di player
        cameraController.update(worldCamera, worldViewport, player);
    }

    private void switchToRoom(RoomId id) {
        currentRoomId = id;
        GameManager gm = GameManager.getInstance();
        gm.setCurrentRoomId(id);
        
        // Get room-specific dimensions
        float roomWidth = id.getWidth();
        float roomHeight = id.getHeight();
        
        // Keep viewport at fixed virtual size - camera will handle showing room bounds
        // This prevents stretching caused by changing aspect ratios
        // worldViewport stays at VIRTUAL_WIDTH x VIRTUAL_HEIGHT
        
        // Update camera controller bounds for the new room size
        cameraController.setWorldBounds(roomWidth, roomHeight);
        
        // Load TMX map for this room (if available)
        tiledMapManager.loadMapForRoom(id);
        
        // Create room with custom dimensions
        currentRoom = rooms.computeIfAbsent(id,
                rid -> RoomFactory.createRoom(rid, rid.getWidth(), rid.getHeight()));
        currentInteractable = null;

        // === NOTIFY ROOMDIRECTOR: Player changed room ===
        if (!gm.isTestingMode()) {
            gm.notifyPlayerRoomChange(id);

            // Remove physical enemy when changing rooms (becomes abstract)
            if (josh != null) {
                RoomDirector rd = gm.getRoomDirector();
                if (rd != null) {
                    rd.onEnemyDespawn();
                }
                josh = null; // Will respawn via RoomDirector if needed
            }
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

        // ---------- WORLD RENDER ----------
        worldViewport.apply();
        worldCamera.update();
        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        batch.setProjectionMatrix(worldCamera.combined);

        // 1) Draw floor and background layers (TMX or procedural)
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        // If TMX map is loaded, render layers below the player (excluding Y-sorted layers like furniture)
        if (tiledMapManager.hasCurrentMap()) {
            batch.end();
            tiledMapManager.renderBelowPlayerExcludingYSorted(worldCamera);
            batch.begin();
        }

        batch.end();

        // 2) Shapes: lighting (walls are now in TMX)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        lightingRenderer.render(shapeRenderer, player, player.isFlashlightOn(), battery / BATTERY_MAX);

        // === RENDER ENEMY (JOSH) - KOTAK BERWARNA ===
        // Enemy HARUS di-render di world layer SEBELUM fog-of-war
        // Animated sprite untuk Josh dengan state-based animation
        if (!GameManager.getInstance().isTestingMode()) {
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

        // Y-sorted rendering for furniture layers and player
        // This makes tables/chairs render above or below player based on Y position
        if (tiledMapManager.hasCurrentMap()) {
            TextureRegion playerFrame = player.getCurrentFrame(isMoving);
            tiledMapManager.renderWithYSort(worldCamera, batch, player.getY(), () -> {
                batch.draw(playerFrame,
                        player.getX(),
                        player.getY(),
                        player.getRenderWidth(),
                        player.getRenderHeight());
            });
        } else {
            // Fallback: render player directly without Y-sort
            TextureRegion frame = player.getCurrentFrame(isMoving);
            batch.draw(frame,
                    player.getX(),
                    player.getY(),
                    player.getRenderWidth(),
                    player.getRenderHeight());
        }

        // prompt E for legacy interactables
        if (currentInteractable != null && currentInteractable.isActive()) {
            font.draw(batch,
                    "E",
                    currentInteractable.getCenterX() - 4,
                    currentInteractable.getCenterY() + 30);
        }
        
        // prompt E for TMX tile interactables
        if (currentTileInteractable != null) {
            font.draw(batch,
                    "E",
                    currentTileInteractable.getCenterX() - 4,
                    currentTileInteractable.getCenterY() + 30);
        }
        
        // Floating message above player (e.g., "It's locked...")
        if (floatingMessage != null && floatingMessageTimer > 0) {
            font.draw(batch,
                    floatingMessage,
                    player.getCenterX() - floatingMessage.length() * 3,
                    player.getY() + player.getRenderHeight() + 20);
        }

        batch.end();

        // 3.5) Render TMX layers ABOVE player (bottom wall, bottom door)
        // This makes the player appear behind the lower wall
        if (tiledMapManager.hasCurrentMap()) {
            tiledMapManager.renderAbovePlayer(worldCamera);
        }

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

        // Render health bar (hearts)
        batch.setProjectionMatrix(uiCamera.combined);
        renderHealthBar();

        // Render inventory bar (7 slots, Minecraft-style)
        batch.setProjectionMatrix(uiCamera.combined);
        renderInventory();

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
        
        // Update floating message timer
        if (floatingMessageTimer > 0) {
            floatingMessageTimer -= delta;
            if (floatingMessageTimer <= 0) {
                floatingMessage = null;
            }
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
        if (!GameManager.getInstance().isTestingMode()) {
            updateRoomDirector(delta);
        }
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

        // Update RoomDirector logic (abstract mode movement, grace period, etc.)
        rd.update(delta);

        // Check if enemy should spawn physically
        if (rd.isEnemyPhysicallyPresent() && josh == null) {
            // Spawn enemy at door position
            spawnEnemyPhysically(rd);
        }

        // Update physical enemy if present
        if (josh != null && !josh.isDespawned()) {
            josh.update(player, currentRoom, delta);

            // Check collision with player (GAME OVER)
            if (checkEnemyPlayerCollision(josh, player)) {
                System.out.println("[GAME OVER] Josh caught you!");
                // [TODO] Trigger game over / death sequence
            }

            // Clamp to room bounds
            clampEnemyToRoom(josh);
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

        // Get spawn position from RoomDirector (at door)
        float[] pos = rd.getEnemySpawnPosition(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, enemyW, enemyH);

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

        // Check TMX tile collision (if map is loaded for this room)
        if (tiledMapManager.hasCurrentMap()) {
            if (!tiledMapManager.isAreaWalkable(
                    footBounds.x, footBounds.y, 
                    footBounds.width, footBounds.height)) {
                return true;
            }
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
        
        // Use room-specific dimensions
        float roomW = getRoomWidth();
        float roomH = getRoomHeight();

        // If TMX map is loaded, use tile-based collision bounds
        if (tiledMapManager.hasCurrentMap()) {
            com.badlogic.gdx.math.Rectangle walkable = tiledMapManager.getWalkableBounds();
            if (walkable != null) {
                // Clamp to walkable bounds
                if (ex < walkable.x) {
                    enemy.setX(walkable.x);
                }
                if (ex + ew > walkable.x + walkable.width) {
                    enemy.setX(walkable.x + walkable.width - ew);
                }
                if (ey < walkable.y) {
                    enemy.setY(walkable.y);
                }
                if (ey + eh > walkable.y + walkable.height) {
                    enemy.setY(walkable.y + walkable.height - eh);
                }
                return;
            }
        }

        // Fallback: Clamp ke dalam ruangan (dengan room border)
        if (ex < ROOM_BORDER) {
            enemy.setX(ROOM_BORDER);
        }
        if (ex + ew > roomW - ROOM_BORDER) {
            enemy.setX(roomW - ew - ROOM_BORDER);
        }
        if (ey < ROOM_BORDER) {
            enemy.setY(ROOM_BORDER);
        }
        if (ey + eh > roomH - ROOM_BORDER) {
            enemy.setY(roomH - eh - ROOM_BORDER);
        }
    }

    private void checkRoomTransition() {
        // === TMX DOOR TRANSITION ===
        // Check if player is on a door object defined in the TMX map
        if (tiledMapManager.hasCurrentMap()) {
            com.badlogic.gdx.math.Rectangle footBounds = player.getFootBounds();
            RoomId tmxDoorDestination = tiledMapManager.checkDoorTransition(
                footBounds.x, footBounds.y, footBounds.width, footBounds.height);
            
            if (tmxDoorDestination != null && tmxDoorDestination != currentRoomId) {
                // Check if destination room has a TMX map
                if (!tiledMapManager.hasMapForRoom(tmxDoorDestination)) {
                    // Room not implemented yet - show locked message
                    showFloatingMessage("It's locked...");
                    transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                    return;
                }
                
                System.out.println("[PlayScreen] TMX Door transition: " + currentRoomId + " -> " + tmxDoorDestination);
                
                float roomW = getRoomWidth();
                float roomH = getRoomHeight();
                
                // Get door info for positioning
                com.fearjosh.frontend.render.TiledMapManager.DoorInfo doorInfo = 
                    tiledMapManager.getDoorAt(footBounds.x, footBounds.y, footBounds.width, footBounds.height);
                
                // Determine which wall the door is on (for spawn positioning in new room)
                String doorWall = determineDoorWall(doorInfo, roomW, roomH);
                System.out.println("[PlayScreen] Door wall: " + doorWall);
                
                // Remember which room we came from
                RoomId previousRoom = currentRoomId;
                
                switchToRoom(tmxDoorDestination);
                
                // Position player - first try TMX spawn point, then fallback to wall-based
                positionPlayerForDoorEntry(doorWall, tmxDoorDestination, previousRoom);
                transitionCooldown = TRANSITION_COOLDOWN_DURATION;
            }
        }
    }
    
    /**
     * Display a floating message above the player's head
     */
    private void showFloatingMessage(String message) {
        this.floatingMessage = message;
        this.floatingMessageTimer = FLOATING_MESSAGE_DURATION;
    }
    
    /**
     * Determine which wall a door is on based on its position
     */
    private String determineDoorWall(com.fearjosh.frontend.render.TiledMapManager.DoorInfo doorInfo, float roomW, float roomH) {
        if (doorInfo == null) return "BOTTOM";
        
        com.badlogic.gdx.math.Rectangle bounds = doorInfo.bounds;
        float centerX = bounds.x + bounds.width / 2f;
        float centerY = bounds.y + bounds.height / 2f;
        
        // Check which wall the door is closest to
        float distToTop = roomH - centerY;
        float distToBottom = centerY;
        float distToLeft = centerX;
        float distToRight = roomW - centerX;
        
        float minDist = Math.min(Math.min(distToTop, distToBottom), Math.min(distToLeft, distToRight));
        
        if (minDist == distToTop) return "TOP";
        if (minDist == distToBottom) return "BOTTOM";
        if (minDist == distToLeft) return "LEFT";
        return "RIGHT";
    }
    
    /**
     * Position player at the opposite side when entering through a TMX door
     */
    private void positionPlayerForDoorEntry(String exitedWall, RoomId newRoom, RoomId previousRoom) {
        float roomW = getRoomWidth();
        float roomH = getRoomHeight();
        float playerW = player.getRenderWidth();
        float playerH = player.getRenderHeight();
        
        // First, try to use TMX spawn point based on previous room
        if (tiledMapManager.hasCurrentMap() && previousRoom != null) {
            com.fearjosh.frontend.render.TiledMapManager.SpawnInfo spawn = tiledMapManager.getSpawnPoint(previousRoom);
            if (spawn != null) {
                System.out.println("[PlayScreen] Using TMX spawn point from " + previousRoom + " at (" + spawn.x + ", " + spawn.y + ")");
                player.setX(spawn.x - playerW / 2f);
                player.setY(spawn.y - playerH / 2f);
                return;
            }
        }
        
        // Fallback to wall-based positioning
        // Get walkable bounds if TMX map is loaded
        com.badlogic.gdx.math.Rectangle walkable = null;
        if (tiledMapManager.hasCurrentMap()) {
            walkable = tiledMapManager.getWalkableBounds();
        }
        
        float spawnX = roomW / 2f - playerW / 2f;
        float spawnY = roomH / 2f - playerH / 2f;
        
        if (walkable != null) {
            // Position based on walkable area
            switch (exitedWall) {
                case "TOP":
                    // Exited through TOP door, spawn at BOTTOM of new room
                    spawnY = walkable.y + ENTRY_OFFSET;
                    spawnX = walkable.x + walkable.width / 2f - playerW / 2f;
                    break;
                case "BOTTOM":
                    // Exited through BOTTOM door, spawn at TOP of new room
                    spawnY = walkable.y + walkable.height - playerH - ENTRY_OFFSET;
                    spawnX = walkable.x + walkable.width / 2f - playerW / 2f;
                    break;
                case "LEFT":
                    // Exited through LEFT door, spawn at RIGHT of new room
                    spawnX = walkable.x + walkable.width - playerW - ENTRY_OFFSET;
                    spawnY = walkable.y + walkable.height / 2f - playerH / 2f;
                    break;
                case "RIGHT":
                    // Exited through RIGHT door, spawn at LEFT of new room
                    spawnX = walkable.x + ENTRY_OFFSET;
                    spawnY = walkable.y + walkable.height / 2f - playerH / 2f;
                    break;
            }
        } else {
            // Fallback positioning
            switch (exitedWall) {
                case "TOP":
                    spawnY = ROOM_BORDER + ENTRY_OFFSET;
                    break;
                case "BOTTOM":
                    spawnY = roomH - playerH - ROOM_BORDER - ENTRY_OFFSET;
                    break;
                case "LEFT":
                    spawnX = roomW - playerW - ROOM_BORDER - ENTRY_OFFSET;
                    break;
                case "RIGHT":
                    spawnX = ROOM_BORDER + ENTRY_OFFSET;
                    break;
            }
        }
        
        player.setX(spawnX);
        player.setY(spawnY);
    }
    
    // Overload for legacy calls without previousRoom
    private void positionPlayerForDoorEntry(String exitedWall, RoomId newRoom) {
        positionPlayerForDoorEntry(exitedWall, newRoom, null);
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
     */
    private void handleInventoryInput() {
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
        currentTileInteractable = null;
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
        
        // Check TMX tile interactables (lockers from Tiled map)
        if (tiledMapManager.hasCurrentMap()) {
            com.fearjosh.frontend.render.TiledMapManager.TileInteractable tileInter = 
                tiledMapManager.getTileInteractableAt(px, py, INTERACT_RANGE);
            if (tileInter != null) {
                float dx = tileInter.getCenterX() - px;
                float dy = tileInter.getCenterY() - py;
                float dist2 = dx * dx + dy * dy;
                
                if (dist2 < bestDist2) {
                    bestDist2 = dist2;
                    currentTileInteractable = tileInter;
                    currentInteractable = null; // TMX interactable takes priority
                }
            }
        }
    }

    private void handleInteractInput() {
        // Handle TMX tile interactables (lockers from Tiled map)
        if (currentTileInteractable != null && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            System.out.println("[Interact] Interacting with TMX tile: " + currentTileInteractable.type + 
                " (" + currentTileInteractable.name + ")");
            
            boolean success = tiledMapManager.toggleTileInteractable(currentTileInteractable);
            if (success) {
                System.out.println("[Interact] Tile toggled to: " + 
                    (currentTileInteractable.isOpen ? "OPEN" : "CLOSED"));
                // You can add sound effects, item drops, etc. here based on type
                // if (currentTileInteractable.type.equals("locker")) { ... }
            }
            return;
        }
        
        // Handle legacy interactables
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
    // FOG OF WAR
    // ======================

    private void renderDarknessLayer() {
        // Skip fog of war for LOBBY - it's well lit
        if (currentRoomId == RoomId.LOBBY) {
            return;
        }
        
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
     */
    private void renderInventory() {
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

        vignetteTexture.dispose();
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
        if (tiledMapManager != null) {
            tiledMapManager.dispose();
        }
    }

}
