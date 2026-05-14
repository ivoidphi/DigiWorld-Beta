package digiworld.core;

import java.awt.Color;
import java.awt.Graphics2D;

public abstract class Entity {
    protected double x;
    protected double y;
    protected int size;
    protected Color color;
    protected boolean moving;
    private double bobTimer;
    private static final double BOB_SPEED = 12.0;

    protected Entity(double x, double y, int size, Color color) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = color;
        this.moving = false;
        this.bobTimer = 0.0;
    }

    public abstract void update(double deltaSeconds, World world);

    public void render(Graphics2D g2d, Camera camera) {
        int bobOffset = getBobOffsetY();
        renderShadow(g2d, camera);
        g2d.setColor(color);
        g2d.fillRect((int) x - camera.getX(), (int) y - camera.getY() + bobOffset, size, size);
    }

    protected void updateBobbing(double deltaSeconds) {
        if (moving) {
            bobTimer += deltaSeconds * BOB_SPEED;
        }
    }

    protected int getBobOffsetY() {
        if (!moving) {
            return 0;
        }
        return Math.sin(bobTimer) > 0 ? 1 : 0;
    }

    protected void renderShadow(Graphics2D g2d, Camera camera) {
        int drawX = (int) x - camera.getX();
        int drawY = (int) y - camera.getY();
        int shadowW = Math.max(8, size / 2);
        int shadowH = Math.max(4, size / 6);
        int shadowX = drawX + (size - shadowW) / 2;
        int shadowY = drawY + size - shadowH / 2;
        g2d.setColor(new Color(0, 0, 0, 90));
        g2d.fillOval(shadowX, shadowY, shadowW, shadowH);
    }

    public int getCenterX() {
        return (int) x + size / 2;
    }

    public int getCenterY() {
        return (int) y + size / 2;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getSize() {
        return size;
    }

    public int getVisualBobOffsetY() {
        return getBobOffsetY();
    }

    public boolean isMoving() {
        return moving;
    }
}
