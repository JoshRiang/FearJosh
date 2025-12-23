package com.fearjosh.frontend.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.fearjosh.frontend.world.RoomId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages Tiled TMX map loading and rendering for rooms.
 * Each room can have its own TMX map file.
 * 
 * TMX Door Objects:
 * Add rectangle objects to a "Doors" object layer with a custom property "room"
 * set to the RoomId name (e.g., "CLASS_1A", "LOBBY").
 * When player overlaps the door object, they transition to that room.
 */
public class TiledMapManager implements Disposable {

    private static final String MAPS_PATH = "Maps/";
    private static final String DOORS_LAYER = "Doors";

    // Layer names for collision detection
    private static final String FLOOR_LAYER = "floor_wall";
    private static final String COLLISION_LAYER = "collision"; // Optional dedicated collision layer

    // Layers that need Y-sorted rendering (furniture/objects that player can walk
    // behind/in front of)
    private static final java.util.Set<String> Y_SORTED_LAYERS = new java.util.HashSet<>(java.util.Arrays.asList(
            "tables", "chairs", "furnitures_props"));

    // Floor tile IDs (walkable tiles) - add any floor tile IDs from your tilesets
    private static final java.util.Set<Integer> WALKABLE_TILE_IDS = new java.util.HashSet<>(java.util.Arrays.asList(
            504, // Hallway floor tile
            281, // Gym floor tile
            248, // Alternative floor
            264 // Alternative floor
    ));

    private final TmxMapLoader mapLoader;
    private final Map<RoomId, TiledMap> loadedMaps;
    private final Map<RoomId, String> roomMapFiles;

    private TiledMap currentMap;
    private OrthogonalTiledMapRenderer mapRenderer;
    private float unitScale = 1f;

    // Cached collision data
    private int tileWidth;
    private int tileHeight;
    private int mapWidthTiles;
    private int mapHeightTiles;

    // Physical pickup texture for locker_key ONLY
    // locker_key is the ONLY key that renders physically in the world
    // janitor_key and gym_key are INVISIBLE trigger zones
    private com.badlogic.gdx.graphics.Texture lockerKeyTexture;
    private static final float KEY_RENDER_SIZE = 16f; // Size to render physical key (keys are small!)

    public TiledMapManager() {
        this.mapLoader = new TmxMapLoader();
        this.loadedMaps = new HashMap<>();
        this.roomMapFiles = new HashMap<>();

        // Register which rooms have TMX maps
        registerRoomMaps();
        
        // Load texture for physical locker_key pickup ONLY
        loadLockerKeyTexture();
    }
    
    /**
     * Load the texture for physical locker_key pickup.
     * ONLY locker_key is rendered physically - other keys are invisible triggers.
     */
    private void loadLockerKeyTexture() {
        try {
            if (Gdx.files.internal("Items/locker_key.png").exists()) {
                lockerKeyTexture = new com.badlogic.gdx.graphics.Texture(
                    Gdx.files.internal("Items/locker_key.png")
                );
                Gdx.app.log("TiledMapManager", "Loaded physical locker_key texture");
            } else {
                Gdx.app.error("TiledMapManager", "Items/locker_key.png not found!");
            }
        } catch (Exception e) {
            Gdx.app.error("TiledMapManager", "Failed to load locker_key texture", e);
        }
    }

    /**
     * Register TMX map files for specific rooms.
     * Add entries here as you create more room maps.
     */
    private void registerRoomMaps() {
        // Example: HALLWAY uses hallway.tmx
        roomMapFiles.put(RoomId.HALLWAY, "hallway.tmx");
        roomMapFiles.put(RoomId.GYM, "gym.tmx");
        roomMapFiles.put(RoomId.CLASS_1A, "CLASSROOM_1A.tmx");
        roomMapFiles.put(RoomId.CLASS_2A, "CLASSROOM_2A.tmx");
        roomMapFiles.put(RoomId.LOBBY, "lobby.tmx");
        roomMapFiles.put(RoomId.JANITOR, "janitor.tmx");

        // Add more room mappings as needed:
        // etc.
    }

    /**
     * Check if a room has a TMX map available
     */
    public boolean hasMapForRoom(RoomId roomId) {
        return roomMapFiles.containsKey(roomId);
    }

    /**
     * Load the map for a specific room
     */
    public void loadMapForRoom(RoomId roomId) {
        if (!hasMapForRoom(roomId)) {
            currentMap = null;
            if (mapRenderer != null) {
                mapRenderer.setMap(null);
            }
            return;
        }

        // Check if already loaded
        if (loadedMaps.containsKey(roomId)) {
            currentMap = loadedMaps.get(roomId);
        } else {
            // Load the map
            String mapFile = MAPS_PATH + roomMapFiles.get(roomId);
            try {
                TiledMap map = mapLoader.load(mapFile);
                loadedMaps.put(roomId, map);
                currentMap = map;
                Gdx.app.log("TiledMapManager", "Loaded map: " + mapFile);
            } catch (Exception e) {
                Gdx.app.error("TiledMapManager", "Failed to load map: " + mapFile, e);
                currentMap = null;
                return;
            }
        }

        // Cache tile dimensions for collision
        tileWidth = currentMap.getProperties().get("tilewidth", Integer.class);
        tileHeight = currentMap.getProperties().get("tileheight", Integer.class);
        mapWidthTiles = currentMap.getProperties().get("width", Integer.class);
        mapHeightTiles = currentMap.getProperties().get("height", Integer.class);

        // Update or create renderer
        if (mapRenderer == null) {
            mapRenderer = new OrthogonalTiledMapRenderer(currentMap, unitScale);
        } else {
            mapRenderer.setMap(currentMap);
        }

        // Log available doors for debugging
        logDoorInfo();

        // Load tile-based interactables (lockers, chests, etc.)
        loadTileInteractables();
        
        // Load exit_key trigger zones (NOT physical pickups)
        loadExitKeyTriggers();
    }
    
