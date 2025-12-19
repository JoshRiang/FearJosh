package com.fearjosh.frontend.factory;

import com.badlogic.gdx.math.MathUtils;
import com.fearjosh.frontend.world.*;
import com.fearjosh.frontend.world.objects.Table;
import com.fearjosh.frontend.world.objects.Locker;
import com.fearjosh.frontend.world.items.Battery;
import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.difficulty.DifficultyStrategy;

import java.util.ArrayList;
import java.util.List;

public class RoomFactory {

    private static final float WALL_MARGIN = 40f;
    private static final float LOCKER_WIDTH = 40f;
    private static final float LOCKER_HEIGHT = 80f;
    private static final float TABLE_WIDTH = 75f;
    private static final float TABLE_HEIGHT = 45f;

    private static final float DOOR_WIDTH = 80f;
    private static final float DOOR_THICKNESS = 20f;

    public static Room createRoom(RoomId id, float worldWidth, float worldHeight) {
        List<Table> tables = new ArrayList<>();
        List<Locker> lockers = new ArrayList<>();
        List<Interactable> interactables = new ArrayList<>();

        // --- Hitung door zones untuk sisi yang punya tetangga ---
        List<float[]> doorZones = buildDoorZones(id, worldWidth, worldHeight);

        // --- Generate loker (1..7) di satu sisi dinding, berjajar & nempel tembok ---
        int maxLockers = 7;
        int lockerCount = MathUtils.random(1, maxLockers);
        Side lockerSide = Side.randomSide();

        // Kalau sisi kiri/kanan DAN sisi itu punya gate, limit jadi 1
        boolean sideHasGate = false;
        if (lockerSide == Side.LEFT && id.hasLeft())
            sideHasGate = true;
        if (lockerSide == Side.RIGHT && id.hasRight())
            sideHasGate = true;
        if (lockerSide == Side.TOP && id.hasUp())
            sideHasGate = true;
        if (lockerSide == Side.BOTTOM && id.hasDown())
            sideHasGate = true;

        // khusus permintaan: kalau side kiri/kanan dan ada gate, max 1
        if (sideHasGate && (lockerSide == Side.LEFT || lockerSide == Side.RIGHT)) {
            lockerCount = 1;
        }

        placeLockers(lockerSide, lockerCount, lockers, interactables,
                worldWidth, worldHeight, doorZones);

        // --- Generate meja (0..2), nempel ke tembok lain yang tidak ada locker ---
        int tableCount = MathUtils.random(0, 2);
        placeTables(tableCount, tables, lockers, worldWidth, worldHeight, doorZones, lockerSide);

        // --- Chance spawn baterai ---
        DifficultyStrategy ds = GameManager.getInstance().getDifficultyStrategy();
        float baseChance = 0.6f * ds.itemSpawnRateMultiplier();
        if (baseChance < 0f)
            baseChance = 0f;
        if (baseChance > 1f)
            baseChance = 1f;
        boolean spawnBattery = MathUtils.randomBoolean(baseChance);
        if (spawnBattery) {
            boolean onTable = MathUtils.randomBoolean();
            if (onTable && !tables.isEmpty()) {
                // Baterai di atas meja
                Table t = tables.get(MathUtils.random(0, tables.size() - 1));
                float bw = 12f;
                float bh = 20f;
                float bx = t.getCenterX() - bw / 2f;
                float by = t.getY() + t.getHeight() + 4f;
                interactables.add(new Battery(bx, by, bw, bh));
            } else if (!lockers.isEmpty()) {
                // Baterai di dalam loker
                Locker locker = lockers.get(MathUtils.random(0, lockers.size() - 1));
                float bw = 12f;
                float bh = 20f;
                float bx = locker.getCenterX() - bw / 2f;
                float by = locker.getY() + bh;
                Battery batteryInside = new Battery(bx, by, bw, bh);
                locker.setContainedBattery(batteryInside);
            }
        }

        return new Room(id, tables, lockers, interactables);
    }

    // ----------------- helper enum -----------------

    private enum Side {
        TOP, BOTTOM, LEFT, RIGHT;

