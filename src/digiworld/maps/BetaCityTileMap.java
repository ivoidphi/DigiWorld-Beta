package digiworld.maps;

import digiworld.core.World;

import java.util.Random;

public final class BetaCityTileMap {
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
        return map;
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
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                world.setTile(x, y, TileMapPalette.toTile(map[y][x]));
            }
        }
    }
}
