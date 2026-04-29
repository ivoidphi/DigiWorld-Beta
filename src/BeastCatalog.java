public final class BeastCatalog {
    public record BeastTemplate(String name, int baseHp, int baseAttack, int baseDefense, int level) {}

    private static final BeastTemplate[] TEMPLATES = new BeastTemplate[]{
            new BeastTemplate("Nokami", 38, 12, 8, 5),
            new BeastTemplate("Vineratops", 46, 10, 11, 5),
            new BeastTemplate("Kyoflare", 34, 14, 7, 5)
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
