package digiworld.core;

/**
 * World index constants — use these everywhere instead of magic numbers.
 * Add a new constant each time you add a world to GamePanel.createWorlds().
 */
public final class WorldIndex {

    private WorldIndex() {}

    public static final int HOMETOWN      = 0;
    public static final int ALPHA_VILLAGE = 1;
    public static final int BETA_CITY     = 2;
    public static final int COLLAPSE_ZONE = 3;
    public static final int HOUSE_1       = 4;
    public static final int LABORATORY    = 5;

    // Add new worlds here as you build them:
    // public static final int HOUSE_2    = 5;
}
