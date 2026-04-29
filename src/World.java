import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

public class World {
    private final String name;
    private final TileType[][] tiles;
    private final Npc[] npcs;
    private final int spawnTileX;
    private final int spawnTileY;
    private static final BufferedImage TILE_GRASS1 = loadTile("res/tiles/grass1.png");
    private static final BufferedImage TILE_GRASS2 = loadTile("res/tiles/grass2.png");
    private static final BufferedImage TILE_GRASS3 = loadTile("res/tiles/grass3.png");
    private static final BufferedImage TILE_GRASS_BUSH = loadTile("res/tiles/grassbush.png");
    private static final BufferedImage TILE_WATER = loadTile("res/tiles/water.png");
    private final double[][] windStreaks;

    public World(
            String name,
            int width,
            int height,
            int tileSize,
            int spawnTileX,
            int spawnTileY,
            Npc[] npcs
    ) {
        this.name = name;
        this.spawnTileX = spawnTileX;
        this.spawnTileY = spawnTileY;
        this.tiles = new TileType[height][width];
        this.npcs = npcs;
        this.windStreaks = createWindStreaks(width, height, tileSize, name.hashCode());
        buildMap(tileSize);
    }

    private void buildMap(int tileSize) {
        Random random = new Random(name.hashCode());
        double[][] grassNoise = new double[tiles.length][tiles[0].length];
        boolean[][] landMask = new boolean[tiles.length][tiles[0].length];
        boolean isHometown = "Hometown".equalsIgnoreCase(name);
        int shapeVariant = Math.abs(name.hashCode()) % 4;
        int waterMargin = 5;
        double cx = (getWidth() - 1) / 2.0;
        double cy = (getHeight() - 1) / 2.0;
        double rx = (getWidth() - 1 - waterMargin * 2) / 2.0;
        double ry = (getHeight() - 1 - waterMargin * 2) / 2.0;

        for (int y = 0; y < tiles.length; y++) {
            for (int x = 0; x < tiles[0].length; x++) {
                tiles[y][x] = TileType.WATER;
                grassNoise[y][x] = random.nextDouble();
            }
        }

        for (int y = waterMargin; y < getHeight() - waterMargin; y++) {
            for (int x = waterMargin; x < getWidth() - waterMargin; x++) {
                double nx = (x - cx) / Math.max(1.0, rx);
                double ny = (y - cy) / Math.max(1.0, ry);
                boolean isLand = false;

                if (isHometown) {
                    double wave = 0.05 * Math.sin((x + y) * 0.25) + 0.04 * Math.cos(y * 0.35);
                    isLand = (nx * nx + ny * ny) <= (0.96 + wave);
                } else if (shapeVariant == 0) {
                    double wave = 0.08 * Math.sin((x + y) * 0.35) + 0.08 * Math.cos(y * 0.55);
                    isLand = (nx * nx + ny * ny) <= (0.86 + wave);
                } else if (shapeVariant == 1) {
                    double diamond = Math.abs(nx) + Math.abs(ny);
                    double wave = 0.1 * Math.sin(x * 0.45) - 0.08 * Math.cos(y * 0.33);
                    isLand = diamond <= (1.12 + wave);
                } else if (shapeVariant == 2) {
                    double left = Math.pow((x - (cx - rx * 0.28)) / (rx * 0.84), 2) + Math.pow((y - cy) / (ry * 0.9), 2);
                    double right = Math.pow((x - (cx + rx * 0.28)) / (rx * 0.84), 2) + Math.pow((y - cy) / (ry * 0.9), 2);
                    isLand = left <= 1.0 || right <= 1.0;
                } else {
                    double outer = nx * nx + ny * ny;
                    double inner = Math.pow((x - (cx + rx * 0.22)) / (rx * 0.72), 2) + Math.pow((y - cy) / (ry * 0.75), 2);
                    isLand = outer <= 1.0 && inner >= 0.86;
                }

                if (isLand) {
                    landMask[y][x] = true;
                    tiles[y][x] = TileType.GRASS1;
                }
            }
        }

        // Smooth random blending between grass1 and grass2.
        for (int pass = 0; pass < 3; pass++) {
            double[][] next = new double[tiles.length][tiles[0].length];
            for (int y = 1; y < tiles.length - 1; y++) {
                for (int x = 1; x < tiles[0].length - 1; x++) {
                    if (!landMask[y][x]) {
                        continue;
                    }
                    double sum = 0;
                    int count = 0;
                    for (int oy = -1; oy <= 1; oy++) {
                        for (int ox = -1; ox <= 1; ox++) {
                            if (landMask[y + oy][x + ox]) {
                                sum += grassNoise[y + oy][x + ox];
                                count++;
                            }
                        }
                    }
                    next[y][x] = count == 0 ? grassNoise[y][x] : sum / count;
                }
            }
            grassNoise = next;
        }

        for (int y = 1; y < tiles.length - 1; y++) {
            for (int x = 1; x < tiles[0].length - 1; x++) {
                if (landMask[y][x]) {
                    tiles[y][x] = grassNoise[y][x] > 0.5 ? TileType.GRASS2 : TileType.GRASS1;
                }
            }
        }

        // Sprinkle flower grass (grass3) as smooth, sparse patches for visual variation.
        double[][] flowerNoise = new double[tiles.length][tiles[0].length];
        for (int y = 1; y < tiles.length - 1; y++) {
            for (int x = 1; x < tiles[0].length - 1; x++) {
                if (landMask[y][x]) {
                    flowerNoise[y][x] = random.nextDouble();
                }
            }
        }
        for (int pass = 0; pass < 2; pass++) {
            double[][] next = new double[tiles.length][tiles[0].length];
            for (int y = 1; y < tiles.length - 1; y++) {
                for (int x = 1; x < tiles[0].length - 1; x++) {
                    if (!landMask[y][x]) {
                        continue;
                    }
                    double sum = 0;
                    int count = 0;
                    for (int oy = -1; oy <= 1; oy++) {
                        for (int ox = -1; ox <= 1; ox++) {
                            if (landMask[y + oy][x + ox]) {
                                sum += flowerNoise[y + oy][x + ox];
                                count++;
                            }
                        }
                    }
                    next[y][x] = count == 0 ? flowerNoise[y][x] : sum / count;
                }
            }
            flowerNoise = next;
        }
        double flowerThreshold = 0.63;
        for (int y = 1; y < tiles.length - 1; y++) {
            for (int x = 1; x < tiles[0].length - 1; x++) {
                if (!landMask[y][x]) {
                    continue;
                }
                if (isProtectedPoint(x, y, tileSize)) {
                    continue;
                }
                if ((tiles[y][x] == TileType.GRASS1 || tiles[y][x] == TileType.GRASS2) && flowerNoise[y][x] > flowerThreshold) {
                    tiles[y][x] = TileType.GRASS3;
                }
            }
        }

        // Cluster grassbush in random circular patches about 8x8.
        int area = getWidth() * getHeight();
        int clusterCount = Math.max(8, area / 1800);
        int radius = 4;
        for (int i = 0; i < clusterCount; i++) {
            int clusterX = 2 + random.nextInt(Math.max(1, getWidth() - 4));
            int clusterY = 2 + random.nextInt(Math.max(1, getHeight() - 4));
            for (int y = clusterY - radius; y <= clusterY + radius; y++) {
                for (int x = clusterX - radius; x <= clusterX + radius; x++) {
                    if (x <= 0 || y <= 0 || x >= getWidth() - 1 || y >= getHeight() - 1) {
                        continue;
                    }
                    int dx = x - clusterX;
                    int dy = y - clusterY;
                    if (dx * dx + dy * dy <= radius * radius) {
                        if (landMask[y][x] && !isProtectedPoint(x, y, tileSize) && random.nextDouble() > 0.18) {
                            tiles[y][x] = TileType.GRASS_BUSH;
                        }
                    }
                }
            }
        }

        clearAreaToLand(spawnTileX, spawnTileY, 3);
        for (Npc npc : npcs) {
            int npcTileX = (int) npc.getX() / tileSize;
            int npcTileY = (int) npc.getY() / tileSize;
            clearAreaToLand(npcTileX, npcTileY, 2);
        }
    }

