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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.camera.CameraController;
import com.fearjosh.frontend.entity.Enemy;
import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.entity.KeyItem;
import com.fearjosh.frontend.render.HudRenderer;
import com.fearjosh.frontend.render.LightingRenderer;
import com.fearjosh.frontend.render.TiledMapManager;
import com.fearjosh.frontend.world.*;
import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.core.RoomDirector;
import com.fearjosh.frontend.core.JoshSpawnController;
import com.fearjosh.frontend.input.InputHandler;
import com.fearjosh.frontend.systems.Inventory;
import com.fearjosh.frontend.systems.TutorialOverlay;
import com.fearjosh.frontend.systems.KeyManager;
import com.fearjosh.frontend.cutscene.InGameCutscene;
import com.fearjosh.frontend.entity.Item;
import com.fearjosh.frontend.entity.BatteryItem;
import com.fearjosh.frontend.entity.ChocolateItem;
import com.fearjosh.frontend.systems.AudioManager;
import com.fearjosh.frontend.systems.JumpscareManager;
import com.fearjosh.frontend.ui.PauseMenuOverlay;
import com.fearjosh.frontend.ui.InjuredMinigame;

public class PlayScreen implements Screen, RoomDirector.RoomDirectorEventListener {

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
    
    // ============== NEW SYSTEMS FOR FINAL GAME ==============
    // In-game cutscene for Josh encounter
    private InGameCutscene inGameCutscene;
    
    // Tutorial overlay for STORY state
    private TutorialOverlay tutorialOverlay;
    
    // Track if gym encounter has been triggered
    private boolean gymEncounterTriggered = false;
    
    // Gym center trigger zone
    private static final float GYM_TRIGGER_ZONE_SIZE = 150f;
    private SpriteBatch batch;
    private BitmapFont font;

    private Player player;
    private Enemy josh;
    private Texture playerTexture; // hanya dipakai untuk ukuran awal
    // overlay ambience gelap (vignette)
    private Texture vignetteTexture;

    // ------------ INJURY/DEATH SYSTEM ------------
    private boolean playerBeingCaught = false;
    private float injuryTimer = 0f;
    private static final float INJURY_DELAY = 2.0f; // 2 seconds before player is injured
    private boolean playerFullyInjured = false;
    private Texture injuryTransitionTexture;
    private float injuryTransitionAlpha = 0f;
    private static final float INJURY_TRANSITION_DURATION = 1.5f; // Fade in duration
    private float injuryPhaseTimer = 0f; // Timer for injury phase (5 seconds)
    private static final float INJURY_PHASE_DURATION = 3.0f; // 3 seconds for overlay

    // ------------ INJURED MINIGAME (replaces old escape minigame) ------------
    private InjuredMinigame injuredMinigame;
    
    // Legacy escape minigame fields (kept for transition, will be replaced by InjuredMinigame)
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

    // ------------ AMBIENT AUDIO ------------
    private long cricketSoundId = -1;
    private long rainSoundId = -1;
    private long footstepSoundId = -1;

    // Monster audio
    private float monsterGruntTimer = 0f;
    private static final float MONSTER_GRUNT_INTERVAL = 3.0f; // Grunt every 3 seconds
    private static final float MONSTER_PROXIMITY_RANGE = 700f; // Range for grunt sound

    private float monsterRoarTimer = 0f;
    private static final float MONSTER_ROAR_INTERVAL = 3.0f; // Roar every 4 seconds
    private static final float MONSTER_ROAR_RANGE = 400f; // Closer range for roar (aggressive)

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

    // Current room tracking (TMX-based)
    private RoomId currentRoomId = RoomId.LOBBY; // Start in LOBBY

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

    // Interaction (TMX tile-based only - legacy Interactable system removed)
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
    
    // New pause menu overlay with confirmation dialog
    private PauseMenuOverlay pauseMenuOverlay;

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
        
        // Connect LightingRenderer to TiledMapManager for wall collision on flashlight
        lightingRenderer.setMapManager(tiledMapManager);

        vignetteTexture = new Texture("General/vignette.png");

        // Load injury transition texture
        injuryTransitionTexture = new Texture("UI/josh_caught_you.jpg");

        // Initialize injury state
        playerBeingCaught = false;
        playerFullyInjured = false;
        injuryTimer = 0f;
        injuryTransitionAlpha = 0f;
        injuryPhaseTimer = 0f;
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

        // Position player at first spawn point if available (for game start)
        if (tiledMapManager.hasCurrentMap()) {
            com.fearjosh.frontend.render.TiledMapManager.SpawnInfo firstSpawn = tiledMapManager.getFirstSpawnPoint();
            if (firstSpawn != null) {
                player.setX(firstSpawn.x - player.getRenderWidth() / 2f);
                player.setY(firstSpawn.y - player.getRenderHeight() / 2f);
                System.out.println(
                        "[PlayScreen] Positioned player at first spawn: (" + firstSpawn.x + ", " + firstSpawn.y + ")");
            }
        }

        // === NEW ENEMY SYSTEM: RoomDirector-based spawning ===
        // Enemy spawn is now handled by RoomDirector (abstract/physical mode)
        // Old direct spawn removed - see RoomDirector for stalking behavior
        // Enemy will be created lazily when RoomDirector signals physical presence
        // Enemy entity will be created on-demand via spawnEnemyPhysically()
        josh = null; // Start with no physical enemy
        // ====================================================
        
        // === INITIALIZE NEW FINAL GAME SYSTEMS ===
        // In-game cutscene system
        inGameCutscene = new InGameCutscene();
        
        // Injured minigame system (replaces old escape minigame)
        injuredMinigame = new InjuredMinigame();
        
        // Tutorial overlay
        tutorialOverlay = TutorialOverlay.getInstance();
        
        // Reset key manager for new game
        KeyManager.getInstance().reset();
        
        // Initialize pause menu overlay with callbacks
        pauseMenuOverlay = new PauseMenuOverlay(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        pauseMenuOverlay.setOnResume(() -> {
            paused = false;
            GameManager.getInstance().setCurrentState(GameManager.GameState.PLAYING);
            System.out.println("[PlayScreen] Pause -> Resume");
        });
        pauseMenuOverlay.setOnConfirmQuit(() -> {
            // QUIT TO MENU - Clear session and go to fresh main menu
            GameManager gm2 = GameManager.getInstance();
            gm2.clearSession(); // IMPORTANT: Clear session so no Resume in menu
            gm2.setCurrentState(GameManager.GameState.MAIN_MENU);
            game.setScreen(new MainMenuScreen(game));
            System.out.println("[PlayScreen] Pause -> Quit to Menu: Session cleared, fresh menu");
        });
        
        // Check if this is STORY state (first time playing after cutscenes)
        // In STORY state, player can move but interactions are limited
        if (gm.getCurrentState() == GameManager.GameState.STORY) {
            tutorialOverlay.start();
            gymEncounterTriggered = false;
            System.out.println("[PlayScreen] STORY mode - Tutorial started, Josh encounter pending");
        } else if (gm.hasMetJosh()) {
            // Already met Josh, full gameplay
            gymEncounterTriggered = true;
            System.out.println("[PlayScreen] Full PLAYING mode - Josh already encountered");
        }
        // ==========================================

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

        // Get room-specific dimensions
        float roomWidth = id.getWidth();
        float roomHeight = id.getHeight();

        // Update camera controller bounds for the new room size
        cameraController.setWorldBounds(roomWidth, roomHeight);

        // Load TMX map for this room (if available)
        tiledMapManager.loadMapForRoom(id);
        
        // Restore consumed exit_key trigger states for this room
        restoreConsumedExitKeyTriggers(id);

        // === NOTIFY ROOMDIRECTOR: Player changed room ===
        gm.notifyPlayerRoomChange(id);

        // Remove physical enemy when changing rooms (becomes abstract)
        if (josh != null) {
            RoomDirector rd = gm.getRoomDirector();
            if (rd != null) {
                rd.onEnemyDespawn();
            }
            // Notify spawn controller that Josh despawned
            JoshSpawnController spawnController = gm.getJoshSpawnController();
            if (spawnController != null) {
                spawnController.onJoshDespawned();
            }
            josh = null; // Will respawn via JoshSpawnController if needed
        }
        // ===============================================
    }

    // ======================
    // GAME LOOP
    // ======================