    /**
     * Load exit_key TRIGGER ZONES from the exit_key object layer.
     * These are invisible trigger areas - NOT physical key pickups.
     * Players trigger them by walking into the zone.
     * Supports both Rectangle objects and Point objects (with default size).
     */
    private void loadExitKeyTriggers() {
        currentExitKeyTriggers.clear();
        
        if (currentMap == null) return;
        
        MapLayer keyLayer = currentMap.getLayers().get(EXIT_KEY_LAYER);
        if (keyLayer == null) {
            Gdx.app.log("TiledMapManager", "No exit_key layer found in current map");
            return;
        }
        
        // Default trigger size for point objects (48x48 pixels, centered on point)
        final float DEFAULT_TRIGGER_SIZE = 48f;
        
        MapObjects objects = keyLayer.getObjects();
        for (MapObject obj : objects) {
            String itemType = obj.getProperties().get("item", String.class);
            
            if (itemType == null || itemType.isEmpty()) {
                Gdx.app.log("TiledMapManager", "Skipping exit_key object without 'item' property");
                continue; // Skip objects without item property
            }
            
            Rectangle scaledBounds = null;
            
            if (obj instanceof RectangleMapObject) {
                // Rectangle object with explicit size
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                
                // Check if it's actually a rectangle (has width/height > 0)
                if (rect.width > 0 && rect.height > 0) {
                    scaledBounds = new Rectangle(
                        rect.x * unitScale,
                        rect.y * unitScale,
                        rect.width * unitScale,
                        rect.height * unitScale
                    );
                    Gdx.app.log("TiledMapManager", "exit_key Rectangle: " + itemType + 
                        " at (" + rect.x + ", " + rect.y + ") size=(" + rect.width + "x" + rect.height + ")");
                } else {
                    // It's a point disguised as RectangleMapObject (width/height = 0)
                    scaledBounds = new Rectangle(
                        (rect.x - DEFAULT_TRIGGER_SIZE / 2f) * unitScale,
                        (rect.y - DEFAULT_TRIGGER_SIZE / 2f) * unitScale,
                        DEFAULT_TRIGGER_SIZE * unitScale,
                        DEFAULT_TRIGGER_SIZE * unitScale
                    );
                    Gdx.app.log("TiledMapManager", "exit_key Point (from Rect): " + itemType + 
                        " at (" + rect.x + ", " + rect.y + ") with default size " + DEFAULT_TRIGGER_SIZE);
                }
            } else {
                // Other object types (EllipseMapObject, PolygonMapObject, etc.)
                // Try to read x and y from properties
                Float x = obj.getProperties().get("x", Float.class);
                Float y = obj.getProperties().get("y", Float.class);
                
                if (x != null && y != null) {
                    // Center the trigger box on the point position
                    scaledBounds = new Rectangle(
                        (x - DEFAULT_TRIGGER_SIZE / 2f) * unitScale,
                        (y - DEFAULT_TRIGGER_SIZE / 2f) * unitScale,
                        DEFAULT_TRIGGER_SIZE * unitScale,
                        DEFAULT_TRIGGER_SIZE * unitScale
                    );
                    Gdx.app.log("TiledMapManager", "exit_key Other: " + itemType + 
                        " at (" + x + ", " + y + ") with default size " + DEFAULT_TRIGGER_SIZE);
                } else {
                    Gdx.app.error("TiledMapManager", "exit_key object '" + itemType + 
                        "' has no valid position! Type: " + obj.getClass().getSimpleName());
                }
            }
            
            if (scaledBounds != null) {
                // Get TMX object ID for tracking consumed triggers
                int objectId = obj.getProperties().get("id", 0, Integer.class);
                if (objectId == 0) {
                    // Fallback: use object's name hash or generate from position
                    objectId = (int)(scaledBounds.x * 1000 + scaledBounds.y);
                }
                
                ExitKeyTrigger trigger = new ExitKeyTrigger(scaledBounds, itemType, objectId);
                currentExitKeyTriggers.add(trigger);
                Gdx.app.log("TiledMapManager", "Loaded exit_key TRIGGER: " + itemType + 
                    " objectId=" + objectId + " at (" + scaledBounds.x + ", " + scaledBounds.y + 
                    ") size=(" + scaledBounds.width + "x" + scaledBounds.height + ")");
            }
        }
        
        Gdx.app.log("TiledMapManager", "Loaded " + currentExitKeyTriggers.size + " exit_key TRIGGER ZONES");
    }
    
    /**
     * Check if player overlaps any unconsumed exit_key trigger zone.
     * 
     * @param playerBounds Player's collision bounds
     * @return ExitKeyTrigger if overlapping, null otherwise
     */
    public ExitKeyTrigger checkExitKeyTrigger(Rectangle playerBounds) {
        for (ExitKeyTrigger trigger : currentExitKeyTriggers) {
            if (!trigger.consumed && trigger.overlaps(playerBounds)) {
                return trigger;
            }
        }
        return null;
    }
    
    /**
     * Check if player overlaps any unconsumed exit_key trigger zone (by center point).
     * 
     * @param playerX Player center X
     * @param playerY Player center Y
     * @return ExitKeyTrigger if overlapping, null otherwise
     */
    public ExitKeyTrigger checkExitKeyTrigger(float playerX, float playerY) {
        for (ExitKeyTrigger trigger : currentExitKeyTriggers) {
            if (!trigger.consumed && trigger.overlaps(playerX, playerY)) {
                return trigger;
            }
        }
        return null;
    }
    
    /**
     * Mark an exit_key trigger as consumed (used).
     * Returns a unique key for persistent tracking: "roomId_objectId"
     */
    public String consumeExitKeyTrigger(ExitKeyTrigger trigger) {
        if (trigger != null) {
            trigger.consumed = true;
            Gdx.app.log("TiledMapManager", "Consumed exit_key trigger: " + trigger.itemType + 
                " objectId=" + trigger.objectId);
        }
        return trigger != null ? String.valueOf(trigger.objectId) : null;
    }
    
    /**
     * Mark a trigger as consumed by its object ID (for restoring state on room load).
     */
    public void markTriggerConsumed(int objectId) {
        for (ExitKeyTrigger trigger : currentExitKeyTriggers) {
            if (trigger.objectId == objectId) {
                trigger.consumed = true;
                Gdx.app.log("TiledMapManager", "Restored consumed state for trigger objectId=" + objectId);
                break;
            }
        }
    }
    
    /**
     * Get all current exit_key triggers (for debugging/state queries).
     */
    public Array<ExitKeyTrigger> getExitKeyTriggers() {
        return currentExitKeyTriggers;
    }
    
    /**
     * Render ONLY the physical locker_key pickup (if unconsumed).
     * janitor_key and gym_key are INVISIBLE trigger zones and are NOT rendered.
     * 
     * @param batch SpriteBatch to render with (must be begun)
     */
    public void renderPhysicalLockerKey(SpriteBatch batch) {
        if (lockerKeyTexture == null) {
            // Texture not loaded, try to load it
            loadLockerKeyTexture();
            if (lockerKeyTexture == null) {
                return;
            }
        }
        
        for (ExitKeyTrigger trigger : currentExitKeyTriggers) {
            // ONLY render locker_key physically - other keys are invisible triggers
            if (!trigger.consumed && "locker_key".equalsIgnoreCase(trigger.itemType)) {
                float renderX = trigger.bounds.x + (trigger.bounds.width - KEY_RENDER_SIZE) / 2f;
                float renderY = trigger.bounds.y + (trigger.bounds.height - KEY_RENDER_SIZE) / 2f;
                
                batch.draw(lockerKeyTexture, renderX, renderY, KEY_RENDER_SIZE, KEY_RENDER_SIZE);
                
                // Debug log (remove after testing)
                // Gdx.app.log("TiledMapManager", "Rendering locker_key at (" + renderX + ", " + renderY + ")");
            }
        }
    }

    /**
     * Log door information for debugging
     */
    private void logDoorInfo() {
        Array<DoorInfo> doors = getDoors();
        Gdx.app.log("TiledMapManager", "Found " + doors.size + " doors in current map:");
        for (DoorInfo door : doors) {
            Gdx.app.log("TiledMapManager", "  Door '" + door.doorName + "' -> " + door.targetRoom +
                    " at bounds: x=" + door.bounds.x + ", y=" + door.bounds.y +
                    ", w=" + door.bounds.width + ", h=" + door.bounds.height);
        }

        // Log all layers
        Gdx.app.log("TiledMapManager", "Map layers:");
        for (MapLayer layer : currentMap.getLayers()) {
            Gdx.app.log("TiledMapManager",
                    "  - " + layer.getName() + " (type: " + layer.getClass().getSimpleName() + ")");
        }
    }

    /**
     * Set the unit scale for rendering (pixels per unit)
     */
    public void setUnitScale(float scale) {
        this.unitScale = scale;
        if (mapRenderer != null && currentMap != null) {
            // Need to recreate renderer with new scale
            mapRenderer.dispose();
            mapRenderer = new OrthogonalTiledMapRenderer(currentMap, unitScale);
        }
    }

    /**
     * Render the current map
     */
    public void render(OrthographicCamera camera) {
        if (mapRenderer != null && currentMap != null) {
            mapRenderer.setView(camera);
            mapRenderer.render();
        }
    }

    /**
     * Render specific layers of the current map
     * 
     * @param camera       The camera to use
     * @param layerIndices Array of layer indices to render
     */
    public void render(OrthographicCamera camera, int[] layerIndices) {
        if (mapRenderer != null && currentMap != null) {
            mapRenderer.setView(camera);
            mapRenderer.render(layerIndices);
        }
    }

