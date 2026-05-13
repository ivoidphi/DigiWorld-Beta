package digiworld.ui;

import digiworld.app.*;
import digiworld.battle.*;
import digiworld.core.*;
import digiworld.dialogue.*;
import digiworld.ui.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

public class Inventory {
    private final List<String> beasts;
    private final LinkedHashSet<Integer> equippedIndices;
    private final Map<String, BufferedImage> iconCache;
    private final Set<String> items;
    private int selectedIndex;

    public Inventory() {
        this.beasts = new ArrayList<>();
        this.equippedIndices = new LinkedHashSet<>();
        this.iconCache = new HashMap<>();
        this.items = new LinkedHashSet<>();
        this.selectedIndex = 0;
    }

    public void addItem(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return;
        }
        items.add(itemName.trim());
    }

    public boolean hasItem(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return false;
        }
        return items.contains(itemName.trim());
    }

    public void addBeast(String beastName) {
        if (beastName == null || beastName.isBlank()) {
            return;
        }
        String normalized = beastName.trim();
        beasts.add(normalized);
        if (equippedIndices.size() < 3) {
            equippedIndices.add(beasts.size() - 1);
        }
        if (beasts.size() == 1) {
            selectedIndex = 0;
        }
    }

    public void moveSelection(int delta) {
        if (beasts.isEmpty()) {
            selectedIndex = 0;
            return;
        }
        selectedIndex += delta;
        if (selectedIndex < 0) {
            selectedIndex = beasts.size() - 1;
        } else if (selectedIndex >= beasts.size()) {
            selectedIndex = 0;
        }
    }

    public String getSelectedBeast() {
        if (beasts.isEmpty()) {
            return "";
        }
        return beasts.get(selectedIndex);
    }

    public int size() {
        return beasts.size();
    }

    public Set<String> getOwnedBeastNames() {
        Set<String> owned = new LinkedHashSet<>();
        owned.addAll(beasts);
        return owned;
    }

    public String[] getEquippedBeastNames() {
        List<String> equipped = new ArrayList<>();
        for (Integer idx : equippedIndices) {
            if (idx != null && idx >= 0 && idx < beasts.size()) {
                equipped.add(beasts.get(idx));
            }
        }
        return equipped.toArray(new String[0]);
    }

    public String toggleEquippedSelected() {
        if (beasts.isEmpty()) {
            return "No beasts to equip.";
        }
        if (equippedIndices.contains(selectedIndex)) {
            equippedIndices.remove(selectedIndex);
            return beasts.get(selectedIndex) + " unequipped.";
        }
        if (equippedIndices.size() >= 3) {
            return "You can only equip 3 beasts.";
        }
        equippedIndices.add(selectedIndex);
        return beasts.get(selectedIndex) + " equipped (" + equippedIndices.size() + "/3).";
    }

    public void render(Graphics2D g2d, int logicalWidth, int logicalHeight) {
        int boxWidth = Math.min(620, logicalWidth - 70);
        int boxHeight = Math.min(280, logicalHeight - 80);
        int x = (logicalWidth - boxWidth) / 2;
        int y = (logicalHeight - boxHeight) / 2;

        g2d.setColor(new Color(0, 0, 0, 220));
        g2d.fillRoundRect(x, y, boxWidth, boxHeight, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(x, y, boxWidth, boxHeight, 10, 10);
        g2d.setFont(UIFont.regular(12f));
        g2d.drawString("Inventory - Caught Beasts", x + 14, y + 24);
        g2d.drawString("Equipped: " + equippedIndices.size() + "/3", x + boxWidth - 120, y + 24);
        g2d.drawString("B / ESC: Close  Up/Down: Navigate  E/ENTER: Equip", x + 14, y + boxHeight - 12);

        if (beasts.isEmpty()) {
            g2d.drawString("No beasts caught yet.", x + 14, y + 52);
            return;
        }

        int listStartY = y + 50;
        int iconSize = 14;
        int listWidth = (int) (boxWidth * 0.48);
        for (int i = 0; i < beasts.size(); i++) {
            int rowY = listStartY + i * 20;
            if (rowY > y + boxHeight - 30) {
                break;
            }
            if (i == selectedIndex) {
                g2d.setColor(new Color(255, 255, 255, 45));
                g2d.fillRoundRect(x + 10, rowY - 13, listWidth - 18, 18, 6, 6);
                g2d.setColor(Color.WHITE);
            } else {
                g2d.setColor(new Color(220, 220, 220));
            }
            String beastName = beasts.get(i);
            String equipMark = equippedIndices.contains(i) ? "[EQ] " : "";
            BufferedImage icon = getBeastIcon(beastName);
            int textX = x + 16;
            if (icon != null) {
                g2d.drawImage(icon, x + 16, rowY - 12, iconSize, iconSize, null);
                textX += iconSize + 6;
            }
            g2d.drawString((i + 1) + ". " + equipMark + beastName, textX, rowY);
        }

        String selected = getSelectedBeast();
        if (!selected.isEmpty()) {
            drawStatsPanel(g2d, x + listWidth, y + 42, boxWidth - listWidth - 14, boxHeight - 56, selected);
        }
    }

    private void drawStatsPanel(Graphics2D g2d, int x, int y, int width, int height, String beastName) {
        g2d.setColor(new Color(28, 33, 46, 210));
        g2d.fillRoundRect(x, y, width, height, 8, 8);
        g2d.setColor(new Color(220, 220, 230));
        g2d.drawRoundRect(x, y, width, height, 8, 8);

        BeastCatalog.BeastTemplate template = BeastCatalog.findByName(beastName);
        g2d.setColor(Color.WHITE);
        g2d.setFont(UIFont.bold(12f));
        g2d.drawString(beastName + " Stats", x + 10, y + 22);
        g2d.setFont(UIFont.regular(11f));

        if (template == null) {
            g2d.drawString("No stat profile found.", x + 10, y + 44);
            return;
        }

        BattleCreature creature = BeastCatalog.createCreature(beastName);
        int lineY = y + 44;
        int step = 18;
        g2d.drawString("Level: " + template.level(), x + 10, lineY); lineY += step;
        g2d.drawString("HP: " + creature.getHp() + "/" + creature.getMaxHp(), x + 10, lineY); lineY += step;
        g2d.drawString("Attack: " + creature.getAttack(), x + 10, lineY); lineY += step;
        g2d.drawString("Defense: " + creature.getDefense(), x + 10, lineY); lineY += step;
        g2d.drawString("Energy: " + creature.getEnergy() + "/" + creature.getMaxEnergy(), x + 10, lineY);
    }

    private BufferedImage getBeastIcon(String beastName) {
        if (beastName == null || beastName.isBlank()) {
            return null;
        }
        String key = beastName.trim().toLowerCase();
        if (iconCache.containsKey(key)) {
            return iconCache.get(key);
        }

        String dir = key.replace(" ", "");
        String base = "res/beasts/" + dir + "/" + dir;
        String[] candidates = new String[]{
                base + "-f.png",
                base + "-fw.png",
                base + "-b.png"
        };
        BufferedImage icon = null;
        for (String path : candidates) {
            try {
                File file = new File(path);
                if (file.exists()) {
                    icon = ImageIO.read(file);
                    break;
                }
            } catch (IOException ignored) {
            }
        }

        iconCache.put(key, icon);
        return icon;
    }
}
