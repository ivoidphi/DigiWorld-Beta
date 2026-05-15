package digiworld.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Npc extends Entity {
    private enum Facing {
        FORWARD,
        BACK,
        LEFT,
        RIGHT
    }

    private final String name;
    private final int tileSize;
    private final int[][] patrolPoints;
    private int currentTargetIndex;
    private final double speedPixelsPerSecond;
    private final double stopDurationSeconds;
    private double stopTimerSeconds;
    private Facing facing;
    private final BufferedImage spriteForward;
    private final BufferedImage spriteBack;
    private final BufferedImage spriteLeft;
    private final BufferedImage spriteRight;
    private boolean interactionLocked;
    private boolean grayscale;
    private BufferedImage grayForward;
    private BufferedImage grayBack;
    private BufferedImage grayLeft;
    private BufferedImage grayRight;

    public Npc(String name, int size, int tileSize, Color color, int[][] patrolPoints) {
        this(name, size, tileSize, color, patrolPoints, null, null, null, null);
    }

    public Npc(
            String name,
            int size,
            int tileSize,
            Color color,
            int[][] patrolPoints,
            String forwardPath,
            String backPath,
            String leftPath,
            String rightPath
    ) {
        super(patrolPoints[0][0] * tileSize, patrolPoints[0][1] * tileSize, size, color);
        this.name = name;
        this.tileSize = tileSize;
        this.patrolPoints = patrolPoints;
        this.currentTargetIndex = 1 % patrolPoints.length;
        this.speedPixelsPerSecond = 42.0;
        this.stopDurationSeconds = 0.8;
        this.stopTimerSeconds = 0.0;
        this.facing = Facing.FORWARD;
        this.spriteForward = loadSprite(forwardPath);
        this.spriteBack = loadSprite(backPath);
        this.spriteLeft = loadSprite(leftPath);
        this.spriteRight = loadSprite(rightPath);
        this.interactionLocked = false;
    }

    @Override
    public void update(double deltaSeconds, World world) {
        update(deltaSeconds, world, null);
    }

    public void update(double deltaSeconds, World world, Player player) {
        if (interactionLocked) {
            moving = false;
            updateBobbing(deltaSeconds);
            return;
        }

        if (stopTimerSeconds > 0) {
            stopTimerSeconds -= deltaSeconds;
            moving = false;
            updateBobbing(deltaSeconds);
            return;
        }

        int[] target = patrolPoints[currentTargetIndex];
        double targetX = target[0] * tileSize;
        double targetY = target[1] * tileSize;

        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 1.0) {
            x = targetX;
            y = targetY;
            currentTargetIndex = (currentTargetIndex + 1) % patrolPoints.length;
            stopTimerSeconds = stopDurationSeconds;
            moving = false;
            updateBobbing(deltaSeconds);
            return;
        }

        double step = speedPixelsPerSecond * deltaSeconds;
        double nx = x + (dx / distance) * Math.min(step, distance);
        double ny = y + (dy / distance) * Math.min(step, distance);
        moving = true;
        updateBobbing(deltaSeconds);
        updateFacing(dx, dy);
        if (!overlapsPlayer(nx, ny, player)) {
            x = nx;
            y = ny;
        } else {
            moving = false;
            stopTimerSeconds = 0.2;
        }
    }

    private boolean overlapsPlayer(double checkX, double checkY, Player player) {
        if (player == null) {
            return false;
        }
        double inset = 2.0;
        double nLeft = checkX + inset;
        double nTop = checkY + inset;
        double nRight = checkX + size - inset;
        double nBottom = checkY + size - inset;

        double pLeft = player.getX() + inset;
        double pTop = player.getY() + inset;
        double pRight = player.getX() + player.getSize() - inset;
        double pBottom = player.getY() + player.getSize() - inset;
        return nLeft < pRight && nRight > pLeft && nTop < pBottom && nBottom > pTop;
    }

    public String getName() {
        return name;
    }

    public void beginInteractionFacing(double playerCenterX, double playerCenterY) {
        interactionLocked = true;
        stopTimerSeconds = 0.0;
        moving = false;
        updateFacing(playerCenterX - getCenterX(), playerCenterY - getCenterY());
    }

    public void endInteraction() {
        interactionLocked = false;
    }

    public void setGrayscale(boolean grayscale) {
        this.grayscale = grayscale;
        if (grayscale) {
            if (grayForward == null && spriteForward != null) grayForward = toGrayscale(spriteForward);
            if (grayBack == null && spriteBack != null) grayBack = toGrayscale(spriteBack);
            if (grayLeft == null && spriteLeft != null) grayLeft = toGrayscale(spriteLeft);
            if (grayRight == null && spriteRight != null) grayRight = toGrayscale(spriteRight);
        }
    }

    @Override
    public void render(Graphics2D g2d, Camera camera) {
        BufferedImage activeSprite = getActiveSprite();
        int drawX = (int) x - camera.getX();
        int drawY = (int) y - camera.getY() + getBobOffsetY();
        renderShadow(g2d, camera);
        if (activeSprite != null) {
            g2d.drawImage(activeSprite, drawX, drawY, size, size, null);
            return;
        }
        super.render(g2d, camera);
    }

    private void updateFacing(double dx, double dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            facing = dx > 0 ? Facing.RIGHT : Facing.LEFT;
        } else {
            facing = dy > 0 ? Facing.FORWARD : Facing.BACK;
        }
    }

    private BufferedImage getActiveSprite() {
        if (grayscale) {
            return switch (facing) {
                case FORWARD -> grayForward;
                case BACK -> grayBack;
                case LEFT -> grayLeft;
                case RIGHT -> grayRight;
            };
        }
        return switch (facing) {
            case FORWARD -> spriteForward;
            case BACK -> spriteBack;
            case LEFT -> spriteLeft;
            case RIGHT -> spriteRight;
        };
    }

    private static BufferedImage loadSprite(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            return null;
        }
    }

    private static BufferedImage toGrayscale(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[width * height];
        source.getRGB(0, 0, width, height, pixels, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int a = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            int grayLevel = (r + g + b) / 3;
            pixels[i] = (a << 24) | (grayLevel << 16) | (grayLevel << 8) | grayLevel;
        }
        gray.setRGB(0, 0, width, height, pixels, 0, width);
        return gray;
    }
}
