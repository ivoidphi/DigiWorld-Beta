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
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                world.setTile(x, y, TileMapPalette.toTile(map[y][x]));
            }
        }
        addHouse(world, LEFT_HOUSE_X, LEFT_HOUSE_Y);
        addHouse(world, CENTER_HOUSE_X, CENTER_HOUSE_Y);
        addHouse(world, RIGHT_HOUSE_X, RIGHT_HOUSE_Y);
        addLaboratory(world, LAB_X, LAB_Y);
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

    private static void addHouse(World world, int tileX, int tileY) {
        world.addStructure(new Structure(
                tileX * 32,
                tileY * 32,
                HOUSE_WIDTH_TILES * 32,
                HOUSE_HEIGHT_TILES * 32,
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
                tileX * 32,
                tileY * 32,
                HOUSE_WIDTH_TILES * 32,
                HOUSE_HEIGHT_TILES * 32,
                -1,
                10,
                10,
                "res/Structures/lab_finalbot.png",
                "res/Structures/lab_finaltop.png",
                32
        ));
    }
}
