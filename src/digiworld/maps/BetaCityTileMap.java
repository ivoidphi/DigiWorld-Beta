package digiworld.maps;

import digiworld.core.Structure;
import digiworld.core.World;

import java.util.Random;

public final class BetaCityTileMap {
    private static final int TILE_SIZE = 32;
    private static final String TREE_PATH = "res/Structures/treefinal.png";
    private static final String HOUSE_BASE_PATH = "res/Structures/house_finalbot.png";
    private static final String HOUSE_ROOF_PATH = "res/Structures/house_finalTop.png";
    private static final String LAB_BASE_PATH = "res/Structures/lab_finalbot.png";
    private static final String LAB_ROOF_PATH = "res/Structures/lab_finaltop.png";
    private static final int BUILDING_WIDTH_TILES = 4;
    private static final int BUILDING_HEIGHT_TILES = 3;
    private static final int BUILDING_ROOF_HEIGHT = 32;
    private static final int TREE_WIDTH_TILES = 1;
    private static final int TREE_HEIGHT_TILES = 2;
    private static final int[][] BUILDING_ANCHORS = {
            {7, 5},
            {8, 12},
            {15, 8},
            {18, 14},
            {37, 6},
            {39, 14},
            {46, 12},
            {6, 17},
            {16, 17},
            {8, 26},
            {7, 34},
            {18, 31},
            {34, 16},
            {38, 29},
            {40, 35},
            {47, 25},
            {47, 32}
    };
    public static final int[][] HOUSE_DOORS = {
            {16, 10},
            {19, 16},
            {47, 14},
            {9, 28},
            {39, 31},
            {48, 34}
    };
    private static final int[][] TREE_ANCHORS = {
            {4, 5}, {5, 7}, {10, 10}, {13, 4}, {18, 5}, {22, 13},
            {34, 5}, {37, 11}, {43, 7}, {49, 7}, {51, 18}, {45, 18},
            {4, 25}, {6, 29}, {11, 24}, {13, 35}, {20, 27}, {22, 35},
            {34, 27}, {35, 34}, {42, 24}, {44, 26}, {47, 37}, {50, 35}
    };

    private BetaCityTileMap() {
    }

