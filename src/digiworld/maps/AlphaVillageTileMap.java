package digiworld.maps;

import digiworld.core.World;
import digiworld.core.Structure;

public final class AlphaVillageTileMap {
    public static final int TRACKED_BUSH_X = 13;
    public static final int TRACKED_BUSH_Y = 15;
    public static final int HEART_CENTER_X = 25;
    public static final int HEART_CENTER_Y = 8;
    private static final String TREE_PATH = "res/Structures/treefinal.png";
    private static final String HOUSE_BASE_PATH = "res/Structures/house_finalbot.png";
    private static final String HOUSE_ROOF_PATH = "res/Structures/house_finalTop.png";
    private static final int TILE_SIZE = 32;
    private static final int HOUSE_WIDTH_TILES = 4;
    private static final int HOUSE_HEIGHT_TILES = 3;
    private static final int HOUSE_ROOF_HEIGHT = 32;
    private static final int[][] CHIEF_REI_HOUSE_ANCHORS = {
            {20, 15},
            {28, 15},
            {20, 22},
            {28, 22}
    };
    public static final int[][] HOUSE_DOORS = {
            {21, 17},
            {29, 17},
            {21, 24},
            {29, 24}
    };

    private AlphaVillageTileMap() {
    }

    public static int[][] build(int width, int height) {
        int[][] map = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean edge = x < 3 || y < 3 || x >= width - 3 || y >= height - 3;
                map[y][x] = edge ? TileMapPalette.WATER : TileMapPalette.GRASS2;
            }
        }

        // Mystic forest marker shape around one valid encounter bush.
        for (int y = TRACKED_BUSH_Y - 2; y <= TRACKED_BUSH_Y + 2; y++) {
            for (int x = TRACKED_BUSH_X - 2; x <= TRACKED_BUSH_X + 2; x++) {
                boolean border = x == TRACKED_BUSH_X - 2 || x == TRACKED_BUSH_X + 2
                        || y == TRACKED_BUSH_Y - 2 || y == TRACKED_BUSH_Y + 2;
                map[y][x] = border ? TileMapPalette.GRASS_BUSH : TileMapPalette.GRASS2;
            }
        }
        map[TRACKED_BUSH_Y][TRACKED_BUSH_X] = TileMapPalette.GRASS_BUSH;

        // Heart of the forest (large zone + central arena for Aldrich).
        int minX = HEART_CENTER_X - 8;
        int maxX = HEART_CENTER_X + 8;
        int minY = HEART_CENTER_Y - 6;
        int maxY = HEART_CENTER_Y + 6;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                boolean border = x == minX || x == maxX || y == minY || y == maxY;
                map[y][x] = border ? TileMapPalette.GRASS_BUSH : TileMapPalette.GRASS2;
            }
        }
        for (int y = HEART_CENTER_Y - 3; y <= HEART_CENTER_Y + 3; y++) {
            for (int x = HEART_CENTER_X - 4; x <= HEART_CENTER_X + 4; x++) {
                map[y][x] = TileMapPalette.GRASS2;
            }
        }

        // Walkable paths.
        for (int x = 20; x >= TRACKED_BUSH_X; x--) {
            for (int w = -1; w <= 1; w++) {
                map[19 + w][x] = TileMapPalette.GRASS2;
            }
        }
        for (int y = 19; y >= TRACKED_BUSH_Y; y--) {
            for (int w = -1; w <= 1; w++) {
                map[y][TRACKED_BUSH_X + w] = TileMapPalette.GRASS2;
            }
        }
        for (int y = TRACKED_BUSH_Y; y >= HEART_CENTER_Y; y--) {
            for (int w = -1; w <= 1; w++) {
                map[y][18 + w] = TileMapPalette.GRASS2;
            }
        }
        for (int x = 18; x <= HEART_CENTER_X; x++) {
            for (int w = -1; w <= 1; w++) {
                map[HEART_CENTER_Y + w][x] = TileMapPalette.GRASS2;
            }
        }

        return map;
    }

    public static void applyTo(World world) {
        int[][] map = build(world.getWidth(), world.getHeight());
        world.clearStructures();
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                world.setTile(x, y, TileMapPalette.toTile(map[y][x]));
            }
        }
        addForestStructures(world, map);
        addChiefReiHouses(world, map);
    }

    private static void addChiefReiHouses(World world, int[][] map) {
        for (int[] anchor : CHIEF_REI_HOUSE_ANCHORS) {
            int tileX = anchor[0];
            int tileY = anchor[1];
            if (canPlaceChiefReiHouse(map, tileX, tileY)) {
                addHouse(world, tileX, tileY);
            }
        }
    }

    private static boolean canPlaceChiefReiHouse(int[][] map, int tileX, int tileY) {
        for (int y = tileY; y < tileY + HOUSE_HEIGHT_TILES; y++) {
            for (int x = tileX; x < tileX + HOUSE_WIDTH_TILES; x++) {
                if (y < 0 || x < 0 || y >= map.length || x >= map[y].length) {
                    return false;
                }
                if (map[y][x] == TileMapPalette.GRASS_BUSH || map[y][x] == TileMapPalette.WATER) {
                    return false;
                }
                if (isInsideBushEncirclement(x, y) || isInsideChiefReiWalkLoop(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isInsideBushEncirclement(int x, int y) {
        boolean trackedBushRing = x >= TRACKED_BUSH_X - 2 && x <= TRACKED_BUSH_X + 2
                && y >= TRACKED_BUSH_Y - 2 && y <= TRACKED_BUSH_Y + 2;
        boolean heartRing = x >= HEART_CENTER_X - 8 && x <= HEART_CENTER_X + 8
                && y >= HEART_CENTER_Y - 6 && y <= HEART_CENTER_Y + 6;
        return trackedBushRing || heartRing;
    }

    private static boolean isInsideChiefReiWalkLoop(int x, int y) {
        return x >= 26 && x <= 28 && y >= 19 && y <= 21;
    }

    private static void addHouse(World world, int tileX, int tileY) {
        world.addStructure(new Structure(
                tileX * TILE_SIZE,
                tileY * TILE_SIZE,
                HOUSE_WIDTH_TILES * TILE_SIZE,
                HOUSE_HEIGHT_TILES * TILE_SIZE,
                0,
                10,
                10,
                HOUSE_BASE_PATH,
                HOUSE_ROOF_PATH,
                HOUSE_ROOF_HEIGHT
        ));
    }

    private static void addForestStructures(World world, int[][] map) {
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                if (!shouldPlaceTree(map, x, y, world.getWidth(), world.getHeight())) {
                    continue;
                }
                world.addStructure(new Structure(
                        x * TILE_SIZE,
                        y * TILE_SIZE - TILE_SIZE,
                        TILE_SIZE,
                        TILE_SIZE * 2,
                        0,
                        0,
                        0,
                        TREE_PATH,
                        null,
                        0
                ));
            }
        }
    }

    private static boolean shouldPlaceTree(int[][] map, int x, int y, int width, int height) {
        if (map[y][x] == TileMapPalette.GRASS_BUSH || map[y][x] == TileMapPalette.WATER) {
            return false;
        }
        // The tree image is 2 tiles tall, so don't hang it into water above.
        if (y <= 0 || map[y - 1][x] == TileMapPalette.WATER) {
            return false;
        }

        // Close off the northwest shore access with a deliberate tree band.
        if (y == 11 && x >= 11 && x <= 16) {
            return true;
        }

        // Remove the narrow 2-column protruding strip near the sign.
        if (x >= 18 && x <= 19 && y >= 5 && y <= 11) {
            return false;
        }

        // Keep the right-side clearing open by removing these vertical tree columns.
        if ((x == 32 || x == 38) && y >= 13 && y <= 24) {
            return false;
        }

        // Keep the village sign area open.
        if (x >= 18 && x <= 31 && y >= 3 && y <= 7) {
            return false;
        }

        // Keep spawn / Chief Rei plaza open with some breathing room.
        if (x >= 20 && x <= 31 && y >= 15 && y <= 25) {
            return false;
        }

        // Keep the Aldrich arena and its approach open.
        if (x >= 14 && x <= 34 && y >= 2 && y <= 16) {
            return false;
        }

        // Keep the tracked bush lane and center field readable.
        if (x >= 9 && x <= 22 && y >= 12 && y <= 22) {
            return false;
        }

        boolean topForest = y >= 5 && y <= 9 && x >= 12 && x <= width - 8;
        boolean leftForest = x >= 5 && x <= 10 && y >= 10 && y <= height - 9;
        boolean rightForest = x >= width - 11 && x <= width - 6 && y >= 10 && y <= height - 10;
        boolean lowerForest = y >= 25 && y <= height - 6 && x >= 8 && x <= width - 8;

        // Add denser coverage around the wild beast side and the trainer side.
        boolean trackedBushSideForest = x >= 8 && x <= 12 && y >= 13 && y <= 22;
        boolean chiefReiSideForest = x >= 32 && x <= 40 && y >= 16 && y <= 24;

        return topForest || leftForest || rightForest || lowerForest
                || trackedBushSideForest || chiefReiSideForest;
    }
}
