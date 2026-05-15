package digiworld.battle;

import digiworld.app.*;
import digiworld.battle.*;
import digiworld.core.*;
import digiworld.dialogue.*;
import digiworld.ui.*;

public class BattleCreature {
    private final String name;
    private final int baseHp;
    private final int baseAttack;
    private final int baseDefense;
    private final int baseSpeed;
    private final int baseEnergy;
    private final int baseEnergyRegen;
    private final BeastElement element;
    private int hp;
    private int level;
    private int exp;
    private int energy;
    private StatusEffect statusEffect;
    private int statusTurns;
    private double attackModifier;
    private int attackModifierTurns;
    private double defenseModifier;
    private int defenseModifierTurns;
    private double permanentAttackModifier;
    private double permanentDefenseModifier;
    private double permanentMaxHpModifier;

    public BattleCreature(String name, int maxHp, int attack, int defense) {
        this(name, maxHp, attack, defense, 80, 100, 12, BeastElement.NEUTRAL, 1);
    }

    public BattleCreature(String name, int maxHp, int attack, int defense, int speed, int maxEnergy, int energyRegen, BeastElement element, int level) {
        this.name = name;
        this.baseHp = maxHp;
        this.baseAttack = attack;
        this.baseDefense = defense;
        this.baseSpeed = speed;
        this.baseEnergy = maxEnergy;
        this.baseEnergyRegen = energyRegen;
        this.element = element;
        this.level = Math.max(1, level);
        this.exp = 0;
        this.energy = getMaxEnergy();
        this.statusEffect = StatusEffect.NONE;
        this.statusTurns = 0;
        this.attackModifier = 1.0;
        this.attackModifierTurns = 0;
        this.defenseModifier = 1.0;
        this.defenseModifierTurns = 0;
        this.permanentAttackModifier = 1.0;
        this.permanentDefenseModifier = 1.0;
        this.permanentMaxHpModifier = 1.0;
        this.hp = getMaxHp();
    }

    public int dealDamageTo(BattleCreature target) {
        int raw = getAttack() - (target.getDefense() / 2);
        int damage = Math.max(2, raw);
        target.takeDamage(damage);
        return damage;
    }

    public int takeDamage(int damage) {
        if (isAllMighty()) {
            return 0;
        }
        int applied = Math.max(0, damage);
        hp = Math.max(0, hp - applied);
        return applied;
    }

    public int heal(int amount) {
        int applied = Math.max(0, amount);
        int before = hp;
        hp = Math.min(getMaxHp(), hp + applied);
        return hp - before;
    }

    public boolean isFainted() {
        return hp <= 0;
    }

    public void healToFull() {
        hp = getMaxHp();
        energy = getMaxEnergy();
        clearStatus();
        clearStatModifiers();
    }

    public void healByPercent(double percent) {
        int amount = (int) Math.round(getMaxHp() * Math.max(0.0, Math.min(1.0, percent)));
        heal(amount);
    }

    public void boostPermanentAttack(double percent) {
        permanentAttackModifier = 1.0 + Math.max(0.0, percent);
    }

    public void boostPermanentDefense(double percent) {
        permanentDefenseModifier = 1.0 + Math.max(0.0, percent);
    }

    public void boostPermanentMaxHp(double percent) {
        double oldMax = getMaxHp();
        permanentMaxHpModifier = 1.0 + Math.max(0.0, percent);
        hp = (int) Math.round(hp * (getMaxHp() / oldMax));
    }

    public String getName() {
        return name;
    }

    public int getMaxHp() {
        int base = ((2 * baseHp) * level / 20) + level + 10;
        return (int) Math.round(base * permanentMaxHpModifier);
    }

    public int getHp() {
        return hp;
    }

    public int getAttack() {
        int stat = ((2 * baseAttack) * level / 20) + 5;
        double combined = attackModifier * permanentAttackModifier;
        return Math.max(1, (int) Math.round(stat * combined));
    }

    public int getDefense() {
        int stat = ((2 * baseDefense) * level / 20) + 5;
        double combined = defenseModifier * permanentDefenseModifier;
        return Math.max(1, (int) Math.round(stat * combined));
    }

    public int getSpeed() {
        return ((2 * baseSpeed) * level / 20) + 5;
    }

    public int getLevel() {
        return level;
    }

    public int getExp() {
        return exp;
    }

    public int getExpToNextLevel() {
        return (int) Math.floor(5.0 * level * level / 2.0);
    }

    public int getEnergy() {
        return energy;
    }

    public int getMaxEnergy() {
        return baseEnergy;
    }

    public int getEnergyRegen() {
        return baseEnergyRegen;
    }

    public BeastElement getElement() {
        return element;
    }

    public boolean isAllMighty() {
        return "All Mighty".equalsIgnoreCase(name);
    }

    public boolean spendEnergy(int cost) {
        if (energy < cost) {
            return false;
        }
        energy -= cost;
        return true;
    }

    public void recoverEnergy(int amount) {
        energy = Math.min(getMaxEnergy(), energy + Math.max(0, amount));
    }

    public boolean addExp(int amount) {
        exp += Math.max(0, amount);
        boolean leveled = false;
        while (exp >= getExpToNextLevel()) {
            exp -= getExpToNextLevel();
            level++;
            leveled = true;
        }
        return leveled;
    }

    public StatusEffect getStatusEffect() {
        return statusEffect;
    }

    public int getStatusTurns() {
        return statusTurns;
    }

    public void setStatus(StatusEffect effect, int turns) {
        if (effect == StatusEffect.NONE) {
            clearStatus();
            return;
        }
        statusEffect = effect;
        statusTurns = Math.max(1, turns);
    }

    public void tickStatus() {
        if (statusEffect == StatusEffect.NONE) {
            return;
        }
        statusTurns--;
        if (statusTurns <= 0) {
            clearStatus();
        }
        tickStatModifiers();
    }

    public void clearStatus() {
        statusEffect = StatusEffect.NONE;
        statusTurns = 0;
    }

    public void lowerAttack(double percent, int turns) {
        attackModifier = Math.max(0.1, 1.0 - Math.max(0.0, percent));
        attackModifierTurns = Math.max(1, turns);
    }

    public void lowerDefense(double percent, int turns) {
        defenseModifier = Math.max(0.1, 1.0 - Math.max(0.0, percent));
        defenseModifierTurns = Math.max(1, turns);
    }

    private void clearStatModifiers() {
        attackModifier = 1.0;
        attackModifierTurns = 0;
        defenseModifier = 1.0;
        defenseModifierTurns = 0;
    }

    private void tickStatModifiers() {
        if (attackModifierTurns > 0) {
            attackModifierTurns--;
            if (attackModifierTurns == 0) {
                attackModifier = 1.0;
            }
        }
        if (defenseModifierTurns > 0) {
            defenseModifierTurns--;
            if (defenseModifierTurns == 0) {
                defenseModifier = 1.0;
            }
        }
    }
}