    public static int[][] build(int width, int height) {
        int[][] map = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                map[y][x] = TileMapPalette.GRASS2;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                int x = width / 2 + dx;
                if (x >= 0 && x < width) {
                    map[y][x] = TileMapPalette.GRASS1;
                }
            }
        }
        for (int x = 0; x < width; x++) {
            for (int dy = -1; dy <= 1; dy++) {
                int y = height / 2 + dy;
                if (y >= 0 && y < height) {
                    map[y][x] = TileMapPalette.GRASS1;
                }
            }
        }

        Random random = new Random(2026);
        for (int i = 0; i < 28; i++) {
            int cx = 4 + random.nextInt(Math.max(1, width - 8));
            int cy = 4 + random.nextInt(Math.max(1, height - 8));
            int size = 2 + random.nextInt(3);
            generateBushCluster(map, cx, cy, size);
        }
        clearReservedFootprints(map, BUILDING_ANCHORS, BUILDING_WIDTH_TILES, BUILDING_HEIGHT_TILES);
        return map;
    }

    private static void clearReservedFootprints(int[][] map, int[][] anchors, int widthTiles, int heightTiles) {
        for (int[] anchor : anchors) {
            for (int y = anchor[1]; y < anchor[1] + heightTiles; y++) {
                for (int x = anchor[0]; x < anchor[0] + widthTiles; x++) {
                    if (y >= 0 && x >= 0 && y < map.length && x < map[y].length) {
                        map[y][x] = TileMapPalette.GRASS2;
                    }
                }
            }
        }
    }

    private static void generateBushCluster(int[][] map, int cx, int cy, int size) {
        int height = map.length;
        int width = map[0].length;
        for (int y = cy - size; y <= cy + size; y++) {
            for (int x = cx - size; x <= cx + size; x++) {
                if (x < 2 || y < 2 || x >= width - 2 || y >= height - 2) {
                    continue;
                }
                int dx = x - cx;
                int dy = y - cy;
                if (dx * dx + dy * dy <= size * size) {
                    if (Math.abs(x - width / 2) <= 2 || Math.abs(y - height / 2) <= 2) {
                        continue;
                    }
                    map[y][x] = TileMapPalette.GRASS_BUSH;
                }
            }
        }
    }

    public static void applyTo(World world) {
        int[][] map = build(world.getWidth(), world.getHeight());
        world.clearStructures();
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                world.setTile(x, y, TileMapPalette.toTile(map[y][x]));
            }
        }
        addCityStructures(world, map);
    }

    private static void addCityStructures(World world, int[][] map) {
        Random random = new Random(0xB37A_C17CL);
        boolean[][] occupied = new boolean[map.length][map[0].length];
        for (int i = 0; i < BUILDING_ANCHORS.length; i++) {
            int[] anchor = BUILDING_ANCHORS[i];
            int tileX = anchor[0];
            int tileY = anchor[1];
            if (!canPlaceStructure(map, occupied, tileX, tileY, BUILDING_WIDTH_TILES, BUILDING_HEIGHT_TILES)) {
                continue;
            }
            if (i == 0 || i == 4 || i == 11) {
                addLaboratory(world, tileX, tileY);
            } else {
                addHouse(world, tileX, tileY);
            }
            markOccupied(occupied, tileX - 1, tileY - 1, BUILDING_WIDTH_TILES + 2, BUILDING_HEIGHT_TILES + 2);
        }

        for (int[] anchor : TREE_ANCHORS) {
            int tileX = anchor[0] + random.nextInt(3) - 1;
            int tileY = anchor[1] + random.nextInt(3) - 1;
            if (canPlaceStructure(map, occupied, tileX, tileY, TREE_WIDTH_TILES, TREE_HEIGHT_TILES)) {
                addTree(world, tileX, tileY);
                markOccupied(occupied, tileX, tileY - 1, TREE_WIDTH_TILES, TREE_HEIGHT_TILES + 1);
            }
        }
    }

    private static boolean canPlaceStructure(int[][] map, boolean[][] occupied, int tileX, int tileY, int widthTiles, int heightTiles) {
        for (int y = tileY; y < tileY + heightTiles; y++) {
            for (int x = tileX; x < tileX + widthTiles; x++) {
                if (y < 0 || x < 0 || y >= map.length || x >= map[y].length) {
                    return false;
                }
                if (occupied[y][x]) {
                    return false;
                }
                if (map[y][x] == TileMapPalette.GRASS_BUSH || isProtectedCityTile(map, x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void markOccupied(boolean[][] occupied, int tileX, int tileY, int widthTiles, int heightTiles) {
        for (int y = tileY; y < tileY + heightTiles; y++) {
            for (int x = tileX; x < tileX + widthTiles; x++) {
                if (y >= 0 && x >= 0 && y < occupied.length && x < occupied[y].length) {
                    occupied[y][x] = true;
                }
            }
        }
    }

    private static boolean isProtectedCityTile(int[][] map, int x, int y) {
        int centerX = map[0].length / 2;
        int centerY = map.length / 2;
        boolean mainRoad = Math.abs(x - centerX) <= 3 || Math.abs(y - centerY) <= 3;
        boolean playerSpawn = x >= 26 && x <= 30 && y >= 19 && y <= 23;
        boolean trialmasterPlaza = x >= 26 && x <= 30 && y >= 10 && y <= 14;
        boolean aceJazzPlaza = x >= 31 && x <= 35 && y >= 24 && y <= 28;
        return mainRoad || playerSpawn || trialmasterPlaza || aceJazzPlaza;
    }

    private static void addHouse(World world, int tileX, int tileY) {
        world.addStructure(new Structure(
                tileX * TILE_SIZE,
                tileY * TILE_SIZE,
                BUILDING_WIDTH_TILES * TILE_SIZE,
                BUILDING_HEIGHT_TILES * TILE_SIZE,
                0,
                10,
                10,
                HOUSE_BASE_PATH,
                HOUSE_ROOF_PATH,
                BUILDING_ROOF_HEIGHT
        ));
    }

    private static void addLaboratory(World world, int tileX, int tileY) {
        world.addStructure(new Structure(
                tileX * TILE_SIZE,
                tileY * TILE_SIZE,
                BUILDING_WIDTH_TILES * TILE_SIZE,
                BUILDING_HEIGHT_TILES * TILE_SIZE,
                -1,
                10,
                10,
                LAB_BASE_PATH,
                LAB_ROOF_PATH,
                BUILDING_ROOF_HEIGHT
        ));
    }

    private static void addTree(World world, int tileX, int tileY) {
        world.addStructure(new Structure(
                tileX * TILE_SIZE,
                tileY * TILE_SIZE - TILE_SIZE,
                TREE_WIDTH_TILES * TILE_SIZE,
                TREE_HEIGHT_TILES * TILE_SIZE,
                0,
                0,
                0,
                TREE_PATH,
                null,
                0
        ));
    }
}