    /**
     * Render layers BEFORE the player (background, floor, upper decorations)
     * These are layers that appear behind the player sprite.
     */
    public void renderBelowPlayer(OrthographicCamera camera) {
        if (mapRenderer == null || currentMap == null)
            return;

        mapRenderer.setView(camera);

        // Find the "Player" layer index and render everything before it
        int playerLayerIndex = getLayerIndex("Player");
        if (playerLayerIndex < 0) {
            // No Player layer found - render floor and door_locker only
            int[] belowLayers = { 0, 1 }; // floor_wall, door_locker
            mapRenderer.render(belowLayers);
        } else {
            // Render all layers before the Player layer
            int[] belowLayers = new int[playerLayerIndex];
            for (int i = 0; i < playerLayerIndex; i++) {
                belowLayers[i] = i;
            }
            if (belowLayers.length > 0) {
                mapRenderer.render(belowLayers);
            }
        }
    }

    /**
     * Render layers ABOVE the player (bottom wall, bottom doors)
     * These are layers that appear in front of the player sprite.
     * EXCLUDES Y-sorted layers since those are rendered via renderWithYSort.
     */
    public void renderAbovePlayer(OrthographicCamera camera) {
        if (mapRenderer == null || currentMap == null)
            return;

        mapRenderer.setView(camera);

        // Find the "Player" layer index and render everything after it, excluding
        // Y-sorted layers
        int playerLayerIndex = getLayerIndex("Player");
        int totalLayers = currentMap.getLayers().getCount();

        List<Integer> layerIndices = new ArrayList<>();
        int startIndex = playerLayerIndex >= 0 ? playerLayerIndex + 1 : 0;

        for (int i = startIndex; i < totalLayers; i++) {
            MapLayer layer = currentMap.getLayers().get(i);
            // Skip Y-sorted layers - they are rendered separately
            if (!Y_SORTED_LAYERS.contains(layer.getName())) {
                layerIndices.add(i);
            }
        }

        if (!layerIndices.isEmpty()) {
            int[] aboveLayers = new int[layerIndices.size()];
            for (int i = 0; i < layerIndices.size(); i++) {
                aboveLayers[i] = layerIndices.get(i);
            }
            mapRenderer.render(aboveLayers);
        }
    }

    /**
     * Get the index of a layer by name
     */
    public int getLayerIndex(String layerName) {
        if (currentMap == null)
            return -1;
        for (int i = 0; i < currentMap.getLayers().getCount(); i++) {
            if (currentMap.getLayers().get(i).getName().equals(layerName)) {
                return i;
            }
        }
        return -1;
    }

    // ======================
    // Y-SORTED RENDERING
    // ======================

    /**
     * Data class for Y-sorted rendering elements
     */
    private static class YSortedElement {
        float y; // Y position (bottom of sprite/tile)
        Runnable render; // Render callback
        boolean isPlayer; // True if this is the player (needs special batch handling)

        YSortedElement(float y, Runnable render) {
            this(y, render, false);
        }

        YSortedElement(float y, Runnable render, boolean isPlayer) {
            this.y = y;
            this.render = render;
            this.isPlayer = isPlayer;
        }
    }

    /**
     * Render furniture layers with Y-sorting relative to player.
     * Objects with higher Y (below player on screen) render first.
     * Objects with lower Y (above player on screen) render after player.
     * 
     * @param camera         The camera
     * @param batch          SpriteBatch for rendering (will be managed internally -
     *                       should NOT be in begin state)
     * @param playerY        Player's Y position (bottom of player sprite)
     * @param playerRenderer Callback to render the player (should handle
     *                       batch.begin/end internally)
     */
    public void renderWithYSort(OrthographicCamera camera, SpriteBatch batch, float playerY, Runnable playerRenderer) {
        if (currentMap == null) {
            playerRenderer.run();
            return;
        }

        List<YSortedElement> elements = new ArrayList<>();

        // Add player to the sorted list (player handles own batch.begin/end, marked as
        // isPlayer=true)
        elements.add(new YSortedElement(playerY, playerRenderer, true));

        // Collect all furniture tiles from Y-sorted layers
        for (MapLayer layer : currentMap.getLayers()) {
            if (layer instanceof TiledMapTileLayer && Y_SORTED_LAYERS.contains(layer.getName())) {
                TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
                collectTilesForYSort(tileLayer, elements, batch);
            }
        }

        // Sort by Y (higher Y values = render first = behind lower Y objects)
        Collections.sort(elements, (a, b) -> Float.compare(b.y, a.y));

        // Render all elements in sorted order
        // Batch tiles together for efficiency
        boolean batchActive = false;
        for (YSortedElement element : elements) {
            if (element.isPlayer) {
                // End batch before player renders (player manages own batch)
                if (batchActive) {
                    batch.end();
                    batchActive = false;
                }
                element.render.run();
            } else {
                // Start batch if not active for tile rendering
                if (!batchActive) {
                    batch.begin();
                    batchActive = true;
                }
                element.render.run();
            }
        }
        // End batch if still active
        if (batchActive) {
            batch.end();
        }
    }

    /**
     * Collect tiles from a layer for Y-sorted rendering.
     * Uses the MIDDLE Y position of the tile for comparison.
     * If player feet Y < tile middle Y → tile renders first (player in front)
     * If player feet Y >= tile middle Y → player renders first (player behind)
     */
    private void collectTilesForYSort(TiledMapTileLayer tileLayer, List<YSortedElement> elements, SpriteBatch batch) {
        int layerWidth = tileLayer.getWidth();
        int layerHeight = tileLayer.getHeight();
        float tileW = tileWidth * unitScale;
        float tileH = tileHeight * unitScale;

        for (int y = 0; y < layerHeight; y++) {
            for (int x = 0; x < layerWidth; x++) {
                TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                if (cell != null && cell.getTile() != null) {
                    TiledMapTile tile = cell.getTile();
                    TextureRegion region = tile.getTextureRegion();

                    float worldX = x * tileW;
                    float worldY = y * tileH;

                    // Calculate sort Y as the MIDDLE of the tile
                    // This is compared against player's feet position
                    float sortY = worldY + (tileH / 2f);

                    // Check if tile has collision objects - use their middle Y instead
                    MapObjects tileObjects = tile.getObjects();
                    if (tileObjects != null && tileObjects.getCount() > 0) {
                        for (MapObject obj : tileObjects) {
                            if (obj instanceof RectangleMapObject) {
                                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                                // Use middle of collision object for sorting
                                sortY = worldY + (rect.y + rect.height / 2f) * unitScale;
                                break; // Use first collision object
                            }
                        }
                    }

                    // Capture values for lambda
                    final float drawX = worldX;
                    final float drawY = worldY;
                    final float drawW = tileW;
                    final float drawH = tileH;
                    final TextureRegion drawRegion = region;

                    // Use middle Y position for sorting
                    elements.add(new YSortedElement(sortY, () -> {
                        batch.draw(drawRegion, drawX, drawY, drawW, drawH);
                    }));
                }
            }
        }
    }

    /**
     * Render layers below player, EXCLUDING Y-sorted layers (which are rendered
     * separately)
     */
    public void renderBelowPlayerExcludingYSorted(OrthographicCamera camera) {
        if (mapRenderer == null || currentMap == null)
            return;

        mapRenderer.setView(camera);

        // Collect layer indices that are NOT Y-sorted
        List<Integer> layerIndices = new ArrayList<>();
        int playerLayerIndex = getLayerIndex("Player");
        int limit = playerLayerIndex >= 0 ? playerLayerIndex : currentMap.getLayers().getCount();

        for (int i = 0; i < limit; i++) {
            MapLayer layer = currentMap.getLayers().get(i);
            if (!Y_SORTED_LAYERS.contains(layer.getName())) {
                layerIndices.add(i);
            }
        }

        if (!layerIndices.isEmpty()) {
            int[] layers = new int[layerIndices.size()];
            for (int i = 0; i < layerIndices.size(); i++) {
                layers[i] = layerIndices.get(i);
            }
            mapRenderer.render(layers);
        }
    }

