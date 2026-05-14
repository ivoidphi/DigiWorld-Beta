package digiworld.core;

public class Camera {
    private int x;
    private int y;

    public void follow(Entity target, World world, int viewportWidth, int viewportHeight, int tileSize) {
        followPoint(target.getCenterX(), target.getCenterY(), world, viewportWidth, viewportHeight, tileSize);
    }

    public void followPoint(int centerX, int centerY, World world, int viewportWidth, int viewportHeight, int tileSize) {
        int worldPixelWidth = world.getWidth() * tileSize;
        int worldPixelHeight = world.getHeight() * tileSize;

        int desiredX = centerX - viewportWidth / 2;
        int desiredY = centerY - viewportHeight / 2;

        x = clamp(desiredX, 0, Math.max(0, worldPixelWidth - viewportWidth));
        y = clamp(desiredY, 0, Math.max(0, worldPixelHeight - viewportHeight));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
