package digiworld.maps;

import digiworld.core.Structure;
import digiworld.core.World;

public class AlphaVillageTileMap {
    public static final int TRACKED_BUSH_X = 13;
    public static final int TRACKED_BUSH_Y = 15;
    public static final int HEART_CENTER_X = 25;
    public static final int HEART_CENTER_Y = 8;

    private final int width;
    private final int height;
    private final int[][] tiles;

    public AlphaVillageTileMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new int[height][width];
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public int getTile(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return TileMapPalette.WATER;
        return tiles[y][x];
    }

    public void setTile(int x, int y, int value) {
        if (x < 0 || x >= width || y < 0 || y >= height) return;
        tiles[y][x] = value;
    }

    public int[][] getTiles() {
        return tiles;
    }

    public void buildBase() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean edge = x < 3 || y < 3 || x >= width - 3 || y >= height - 3;
                tiles[y][x] = edge ? TileMapPalette.WATER : TileMapPalette.GRASS2;
            }
        }
    }

    public void buildFeatures() {
        buildTrackedBush();
        buildHeartArea();
        buildPaths();
    }

    private void buildTrackedBush() {
        for (int y = TRACKED_BUSH_Y - 2; y <= TRACKED_BUSH_Y + 2; y++) {
            for (int x = TRACKED_BUSH_X - 2; x <= TRACKED_BUSH_X + 2; x++) {
                boolean border = x == TRACKED_BUSH_X - 2 || x == TRACKED_BUSH_X + 2
                        || y == TRACKED_BUSH_Y - 2 || y == TRACKED_BUSH_Y + 2;
                tiles[y][x] = border ? TileMapPalette.GRASS_BUSH : TileMapPalette.GRASS2;
            }
        }
        tiles[TRACKED_BUSH_Y][TRACKED_BUSH_X] = TileMapPalette.GRASS_BUSH;
    }

    private void buildHeartArea() {
        int minX = HEART_CENTER_X - 8;
        int maxX = HEART_CENTER_X + 8;
        int minY = HEART_CENTER_Y - 6;
        int maxY = HEART_CENTER_Y + 6;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                boolean border = x == minX || x == maxX || y == minY || y == maxY;
                tiles[y][x] = border ? TileMapPalette.GRASS_BUSH : TileMapPalette.GRASS2;
            }
        }
        for (int y = HEART_CENTER_Y - 3; y <= HEART_CENTER_Y + 3; y++) {
            for (int x = HEART_CENTER_X - 4; x <= HEART_CENTER_X + 4; x++) {
                tiles[y][x] = TileMapPalette.GRASS2;
            }
        }
    }

    private void buildPaths() {
        for (int x = 20; x >= TRACKED_BUSH_X; x--) {
            for (int w = -1; w <= 1; w++) {
                tiles[19 + w][x] = TileMapPalette.GRASS2;
            }
        }
        for (int y = 19; y >= TRACKED_BUSH_Y; y--) {
            for (int w = -1; w <= 1; w++) {
                tiles[y][TRACKED_BUSH_X + w] = TileMapPalette.GRASS2;
            }
        }
        for (int y = TRACKED_BUSH_Y; y >= HEART_CENTER_Y; y--) {
            for (int w = -1; w <= 1; w++) {
                tiles[y][18 + w] = TileMapPalette.GRASS2;
            }
        }
        for (int x = 18; x <= HEART_CENTER_X; x++) {
            for (int w = -1; w <= 1; w++) {
                tiles[HEART_CENTER_Y + w][x] = TileMapPalette.GRASS2;
            }
        }
    }

    public void applyTo(World world) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                world.setTile(x, y, TileMapPalette.toTile(tiles[y][x]));
            }
        }
        world.addStructure(new Structure(25 * 32, 14 * 32, 64, 64, 32, 10, 10,
                "res/Structures/house_finalbot.png", "res/Structures/house_finalTop.png", 32));
    }
}