    // ======================
    // DOOR DETECTION FROM TMX
    // ======================

    /**
     * Data class for door information from TMX
     */
    public static class DoorInfo {
        public final Rectangle bounds;
        public final String targetRoom;
        public final String doorName;

        public DoorInfo(Rectangle bounds, String targetRoom, String doorName) {
            this.bounds = bounds;
            this.targetRoom = targetRoom;
            this.doorName = doorName;
        }
    }

    /**
     * Data class for spawn point information from TMX
     */
    public static class SpawnInfo {
        public final float x;
        public final float y;
        public final String fromRoom;
        
        // Optional properties for josh_spawn context-aware spawning
        public String facing;       // Direction Josh should face (optional)
        public int priority;        // Priority for spawn selection (higher = preferred)
        public String nearDoor;     // Which door this spawn is near (for chase context)

        public SpawnInfo(float x, float y, String fromRoom) {
            this.x = x;
            this.y = y;
            this.fromRoom = fromRoom;
            this.facing = null;
            this.priority = 0;
            this.nearDoor = null;
        }
    }
    
    /**
     * Data class for exit_key TRIGGER ZONE from TMX exit_key layer.
     * These are invisible trigger zones - NOT physical pickups.
     * Players trigger them by overlapping the zone bounds.
     */
    public static class ExitKeyTrigger {
        public final Rectangle bounds;      // TMX bounds (for overlap detection)
        public final String itemType;       // "locker_key", "janitor_key", "gym_key"
        public final int objectId;          // TMX object id for tracking consumed triggers
        public boolean consumed;            // Whether this trigger has been used
        
        public ExitKeyTrigger(Rectangle bounds, String itemType, int objectId) {
            this.bounds = bounds;
            this.itemType = itemType;
            this.objectId = objectId;
            this.consumed = false;
        }
        
        /**
         * Check if a point overlaps this trigger zone
         */
        public boolean overlaps(float x, float y) {
            return bounds.contains(x, y);
        }
        
        /**
         * Check if a rectangle overlaps this trigger zone
         */
        public boolean overlaps(Rectangle playerBounds) {
            return bounds.overlaps(playerBounds);
        }
        
        /**
         * Get center X for distance checks
         */
        public float getCenterX() {
            return bounds.x + bounds.width / 2f;
        }
        
        /**
         * Get center Y for distance checks
         */
        public float getCenterY() {
            return bounds.y + bounds.height / 2f;
        }
    }

    private static final String SPAWN_LAYER = "spawn";
    private static final String EXIT_KEY_LAYER = "exit_key";
    
    // Cache for exit_key trigger zones in current map (NOT physical pickups)
    private Array<ExitKeyTrigger> currentExitKeyTriggers = new Array<>();

    /**
     * Get spawn point for player coming from a specific room.
     * 
     * @param fromRoom The room the player is coming from (e.g., "hallway",
     *                 "HALLWAY")
     * @return SpawnInfo with x,y coordinates, or null if not found
     */
    public SpawnInfo getSpawnPoint(String fromRoom) {
        if (currentMap == null || fromRoom == null)
            return null;

        String normalizedFrom = fromRoom.toUpperCase().replace(" ", "_").replace("-", "_");

        MapLayer spawnLayer = currentMap.getLayers().get(SPAWN_LAYER);
        if (spawnLayer == null) {
            spawnLayer = currentMap.getLayers().get("Spawn");
            if (spawnLayer == null) {
                spawnLayer = currentMap.getLayers().get("spawns");
            }
        }

        if (spawnLayer == null) {
            Gdx.app.log("TiledMapManager", "No spawn layer found in map");
            return null;
        }

        MapObjects objects = spawnLayer.getObjects();
        for (MapObject obj : objects) {
            String objFromRoom = obj.getProperties().get("from_room", String.class);
            if (objFromRoom == null) {
                objFromRoom = obj.getProperties().get("from", String.class);
            }

            if (objFromRoom != null) {
                String normalizedObjFrom = objFromRoom.toUpperCase().replace(" ", "_").replace("-", "_");

                // Check for exact match, or flexible match for CLASS/CLASSROOM naming
                boolean matches = normalizedObjFrom.equals(normalizedFrom) ||
                        matchesRoomName(normalizedFrom, normalizedObjFrom);

                if (matches) {
                    // Get position - handle both point objects and rectangle objects
                    float x, y;
                    if (obj instanceof RectangleMapObject) {
                        Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                        x = rect.x * unitScale;
                        y = rect.y * unitScale;
                    } else {
                        // For point objects, use the object's position
                        x = obj.getProperties().get("x", Float.class) * unitScale;
                        y = obj.getProperties().get("y", Float.class) * unitScale;
                    }
                    Gdx.app.log("TiledMapManager", "Found spawn point for from_room='" + fromRoom + "' (matched '"
                            + objFromRoom + "') at (" + x + ", " + y + ")");
                    return new SpawnInfo(x, y, fromRoom);
                }
            }
        }

        Gdx.app.log("TiledMapManager",
                "No spawn point found for from_room='" + fromRoom + "'. Available spawn points:");
        for (MapObject obj : objects) {
            String objFromRoom = obj.getProperties().get("from_room", String.class);
            Gdx.app.log("TiledMapManager", "  - from_room='" + objFromRoom + "'");
        }
        return null;
    }

    /**
     * Flexible room name matching to handle CLASS_1A vs CLASSROOM_1A discrepancies
     */
    private boolean matchesRoomName(String roomId, String spawnFromRoom) {
        // Handle CLASS_1A matching CLASSROOM_1A
        // Extract the class number (e.g., "1A", "2B") from both
        String classPattern = "(CLASS(?:ROOM)?_?)(\\d+[A-Z])";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(classPattern);

        java.util.regex.Matcher m1 = pattern.matcher(roomId);
        java.util.regex.Matcher m2 = pattern.matcher(spawnFromRoom);

        if (m1.find() && m2.find()) {
            // Both are classroom references - compare the class numbers
            return m1.group(2).equals(m2.group(2));
        }

        return false;
    }

    /**
     * Get spawn point for player coming from a specific RoomId.
     */
    public SpawnInfo getSpawnPoint(RoomId fromRoom) {
        if (fromRoom == null)
            return null;
        return getSpawnPoint(fromRoom.name());
    }

    /**
     * Get the first spawn point (for game start).
     * Looks for a spawn object with from_room="first_spawn" or "start" or
     * "initial".
     */
    public SpawnInfo getFirstSpawnPoint() {
        // Try multiple possible names for first spawn
        SpawnInfo spawn = getSpawnPoint("first_spawn");
        if (spawn != null)
            return spawn;

        spawn = getSpawnPoint("start");
        if (spawn != null)
            return spawn;

        spawn = getSpawnPoint("initial");
        if (spawn != null)
            return spawn;

        spawn = getSpawnPoint("game_start");
        if (spawn != null)
            return spawn;

        return null;
    }

