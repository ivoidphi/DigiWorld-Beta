package digiworld.core;

import digiworld.app.GamePanel;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

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
        doors.add(new DoorEntry(
                WorldIndex.HOMETOWN, 11, 16, tileSize,
                WorldIndex.HOUSE_1, 25, 19
        ));
    }

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
