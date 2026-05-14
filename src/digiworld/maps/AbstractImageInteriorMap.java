package digiworld.maps;

import digiworld.core.TileType;
import digiworld.core.World;

public abstract class AbstractImageInteriorMap {
    protected static final char BLOCKED = '#';
    protected static final char WALKABLE = '.';

    private final String worldName;
    private final String imagePath;

    protected AbstractImageInteriorMap(String worldName, String imagePath) {
        this.worldName = worldName;
        this.imagePath = imagePath;
    }

    public final boolean appliesTo(World world) {
        return world != null && worldName.equalsIgnoreCase(world.getName());
    }

    public final void applyTo(World world) {
        if (!appliesTo(world)) {
            return;
        }

        String[] blockedMask = blockedMask();
        validateDimensions(blockedMask);
        int offsetX = centeredOffset(world.getWidth(), blockedMask[0].length());
        int offsetY = centeredOffset(world.getHeight(), blockedMask.length);

        world.setBackgroundImage(imagePath, offsetX * 32, offsetY * 32);
        String fillImagePath = fillImagePath();
        if (fillImagePath != null && !fillImagePath.isBlank()) {
            world.setBackgroundFillImage(fillImagePath);
        }
        world.clearBlockedTiles();

        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                world.setTile(x, y, floorTile());
                world.setTileBlocked(x, y, true);
            }
        }

        for (int y = 0; y < blockedMask.length; y++) {
            for (int x = 0; x < blockedMask[y].length(); x++) {
                world.setTileBlocked(offsetX + x, offsetY + y, blockedMask[y].charAt(x) == BLOCKED);
            }
        }
    }

    protected TileType floorTile() {
        return TileType.GRASS2;
    }

    protected String fillImagePath() {
        return null;
    }

    protected abstract String[] blockedMask();

    private void validateDimensions(String[] blockedMask) {
        if (blockedMask.length == 0) {
            throw new IllegalArgumentException("Interior mask cannot be empty");
        }
        int width = blockedMask[0].length();
        for (String row : blockedMask) {
            if (row.length() != width) {
                throw new IllegalArgumentException("Interior mask rows must have matching widths");
            }
        }
    }

    private int centeredOffset(int outerSize, int innerSize) {
        if (innerSize > outerSize) {
            throw new IllegalArgumentException("Interior does not fit in world");
        }
        return (outerSize - innerSize) / 2;
    }
}
