package digiworld.core;

public class DoorEntry {

    public final int sourceWorld;
    public final int doorPixelX;
    public final int doorPixelY;
    public final int doorWidth;
    public final int doorHeight;
    public final int destWorld;
    public final int spawnTileX;
    public final int spawnTileY;

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
