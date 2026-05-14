package digiworld.maps;

import digiworld.core.World;
import digiworld.core.Structure;

public final class AlphaVillageTileMap {
    public static final int TRACKED_BUSH_X = 13;
    public static final int TRACKED_BUSH_Y = 15;
    public static final int HEART_CENTER_X = 25;
    public static final int HEART_CENTER_Y = 8;

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

        for (int y = TRACKED_BUSH_Y - 2; y <= TRACKED_BUSH_Y + 2; y++) {
            for (int x = TRACKED_BUSH_X - 2; x <= TRACKED_BUSH_X + 2; x++) {
                boolean border = x == TRACKED_BUSH_X - 2 || x == TRACKED_BUSH_X + 2
                        || y == TRACKED_BUSH_Y - 2 || y == TRACKED_BUSH_Y + 2;
                map[y][x] = border ? TileMapPalette.GRASS_BUSH : TileMapPalette.GRASS2;
            }
        }
        map[TRACKED_BUSH_Y][TRACKED_BUSH_X] = TileMapPalette.GRASS_BUSH;

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
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                world.setTile(x, y, TileMapPalette.toTile(map[y][x]));
            }
        }

        world.addStructure(new Structure(25 * 32, 14 * 32, 64, 64, 32, 10, 10, "res/Structures/house_finalbot.png", "res/Structures/house_finalTop.png", 32));
    }
}
