package digiworld.maps;

import digiworld.core.Structure;
import digiworld.core.World;

public final class HometownTileMap {
    public static final int HOUSE_WIDTH_TILES = 4;
    public static final int HOUSE_HEIGHT_TILES = 3;
    public static final int LEFT_HOUSE_X = 16;
    public static final int LEFT_HOUSE_Y = 11;
    public static final int CENTER_HOUSE_X = 24;
    public static final int CENTER_HOUSE_Y = 11;
    public static final int RIGHT_HOUSE_X = 24;
    public static final int RIGHT_HOUSE_Y = 22;
    public static final int LAB_X = 16;
    public static final int LAB_Y = 22;
    public static final int HOUSE_DOOR_OFFSET_X = 1;
    public static final int HOUSE_DOOR_OFFSET_Y = 2;
    public static final int TELEPORTER_X = 20;
    public static final int TELEPORTER_Y = 16;
    public static final int TELEPORTER_WIDTH_TILES = 4;
    public static final int TELEPORTER_HEIGHT_TILES = 4;
    private static final String TREE_PATH = "res/Structures/treefinal.png";
    private static final String TELEPORTER_PATH = "res/Structures/teleporter.png";
    private static final int TILE_SIZE = 32;

    private HometownTileMap() {
    }

    public static int[][] build(int width, int height) {
        int[][] map = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean edge = x < 2 || y < 2 || x >= width - 2 || y >= height - 2;
                map[y][x] = edge ? TileMapPalette.WATER : TileMapPalette.GRASS2;
            }
        }

        for (int y = 3; y < height - 3; y++) {
            for (int x = 3; x < width - 3; x++) {
                if ((x + y) % 5 == 0) {
                    map[y][x] = TileMapPalette.GRASS1;
                }
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
        addForestBoundary(world);
        addHouse(world, LEFT_HOUSE_X, LEFT_HOUSE_Y);
        addHouse(world, CENTER_HOUSE_X, CENTER_HOUSE_Y);
        addHouse(world, RIGHT_HOUSE_X, RIGHT_HOUSE_Y);
        addLaboratory(world, LAB_X, LAB_Y);
        addTeleporter(world);
    }

    public static int centerHouseDoorTileX() {
        return CENTER_HOUSE_X + HOUSE_DOOR_OFFSET_X;
    }

    public static int centerHouseDoorTileY() {
        return CENTER_HOUSE_Y + HOUSE_DOOR_OFFSET_Y;
    }

    public static int laboratoryDoorTileX() {
        return LAB_X + HOUSE_DOOR_OFFSET_X;
    }

    public static int laboratoryDoorTileY() {
        return LAB_Y + HOUSE_DOOR_OFFSET_Y;
    }

    private static void addForestBoundary(World world) {
        int width = world.getWidth();
        int height = world.getHeight();
        for (int y = 4; y <= height - 5; y++) {
            for (int x = 4; x <= width - 5; x++) {
                if (!isForestBoundaryTile(x, y, width, height)) {
                    continue;
                }
                addTree(world, x, y);
            }
        }
    }

    private static boolean isForestBoundaryTile(int x, int y, int width, int height) {
        boolean northForest = y >= 4 && y <= 7;
        boolean southForest = y >= height - 8 && y <= height - 5;
        boolean westForest = x >= 4 && x <= 7;
        boolean eastForest = x >= width - 8 && x <= width - 5;
        return northForest || southForest || westForest || eastForest;
    }

    private static void addTree(World world, int tileX, int tileY) {
        world.addStructure(new Structure(
                tileX * TILE_SIZE,
                tileY * TILE_SIZE - TILE_SIZE,
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

    private static void addTeleporter(World world) {
        world.addStructure(new Structure(
                TELEPORTER_X * TILE_SIZE,
                TELEPORTER_Y * TILE_SIZE,
                TELEPORTER_WIDTH_TILES * TILE_SIZE,
                TELEPORTER_HEIGHT_TILES * TILE_SIZE,
                TELEPORTER_HEIGHT_TILES * TILE_SIZE,
                0,
                0,
                TELEPORTER_PATH,
                null,
                0
        ));
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
                "res/Structures/house_finalbot.png",
                "res/Structures/house_finalTop.png",
                32
        ));
    }

    private static void addLaboratory(World world, int tileX, int tileY) {
        world.addStructure(new Structure(
                tileX * TILE_SIZE,
                tileY * TILE_SIZE,
                HOUSE_WIDTH_TILES * TILE_SIZE,
                HOUSE_HEIGHT_TILES * TILE_SIZE,
                -1,
                10,
                10,
                "res/Structures/lab_finalbot.png",
                "res/Structures/lab_finaltop.png",
                32
        ));
    }
}
