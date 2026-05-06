import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class GameUiRenderer {
    private final int logicalWidth;
    private final int logicalHeight;
    private final String playerName;
    private final BattleSystem battleSystem;
    private final BufferedImage inventoryIcon;
    private final BufferedImage interactIcon;
    private final java.util.Map<String, BufferedImage> worldBanners;

    public GameUiRenderer(int logicalWidth, int logicalHeight, String playerName, BattleSystem battleSystem) {
        this.logicalWidth = logicalWidth;
        this.logicalHeight = logicalHeight;
        this.playerName = playerName;
        this.battleSystem = battleSystem;
        this.inventoryIcon = loadInventoryIcon();
        this.interactIcon = loadInteractIcon();
        this.worldBanners = loadWorldBanners();
    }

    public void drawHud(Graphics2D g2d, World current, boolean interactionMenuOpen, boolean hasNearbyNpc,
            String interactionMessage, String currentObjective) {
        BufferedImage banner = getWorldBanner(current.getName());
        if (banner != null) {
            int bannerX = (logicalWidth - banner.getWidth()) / 2;
            int bannerY = logicalHeight / 16;
            g2d.drawImage(banner, bannerX, bannerY, null);
        }

        if (currentObjective != null && !currentObjective.isEmpty()) {
            g2d.setFont(UIFont.bold(12f));
            String objText = "OBJ: " + currentObjective;
            int objWidth = g2d.getFontMetrics().stringWidth(objText);
            int objX = (logicalWidth - objWidth) / 2;
            int objY = logicalHeight / 16 + 64 + 22;
            g2d.setColor(new Color(0, 0, 0, 140));
            g2d.fillRoundRect(objX - 8, objY - 12, objWidth + 16, 18, 6, 6);
            g2d.setColor(new Color(255, 230, 120, 240));
            g2d.drawString(objText, objX, objY);
        }

        int iconBoxX = 10;
        int iconBoxY = logicalHeight / 2 - 26;
        int iconBoxW = 52;
        int iconBoxH = 52;
        if (inventoryIcon != null) {
            g2d.drawImage(inventoryIcon, iconBoxX, iconBoxY, iconBoxW, iconBoxH, null);
        }

        if (interactionMessage != null && !interactionMessage.isEmpty()) {
            g2d.setColor(new Color(255, 255, 255, 230));
            g2d.setFont(UIFont.regular(10f));
            g2d.drawString(interactionMessage, 16, logicalHeight - 12);
        }
    }

    public void drawNpcNametag(Graphics2D g2d, Npc npc, Camera camera) {
        String text = npc.getName();
        g2d.setFont(UIFont.regular(10f));
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int paddingX = 6;
        int boxWidth = textWidth + paddingX * 2;
        int boxHeight = 14;
        int npcScreenX = (int) npc.getX() - camera.getX();
        int npcScreenY = (int) npc.getY() - camera.getY() + npc.getVisualBobOffsetY();
        int boxX = npcScreenX + (npc.getSize() - boxWidth) / 2;
        int boxY = npcScreenY - 18;
        g2d.setColor(new Color(0, 0, 0, 190));
        g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 6, 6);
        g2d.setColor(new Color(255, 255, 255, 230));
        g2d.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 6, 6);
        g2d.drawString(text, boxX + paddingX, boxY + 10);
    }

    public void drawInteractBubble(Graphics2D g2d, Npc npc, Player player, Camera camera) {
        if (npc == null) {
            return;
        }
        if (interactIcon == null) {
            return;
        }
        int size = 18;
        int npcScreenLeftX = (int) npc.getX() - camera.getX();
        int npcScreenCenterY = (int) npc.getY() - camera.getY() + npc.getSize() / 2 + npc.getVisualBobOffsetY();
        int x = npcScreenLeftX - size - 4;
        int y = npcScreenCenterY - size / 2;
        x = Math.max(4, Math.min(logicalWidth - size - 4, x));
        y = Math.max(4, Math.min(logicalHeight - size - 4, y));
        g2d.drawImage(interactIcon, x, y, size, size, null);
    }

    public void drawNpcSpeechBubble(Graphics2D g2d, Npc speechBubbleNpc, double speechBubbleTimer,
            String speechBubbleText, Camera camera) {
        if (speechBubbleNpc == null || speechBubbleTimer <= 0.0 || speechBubbleText == null
                || speechBubbleText.isBlank()) {
            return;
        }
        g2d.setFont(UIFont.regular(10f));
        int maxWidth = 220;
        String[] lines = wrapTextByWidth(g2d, speechBubbleText, maxWidth - 18);
        int lineHeight = 12;
        int boxWidth = maxWidth;
        int boxHeight = 12 + lines.length * lineHeight;
        int npcX = (int) speechBubbleNpc.getX() - camera.getX() + speechBubbleNpc.getSize() / 2;
        int npcY = (int) speechBubbleNpc.getY() - camera.getY();
        int x = npcX - boxWidth / 2;
        int y = npcY - boxHeight - 20;
        x = Math.max(6, Math.min(logicalWidth - boxWidth - 6, x));
        y = Math.max(6, y);

        g2d.setColor(new Color(0, 0, 0, 210));
        g2d.fillRoundRect(x, y, boxWidth, boxHeight, 8, 8);
        g2d.setColor(new Color(240, 240, 240));
        g2d.drawRoundRect(x, y, boxWidth, boxHeight, 8, 8);
        g2d.setColor(new Color(255, 230, 140));
        for (int i = 0; i < lines.length; i++) {
            g2d.drawString(lines[i], x + 9, y + 16 + i * lineHeight);
        }
    }

    public void drawMenu(Graphics2D g2d, Npc activeNpc, World currentWorld, boolean starterSequenceCompleted,
            boolean canGoNextWorld, boolean canGoPreviousWorld, boolean hasDialogue, boolean hasSeenDialogue) {
        int boxWidth = 360;
        int boxHeight = 110;
        int x = (logicalWidth - boxWidth) / 2;
        int y = logicalHeight - boxHeight - 14;

        g2d.setColor(new Color(0, 0, 0, 220));
        g2d.fillRoundRect(x, y, boxWidth, boxHeight, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(x, y, boxWidth, boxHeight, 8, 8);
        g2d.setFont(UIFont.regular(12f));
        String menuNpcName = activeNpc != null ? activeNpc.getName() : "NPC";
        g2d.drawString(menuNpcName + " Menu", x + 14, y + 24);

        boolean isProfessorAlfred = activeNpc != null && "Prof Alfred".equalsIgnoreCase(activeNpc.getName());
        if (isProfessorAlfred && !starterSequenceCompleted) {
            g2d.drawString("1: Choose 3 starter beasts", x + 14, y + 50);
        } else if (isProfessorAlfred && starterSequenceCompleted) {
            if ("Hometown".equalsIgnoreCase(currentWorld.getName())) {
                g2d.drawString("1: Go to Alpha Village", x + 14, y + 50);
            } else {
                g2d.drawString("1: Go to Hometown", x + 14, y + 50);
            }
        } else if (isProfessorAlfred && canGoNextWorld) {
            g2d.drawString("1: Go to Alpha Village", x + 14, y + 50);
        } else if (hasDialogue) {
            g2d.drawString("1: Talk", x + 14, y + 50);
            g2d.drawString("2: See dialogue again", x + 14, y + 70);
        } else if (hasDialogue) {
            g2d.drawString("2: " + (hasSeenDialogue ? "See dialogue again" : "Talk"), x + 14, y + 70);
        } else if (canGoPreviousWorld) {
            g2d.drawString("2: Go to previous world", x + 14, y + 70);
        }
        g2d.drawString("E or ESC: Close", x + 14, y + 90);
    }

    public void drawRpgBeastSelection(Graphics2D g2d, String title, String subtitle, String[] choices,
            int selectedIndex, LinkedHashSet<Integer> selectedStarterIndices, GameState gameState) {
        int boxWidth = Math.min(760, logicalWidth - 40);
        int boxHeight = Math.min(420, logicalHeight - 30);
        int x = (logicalWidth - boxWidth) / 2;
        int y = (logicalHeight - boxHeight) / 2;

        g2d.setColor(new Color(18, 14, 28, 235));
        g2d.fillRoundRect(x, y, boxWidth, boxHeight, 12, 12);
        g2d.setColor(new Color(236, 214, 150));
        g2d.drawRoundRect(x, y, boxWidth, boxHeight, 12, 12);
        g2d.drawRoundRect(x + 4, y + 4, boxWidth - 8, boxHeight - 8, 10, 10);

        g2d.setFont(UIFont.bold(14f));
        g2d.setColor(new Color(255, 242, 201));
        g2d.drawString(title, x + 16, y + 26);
        g2d.setFont(UIFont.regular(11f));
        g2d.drawString(subtitle, x + 16, y + 44);

        int cardGap = 8;
        int columns = 5;
        int cardWidth = (boxWidth - 32 - cardGap * (columns - 1)) / columns;
        int cardHeight = 122;
        int cardY = y + 58;
        for (int i = 0; i < choices.length; i++) {
            int row = i / columns;
            int col = i % columns;
            int cardX = x + 16 + col * (cardWidth + cardGap);
            int drawY = cardY + row * (cardHeight + 8);
            boolean selected = i == selectedIndex;
            boolean partySelected = selectedStarterIndices.contains(i) && gameState == GameState.STARTER_SELECT;
            g2d.setColor(selected ? new Color(58, 75, 127, 235) : new Color(31, 35, 58, 220));
            g2d.fillRoundRect(cardX, drawY, cardWidth, cardHeight, 8, 8);
            g2d.setColor(selected ? new Color(255, 236, 171) : new Color(185, 185, 200));
            g2d.drawRoundRect(cardX, drawY, cardWidth, cardHeight, 8, 8);
            if (partySelected) {
                g2d.setColor(new Color(120, 255, 160));
                g2d.drawRoundRect(cardX + 1, drawY + 1, cardWidth - 2, cardHeight - 2, 8, 8);
            }

            BufferedImage sprite = battleSystem.getCreatureSprite(choices[i]);
            if (sprite != null) {
                g2d.drawImage(sprite, cardX + 12, drawY + 8, cardWidth - 24, 50, null);
            }
            g2d.setColor(Color.WHITE);
            g2d.setFont(UIFont.bold(10f));
            g2d.drawString((i + 1) + ". " + choices[i], cardX + 6, drawY + 69);

            BeastCatalog.BeastTemplate stats = BeastCatalog.findByName(choices[i]);
            g2d.setFont(UIFont.regular(9f));
            if (stats != null) {
                g2d.drawString("Lv " + stats.level() + " HP " + stats.baseHp(), cardX + 6, drawY + 84);
                g2d.drawString("ATK " + stats.baseAttack() + " DEF " + stats.baseDefense(), cardX + 6, drawY + 96);
            }
            String label = partySelected ? "Chosen" : (selected ? "Selected" : "Pick");
            g2d.drawString(label, cardX + 6, drawY + 110);
        }

        g2d.setColor(new Color(230, 230, 240));
        g2d.setFont(UIFont.regular(10f));
        if (gameState == GameState.STARTER_SELECT) {
            g2d.drawString("WASD/Arrows/1-9 move | ENTER add/remove | Pick 3 beasts | ESC cancel", x + 16,
                    y + boxHeight - 10);
        } else {
            g2d.drawString("A/D or 1-3 choose | ENTER confirm | ESC cancel", x + 16, y + boxHeight - 10);
        }
    }

    public void drawDialogueAboveNpc(Graphics2D g2d, Npc activeNpc, DialogueController dialogueController,
            Player player, Camera camera) {
        if (activeNpc == null) {
            return;
        }
        DialoguePage page = dialogueController.getCurrentPage();
        String currentSpeaker = page.getSpeaker();
        String typed = dialogueController.getVisibleText();
        String[] wrapped = wrapTextByWidth(g2d, typed, 300);
        int boxWidth = 320;
        int lineHeight = 14;
        int footerHeight = dialogueController.isLineFinished() ? 16 : 0;
        int maxLines = 7;
        String[] visibleLines = wrapped;
        if (wrapped.length > maxLines) {
            visibleLines = new String[maxLines];
            for (int i = 0; i < maxLines - 1; i++) {
                visibleLines[i] = wrapped[i];
            }
            visibleLines[maxLines - 1] = wrapped[maxLines - 1] + "...";
        }
        int boxHeight = 28 + visibleLines.length * lineHeight + footerHeight;
        boolean speakerIsPlayer = playerName.equalsIgnoreCase(currentSpeaker);
        int anchorX = speakerIsPlayer
                ? (int) player.getX() - camera.getX() + player.getSize() / 2
                : (int) activeNpc.getX() - camera.getX() + activeNpc.getSize() / 2;
        int anchorY = speakerIsPlayer
                ? (int) player.getY() - camera.getY()
                : (int) activeNpc.getY() - camera.getY();
        int x = anchorX - boxWidth / 2;
        int y = anchorY - boxHeight - 22;
        x = Math.max(8, Math.min(logicalWidth - boxWidth - 8, x));
        y = Math.max(8, y);

        g2d.setColor(new Color(0, 0, 0, 220));
        g2d.fillRoundRect(x, y, boxWidth, boxHeight, 8, 8);
        g2d.setColor(new Color(230, 230, 240));
        g2d.drawRoundRect(x, y, boxWidth, boxHeight, 8, 8);
        g2d.setColor(speakerIsPlayer ? new Color(170, 220, 255) : new Color(255, 227, 140));
        g2d.setFont(UIFont.bold(11f));
        g2d.drawString(currentSpeaker, x + 10, y + 14);
        g2d.setColor(Color.WHITE);
        g2d.setFont(UIFont.regular(10f));
        for (int i = 0; i < visibleLines.length; i++) {
            g2d.drawString(visibleLines[i], x + 10, y + 32 + i * lineHeight);
        }
        if (dialogueController.isLineFinished()) {
            g2d.setColor(new Color(200, 200, 215));
            g2d.drawString("ENTER/SPACE continue | ESC skip", x + 10, y + boxHeight - 6);
        }
    }

    private String[] wrapTextByWidth(Graphics2D g2d, String text, int maxWidthPx) {
        if (text == null || text.isEmpty()) {
            return new String[] { "" };
        }
        FontMetrics fm = g2d.getFontMetrics(UIFont.regular(10f));
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.isEmpty()) {
                current.append(word);
                continue;
            }
            String candidate = current + " " + word;
            if (fm.stringWidth(candidate) <= maxWidthPx) {
                current.append(" ").append(word);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines.toArray(new String[0]);
    }

    private BufferedImage loadInventoryIcon() {
        return ResourceLoader.loadImage("res/icons/backpack.png");
    }

    private BufferedImage loadInteractIcon() {
        return ResourceLoader.loadImage("res/icons/interact.png");
    }

    private java.util.Map<String, BufferedImage> loadWorldBanners() {
        java.util.Map<String, BufferedImage> map = new java.util.HashMap<>();
        map.put("Hometown", loadBanner("res/ui/hometown.png"));
        map.put("World 2 - Alpha Village", loadBanner("res/ui/alpha-village.png"));
        map.put("World 3 - Beta City", loadBanner("res/ui/beta-city.png"));
        map.put("World 4 - Collapse Zone", loadBanner("res/ui/the-collapse-of-beta-city.png"));
        return map;
    }

    private BufferedImage loadBanner(String path) {
        return ResourceLoader.loadImage(path);
    }

    private BufferedImage getWorldBanner(String worldName) {
        return worldBanners.getOrDefault(worldName, null);
    }
}
