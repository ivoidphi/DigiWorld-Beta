public final class BeastCatalog {
    public record BeastTemplate(String name, int baseHp, int baseAttack, int baseDefense, int level) {}

    private static final BeastTemplate[] TEMPLATES = new BeastTemplate[]{
            new BeastTemplate("Kyoflare", 95, 120, 70, 1),
            new BeastTemplate("Nokami", 100, 95, 70, 1),
            new BeastTemplate("Vineratops", 140, 85, 125, 1),
            new BeastTemplate("Voltchu", 100, 100, 65, 1),
            new BeastTemplate("Zyuugor", 150, 65, 130, 1),
            new BeastTemplate("Pirrot", 105, 90, 65, 1),
            new BeastTemplate("Gekuma", 110, 105, 85, 1),
            new BeastTemplate("Shadefox", 95, 115, 60, 1),
            new BeastTemplate("Kingmantis", 140, 150, 65, 1),
            new BeastTemplate("Woltrix", 150, 160, 130, 1)
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
                template.level()
        );
    }

    public static String[] starterNames() {
        String[] names = new String[TEMPLATES.length];
        for (int i = 0; i < TEMPLATES.length; i++) {
            names[i] = TEMPLATES[i].name();
        }
        return names;
    }
}
