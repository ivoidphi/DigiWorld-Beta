package digiworld.maps;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class TileSet {
    private final String name;
    private final BufferedImage image;
    private final int tileWidth;
    private final int tileHeight;
    private final int columns;
    private final int rows;

    public TileSet(String name, String imagePath, int tileWidth, int tileHeight) {
        this.name = name;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.image = loadImage(imagePath);
        if (image != null) {
            this.columns = image.getWidth() / tileWidth;
            this.rows = image.getHeight() / tileHeight;
        } else {
            this.columns = 0;
            this.rows = 0;
        }
    }

    private BufferedImage loadImage(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                return ImageIO.read(file);
            }
        } catch (IOException e) {
            System.err.println("[TileSet] Failed to load: " + path);
        }
        return null;
    }

    public String getName() { return name; }
    public BufferedImage getImage() { return image; }
    public int getTileWidth() { return tileWidth; }
    public int getTileHeight() { return tileHeight; }
    public int getColumns() { return columns; }
    public int getRows() { return rows; }

    public BufferedImage getTile(int tileId) {
        if (image == null || tileId < 0) return null;

        int col = tileId % columns;
        int row = tileId / columns;

        if (col < 0 || row < 0 || col >= columns || row >= rows) {
            return null;
        }

        return image.getSubimage(
            col * tileWidth,
            row * tileHeight,
            tileWidth,
            tileHeight
        );
    }

    public static TileSet fromTilesetName(String name) {
        return switch (name.toLowerCase()) {
            case "ground" -> new TileSet("ground", "res/tilesets/ground.png", 32, 32);
            case "water" -> new TileSet("water", "res/tilesets/water.png", 32, 32);
            default -> null;
        };
    }
}