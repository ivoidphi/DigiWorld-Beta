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
        if ("Hometown".equalsIgnoreCase(name)) {
            HometownTileMap map = new HometownTileMap(world.getWidth(), world.getHeight());
            map.buildBase();
            map.buildFeatures();
            map.applyTo(world);
            return;
        }
        if ("House 1".equalsIgnoreCase(name)) {
            HometownTileMap map = new HometownTileMap(world.getWidth(), world.getHeight());
            map.buildBase();
            map.buildFeatures();
            map.applyTo(world);
            return;
        }
        if ("World 2 - Alpha Village".equalsIgnoreCase(name)) {
            AlphaVillageTileMap map = new AlphaVillageTileMap(world.getWidth(), world.getHeight());
            map.buildBase();
            map.buildFeatures();
            map.applyTo(world);
            return;
        }
        if ("World 3 - Beta City".equalsIgnoreCase(name)) {
            BetaCityTileMap map = new BetaCityTileMap(world.getWidth(), world.getHeight());
            map.buildBase();
            map.buildFeatures();
            map.applyTo(world);
            return;
        }
        if ("Corrupted Beta City".equalsIgnoreCase(name)) {
            BetaCityTileMap map = new BetaCityTileMap(world.getWidth(), world.getHeight());
            map.buildBase();
            map.buildFeatures();
            map.applyTo(world);
            return;
        }
    }
}