    private void clearAreaToLand(int centerX, int centerY, int radius) {
        for (int y = centerY - radius; y <= centerY + radius; y++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                if (x <= 0 || y <= 0 || x >= getWidth() - 1 || y >= getHeight() - 1) {
                    continue;
                }
                int dx = x - centerX;
                int dy = y - centerY;
                if (dx * dx + dy * dy <= radius * radius) {
                    tiles[y][x] = ((x + y) % 2 == 0) ? TileType.GRASS1 : TileType.GRASS2;
                }
            }
        }
    }

    private boolean isProtectedPoint(int x, int y, int tileSize) {
        if (Math.abs(x - spawnTileX) <= 2 && Math.abs(y - spawnTileY) <= 2) {
            return true;
        }
        for (Npc npc : npcs) {
            int npcTileX = (int) npc.getX() / tileSize;
            int npcTileY = (int) npc.getY() / tileSize;
            if (Math.abs(x - npcTileX) <= 2 && Math.abs(y - npcTileY) <= 2) {
                return true;
            }
        }
        return false;
    }

    public void renderTiles(Graphics2D g2d, Camera camera, int tileSize, int viewportWidth, int viewportHeight, double windTimeSeconds) {
        int startTileX = Math.max(0, camera.getX() / tileSize);
        int startTileY = Math.max(0, camera.getY() / tileSize);
        int endTileX = Math.min(getWidth() - 1, (camera.getX() + viewportWidth) / tileSize + 1);
        int endTileY = Math.min(getHeight() - 1, (camera.getY() + viewportHeight) / tileSize + 1);

        for (int y = startTileY; y <= endTileY; y++) {
            for (int x = startTileX; x <= endTileX; x++) {
                TileType tileType = tiles[y][x];
                BufferedImage tileImage = getTileImage(tileType);
                int drawX = x * tileSize - camera.getX();
                int drawY = y * tileSize - camera.getY();
                if (tileType == TileType.GRASS_BUSH) {
                    double phase = windTimeSeconds * 4.0 + x * 0.55 + y * 0.35;
                    drawX += (int) Math.round(Math.sin(phase) * 1.0);
                }
                if (tileImage != null) {
                    g2d.drawImage(tileImage, drawX, drawY, tileSize, tileSize, null);
                } else {
                    g2d.setColor(tileType.getColor());
                    g2d.fillRect(drawX, drawY, tileSize, tileSize);
                }
            }
        }
    }

    public void update(double deltaSeconds) {
        for (Npc npc : npcs) {
            npc.update(deltaSeconds, this);
        }
    }

    public void renderWindLines(Graphics2D g2d, Camera camera, int viewportWidth, int viewportHeight, double windTimeSeconds) {
        int camX = camera.getX();
        int camY = camera.getY();
        g2d.setColor(new Color(255, 255, 255, 36));
        for (double[] s : windStreaks) {
            double worldX = s[0];
            double worldY = s[1];
            double baseLen = s[2];
            double speed = s[3];
            double phase = s[4];
            double amplitude = s[5];

            double maxWidthPx = s[6];
            double flowX = worldX + ((windTimeSeconds * speed) % maxWidthPx);
            while (flowX >= maxWidthPx) {
                flowX -= maxWidthPx;
            }

            int sx = (int) Math.round(flowX) - camX;
            int sy = (int) Math.round(worldY + Math.sin(windTimeSeconds * 2.4 + phase) * amplitude) - camY;
            int ex = sx + (int) Math.round(baseLen);
            int ey = sy + (int) Math.round(Math.sin(windTimeSeconds * 4.2 + phase) * 2.0);

            if (ex < 0 || sx > viewportWidth || sy < -6 || sy > viewportHeight + 6) {
                continue;
            }
            g2d.drawLine(sx, sy, ex, ey);
        }
    }

    public TileType getTile(int x, int y) {
        if (x < 0 || y < 0 || y >= tiles.length || x >= tiles[0].length) {
            return TileType.WATER;
        }
        return tiles[y][x];
    }

    public int getWidth() {
        return tiles[0].length;
    }

    public int getHeight() {
        return tiles.length;
    }

    public Npc[] getNpcs() {
        return npcs;
    }

    public Npc getClosestNpcInRange(double worldX, double worldY, double maxDistance) {
        Npc closest = null;
        double best = maxDistance;
        for (Npc npc : npcs) {
            double dx = npc.getCenterX() - worldX;
            double dy = npc.getCenterY() - worldY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist <= best) {
                best = dist;
                closest = npc;
            }
        }
        return closest;
    }

    public int getSpawnTileX() {
        return spawnTileX;
    }

    public int getSpawnTileY() {
        return spawnTileY;
    }

    public String getName() {
        return name;
    }

    private BufferedImage getTileImage(TileType type) {
        return switch (type) {
            case GRASS1 -> TILE_GRASS1;
            case GRASS2 -> TILE_GRASS2;
            case GRASS3 -> TILE_GRASS3;
            case GRASS_BUSH -> TILE_GRASS_BUSH;
            case WATER -> TILE_WATER;
        };
    }

    private static BufferedImage loadTile(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            return null;
        }
    }

    private static double[][] createWindStreaks(int widthTiles, int heightTiles, int tileSize, int seed) {
        int count = Math.max(30, (widthTiles * heightTiles) / 50);
        double[][] streaks = new double[count][7];
        Random random = new Random(seed * 31L + 7L);
        double worldWidth = widthTiles * tileSize;
        double worldHeight = heightTiles * tileSize;
        for (int i = 0; i < count; i++) {
            streaks[i][0] = random.nextDouble() * worldWidth;               // x
            streaks[i][1] = random.nextDouble() * worldHeight;              // y
            streaks[i][2] = 16 + random.nextDouble() * 24;                  // len
            streaks[i][3] = 22 + random.nextDouble() * 40;                  // speed px/s
            streaks[i][4] = random.nextDouble() * Math.PI * 2.0;            // phase
            streaks[i][5] = 1.0 + random.nextDouble() * 3.0;                // sway amplitude
            streaks[i][6] = worldWidth;                                      // wrap width
        }
        return streaks;
    }
}
