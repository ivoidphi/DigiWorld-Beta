package digiworld.battle;

import digiworld.app.*;
import digiworld.battle.*;
import digiworld.core.*;
import digiworld.dialogue.*;
import digiworld.ui.*;

public class BattleMove {
    private final String name;
    private final int power;
    private final int energyCost;
    private final int cooldownTurns;
    private final StatusEffect inflictEffect;
    private final double effectChance;
    private final BeastElement type;
    private final MoveEffect moveEffect;
    private final double effectValue;
    private final int effectTurns;
    private final int extraHits;
    private final double extraHitPowerRatio;

    public BattleMove(String name, int power, int energyCost, int cooldownTurns, StatusEffect inflictEffect, double effectChance, BeastElement type) {
        this(name, power, energyCost, cooldownTurns, inflictEffect, effectChance, type, MoveEffect.NONE, 0.0, 0, 0, 0.0);
    }

    public BattleMove(
            String name,
            int power,
            int energyCost,
            int cooldownTurns,
            StatusEffect inflictEffect,
            double effectChance,
            BeastElement type,
            MoveEffect moveEffect,
            double effectValue,
            int effectTurns,
            int extraHits,
            double extraHitPowerRatio
    ) {
        this.name = name;
        this.power = power;
        this.energyCost = energyCost;
        this.cooldownTurns = cooldownTurns;
        this.inflictEffect = inflictEffect;
        this.effectChance = effectChance;
        this.type = type;
        this.moveEffect = moveEffect;
        this.effectValue = effectValue;
        this.effectTurns = effectTurns;
        this.extraHits = extraHits;
        this.extraHitPowerRatio = extraHitPowerRatio;
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

    public BeastElement getType() {
        return type;
    }

    public MoveEffect getMoveEffect() {
        return moveEffect;
    }

    public double getEffectValue() {
        return effectValue;
    }

    public int getEffectTurns() {
        return effectTurns;
    }

    public int getExtraHits() {
        return extraHits;
    }

    public double getExtraHitPowerRatio() {
        return extraHitPowerRatio;
    }
}
