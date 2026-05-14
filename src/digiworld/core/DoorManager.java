package digiworld.core;

import digiworld.app.GamePanel;
import digiworld.maps.HometownTileMap;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages all door entries across all worlds.
 * Uses pixel-based player rect intersection — fires before structure
 * collision blocks movement, same pattern as before.
 *
 * To add a new house door:
 *   doors.add(new DoorEntry(WorldIndex.ALPHA_VILLAGE, doorTileX, doorTileY, TILE_SIZE,
 *                           WorldIndex.HOUSE_1, spawnTileX, spawnTileY));
 */
public class DoorManager {

    private final GamePanel gp;
    private final int tileSize;
    private final List<DoorEntry> doors = new ArrayList<>();

    public DoorManager(GamePanel gp, int tileSize) {
        this.gp = gp;
        this.tileSize = tileSize;
        registerDoors();
    }

    private void registerDoors() {
        // Center house in Hometown.
        doors.add(new DoorEntry(
                WorldIndex.HOMETOWN,
                HometownTileMap.centerHouseDoorTileX(),
                HometownTileMap.centerHouseDoorTileY(),
                tileSize,
                WorldIndex.HOUSE_1, 22, 22
        ));

        doors.add(new DoorEntry(
                WorldIndex.HOUSE_1, 22, 24, tileSize,
                WorldIndex.HOMETOWN,
                HometownTileMap.centerHouseDoorTileX(),
                HometownTileMap.centerHouseDoorTileY() + 1
        ));
        doors.add(new DoorEntry(
                WorldIndex.HOUSE_1, 23, 24, tileSize,
                WorldIndex.HOMETOWN,
                HometownTileMap.centerHouseDoorTileX(),
                HometownTileMap.centerHouseDoorTileY() + 1
        ));

        doors.add(new DoorEntry(
                WorldIndex.HOMETOWN,
                HometownTileMap.laboratoryDoorTileX(),
                HometownTileMap.laboratoryDoorTileY(),
                tileSize,
                WorldIndex.LABORATORY, 22, 22
        ));
        doors.add(new DoorEntry(
                WorldIndex.HOMETOWN,
                HometownTileMap.laboratoryDoorTileX() + 1,
                HometownTileMap.laboratoryDoorTileY(),
                tileSize,
                WorldIndex.LABORATORY, 22, 22
        ));
        doors.add(new DoorEntry(
                WorldIndex.LABORATORY, 22, 24, tileSize,
                WorldIndex.HOMETOWN,
                HometownTileMap.laboratoryDoorTileX(),
                HometownTileMap.laboratoryDoorTileY() + 1
        ));


        // Add more doors here as you build them:
        // doors.add(new DoorEntry(WorldIndex.BETA_CITY, doorTileX, doorTileY, tileSize,
        //                         WorldIndex.HOUSE_2, spawnTileX, spawnTileY));
    }

    /**
     * Called from GamePanel update every frame (exploration state only).
     * Checks the player's current pixel rect against all door regions.
     */
    public void check() {
        int currentWorld = gp.getWorldIndex();
        Rectangle playerRect = getPlayerRect();

        for (DoorEntry door : doors) {
            if (door.sourceWorld != currentWorld) continue;
            if (playerRect.intersects(door.getRect())) {
                gp.teleportWithFade(door.destWorld, door.spawnTileX, door.spawnTileY);
                return;
            }
        }
    }

    /**
     * Called from Player.canMoveTo() with the next-position rect BEFORE
     * collision is resolved — allows door to fire even through structure walls.
     * Returns true if a door was triggered.
     */
    public boolean checkAt(Rectangle nextRect) {
        int currentWorld = gp.getWorldIndex();

        for (DoorEntry door : doors) {
            if (door.sourceWorld != currentWorld) continue;
            if (nextRect.intersects(door.getRect())) {
                gp.teleportWithFade(door.destWorld, door.spawnTileX, door.spawnTileY);
                return true;
            }
        }
        return false;
    }

    private Rectangle getPlayerRect() {
        int inset = 2;
        int px = (int) gp.getPlayer().getX() + inset;
        int py = (int) gp.getPlayer().getY() + inset;
        int pw = gp.getPlayer().getSize() - inset * 2;
        int ph = gp.getPlayer().getSize() - inset * 2;
        return new Rectangle(px, py, pw, ph);
    }

    public List<DoorEntry> getDoors() { return doors; }
}
