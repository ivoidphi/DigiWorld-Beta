public class BattleMove {
    private final String name;
    private final int power;
    private final int energyCost;
    private final int cooldownTurns;
    private final StatusEffect inflictEffect;
    private final double effectChance;
    private final BattleType type;

    public BattleMove(String name, int power, int energyCost, int cooldownTurns, StatusEffect inflictEffect, double effectChance, BattleType type) {
        this.name = name;
        this.power = power;
        this.energyCost = energyCost;
        this.cooldownTurns = cooldownTurns;
        this.inflictEffect = inflictEffect;
        this.effectChance = effectChance;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public int getPower() {
        return power;
    }

    public int getEnergyCost() {
        return energyCost;
    }

    public int getCooldownTurns() {
        return cooldownTurns;
    }

    public StatusEffect getInflictEffect() {
        return inflictEffect;
    }

    public double getEffectChance() {
        return effectChance;
    }

    public BattleType getType() {
        return type;
    }
}
