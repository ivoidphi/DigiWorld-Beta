public class BattleCreature {
    private final String name;
    private final int baseHp;
    private final int baseAttack;
    private final int baseDefense;
    private int hp;
    private int level;
    private int exp;
    private int energy;
    private StatusEffect statusEffect;
    private int statusTurns;

    public BattleCreature(String name, int maxHp, int attack, int defense) {
        this(name, maxHp, attack, defense, 5);
    }

    public BattleCreature(String name, int maxHp, int attack, int defense, int level) {
        this.name = name;
        this.baseHp = maxHp;
        this.baseAttack = attack;
        this.baseDefense = defense;
        this.level = Math.max(1, level);
        this.exp = 0;
        this.energy = getMaxEnergy();
        this.statusEffect = StatusEffect.NONE;
        this.statusTurns = 0;
        this.hp = getMaxHp();
    }

    public int dealDamageTo(BattleCreature target) {
        int raw = getAttack() - (target.getDefense() / 2);
        int damage = Math.max(2, raw);
        target.takeDamage(damage);
        return damage;
    }

    public int takeDamage(int damage) {
        int applied = Math.max(0, damage);
        hp = Math.max(0, hp - applied);
        return applied;
    }

    public boolean isFainted() {
        return hp <= 0;
    }

    public void healToFull() {
        hp = getMaxHp();
        energy = getMaxEnergy();
        clearStatus();
    }

    public String getName() {
        return name;
    }

    public int getMaxHp() {
        return baseHp + level * 3;
    }

    public int getHp() {
        return hp;
    }

    public int getAttack() {
        return baseAttack + level;
    }

    public int getDefense() {
        return baseDefense + (level / 2);
    }

    public int getLevel() {
        return level;
    }

    public int getExp() {
        return exp;
    }

    public int getExpToNextLevel() {
        return 40 + (level * 20);
    }

    public int getEnergy() {
        return energy;
    }

    public int getMaxEnergy() {
        return 16 + level * 2;
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
        if (leveled) {
            hp = getMaxHp();
            energy = getMaxEnergy();
            clearStatus();
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
    }

    public void clearStatus() {
        statusEffect = StatusEffect.NONE;
        statusTurns = 0;
    }
}
