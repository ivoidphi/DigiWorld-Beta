public class Camera {
    private int x;
    private int y;

    public void follow(Entity target, World world, int viewportWidth, int viewportHeight, int tileSize) {
        int worldPixelWidth = world.getWidth() * tileSize;
        int worldPixelHeight = world.getHeight() * tileSize;

        int desiredX = target.getCenterX() - viewportWidth / 2;
        int desiredY = target.getCenterY() - viewportHeight / 2;

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
