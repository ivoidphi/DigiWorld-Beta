package digiworld.battle;

import digiworld.app.*;
import digiworld.battle.*;
import digiworld.core.*;
import digiworld.dialogue.*;
import digiworld.ui.*;

public final class BeastCatalog {
    public record BeastTemplate(
            String name,
            int baseHp,
            int baseAttack,
            int baseDefense,
            int baseSpeed,
            int baseEnergy,
            int energyRegen,
            int level,
            BeastElement element,
            String henshinAnnouncement,
            BattleMove[] moves
    ) {}

    private static final BeastTemplate[] TEMPLATES = new BeastTemplate[]{
            new BeastTemplate("Kyoflare", 95, 120, 70, 95, 110, 14, 1, BeastElement.FIRE, "The supernova tyrant, KYOFLARE!",
                    new BattleMove[]{
                            new BattleMove("Kyoryu Tail", 40, 0, 0, StatusEffect.NONE, 0.0, BeastElement.NEUTRAL),
                            new BattleMove("Blaze Fangs", 85, 35, 1, StatusEffect.BURN, 0.10, BeastElement.FIRE),
                            new BattleMove("Solar Burst", 130, 65, 3, StatusEffect.BURN, 0.30, BeastElement.FIRE)
                    }),
            new BeastTemplate("Nokami", 100, 95, 70, 115, 100, 15, 1, BeastElement.WATER, "The master of water element, NOKAMI!",
                    new BattleMove[]{
                            new BattleMove("Swift Strike", 40, 0, 0, StatusEffect.NONE, 0.0, BeastElement.NEUTRAL),
                            new BattleMove("Aqua Slice", 70, 30, 1, StatusEffect.NONE, 0.0, BeastElement.WATER, MoveEffect.HIGH_CRIT, 0.45, 0, 0, 0.0),
                            new BattleMove("Shuriken Wave", 110, 0, 2, StatusEffect.NONE, 0.0, BeastElement.WATER, MoveEffect.EXTRA_HITS_CHANCE, 0.50, 0, 1, 0.5)
                    }),
            new BeastTemplate("Vineratops", 140, 85, 125, 50, 120, 16, 1, BeastElement.GRASS, "The forest guardian, VINERATOPS!",
                    new BattleMove[]{
                            new BattleMove("Horn Charge", 35, 0, 0, StatusEffect.NONE, 0.0, BeastElement.NEUTRAL),
                            new BattleMove("Horn Vines", 75, 30, 1, StatusEffect.NONE, 0.0, BeastElement.GRASS, MoveEffect.HEAL_SELF_PERCENT_MAX_HP, 0.10, 0, 0, 0.0),
                            new BattleMove("Horn Bloom", 115, 60, 3, StatusEffect.NONE, 0.0, BeastElement.GRASS, MoveEffect.HEAL_SELF_PERCENT_MAX_HP, 0.15, 0, 0, 0.0)
                    }),
            new BeastTemplate("Voltchu", 100, 100, 65, 120, 100, 15, 1, BeastElement.ELECTRIC, "The greatest lightning swordsmouse, VOLTCHU!",
                    new BattleMove[]{
                            new BattleMove("Quick Draw", 40, 0, 0, StatusEffect.NONE, 0.0, BeastElement.NEUTRAL),
                            new BattleMove("Electric Slash", 80, 30, 1, StatusEffect.NONE, 0.0, BeastElement.ELECTRIC, MoveEffect.LOWER_TARGET_ATTACK_PERCENT, 0.20, 1, 0, 0.0),
                            new BattleMove("Lightning Wrath", 120, 60, 3, StatusEffect.PARALYZE, 0.30, BeastElement.ELECTRIC)
                    }),
            new BeastTemplate("Zyuugor", 150, 65, 130, 45, 130, 18, 1, BeastElement.EARTH, "The earthshaking gorilla, Zyuugor!",
                    new BattleMove[]{
                            new BattleMove("Kong Fist", 40, 0, 0, StatusEffect.NONE, 0.0, BeastElement.NEUTRAL),
                            new BattleMove("Mineral Bomb", 80, 35, 1, StatusEffect.NONE, 0.30, BeastElement.EARTH, MoveEffect.AFTERSHOCK_PERCENT_TARGET_MAX_HP, 0.10, 0, 0, 0.0),
                            new BattleMove("Colossus Breaker", 125, 70, 3, StatusEffect.NONE, 1.0, BeastElement.EARTH, MoveEffect.AFTERSHOCK_PERCENT_TARGET_MAX_HP, 0.10, 0, 0, 0.0)
                    }),
            new BeastTemplate("Pirrot", 105, 90, 65, 115, 100, 15, 1, BeastElement.WIND, "The Bird of the seven seas, Pirrot!",
                    new BattleMove[]{
                            new BattleMove("Feather Bullet", 40, 0, 0, StatusEffect.NONE, 0.0, BeastElement.NEUTRAL),
                            new BattleMove("Wing Blade", 80, 30, 1, StatusEffect.NONE, 0.0, BeastElement.WIND, MoveEffect.LOWER_TARGET_DEFENSE_PERCENT, 0.20, 1, 0, 0.0),
                            new BattleMove("Tempest Slash", 115, 55, 2, StatusEffect.NONE, 0.0, BeastElement.WIND, MoveEffect.LOWER_TARGET_ATTACK_PERCENT, 0.50, 1, 0, 0.0)
                    }),
            new BeastTemplate("Gekuma", 110, 105, 85, 80, 110, 15, 1, BeastElement.FIGHTING, "The master of Bear God Fist, Gekuma!",
                    new BattleMove[]{
                            new BattleMove("Bear Claw", 40, 0, 0, StatusEffect.NONE, 0.0, BeastElement.NEUTRAL),
                            new BattleMove("Power Jab", 85, 35, 1, StatusEffect.NONE, 0.0, BeastElement.FIGHTING, MoveEffect.LOWER_TARGET_DEFENSE_PERCENT, 0.20, 1, 0, 0.0),
                            new BattleMove("Primal Strike", 120, 60, 3, StatusEffect.NONE, 0.0, BeastElement.FIGHTING, MoveEffect.EXTRA_HITS, 0.0, 0, 3, 0.20)
                    }),
            new BeastTemplate("Shadefox", 95, 115, 60, 110, 100, 14, 1, BeastElement.DARK, "The shadowy phantom thief, SHADEFOX!",
                    new BattleMove[]{
                            new BattleMove("Sneak Bite", 40, 0, 0, StatusEffect.NONE, 0.0, BeastElement.NEUTRAL),
                            new BattleMove("Crescent Claw", 90, 35, 1, StatusEffect.NONE, 0.0, BeastElement.DARK, MoveEffect.HIGH_CRIT, 0.20, 0, 0, 0.0),
                            new BattleMove("Phantom Slash", 125, 65, 2, StatusEffect.FEAR, 0.50, BeastElement.DARK)
                    }),
            new BeastTemplate("Kingmantis", 140, 150, 65, 90, 140, 18, 1, BeastElement.STEEL, "The king of blades, KINGMANTIS!",
                    new BattleMove[]{
                            new BattleMove("Iron Jab", 50, 0, 0, StatusEffect.NONE, 0.0, BeastElement.NEUTRAL),
                            new BattleMove("Twin Dagger", 95, 35, 1, StatusEffect.NONE, 0.0, BeastElement.STEEL, MoveEffect.EXTRA_HITS, 0.0, 0, 1, 0.50),
                            new BattleMove("Sovereign Blade", 145, 70, 2, StatusEffect.NONE, 0.0, BeastElement.STEEL, MoveEffect.HIGH_CRIT, 0.55, 0, 0, 0.0)
                    }),
            new BeastTemplate("Woltrix", 150, 160, 130, 140, 180, 25, 1, BeastElement.ELECTRIC, "The incomplete lightning savage, WOLTRIX!",
                    new BattleMove[]{
                            new BattleMove("Iron Fang", 55, 0, 0, StatusEffect.NONE, 0.0, BeastElement.NEUTRAL),
                            new BattleMove("Volt Saber", 105, 45, 1, StatusEffect.PARALYZE, 0.40, BeastElement.ELECTRIC),
                            new BattleMove("Thunder Barrage", 165, 90, 3, StatusEffect.NONE, 0.0, BeastElement.ELECTRIC, MoveEffect.EXTRA_HITS, 0.0, 0, 4, 0.30)
                    }),
            new BeastTemplate("All Mighty", 9999, 9999, 9999, 9999, 9999, 999, 99, BeastElement.NEUTRAL, "The invincible test beast, ALL MIGHTY!",
                    new BattleMove[]{
                            new BattleMove("All Breaker", 9999, 0, 0, StatusEffect.NONE, 0.0, BeastElement.NEUTRAL),
                            new BattleMove("Absolute Zero", 9999, 0, 0, StatusEffect.NONE, 0.0, BeastElement.NEUTRAL),
                            new BattleMove("Final Verdict", 9999, 0, 0, StatusEffect.NONE, 0.0, BeastElement.NEUTRAL)
                    })
    };

