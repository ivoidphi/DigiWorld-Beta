package digiworld.maps;

public final class House1InteriorTileMap extends AbstractImageInteriorMap {
    private static final String[] BLOCKED_MASK = {
            "############",
            "############",
            "############",
            "###.......##",
            "#........###",
            "#....##..###",
            "#........###",
            "#.......#.##",
            "#.........##",
            "#.........##",
            "#####..#####",
            "#####..#####",
            "#####..#####"
    };

    public House1InteriorTileMap() {
        super("House 1", "res/Structures/House/House_interior.png");
    }

    @Override
    protected String fillImagePath() {
        return "res/Structures/House/black.png";
    }

    @Override
    protected String[] blockedMask() {
        return BLOCKED_MASK;
    }
}
