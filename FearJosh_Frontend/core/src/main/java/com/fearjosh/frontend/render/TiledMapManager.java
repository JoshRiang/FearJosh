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

public class TiledMapManager implements Disposable {

    public interface ExitDoorListener {
        void onExitDoorOverlap(DoorInfo doorInfo, String currentMapName);
    }

    private static final String MAPS_PATH = "Maps/";
    private static final String DOORS_LAYER = "Doors";
    
    private ExitDoorListener exitDoorListener;
    private boolean wasOverlappingExitLastFrame = false;

    private static final String FLOOR_LAYER = "floor_wall";
    private static final String COLLISION_LAYER = "collision";

    // Y-sorted layers
    private static final java.util.Set<String> Y_SORTED_LAYERS = new java.util.HashSet<>(java.util.Arrays.asList(
            "tables", "chairs", "furnitures_props"));

    // Walkable tile IDs
    private static final java.util.Set<Integer> WALKABLE_TILE_IDS = new java.util.HashSet<>(java.util.Arrays.asList(
            504, 281, 248, 264));

    private final TmxMapLoader mapLoader;
    private final Map<RoomId, TiledMap> loadedMaps;
    private final Map<RoomId, String> roomMapFiles;

    private TiledMap currentMap;
    private RoomId currentRoomId; // room tracking
    private OrthogonalTiledMapRenderer mapRenderer;
    private float unitScale = 1f;

    // Cached collision
    private int tileWidth;
    private int tileHeight;
    private int mapWidthTiles;
    private int mapHeightTiles;

    // Locker key texture
    private com.badlogic.gdx.graphics.Texture lockerKeyTexture;
    private static final float KEY_RENDER_SIZE = 16f;

    public TiledMapManager() {
        this.mapLoader = new TmxMapLoader();
        this.loadedMaps = new HashMap<>();
        this.roomMapFiles = new HashMap<>();

        registerRoomMaps();
        loadLockerKeyTexture();
    }
    
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

    private void registerRoomMaps() {
        roomMapFiles.put(RoomId.HALLWAY, "hallway.tmx");
        roomMapFiles.put(RoomId.GYM, "gym.tmx");
        roomMapFiles.put(RoomId.CLASS_1A, "CLASSROOM_1A.tmx");
        roomMapFiles.put(RoomId.CLASS_2A, "CLASSROOM_2A.tmx");
        roomMapFiles.put(RoomId.LOBBY, "lobby.tmx");
        roomMapFiles.put(RoomId.JANITOR, "janitor.tmx");
    }

    public boolean hasMapForRoom(RoomId roomId) {
        return roomMapFiles.containsKey(roomId);
    }

    public void loadMapForRoom(RoomId roomId) {
        if (!hasMapForRoom(roomId)) {
            currentMap = null;
            currentRoomId = null;
            if (mapRenderer != null) {
                mapRenderer.setMap(null);
            }
            resetExitDoorTracking();
            return;
        }

        if (loadedMaps.containsKey(roomId)) {
            currentMap = loadedMaps.get(roomId);
        } else {
            String mapFile = MAPS_PATH + roomMapFiles.get(roomId);
            try {
                TiledMap map = mapLoader.load(mapFile);
                loadedMaps.put(roomId, map);
                currentMap = map;
                Gdx.app.log("TiledMapManager", "Loaded map: " + mapFile);
            } catch (Exception e) {
                Gdx.app.error("TiledMapManager", "Failed to load map: " + mapFile, e);
                currentMap = null;
                currentRoomId = null;
                return;
            }
        }

        currentRoomId = roomId;
        resetExitDoorTracking();

        tileWidth = currentMap.getProperties().get("tilewidth", Integer.class);
        tileHeight = currentMap.getProperties().get("tileheight", Integer.class);
        mapWidthTiles = currentMap.getProperties().get("width", Integer.class);
        mapHeightTiles = currentMap.getProperties().get("height", Integer.class);

        if (mapRenderer == null) {
            mapRenderer = new OrthogonalTiledMapRenderer(currentMap, unitScale);
        } else {
            mapRenderer.setMap(currentMap);
        }

        logDoorInfo();
        loadTileInteractables();
        loadExitKeyTriggers();
    }
    