        public static Side randomSide() {
            int idx = MathUtils.random(0, 3);
            return values()[idx];
        }
    }

    // ----------------- door zones -----------------

    private static List<float[]> buildDoorZones(RoomId id, float worldWidth, float worldHeight) {
        List<float[]> zones = new ArrayList<>();

        float centerX = worldWidth / 2f;
        float centerY = worldHeight / 2f;

        float doorMinX = centerX - DOOR_WIDTH / 2f;
        float doorMinY = centerY - DOOR_WIDTH / 2f;

        // top door
        if (id.hasUp()) {
            zones.add(new float[] { doorMinX, worldHeight - DOOR_THICKNESS, DOOR_WIDTH, DOOR_THICKNESS });
        }
        // bottom door
        if (id.hasDown()) {
            zones.add(new float[] { doorMinX, 0f, DOOR_WIDTH, DOOR_THICKNESS });
        }
        // left door
        if (id.hasLeft()) {
            zones.add(new float[] { 0f, doorMinY, DOOR_THICKNESS, DOOR_WIDTH });
        }
        // right door
        if (id.hasRight()) {
            zones.add(new float[] { worldWidth - DOOR_THICKNESS, doorMinY, DOOR_THICKNESS, DOOR_WIDTH });
        }

        return zones;
    }

    private static boolean overlapsRect(float x, float y, float w, float h,
            float x2, float y2, float w2, float h2) {
        return x < x2 + w2 &&
                x + w > x2 &&
                y < y2 + h2 &&
                y + h > y2;
    }

    private static boolean overlapsZones(float x, float y, float w, float h, List<float[]> zones) {
        for (float[] r : zones) {
            if (overlapsRect(x, y, w, h, r[0], r[1], r[2], r[3])) {
                return true;
            }
        }
        return false;
    }

    // ----------------- place lockers -----------------

    private static void placeLockers(Side side,
            int lockerCount,
            List<Locker> lockers,
            List<Interactable> interactables,
            float worldWidth,
            float worldHeight,
            List<float[]> doorZones) {

        int placed = 0;
        float gap = 8f;

        switch (side) {
            case TOP: {
                float y = worldHeight - WALL_MARGIN - LOCKER_HEIGHT;
                float x = WALL_MARGIN;
                while (placed < lockerCount && x + LOCKER_WIDTH <= worldWidth - WALL_MARGIN) {
                    float candidateX = x;
                    float candidateY = y;
                    if (!overlapsZones(candidateX, candidateY, LOCKER_WIDTH, LOCKER_HEIGHT, doorZones) &&
                            !overlapsAnyLocker(candidateX, candidateY, LOCKER_WIDTH, LOCKER_HEIGHT, lockers)) {
                        Locker locker = new Locker(candidateX, candidateY, LOCKER_WIDTH, LOCKER_HEIGHT, interactables);
                        lockers.add(locker);
                        placed++;
                    }
                    x += LOCKER_WIDTH + gap;
                }
                break;
            }
            case BOTTOM: {
                float y = WALL_MARGIN;
                float x = WALL_MARGIN;
                while (placed < lockerCount && x + LOCKER_WIDTH <= worldWidth - WALL_MARGIN) {
                    float candidateX = x;
                    float candidateY = y;
                    if (!overlapsZones(candidateX, candidateY, LOCKER_WIDTH, LOCKER_HEIGHT, doorZones) &&
                            !overlapsAnyLocker(candidateX, candidateY, LOCKER_WIDTH, LOCKER_HEIGHT, lockers)) {
                        Locker locker = new Locker(candidateX, candidateY, LOCKER_WIDTH, LOCKER_HEIGHT, interactables);
                        lockers.add(locker);
                        placed++;
                    }
                    x += LOCKER_WIDTH + gap;
                }
                break;
            }
            case LEFT: {
                float x = WALL_MARGIN;
                float y = WALL_MARGIN;
                while (placed < lockerCount && y + LOCKER_HEIGHT <= worldHeight - WALL_MARGIN) {
                    float candidateX = x;
                    float candidateY = y;
                    if (!overlapsZones(candidateX, candidateY, LOCKER_WIDTH, LOCKER_HEIGHT, doorZones) &&
                            !overlapsAnyLocker(candidateX, candidateY, LOCKER_WIDTH, LOCKER_HEIGHT, lockers)) {
                        Locker locker = new Locker(candidateX, candidateY, LOCKER_WIDTH, LOCKER_HEIGHT, interactables);
                        lockers.add(locker);
                        placed++;
                    }
                    y += LOCKER_HEIGHT + gap;
                }
                break;
            }
            case RIGHT: {
                float x = worldWidth - WALL_MARGIN - LOCKER_WIDTH;
                float y = WALL_MARGIN;
                while (placed < lockerCount && y + LOCKER_HEIGHT <= worldHeight - WALL_MARGIN) {
                    float candidateX = x;
                    float candidateY = y;
                    if (!overlapsZones(candidateX, candidateY, LOCKER_WIDTH, LOCKER_HEIGHT, doorZones) &&
                            !overlapsAnyLocker(candidateX, candidateY, LOCKER_WIDTH, LOCKER_HEIGHT, lockers)) {
                        Locker locker = new Locker(candidateX, candidateY, LOCKER_WIDTH, LOCKER_HEIGHT, interactables);
                        lockers.add(locker);
                        placed++;
                    }
                    y += LOCKER_HEIGHT + gap;
                }
                break;
            }
        }
    }

