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

public class PlayScreen implements Screen, RoomDirector.RoomDirectorEventListener, TiledMapManager.ExitDoorListener {

    // Ending flag
    private boolean endingTriggered = false;

    // Transition antar room
    private float transitionCooldown = 0f;
    private static final float TRANSITION_COOLDOWN_DURATION = 0.2f;

    // Virtual resolution
    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;

    // Camera zoom
    private static final float CAMERA_ZOOM = 0.7f;

    private static final float DOOR_WIDTH = 80f;
    private static final float WALL_THICKNESS = 6f;
    private static final float ENTRY_OFFSET = 20f;

    private static final float INTERACT_RANGE = 60f;

    private final FearJosh game;
    private ShapeRenderer shapeRenderer;
    
    private InGameCutscene inGameCutscene;
    
    // Tutorial
    private TutorialOverlay tutorialOverlay;
    
    // Gym encounter
    private boolean gymEncounterTriggered = false;
    
    // Gym trigger zone
    private static final float GYM_TRIGGER_ZONE_SIZE = 150f;
    private SpriteBatch batch;
    private BitmapFont font;

    private Player player;
    private Enemy josh;
    private Texture playerTexture;
    // Vignette
    private Texture vignetteTexture;

    // INJURY
    private boolean playerBeingCaught = false;
    private float injuryTimer = 0f;
    private static final float INJURY_DELAY = 2.0f;
    private boolean playerFullyInjured = false;
    private Texture injuryTransitionTexture;
    private float injuryTransitionAlpha = 0f;
    private static final float INJURY_TRANSITION_DURATION = 1.5f;
    private float injuryPhaseTimer = 0f;
    private static final float INJURY_PHASE_DURATION = 3.0f;

    // MINIGAME
    private InjuredMinigame injuredMinigame;

    // GAME OVER
    private boolean isGameOver = false;

    // Health UI
    private Texture heartTexture;
    private static final float HEART_SIZE = 32f;
    private static final float HEART_SPACING = 8f;

    // AMBIENT AUDIO
    private long cricketSoundId = -1;
    private long rainSoundId = -1;
    private long footstepSoundId = -1;

    // Monster audio
    private float monsterGruntTimer = 0f;
    private static final float MONSTER_GRUNT_INTERVAL = 3.0f;
    private static final float MONSTER_PROXIMITY_RANGE = 700f;

    private float monsterRoarTimer = 0f;
    private static final float MONSTER_ROAR_INTERVAL = 3.0f;
    private static final float MONSTER_ROAR_RANGE = 400f;

    // Inventory UI
    private Texture inventorySlotTexture;
    private Texture inventorySlotSelectedTexture;
    private static final float SLOT_SIZE = 50f;
    private static final float SLOT_SPACING = 4f;
    private static final float INVENTORY_MARGIN_BOTTOM = 20f;
    private static final int INVENTORY_SLOTS = 7;

    // Fog of War
    private FrameBuffer darknessFrameBuffer;
    private Texture lightTexture;

    // Current room
    private RoomId currentRoomId = RoomId.LOBBY;

    // Movement speed
    private static final float WALK_SPEED = 150f;

    // Flashlight
    private static final float BATTERY_MAX = 1f;
    private static final float BATTERY_DRAIN_RATE = 0.08f;
    private float battery = BATTERY_MAX;
    // Interaction
    private com.fearjosh.frontend.render.TiledMapManager.TileInteractable currentTileInteractable = null;

    // Floating message
    private String floatingMessage = null;
    private float floatingMessageTimer = 0f;
    private static final float FLOATING_MESSAGE_DURATION = 2f;

    // Movement flag
    private boolean isMoving = false;

    // Cameras
    private OrthographicCamera worldCamera;
    private Viewport worldViewport;
    private OrthographicCamera uiCamera;

    // Systems
    private CameraController cameraController;
    private LightingRenderer lightingRenderer;
    private HudRenderer hudRenderer;
    private InputHandler inputHandler;
    private TiledMapManager tiledMapManager;

    // Debug
    private static boolean debugHitbox = false;
    private static boolean debugEnemy = false;
    private boolean paused = false;
    
    // Pause menu
    private PauseMenuOverlay pauseMenuOverlay;

    // ROOM DIMENSION
    private float getRoomWidth() {
        return currentRoomId != null ? currentRoomId.getWidth() : VIRTUAL_WIDTH;
    }

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
        
        tiledMapManager.setExitDoorListener(this);
        
        lightingRenderer.setMapManager(tiledMapManager);

        vignetteTexture = new Texture("General/vignette.png");

        injuryTransitionTexture = new Texture("UI/josh_caught_you.jpg");

        // Init injury
        playerBeingCaught = false;
        playerFullyInjured = false;
        injuryTimer = 0f;
        injuryTransitionAlpha = 0f;
        injuryPhaseTimer = 0f;

        isGameOver = false;

        darknessFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, (int) VIRTUAL_WIDTH, (int) VIRTUAL_HEIGHT, false);
        lightTexture = createLightTexture(512);

        GameManager gm = GameManager.getInstance();
        gm.initIfNeeded(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        player = gm.getPlayer();
        playerTexture = new Texture("General/white.png");
        heartTexture = new Texture("UI/HUD/heart.png");
        inventorySlotTexture = createInventorySlotTexture(false);
        inventorySlotSelectedTexture = createInventorySlotTexture(true);

        currentRoomId = gm.getCurrentRoomId();
        switchToRoom(currentRoomId);

        // Initial spawn
        if (tiledMapManager.hasCurrentMap()) {
            com.fearjosh.frontend.render.TiledMapManager.SpawnInfo firstSpawn = tiledMapManager.getFirstSpawnPoint();
            if (firstSpawn != null) {
                player.setX(firstSpawn.x - player.getRenderWidth() / 2f);
                player.setY(firstSpawn.y - player.getRenderHeight() / 2f);
                System.out.println(
                        "[PlayScreen] Positioned player at first spawn: (" + firstSpawn.x + ", " + firstSpawn.y + ")");
            }
        }

        josh = null;
        
        // Init systems
        inGameCutscene = new InGameCutscene();
        injuredMinigame = new InjuredMinigame();
        tutorialOverlay = TutorialOverlay.getInstance();
        KeyManager.getInstance().reset();
        pauseMenuOverlay = new PauseMenuOverlay(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        pauseMenuOverlay.setOnResume(() -> {
            paused = false;
            GameManager.getInstance().setCurrentState(GameManager.GameState.PLAYING);
        });
        pauseMenuOverlay.setOnConfirmQuit(() -> {
            GameManager gm2 = GameManager.getInstance();
            gm2.clearSession();
            gm2.setCurrentState(GameManager.GameState.MAIN_MENU);
            game.setScreen(new MainMenuScreen(game));
        });
        
        // Story mode check
        if (gm.getCurrentState() == GameManager.GameState.STORY) {
            tutorialOverlay.start();
            gymEncounterTriggered = false;
        } else if (gm.hasMetJosh()) {
            gymEncounterTriggered = true;
        }

        cameraController.update(worldCamera, worldViewport, player);
    }

    private void switchToRoom(RoomId id) {
        currentRoomId = id;
        GameManager gm = GameManager.getInstance();
        gm.setCurrentRoomId(id);

        float roomWidth = id.getWidth();
        float roomHeight = id.getHeight();
        cameraController.setWorldBounds(roomWidth, roomHeight);
        tiledMapManager.loadMapForRoom(id);
        restoreConsumedExitKeyTriggers(id);
        gm.notifyPlayerRoomChange(id);

        // Despawn enemy
        if (josh != null) {
            RoomDirector rd = gm.getRoomDirector();
            if (rd != null) rd.onEnemyDespawn();
            JoshSpawnController spawnController = gm.getJoshSpawnController();
            if (spawnController != null) spawnController.onJoshDespawned();
            josh = null;
        }
    }

    // GAME LOOP
    @Override
    public void render(float delta) {
        if (inGameCutscene != null && inGameCutscene.isActive()) {
            inGameCutscene.update(delta);
        }
        
        if (tutorialOverlay != null && tutorialOverlay.isActive()) {
            tutorialOverlay.update(delta);
        }
        
        GameManager gm = GameManager.getInstance();
        boolean canUpdateGameLogic = !paused && !pauseMenuOverlay.isActive() && 
            (gm.isPlaying() || gm.getCurrentState() == GameManager.GameState.STORY);
        
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

        if (isGameOver || endingTriggered) return;

        // Injury transition
        if (playerFullyInjured && injuryPhaseTimer < INJURY_PHASE_DURATION) {
            renderInjuryTransitionPhase();
            return;
        }

        // World render
        worldViewport.apply();
        worldCamera.update();
        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        batch.setProjectionMatrix(worldCamera.combined);

        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        if (tiledMapManager.hasCurrentMap()) {
            batch.end();
            tiledMapManager.renderBelowPlayerExcludingYSorted(worldCamera);
            batch.begin();
        }

        batch.end();

        // Lighting
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        lightingRenderer.render(shapeRenderer, player, player.isFlashlightOn(), battery / BATTERY_MAX);

        // Enemy render
        if (josh != null && !josh.isDespawned()) {
            shapeRenderer.end();
            batch.setProjectionMatrix(worldCamera.combined);
            batch.begin();
            josh.render(batch);
            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            if (debugEnemy) {
                josh.renderDebugEnhanced(shapeRenderer);
            }
        }

        shapeRenderer.end();

        // Player + entities
        boolean shouldRenderPlayer = !injuredMinigame.isActive();
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

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

            tiledMapManager.renderPhysicalLockerKey(batch);
            batch.end();
            tiledMapManager.renderAbovePlayer(worldCamera);
            batch.begin();
        } else {
            if (shouldRenderPlayer) {
                TextureRegion frame = player.getCurrentFrame(isMoving);
                batch.draw(frame,
                        player.getX(),
                        player.getY(),
                        player.getRenderWidth(),
                        player.getRenderHeight());
            }
        }

        // Prompt E
        if (currentTileInteractable != null && !injuredMinigame.isActive()) {
            font.draw(batch, "E",
                    currentTileInteractable.getCenterX() - 4,
                    currentTileInteractable.getCenterY() + 30);
        }

        // Floating msg
        if (floatingMessage != null && floatingMessageTimer > 0 && !injuredMinigame.isActive()) {
            font.draw(batch,
                    floatingMessage,
                    player.getCenterX() - floatingMessage.length() * 3,
                    player.getY() + player.getRenderHeight() + 20);
        }

        batch.end();

        renderDarknessLayer();

        // HUD
        uiCamera.update();
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        batch.setProjectionMatrix(uiCamera.combined);

        if (!playerFullyInjured || injuryPhaseTimer < INJURY_PHASE_DURATION) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            hudRenderer.render(shapeRenderer,
                    VIRTUAL_WIDTH,
                    VIRTUAL_HEIGHT,
                    player.getStamina(),
                    player.getMaxStamina(),
                    battery,
                    BATTERY_MAX);

            if (debugHitbox && GameManager.getInstance().isPlaying()) {
                player.debugRenderHitboxes(shapeRenderer);
            }

            shapeRenderer.end();

            batch.setProjectionMatrix(uiCamera.combined);
            renderHealthBar();
            batch.setProjectionMatrix(uiCamera.combined);
            renderInventory();
        }
        if (playerFullyInjured && injuryPhaseTimer >= INJURY_PHASE_DURATION && !injuredMinigame.isActive()) {
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

        // INJURED MINIGAME
        if (injuredMinigame.isActive()) {
            shapeRenderer.setProjectionMatrix(uiCamera.combined);
            batch.setProjectionMatrix(uiCamera.combined);
            injuredMinigame.render(shapeRenderer, batch, font, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        }

        // Pause click
        if (GameManager.getInstance().isPlaying() && !pauseMenuOverlay.isActive() && Gdx.input.justTouched()) {
            float screenX = Gdx.input.getX();
            float screenY = Gdx.input.getY();
            com.badlogic.gdx.math.Vector3 uiCoords = uiCamera
                    .unproject(new com.badlogic.gdx.math.Vector3(screenX, screenY, 0));
            com.badlogic.gdx.math.Rectangle r = hudRenderer.getPauseButtonBounds();
            if (r.contains(uiCoords.x, uiCoords.y)) {
                paused = true;
                pauseMenuOverlay.show();
                GameManager.getInstance().setCurrentState(GameManager.GameState.PAUSED);
            }
        }
        
        // ESC to pause
        if (GameManager.getInstance().isPlaying() && !pauseMenuOverlay.isActive() && 
            Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = true;
            pauseMenuOverlay.show();
            GameManager.getInstance().setCurrentState(GameManager.GameState.PAUSED);
        }

        // Pause menu
        if (pauseMenuOverlay.isActive()) {
            pauseMenuOverlay.update(uiCamera);
            batch.setProjectionMatrix(uiCamera.combined);
            pauseMenuOverlay.render(shapeRenderer, batch, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        }
        
        // Tutorial overlay
        if (tutorialOverlay != null && tutorialOverlay.isActive()) {
            tutorialOverlay.render(shapeRenderer, batch, font, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        }
        
        // Cutscene
        if (inGameCutscene != null && inGameCutscene.isActive()) {
            inGameCutscene.render(shapeRenderer, batch, font, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        }
        
        // Difficulty/room info
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        hudRenderer.renderText(batch, font, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        batch.end();
        
        // Objective hint
        if (GameManager.getInstance().isObjectiveEscape() && !paused && !isGameOver && 
            (inGameCutscene == null || !inGameCutscene.isActive())) {
            renderObjectiveHint();
        }
        
        // Difficulty text
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        hudRenderer.renderDifficultyText(batch, font);
        batch.end();

        // Jumpscare
        if (JumpscareManager.getInstance().isJumpscareActive()) {
            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            JumpscareManager.getInstance().render(batch, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            batch.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
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
            
            // Exit door check
            checkExitDoorTrigger();
        }
        
        checkGymEncounterTrigger();

        updateBattery(delta);
        findCurrentTileInteractable();
        
        checkExitKeyTriggers();
        
        checkAutoKeyTrigger();
        
        handleTileInteractInput();
        handleInventoryInput();

        boolean jumpscareBlocked = paused || pauseMenuOverlay.isActive() || 
            (inGameCutscene != null && inGameCutscene.isActive()) ||
            injuredMinigame.isActive() || tutorialOverlay.isActive();
        JumpscareManager.getInstance().update(delta, !jumpscareBlocked);
        
        JumpscareManager.getInstance().setInHallway(currentRoomId == RoomId.HALLWAY);

        if (GameManager.getInstance().hasMetJosh()) {
            updateRoomDirector(delta);
        }

        cameraController.update(worldCamera, worldViewport, player);
    }
    
    private void checkGymEncounterTrigger() {
        GameManager gm = GameManager.getInstance();
        
        if (gymEncounterTriggered || gm.hasMetJosh()) {
            return;
        }
        
        if (currentRoomId != RoomId.GYM) {
            return;
        }
        
        float gymCenterX = getRoomWidth() / 2f;
        float gymCenterY = getRoomHeight() / 2f;
        
        float playerCenterX = player.getCenterX();
        float playerCenterY = player.getCenterY();
        
        float dx = playerCenterX - gymCenterX;
        float dy = playerCenterY - gymCenterY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance < GYM_TRIGGER_ZONE_SIZE) {
            gymEncounterTriggered = true;
            System.out.println("[PlayScreen] GYM ENCOUNTER TRIGGERED! Starting cutscene...");
            
            if (tutorialOverlay != null && tutorialOverlay.isActive()) {
                tutorialOverlay.skip();
            }
            
            inGameCutscene.startCutscene(InGameCutscene.CutsceneType.GYM_ENCOUNTER, () -> {
                System.out.println("[PlayScreen] Gym encounter complete, showing objective change...");
                
                inGameCutscene.startCutscene(InGameCutscene.CutsceneType.OBJECTIVE_CHANGE, () -> {
                    System.out.println("[PlayScreen] Starting injured minigame TUTORIAL (no health loss)...");
                    
                    startInjuredMinigameTutorial();
                });
            });
        }
    }
    
    private void startInjuredMinigameTutorial() {
        System.out.println("[Tutorial] Injured minigame tutorial started - player learns bandage mechanic");
        
        injuredMinigame.start(
            () -> {
                System.out.println("[Tutorial] Player completed injured minigame tutorial!");
                onInjuredTutorialComplete();
            },
            () -> {
                System.out.println("[Tutorial] Player failed tutorial - letting them retry...");
                startInjuredMinigameTutorial();
            },
            true
        );
    }
    
    private void onInjuredTutorialComplete() {
        System.out.println("[Tutorial] Injured tutorial complete - transitioning to full gameplay");
        
        GameManager gm = GameManager.getInstance();
        gm.transitionToFullPlayMode();
        
        showFloatingMessage("Kamu sudah belajar cara membalut luka!");
    }
    
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
    
    private void checkExitKeyTriggers() {
        if (tiledMapManager == null) return;
        
        GameManager gm = GameManager.getInstance();
        KeyManager keyMgr = KeyManager.getInstance();
        
        if (gm.isInTutorialState()) {
            return;
        }
        
        Rectangle playerBounds = player.getFootBounds();
        TiledMapManager.ExitKeyTrigger trigger = tiledMapManager.checkExitKeyTrigger(playerBounds);
        
        if (trigger == null) return;
        
        if (keyMgr.isExitKeyTriggerConsumed(currentRoomId, trigger.objectId)) {
            trigger.consumed = true; // Sync local state
            return;
        }
        
        String itemType = trigger.itemType.toLowerCase();
        Inventory inventory = gm.getInventory();
        
        System.out.println("[ExitKeyTrigger] Player overlapping trigger: " + itemType + 
            " in room=" + currentRoomId + " objectId=" + trigger.objectId);
        
        if (itemType.equals("locker_key")) {
            if (!keyMgr.hasLockerKey()) {
                KeyItem keyItem = new KeyItem(KeyItem.KeyType.LOCKER_KEY);
                if (inventory.addItem(keyItem)) {
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
        
        if (itemType.equals("janitor_key")) {
            if (keyMgr.hasJanitorKey()) {
                return;
            }
            
            if (!keyMgr.hasLockerKey()) {
                showFloatingMessage("Loker ini butuh kunci...");
                System.out.println("[ExitKeyTrigger] janitor_key trigger blocked - missing LOCKER_KEY");
                return;
            }
            
            KeyItem janitorKey = new KeyItem(KeyItem.KeyType.JANITOR_KEY);
            if (inventory.addItem(janitorKey)) {
                keyMgr.useKey(KeyItem.KeyType.LOCKER_KEY);
                keyMgr.unlockSpecialLocker();
                
                tiledMapManager.consumeExitKeyTrigger(trigger);
                keyMgr.markExitKeyTriggerConsumed(currentRoomId, trigger.objectId);
                
                showFloatingMessage("Pakai Kunci Loker! Dapat: Kunci Ruang Penjaga!");
                System.out.println("[ExitKeyTrigger] Consumed LOCKER_KEY, granted JANITOR_KEY");
            } else {
                showFloatingMessage("Tas penuh!");
            }
            return;
        }
        
        if (itemType.equals("gym_key")) {
            if (!keyMgr.hasGymKey()) {
                KeyItem gymKey = new KeyItem(KeyItem.KeyType.GYM_KEY);
                if (inventory.addItem(gymKey)) {
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
        
        System.out.println("[ExitKeyTrigger] Unknown trigger item type: " + itemType);
    }
    
    private void checkAutoKeyTrigger() {
        if (tiledMapManager == null || !tiledMapManager.hasCurrentMap()) return;
        
        GameManager gm = GameManager.getInstance();
        KeyManager keyMgr = KeyManager.getInstance();
        
        if (gm.isInTutorialState()) return;
        
        TiledMapManager.DoorInfo doorInfo = tiledMapManager.getDoorAt(
            player.getFootBounds().x, player.getFootBounds().y,
            player.getFootBounds().width, player.getFootBounds().height);
        
        if (keyMgr.hasJanitorKey() && !keyMgr.isJanitorRoomUnlocked()) {
            if (doorInfo != null && doorInfo.targetRoom != null &&
                doorInfo.targetRoom.equalsIgnoreCase("JANITOR")) {
                System.out.println("[AutoKey] Unlocking janitor door with JANITOR_KEY...");
                keyMgr.unlockJanitorRoom();
                
                showFloatingMessage("Ruang Penjaga terbuka!");
                return;
            }
        }
        
        if (keyMgr.hasGymKey() && !keyMgr.isGymBackDoorUnlocked()) {
            if (doorInfo != null && doorInfo.doorName != null &&
                doorInfo.doorName.toLowerCase().contains("gym_back")) {
                System.out.println("[AutoKey] Using GYM_KEY at gym back door...");
                keyMgr.unlockGymBackDoor();
                
                showFloatingMessage("Pintu Belakang Gym terbuka! Waktunya kabur...");
                return;
            }
        }
    }

    private void checkExitDoorTrigger() {
        if (endingTriggered) return;
        
        GameManager gm = GameManager.getInstance();
        if (!gm.isPlaying()) return;
        
        if (tiledMapManager != null && tiledMapManager.hasCurrentMap()) {
            com.badlogic.gdx.math.Rectangle footBounds = player.getFootBounds();
            tiledMapManager.checkExitDoorOverlap(
                footBounds.x, footBounds.y, footBounds.width, footBounds.height);
        }
    }

    @Override
    public void onExitDoorOverlap(TiledMapManager.DoorInfo doorInfo, String currentMapName) {
        if (endingTriggered) {
            System.out.println("[ExitDoor] Ending already triggered, ignoring...");
            return;
        }
        
        System.out.println("[ExitDoor] Exit door overlap detected! Door: " + doorInfo.doorName + 
            ", Map: " + currentMapName);
        
        KeyManager keyMgr = KeyManager.getInstance();
        
        if (keyMgr.hasGymKey()) {
            System.out.println("[ExitDoor] Player has gym_key - TRIGGERING ESCAPE ENDING!");
            endingTriggered = true;
            triggerEscapeEnding();
        } else {
            showFloatingMessage("Pintu ini terkunci. Aku butuh kunci...");
            System.out.println("[ExitDoor] Exit door locked - player needs gym_key");
        }
    }

    private void triggerEscapeEnding() {
        System.out.println("[ENDING] Escape ending triggered via exit door!");
        
        GameManager gm = GameManager.getInstance();
        gm.setCurrentState(GameManager.GameState.ENDING);
        
        playerFullyInjured = false;
        paused = false;
        
        stopAmbientAudio();
        
        if (josh != null) {
            RoomDirector rd = gm.getRoomDirector();
            if (rd != null) {
                rd.onEnemyDespawn();
            }
            josh = null;
        }
        
        if (gm.getCurrentSession() != null) {
            gm.getCurrentSession().setActive(false);
        }
        
        game.setScreen(new EndingCutsceneScreen(game));
    }

    private void stopAmbientAudio() {
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
        
        System.out.println("[PlayScreen] Ambient audio stopped for ending transition");
    }

    private void updateRoomDirector(float delta) {
        GameManager gm = GameManager.getInstance();
        RoomDirector rd = gm.getRoomDirector();
        JoshSpawnController spawnController = gm.getJoshSpawnController();

        if (rd == null)
            return;
            
        if (rd.getEventListener() == null) {
            rd.setEventListener(this);
        }
        
        if (spawnController != null) {
            spawnController.update(delta);
        }
        if (!playerFullyInjured) {
            rd.update(delta);

            // Josh spawn
            if (josh == null && spawnController != null) {
                boolean canSpawn = gm.isPlaying() && 
                                   !injuredMinigame.isActive() && 
                                   (inGameCutscene == null || !inGameCutscene.isActive());
                
                JoshSpawnController.SpawnDecision decision = spawnController.shouldSpawnJosh(
                    tiledMapManager,
                    player.getCenterX(),
                    player.getCenterY(),
                    canSpawn
                );
                
                if (decision.shouldSpawn) {
                    spawnEnemyAtPosition(decision.spawnX, decision.spawnY);
                    spawnController.onJoshSpawned(currentRoomId);
                    
                    System.out.println("[JoshSpawn] ✅ " + decision.reason + 
                        " at (" + (int)decision.spawnX + ", " + (int)decision.spawnY + ")");
                } else if (com.fearjosh.frontend.config.Constants.DEBUG_ROOM_DIRECTOR) {
                    if (Math.random() < 0.01) {
                        System.out.println("[JoshSpawn] ❌ " + decision.reason);
                    }
                }
            }
        }

        // Update enemy
        if (josh != null && !josh.isDespawned() && !playerFullyInjured) {
            josh.update(player, delta);
            
            if (spawnController != null && 
                josh.getCurrentStateType() == com.fearjosh.frontend.state.enemy.EnemyStateType.CHASING) {
                spawnController.onPlayerChased();
            }

            // Monster proximity
            float dx = josh.getCenterX() - player.getCenterX();
            float dy = josh.getCenterY() - player.getCenterY();
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            monsterGruntTimer += delta;

            if (distance < MONSTER_PROXIMITY_RANGE && monsterGruntTimer >= MONSTER_GRUNT_INTERVAL) {
                float volumeMultiplier = 1.0f - (distance / MONSTER_PROXIMITY_RANGE) * 0.8f;
                volumeMultiplier = Math.max(0.2f, Math.min(1.0f, volumeMultiplier));

                AudioManager.getInstance().playSound("Audio/Effect/monster_grunt_sound_effect.wav", volumeMultiplier);
                monsterGruntTimer = 0f; // Reset timer

                if (com.fearjosh.frontend.config.Constants.DEBUG_ROOM_DIRECTOR) {
                    System.out.println(
                            "[Monster] Grunt at distance: " + (int) distance + ", volume: " + volumeMultiplier);
                }
            }

            // Monster roar
            monsterRoarTimer += delta;

            if (josh.getCurrentStateType() == com.fearjosh.frontend.state.enemy.EnemyStateType.CHASING
                    && distance < MONSTER_ROAR_RANGE
                    && monsterRoarTimer >= MONSTER_ROAR_INTERVAL) {
                float roarVolume = 1.0f - (distance / MONSTER_ROAR_RANGE) * 0.7f;
                roarVolume = Math.max(0.3f, Math.min(1.0f, roarVolume));

                AudioManager.getInstance().playSound("Audio/Effect/monster_roar_sound_effect.wav", roarVolume);
                monsterRoarTimer = 0f; // Reset timer

                if (com.fearjosh.frontend.config.Constants.DEBUG_ROOM_DIRECTOR) {
                    System.out.println(
                            "[Monster] ROAR at distance: " + (int) distance + ", volume: " + roarVolume + " [CHASING]");
                }
            }

            // Injury collision
            if (checkEnemyPlayerCollision(josh, player)) {
                if (!playerBeingCaught && !playerFullyInjured) {
                    playerBeingCaught = true;
                    injuryTimer = 0f;
                    System.out.println("[INJURY] Josh caught player! Timer started...");
                } else if (playerBeingCaught) {
                    injuryTimer += delta;

                    if (injuryTimer >= INJURY_DELAY) {
                        triggerPlayerInjured();
                    }
                }
            } else {
                if (playerBeingCaught && !playerFullyInjured) {
                    System.out.println("[INJURY] Player escaped from Josh!");
                    playerBeingCaught = false;
                    injuryTimer = 0f;
                }
            }

            if (josh != null) {
                clampEnemyToRoom(josh);
            }
        }

        if (playerFullyInjured) {
            injuryPhaseTimer += delta;

            if (injuryTransitionAlpha < 1f && injuryPhaseTimer < INJURY_TRANSITION_DURATION) {
                injuryTransitionAlpha += delta / INJURY_TRANSITION_DURATION;
                if (injuryTransitionAlpha > 1f) {
                    injuryTransitionAlpha = 1f;
                }
            }
        }

        if (injuredMinigame.isActive()) {
            injuredMinigame.update(delta);
        }
    }

    private void triggerPlayerInjured() {
        if (playerFullyInjured)
            return;

        playerBeingCaught = false;
        
        System.out.println("[CAPTURE] Josh caught player! Triggering capture jumpscare...");
        
        JumpscareManager.getInstance().triggerCaptureJumpscare(() -> {
            executePlayerInjuredState();
        });
    }
    
    private void executePlayerInjuredState() {
        if (playerFullyInjured)
            return;
            
        playerFullyInjured = true;

        System.out.println("[INJURED] Jumpscare complete. Player is now injured.");

        player.setState(com.fearjosh.frontend.state.player.InjuredState.getInstance());
        player.setInjured(true);

        GameManager gm = GameManager.getInstance();
        gm.loseLife();

        System.out.println("[LIVES] Remaining lives: " + gm.getCurrentLives());
        
        JoshSpawnController spawnController = gm.getJoshSpawnController();
        if (spawnController != null) {
            spawnController.onPlayerCaught();
        }

        if (gm.getCurrentLives() <= 0) {
            triggerGameOver();
            return;
        }

        // Josh retreat
        if (josh != null) {
            RoomDirector rd = gm.getRoomDirector();
            if (rd != null) {
                rd.onEnemyDespawn();
                rd.forceEnemyRetreat();
            }
            if (spawnController != null) {
                spawnController.onJoshRetreated();
            }
            josh = null;
            System.out.println("[INJURED] Josh retreated to another room. Player has time to bandage!");
        }
    }

    private void spawnEnemyAtPosition(float x, float y) {
        float playerW = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_WIDTH;
        float playerH = com.fearjosh.frontend.config.Constants.PLAYER_RENDER_HEIGHT;

        float enemyW = playerW * 2f;
        float enemyH = playerH * 2f;
        
        float spawnX = x - enemyW / 2f;
        float spawnY = y - enemyH / 2f;
        
        josh = new Enemy(spawnX, spawnY, enemyW, enemyH);
        josh.setTiledMapManager(tiledMapManager);
        
        System.out.println("[PlayScreen] ✅ Enemy spawned via JoshSpawnController at (" + 
            (int)spawnX + ", " + (int)spawnY + "), size: " + enemyW + "x" + enemyH);
    }

    private void handleInput(float delta) {
        if (injuredMinigame.isActive()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                injuredMinigame.onSpacebarPressed();
                AudioManager.getInstance().playSound("Audio/Effect/cut_rope_sound_effect.wav");
                System.out.println("[MINIGAME] Space pressed for bandaging!");
            }
            return;
        }

        if (playerFullyInjured && injuryPhaseTimer >= INJURY_PHASE_DURATION && !injuredMinigame.isActive()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                System.out.println("[INJURED] Player pressed F! Starting bandage minigame...");
                startInjuredMinigame();
            }
            return;
        }

        inputHandler.update(player, delta);

        float dx = inputHandler.getMoveDirX();
        float dy = inputHandler.getMoveDirY();
        boolean wasMoving = isMoving;
        isMoving = inputHandler.isMoving();

        // Footstep
        AudioManager audioManager = AudioManager.getInstance();
        if (isMoving && !wasMoving) {
            if (footstepSoundId == -1) {
                footstepSoundId = audioManager.loopSound("Audio/Effect/footstep_sound_effect.wav");
            }
        } else if (!isMoving && wasMoving) {
            if (footstepSoundId != -1) {
                audioManager.stopSound("Audio/Effect/footstep_sound_effect.wav", footstepSoundId);
                footstepSoundId = -1;
            }
        }

        float baseSpeed = WALK_SPEED;
        float speed = baseSpeed * player.getSpeedMultiplier() * delta;

        if (isMoving) {
            float mx = dx * speed;
            float my = dy * speed;
            float oldX = player.getX();
            float oldY = player.getY();

            player.move(mx, my);

            // Collision slide
            if (collidesWithFurniture(player)) {
                player.setX(oldX + mx);
                player.setY(oldY);
                boolean collideX = collidesWithFurniture(player);

                player.setX(oldX);
                player.setY(oldY + my);
                boolean collideY = collidesWithFurniture(player);

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
    }

    private boolean collidesWithFurniture(Player p) {
        com.badlogic.gdx.math.Rectangle footBounds = p.getFootBounds();

        // TMX collision
        if (tiledMapManager.hasCurrentMap()) {
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

    private boolean checkEnemyPlayerCollision(Enemy enemy, Player player) {
        com.badlogic.gdx.math.Rectangle playerBody = player.getBodyBounds();
        com.badlogic.gdx.math.Rectangle enemyBody = enemy.getBodyBounds();

        return playerBody.overlaps(enemyBody);
    }

    private void clampEnemyToRoom(Enemy enemy) {
        float ex = enemy.getX();
        float ey = enemy.getY();
        float ew = enemy.getWidth();
        float eh = enemy.getHeight();

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
        
        // Door transition
        if (tiledMapManager.hasCurrentMap()) {
            com.badlogic.gdx.math.Rectangle footBounds = player.getFootBounds();
            RoomId tmxDoorDestination = tiledMapManager.checkDoorTransition(
                    footBounds.x, footBounds.y, footBounds.width, footBounds.height);

            if (tmxDoorDestination != null && tmxDoorDestination != currentRoomId) {
                com.fearjosh.frontend.render.TiledMapManager.DoorInfo doorInfo = tiledMapManager.getDoorAt(
                        footBounds.x, footBounds.y, footBounds.width, footBounds.height);
                
                // Locked doors
                if (currentRoomId == RoomId.HALLWAY && tmxDoorDestination == RoomId.LOBBY && gm.hasMetJosh()) {
                    showFloatingMessage("Akh terkunci! Aku harus cari jalan lain...");
                    transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                    System.out.println("[PlayScreen] MAIN EXIT LOCKED - Player must find alternative route!");
                    return;
                }
                
                // Gym back door
                if (doorInfo != null && doorInfo.doorName != null && 
                    doorInfo.doorName.toLowerCase().contains("gym_back")) {
                    if (gm.isObjectiveEscape()) {
                        if (keyMgr.hasGymKey()) {
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
                
                // Janitor room
                if (tmxDoorDestination == RoomId.JANITOR && !keyMgr.isJanitorRoomUnlocked()) {
                    if (keyMgr.hasJanitorKey()) {
                        keyMgr.unlockJanitorRoom();
                        showFloatingMessage("Ruang penjaga terbuka!");
                        System.out.println("[PlayScreen] Janitor room unlocked with janitor_key!");
                    } else {
                        showFloatingMessage("Ruang penjaga terkunci. Aku butuh kunci...");
                        transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                        return;
                    }
                }
                // Story state check
                if (gm.getCurrentState() == GameManager.GameState.STORY && !gm.hasMetJosh()) {
                    boolean allowedTransition = false;
                    
                    if (currentRoomId == RoomId.LOBBY && tmxDoorDestination == RoomId.HALLWAY) {
                        allowedTransition = true;
                    } else if (currentRoomId == RoomId.HALLWAY) {
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
                
                // Check TMX map
                if (!tiledMapManager.hasMapForRoom(tmxDoorDestination)) {
                    showFloatingMessage("Terkunci...");
                    transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                    return;
                }

                System.out.println("[PlayScreen] TMX Door transition: " + currentRoomId + " -> " + tmxDoorDestination);

                float roomW = getRoomWidth();
                float roomH = getRoomHeight();

                // Door wall
                String doorWall = determineDoorWall(doorInfo, roomW, roomH);
                System.out.println("[PlayScreen] Door wall: " + doorWall);

                // Previous room
                RoomId previousRoom = currentRoomId;

                switchToRoom(tmxDoorDestination);

                // Position player
                positionPlayerForDoorEntry(doorWall, tmxDoorDestination, previousRoom);
                transitionCooldown = TRANSITION_COOLDOWN_DURATION;
                return;
            }
        }

        // Legacy fallback
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

    private void showFloatingMessage(String message) {
        this.floatingMessage = message;
        this.floatingMessageTimer = FLOATING_MESSAGE_DURATION;
    }

    private String determineDoorWall(com.fearjosh.frontend.render.TiledMapManager.DoorInfo doorInfo, float roomW,
            float roomH) {
        if (doorInfo == null)
            return "BOTTOM";

        com.badlogic.gdx.math.Rectangle bounds = doorInfo.bounds;
        float centerX = bounds.x + bounds.width / 2f;
        float centerY = bounds.y + bounds.height / 2f;

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

    private void positionPlayerForDoorEntry(String exitedWall, RoomId newRoom, RoomId previousRoom) {
        float roomW = getRoomWidth();
        float roomH = getRoomHeight();
        float playerW = player.getRenderWidth();
        float playerH = player.getRenderHeight();

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
        if (GameManager.getInstance().isTestingMode()) {
            battery = BATTERY_MAX;
            if (player.isFlashlightOn()) {
                player.setFlashlightOn(true);
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

    public void rechargeBattery(float amount) {
        battery = Math.min(BATTERY_MAX, battery + amount);
        System.out.println("[PlayScreen] Battery recharged to: " + battery);
    }

    private void handleInventoryInput() {
        if (playerFullyInjured) {
            return;
        }

        GameManager gm = GameManager.getInstance();
        Inventory inventory = gm.getInventory();

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

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            Item selectedItem = inventory.getSelectedItem();
            System.out.println("[Inventory] Q pressed - Selected slot: " + inventory.getSelectedSlot() + ", Item: "
                    + (selectedItem != null ? selectedItem.getName() : "null"));

            if (selectedItem != null) {
                if (selectedItem instanceof KeyItem) {
                    KeyItem keyItem = (KeyItem) selectedItem;
                    boolean keyUsed = tryUseKeyOnNearbyObject(keyItem);
                    if (keyUsed) {
                        inventory.removeItem(inventory.getSelectedSlot());
                        System.out.println("[Inventory] Key used and removed: " + keyItem.getName());
                    } else {
                        showFloatingMessage("Tidak ada benda terkunci di dekat sini...");
                    }
                } else if (selectedItem.isUsable()) {
                    boolean success = selectedItem.useItem();
                    System.out.println("[Inventory] Item use result: " + success);
                    if (success) {
                        if (selectedItem instanceof BatteryItem) {
                            BatteryItem batteryItem = (BatteryItem) selectedItem;
                            rechargeBattery(batteryItem.getRechargeAmount());
                            inventory.removeItem(inventory.getSelectedSlot());
                            System.out.println("[Inventory] Battery consumed and removed");
                        } else if (selectedItem instanceof ChocolateItem) {
                            ChocolateItem chocolateItem = (ChocolateItem) selectedItem;
                            float restoreAmount = chocolateItem.getStaminaRestoreAmount() * player.getMaxStamina();
                            float newStamina = Math.min(player.getMaxStamina(), player.getStamina() + restoreAmount);
                            player.setStamina(newStamina);
                            inventory.removeItem(inventory.getSelectedSlot());
                            System.out.println("[Inventory] Chocolate consumed - stamina restored to: " + newStamina);
                        }
                    }
                } else {
                    System.out.println("[Inventory] " + selectedItem.getName() + " cannot be used");
                }
            }
        }
    }

    private boolean tryUseKeyOnNearbyObject(KeyItem keyItem) {
        KeyManager keyMgr = KeyManager.getInstance();
        Inventory inv = GameManager.getInstance().getInventory();
        KeyItem.KeyType keyType = keyItem.getKeyType();
        
        switch (keyType) {
            case LOCKER_KEY:
                if (currentTileInteractable != null && 
                    currentTileInteractable.type.equals("special_locker") &&
                    !keyMgr.isSpecialLockerUnlocked()) {
                    keyMgr.unlockSpecialLocker();
                    showFloatingMessage("Loker khusus terbuka! Ada kunci di dalamnya...");
                    inv.addItem(new KeyItem(KeyItem.KeyType.JANITOR_KEY));
                    return true;
                }
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
                if (!keyMgr.isJanitorRoomUnlocked()) {
                    if (tiledMapManager != null && tiledMapManager.hasCurrentMap()) {
                        TiledMapManager.DoorInfo doorInfo = tiledMapManager.getDoorAt(
                            player.getFootBounds().x, player.getFootBounds().y,
                            player.getFootBounds().width, player.getFootBounds().height);
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
                if (tiledMapManager != null && tiledMapManager.hasCurrentMap()) {
                    TiledMapManager.DoorInfo doorInfo = tiledMapManager.getDoorAt(
                        player.getFootBounds().x, player.getFootBounds().y,
                        player.getFootBounds().width, player.getFootBounds().height);
                    if (doorInfo != null && doorInfo.doorName != null &&
                        doorInfo.doorName.toLowerCase().contains("gym_back")) {
                        keyMgr.unlockGymBackDoor();
                        showFloatingMessage("Pintu belakang gym terbuka! Saatnya kabur!");
                        return false;
                    }
                }
                break;
                
            default:
                break;
        }
        
        return false;
    }

    private void findCurrentTileInteractable() {
        currentTileInteractable = null;

        if (tiledMapManager == null)
            return;

        float px = player.getCenterX();
        float py = player.getCenterY();

        TiledMapManager.TileInteractable ti = tiledMapManager.getTileInteractableAt(px, py, INTERACT_RANGE);

        if (ti != null && !ti.isOpen) {
            currentTileInteractable = ti;
        }
    }

    private void handleTileInteractInput() {
        if (currentTileInteractable == null)
            return;

        if (currentTileInteractable.isOpen)
            return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            GameManager gm = GameManager.getInstance();
            
            if (gm.isInTutorialState()) {
                showFloatingMessage("Aku harus ke gym dulu...");
                System.out.println("[TileInteract] Blocked in tutorial state - player must meet Josh first");
                return;
            }
            
            System.out.println("[TileInteract] Attempting to open: " + currentTileInteractable.name +
                    " type=" + currentTileInteractable.type);

            boolean toggled = tiledMapManager.toggleTileInteractable(currentTileInteractable);

            if (toggled) {
                com.fearjosh.frontend.systems.Inventory playerInventory = gm.getInventory();
                
                String itemType;
                if (currentTileInteractable.hasItem()) {
                    itemType = currentTileInteractable.containedItem.toLowerCase();
                } else {
                    double chance = Math.random();
                    if (chance < 0.35) {
                        itemType = "battery";
                    } else if (chance < 0.70) {
                        itemType = "chocolate";
                    } else {
                        itemType = null;
                    }
                }
                
                if (itemType != null) {
                    boolean itemGiven = false;
                    
                    switch (itemType) {
                        case "battery":
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
                    showFloatingMessage("Loker kosong...");
                    System.out.println("[TileInteract] Opened locker - empty");
                }
            }
        }
    }

    private void renderDarknessLayer() {
        com.fearjosh.frontend.difficulty.DifficultyStrategy ds = com.fearjosh.frontend.core.GameManager.getInstance()
                .getDifficultyStrategy();
        float visionRadius = ds.visionRadius();
        float fogDarkness = ds.fogDarkness();
        darknessFrameBuffer.begin();

        Gdx.gl.glClearColor(0f, 0f, 0f, fogDarkness);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        batch.setBlendFunction(GL20.GL_ZERO, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float cx = player.getCenterX();
        float cy = player.getCenterY();

        float baseSize = visionRadius * 2f;

        batch.setColor(1f, 1f, 1f, 0.3f);
        float outerSize = baseSize * 1.2f;
        batch.draw(lightTexture, cx - outerSize / 2f, cy - outerSize / 2f, outerSize, outerSize);

        batch.setColor(1f, 1f, 1f, 0.6f);
        float midSize = baseSize * 1.1f;
        batch.draw(lightTexture, cx - midSize / 2f, cy - midSize / 2f, midSize, midSize);

        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(lightTexture, cx - baseSize / 2f, cy - baseSize / 2f, baseSize, baseSize);

        if (player.isFlashlightOn() && battery > 0f) {
            float batteryFrac = battery / BATTERY_MAX;

            float minConeLength = 140f;
            float maxConeLength = 280f;
            float minConeWidth = 40f;
            float maxConeWidth = 80f;

            float coneLength = minConeLength + (maxConeLength - minConeLength) * batteryFrac;
            float coneWidth = minConeWidth + (maxConeWidth - minConeWidth) * batteryFrac;

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

            // Cone light path
            int steps = 12;
            for (int i = 0; i <= steps; i++) {
                float t = (float) i / steps;
                float lx = cx + (tipX - cx) * t;
                float ly = cy + (tipY - cy) * t;

                float circleSize = (visionRadius + coneWidth * t) * 2f;

                float alpha = 0.5f * (1f - t * 0.7f);
                batch.setColor(1f, 1f, 0.85f, alpha);

                batch.draw(lightTexture, lx - circleSize / 2f, ly - circleSize / 2f, circleSize, circleSize);
            }
        }

        // Reset
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        batch.end();

        darknessFrameBuffer.end();

        // UI overlay
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f);

        batch.draw(darknessFrameBuffer.getColorBufferTexture(), 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, 0, 0, 1, 1);

        batch.end();
    }

    private void renderHealthBar() {
        GameManager gm = GameManager.getInstance();
        int currentLives = gm.getCurrentLives();
        int maxLives = gm.getMaxLives();

        // Position hearts
        float totalInventoryWidth = (SLOT_SIZE * INVENTORY_SLOTS) + (SLOT_SPACING * (INVENTORY_SLOTS - 1));
        float inventoryStartX = (VIRTUAL_WIDTH - totalInventoryWidth) / 2f;
        float inventoryY = INVENTORY_MARGIN_BOTTOM;

        float startX = inventoryStartX + 8f;
        float startY = inventoryY + SLOT_SIZE + 8f;

        batch.begin();

        // Draw hearts
        for (int i = 0; i < maxLives; i++) {
            float x = startX + (i * (HEART_SIZE + HEART_SPACING));
            float y = startY;

            if (i < currentLives) {
                batch.draw(heartTexture, x, y, HEART_SIZE, HEART_SIZE);
            }
        }

        batch.end();
    }

    private void renderInventory() {
        if (playerFullyInjured) {
            return;
        }

        GameManager gm = GameManager.getInstance();
        Inventory inventory = gm.getInventory();

        float totalWidth = (SLOT_SIZE * INVENTORY_SLOTS) + (SLOT_SPACING * (INVENTORY_SLOTS - 1));
        float startX = (VIRTUAL_WIDTH - totalWidth) / 2f;
        float startY = INVENTORY_MARGIN_BOTTOM;

        batch.begin();

        // Draw slots
        for (int i = 0; i < INVENTORY_SLOTS; i++) {
            float x = startX + (i * (SLOT_SIZE + SLOT_SPACING));
            float y = startY;

            Texture slotTex = (i == inventory.getSelectedSlot()) ? inventorySlotSelectedTexture : inventorySlotTexture;
            batch.draw(slotTex, x, y, SLOT_SIZE, SLOT_SIZE);

            Item item = inventory.getItem(i);
            if (item != null && item.getIcon() != null) {
                float iconPadding = 4f;
                float iconSize = SLOT_SIZE - (iconPadding * 2);
                batch.draw(item.getIcon(), x + iconPadding, y + iconPadding, iconSize, iconSize);
            }
        }

        batch.end();
    }

    private Texture createInventorySlotTexture(boolean selected) {
        int size = 64;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // Background color
        if (selected) {
            pixmap.setColor(0.3f, 0.3f, 0.3f, 0.9f);
        } else {
            pixmap.setColor(0.15f, 0.15f, 0.15f, 0.8f);
        }
        pixmap.fill();

        // Border
        if (selected) {
            pixmap.setColor(1f, 1f, 1f, 1f);
        } else {
            pixmap.setColor(0.4f, 0.4f, 0.4f, 1f);
        }

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

    private void renderInjuryTransitionPhase() {
        uiCamera.update();
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        batch.setProjectionMatrix(uiCamera.combined);

        // Draw black background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 1f);
        shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        shapeRenderer.end();

        if (injuryTransitionAlpha > 0f && injuryTransitionTexture != null) {
            batch.begin();
            Color c = batch.getColor();
            batch.setColor(c.r, c.g, c.b, injuryTransitionAlpha);
            batch.draw(injuryTransitionTexture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            batch.setColor(c.r, c.g, c.b, 1f);
            batch.end();
        }
    }

    @SuppressWarnings("unused")
    private void renderInjuryTransition() {
        if (injuryTransitionTexture == null)
            return;

        batch.begin();

        Color c = batch.getColor();
        batch.setColor(c.r, c.g, c.b, injuryTransitionAlpha);

        batch.draw(injuryTransitionTexture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        batch.setColor(c.r, c.g, c.b, 1f);

        batch.end();
    }

    @SuppressWarnings("unused")
    private void renderInjuryHintBox() {
        if (injuryTransitionAlpha < 1f)
            return;

        float boxWidth = 300f;
        float boxHeight = 60f;
        float boxX = (VIRTUAL_WIDTH - boxWidth) / 2f;
        float boxY = VIRTUAL_HEIGHT / 2f - 100f;

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.8f);
        shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);

        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 1f, 1f, 1f);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1f);

        batch.begin();
        font.setColor(Color.WHITE);

        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
        layout.setText(font, "Tekan F untuk melepaskan diri!");

        float textX = boxX + (boxWidth - layout.width) / 2f;
        float textY = boxY + (boxHeight + layout.height) / 2f;

        font.draw(batch, "Tekan F untuk melepaskan diri!", textX, textY);
        batch.end();
    }

    private Texture createLightTexture(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f);

        int centerX = size / 2;
        int centerY = size / 2;
        int radius = size / 2;

        pixmap.fillCircle(centerX, centerY, radius);

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        return texture;
    }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height);
        uiCamera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
    }

    @Override
    public void show() {
        com.badlogic.gdx.Gdx.input.setInputProcessor(null);

        AudioManager.getInstance().stopMusic();

        // Ambient audio
        AudioManager audioManager = AudioManager.getInstance();
        audioManager.playMusic("Audio/Music/background_playing_music.wav", true);
        cricketSoundId = audioManager.loopSound("Audio/Effect/cricket_sound_effect.wav");
        rainSoundId = audioManager.loopSound("Audio/Effect/rain_sound_effect.wav");
        System.out.println("[PlayScreen.show()] Ambient audio started - Music, Cricket, Rain");

        monsterGruntTimer = 0f;
        monsterRoarTimer = 0f;

        playerBeingCaught = false;
        playerFullyInjured = false;
        injuryTimer = 0f;
        injuryTransitionAlpha = 0f;

        System.out.println("[PlayScreen.show()] Injury state reset - playerFullyInjured: " + playerFullyInjured);

        if (player != null) {
            player.setInjured(false);
            if (player.getCurrentState() instanceof com.fearjosh.frontend.state.player.InjuredState) {
                player.setState(com.fearjosh.frontend.state.player.NormalState.getInstance());
                System.out.println("[PlayScreen.show()] Player state reset to NormalState");
            }
        }

        GameManager gm = GameManager.getInstance();
        GameManager.GameState currentState = gm.getCurrentState();
        if (currentState != GameManager.GameState.STORY && 
            currentState != GameManager.GameState.PLAYING && 
            !gm.isPaused()) {
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

        AudioManager.getInstance().pauseMusic();
    }

    @Override
    public void resume() {
        paused = false;
        GameManager.getInstance().setCurrentState(GameManager.GameState.PLAYING);

        AudioManager.getInstance().resumeMusic();
    }

    @Override
    public void hide() {
        paused = false;

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

        JumpscareManager.getInstance().dispose();
    }

    private void startInjuredMinigame() {
        player.setState(com.fearjosh.frontend.state.player.InjuredState.getInstance());
        player.setInjured(true);
        
        injuredMinigame.startAtPosition(
            () -> {
                System.out.println("[INJURED] Player berhasil membalut lukanya!");
                onInjuredMinigameSuccess();
            },
            () -> {
                System.out.println("[INJURED] Player gagal membalut! Press F to try again.");
                onInjuredMinigameFail();
            },
            player.getX(),
            player.getY()
        );
        System.out.println("[MINIGAME] Injured minigame started at (" + player.getX() + ", " + player.getY() + ")! Press SPACE rapidly to bandage yourself!");
    }

    private void onInjuredMinigameSuccess() {
        playerFullyInjured = false;
        injuryPhaseTimer = 0f;

        player.setState(com.fearjosh.frontend.state.player.NormalState.getInstance());
        player.setInjured(false);

        System.out.println("[INJURED] Player recovered and can move again!");
    }

    private void onInjuredMinigameFail() {
        System.out.println("[INJURED] Bandaging failed! Press F to try again.");
    }

    private void triggerGoodEnding() {
        if (endingTriggered) return;
        endingTriggered = true;
        
        System.out.println("[ENDING] Good ending triggered - Player escapes!");
        
        GameManager gm = GameManager.getInstance();
        gm.setCurrentState(GameManager.GameState.ENDING);
        
        playerFullyInjured = false;
        paused = false;
        
        stopAmbientAudio();
        
        if (josh != null) {
            RoomDirector rd = gm.getRoomDirector();
            if (rd != null) {
                rd.onEnemyDespawn();
            }
            josh = null;
        }
        
        if (gm.getCurrentSession() != null) {
            gm.getCurrentSession().setActive(false);
        }
        
        if (inGameCutscene != null) {
            inGameCutscene.startCutscene(InGameCutscene.CutsceneType.ESCAPE_SUCCESS, () -> {
                game.setScreen(new EndingCutsceneScreen(game));
            });
        } else {
            game.setScreen(new EndingCutsceneScreen(game));
        }
    }

    private void triggerGameOver() {
        if (endingTriggered) return;
        endingTriggered = true;
        
        System.out.println("[GAME OVER] All lives lost! Game Over triggered.");

        GameManager gm = GameManager.getInstance();
        gm.setCurrentState(GameManager.GameState.GAME_OVER);

        playerFullyInjured = false;
        paused = false;
        isGameOver = true;

        stopAmbientAudio();

        if (josh != null) {
            RoomDirector rd = gm.getRoomDirector();
            if (rd != null) {
                rd.onEnemyDespawn();
            }
            josh = null;
        }
        
        if (gm.getCurrentSession() != null) {
            gm.getCurrentSession().setActive(false);
        }
        
        game.setScreen(new GameOverCutsceneScreen(game));
    }

    @Override
    public void onCameraShake(float duration, float intensity) {
        if (cameraController != null) {
            cameraController.shake(duration, intensity);
        }
    }
    
    @Override
    public void onDoorEntry(RoomDirector.DoorDirection direction) {
    }

}
