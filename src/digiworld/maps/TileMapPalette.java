package digiworld.maps;

import digiworld.core.TileType;

public final class TileMapPalette {
    public static final int WATER = 0;
    public static final int GRASS2 = 1;
    public static final int GRASS1 = 2;
    public static final int GRASS_BUSH = 3;
    public static final int GRASS3 = 4;

    private TileMapPalette() {
    }

    public static TileType toTile(int code) {
        return switch (code) {
            case WATER -> TileType.WATER;
            case GRASS1 -> TileType.GRASS1;
            case GRASS_BUSH -> TileType.GRASS_BUSH;
            case GRASS3 -> TileType.GRASS3;
            default -> TileType.GRASS2;
        };
    }
}