    /**
     * Get Josh (enemy) spawn point from the current map.
     * Looks for a spawn object with josh_spawn=true property.
     * 
     * @return SpawnInfo with x,y coordinates, or null if not found
     */
    public SpawnInfo getJoshSpawnPoint() {
        if (currentMap == null)
            return null;

        MapLayer spawnLayer = currentMap.getLayers().get(SPAWN_LAYER);
        if (spawnLayer == null) {
            spawnLayer = currentMap.getLayers().get("Spawn");
            if (spawnLayer == null) {
                spawnLayer = currentMap.getLayers().get("spawns");
            }
        }

        if (spawnLayer == null) {
            Gdx.app.log("TiledMapManager", "No spawn layer found for josh spawn");
            return null;
        }

        MapObjects objects = spawnLayer.getObjects();
        for (MapObject obj : objects) {
            // Check for josh_spawn boolean property
            Boolean isJoshSpawn = obj.getProperties().get("josh_spawn", Boolean.class);
            
            if (isJoshSpawn != null && isJoshSpawn) {
                // Get position - handle both point objects and rectangle objects
                float x, y;
                if (obj instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                    x = rect.x * unitScale;
                    y = rect.y * unitScale;
                } else {
                    // For point objects, use the object's position
                    x = obj.getProperties().get("x", Float.class) * unitScale;
                    y = obj.getProperties().get("y", Float.class) * unitScale;
                }
                Gdx.app.log("TiledMapManager", "Found Josh spawn point at (" + x + ", " + y + ")");
                return new SpawnInfo(x, y, "josh_spawn");
            }
        }

        Gdx.app.log("TiledMapManager", "No josh_spawn point found in current map");
        return null;
    }
    
    /**
     * Get ALL Josh spawn points from the current map.
     * Used by JoshSpawnController for context-aware spawn selection.
     * 
     * @return Array of SpawnInfo, or empty array if none found
     */
    public Array<SpawnInfo> getAllJoshSpawnPoints() {
        Array<SpawnInfo> spawnPoints = new Array<>();
        
        if (currentMap == null)
            return spawnPoints;

        MapLayer spawnLayer = currentMap.getLayers().get(SPAWN_LAYER);
        if (spawnLayer == null) {
            spawnLayer = currentMap.getLayers().get("Spawn");
            if (spawnLayer == null) {
                spawnLayer = currentMap.getLayers().get("spawns");
            }
        }

        if (spawnLayer == null) {
            return spawnPoints;
        }

        MapObjects objects = spawnLayer.getObjects();
        for (MapObject obj : objects) {
            // Check for josh_spawn boolean property
            Boolean isJoshSpawn = obj.getProperties().get("josh_spawn", Boolean.class);
            
            if (isJoshSpawn != null && isJoshSpawn) {
                // Get position - handle both point objects and rectangle objects
                float x, y;
                if (obj instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                    x = rect.x * unitScale;
                    y = rect.y * unitScale;
                } else {
                    // For point objects, use the object's position
                    Float objX = obj.getProperties().get("x", Float.class);
                    Float objY = obj.getProperties().get("y", Float.class);
                    if (objX == null || objY == null) continue;
                    x = objX * unitScale;
                    y = objY * unitScale;
                }
                
                // Get optional properties
                String facing = obj.getProperties().get("facing", String.class);
                Integer priority = obj.getProperties().get("priority", Integer.class);
                String nearDoor = obj.getProperties().get("near_door", String.class);
                
                SpawnInfo info = new SpawnInfo(x, y, "josh_spawn");
                info.facing = facing;
                info.priority = priority != null ? priority : 0;
                info.nearDoor = nearDoor;
                
                spawnPoints.add(info);
            }
        }
        
        Gdx.app.log("TiledMapManager", "Found " + spawnPoints.size + " josh_spawn points in current map");
        return spawnPoints;
    }

    /**
     * Get all doors defined in the current map's "Doors" object layer.
     * Each door should have a "room" property with the target RoomId name.
     */
    public Array<DoorInfo> getDoors() {
        Array<DoorInfo> doors = new Array<>();
        if (currentMap == null) {
            return doors;
        }

        MapLayer doorsLayer = currentMap.getLayers().get(DOORS_LAYER);
        if (doorsLayer == null) {
            // Try alternative names
            doorsLayer = currentMap.getLayers().get("doors");
            if (doorsLayer == null) {
                doorsLayer = currentMap.getLayers().get("Door");
            }
        }

        if (doorsLayer == null) {
            return doors;
        }

        MapObjects objects = doorsLayer.getObjects();
        for (MapObject obj : objects) {
            if (obj instanceof RectangleMapObject) {
                RectangleMapObject rectObj = (RectangleMapObject) obj;
                Rectangle rect = rectObj.getRectangle();

                // Scale rectangle by unit scale
                Rectangle scaledRect = new Rectangle(
                        rect.x * unitScale,
                        rect.y * unitScale,
                        rect.width * unitScale,
                        rect.height * unitScale);

                // Get the "room" property
                String targetRoom = obj.getProperties().get("room", String.class);
                if (targetRoom == null) {
                    targetRoom = obj.getProperties().get("Room", String.class);
                }
                if (targetRoom == null) {
                    targetRoom = obj.getProperties().get("target", String.class);
                }

                String doorName = obj.getName();
                if (doorName == null)
                    doorName = "Door";

                if (targetRoom != null && !targetRoom.isEmpty()) {
                    doors.add(new DoorInfo(scaledRect, targetRoom, doorName));
                }
            }
        }

        return doors;
    }

    /**
     * Check if a position overlaps any door and return the target room.
     * 
     * @param x      Player X position
     * @param y      Player Y position
     * @param width  Player width
     * @param height Player height
     * @return The target RoomId, or null if not on a door
     */
    public RoomId checkDoorTransition(float x, float y, float width, float height) {
        if (currentMap == null)
            return null;

        Rectangle playerBounds = new Rectangle(x, y, width, height);
        Array<DoorInfo> doors = getDoors();

        for (DoorInfo door : doors) {
            if (playerBounds.overlaps(door.bounds)) {
                RoomId targetRoom = parseRoomName(door.targetRoom);
                Gdx.app.log("TiledMapManager",
                        "Door overlap! " + door.doorName + " -> " + door.targetRoom + " (parsed: " + targetRoom + ")");
                if (targetRoom != null) {
                    return targetRoom;
                }
            }
        }

        return null;
    }

    /**
     * Parse room name from TMX to RoomId, handling aliases
     */
    private RoomId parseRoomName(String roomName) {
        if (roomName == null || roomName.isEmpty())
            return null;

        // Normalize: uppercase, replace spaces with underscores
        String normalized = roomName.toUpperCase().replace(" ", "_").replace("-", "_");

        // Handle aliases (TMX naming -> RoomId enum)
        switch (normalized) {
            case "CLASSROOM_1A":
                return RoomId.CLASS_1A;
            case "CLASSROOM_2A":
                return RoomId.CLASS_2A;
            case "CLASSROOM_3A":
                return RoomId.CLASS_3A;
            case "CLASSROOM_4A":
                return RoomId.CLASS_4A;
            case "CLASSROOM_5A":
                return RoomId.CLASS_5A;
            case "CLASSROOM_6A":
                return RoomId.CLASS_6A;
            case "CLASSROOM_7A":
                return RoomId.CLASS_7A;
            case "CLASSROOM_8A":
                return RoomId.CLASS_8A;
            case "CLASSROOM_1B":
                return RoomId.CLASS_1B;
            case "CLASSROOM_2B":
                return RoomId.CLASS_2B;
            case "CLASSROOM_3B":
                return RoomId.CLASS_3B;
            case "CLASSROOM_4B":
                return RoomId.CLASS_4B;
            case "CLASSROOM_5B":
                return RoomId.CLASS_5B;
            case "CLASSROOM_6B":
                return RoomId.CLASS_6B;
            case "CLASSROOM_7B":
                return RoomId.CLASS_7B;
            case "CLASSROOM_8B":
                return RoomId.CLASS_8B;
            case "JANITOR":
                return RoomId.JANITOR;
            case "RESTROOM":
                return RoomId.RESTROOM;
            case "TEACHERS_ROOM":
                return RoomId.TEACHERS_ROOM;
            case "GYM":
            case "GYMNASIUM":
                return RoomId.GYM;
            case "HALLWAY":
            case "MAIN_HALLWAY":
                return RoomId.HALLWAY;
            case "LOBBY":
            case "MAIN_LOBBY":
                return RoomId.LOBBY;
            case "PARKING":
            case "PARKING_LOT":
                return RoomId.PARKING;
            default:
                // Try direct match
                try {
                    return RoomId.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    Gdx.app.error("TiledMapManager", "Unknown room: " + roomName + " (normalized: " + normalized + ")");
                    return null;
                }
        }
    }