    private BeastCatalog() {}

    public static BeastTemplate findByName(String name) {
        if (name == null) {
            return null;
        }
        for (BeastTemplate template : TEMPLATES) {
            if (template.name().equalsIgnoreCase(name.trim())) {
                return template;
            }
        }
        return null;
    }

    public static BattleCreature createCreature(String name) {
        BeastTemplate template = findByName(name);
        if (template == null) {
            template = TEMPLATES[0];
        }
        return new BattleCreature(
                template.name(),
                template.baseHp(),
                template.baseAttack(),
                template.baseDefense(),
                template.baseSpeed(),
                template.baseEnergy(),
                template.energyRegen(),
                template.element(),
                template.level()
        );
    }

    public static BattleMove[] movesFor(String name) {
        BeastTemplate template = findByName(name);
        if (template == null) {
            template = TEMPLATES[0];
        }
        return template.moves();
    }

    public static BeastElement elementFor(String name) {
        BeastTemplate template = findByName(name);
        if (template == null) {
            return BeastElement.NEUTRAL;
        }
        return template.element();
    }

    public static String[] starterNames() {
        String[] names = new String[TEMPLATES.length];
        for (int i = 0; i < TEMPLATES.length; i++) {
            names[i] = TEMPLATES[i].name();
        }
        return names;
    }
}