    @Override
    public void render(float delta) {
        // === UPDATE IN-GAME CUTSCENE (must happen even when not PLAYING) ===
        if (inGameCutscene != null && inGameCutscene.isActive()) {
            inGameCutscene.update(delta);
        }
        
        // === UPDATE TUTORIAL OVERLAY (during STORY state) ===
        if (tutorialOverlay != null && tutorialOverlay.isActive()) {
            tutorialOverlay.update(delta);
        }
        
        // STATE CHECK: Update game logic only when PLAYING (not paused or in cutscene)
        GameManager gm = GameManager.getInstance();
        boolean canUpdateGameLogic = !paused && !pauseMenuOverlay.isActive() && 
            (gm.isPlaying() || gm.getCurrentState() == GameManager.GameState.STORY);
        
        // Skip game logic update during active cutscene
        if (inGameCutscene != null && inGameCutscene.isActive()) {
            canUpdateGameLogic = false;
        }
        
        if (canUpdateGameLogic) {
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

        // If injured and in phase 1 (first 5 seconds): show black screen + overlay
        if (playerFullyInjured && injuryPhaseTimer < INJURY_PHASE_DURATION) {
            renderInjuryTransitionPhase();
            return;
        }

        // ---------- WORLD RENDER ----------
        worldViewport.apply();
        worldCamera.update();
        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        batch.setProjectionMatrix(worldCamera.combined);

        // 1) Draw floor and background layers (TMX or procedural)
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        // If TMX map is loaded, render layers below the player (excluding Y-sorted
        // layers like furniture)
        if (tiledMapManager.hasCurrentMap()) {
            batch.end();
            tiledMapManager.renderBelowPlayerExcludingYSorted(worldCamera);
            batch.begin();
            
            // NOTE: locker_key is now rendered AFTER Y-sorted layers (furniture)
            // so it appears ON TOP of tables/desks
        }
        // No fallback procedural floor - TMX maps only

        batch.end();

        // 2) Shapes: lighting (walls are now in TMX)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        lightingRenderer.render(shapeRenderer, player, player.isFlashlightOn(), battery / BATTERY_MAX);

        // Walls are now in TMX maps only

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

        // Legacy Interactable rendering removed - all interactables now via TMX

        shapeRenderer.end();

        // 3) Texture: player + prompt 'E' (with Y-sorted TMX furniture)
        // Skip player rendering if injured minigame is active (player sprite handled by minigame)
        boolean shouldRenderPlayer = !injuredMinigame.isActive();
        
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        // If TMX map loaded, use Y-sorted rendering for player among furniture
        if (tiledMapManager.hasCurrentMap()) {
            batch.end();
            tiledMapManager.renderWithYSort(worldCamera, batch, player.getY(), () -> {
                if (shouldRenderPlayer) {
                    batch.begin();
                    TextureRegion frame = player.getCurrentFrame(isMoving);
                    batch.draw(frame,
                            player.getX(),
                            player.getY(),
                            player.getRenderWidth(),
                            player.getRenderHeight());
                    batch.end();
                }
            });
            batch.begin();

            // Render ONLY physical locker_key pickup (janitor_key and gym_key are invisible triggers)
            // Rendered AFTER Y-sorted layers so key appears ON TOP of furniture/tables
            tiledMapManager.renderPhysicalLockerKey(batch);
            
            // Render above-player layers (if any)
            batch.end();
            tiledMapManager.renderAbovePlayer(worldCamera);
            batch.begin();
        } else {
            // Fallback: render player without Y-sort
            if (shouldRenderPlayer) {
                TextureRegion frame = player.getCurrentFrame(isMoving);
                batch.draw(frame,
                        player.getX(),
                        player.getY(),
                        player.getRenderWidth(),
                        player.getRenderHeight());
            }
        }

        // prompt E for tile interactables (TMX-based only now) - only when not in minigame
        if (currentTileInteractable != null && !injuredMinigame.isActive()) {
            font.draw(batch,
                    "E",
                    currentTileInteractable.getCenterX() - 4,
                    currentTileInteractable.getCenterY() + 30);
        }

        // Floating message above player (e.g., "It's locked...")
        if (floatingMessage != null && floatingMessageTimer > 0 && !injuredMinigame.isActive()) {
            font.draw(batch,
                    floatingMessage,
                    player.getCenterX() - floatingMessage.length() * 3,
                    player.getY() + player.getRenderHeight() + 20);
        }

        batch.end();

        // 4) Fog of War - Render to darkness buffer, then composite
        renderDarknessLayer();

        // ---------- HUD RENDER (kamera UI) ----------
        uiCamera.update();
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        batch.setProjectionMatrix(uiCamera.combined);

        // Only render HUD and inventory if NOT injured (or still in phase 1)
        if (!playerFullyInjured || injuryPhaseTimer < INJURY_PHASE_DURATION) {
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

            // Render health bar (hearts)
            batch.setProjectionMatrix(uiCamera.combined);
            renderHealthBar();

            // Render inventory bar (7 slots, Minecraft-style)
            batch.setProjectionMatrix(uiCamera.combined);
            renderInventory();
        }

        // If injured and after 3 seconds, show hint text (only if injured minigame is not active)
        if (playerFullyInjured && injuryPhaseTimer >= INJURY_PHASE_DURATION && !injuredMinigame.isActive() && !escapeMinigameActive) {
            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            String hintText = "Press F to bandage yourself";
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font,
                    hintText);
            float textX = (VIRTUAL_WIDTH - layout.width) / 2f;
            float textY = 60f; // Bottom center
            font.draw(batch, hintText, textX, textY);
            batch.end();
        }

        // === RENDER INJURED MINIGAME FULL OVERLAY ===
        // This renders the complete minigame UI with injured sprite, progress bar, and instructions
        if (injuredMinigame.isActive()) {
            shapeRenderer.setProjectionMatrix(uiCamera.combined);
            batch.setProjectionMatrix(uiCamera.combined);
            // Use full overlay render - shows injured sprite, progress bar, text instructions
            injuredMinigame.render(shapeRenderer, batch, font, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        }

        // Render escape minigame UI (legacy - kept for transition)
        if (escapeMinigameActive) {
            renderEscapeMinigame();
        }

        // Handle pause button click in UI space - HANYA jika state PLAYING dan tidak sedang dalam pause overlay
        if (GameManager.getInstance().isPlaying() && !pauseMenuOverlay.isActive() && Gdx.input.justTouched()) {
            float screenX = Gdx.input.getX();
            float screenY = Gdx.input.getY();
            // Convert to UI world coords
            com.badlogic.gdx.math.Vector3 uiCoords = uiCamera
                    .unproject(new com.badlogic.gdx.math.Vector3(screenX, screenY, 0));
            com.badlogic.gdx.math.Rectangle r = hudRenderer.getPauseButtonBounds();
            if (r.contains(uiCoords.x, uiCoords.y)) {
                paused = true;
                pauseMenuOverlay.show();
                GameManager.getInstance().setCurrentState(GameManager.GameState.PAUSED);
            }
        }
        
        // Handle ESC key to pause - HANYA jika PLAYING
        if (GameManager.getInstance().isPlaying() && !pauseMenuOverlay.isActive() && 
            Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = true;
            pauseMenuOverlay.show();
            GameManager.getInstance().setCurrentState(GameManager.GameState.PAUSED);
        }

        // Update and render pause menu overlay
        if (pauseMenuOverlay.isActive()) {
            pauseMenuOverlay.update(uiCamera);
            batch.setProjectionMatrix(uiCamera.combined);
            pauseMenuOverlay.render(shapeRenderer, batch, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        }
        
        // === RENDER TUTORIAL OVERLAY (during STORY state) ===
        if (tutorialOverlay != null && tutorialOverlay.isActive()) {
            tutorialOverlay.render(shapeRenderer, batch, font, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        }
        
        // === RENDER IN-GAME CUTSCENE ===
        if (inGameCutscene != null && inGameCutscene.isActive()) {
            inGameCutscene.render(shapeRenderer, batch, font, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        }
        
        // === ALWAYS RENDER DIFFICULTY AND ROOM INFO (bottom-left and top-right) ===
        // This is rendered outside the injured check so it's always visible
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        hudRenderer.renderText(batch, font, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        batch.end();
        
        // === RENDER OBJECTIVE HINT (after meeting Josh) ===
        if (GameManager.getInstance().isObjectiveEscape() && !paused && !isGameOver && 
            (inGameCutscene == null || !inGameCutscene.isActive())) {
            renderObjectiveHint();
        }
        
        // === RENDER DIFFICULTY TEXT - MUST BE LAST! ===
        // This ensures difficulty is ALWAYS visible at bottom-left, regardless of state
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        hudRenderer.renderDifficultyText(batch, font);
        batch.end();

        // === RENDER JUMPSCARE (ABSOLUTE LAST - on top of EVERYTHING) ===
        // Jumpscares should cover the entire screen including UI
        if (JumpscareManager.getInstance().isJumpscareActive()) {
            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            JumpscareManager.getInstance().render(batch, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            batch.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    /**
     * Render objective hint at top of screen
     */
    private void renderObjectiveHint() {
        String hint = KeyManager.getInstance().getProgressHint();
        if (hint == null || hint.isEmpty()) return;
        
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        font.setColor(0.9f, 0.9f, 0.5f, 0.8f);
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, hint);
        font.draw(batch, hint, (VIRTUAL_WIDTH - layout.width) / 2f, VIRTUAL_HEIGHT - 20f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void update(float delta) {
        // Cutscene and tutorial updates are handled in render() to prevent freeze
        // when game state changes to CUTSCENE (which blocks isPlaying() check)

        // Update floating message timer
        if (floatingMessageTimer > 0) {
            floatingMessageTimer -= delta;
            if (floatingMessageTimer <= 0) {
                floatingMessage = null;
            }
        }

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
        
        // === CHECK GYM ENCOUNTER TRIGGER (STORY state only) ===
        checkGymEncounterTrigger();

        updateBattery(delta);
        findCurrentTileInteractable(); // TMX-based interaction only
        
        // === CHECK EXIT_KEY TRIGGERS (TMX trigger zones for key progression) ===
        // Handles: locker_key (Class1A), janitor_key (Hallway), gym_key (Janitor room)
        checkExitKeyTriggers();
        
        // === AUTO-TRIGGER KEY USAGE for DOORS (proximity-based) ===
        checkAutoKeyTrigger();
        
        handleTileInteractInput();
        handleInventoryInput();

        // === UPDATE JUMPSCARE MANAGER ===
        // Only update when not blocked (paused/cutscene/minigame)
        boolean jumpscareBlocked = paused || pauseMenuOverlay.isActive() || 
            (inGameCutscene != null && inGameCutscene.isActive()) ||
            injuredMinigame.isActive() || tutorialOverlay.isActive();
        JumpscareManager.getInstance().update(delta, !jumpscareBlocked);
        
        // Update hallway state for ambient jumpscare boost
        JumpscareManager.getInstance().setInHallway(currentRoomId == RoomId.HALLWAY);

        // === ROOM DIRECTOR SYSTEM: Abstract/Physical enemy control ===
        // Only active AFTER meeting Josh (not during STORY state)
        if (GameManager.getInstance().hasMetJosh()) {
            updateRoomDirector(delta);
        }
        // ==============================================================

        // Kamera update di akhir logic
        cameraController.update(worldCamera, worldViewport, player);
    }
    
    /**
     * Check if player has reached gym center to trigger Josh encounter
     * Only triggers during STORY state (before meeting Josh)
     */
    private void checkGymEncounterTrigger() {
        GameManager gm = GameManager.getInstance();
        
        // Only trigger during STORY state before meeting Josh
        if (gymEncounterTriggered || gm.hasMetJosh()) {
            return;
        }
        
        // Only in GYM room
        if (currentRoomId != RoomId.GYM) {
            return;
        }
        
        // Check if player is near center of gym
        float gymCenterX = getRoomWidth() / 2f;
        float gymCenterY = getRoomHeight() / 2f;
        
        float playerCenterX = player.getCenterX();
        float playerCenterY = player.getCenterY();
        
        float dx = playerCenterX - gymCenterX;
        float dy = playerCenterY - gymCenterY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance < GYM_TRIGGER_ZONE_SIZE) {
            // TRIGGER GYM ENCOUNTER!
            gymEncounterTriggered = true;
            System.out.println("[PlayScreen] GYM ENCOUNTER TRIGGERED! Starting cutscene...");
            
            // Skip tutorial if still running
            if (tutorialOverlay != null && tutorialOverlay.isActive()) {
                tutorialOverlay.skip();
            }
            
            // Start gym encounter cutscene
            inGameCutscene.startCutscene(InGameCutscene.CutsceneType.GYM_ENCOUNTER, () -> {
                // After gym encounter cutscene completes, show objective change
                System.out.println("[PlayScreen] Gym encounter complete, showing objective change...");
                
                inGameCutscene.startCutscene(InGameCutscene.CutsceneType.OBJECTIVE_CHANGE, () -> {
                    // After objective change, start TUTORIAL for injured/bandage minigame
                    // Player "wakes up" injured and must learn how to bandage themselves
                    System.out.println("[PlayScreen] Starting injured minigame TUTORIAL (no health loss)...");
                    
                    // Start tutorial injured minigame
                    startInjuredMinigameTutorial();
                });
            });
        }
    }
    
    /**
     * Start injured minigame as a TUTORIAL (after gym encounter cutscene).
     * This teaches the player how to use the bandage mechanic without losing health.
     */
    private void startInjuredMinigameTutorial() {
        System.out.println("[Tutorial] Injured minigame tutorial started - player learns bandage mechanic");
        
        // Start the injured minigame with tutorial mode enabled (easier + no real penalty)
        injuredMinigame.start(
            // On success callback
            () -> {
                System.out.println("[Tutorial] Player completed injured minigame tutorial!");
                onInjuredTutorialComplete();
            },
            // On fail callback - in tutorial, let them retry without penalty
            () -> {
                System.out.println("[Tutorial] Player failed tutorial - letting them retry...");
                // Restart tutorial immediately (no penalty)
                startInjuredMinigameTutorial();
            },
            true  // Tutorial mode - longer time, slower decay
        );
    }
    
    /**
     * Called when player completes the injured minigame tutorial.
     * Transitions to full PLAYING state.
     */
    private void onInjuredTutorialComplete() {
        System.out.println("[Tutorial] Injured tutorial complete - transitioning to full gameplay");
        
        GameManager gm = GameManager.getInstance();
        gm.transitionToFullPlayMode();
        
        // Show a brief message
        showFloatingMessage("Kamu sudah belajar cara membalut luka!");
        
        // Now Josh can hunt the player
        // RoomDirector will start spawning Josh
    }
    
    /**
     * Restore consumed exit_key trigger states when entering a room.
     * This ensures triggers that were already used don't re-trigger.
     */
    private void restoreConsumedExitKeyTriggers(RoomId roomId) {
        if (tiledMapManager == null) return;
        
        KeyManager keyMgr = KeyManager.getInstance();
        java.util.Set<Integer> consumedIds = keyMgr.getConsumedTriggerIdsForRoom(roomId);
        
        if (consumedIds.isEmpty()) return;
        
        System.out.println("[ExitKeyTrigger] Restoring " + consumedIds.size() + 
            " consumed triggers for room " + roomId);
        
        for (int objectId : consumedIds) {
            tiledMapManager.markTriggerConsumed(objectId);
        }
    }
    
    /**
     * Handle TMX exit_key trigger zones.
     * These are INVISIBLE trigger areas that grant keys when player overlaps them.
     * Keys are NOT rendered physically - they appear in inventory after trigger activation.
     * 
     * TRIGGER LOGIC:
     * - locker_key: Granted directly when player overlaps trigger in Class1A
     * - janitor_key: Granted when player overlaps trigger in Hallway IF player has locker_key (consumes it)
     * - gym_key: Granted when player overlaps trigger in Janitor room IF player has janitor_key (consumes it)
     */
    private void checkExitKeyTriggers() {
        if (tiledMapManager == null) return;
        
        GameManager gm = GameManager.getInstance();
        KeyManager keyMgr = KeyManager.getInstance();
        
        // === TUTORIAL STATE RESTRICTION ===
        // During tutorial state, don't allow key triggers
        if (gm.isInTutorialState()) {
            return;
        }
        
        // Check if player overlaps any exit_key trigger zone
        Rectangle playerBounds = player.getFootBounds();
        TiledMapManager.ExitKeyTrigger trigger = tiledMapManager.checkExitKeyTrigger(playerBounds);
        
        if (trigger == null) return;
        
        // Check if this trigger was already consumed (persistent tracking)
        if (keyMgr.isExitKeyTriggerConsumed(currentRoomId, trigger.objectId)) {
            trigger.consumed = true; // Sync local state
            return;
        }
        
        String itemType = trigger.itemType.toLowerCase();
        Inventory inventory = gm.getInventory();
        
        System.out.println("[ExitKeyTrigger] Player overlapping trigger: " + itemType + 
            " in room=" + currentRoomId + " objectId=" + trigger.objectId);
        
        // === LOCKER_KEY TRIGGER (Class1A) ===
        // No requirements - just grant the key
        if (itemType.equals("locker_key")) {
            if (!keyMgr.hasLockerKey()) {
                KeyItem keyItem = new KeyItem(KeyItem.KeyType.LOCKER_KEY);
                if (inventory.addItem(keyItem)) {
                    // Mark trigger as consumed (local + persistent)
                    tiledMapManager.consumeExitKeyTrigger(trigger);
                    keyMgr.markExitKeyTriggerConsumed(currentRoomId, trigger.objectId);
                    
                    showFloatingMessage("Dapat: Kunci Loker!");
                    System.out.println("[ExitKeyTrigger] Granted LOCKER_KEY");
                } else {
                    showFloatingMessage("Tas penuh!");
                }
            }
            return;
        }
        
        // === JANITOR_KEY TRIGGER (Hallway) ===
        // REQUIRES: locker_key in inventory (will be consumed)
        if (itemType.equals("janitor_key")) {
            if (keyMgr.hasJanitorKey()) {
                return; // Already have it
            }
            
            if (!keyMgr.hasLockerKey()) {
                // Player doesn't have required key - show hint
                showFloatingMessage("Loker ini butuh kunci...");
                System.out.println("[ExitKeyTrigger] janitor_key trigger blocked - missing LOCKER_KEY");
                return;
            }
            
            // Player has locker_key - consume it and grant janitor_key
            KeyItem janitorKey = new KeyItem(KeyItem.KeyType.JANITOR_KEY);
            if (inventory.addItem(janitorKey)) {
                // Consume locker_key from inventory
                keyMgr.useKey(KeyItem.KeyType.LOCKER_KEY);
                keyMgr.unlockSpecialLocker();
                
                // Mark trigger as consumed (local + persistent)
                tiledMapManager.consumeExitKeyTrigger(trigger);
                keyMgr.markExitKeyTriggerConsumed(currentRoomId, trigger.objectId);
                
                showFloatingMessage("Pakai Kunci Loker! Dapat: Kunci Ruang Penjaga!");
                System.out.println("[ExitKeyTrigger] Consumed LOCKER_KEY, granted JANITOR_KEY");
            } else {
                showFloatingMessage("Tas penuh!");
            }
            return;
        }
        
        // === GYM_KEY TRIGGER (Janitor Room) ===
        // No requirements - just grant the key (same concept as locker_key)
        if (itemType.equals("gym_key")) {
            if (!keyMgr.hasGymKey()) {
                KeyItem gymKey = new KeyItem(KeyItem.KeyType.GYM_KEY);
                if (inventory.addItem(gymKey)) {
                    // Mark trigger as consumed (local + persistent)
                    tiledMapManager.consumeExitKeyTrigger(trigger);
                    keyMgr.markExitKeyTriggerConsumed(currentRoomId, trigger.objectId);
                    
                    showFloatingMessage("Dapat: Kunci Pintu Belakang Gym!");
                    System.out.println("[ExitKeyTrigger] Granted GYM_KEY");
                } else {
                    showFloatingMessage("Tas penuh!");
                }
            }
            return;
        }
        
        // Unknown trigger type
        System.out.println("[ExitKeyTrigger] Unknown trigger item type: " + itemType);
    }
    
    /**
     * Auto-trigger key usage based on proximity to locked DOORS.
     * Called every frame to check if player is near a locked door that matches a key in inventory.
     * Keys are used AUTOMATICALLY when player walks near the correct locked door.
     * 
     * NOTE: This is SEPARATE from exit_key triggers - this handles door unlocking.
     */
    private void checkAutoKeyTrigger() {
        if (tiledMapManager == null || !tiledMapManager.hasCurrentMap()) return;
        
        GameManager gm = GameManager.getInstance();
        KeyManager keyMgr = KeyManager.getInstance();
        
        // Don't auto-trigger during tutorial state
        if (gm.isInTutorialState()) return;
        
        // Check for nearby doors/triggers using TMX object layer
        TiledMapManager.DoorInfo doorInfo = tiledMapManager.getDoorAt(
            player.getFootBounds().x, player.getFootBounds().y,
            player.getFootBounds().width, player.getFootBounds().height);
        
        // NOTE: Key granting (locker_key → janitor_key → gym_key) is now handled by
        // checkExitKeyTriggers() via TMX exit_key object layer.
        // This method only handles DOOR UNLOCKING with keys already in inventory.
        
        // === AUTO-TRIGGER: JANITOR_KEY → JANITOR_DOOR ===
        // If player has janitor_key and is near janitor room door
        if (keyMgr.hasJanitorKey() && !keyMgr.isJanitorRoomUnlocked()) {
            if (doorInfo != null && doorInfo.targetRoom != null &&
                doorInfo.targetRoom.equalsIgnoreCase("JANITOR")) {
                // AUTO-USE janitor_key to unlock door (key already consumed by exit_key trigger)
                System.out.println("[AutoKey] Unlocking janitor door with JANITOR_KEY...");
                keyMgr.unlockJanitorRoom();
                
                showFloatingMessage("Ruang Penjaga terbuka!");
                return;
            }
        }
        
        // === AUTO-TRIGGER: GYM_KEY → GYM_BACK_DOOR ===
        // If player has gym_key and is near gym back door
        if (keyMgr.hasGymKey() && !keyMgr.isGymBackDoorUnlocked()) {
            if (doorInfo != null && doorInfo.doorName != null &&
                doorInfo.doorName.toLowerCase().contains("gym_back")) {
                // AUTO-USE gym_key
                System.out.println("[AutoKey] Using GYM_KEY at gym back door...");
                keyMgr.unlockGymBackDoor();
                
                // Don't remove gym_key (keep for story)
                showFloatingMessage("Pintu Belakang Gym terbuka! Waktunya kabur...");
                return;
            }
        }
    }

    /**
     * Update RoomDirector and handle enemy spawning via JoshSpawnController
     * 
     * NEW SYSTEM: Uses context-aware JoshSpawnController for intelligent spawning
     * - Josh only spawns in SAME room as player
     * - Spawn positions come from TMX josh_spawn objects
     * - Spawn is conditional (cooldown + chance + context)
     */
    private void updateRoomDirector(float delta) {
        GameManager gm = GameManager.getInstance();
        RoomDirector rd = gm.getRoomDirector();
        JoshSpawnController spawnController = gm.getJoshSpawnController();

        if (rd == null)
            return;
            
        // Register this PlayScreen as the event listener for camera shake etc.
        if (rd.getEventListener() == null) {
            rd.setEventListener(this);
        }
        
        // Update spawn controller timer
        if (spawnController != null) {
            spawnController.update(delta);
        }

        // Don't update if player is injured (freeze enemy behavior)
        if (!playerFullyInjured) {
            // Update RoomDirector logic (for audio cues and proximity tracking)
            rd.update(delta);

            // === NEW CONTEXT-AWARE SPAWN SYSTEM ===
            // Use JoshSpawnController instead of RoomDirector for spawning
            if (josh == null && spawnController != null) {
                // Determine if spawning is allowed (not during cutscene/minigame/tutorial)
                boolean canSpawn = gm.isPlaying() && 
                                   !injuredMinigame.isActive() && 
                                   !escapeMinigameActive &&
                                   (inGameCutscene == null || !inGameCutscene.isActive());
                
                JoshSpawnController.SpawnDecision decision = spawnController.shouldSpawnJosh(
                    tiledMapManager,
                    player.getCenterX(),
                    player.getCenterY(),
                    canSpawn
                );
                
                if (decision.shouldSpawn) {
                    // Spawn Josh at the selected position
                    spawnEnemyAtPosition(decision.spawnX, decision.spawnY);
                    spawnController.onJoshSpawned(currentRoomId);
                    
                    System.out.println("[JoshSpawn] ✅ " + decision.reason + 
                        " at (" + (int)decision.spawnX + ", " + (int)decision.spawnY + ")");
                } else if (com.fearjosh.frontend.config.Constants.DEBUG_ROOM_DIRECTOR) {
                    // Debug: show why spawn was blocked (only occasionally to avoid spam)
                    if (Math.random() < 0.01) { // 1% chance to log
                        System.out.println("[JoshSpawn] ❌ " + decision.reason);
                    }
                }
            }
        }

        // Update physical enemy if present (but not if player is injured)
        if (josh != null && !josh.isDespawned() && !playerFullyInjured) {
            josh.update(player, delta);
            
            // Notify spawn controller when Josh is chasing
            if (spawnController != null && 
                josh.getCurrentStateType() == com.fearjosh.frontend.state.enemy.EnemyStateType.CHASING) {
                spawnController.onPlayerChased();
            }

            // === MONSTER PROXIMITY SOUND ===
            // Calculate distance from monster to player
            float dx = josh.getCenterX() - player.getCenterX();
            float dy = josh.getCenterY() - player.getCenterY();
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            // Update grunt timer
            monsterGruntTimer += delta;

            // Play grunt sound when monster is close and timer expires
            if (distance < MONSTER_PROXIMITY_RANGE && monsterGruntTimer >= MONSTER_GRUNT_INTERVAL) {
                // Calculate volume based on distance (closer = louder)
                // At distance 0: volume = 1.0, at MONSTER_PROXIMITY_RANGE: volume = 0.2
                float volumeMultiplier = 1.0f - (distance / MONSTER_PROXIMITY_RANGE) * 0.8f;
                volumeMultiplier = Math.max(0.2f, Math.min(1.0f, volumeMultiplier));

                AudioManager.getInstance().playSound("Audio/Effect/monster_grunt_sound_effect.wav", volumeMultiplier);
                monsterGruntTimer = 0f; // Reset timer

                if (com.fearjosh.frontend.config.Constants.DEBUG_ROOM_DIRECTOR) {
                    System.out.println(
                            "[Monster] Grunt at distance: " + (int) distance + ", volume: " + volumeMultiplier);
                }
            }

            // === MONSTER ROAR SOUND (CHASE STATE + VERY CLOSE) ===
            monsterRoarTimer += delta;

            // Play roar sound when monster is CHASING and VERY close
            if (josh.getCurrentStateType() == com.fearjosh.frontend.state.enemy.EnemyStateType.CHASING
                    && distance < MONSTER_ROAR_RANGE
                    && monsterRoarTimer >= MONSTER_ROAR_INTERVAL) {
                // Calculate volume based on distance (closer = louder)
                // At distance 0: volume = 1.0, at MONSTER_ROAR_RANGE: volume = 0.3
                float roarVolume = 1.0f - (distance / MONSTER_ROAR_RANGE) * 0.7f;
                roarVolume = Math.max(0.3f, Math.min(1.0f, roarVolume));

                AudioManager.getInstance().playSound("Audio/Effect/monster_roar_sound_effect.wav", roarVolume);
                monsterRoarTimer = 0f; // Reset timer

                if (com.fearjosh.frontend.config.Constants.DEBUG_ROOM_DIRECTOR) {
                    System.out.println(
                            "[Monster] ROAR at distance: " + (int) distance + ", volume: " + roarVolume + " [CHASING]");
                }
            }

            // Check collision with player (INJURY SYSTEM)
            if (checkEnemyPlayerCollision(josh, player)) {
                if (!playerBeingCaught && !playerFullyInjured) {
                    // Start injury timer
                    playerBeingCaught = true;
                    injuryTimer = 0f;
                    System.out.println("[INJURY] Josh caught player! Timer started...");
                } else if (playerBeingCaught) {
                    // Update injury timer
                    injuryTimer += delta;

                    if (injuryTimer >= INJURY_DELAY) {
                        // Player is injured after 2 seconds
                        triggerPlayerInjured();
                    }
                }
            } else {
                // Player escaped before 2 seconds
                if (playerBeingCaught && !playerFullyInjured) {
                    System.out.println("[INJURY] Player escaped from Josh!");
                    playerBeingCaught = false;
                    injuryTimer = 0f;
                }
            }

            // Clamp to room bounds (only if enemy still exists)
            if (josh != null) {
                clampEnemyToRoom(josh);
            }
        }

        // Update injury phase timer and transition
        if (playerFullyInjured) {
            injuryPhaseTimer += delta;

            // Fade in overlay during first 1.5 seconds
            if (injuryTransitionAlpha < 1f && injuryPhaseTimer < INJURY_TRANSITION_DURATION) {
                injuryTransitionAlpha += delta / INJURY_TRANSITION_DURATION;
                if (injuryTransitionAlpha > 1f) {
                    injuryTransitionAlpha = 1f;
                }
            }
        }

        // Update injured minigame (new system)
        if (injuredMinigame.isActive()) {
            injuredMinigame.update(delta);
        }

        // Update escape minigame (legacy)
        if (escapeMinigameActive) {
            updateEscapeMinigame(delta);
        }
    }

    /**
     * Triggered when player has been caught for 2 seconds - player becomes injured
     * Now triggers CAPTURE JUMPSCARE before transitioning to injured state
     */
    private void triggerPlayerInjured() {
        if (playerFullyInjured)
            return; // Already injured

        // Mark as caught to prevent duplicate triggers
        playerBeingCaught = false;
        
        System.out.println("[CAPTURE] Josh caught player! Triggering capture jumpscare...");
        
        // Trigger capture jumpscare FIRST, then execute actual injury logic in callback
        JumpscareManager.getInstance().triggerCaptureJumpscare(() -> {
            // This callback runs AFTER the jumpscare finishes
            executePlayerInjuredState();
        });
    }
    
    /**
     * Actually execute the injured state transition (called after capture jumpscare)
     */
    private void executePlayerInjuredState() {
        if (playerFullyInjured)
            return; // Double-check in case of race condition
            
        playerFullyInjured = true;

        System.out.println("[INJURED] Jumpscare complete. Player is now injured.");

        // Set player state to injured
        player.setState(com.fearjosh.frontend.state.player.InjuredState.getInstance());
        player.setInjured(true);

        // Lose a life
        GameManager gm = GameManager.getInstance();
        gm.loseLife();

        System.out.println("[LIVES] Remaining lives: " + gm.getCurrentLives());
        
        // Notify JoshSpawnController that player was caught
        JoshSpawnController spawnController = gm.getJoshSpawnController();
        if (spawnController != null) {
            spawnController.onPlayerCaught();
        }

        // Check if game over (no lives left)
        if (gm.getCurrentLives() <= 0) {
            triggerGameOver();
            return; // Don't do retreat if game over
        }

        // Make Josh retreat to another room (give player time to bandage)
        if (josh != null) {
            RoomDirector rd = gm.getRoomDirector();
            if (rd != null) {
                rd.onEnemyDespawn();
                rd.forceEnemyRetreat();
            }
            // Notify spawn controller that Josh retreated
            if (spawnController != null) {
                spawnController.onJoshRetreated();
            }
            josh = null; // Despawn physical enemy
            System.out.println("[INJURED] Josh retreated to another room. Player has time to bandage!");
        }
    }

    /**
     * Spawn enemy physically at door based on RoomDirector
     */
    private void spawnEnemyPhysically(RoomDirector rd) {
        // Josh size = 2x player size (menakutkan!)
        float playerW = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_WIDTH;
        float playerH = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_HEIGHT;

        float enemyW = playerW * 2f;
        float enemyH = playerH * 2f;

        float spawnX, spawnY;

        // First, try to get josh spawn point from TMX map
        if (tiledMapManager.hasCurrentMap()) {
            com.fearjosh.frontend.render.TiledMapManager.SpawnInfo joshSpawn = tiledMapManager.getJoshSpawnPoint();
            if (joshSpawn != null) {
                // Use TMX-defined josh spawn point (center the enemy on the spawn point)
                spawnX = joshSpawn.x - enemyW / 2f;
                spawnY = joshSpawn.y - enemyH / 2f;
                System.out.println("[PlayScreen] ✅ Enemy spawned at TMX josh_spawn point (" + spawnX + ", " + spawnY + ")");
            } else {
                // Fallback to RoomDirector position (at door)
                float[] pos = rd.getEnemySpawnPosition(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, enemyW, enemyH, player.getX(),
                        player.getY());
                spawnX = pos[0];
                spawnY = pos[1];
                System.out.println("[PlayScreen] ✅ Enemy spawned via RoomDirector at (" + spawnX + ", " + spawnY + ")");
            }
        } else {
            // No TMX map, use RoomDirector
            float[] pos = rd.getEnemySpawnPosition(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, enemyW, enemyH, player.getX(),
                    player.getY());
            spawnX = pos[0];
            spawnY = pos[1];
            System.out.println("[PlayScreen] ✅ Enemy spawned via RoomDirector (no TMX) at (" + spawnX + ", " + spawnY + ")");
        }

        josh = new Enemy(spawnX, spawnY, enemyW, enemyH);

        // Set TiledMapManager for TMX collision detection
        josh.setTiledMapManager(tiledMapManager);

        System.out.println("[PlayScreen] Enemy size: " + enemyW + "x" + enemyH +
                ", state: " + josh.getCurrentStateType());
    }
    
    /**
     * Spawn enemy at specific position (used by JoshSpawnController)
     * NEW CONTEXT-AWARE SPAWN METHOD
     * 
     * @param x X position to spawn at (from TMX josh_spawn)
     * @param y Y position to spawn at (from TMX josh_spawn)
     */
    private void spawnEnemyAtPosition(float x, float y) {
        // Josh size = 2x player size (menakutkan!)
        float playerW = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_WIDTH;
        float playerH = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_HEIGHT;

        float enemyW = playerW * 2f;
        float enemyH = playerH * 2f;
        
        // Center enemy on spawn point
        float spawnX = x - enemyW / 2f;
        float spawnY = y - enemyH / 2f;
        
        josh = new Enemy(spawnX, spawnY, enemyW, enemyH);
        
        // Set TiledMapManager for TMX collision detection
        josh.setTiledMapManager(tiledMapManager);
        
        System.out.println("[PlayScreen] ✅ Enemy spawned via JoshSpawnController at (" + 
            (int)spawnX + ", " + (int)spawnY + "), size: " + enemyW + "x" + enemyH);
    }

    // ======================
    // INPUT & MOVEMENT (via InputHandler + Command Pattern)
    // ======================

    private void handleInput(float delta) {
        // If injured minigame is active, only handle spacebar
        if (injuredMinigame.isActive()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                injuredMinigame.onSpacebarPressed();
                AudioManager.getInstance().playSound("Audio/Effect/cut_rope_sound_effect.wav");
                System.out.println("[MINIGAME] Space pressed for bandaging!");
            }
            return; // Block all other input during injured minigame
        }
        
        // Legacy escape minigame - only handle spacebar
        if (escapeMinigameActive) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                escapeProgress += ESCAPE_PROGRESS_PER_PRESS;
                escapeSpacebarPresses++;
                AudioManager.getInstance().playSound("Audio/Effect/cut_rope_sound_effect.wav");

                if (escapeProgress > 1f) {
                    escapeProgress = 1f;
                }

                // Play cut rope sound effect

                System.out.println("[MINIGAME] Space pressed! Progress: " + (escapeProgress * 100) + "% ("
                        + escapeSpacebarPresses + " presses)");
            }
            return; // Don't process other input during minigame
        }

        // If player is injured (but minigame not started), only handle F key
        if (playerFullyInjured && injuryPhaseTimer >= INJURY_PHASE_DURATION && !injuredMinigame.isActive()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                System.out.println("[INJURED] Player pressed F! Starting bandage minigame...");
                startInjuredMinigame();
            }
            return; // Don't process any other input when injured
        }

        // 1. Poll input dan execute commands via InputHandler
        inputHandler.update(player, delta);

        // 2. Get movement direction dari InputHandler
        float dx = inputHandler.getMoveDirX();
        float dy = inputHandler.getMoveDirY();
        boolean wasMoving = isMoving;
        isMoving = inputHandler.isMoving();

        // Handle footstep sound
        AudioManager audioManager = AudioManager.getInstance();
        if (isMoving && !wasMoving) {
            // Started moving - play footstep sound
            if (footstepSoundId == -1) {
                footstepSoundId = audioManager.loopSound("Audio/Effect/footstep_sound_effect.wav");
            }
        } else if (!isMoving && wasMoving) {
            // Stopped moving - stop footstep sound
            if (footstepSoundId != -1) {
                audioManager.stopSound("Audio/Effect/footstep_sound_effect.wav", footstepSoundId);
                footstepSoundId = -1;
            }
        }

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

        // === TMX MAP COLLISION ===
        // If TMX map is loaded, use its collision detection
        if (tiledMapManager.hasCurrentMap()) {
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

    // ========================
    // ENEMY COLLISION HELPERS
    // ========================

    /**
     * COLLISION ENEMY: Pakai BODY HITBOX (full body) untuk KEDUA entity
     * - Player: bodyBounds (full body untuk collision dengan enemy)
     * - Enemy: bodyBounds (full body untuk capture detection)
     * 
     * Enemy menangkap player jika BODY hitbox overlap
     * (FOOT hitbox digunakan untuk world collision, bukan capture)
     */
    private boolean checkEnemyPlayerCollision(Enemy enemy, Player player) {
        // GUNAKAN BODY HITBOX untuk kedua entity (full body, bukan foot!)
        com.badlogic.gdx.math.Rectangle playerBody = player.getBodyBounds();
        com.badlogic.gdx.math.Rectangle enemyBody = enemy.getBodyBounds();

        return playerBody.overlaps(enemyBody);
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
        GameManager gm = GameManager.getInstance();
        KeyManager keyMgr = KeyManager.getInstance();
        
        // === TMX DOOR TRANSITION ===
        // Check if player is on a door object defined in the TMX map
        if (tiledMapManager.hasCurrentMap()) {
            com.badlogic.gdx.math.Rectangle footBounds = player.getFootBounds();
            RoomId tmxDoorDestination = tiledMapManager.checkDoorTransition(
                    footBounds.x, footBounds.y, footBounds.width, footBounds.height);

            if (tmxDoorDestination != null && tmxDoorDestination != currentRoomId) {
                // Get door info for checking special doors
                com.fearjosh.frontend.render.TiledMapManager.DoorInfo doorInfo = tiledMapManager.getDoorAt(
                        footBounds.x, footBounds.y, footBounds.width, footBounds.height);
                
                // === SPECIAL DOOR CHECKS FOR KEY-BASED PROGRESSION ===
                
                // MAIN EXIT LOCKED - After meeting Josh, the main exit (hallway -> lobby) is locked
                // This forces player to find alternative escape route (key progression)
                if (currentRoomId == RoomId.HALLWAY && tmxDoorDestination == RoomId.LOBBY && gm.hasMetJosh()) {
                    showFloatingMessage("Akh terkunci! Aku harus cari jalan lain...");
                    transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                    System.out.println("[PlayScreen] MAIN EXIT LOCKED - Player must find alternative route!");
                    return;
                }
                
                // GYM back door - requires gym_key to escape
                if (doorInfo != null && doorInfo.doorName != null && 
                    doorInfo.doorName.toLowerCase().contains("gym_back")) {
                    if (gm.isObjectiveEscape()) {
                        // Check if player has gym key
                        if (keyMgr.hasGymKey()) {
                            // TRIGGER ENDING - Player escapes!
                            System.out.println("[PlayScreen] GYM BACK DOOR - Player escapes! Triggering ending...");
                            triggerGoodEnding();
                            return;
                        } else {
                            showFloatingMessage("Pintunya terkunci. Aku butuh kunci...");
                            transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                            return;
                        }
                    } else {
                        showFloatingMessage("Terkunci dari luar...");
                        transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                        return;
                    }
                }
                
                // Janitor room - requires janitor_key
                if (tmxDoorDestination == RoomId.JANITOR && !keyMgr.isJanitorRoomUnlocked()) {
                    // Check if player has janitor key
                    if (keyMgr.hasJanitorKey()) {
                        // Unlock janitor room
                        keyMgr.unlockJanitorRoom();
                        showFloatingMessage("Ruang penjaga terbuka!");
                        System.out.println("[PlayScreen] Janitor room unlocked with janitor_key!");
                        // Continue to allow transition
                    } else {
                        showFloatingMessage("Ruang penjaga terkunci. Aku butuh kunci...");
                        transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                        return;
                    }
                }
                
                // === STORY MODE RESTRICTIONS ===
                // During STORY state, only allow certain rooms
                if (gm.getCurrentState() == GameManager.GameState.STORY && !gm.hasMetJosh()) {
                    // During story, most doors are "locked" until player reaches gym
                    // Allow: LOBBY -> HALLWAY -> CLASS_1A -> back to HALLWAY -> GYM
                    boolean allowedTransition = false;
                    
                    // Allow transitions on the main story path
                    if (currentRoomId == RoomId.LOBBY && tmxDoorDestination == RoomId.HALLWAY) {
                        allowedTransition = true;
                    } else if (currentRoomId == RoomId.HALLWAY) {
                        // From hallway, allow going to CLASS_1A, GYM, or back to LOBBY
                        if (tmxDoorDestination == RoomId.CLASS_1A || 
                            tmxDoorDestination == RoomId.GYM || 
                            tmxDoorDestination == RoomId.LOBBY) {
                            allowedTransition = true;
                        }
                    } else if (currentRoomId == RoomId.CLASS_1A && tmxDoorDestination == RoomId.HALLWAY) {
                        allowedTransition = true;
                    } else if (currentRoomId == RoomId.GYM && tmxDoorDestination == RoomId.HALLWAY) {
                        allowedTransition = true;
                    }
                    
                    if (!allowedTransition) {
                        showFloatingMessage("Aku harus menjelajahi area utama dulu...");
                        transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                        return;
                    }
                }
                
                // Check if destination room has a TMX map
                if (!tiledMapManager.hasMapForRoom(tmxDoorDestination)) {
                    // Room not implemented yet - show locked message
                    showFloatingMessage("Terkunci...");
                    transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                    return;
                }

                System.out.println("[PlayScreen] TMX Door transition: " + currentRoomId + " -> " + tmxDoorDestination);

                float roomW = getRoomWidth();
                float roomH = getRoomHeight();

                // Determine which wall the door is on (for spawn positioning in new room)
                String doorWall = determineDoorWall(doorInfo, roomW, roomH);
                System.out.println("[PlayScreen] Door wall: " + doorWall);

                // Remember which room we came from
                RoomId previousRoom = currentRoomId;

                switchToRoom(tmxDoorDestination);

                // Position player - first try TMX spawn point, then fallback to wall-based
                positionPlayerForDoorEntry(doorWall, tmxDoorDestination, previousRoom);
                transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                return;
            }
        }

        // Fallback to legacy grid-based transition if no TMX door found
        float cx = player.getCenterX();
        float cy = player.getCenterY();

        float doorMinX = getRoomWidth() / 2f - DOOR_WIDTH / 2f;
        float doorMaxX = getRoomWidth() / 2f + DOOR_WIDTH / 2f;
        float doorMinY = getRoomHeight() / 2f - DOOR_WIDTH / 2f;
        float doorMaxY = getRoomHeight() / 2f + DOOR_WIDTH / 2f;

        boolean moved = false;

        // ATAS
        if (player.getY() + player.getRenderHeight() >= getRoomHeight() - WALL_THICKNESS) {
            RoomId up = currentRoomId.up();
            if (up != null && cx >= doorMinX && cx <= doorMaxX) {
                switchToRoom(up);
                float newY = WALL_THICKNESS + ENTRY_OFFSET;
                player.setY(newY);
                moved = true;
            } else {
                player.setY(getRoomHeight() - player.getRenderHeight() - WALL_THICKNESS);
            }
        }

        // BAWAH
        if (!moved && player.getY() <= WALL_THICKNESS) {
            RoomId down = currentRoomId.down();
            if (down != null && cx >= doorMinX && cx <= doorMaxX) {
                switchToRoom(down);
                float newY = getRoomHeight() - WALL_THICKNESS - player.getRenderHeight() - ENTRY_OFFSET;
                player.setY(newY);
                moved = true;
            } else {
                player.setY(WALL_THICKNESS);
            }
        }

        // KANAN
        if (!moved && player.getX() + player.getRenderWidth() >= getRoomWidth() - WALL_THICKNESS) {
            RoomId right = currentRoomId.right();
            if (right != null && cy >= doorMinY && cy <= doorMaxY) {
                switchToRoom(right);
                float newX = WALL_THICKNESS + ENTRY_OFFSET;
                player.setX(newX);
                moved = true;
            } else {
                player.setX(getRoomWidth() - player.getRenderWidth() - WALL_THICKNESS);
            }
        }

        // KIRI
        if (!moved && player.getX() <= WALL_THICKNESS) {
            RoomId left = currentRoomId.left();
            if (left != null && cy >= doorMinY && cy <= doorMaxY) {
                switchToRoom(left);
                float newX = getRoomWidth() - WALL_THICKNESS - player.getRenderWidth() - ENTRY_OFFSET;
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
    private String determineDoorWall(com.fearjosh.frontend.render.TiledMapManager.DoorInfo doorInfo, float roomW,
            float roomH) {
        if (doorInfo == null)
            return "BOTTOM";

        com.badlogic.gdx.math.Rectangle bounds = doorInfo.bounds;
        float centerX = bounds.x + bounds.width / 2f;
        float centerY = bounds.y + bounds.height / 2f;

        // Check which wall the door is closest to
        float distToTop = roomH - centerY;
        float distToBottom = centerY;
        float distToLeft = centerX;
        float distToRight = roomW - centerX;

        float minDist = Math.min(Math.min(distToTop, distToBottom), Math.min(distToLeft, distToRight));

        if (minDist == distToTop)
            return "TOP";
        if (minDist == distToBottom)
            return "BOTTOM";
        if (minDist == distToLeft)
            return "LEFT";
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
                System.out.println("[PlayScreen] Using TMX spawn point from " + previousRoom + " at (" + spawn.x + ", "
                        + spawn.y + ")");
                player.setX(spawn.x - playerW / 2f);
                player.setY(spawn.y - playerH / 2f);
                return;
            }
        }

        // Fallback to wall-based positioning
        com.badlogic.gdx.math.Rectangle walkable = null;
        if (tiledMapManager.hasCurrentMap()) {
            walkable = tiledMapManager.getWalkableBounds();
        }

        float spawnX = roomW / 2f - playerW / 2f;
        float spawnY = roomH / 2f - playerH / 2f;

        if (walkable != null) {
            switch (exitedWall) {
                case "TOP":
                    spawnY = walkable.y + ENTRY_OFFSET;
                    spawnX = walkable.x + walkable.width / 2f - playerW / 2f;
                    break;
                case "BOTTOM":
                    spawnY = walkable.y + walkable.height - playerH - ENTRY_OFFSET;
                    spawnX = walkable.x + walkable.width / 2f - playerW / 2f;
                    break;
                case "LEFT":
                    spawnX = walkable.x + walkable.width - playerW - ENTRY_OFFSET;
                    spawnY = walkable.y + walkable.height / 2f - playerH / 2f;
                    break;
                case "RIGHT":
                    spawnX = walkable.x + ENTRY_OFFSET;
                    spawnY = walkable.y + walkable.height / 2f - playerH / 2f;
                    break;
            }
        } else {
            switch (exitedWall) {
                case "TOP":
                    spawnY = WALL_THICKNESS + ENTRY_OFFSET;
                    break;
                case "BOTTOM":
                    spawnY = roomH - playerH - WALL_THICKNESS - ENTRY_OFFSET;
                    break;
                case "LEFT":
                    spawnX = roomW - playerW - WALL_THICKNESS - ENTRY_OFFSET;
                    break;
                case "RIGHT":
                    spawnX = WALL_THICKNESS + ENTRY_OFFSET;
                    break;
            }
        }

        player.setX(spawnX);
        player.setY(spawnY);
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
     * Disabled when player is injured
     */
    private void handleInventoryInput() {
        // Don't allow inventory input when injured
        if (playerFullyInjured) {
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
                // Handle KeyItem usage separately (keys are not "usable" but can be used near appropriate objects)
                if (selectedItem instanceof KeyItem) {
                    KeyItem keyItem = (KeyItem) selectedItem;
                    boolean keyUsed = tryUseKeyOnNearbyObject(keyItem);
                    if (keyUsed) {
                        // Remove key from inventory after use
                        inventory.removeItem(inventory.getSelectedSlot());
                        System.out.println("[Inventory] Key used and removed: " + keyItem.getName());
                    } else {
                        showFloatingMessage("Tidak ada benda terkunci di dekat sini...");
                    }
                } else if (selectedItem.isUsable()) {
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
                        } else if (selectedItem instanceof ChocolateItem) {
                            ChocolateItem chocolateItem = (ChocolateItem) selectedItem;
                            // Restore stamina
                            float restoreAmount = chocolateItem.getStaminaRestoreAmount() * player.getMaxStamina();
                            float newStamina = Math.min(player.getMaxStamina(), player.getStamina() + restoreAmount);
                            player.setStamina(newStamina);
                            // Remove chocolate after use (consumed)
                            inventory.removeItem(inventory.getSelectedSlot());
                            System.out.println("[Inventory] Chocolate consumed - stamina restored to: " + newStamina);
                        }
                        // Add more item types here as needed
                    }
                } else {
                    System.out.println("[Inventory] " + selectedItem.getName() + " cannot be used");
                }
            }
        }
    }

    /**
     * Try to use a key on a nearby locked object.
     * @param keyItem The key to use
     * @return true if the key was successfully used, false otherwise
     */
    private boolean tryUseKeyOnNearbyObject(KeyItem keyItem) {
        KeyManager keyMgr = KeyManager.getInstance();
        Inventory inv = GameManager.getInstance().getInventory();
        KeyItem.KeyType keyType = keyItem.getKeyType();
        
        // Check for nearby doors/locked objects based on key type
        switch (keyType) {
            case LOCKER_KEY:
                // Check if near special locker and it's not opened yet
                if (currentTileInteractable != null && 
                    currentTileInteractable.type.equals("special_locker") &&
                    !keyMgr.isSpecialLockerUnlocked()) {
                    keyMgr.unlockSpecialLocker();
                    showFloatingMessage("Loker khusus terbuka! Ada kunci di dalamnya...");
                    // Add janitor key to inventory
                    inv.addItem(new KeyItem(KeyItem.KeyType.JANITOR_KEY));
                    return true;
                }
                // Also check TMX doors
                if (tiledMapManager != null && tiledMapManager.hasCurrentMap()) {
                    TiledMapManager.DoorInfo doorInfo = tiledMapManager.getDoorAt(
                        player.getFootBounds().x, player.getFootBounds().y,
                        player.getFootBounds().width, player.getFootBounds().height);
                    if (doorInfo != null && doorInfo.doorName != null &&
                        doorInfo.doorName.toLowerCase().contains("special_locker")) {
                        keyMgr.unlockSpecialLocker();
                        showFloatingMessage("Loker khusus terbuka! Ada kunci di dalamnya...");
                        inv.addItem(new KeyItem(KeyItem.KeyType.JANITOR_KEY));
                        return true;
                    }
                }
                break;
                
            case JANITOR_KEY:
                // Check if near janitor room door and it's not opened yet
                if (!keyMgr.isJanitorRoomUnlocked()) {
                    if (tiledMapManager != null && tiledMapManager.hasCurrentMap()) {
                        TiledMapManager.DoorInfo doorInfo = tiledMapManager.getDoorAt(
                            player.getFootBounds().x, player.getFootBounds().y,
                            player.getFootBounds().width, player.getFootBounds().height);
                        // Check if door leads to janitor room
                        if (doorInfo != null && doorInfo.targetRoom != null &&
                            doorInfo.targetRoom.equalsIgnoreCase("JANITOR")) {
                            keyMgr.unlockJanitorRoom();
                            showFloatingMessage("Ruang penjaga terbuka!");
                            return true;
                        }
                    }
                }
                break;
                
            case GYM_KEY:
                // Check if near gym back door
                if (tiledMapManager != null && tiledMapManager.hasCurrentMap()) {
                    TiledMapManager.DoorInfo doorInfo = tiledMapManager.getDoorAt(
                        player.getFootBounds().x, player.getFootBounds().y,
                        player.getFootBounds().width, player.getFootBounds().height);
                    if (doorInfo != null && doorInfo.doorName != null &&
                        doorInfo.doorName.toLowerCase().contains("gym_back")) {
                        keyMgr.unlockGymBackDoor();
                        showFloatingMessage("Pintu belakang gym terbuka! Saatnya kabur!");
                        // Don't consume gym key - keep for story purposes
                        return false; // Return false to not remove the key
                    }
                }
                break;
                
            default:
                break;
        }
        
        return false;
    }

    // ======================
    // TILE INTERACTABLES (TMX-based only)
    // ======================

    /**
     * Find the nearest tile interactable (locker) within range
     */
    private void findCurrentTileInteractable() {
        currentTileInteractable = null;

        if (tiledMapManager == null)
            return;

        float px = player.getCenterX();
        float py = player.getCenterY();

        // Use TiledMapManager to find tile interactable at player position
        TiledMapManager.TileInteractable ti = tiledMapManager.getTileInteractableAt(px, py, INTERACT_RANGE);

        // Only show if not already opened
        if (ti != null && !ti.isOpen) {
            currentTileInteractable = ti;
        }
    }

    /**
     * Handle E key press for tile interactables (lockers)
     * Keys are now picked up from exit_key layer automatically - this handles regular lockers only
     */
    private void handleTileInteractInput() {
        if (currentTileInteractable == null)
            return;

        // Already opened, skip
        if (currentTileInteractable.isOpen)
            return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            GameManager gm = GameManager.getInstance();
            
            // === TUTORIAL STATE RESTRICTION ===
            // During tutorial (STORY state before meeting Josh), block tile interactions
            if (gm.isInTutorialState()) {
                showFloatingMessage("Aku harus ke gym dulu...");
                System.out.println("[TileInteract] Blocked in tutorial state - player must meet Josh first");
                return;
            }
            
            System.out.println("[TileInteract] Attempting to open: " + currentTileInteractable.name +
                    " type=" + currentTileInteractable.type);

            // Toggle the tile (open the locker visually)
            boolean toggled = tiledMapManager.toggleTileInteractable(currentTileInteractable);

            if (toggled) {
                // Get inventory from GameManager
                com.fearjosh.frontend.systems.Inventory playerInventory = gm.getInventory();
                
                // Determine what item to give (predefined or random)
                String itemType;
                if (currentTileInteractable.hasItem()) {
                    // Use predefined item from TMX
                    itemType = currentTileInteractable.containedItem.toLowerCase();
                } else {
                    // Random item system: 70% chance to find something
                    double chance = Math.random();
                    if (chance < 0.35) {
                        itemType = "battery";
                    } else if (chance < 0.70) {
                        itemType = "chocolate";
                    } else {
                        itemType = null; // Empty locker
                    }
                }
                
                if (itemType != null) {
                    boolean itemGiven = false;
                    
                    switch (itemType) {
                        case "battery":
                            // Give battery to player's inventory
                            BatteryItem batteryItem = new BatteryItem();
                            if (playerInventory.addItem(batteryItem)) {
                                showFloatingMessage("Dapat Baterai!");
                                System.out.println("[TileInteract] Player found a Battery in the locker!");
                                itemGiven = true;
                            } else {
                                showFloatingMessage("Tas penuh!");
                            }
                            break;
                            
                        case "chocolate":
                            // Give chocolate to player's inventory
                            ChocolateItem chocolateItem = new ChocolateItem();
                            if (playerInventory.addItem(chocolateItem)) {
                                showFloatingMessage("Dapat Cokelat!");
                                System.out.println("[TileInteract] Player found Chocolate in the locker!");
                                itemGiven = true;
                            } else {
                                showFloatingMessage("Tas penuh!");
                            }
                            break;
                            
                        default:
                            showFloatingMessage("Dapat: " + itemType);
                            System.out.println("[TileInteract] Found unknown item: " + itemType);
                            itemGiven = true;
                            break;
                    }
                    
                    if (itemGiven) {
                        currentTileInteractable.itemCollected = true;
                    }
                } else {
                    // Locker is empty
                    showFloatingMessage("Loker kosong...");
                    System.out.println("[TileInteract] Opened locker - empty");
                }
            }
        }
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
     * Hidden when player is injured
     */
    private void renderInventory() {
        // Don't render inventory when injured
        if (playerFullyInjured) {
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
     * Render injury transition phase (first 5 seconds) - black background +
     * overlay
     */
    private void renderInjuryTransitionPhase() {
        uiCamera.update();
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        batch.setProjectionMatrix(uiCamera.combined);

        // Draw black background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 1f);
        shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        shapeRenderer.end();

        // Draw transition overlay (josh_caught_you.jpg)
        if (injuryTransitionAlpha > 0f && injuryTransitionTexture != null) {
            batch.begin();
            Color c = batch.getColor();
            batch.setColor(c.r, c.g, c.b, injuryTransitionAlpha);
            batch.draw(injuryTransitionTexture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            batch.setColor(c.r, c.g, c.b, 1f);
            batch.end();
        }
    }

    /**
     * Render injury transition overlay (josh_caught_you.jpg fade in)
     */
    @SuppressWarnings("unused") // Reserved for injury animation feature
    private void renderInjuryTransition() {
        if (injuryTransitionTexture == null)
            return;

        batch.begin();

        // Set alpha for fade in effect
        Color c = batch.getColor();
        batch.setColor(c.r, c.g, c.b, injuryTransitionAlpha);

        // Draw fullscreen transition image
        batch.draw(injuryTransitionTexture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        // Reset color
        batch.setColor(c.r, c.g, c.b, 1f);

        batch.end();
    }

    /**
     * Render hint box for injured state ("Press F to bandage yourself")
     */
    @SuppressWarnings("unused") // Reserved for injury minigame UI
    private void renderInjuryHintBox() {
        // Only show after transition is complete
        if (injuryTransitionAlpha < 1f)
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
        layout.setText(font, "Tekan F untuk melepaskan diri!");

        float textX = boxX + (boxWidth - layout.width) / 2f;
        float textY = boxY + (boxHeight + layout.height) / 2f;

        font.draw(batch, "Tekan F untuk melepaskan diri!", textX, textY);
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

        // Start gameplay ambient audio
        AudioManager audioManager = AudioManager.getInstance();
        audioManager.playMusic("Audio/Music/background_playing_music.wav", true);
        cricketSoundId = audioManager.loopSound("Audio/Effect/cricket_sound_effect.wav");
        rainSoundId = audioManager.loopSound("Audio/Effect/rain_sound_effect.wav");
        System.out.println("[PlayScreen.show()] Ambient audio started - Music, Cricket, Rain");

        // Reset monster sound timers
        monsterGruntTimer = 0f;
        monsterRoarTimer = 0f;

        // Reset injury state for new game
        playerBeingCaught = false;
        playerFullyInjured = false;
        injuryTimer = 0f;
        injuryTransitionAlpha = 0f;

        System.out.println("[PlayScreen.show()] Injury state reset - playerFullyInjured: " + playerFullyInjured);

        // Ensure player is not in injured state
        if (player != null) {
            player.setInjured(false);
            // Reset to normal state if currently injured
            if (player.getCurrentState() instanceof com.fearjosh.frontend.state.player.InjuredState) {
                player.setState(com.fearjosh.frontend.state.player.NormalState.getInstance());
                System.out.println("[PlayScreen.show()] Player state reset to NormalState");
            }
        }

        // Handle game state transitions properly
        GameManager gm = GameManager.getInstance();
        
        // If coming from STORY state (after intro cutscene), keep STORY
        // If coming from PLAYING state (resume/testing), keep PLAYING
        // Only override if currently not in a gameplay state
        GameManager.GameState currentState = gm.getCurrentState();
        if (currentState != GameManager.GameState.STORY && 
            currentState != GameManager.GameState.PLAYING && 
            !gm.isPaused()) {
            // Coming from somewhere else (CUTSCENE ended), determine correct state
            if (!gm.hasMetJosh()) {
                gm.setCurrentState(GameManager.GameState.STORY);
                tutorialOverlay.start();
                System.out.println("[PlayScreen.show()] Set to STORY state - tutorial started");
            } else {
                gm.setCurrentState(GameManager.GameState.PLAYING);
                System.out.println("[PlayScreen.show()] Set to PLAYING state - Josh already met");
            }
        }

        System.out.println(
                "[PlayScreen.show()] Screen shown - GameState: " + gm.getCurrentState());
    }

    @Override
    public void pause() {
        paused = true;
        GameManager.getInstance().setCurrentState(GameManager.GameState.PAUSED);

        // Pause ambient audio when game is paused
        AudioManager.getInstance().pauseMusic();
    }

    @Override
    public void resume() {
        paused = false;
        GameManager.getInstance().setCurrentState(GameManager.GameState.PLAYING);

        // Resume ambient audio when game is resumed
        AudioManager.getInstance().resumeMusic();
    }

    @Override
    public void hide() {
        // Clean up resources when screen is hidden
        paused = false;

        // Stop all ambient audio when leaving the screen
        AudioManager audioManager = AudioManager.getInstance();
        audioManager.stopMusic();
        if (cricketSoundId != -1) {
            audioManager.stopSound("Audio/Effect/cricket_sound_effect.wav", cricketSoundId);
            cricketSoundId = -1;
        }
        if (rainSoundId != -1) {
            audioManager.stopSound("Audio/Effect/rain_sound_effect.wav", rainSoundId);
            rainSoundId = -1;
        }
        if (footstepSoundId != -1) {
            audioManager.stopSound("Audio/Effect/footstep_sound_effect.wav", footstepSoundId);
            footstepSoundId = -1;
        }
        System.out.println("[PlayScreen.hide()] Ambient audio stopped");
    }

    public void dispose() {
        // Stop ambient audio first
        AudioManager audioManager = AudioManager.getInstance();
        audioManager.stopMusic();
        if (cricketSoundId != -1) {
            audioManager.stopSound("Audio/Effect/cricket_sound_effect.wav", cricketSoundId);
        }
        if (rainSoundId != -1) {
            audioManager.stopSound("Audio/Effect/rain_sound_effect.wav", rainSoundId);
        }
        if (footstepSoundId != -1) {
            audioManager.stopSound("Audio/Effect/footstep_sound_effect.wav", footstepSoundId);
        }

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
        if (injuryTransitionTexture != null) {
            injuryTransitionTexture.dispose();
        }
        if (injuredMinigame != null) {
            injuredMinigame.dispose();
        }
        
        // Dispose jumpscare manager resources
        JumpscareManager.getInstance().dispose();
    }

    // ======================
    // ESCAPE MINIGAME
    // ======================

    // --- INJURED MINIGAME (NEW SYSTEM) ---
    
    /**
     * Start the injured minigame (triggered by pressing F when injured)
     * Uses new InjuredMinigame class with bandaging mechanic
     * The minigame renders IN-PLACE at the player's current position
     */
    private void startInjuredMinigame() {
        // Set player to injured state - this changes the player's sprite to jonatan_injured
        player.setState(com.fearjosh.frontend.state.player.InjuredState.getInstance());
        player.setInjured(true);
        
        // Start minigame at player's current position (in-world rendering)
        injuredMinigame.startAtPosition(
            // On success callback
            () -> {
                System.out.println("[INJURED] Player berhasil membalut lukanya!");
                onInjuredMinigameSuccess();
            },
            // On fail callback
            () -> {
                System.out.println("[INJURED] Player gagal membalut! Press F to try again.");
                onInjuredMinigameFail();
            },
            player.getX(),  // Player's current X position
            player.getY()   // Player's current Y position
        );
        System.out.println("[MINIGAME] Injured minigame started at (" + player.getX() + ", " + player.getY() + ")! Press SPACE rapidly to bandage yourself!");
    }
    
    /**
     * Called when player successfully completes bandaging minigame
     */
    private void onInjuredMinigameSuccess() {
        playerFullyInjured = false;
        injuryPhaseTimer = 0f;

        // Reset player state to normal
        player.setState(com.fearjosh.frontend.state.player.NormalState.getInstance());
        player.setInjured(false);  // Clear injured flag so normal sprite shows

        System.out.println("[INJURED] Player recovered and can move again!");
    }
    
    /**
     * Called when player fails the bandaging minigame
     */
    private void onInjuredMinigameFail() {
        // Player still injured, must try again
        // (life already lost when caught, no need to lose again)
        System.out.println("[INJURED] Bandaging failed! Press F to try again.");
    }

    // --- LEGACY ESCAPE MINIGAME (kept for compatibility) ---

    /**
     * Start the escape minigame (triggered by pressing F when injured)
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
        playerFullyInjured = false;
        injuryPhaseTimer = 0f;

        // Reset player state to normal
        player.setState(com.fearjosh.frontend.state.player.NormalState.getInstance());
        player.setInjured(false);

        System.out.println("[ESCAPE] Player successfully escaped!");
    }

    /**
     * Called when player fails to escape (time ran out)
     */
    private void escapeFailed() {
        escapeMinigameActive = false;
        // Player still injured, must try again
        System.out.println("[ESCAPE] Player failed to escape! Press F to try again.");
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
        String instruction = "TEKAN SPASI UNTUK KABUR!";
        com.badlogic.gdx.graphics.g2d.GlyphLayout instructionLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(
                font, instruction);
        font.draw(batch, instruction, (VIRTUAL_WIDTH - instructionLayout.width) / 2f, barY + barHeight + 40f);

        // Timer
        float timeRemaining = ESCAPE_TIME_LIMIT - escapeTimer;
        String timerText = String.format("Waktu: %.1fd", timeRemaining);
        com.badlogic.gdx.graphics.g2d.GlyphLayout timerLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font,
                timerText);
        font.draw(batch, timerText, (VIRTUAL_WIDTH - timerLayout.width) / 2f, barY - 20f);

        // Progress percentage
        String progressText = String.format("Kemajuan: %d%%", (int) (escapeProgress * 100));
        com.badlogic.gdx.graphics.g2d.GlyphLayout progressLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font,
                progressText);
        font.draw(batch, progressText, (VIRTUAL_WIDTH - progressLayout.width) / 2f, barY + barHeight / 2f + 5f);

        batch.end();
    }

    // ======================
    // ENDING SYSTEM
    // ======================

    /**
     * Trigger good ending - player successfully escapes through gym back door
     */
    private void triggerGoodEnding() {
        System.out.println("[ENDING] Good ending triggered - Player escapes!");
        
        GameManager gm = GameManager.getInstance();
        gm.setCurrentState(GameManager.GameState.ENDING);
        
        // Stop gameplay
        playerFullyInjured = false;
        escapeMinigameActive = false;
        paused = false;
        
        // Despawn enemy
        if (josh != null) {
            RoomDirector rd = gm.getRoomDirector();
            if (rd != null) {
                rd.onEnemyDespawn();
            }
            josh = null;
        }
        
        // Play escape success in-game cutscene first
        inGameCutscene.startCutscene(InGameCutscene.CutsceneType.ESCAPE_SUCCESS, () -> {
            // After in-game cutscene, play ending storyboard cutscene
            System.out.println("[ENDING] Transitioning to ending cutscene...");
            
            // End the current session
            if (gm.getCurrentSession() != null) {
                gm.getCurrentSession().setActive(false);
            }
            
            // Transition to ending cutscene screen
            com.fearjosh.frontend.cutscene.CutsceneManager cutsceneMgr = 
                com.fearjosh.frontend.cutscene.CutsceneManager.getInstance();
            
            // Create main menu screen as the destination after cutscene
            MainMenuScreen mainMenu = new MainMenuScreen(game);
            
            // Play the ending cutscene
            boolean played = cutsceneMgr.playCutscene(game, "ending_good", mainMenu);
            
            if (!played) {
                // Fallback if cutscene not found - go straight to main menu
                System.out.println("[ENDING] Warning: ending_good cutscene not found, going to main menu");
                game.setScreen(mainMenu);
            }
        });
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
        playerFullyInjured = false; // Exit injured state
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

        // Main "TAMAT" text
        String gameOverText = "TAMAT";
        com.badlogic.gdx.graphics.g2d.GlyphLayout gameOverLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font,
                gameOverText);
        float gameOverX = (VIRTUAL_WIDTH - gameOverLayout.width) / 2f;
        float gameOverY = VIRTUAL_HEIGHT / 2f + 50f;
        font.draw(batch, gameOverText, gameOverX, gameOverY);

        // Subtitle text
        String subtitleText = "Josh menangkapmu...";
        com.badlogic.gdx.graphics.g2d.GlyphLayout subtitleLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font,
                subtitleText);
        float subtitleX = (VIRTUAL_WIDTH - subtitleLayout.width) / 2f;
        float subtitleY = VIRTUAL_HEIGHT / 2f;
        font.draw(batch, subtitleText, subtitleX, subtitleY);

        // Timer countdown text
        float timeRemaining = GAME_OVER_DURATION - gameOverTimer;
        String timerText = String.format("Kembali ke menu dalam %.0f detik...", timeRemaining);
        com.badlogic.gdx.graphics.g2d.GlyphLayout timerLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font,
                timerText);
        float timerX = (VIRTUAL_WIDTH - timerLayout.width) / 2f;
        float timerY = VIRTUAL_HEIGHT / 2f - 50f;
        font.draw(batch, timerText, timerX, timerY);

        batch.end();
    }

    // ==================== RoomDirectorEventListener Implementation ====================
    
    /**
     * Called by RoomDirector when camera shake should be triggered
     * (e.g., enemy enters room through door)
     */
    @Override
    public void onCameraShake(float duration, float intensity) {
        if (cameraController != null) {
            cameraController.shake(duration, intensity);
            System.out.println("[PlayScreen] Camera shake triggered: " + duration + "s, intensity " + intensity);
        }
    }
    
    /**
     * Called by RoomDirector when enemy enters through a specific door
     */
    @Override
    public void onDoorEntry(RoomDirector.DoorDirection direction) {
        System.out.println("[PlayScreen] Enemy door entry from: " + direction);
        // Additional visual effects could be added here (flash, etc.)
    }

}
