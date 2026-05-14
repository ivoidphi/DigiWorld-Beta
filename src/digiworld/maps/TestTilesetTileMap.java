package digiworld.maps;

import digiworld.core.World;

public class TestTilesetTileMap {
    private final int width;
    private final int height;
    private final int[][] tiles;
    private OgmoMap ogmoMap;

    public TestTilesetTileMap() {
        this.width = 15;
        this.height = 14;
        this.tiles = new int[height][width];
        loadOgmoMap();
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

    private void loadOgmoMap() {
        ogmoMap = MapLoader.load("res/testtileset.json");
        if (ogmoMap == null) {
            buildDefault();
        }
    }

    public void applyTo(World world) {
        if (ogmoMap != null) {
            world.setOgmoMap(ogmoMap);

            TileSet groundTileset = TileSet.fromTilesetName("ground");
            TileSet waterTileset = TileSet.fromTilesetName("water");

            if (groundTileset != null) world.addTileSet("ground", groundTileset);
            if (waterTileset != null) world.addTileSet("water", waterTileset);
        } else {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    world.setTile(x, y, TileMapPalette.toTile(tiles[y][x]));
                }
            }
        }
    }

    private void buildDefault() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = TileMapPalette.GRASS2;
            }
        }
    }
}