    private void loadExitKeyTriggers() {
        currentExitKeyTriggers.clear();
        
        if (currentMap == null) return;
        
        MapLayer keyLayer = currentMap.getLayers().get(EXIT_KEY_LAYER);
        if (keyLayer == null) {
            Gdx.app.log("TiledMapManager", "No exit_key layer found in current map");
            return;
        }
        
        final float DEFAULT_TRIGGER_SIZE = 48f;
        
        MapObjects objects = keyLayer.getObjects();
        for (MapObject obj : objects) {
            String itemType = obj.getProperties().get("item", String.class);
            
            if (itemType == null || itemType.isEmpty()) {
                Gdx.app.log("TiledMapManager", "Skipping exit_key object without 'item' property");
                continue;
            }
            
            Rectangle scaledBounds = null;
            
            if (obj instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                
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
                    // Point object
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
                Float x = obj.getProperties().get("x", Float.class);
                Float y = obj.getProperties().get("y", Float.class);
                
                if (x != null && y != null) {
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
                int objectId = obj.getProperties().get("id", 0, Integer.class);
                if (objectId == 0) {
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
    
    public ExitKeyTrigger checkExitKeyTrigger(Rectangle playerBounds) {
        for (ExitKeyTrigger trigger : currentExitKeyTriggers) {
            if (!trigger.consumed && trigger.overlaps(playerBounds)) {
                return trigger;
            }
        }
        return null;
    }
    
    public ExitKeyTrigger checkExitKeyTrigger(float playerX, float playerY) {
        for (ExitKeyTrigger trigger : currentExitKeyTriggers) {
            if (!trigger.consumed && trigger.overlaps(playerX, playerY)) {
                return trigger;
            }
        }
        return null;
    }
    
    public String consumeExitKeyTrigger(ExitKeyTrigger trigger) {
        if (trigger != null) {
            trigger.consumed = true;
            Gdx.app.log("TiledMapManager", "Consumed exit_key trigger: " + trigger.itemType + 
                " objectId=" + trigger.objectId);
        }
        return trigger != null ? String.valueOf(trigger.objectId) : null;
    }
    
    public void markTriggerConsumed(int objectId) {
        for (ExitKeyTrigger trigger : currentExitKeyTriggers) {
            if (trigger.objectId == objectId) {
                trigger.consumed = true;
                Gdx.app.log("TiledMapManager", "Restored consumed state for trigger objectId=" + objectId);
                break;
            }
        }
    }
    
    public Array<ExitKeyTrigger> getExitKeyTriggers() {
        return currentExitKeyTriggers;
    }
    
    public void renderPhysicalLockerKey(SpriteBatch batch) {
        if (lockerKeyTexture == null) {
            // load texture
            loadLockerKeyTexture();
            if (lockerKeyTexture == null) {
                return;
            }
        }
        
        for (ExitKeyTrigger trigger : currentExitKeyTriggers) {
            if (!trigger.consumed && "locker_key".equalsIgnoreCase(trigger.itemType)) {
                float renderX = trigger.bounds.x + (trigger.bounds.width - KEY_RENDER_SIZE) / 2f;
                float renderY = trigger.bounds.y + (trigger.bounds.height - KEY_RENDER_SIZE) / 2f;
                
                batch.draw(lockerKeyTexture, renderX, renderY, KEY_RENDER_SIZE, KEY_RENDER_SIZE);
            }
        }
    }

    private void logDoorInfo() {
        Array<DoorInfo> doors = getDoors();
        Gdx.app.log("TiledMapManager", "Found " + doors.size + " doors in current map:");
        for (DoorInfo door : doors) {
            Gdx.app.log("TiledMapManager", "  Door '" + door.doorName + "' -> " + door.targetRoom +
                    " at bounds: x=" + door.bounds.x + ", y=" + door.bounds.y +
                    ", w=" + door.bounds.width + ", h=" + door.bounds.height);
        }

        Gdx.app.log("TiledMapManager", "Map layers:");
        for (MapLayer layer : currentMap.getLayers()) {
            Gdx.app.log("TiledMapManager",
                    "  - " + layer.getName() + " (type: " + layer.getClass().getSimpleName() + ")");
        }
    }

    public void setUnitScale(float scale) {
        this.unitScale = scale;
        if (mapRenderer != null && currentMap != null) {
            mapRenderer.dispose();
            mapRenderer = new OrthogonalTiledMapRenderer(currentMap, unitScale);
        }
    }

    public void render(OrthographicCamera camera) {
        if (mapRenderer != null && currentMap != null) {
            mapRenderer.setView(camera);
            mapRenderer.render();
        }
    }

    public void render(OrthographicCamera camera, int[] layerIndices) {
        if (mapRenderer != null && currentMap != null) {
            mapRenderer.setView(camera);
            mapRenderer.render(layerIndices);
        }
    }

    public void renderBelowPlayer(OrthographicCamera camera) {
        if (mapRenderer == null || currentMap == null)
            return;

        mapRenderer.setView(camera);

        int playerLayerIndex = getLayerIndex("Player");
        if (playerLayerIndex < 0) {
            int[] belowLayers = { 0, 1 };
            mapRenderer.render(belowLayers);
        } else {
            // pre-player layers
            int[] belowLayers = new int[playerLayerIndex];
            for (int i = 0; i < playerLayerIndex; i++) {
                belowLayers[i] = i;
            }
            if (belowLayers.length > 0) {
                mapRenderer.render(belowLayers);
            }
        }
    }

    public void renderAbovePlayer(OrthographicCamera camera) {
        if (mapRenderer == null || currentMap == null)
            return;

        mapRenderer.setView(camera);

        int playerLayerIndex = getLayerIndex("Player");
        int totalLayers = currentMap.getLayers().getCount();

        List<Integer> layerIndices = new ArrayList<>();
        int startIndex = playerLayerIndex >= 0 ? playerLayerIndex + 1 : 0;

        for (int i = startIndex; i < totalLayers; i++) {
            MapLayer layer = currentMap.getLayers().get(i);
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

    // Y-SORTED RENDERING

    private static class YSortedElement {
        float y;
        Runnable render;
        boolean isPlayer;

        YSortedElement(float y, Runnable render) {
            this(y, render, false);
        }

        YSortedElement(float y, Runnable render, boolean isPlayer) {
            this.y = y;
            this.render = render;
            this.isPlayer = isPlayer;
        }
    }

    public void renderWithYSort(OrthographicCamera camera, SpriteBatch batch, float playerY, Runnable playerRenderer) {
        if (currentMap == null) {
            playerRenderer.run();
            return;
        }

        List<YSortedElement> elements = new ArrayList<>();

        elements.add(new YSortedElement(playerY, playerRenderer, true));

        for (MapLayer layer : currentMap.getLayers()) {
            if (layer instanceof TiledMapTileLayer && Y_SORTED_LAYERS.contains(layer.getName())) {
                TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
                collectTilesForYSort(tileLayer, elements, batch);
            }
        }

        Collections.sort(elements, (a, b) -> Float.compare(b.y, a.y));

        boolean batchActive = false;
        for (YSortedElement element : elements) {
            if (element.isPlayer) {
                if (batchActive) {
                    batch.end();
                    batchActive = false;
                }
                element.render.run();
            } else {
                if (!batchActive) {
                    batch.begin();
                    batchActive = true;
                }
                element.render.run();
            }
        }
        if (batchActive) {
            batch.end();
        }
    }

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

                    float sortY = worldY + (tileH / 2f);

                    MapObjects tileObjects = tile.getObjects();
                    if (tileObjects != null && tileObjects.getCount() > 0) {
                        for (MapObject obj : tileObjects) {
                            if (obj instanceof RectangleMapObject) {
                                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                                sortY = worldY + (rect.y + rect.height / 2f) * unitScale;
                                break;
                            }
                        }
                    }

                    final float drawX = worldX;
                    final float drawY = worldY;
                    final float drawW = tileW;
                    final float drawH = tileH;
                    final TextureRegion drawRegion = region;

                    elements.add(new YSortedElement(sortY, () -> {
                        batch.draw(drawRegion, drawX, drawY, drawW, drawH);
                    }));
                }
            }
        }
    }

    public void renderBelowPlayerExcludingYSorted(OrthographicCamera camera) {
        if (mapRenderer == null || currentMap == null)
            return;

        mapRenderer.setView(camera);

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

    // DOOR DETECTION

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

    public static class SpawnInfo {
        public final float x;
        public final float y;
        public final String fromRoom;
        
        public String facing;
        public int priority;
        public String nearDoor;

        public SpawnInfo(float x, float y, String fromRoom) {
            this.x = x;
            this.y = y;
            this.fromRoom = fromRoom;
            this.facing = null;
            this.priority = 0;
            this.nearDoor = null;
        }
    }
    
    public static class ExitKeyTrigger {
        public final Rectangle bounds;
        public final String itemType;
        public final int objectId;
        public boolean consumed;
        
        public ExitKeyTrigger(Rectangle bounds, String itemType, int objectId) {
            this.bounds = bounds;
            this.itemType = itemType;
            this.objectId = objectId;
            this.consumed = false;
        }
        
        public boolean overlaps(float x, float y) {
            return bounds.contains(x, y);
        }
        
        public boolean overlaps(Rectangle playerBounds) {
            return bounds.overlaps(playerBounds);
        }
        
        public float getCenterX() {
            return bounds.x + bounds.width / 2f;
        }
        
        public float getCenterY() {
            return bounds.y + bounds.height / 2f;
        }
    }

    private static final String SPAWN_LAYER = "spawn";
    private static final String EXIT_KEY_LAYER = "exit_key";
    
    private Array<ExitKeyTrigger> currentExitKeyTriggers = new Array<>();

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

                boolean matches = normalizedObjFrom.equals(normalizedFrom) ||
                        matchesRoomName(normalizedFrom, normalizedObjFrom);

                if (matches) {
                    float x, y;
                    if (obj instanceof RectangleMapObject) {
                        Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                        x = rect.x * unitScale;
                        y = rect.y * unitScale;
                    } else {
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

    private boolean matchesRoomName(String roomId, String spawnFromRoom) {
        String classPattern = "(CLASS(?:ROOM)?_?)(\\d+[A-Z])";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(classPattern);

        java.util.regex.Matcher m1 = pattern.matcher(roomId);
        java.util.regex.Matcher m2 = pattern.matcher(spawnFromRoom);

        if (m1.find() && m2.find()) {
            return m1.group(2).equals(m2.group(2));
        }

        return false;
    }

    public SpawnInfo getSpawnPoint(RoomId fromRoom) {
        if (fromRoom == null)
            return null;
        return getSpawnPoint(fromRoom.name());
    }

    public SpawnInfo getFirstSpawnPoint() {
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
            Boolean isJoshSpawn = obj.getProperties().get("josh_spawn", Boolean.class);
            
            if (isJoshSpawn != null && isJoshSpawn) {
                float x, y;
                if (obj instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                    x = rect.x * unitScale;
                    y = rect.y * unitScale;
                } else {
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
            Boolean isJoshSpawn = obj.getProperties().get("josh_spawn", Boolean.class);
            
            if (isJoshSpawn != null && isJoshSpawn) {
                float x, y;
                if (obj instanceof RectangleMapObject) {
                    Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                    x = rect.x * unitScale;
                    y = rect.y * unitScale;
                } else {
                    Float objX = obj.getProperties().get("x", Float.class);
                    Float objY = obj.getProperties().get("y", Float.class);
                    if (objX == null || objY == null) continue;
                    x = objX * unitScale;
                    y = objY * unitScale;
                }
                
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

    public Array<DoorInfo> getDoors() {
        Array<DoorInfo> doors = new Array<>();
        if (currentMap == null) {
            return doors;
        }

        MapLayer doorsLayer = currentMap.getLayers().get(DOORS_LAYER);
        if (doorsLayer == null) {
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

                Rectangle scaledRect = new Rectangle(
                        rect.x * unitScale,
                        rect.y * unitScale,
                        rect.width * unitScale,
                        rect.height * unitScale);

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

    private RoomId parseRoomName(String roomName) {
        if (roomName == null || roomName.isEmpty())
            return null;

        String normalized = roomName.toUpperCase().replace(" ", "_").replace("-", "_");

        // Room aliases
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
                try {
                    return RoomId.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    Gdx.app.error("TiledMapManager", "Unknown room: " + roomName + " (normalized: " + normalized + ")");
                    return null;
                }
        }
    }

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

    // Exit doors

    public void setExitDoorListener(ExitDoorListener listener) {
        this.exitDoorListener = listener;
    }

    public DoorInfo checkExitDoorOverlap(float x, float y, float width, float height) {
        if (currentMap == null) {
            wasOverlappingExitLastFrame = false;
            return null;
        }

        Rectangle playerBounds = new Rectangle(x, y, width, height);
        Array<DoorInfo> doors = getDoors();

        boolean currentlyOverlappingExit = false;
        DoorInfo exitDoor = null;

        for (DoorInfo door : doors) {
            if (playerBounds.overlaps(door.bounds)) {
                boolean isExitDoor = isExitDoor(door);
                if (isExitDoor) {
                    currentlyOverlappingExit = true;
                    exitDoor = door;
                    break;
                }
            }
        }

        boolean shouldFireEvent = currentlyOverlappingExit && !wasOverlappingExitLastFrame;
        wasOverlappingExitLastFrame = currentlyOverlappingExit;

        if (shouldFireEvent && exitDoor != null) {
            Gdx.app.log("TiledMapManager", 
                "EXIT DOOR OVERLAP DETECTED! Door: " + exitDoor.doorName + 
                " -> " + exitDoor.targetRoom);
            
            if (exitDoorListener != null) {
                String mapName = currentRoomId != null ? currentRoomId.name() : "UNKNOWN";
                exitDoorListener.onExitDoorOverlap(exitDoor, mapName);
            }
            
            return exitDoor;
        }

        return null;
    }

    public boolean isExitDoor(DoorInfo door) {
        if (door == null) return false;
        
        if (door.doorName != null) {
            String nameLower = door.doorName.toLowerCase();
            if (nameLower.contains("exit") || nameLower.equals("exit")) {
                return true;
            }
        }
        
        if (door.targetRoom != null) {
            String targetLower = door.targetRoom.toLowerCase();
            if (targetLower.contains("exit") || targetLower.equals("exit")) {
                return true;
            }
        }
        
        return false;
    }

    public void resetExitDoorTracking() {
        wasOverlappingExitLastFrame = false;
    }

    public TiledMap getCurrentMap() {
        return currentMap;
    }

    public float getMapWidth() {
        if (currentMap == null)
            return 0;
        int tileWidth = currentMap.getProperties().get("tilewidth", Integer.class);
        int mapWidth = currentMap.getProperties().get("width", Integer.class);
        return mapWidth * tileWidth * unitScale;
    }

    public float getMapHeight() {
        if (currentMap == null)
            return 0;
        int tileHeight = currentMap.getProperties().get("tileheight", Integer.class);
        int mapHeight = currentMap.getProperties().get("height", Integer.class);
        return mapHeight * tileHeight * unitScale;
    }

    public boolean hasCurrentMap() {
        return currentMap != null;
    }

    // Interactables

    @SuppressWarnings("unused")
    private static final String INTERACTABLES_LAYER = "interactables";

    public static class TileInteractable {
        public final Rectangle bounds;
        public final String type;
        public final String targetLayer;
        public final int tileX;
        public final int tileY;
        public final int closedTileId;
        public final int openedTileId;
        public final String name;
        public final String containedItem;
        public boolean isOpen;
        public boolean itemCollected;

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

    private List<TileInteractable> tileInteractables = new ArrayList<>();

    private void loadTileInteractables() {
        tileInteractables.clear();
        if (currentMap == null)
            return;

        float tileW = tileWidth * unitScale;
        float tileH = tileHeight * unitScale;

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

                    Boolean isInteractable = tile.getProperties().get("interactable", Boolean.class);
                    if (isInteractable == null || !isInteractable)
                        continue;

                    String type = tile.getProperties().get("type", "unknown", String.class);
                    Integer openedTileGid = tile.getProperties().get("opened_tile", Integer.class);
                    String name = tile.getProperties().get("name", "Interactable", String.class);
                    String containedItem = tile.getProperties().get("item", String.class);

                    if (openedTileGid == null) {
                        Gdx.app.log("TiledMapManager", "Warning: Interactable tile at (" + x + ", " + y +
                                ") missing 'opened_tile' property");
                        continue;
                    }

                    float worldX = x * tileW;
                    float worldY = y * tileH;
                    Rectangle bounds = new Rectangle(worldX, worldY, tileW, tileH);

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

    public List<TileInteractable> getTileInteractables() {
        return tileInteractables;
    }

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

    public boolean toggleTileInteractable(TileInteractable interactable) {
        if (currentMap == null || interactable == null)
            return false;

        MapLayer layer = currentMap.getLayers().get(interactable.targetLayer);
        if (layer == null || !(layer instanceof TiledMapTileLayer)) {
            Gdx.app.error("TiledMapManager", "Target layer not found: " + interactable.targetLayer);
            return false;
        }

        TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;

        int newTileId = interactable.isOpen ? interactable.closedTileId : interactable.openedTileId;

        TiledMapTile newTile = findTileById(newTileId);
        if (newTile == null) {
            Gdx.app.error("TiledMapManager", "Tile not found with ID: " + newTileId);
            return false;
        }

        TiledMapTileLayer.Cell newCell = new TiledMapTileLayer.Cell();
        newCell.setTile(newTile);

        tileLayer.setCell(interactable.tileX, interactable.tileY, newCell);

        interactable.isOpen = !interactable.isOpen;

        Gdx.app.log("TiledMapManager", "Toggled interactable " + interactable.name +
                " to " + (interactable.isOpen ? "OPEN" : "CLOSED") + " (tile ID: " + newTileId + ")");

        return true;
    }

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

    // Collision
    
    public boolean isWallTile(float worldX, float worldY) {
        if (currentMap == null)
            return false;

        int tileX = (int) (worldX / (tileWidth * unitScale));
        int tileY = (int) (worldY / (tileHeight * unitScale));

        if (tileX < 0 || tileX >= mapWidthTiles || tileY < 0 || tileY >= mapHeightTiles) {
            return false;
        }

        for (MapLayer layer : currentMap.getLayers()) {
            if (layer instanceof TiledMapTileLayer) {
                TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
                TiledMapTileLayer.Cell cell = tileLayer.getCell(tileX, tileY);
                if (cell != null && cell.getTile() != null) {
                    Boolean flashlightPassthrough = cell.getTile().getProperties().get("flashlight_passthrough", Boolean.class);
                    if (flashlightPassthrough != null && !flashlightPassthrough) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isWalkable(float worldX, float worldY) {
        if (currentMap == null)
            return true;

        int tileX = (int) (worldX / (tileWidth * unitScale));
        int tileY = (int) (worldY / (tileHeight * unitScale));

        if (tileX < 0 || tileX >= mapWidthTiles || tileY < 0 || tileY >= mapHeightTiles) {
            return false;
        }

        if (isInCollisionObject(worldX, worldY)) {
            return false;
        }

        for (MapLayer layer : currentMap.getLayers()) {
            if (layer instanceof TiledMapTileLayer) {
                TiledMapTileLayer tileLayer = (TiledMapTileLayer) layer;
                TiledMapTileLayer.Cell cell = tileLayer.getCell(tileX, tileY);
                if (cell != null && cell.getTile() != null) {
                    Boolean walkable = cell.getTile().getProperties().get("walkable", Boolean.class);
                    if (walkable != null && !walkable) {
                        return false;
                    }
                    Boolean collision = cell.getTile().getProperties().get("collision", Boolean.class);
                    if (collision != null && collision) {
                        return false;
                    }

                    MapObjects tileObjects = cell.getTile().getObjects();
                    if (tileObjects != null && tileObjects.getCount() > 0) {
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

        TiledMapTileLayer floorLayer = (TiledMapTileLayer) currentMap.getLayers().get(FLOOR_LAYER);
        if (floorLayer != null) {
            TiledMapTileLayer.Cell cell = floorLayer.getCell(tileX, tileY);
            if (cell != null && cell.getTile() != null) {
                Boolean walkable = cell.getTile().getProperties().get("walkable", Boolean.class);
                if (walkable != null) {
                    return walkable;
                }

                int tileId = cell.getTile().getId();
                return WALKABLE_TILE_IDS.contains(tileId);
            }
        }

        return false;
    }

    private boolean isInCollisionObject(float worldX, float worldY) {
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

    public boolean isAreaWalkable(float x, float y, float width, float height) {
        if (currentMap == null)
            return true;

        return isWalkable(x, y) &&
                isWalkable(x + width, y) &&
                isWalkable(x, y + height) &&
                isWalkable(x + width, y + height) &&
                isWalkable(x + width / 2, y + height / 2);
    }

    public boolean isAreaWalkableDetailed(float x, float y, float width, float height) {
        if (currentMap == null)
            return true;

        float step = Math.min(tileWidth, tileHeight) * unitScale * 0.5f;

        for (float checkX = x; checkX <= x + width; checkX += step) {
            if (!isWalkable(checkX, y))
                return false;
        }

        for (float checkX = x; checkX <= x + width; checkX += step) {
            if (!isWalkable(checkX, y + height))
                return false;
        }

        for (float checkY = y; checkY <= y + height; checkY += step) {
            if (!isWalkable(x, checkY))
                return false;
        }

        for (float checkY = y; checkY <= y + height; checkY += step) {
            if (!isWalkable(x + width, checkY))
                return false;
        }

        return true;
    }

    public Array<Rectangle> getCollidingTiles(float x, float y, float width, float height) {
        Array<Rectangle> tiles = new Array<>();
        if (currentMap == null)
            return tiles;

        float tileW = tileWidth * unitScale;
        float tileH = tileHeight * unitScale;

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
                    isSolid = true;
                } else {
                    Boolean walkable = cell.getTile().getProperties().get("walkable", Boolean.class);
                    if (walkable != null) {
                        isSolid = !walkable;
                    } else {
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

    public float getTileWidth() {
        return tileWidth * unitScale;
    }

    public float getTileHeight() {
        return tileHeight * unitScale;
    }

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
        
        if (lockerKeyTexture != null) {
            lockerKeyTexture.dispose();
            lockerKeyTexture = null;
        }
    }
}
