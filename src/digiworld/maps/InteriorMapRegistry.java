package digiworld.maps;

import digiworld.core.World;

public final class InteriorMapRegistry {
    private static final AbstractImageInteriorMap[] INTERIORS = {
            new House1InteriorTileMap(),
            new LaboratoryInteriorTileMap()
    };

    private InteriorMapRegistry() {
    }

    public static boolean apply(World world) {
        for (AbstractImageInteriorMap interior : INTERIORS) {
            if (!interior.appliesTo(world)) {
                continue;
            }
            interior.applyTo(world);
            return true;
        }
        return false;
    }
}