    private static boolean overlapsAnyLocker(float x, float y, float w, float h, List<Locker> lockers) {
        for (Locker locker : lockers) {
            if (overlapsRect(x, y, w, h,
                    locker.getX(), locker.getY(), locker.getWidth(), locker.getHeight())) {
                return true;
            }
        }
        return false;
    }

    // ----------------- place tables -----------------

    private static void placeTables(int tableCount,
            List<Table> tables,
            List<Locker> lockers,
            float worldWidth,
            float worldHeight,
            List<float[]> doorZones,
            Side lockerSide) {

        int placed = 0;
        int attemptsTotal = 0;

        while (placed < tableCount && attemptsTotal < 100) {
            attemptsTotal++;

            // Pilih sisi tembok selain yang dipakai locker
            Side side = randomSideExcept(lockerSide);
            float x = 0f, y = 0f;

            switch (side) {
                case TOP:
                    y = worldHeight - WALL_MARGIN - TABLE_HEIGHT - 10f;
                    x = MathUtils.random(WALL_MARGIN, worldWidth - WALL_MARGIN - TABLE_WIDTH);
                    break;
                case BOTTOM:
                    y = WALL_MARGIN + 10f;
                    x = MathUtils.random(WALL_MARGIN, worldWidth - WALL_MARGIN - TABLE_WIDTH);
                    break;
                case LEFT:
                    x = WALL_MARGIN + 10f;
                    y = MathUtils.random(WALL_MARGIN, worldHeight - WALL_MARGIN - TABLE_HEIGHT);
                    break;
                case RIGHT:
                    x = worldWidth - WALL_MARGIN - TABLE_WIDTH - 10f;
                    y = MathUtils.random(WALL_MARGIN, worldHeight - WALL_MARGIN - TABLE_HEIGHT);
                    break;
            }

            if (overlapsZones(x, y, TABLE_WIDTH, TABLE_HEIGHT, doorZones))
                continue;
            if (overlapsAnyLocker(x, y, TABLE_WIDTH, TABLE_HEIGHT, lockers))
                continue;
            if (overlapsAnyTable(x, y, TABLE_WIDTH, TABLE_HEIGHT, tables))
                continue;

            tables.add(new Table(x, y, TABLE_WIDTH, TABLE_HEIGHT));
            placed++;
        }
    }

    private static Side randomSideExcept(Side exclude) {
        Side side;
        do {
            side = Side.randomSide();
        } while (side == exclude);
        return side;
    }

    private static boolean overlapsAnyTable(float x, float y, float w, float h, List<Table> tables) {
        for (Table table : tables) {
            if (overlapsRect(x, y, w, h,
                    table.getX(), table.getY(), table.getWidth(), table.getHeight())) {
                return true;
            }
        }
        return false;
    }
}
