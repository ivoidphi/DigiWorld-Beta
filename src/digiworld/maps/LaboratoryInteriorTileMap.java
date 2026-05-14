package digiworld.maps;

public final class LaboratoryInteriorTileMap extends AbstractImageInteriorMap {
    private static final String[] BLOCKED_MASK = {
            "############",
            "############",
            "############",
            "############",
            "##.......###",
            "#........###",
            "#...###..###",
            "#...###..###",
            "#........###",
            "#........###",
            "#........###",
            "#####..#####",
            "#####..#####"
    };

    public LaboratoryInteriorTileMap() {
        super("Laboratory", "res/Structures/House/lab_interior.png");
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
