package digiworld.core;

/**
 * Defines a door: which world it's on, the pixel region of the door,
 * which world it leads to, and where the player spawns on entry.
 */
public class DoorEntry {

    public final int sourceWorld;  // world index this door is in
    public final int doorPixelX;   // left edge of door region in world pixels
    public final int doorPixelY;   // top edge of door region in world pixels
    public final int doorWidth;    // width of detection region
    public final int doorHeight;   // height of detection region
    public final int destWorld;    // world index to travel to
    public final int spawnTileX;   // player spawn tile X in dest world
    public final int spawnTileY;   // player spawn tile Y in dest world

    /**
     * Tile-based constructor — converts tile coords to pixels automatically.
     */
    public DoorEntry(int sourceWorld, int doorTileX, int doorTileY, int tileSize,
                     int destWorld, int spawnTileX, int spawnTileY) {
        this.sourceWorld = sourceWorld;
        this.doorPixelX  = doorTileX * tileSize;
        this.doorPixelY  = doorTileY * tileSize;
        this.doorWidth   = tileSize;
        this.doorHeight  = tileSize;
        this.destWorld   = destWorld;
        this.spawnTileX  = spawnTileX;
        this.spawnTileY  = spawnTileY;
    }

    /**
     * Pixel-based constructor — full control over door region size.
     */
    public DoorEntry(int sourceWorld, int doorPixelX, int doorPixelY, int doorWidth, int doorHeight,
                     int destWorld, int spawnTileX, int spawnTileY) {
        this.sourceWorld = sourceWorld;
        this.doorPixelX  = doorPixelX;
        this.doorPixelY  = doorPixelY;
        this.doorWidth   = doorWidth;
        this.doorHeight  = doorHeight;
        this.destWorld   = destWorld;
        this.spawnTileX  = spawnTileX;
        this.spawnTileY  = spawnTileY;
    }

    public java.awt.Rectangle getRect() {
        return new java.awt.Rectangle(doorPixelX, doorPixelY, doorWidth, doorHeight);
    }
}
