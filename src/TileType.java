import java.awt.Color;

public enum TileType {
    GRASS1(new Color(96, 176, 78), false),
    GRASS2(new Color(84, 160, 67), false),
    GRASS3(new Color(105, 184, 88), false),
    GRASS_BUSH(new Color(70, 140, 54), false),
    WATER(new Color(45, 118, 204), true);

    private final Color color;
    private final boolean blocked;

    TileType(Color color, boolean blocked) {
        this.color = color;
        this.blocked = blocked;
    }

    public Color getColor() {
        return color;
    }

    public boolean isBlocked() {
        return blocked;
    }
}