    /**
     * Get the door info at a specific position
     */
    public DoorInfo getDoorAt(float x, float y, float width, float height) {
        if (currentMap == null)
            return null;

        Rectangle playerBounds = new Rectangle(x, y, width, height);
        Array<DoorInfo> doors = getDoors();

        for (DoorInfo door : doors) {
            if (playerBounds.overlaps(door.bounds)) {
                return door;
            }
        }

        return null;
    }

    /**
     * Get the current loaded map
     */
    public TiledMap getCurrentMap() {
        return currentMap;
    }

    /**
     * Get map width in pixels
     */
    public float getMapWidth() {
        if (currentMap == null)
            return 0;
        int tileWidth = currentMap.getProperties().get("tilewidth", Integer.class);
        int mapWidth = currentMap.getProperties().get("width", Integer.class);
        return mapWidth * tileWidth * unitScale;
    }

    /**
     * Get map height in pixels
     */
    public float getMapHeight() {
        if (currentMap == null)
            return 0;
        int tileHeight = currentMap.getProperties().get("tileheight", Integer.class);
        int mapHeight = currentMap.getProperties().get("height", Integer.class);
        return mapHeight * tileHeight * unitScale;
    }

    /**
     * Check if a map is currently loaded
     */
    public boolean hasCurrentMap() {
        return currentMap != null;
    }

    // ======================
    // TILE INTERACTABLES
    // ======================

    @SuppressWarnings("unused") // Reserved for future tile-based interactable system
    private static final String INTERACTABLES_LAYER = "interactables";

    /**
     * Data class for tile-based interactable objects from TMX.
     * Use this to create lockers, chests, switches, etc. that change tile
     * appearance.
     */
    public static class TileInteractable {
        public final Rectangle bounds;
        public final String type; // e.g., "locker", "chest", "switch"
        public final String targetLayer; // Which tile layer to modify
        public final int tileX; // Tile X coordinate
        public final int tileY; // Tile Y coordinate
        public final int closedTileId; // GID of closed/inactive tile
        public final int openedTileId; // GID of opened/active tile
        public final String name; // Optional name
        public final String containedItem; // Item inside (e.g., "battery", "chocolate", null if empty)
        public boolean isOpen; // Current state
        public boolean itemCollected; // Has the item been picked up?

        public TileInteractable(Rectangle bounds, String type, String targetLayer,
                int tileX, int tileY, int closedTileId, int openedTileId, String name, String containedItem) {
            this.bounds = bounds;
            this.type = type;
            this.targetLayer = targetLayer;
            this.tileX = tileX;
            this.tileY = tileY;
            this.closedTileId = closedTileId;
            this.openedTileId = openedTileId;
            this.name = name;
            this.containedItem = containedItem;
            this.isOpen = false;
            this.itemCollected = false;
        }
        
        // Legacy constructor (no item)
        public TileInteractable(Rectangle bounds, String type, String targetLayer,
                int tileX, int tileY, int closedTileId, int openedTileId, String name) {
            this(bounds, type, targetLayer, tileX, tileY, closedTileId, openedTileId, name, null);
        }
        
        public boolean hasItem() {
            return containedItem != null && !containedItem.isEmpty() && !itemCollected;
        }

        public float getCenterX() {
            return bounds.x + bounds.width / 2f;
        }

        public float getCenterY() {
            return bounds.y + bounds.height / 2f;
        }
    }

    // Cache of tile interactables for current map
    private List<TileInteractable> tileInteractables = new ArrayList<>();

    /**
     * Load tile interactables by scanning all tile layers for tiles with
     * "interactable" property.
     * This reads properties directly from the tileset (TSX file).
     * 
     * TSX Tile Properties (add these in Tiled to the tile in the tileset):
     * - interactable: bool = true (marks this tile as interactable)
     * - type: string = "locker", "chest", etc.
     * - opened_tile: int = GID of the opened/active version of this tile
     * 
     * The system will automatically find these tiles when placed in any map.
     */
    private void loadTileInteractables() {
        tileInteractables.clear();
        if (currentMap == null)
            return;

        float tileW = tileWidth * unitScale;
        float tileH = tileHeight * unitScale;

        // Scan ALL tile layers for tiles with "interactable" property
        for (MapLayer mapLayer : currentMap.getLayers()) {
            if (!(mapLayer instanceof TiledMapTileLayer))
                continue;

            TiledMapTileLayer tileLayer = (TiledMapTileLayer) mapLayer;
            String layerName = tileLayer.getName();

            for (int y = 0; y < tileLayer.getHeight(); y++) {
                for (int x = 0; x < tileLayer.getWidth(); x++) {
                    TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                    if (cell == null || cell.getTile() == null)
                        continue;

                    TiledMapTile tile = cell.getTile();

                    // Check if tile has "interactable" property
                    Boolean isInteractable = tile.getProperties().get("interactable", Boolean.class);
                    if (isInteractable == null || !isInteractable)
                        continue;

                    // Get tile properties
                    String type = tile.getProperties().get("type", "unknown", String.class);
                    Integer openedTileGid = tile.getProperties().get("opened_tile", Integer.class);
                    String name = tile.getProperties().get("name", "Interactable", String.class);
                    // Get item contained in locker (can be null)
                    String containedItem = tile.getProperties().get("item", String.class);

                    if (openedTileGid == null) {
                        Gdx.app.log("TiledMapManager", "Warning: Interactable tile at (" + x + ", " + y +
                                ") missing 'opened_tile' property");
                        continue;
                    }

                    // Create bounds for this tile
                    float worldX = x * tileW;
                    float worldY = y * tileH;
                    Rectangle bounds = new Rectangle(worldX, worldY, tileW, tileH);

                    // Current tile GID is the "closed" state
                    int closedTileGid = tile.getId();

                    TileInteractable interactable = new TileInteractable(
                            bounds, type, layerName, x, y,
                            closedTileGid, openedTileGid, name, containedItem);
                    tileInteractables.add(interactable);

                    String itemInfo = containedItem != null ? " contains=" + containedItem : "";
                    Gdx.app.log("TiledMapManager", "Found interactable tile: " + type +
                            " '" + name + "' at (" + x + ", " + y + ") layer=" + layerName +
                            " closed=" + closedTileGid + " opened=" + openedTileGid + itemInfo);
                }
            }
        }

        Gdx.app.log("TiledMapManager",
                "Loaded " + tileInteractables.size() + " tile interactables from tileset properties");
    }

    /**
     * Get all tile interactables for the current map
     */
    public List<TileInteractable> getTileInteractables() {
        return tileInteractables;
    }

    /**
     * Find a tile interactable near a world position
     */
    public TileInteractable getTileInteractableAt(float worldX, float worldY, float range) {
        for (TileInteractable ti : tileInteractables) {
            float dx = ti.getCenterX() - worldX;
            float dy = ti.getCenterY() - worldY;
            float dist2 = dx * dx + dy * dy;
            if (dist2 <= range * range) {
                return ti;
            }
        }
        return null;
    }

