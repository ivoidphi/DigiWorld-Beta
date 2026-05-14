package digiworld.core;

import digiworld.app.InputHandler;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Player extends Entity {

    private enum Facing { FORWARD, BACK, LEFT, RIGHT }

    private final InputHandler input;
    private final int tileSize;
    private final double speedPixelsPerSecond;
    private Facing facing;

    private final BufferedImage spriteForward;
    private final BufferedImage spriteBack;
    private final BufferedImage spriteLeft;
    private final BufferedImage spriteRight;

    // Reference to DoorManager — set by GamePanel after construction
    private DoorManager doorManager;

    public Player(double x, double y, int size, InputHandler input, int tileSize) {
        super(x, y, size, new Color(255, 209, 102));
        this.input  = input;
        this.tileSize = tileSize;
        this.speedPixelsPerSecond = 84.0;
        this.facing = Facing.FORWARD;
        this.spriteForward = loadSprite("res/characters/player/player-fw.png");
        this.spriteBack    = loadSprite("res/characters/player/player-b.png");
        this.spriteLeft    = loadSprite("res/characters/player/player-l.png");
        this.spriteRight   = loadSprite("res/characters/player/player-r.png");
    }

    public void setDoorManager(DoorManager dm) {
        this.doorManager = dm;
    }

    @Override
    public void update(double deltaSeconds, World world) {
        double moveX = 0, moveY = 0;

        if (input.isPressed(java.awt.event.KeyEvent.VK_W) || input.isPressed(java.awt.event.KeyEvent.VK_UP))    moveY -= 1;
        if (input.isPressed(java.awt.event.KeyEvent.VK_S) || input.isPressed(java.awt.event.KeyEvent.VK_DOWN))  moveY += 1;
        if (input.isPressed(java.awt.event.KeyEvent.VK_A) || input.isPressed(java.awt.event.KeyEvent.VK_LEFT))  moveX -= 1;
        if (input.isPressed(java.awt.event.KeyEvent.VK_D) || input.isPressed(java.awt.event.KeyEvent.VK_RIGHT)) moveX += 1;

        if (moveX != 0 && moveY != 0) {
            double inv = 1.0 / Math.sqrt(2);
            moveX *= inv; moveY *= inv;
        }

        moving = moveX != 0 || moveY != 0;
        updateBobbing(deltaSeconds);
        updateFacing(moveX, moveY);

        double nextX = x + moveX * speedPixelsPerSecond * deltaSeconds;
        double nextY = y + moveY * speedPixelsPerSecond * deltaSeconds;

        // Check door BEFORE collision so structure walls don't block entry
        if (moving && doorManager != null) {
            Rectangle nextRect = getRect(nextX, nextY);
            if (doorManager.checkAt(nextRect)) return;
        }

        if (canMoveTo(nextX, y, world)) x = nextX;
        if (canMoveTo(x, nextY, world)) y = nextY;
    }

    private boolean canMoveTo(double checkX, double checkY, World world) {
        int leftTile   = (int) checkX / tileSize;
        int topTile    = (int) checkY / tileSize;
        int rightTile  = ((int) checkX + size - 1) / tileSize;
        int bottomTile = ((int) checkY + size - 1) / tileSize;

        // Tile collision
        if (world.getTile(leftTile,  topTile).isBlocked()    ||
            world.getTile(rightTile, topTile).isBlocked()    ||
            world.getTile(leftTile,  bottomTile).isBlocked() ||
            world.getTile(rightTile, bottomTile).isBlocked()) return false;

        double inset = 2.0;
        double pLeft = checkX + inset, pTop = checkY + inset;
        double pRight = checkX + size - inset, pBottom = checkY + size - inset;
        double cLeft = x + inset, cTop = y + inset;
        double cRight = x + size - inset, cBottom = y + size - inset;

        // NPC collision (unchanged from original)
        for (Npc npc : world.getNpcs()) {
            double nLeft = npc.getX() + inset, nTop = npc.getY() + inset;
            double nRight = npc.getX() + npc.getSize() - inset;
            double nBottom = npc.getY() + npc.getSize() - inset;
            boolean overlap = pLeft < nRight && pRight > nLeft && pTop < nBottom && pBottom > nTop;
            if (overlap) {
                boolean curOverlap = cLeft < nRight && cRight > nLeft && cTop < nBottom && cBottom > nTop;
                if (curOverlap) {
                    double next = overlapArea(pLeft, pTop, pRight, pBottom, nLeft, nTop, nRight, nBottom);
                    double cur  = overlapArea(cLeft, cTop, cRight, cBottom, nLeft, nTop, nRight, nBottom);
                    if (next + 0.001 < cur) continue;
                }
                return false;
            }
        }

        // Structure collision — pixel-based rect, same style as NPC
        for (Structure s : world.getStructures()) {
            Rectangle sr = s.getCollisionRect();
            double sLeft = sr.x, sTop = sr.y;
            double sRight = sr.x + sr.width, sBottom = sr.y + sr.height;
            boolean overlap = pLeft < sRight && pRight > sLeft && pTop < sBottom && pBottom > sTop;
            if (overlap) {
                boolean curOverlap = cLeft < sRight && cRight > sLeft && cTop < sBottom && cBottom > sTop;
                if (curOverlap) {
                    double next = overlapArea(pLeft, pTop, pRight, pBottom, sLeft, sTop, sRight, sBottom);
                    double cur  = overlapArea(cLeft, cTop, cRight, cBottom, sLeft, sTop, sRight, sBottom);
                    if (next + 0.001 < cur) continue;
                }
                return false;
            }
        }

        return true;
    }

    /** Player rect at a given position (used for door lookahead). */
    private Rectangle getRect(double px, double py) {
        int inset = 2;
        return new Rectangle((int) px + inset, (int) py + inset, size - inset * 2, size - inset * 2);
    }

    private double overlapArea(double aL, double aT, double aR, double aB,
                               double bL, double bT, double bR, double bB) {
        return Math.max(0, Math.min(aR, bR) - Math.max(aL, bL))
             * Math.max(0, Math.min(aB, bB) - Math.max(aT, bT));
    }

    public void teleportToTile(int tileX, int tileY) {
        this.x = tileX * tileSize;
        this.y = tileY * tileSize;
    }

    @Override
    public void render(Graphics2D g2, Camera camera) {
        BufferedImage sprite = getActiveSprite();
        int drawX = (int) x - camera.getX();
        int drawY = (int) y - camera.getY() + getBobOffsetY();
        renderShadow(g2, camera);
        if (sprite != null) g2.drawImage(sprite, drawX, drawY, size, size, null);
        else super.render(g2, camera);
    }

    private void updateFacing(double moveX, double moveY) {
        if (moveX == 0 && moveY == 0) return;
        if (moveX != 0) facing = moveX > 0 ? Facing.RIGHT : Facing.LEFT;
        else            facing = moveY > 0 ? Facing.FORWARD : Facing.BACK;
    }

    private BufferedImage getActiveSprite() {
        return switch (facing) {
            case FORWARD -> spriteForward;
            case BACK    -> spriteBack;
            case LEFT    -> spriteLeft;
            case RIGHT   -> spriteRight;
        };
    }

    private static BufferedImage loadSprite(String path) {
        try { return ImageIO.read(new File(path)); }
        catch (IOException e) { return null; }
    }
}
