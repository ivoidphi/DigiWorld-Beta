package digiworld.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * A placed structure in a world (house, lab, etc.).
 * Rendered in two passes: base before player, roof after player.
 * Collision uses pixel-based rect matching Player.canMoveTo() style.
 */
public class Structure {

    private final int x, y;           // top-left pixel position
    private final int width, height;  // rendered size of base image
    private final int wallOffsetY;    // px from top of base where solid wall starts
    private final int leftPad;        // horizontal collision inset (left)
    private final int rightPad;       // horizontal collision inset (right)
    private final int roofHeight;     // rendered height of roof tip image

    private final BufferedImage baseImage;
    private final BufferedImage roofImage; // nullable

    /**
     * @param x           top-left pixel X
     * @param y           top-left pixel Y
     * @param width       rendered width in pixels
     * @param height      rendered height of base image
     * @param wallOffsetY px from top of base where wall/collision begins
     * @param leftPad     left collision inset
     * @param rightPad    right collision inset
     * @param basePath    path to base image
     * @param roofPath    path to roof tip image (null if none)
     * @param roofHeight  rendered height of roof image
     */
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
        catch (IOException e) { System.out.println("Could not load structure image: " + path); return null; }
    }

    /** Draw the base (walls, door, windows). Call BEFORE player. */
    public void drawBase(Graphics2D g2, Camera camera) {
        if (baseImage != null)
            g2.drawImage(baseImage, x - camera.getX(), y - camera.getY(), width, height, null);
    }

    /** Draw roof tip overlay. Call AFTER player so it renders on top. */
    public void drawRoof(Graphics2D g2, Camera camera) {
        if (roofImage != null)
            g2.drawImage(roofImage, x - camera.getX(), y - roofHeight - camera.getY(), width, roofHeight, null);
    }

    /**
     * Y position in world pixels where the solid wall begins.
     * Used for depth sorting: player above this → roof overlays them.
     */
    public int wallStartY() { return y + wallOffsetY; }

    /**
     * Solid collision rect in world pixels.
     * Matches the pixel-overlap style used in Player.canMoveTo().
     */
    public Rectangle getCollisionRect() {
        return new Rectangle(x + leftPad, y + wallOffsetY, width - leftPad - rightPad, height - wallOffsetY);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