    /**
     * Toggle a tile interactable (open <-> closed)
     * This actually changes the tile in the tile layer.
     */
    public boolean toggleTileInteractable(TileInteractable interactable) {
        if (currentMap == null || interactable == null)
            return false;

        MapLayer layer = currentMap.getLayers().get(interactable.targetLayer);
        if (layer == null || !(layer instanceof TiledMapTileLayer)) {
            Gdx.app.error("TiledMapManager", "Target layer not found: " + interactable.targetLayer);
            return false;
        }

        TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;

        // Get the tile to set based on current state
        int newTileId = interactable.isOpen ? interactable.closedTileId : interactable.openedTileId;

        // Find the tile in the tileset
        TiledMapTile newTile = findTileById(newTileId);
        if (newTile == null) {
            Gdx.app.error("TiledMapManager", "Tile not found with ID: " + newTileId);
            return false;
        }

        // Create a new cell with the tile
        TiledMapTileLayer.Cell newCell = new TiledMapTileLayer.Cell();
        newCell.setTile(newTile);

        // Set the cell at the interactable's position
        tileLayer.setCell(interactable.tileX, interactable.tileY, newCell);

        // Toggle state
        interactable.isOpen = !interactable.isOpen;

        Gdx.app.log("TiledMapManager", "Toggled interactable " + interactable.name +
                " to " + (interactable.isOpen ? "OPEN" : "CLOSED") + " (tile ID: " + newTileId + ")");

        return true;
    }

    /**
     * Set a tile at a specific position in a layer
     */
    public boolean setTileAt(String layerName, int tileX, int tileY, int tileGid) {
        if (currentMap == null)
            return false;

        MapLayer layer = currentMap.getLayers().get(layerName);
        if (layer == null || !(layer instanceof TiledMapTileLayer)) {
            return false;
        }

        TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
        TiledMapTile tile = findTileById(tileGid);

        if (tile == null)
            return false;

        TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
        cell.setTile(tile);
        tileLayer.setCell(tileX, tileY, cell);

        return true;
    }

    /**
     * Find a tile by its GID (global ID) across all tilesets
     */
    private TiledMapTile findTileById(int gid) {
        if (currentMap == null)
            return null;

        for (com.badlogic.gdx.maps.tiled.TiledMapTileSet tileset : currentMap.getTileSets()) {
            TiledMapTile tile = tileset.getTile(gid);
            if (tile != null) {
                return tile;
            }
        }
        return null;
    }

    // ======================
    // COLLISION DETECTION
    // ======================
    
