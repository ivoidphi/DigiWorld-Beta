import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public class Inventory {
    private final List<String> beasts;
    private final Map<String, BufferedImage> iconCache;
    private int selectedIndex;

    public Inventory() {
        this.beasts = new ArrayList<>();
        this.iconCache = new HashMap<>();
        this.selectedIndex = 0;
    }

    public void addBeast(String beastName) {
        if (beastName == null || beastName.isBlank()) {
            return;
        }
        beasts.add(beastName.trim());
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

    public void render(Graphics2D g2d, int logicalWidth, int logicalHeight) {
        int boxWidth = Math.min(500, logicalWidth - 80);
        int boxHeight = Math.min(280, logicalHeight - 80);
        int x = (logicalWidth - boxWidth) / 2;
        int y = (logicalHeight - boxHeight) / 2;

        g2d.setColor(new Color(0, 0, 0, 220));
        g2d.fillRoundRect(x, y, boxWidth, boxHeight, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(x, y, boxWidth, boxHeight, 10, 10);
        g2d.setFont(UIFont.regular(12f));
        g2d.drawString("Inventory - Caught Beasts", x + 14, y + 24);
        g2d.drawString("B / ESC: Close  Up/Down: Navigate", x + 14, y + boxHeight - 12);

        if (beasts.isEmpty()) {
            g2d.drawString("No beasts caught yet.", x + 14, y + 52);
            return;
        }

        int listStartY = y + 50;
        int iconSize = 14;
        for (int i = 0; i < beasts.size(); i++) {
            int rowY = listStartY + i * 20;
            if (rowY > y + boxHeight - 30) {
                break;
            }
            if (i == selectedIndex) {
                g2d.setColor(new Color(255, 255, 255, 45));
                g2d.fillRoundRect(x + 10, rowY - 13, boxWidth - 20, 18, 6, 6);
                g2d.setColor(Color.WHITE);
            } else {
                g2d.setColor(new Color(220, 220, 220));
            }
            String beastName = beasts.get(i);
            BufferedImage icon = getBeastIcon(beastName);
            int textX = x + 16;
            if (icon != null) {
                g2d.drawImage(icon, x + 16, rowY - 12, iconSize, iconSize, null);
                textX += iconSize + 6;
            }
            g2d.drawString((i + 1) + ". " + beastName, textX, rowY);
        }
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
