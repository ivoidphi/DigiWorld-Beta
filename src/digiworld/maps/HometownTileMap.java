package digiworld.maps;

import digiworld.core.Structure;
import digiworld.core.World;

public class HometownTileMap {
    private final int width;
    private final int height;
    private final int[][] tiles;

    public HometownTileMap(int width, int height) {
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
                boolean edge = x < 2 || y < 2 || x >= width - 2 || y >= height - 2;
                tiles[y][x] = edge ? TileMapPalette.WATER : TileMapPalette.GRASS2;
            }
        }
    }

    public void buildFeatures() {
        for (int y = 3; y < height - 3; y++) {
            for (int x = 3; x < width - 3; x++) {
                if ((x + y) % 5 == 0) {
                    tiles[y][x] = TileMapPalette.GRASS1;
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
        world.addStructure(new Structure(10 * 32, 14 * 32, 128, 96, 32, 10, 10,
                "res/Structures/house_finalbot.png", "res/Structures/house_finalTop.png", 32));
    }
}
