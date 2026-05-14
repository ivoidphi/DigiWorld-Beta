package digiworld.maps;

import digiworld.core.World;
import java.util.Random;

public class BetaCityTileMap {
    private final int width;
    private final int height;
    private final int[][] tiles;
    private final Random random;

    public BetaCityTileMap(int width, int height) {
        this(width, height, new Random(2026));
    }

    public BetaCityTileMap(int width, int height, Random random) {
        this.width = width;
        this.height = height;
        this.tiles = new int[height][width];
        this.random = random;
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
                tiles[y][x] = TileMapPalette.GRASS2;
            }
        }
    }

    public void buildFeatures() {
        buildCrossRoads();
        buildBushClusters();
    }

    private void buildCrossRoads() {
        for (int y = 0; y < height; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                int x = width / 2 + dx;
                if (x >= 0 && x < width) {
                    tiles[y][x] = TileMapPalette.GRASS1;
                }
            }
        }
        for (int x = 0; x < width; x++) {
            for (int dy = -1; dy <= 1; dy++) {
                int y = height / 2 + dy;
                if (y >= 0 && y < height) {
                    tiles[y][x] = TileMapPalette.GRASS1;
                }
            }
        }
    }

    private void buildBushClusters() {
        for (int i = 0; i < 28; i++) {
            int cx = 4 + random.nextInt(Math.max(1, width - 8));
            int cy = 4 + random.nextInt(Math.max(1, height - 8));
            int size = 2 + random.nextInt(3);
            generateBushCluster(cx, cy, size);
        }
    }

    private void generateBushCluster(int cx, int cy, int size) {
        for (int y = cy - size; y <= cy + size; y++) {
            for (int x = cx - size; x <= cx + size; x++) {
                if (x < 2 || y < 2 || x >= width - 2 || y >= height - 2) continue;
                int dx = x - cx;
                int dy = y - cy;
                if (dx * dx + dy * dy <= size * size) {
                    if (Math.abs(x - width / 2) <= 2 || Math.abs(y - height / 2) <= 2) continue;
                    tiles[y][x] = TileMapPalette.GRASS_BUSH;
                }
            }
        }
    }

    public void applyTo(World world) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                world.setTile(x, y, TileMapPalette.toTile(tiles[y][x]));
            }
        }
    }
}
