import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Player extends Entity {
    private enum Facing {
        FORWARD,
        BACK,
        LEFT,
        RIGHT
    }

    private final InputHandler input;
    private final int tileSize;
    private final double speedPixelsPerSecond;
    private Facing facing;
    private final BufferedImage spriteForward;
    private final BufferedImage spriteBack;
    private final BufferedImage spriteLeft;
    private final BufferedImage spriteRight;

    public Player(double x, double y, int size, InputHandler input, int tileSize) {
        super(x, y, size, new Color(255, 209, 102));
        this.input = input;
        this.tileSize = tileSize;
        this.speedPixelsPerSecond = 84.0;
        this.facing = Facing.FORWARD;
        this.spriteForward = loadSprite("res/characters/player/player-fw.png");
        this.spriteBack = loadSprite("res/characters/player/player-b.png");
        this.spriteLeft = loadSprite("res/characters/player/player-l.png");
        this.spriteRight = loadSprite("res/characters/player/player-r.png");
    }

    @Override
    public void update(double deltaSeconds, World world) {
        double moveX = 0;
        double moveY = 0;

        if (input.isPressed(java.awt.event.KeyEvent.VK_W) || input.isPressed(java.awt.event.KeyEvent.VK_UP)) {
            moveY -= 1;
        }
        if (input.isPressed(java.awt.event.KeyEvent.VK_S) || input.isPressed(java.awt.event.KeyEvent.VK_DOWN)) {
            moveY += 1;
        }
        if (input.isPressed(java.awt.event.KeyEvent.VK_A) || input.isPressed(java.awt.event.KeyEvent.VK_LEFT)) {
            moveX -= 1;
        }
        if (input.isPressed(java.awt.event.KeyEvent.VK_D) || input.isPressed(java.awt.event.KeyEvent.VK_RIGHT)) {
            moveX += 1;
        }

        if (moveX != 0 && moveY != 0) {
            double invLength = 1.0 / Math.sqrt(2);
            moveX *= invLength;
            moveY *= invLength;
        }
        moving = moveX != 0 || moveY != 0;
        updateBobbing(deltaSeconds);
        updateFacing(moveX, moveY);

        double nextX = x + moveX * speedPixelsPerSecond * deltaSeconds;
        double nextY = y + moveY * speedPixelsPerSecond * deltaSeconds;

        if (canMoveTo(nextX, y, world)) {
            x = nextX;
        }
        if (canMoveTo(x, nextY, world)) {
            y = nextY;
        }
    }

    private boolean canMoveTo(double checkX, double checkY, World world) {
        int leftTile = (int) checkX / tileSize;
        int topTile = (int) checkY / tileSize;
        int rightTile = ((int) checkX + size - 1) / tileSize;
        int bottomTile = ((int) checkY + size - 1) / tileSize;

        boolean tileWalkable = !world.getTile(leftTile, topTile).isBlocked()
                && !world.getTile(rightTile, topTile).isBlocked()
                && !world.getTile(leftTile, bottomTile).isBlocked()
                && !world.getTile(rightTile, bottomTile).isBlocked();
        if (!tileWalkable) {
            return false;
        }

        double inset = 2.0;
        double pLeft = checkX + inset;
        double pTop = checkY + inset;
        double pRight = checkX + size - inset;
        double pBottom = checkY + size - inset;
        double currentLeft = x + inset;
        double currentTop = y + inset;
        double currentRight = x + size - inset;
        double currentBottom = y + size - inset;
        for (Npc npc : world.getNpcs()) {
            double nLeft = npc.getX() + inset;
            double nTop = npc.getY() + inset;
            double nRight = npc.getX() + npc.getSize() - inset;
            double nBottom = npc.getY() + npc.getSize() - inset;
            boolean overlap = pLeft < nRight && pRight > nLeft && pTop < nBottom && pBottom > nTop;
            if (overlap) {
                double nextOverlapArea = overlapArea(pLeft, pTop, pRight, pBottom, nLeft, nTop, nRight, nBottom);
                boolean currentlyOverlapping = currentLeft < nRight && currentRight > nLeft && currentTop < nBottom && currentBottom > nTop;
                if (currentlyOverlapping) {
                    double currentOverlapArea = overlapArea(currentLeft, currentTop, currentRight, currentBottom, nLeft, nTop, nRight, nBottom);
                    if (nextOverlapArea + 0.001 < currentOverlapArea) {
                        continue;
                    }
                }
                return false;
            }
        }
        return true;
    }

    private double overlapArea(double aLeft, double aTop, double aRight, double aBottom,
                               double bLeft, double bTop, double bRight, double bBottom) {
        double overlapW = Math.max(0.0, Math.min(aRight, bRight) - Math.max(aLeft, bLeft));
        double overlapH = Math.max(0.0, Math.min(aBottom, bBottom) - Math.max(aTop, bTop));
        return overlapW * overlapH;
    }

    public void teleportToTile(int tileX, int tileY) {
        this.x = tileX * tileSize;
        this.y = tileY * tileSize;
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

    private void updateFacing(double moveX, double moveY) {
        if (moveX == 0 && moveY == 0) {
            return;
        }
        if (moveX != 0) {
            facing = moveX > 0 ? Facing.RIGHT : Facing.LEFT;
        } else {
            facing = moveY > 0 ? Facing.FORWARD : Facing.BACK;
        }
    }

    private BufferedImage getActiveSprite() {
        return switch (facing) {
            case FORWARD -> spriteForward;
            case BACK -> spriteBack;
            case LEFT -> spriteLeft;
            case RIGHT -> spriteRight;
        };
    }

    private static BufferedImage loadSprite(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            return null;
        }
    }
}
