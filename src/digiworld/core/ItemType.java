package digiworld.core;

public enum ItemType {
    BEAST_BATTERY("Beast Battery", "Restores 30% of max HP", 50, Effect.HEAL_PERCENT, 0.3),
    PREMIUM_BEAST_BATTERY("Premium Beast Battery", "Restores 70% of max HP", 120, Effect.HEAL_PERCENT, 0.7),
    ULTRA_BEAST_BATTERY("Ultra Beast Battery", "Restores 100% of max HP", 200, Effect.HEAL_PERCENT, 1.0),
    BEAST_ARMOR("Beast Armor", "Boosts Defense by 30%", 150, Effect.BUFF_DEFENSE, 0.3),
    POWER_GEAR("Power Gear", "Boosts Attack by 30%", 150, Effect.BUFF_ATTACK, 0.3),
    SECONDARY_BATTERY("Secondary Battery", "Increases max HP by 30%", 180, Effect.BUFF_MAX_HP, 0.3);

    public enum Effect {
        HEAL_PERCENT, BUFF_ATTACK, BUFF_DEFENSE, BUFF_MAX_HP
    }

    private final String displayName;
    private final String description;
    private final int price;
    private final Effect effect;
    private final double value;

    ItemType(String displayName, String description, int price, Effect effect, double value) {
        this.displayName = displayName;
        this.description = description;
        this.price = price;
        this.effect = effect;
        this.value = value;
    }

    public String displayName() { return displayName; }
    public String description() { return description; }
    public int price() { return price; }
    public Effect effect() { return effect; }
    public double value() { return value; }

    public static ItemType fromDisplayName(String name) {
        for (ItemType t : values()) {
            if (t.displayName.equalsIgnoreCase(name)) return t;
        }
        return null;
    }
}
