package digiworld.maps;

import digiworld.core.World;

public final class HometownTileMap {
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

        // Light variation stripes for readability.
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
    }
}

