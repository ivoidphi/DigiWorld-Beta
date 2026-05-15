package digiworld.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Structure {

    private final int x, y;
    private final int width, height;
    private final int wallOffsetY;
    private final int leftPad;
    private final int rightPad;
    private final int roofHeight;

    private final BufferedImage baseImage;
    private final BufferedImage roofImage;

    public Structure(int x, int y, int width, int height, int wallOffsetY,
                     int leftPad, int rightPad,
                     String basePath, String roofPath, int roofHeight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.wallOffsetY = wallOffsetY;
        this.leftPad = leftPad;
        this.rightPad = rightPad;
        this.roofHeight = roofHeight;
        this.baseImage = load(basePath);
        this.roofImage = roofPath != null ? load(roofPath) : null;
    }

    private static BufferedImage load(String path) {
        if (path == null || path.isEmpty()) return null;
        try { return ImageIO.read(new File(path)); }
        catch (IOException e) { return null; }
    }

    public void drawBase(Graphics2D g2, Camera camera) {
        if (baseImage != null)
            g2.drawImage(baseImage, x - camera.getX(), y - camera.getY(), width, height, null);
    }

    public void drawRoof(Graphics2D g2, Camera camera) {
        if (roofImage != null)
            g2.drawImage(roofImage, x - camera.getX(), y - roofHeight - camera.getY(), width, roofHeight, null);
    }

    public int wallStartY() { return y + wallOffsetY; }

    public Rectangle getCollisionRect() {
        int collisionWidth = width - leftPad - rightPad;
        int collisionHeight = height - wallOffsetY;
        if (collisionWidth <= 0 || collisionHeight <= 0) {
            return new Rectangle(Integer.MIN_VALUE / 4, Integer.MIN_VALUE / 4, 0, 0);
        }
        return new Rectangle(x + leftPad, y + wallOffsetY, collisionWidth, collisionHeight);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
