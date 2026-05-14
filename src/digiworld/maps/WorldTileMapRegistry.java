package digiworld.maps;

import digiworld.core.World;

public final class WorldTileMapRegistry {
    private WorldTileMapRegistry() {
    }

    public static void apply(World world) {
        if (world == null) {
            return;
        }
        String name = world.getName();
        if ("Hometown".equalsIgnoreCase(name) || "House 1".equalsIgnoreCase(name)) {
            HometownTileMap.applyTo(world);
            return;
        }
        if ("World 2 - Alpha Village".equalsIgnoreCase(name)) {
            AlphaVillageTileMap.applyTo(world);
            return;
        }
        if ("World 3 - Beta City".equalsIgnoreCase(name)) {
            BetaCityTileMap.applyTo(world);
        }
    }
}