    /**
     * Check if a world position contains a tile that should BLOCK flashlight.
     * Uses the "flashlight_passthrough" property from tileset (A4.tsx).
     * If flashlight_passthrough = false, the tile blocks light.
     * If flashlight_passthrough is not defined (or true), light passes through.
     * 
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @return true if position contains a tile that should block light
     */
    public boolean isWallTile(float worldX, float worldY) {
        if (currentMap == null)
            return false;

        // Convert world coordinates to tile coordinates
        int tileX = (int) (worldX / (tileWidth * unitScale));
        int tileY = (int) (worldY / (tileHeight * unitScale));

        // Out of bounds = no wall
        if (tileX < 0 || tileX >= mapWidthTiles || tileY < 0 || tileY >= mapHeightTiles) {
            return false;
        }

        // Check ALL tile layers for tiles with flashlight_passthrough = false
        for (MapLayer layer : currentMap.getLayers()) {
            if (layer instanceof TiledMapTileLayer) {
                TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
                TiledMapTileLayer.Cell cell = tileLayer.getCell(tileX, tileY);
                if (cell != null && cell.getTile() != null) {
                    // Check for "flashlight_passthrough" property from tileset
                    // If flashlight_passthrough = false, this tile blocks light
                    Boolean flashlightPassthrough = cell.getTile().getProperties().get("flashlight_passthrough", Boolean.class);
                    if (flashlightPassthrough != null && !flashlightPassthrough) {
                        return true; // Blocks light
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if a world position is walkable (not colliding with walls)
     * 
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @return true if the position is walkable
     */
    public boolean isWalkable(float worldX, float worldY) {
        if (currentMap == null)
            return true;

        // Convert world coordinates to tile coordinates
        // Note: Tiled uses top-left origin, but LibGDX uses bottom-left
        int tileX = (int) (worldX / (tileWidth * unitScale));
        int tileY = (int) (worldY / (tileHeight * unitScale));

        // Out of bounds check
        if (tileX < 0 || tileX >= mapWidthTiles || tileY < 0 || tileY >= mapHeightTiles) {
            return false;
        }

        // First check collision object layer (blocked_area, collision, etc.)
        // This takes priority over tile walkable property
        if (isInCollisionObject(worldX, worldY)) {
            return false;
        }

        // Check ALL tile layers for tiles with walkable=false property
        // This allows furniture, chairs, tables etc. on any layer to block movement
        for (MapLayer layer : currentMap.getLayers()) {
            if (layer instanceof TiledMapTileLayer) {
                TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
                TiledMapTileLayer.Cell cell = tileLayer.getCell(tileX, tileY);
                if (cell != null && cell.getTile() != null) {
                    // Check for walkable property
                    Boolean walkable = cell.getTile().getProperties().get("walkable", Boolean.class);
                    if (walkable != null && !walkable) {
                        return false;
                    }
                    // Also check for collision property
                    Boolean collision = cell.getTile().getProperties().get("collision", Boolean.class);
                    if (collision != null && collision) {
                        return false;
                    }

                    // Check for per-tile collision objects (objectgroup in tileset)
                    // These are stored in the tile's Objects property by LibGDX
                    MapObjects tileObjects = cell.getTile().getObjects();
                    if (tileObjects != null && tileObjects.getCount() > 0) {
                        // If the tile has any collision objects, it's not walkable
                        // Calculate local position within tile
                        float localX = worldX - (tileX * tileWidth * unitScale);
                        float localY = worldY - (tileY * tileHeight * unitScale);

                        for (MapObject obj : tileObjects) {
                            if (obj instanceof RectangleMapObject) {
                                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                                float scaledX = rect.x * unitScale;
                                float scaledY = rect.y * unitScale;
                                float scaledW = rect.width * unitScale;
                                float scaledH = rect.height * unitScale;

                                if (localX >= scaledX && localX <= scaledX + scaledW &&
                                        localY >= scaledY && localY <= scaledY + scaledH) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Check floor layer for walkability
        TiledMapTileLayer floorLayer = (TiledMapTileLayer) currentMap.getLayers().get(FLOOR_LAYER);
        if (floorLayer != null) {
            TiledMapTileLayer.Cell cell = floorLayer.getCell(tileX, tileY);
            if (cell != null && cell.getTile() != null) {
                // Check for "walkable" property on the floor tile
                Boolean walkable = cell.getTile().getProperties().get("walkable", Boolean.class);
                if (walkable != null) {
                    return walkable;
                }

                // Fallback to tile ID check for backwards compatibility
                int tileId = cell.getTile().getId();
                return WALKABLE_TILE_IDS.contains(tileId);
            }
        }

        return false; // No tile = not walkable
    }

    /**
     * Check if a position is inside any collision object
     */
    private boolean isInCollisionObject(float worldX, float worldY) {
        // Try multiple layer names
        MapLayer collisionLayer = currentMap.getLayers().get(COLLISION_LAYER);
        if (collisionLayer == null) {
            collisionLayer = currentMap.getLayers().get("Collision");
        }
        if (collisionLayer == null) {
            collisionLayer = currentMap.getLayers().get("blocked_area");
        }
        if (collisionLayer == null) {
            collisionLayer = currentMap.getLayers().get("blocked");
        }
        if (collisionLayer == null)
            return false;

        MapObjects objects = collisionLayer.getObjects();
        for (MapObject obj : objects) {
            if (obj instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                // Scale and check
                float scaledX = rect.x * unitScale;
                float scaledY = rect.y * unitScale;
                float scaledW = rect.width * unitScale;
                float scaledH = rect.height * unitScale;

                if (worldX >= scaledX && worldX <= scaledX + scaledW &&
                        worldY >= scaledY && worldY <= scaledY + scaledH) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a rectangle area is completely walkable
     * 
     * @param x      Left edge X
     * @param y      Bottom edge Y
     * @param width  Rectangle width
     * @param height Rectangle height
     * @return true if entire area is walkable
     */
    public boolean isAreaWalkable(float x, float y, float width, float height) {
        if (currentMap == null)
            return true;

        // Check all four corners and center
        return isWalkable(x, y) && // Bottom-left
                isWalkable(x + width, y) && // Bottom-right
                isWalkable(x, y + height) && // Top-left
                isWalkable(x + width, y + height) && // Top-right
                isWalkable(x + width / 2, y + height / 2); // Center
    }

    /**
     * Check collision with finer granularity - checks multiple points along edges
     * 
     * @param x      Left edge X
     * @param y      Bottom edge Y
     * @param width  Rectangle width
     * @param height Rectangle height
     * @return true if entire area is walkable
     */
    public boolean isAreaWalkableDetailed(float x, float y, float width, float height) {
        if (currentMap == null)
            return true;

        float step = Math.min(tileWidth, tileHeight) * unitScale * 0.5f; // Check every half tile

        // Check bottom edge
        for (float checkX = x; checkX <= x + width; checkX += step) {
            if (!isWalkable(checkX, y))
                return false;
        }

        // Check top edge
        for (float checkX = x; checkX <= x + width; checkX += step) {
            if (!isWalkable(checkX, y + height))
                return false;
        }

        // Check left edge
        for (float checkY = y; checkY <= y + height; checkY += step) {
            if (!isWalkable(x, checkY))
                return false;
        }

        // Check right edge
        for (float checkY = y; checkY <= y + height; checkY += step) {
            if (!isWalkable(x + width, checkY))
                return false;
        }

        return true;
    }

    /**
     * Get collision rectangles for tiles that would collide with a moving entity.
     * Useful for resolving collisions by pushing the entity out.
     * 
     * @param x      Entity X
     * @param y      Entity Y
     * @param width  Entity width
     * @param height Entity height
     * @return Array of tile rectangles that are solid
     */
    public Array<Rectangle> getCollidingTiles(float x, float y, float width, float height) {
        Array<Rectangle> tiles = new Array<>();
        if (currentMap == null)
            return tiles;

        float tileW = tileWidth * unitScale;
        float tileH = tileHeight * unitScale;

        // Get tile range to check
        int startTileX = Math.max(0, (int) (x / tileW) - 1);
        int endTileX = Math.min(mapWidthTiles - 1, (int) ((x + width) / tileW) + 1);
        int startTileY = Math.max(0, (int) (y / tileH) - 1);
        int endTileY = Math.min(mapHeightTiles - 1, (int) ((y + height) / tileH) + 1);

        TiledMapTileLayer floorLayer = (TiledMapTileLayer) currentMap.getLayers().get(FLOOR_LAYER);
        if (floorLayer == null)
            return tiles;

        for (int tileY = startTileY; tileY <= endTileY; tileY++) {
            for (int tileX = startTileX; tileX <= endTileX; tileX++) {
                TiledMapTileLayer.Cell cell = floorLayer.getCell(tileX, tileY);
                boolean isSolid = false;

                if (cell == null || cell.getTile() == null) {
                    // Empty tile outside floor area - treat as solid wall
                    isSolid = true;
                } else {
                    // First check for "walkable" property
                    Boolean walkable = cell.getTile().getProperties().get("walkable", Boolean.class);
                    if (walkable != null) {
                        isSolid = !walkable;
                    } else {
                        // Fallback to tile ID check
                        int tileId = cell.getTile().getId();
                        isSolid = !WALKABLE_TILE_IDS.contains(tileId);
                    }
                }

                if (isSolid) {
                    tiles.add(new Rectangle(tileX * tileW, tileY * tileH, tileW, tileH));
                }
            }
        }

        return tiles;
    }

    /**
     * Get the walkable bounds of the map (bounding box of floor tiles)
     * 
     * @return Rectangle representing walkable area, or null if no map
     */
    public Rectangle getWalkableBounds() {
        if (currentMap == null)
            return null;

        TiledMapTileLayer floorLayer = (TiledMapTileLayer) currentMap.getLayers().get(FLOOR_LAYER);
        if (floorLayer == null)
            return null;

        float tileW = tileWidth * unitScale;
        float tileH = tileHeight * unitScale;

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

        for (int y = 0; y < mapHeightTiles; y++) {
            for (int x = 0; x < mapWidthTiles; x++) {
                TiledMapTileLayer.Cell cell = floorLayer.getCell(x, y);
                if (cell != null && cell.getTile() != null) {
                    // Check walkable property first, then fallback to tile ID
                    Boolean walkable = cell.getTile().getProperties().get("walkable", Boolean.class);
                    boolean isWalkable = (walkable != null) ? walkable
                            : WALKABLE_TILE_IDS.contains(cell.getTile().getId());

                    if (isWalkable) {
                        float worldX = x * tileW;
                        float worldY = y * tileH;
                        minX = Math.min(minX, worldX);
                        minY = Math.min(minY, worldY);
                        maxX = Math.max(maxX, worldX + tileW);
                        maxY = Math.max(maxY, worldY + tileH);
                    }
                }
            }
        }

        if (minX == Float.MAX_VALUE)
            return null;
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Get tile width in world units
     */
    public float getTileWidth() {
        return tileWidth * unitScale;
    }

    /**
     * Get tile height in world units
     */
    public float getTileHeight() {
        return tileHeight * unitScale;
    }

    /**
     * Debug method to print tile info at a position
     * Call this with player position to debug collision issues
     */
    public void debugTileAt(float worldX, float worldY) {
        if (currentMap == null) {
            Gdx.app.log("TileDebug", "No map loaded");
            return;
        }

        int tileX = (int) (worldX / (tileWidth * unitScale));
        int tileY = (int) (worldY / (tileHeight * unitScale));

        Gdx.app.log("TileDebug", "Position: (" + worldX + ", " + worldY + ") -> Tile: (" + tileX + ", " + tileY + ")");

        for (MapLayer layer : currentMap.getLayers()) {
            if (layer instanceof TiledMapTileLayer) {
                TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
                TiledMapTileLayer.Cell cell = tileLayer.getCell(tileX, tileY);
                if (cell != null && cell.getTile() != null) {
                    int tileId = cell.getTile().getId();
                    Boolean walkable = cell.getTile().getProperties().get("walkable", Boolean.class);
                    MapObjects tileObjects = cell.getTile().getObjects();
                    int objCount = tileObjects != null ? tileObjects.getCount() : 0;

                    Gdx.app.log("TileDebug", "  Layer: " + layer.getName() +
                            ", TileID: " + tileId +
                            ", walkable: " + walkable +
                            ", collisionObjects: " + objCount);

                    // Print all properties
                    for (java.util.Iterator<String> it = cell.getTile().getProperties().getKeys(); it.hasNext();) {
                        String key = it.next();
                        Object val = cell.getTile().getProperties().get(key);
                        Gdx.app.log("TileDebug", "    Property: " + key + " = " + val);
                    }
                }
            }
        }
    }

    @Override
    public void dispose() {
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
        for (TiledMap map : loadedMaps.values()) {
            map.dispose();
        }
        loadedMaps.clear();
        
        // Dispose physical locker_key texture
        if (lockerKeyTexture != null) {
            lockerKeyTexture.dispose();
            lockerKeyTexture = null;
        }
    }
}
