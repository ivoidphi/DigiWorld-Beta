package digiworld.ui;

import digiworld.app.*;
import digiworld.battle.*;
import digiworld.core.*;
import digiworld.dialogue.*;
import digiworld.ui.*;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.Composite;
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
    private final java.util.Map<String, BufferedImage> dialoguePortraits;

    public GameUiRenderer(int logicalWidth, int logicalHeight, String playerName, BattleSystem battleSystem) {
        this.logicalWidth = logicalWidth;
        this.logicalHeight = logicalHeight;
        this.playerName = playerName;
        this.battleSystem = battleSystem;
        this.inventoryIcon = loadInventoryIcon();
        this.interactIcon = loadInteractIcon();
        this.worldBanners = loadWorldBanners();
        this.dialoguePortraits = loadDialoguePortraits();
    }

    public void drawHud(Graphics2D g2d, World current, boolean interactionMenuOpen, boolean hasNearbyNpc, String interactionMessage, String currentObjective, double objectiveAlpha, double objectiveCompleteAlpha, boolean hasGWatch, boolean scanActive, int coins) {
        drawHud(g2d, current, interactionMenuOpen, hasNearbyNpc, interactionMessage, currentObjective, objectiveAlpha, objectiveCompleteAlpha, hasGWatch, scanActive, coins, 1.0);
    }

    public void drawHud(Graphics2D g2d, World current, boolean interactionMenuOpen, boolean hasNearbyNpc, String interactionMessage, String currentObjective, double objectiveAlpha, double objectiveCompleteAlpha, boolean hasGWatch, boolean scanActive, int coins, double bannerAlpha) {
        BufferedImage banner = getWorldBanner(current.getName());
        if (banner != null && bannerAlpha > 0.001) {
            int scaledW = (int) Math.round(banner.getWidth() * 1.45);
            int scaledH = (int) Math.round(banner.getHeight() * 1.45);
            int bannerX = (logicalWidth - scaledW) / 2;
            int bannerY = logicalHeight / 16;
            Composite oldComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0.0, Math.min(1.0, bannerAlpha))));
            drawImageShadow(g2d, banner, bannerX, bannerY, scaledW, scaledH, 3, 3, 0.45f);
            g2d.drawImage(banner, bannerX, bannerY, scaledW, scaledH, null);
            g2d.setComposite(oldComposite);
        }

        int objectiveX = 14;
        int objectiveY = 22;
        if (banner != null) {
            int scaledH = (int) Math.round(banner.getHeight() * 1.45);
            int bannerY = logicalHeight / 16;
            objectiveY = bannerY + scaledH + 14;
        }

        if (currentObjective != null && !currentObjective.isEmpty() && objectiveAlpha > 0.001) {
            Composite oldComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0.0, Math.min(1.0, objectiveAlpha))));
            g2d.setFont(UIFont.bold(12f));
            String objText = "OBJ: " + currentObjective;
            int objWidth = g2d.getFontMetrics().stringWidth(objText);
            g2d.setColor(new Color(0, 0, 0, 140));
            g2d.fillRoundRect(objectiveX - 8, objectiveY - 12, objWidth + 16, 18, 6, 6);
            g2d.setColor(new Color(255, 230, 120, 240));
            drawStringShadow(g2d, objText, objectiveX, objectiveY, 1, 1, new Color(0, 0, 0, 180));
            g2d.drawString(objText, objectiveX, objectiveY);
            g2d.setComposite(oldComposite);
        }
        if (objectiveCompleteAlpha > 0.001) {
            Composite oldComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0.0, Math.min(1.0, objectiveCompleteAlpha))));
            g2d.setFont(UIFont.bold(12f));
            String done = "Objective Complete!";
            int w = g2d.getFontMetrics().stringWidth(done);
            g2d.setColor(new Color(0, 0, 0, 140));
            g2d.fillRoundRect(objectiveX - 8, objectiveY - 12, w + 16, 18, 6, 6);
            g2d.setColor(new Color(148, 255, 154, 240));
            drawStringShadow(g2d, done, objectiveX, objectiveY, 1, 1, new Color(0, 0, 0, 180));
            g2d.drawString(done, objectiveX, objectiveY);
            g2d.setComposite(oldComposite);
        }

        int iconBoxX = 10;
        int iconBoxY = logicalHeight / 2 - 19;
        int iconBoxW = 38;
        int iconBoxH = 38;
        if (inventoryIcon != null) {
            drawImageShadow(g2d, inventoryIcon, iconBoxX, iconBoxY, iconBoxW, iconBoxH, 2, 2, 0.5f);
            g2d.drawImage(inventoryIcon, iconBoxX, iconBoxY, iconBoxW, iconBoxH, null);
        }

        int coinBoxY = iconBoxY + iconBoxH + 4;
        int coinBoxH = 16;
        g2d.setColor(new Color(10, 10, 16, 210));
        g2d.fillRoundRect(iconBoxX, coinBoxY, iconBoxW, coinBoxH, 6, 6);
        g2d.setColor(new Color(255, 215, 60));
        g2d.drawRoundRect(iconBoxX, coinBoxY, iconBoxW, coinBoxH, 6, 6);
        g2d.setFont(UIFont.bold(9f));
        String coinText = String.valueOf(coins);
        int coinTextW = g2d.getFontMetrics().stringWidth(coinText);
        drawStringShadow(g2d, coinText, iconBoxX + (iconBoxW - coinTextW) / 2, coinBoxY + 12, 1, 1, new Color(0, 0, 0, 180));
        g2d.setColor(new Color(255, 230, 100));
        g2d.drawString(coinText, iconBoxX + (iconBoxW - coinTextW) / 2, coinBoxY + 12);

        if (interactionMessage != null && !interactionMessage.isEmpty()) {
            g2d.setColor(new Color(255, 255, 255, 230));
            g2d.setFont(UIFont.regular(10f));
            drawStringShadow(g2d, interactionMessage, 16, logicalHeight - 12, 1, 1, new Color(0, 0, 0, 180));
            g2d.drawString(interactionMessage, 16, logicalHeight - 12);
        }

        if (hasGWatch) {
            int slotW = 54;
            int slotH = 22;
            int x = logicalWidth / 2 - slotW / 2;
            int y = logicalHeight - slotH - 10;
            g2d.setColor(new Color(10, 10, 16, 210));
            g2d.fillRoundRect(x, y, slotW, slotH, 6, 6);
            g2d.setColor(scanActive ? new Color(255, 110, 110) : new Color(190, 190, 205));
            g2d.drawRoundRect(x, y, slotW, slotH, 6, 6);
            g2d.setFont(UIFont.bold(10f));
            g2d.drawString("G", x + 6, y + 14);
            g2d.setFont(UIFont.regular(9f));
            g2d.drawString("G-Watch", x + 16, y + 14);
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
        g2d.fillRoundRect(boxX + 2, boxY + 2, boxWidth, boxHeight, 6, 6);
        g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 6, 6);
        g2d.setColor(new Color(255, 255, 255, 230));
        g2d.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 6, 6);
        drawStringShadow(g2d, text, boxX + paddingX, boxY + 10, 1, 1, new Color(0, 0, 0, 180));
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
        drawImageShadow(g2d, interactIcon, x, y, size, size, 1, 1, 0.5f);
        g2d.drawImage(interactIcon, x, y, size, size, null);
    }

    public void drawNpcSpeechBubble(Graphics2D g2d, Npc speechBubbleNpc, double speechBubbleTimer, String speechBubbleText, Camera camera) {
        if (speechBubbleNpc == null || speechBubbleTimer <= 0.0 || speechBubbleText == null || speechBubbleText.isBlank()) {
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
        g2d.fillRoundRect(x + 2, y + 2, boxWidth, boxHeight, 8, 8);
        g2d.fillRoundRect(x, y, boxWidth, boxHeight, 8, 8);
        g2d.setColor(new Color(240, 240, 240));
        g2d.drawRoundRect(x, y, boxWidth, boxHeight, 8, 8);
        g2d.setColor(new Color(255, 230, 140));
        for (int i = 0; i < lines.length; i++) {
            g2d.drawString(lines[i], x + 9, y + 16 + i * lineHeight);
        }
    }

    public void drawMenu(Graphics2D g2d, Npc activeNpc, World currentWorld, boolean starterSequenceCompleted, boolean canGoNextWorld, boolean canGoPreviousWorld, boolean hasDialogue, boolean hasSeenDialogue) {
        int boxWidth = 360;
        int boxHeight = 110;
        int x = (logicalWidth - boxWidth) / 2;
        int y = logicalHeight - boxHeight - 14;

        g2d.setColor(new Color(0, 0, 0, 220));
        g2d.fillRoundRect(x + 2, y + 2, boxWidth, boxHeight, 8, 8);
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

    public void drawRpgBeastSelection(Graphics2D g2d, String title, String subtitle, String[] choices, int selectedIndex, LinkedHashSet<Integer> selectedStarterIndices, GameState gameState) {
        int boxWidth = Math.min(720, logicalWidth - 36);
        int boxHeight = Math.min(410, logicalHeight - 28);
        int x = (logicalWidth - boxWidth) / 2;
        int y = (logicalHeight - boxHeight) / 2;

        g2d.setColor(new Color(20, 22, 32, 235));
        g2d.fillRoundRect(x + 2, y + 2, boxWidth, boxHeight, 10, 10);
        g2d.fillRoundRect(x, y, boxWidth, boxHeight, 10, 10);
        g2d.setColor(new Color(220, 220, 230));
        g2d.drawRoundRect(x, y, boxWidth, boxHeight, 10, 10);

        g2d.setFont(UIFont.bold(13f));
        g2d.setColor(new Color(245, 245, 250));
        g2d.drawString(title, x + 16, y + 26);
        g2d.setFont(UIFont.regular(10f));
        g2d.setColor(new Color(205, 205, 220));
        g2d.drawString(subtitle, x + 16, y + 42);
        if (gameState == GameState.STARTER_SELECT) {
            g2d.drawString("Chosen: " + selectedStarterIndices.size() + "/3", x + boxWidth - 110, y + 26);
        }

        int listX = x + 14;
        int listY = y + 56;
        int listW = (int) (boxWidth * 0.60);
        int listH = boxHeight - 86;
        g2d.setColor(new Color(30, 34, 48, 210));
        g2d.fillRoundRect(listX, listY, listW, listH, 8, 8);
        g2d.setColor(new Color(185, 185, 200));
        g2d.drawRoundRect(listX, listY, listW, listH, 8, 8);

        int columns = 3;
        int cardGap = 7;
        int cardWidth = (listW - 14 - cardGap * (columns - 1)) / columns;
        int cardHeight = 74;
        for (int i = 0; i < choices.length; i++) {
            int row = i / columns;
            int col = i % columns;
            int cardX = listX + 7 + col * (cardWidth + cardGap);
            int drawY = listY + 8 + row * (cardHeight + 6);
            boolean selected = i == selectedIndex;
            boolean partySelected = selectedStarterIndices.contains(i) && gameState == GameState.STARTER_SELECT;
            g2d.setColor(selected ? new Color(66, 86, 138, 235) : new Color(40, 45, 64, 220));
            g2d.fillRoundRect(cardX, drawY, cardWidth, cardHeight, 8, 8);
            g2d.setColor(selected ? new Color(255, 236, 171) : new Color(170, 170, 185));
            g2d.drawRoundRect(cardX, drawY, cardWidth, cardHeight, 8, 8);
            if (partySelected) {
                g2d.setColor(new Color(120, 255, 160));
                g2d.drawRoundRect(cardX + 1, drawY + 1, cardWidth - 2, cardHeight - 2, 8, 8);
            }

            BufferedImage sprite = battleSystem.getCreatureSprite(choices[i]);
            if (sprite != null) {
                int spriteW = sprite.getWidth();
                int spriteH = sprite.getHeight();
                int targetSize = 34;
                double scale = Math.min((double) targetSize / spriteW, (double) targetSize / spriteH);
                int drawW = (int) (spriteW * scale);
                int drawH = (int) (spriteH * scale);
                int offsetX = (targetSize - drawW) / 2;
                int offsetY = (targetSize - drawH) / 2;
                g2d.drawImage(sprite, cardX + 6 + offsetX, drawY + 6 + offsetY, drawW, drawH, null);
            }
            g2d.setColor(Color.WHITE);
            g2d.setFont(UIFont.bold(10f));
            g2d.drawString((i + 1) + ". " + choices[i], cardX + 44, drawY + 18);
            BeastCatalog.BeastTemplate stats = BeastCatalog.findByName(choices[i]);
            g2d.setFont(UIFont.regular(9f));
            if (stats != null) {
                g2d.drawString("Lv " + stats.level() + "  HP " + stats.baseHp(), cardX + 44, drawY + 34);
                g2d.drawString("ATK " + stats.baseAttack() + "  DEF " + stats.baseDefense(), cardX + 44, drawY + 48);
            }
            String label = partySelected ? "Chosen" : (selected ? "Selected" : "");
            if (!label.isEmpty()) {
                g2d.setColor(new Color(228, 228, 240));
                g2d.drawString(label, cardX + 44, drawY + 62);
            }
        }

        int detailX = listX + listW + 10;
        int detailY = listY;
        int detailW = boxWidth - (detailX - x) - 14;
        int detailH = listH;
        g2d.setColor(new Color(30, 34, 48, 210));
        g2d.fillRoundRect(detailX, detailY, detailW, detailH, 8, 8);
        g2d.setColor(new Color(185, 185, 200));
        g2d.drawRoundRect(detailX, detailY, detailW, detailH, 8, 8);
        String selectedName = choices[Math.max(0, Math.min(choices.length - 1, selectedIndex))];
        BeastCatalog.BeastTemplate selectedStats = BeastCatalog.findByName(selectedName);
        BufferedImage selectedSprite = battleSystem.getCreatureSprite(selectedName);
        if (selectedSprite != null) {
            int spriteW = selectedSprite.getWidth();
            int spriteH = selectedSprite.getHeight();
            int targetW = detailW - 36;
            int targetH = 92;
            double scale = Math.min((double) targetW / spriteW, (double) targetH / spriteH);
            int drawW = (int) (spriteW * scale);
            int drawH = (int) (spriteH * scale);
            int offsetX = (targetW - drawW) / 2;
            int offsetY = (targetH - drawH) / 2;
            g2d.drawImage(selectedSprite, detailX + 18 + offsetX, detailY + 14 + offsetY, drawW, drawH, null);
        }
        g2d.setColor(Color.WHITE);
        g2d.setFont(UIFont.bold(12f));
        g2d.drawString(selectedName, detailX + 12, detailY + 124);
        g2d.setFont(UIFont.regular(10f));
        if (selectedStats != null) {
            g2d.drawString("Level: " + selectedStats.level(), detailX + 12, detailY + 146);
            g2d.drawString("HP: " + selectedStats.baseHp(), detailX + 12, detailY + 164);
            g2d.drawString("ATK: " + selectedStats.baseAttack(), detailX + 12, detailY + 182);
            g2d.drawString("DEF: " + selectedStats.baseDefense(), detailX + 12, detailY + 200);
        }

        g2d.setColor(new Color(230, 230, 240));
        g2d.setFont(UIFont.regular(10f));
        if (gameState == GameState.STARTER_SELECT) {
            g2d.drawString("WASD/Arrows select | ENTER add/remove | Pick 3 | ESC cancel", x + 16, y + boxHeight - 10);
        } else {
            g2d.drawString("A/D or 1-3 choose | ENTER confirm | ESC cancel", x + 16, y + boxHeight - 10);
        }
    }

    public void drawDialogueAboveNpc(Graphics2D g2d, Npc activeNpc, DialogueController dialogueController, Player player, Camera camera, World currentWorld) {
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
        int bottomMargin = (int)(logicalHeight * 0.25);
        int x = (logicalWidth - boxWidth) / 2;
        int y = logicalHeight - bottomMargin - boxHeight;
        y = Math.max(8, y);

        g2d.setColor(new Color(0, 0, 0, 220));
        g2d.fillRoundRect(x + 2, y + 2, boxWidth, boxHeight, 8, 8);
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
            g2d.drawString("ENTER/SPACE/E continue | ESC skip", x + 10, y + boxHeight - 6);
        }
    }

    private BufferedImage resolveDialoguePortrait(DialoguePage page, String speaker) {
        if (page.getPortraitPath() != null && !page.getPortraitPath().isBlank()) {
            BufferedImage direct = loadBanner(page.getPortraitPath());
            if (direct != null) {
                return direct;
            }
        }
        if (speaker == null) {
            return null;
        }
        return dialoguePortraits.getOrDefault(speaker.trim().toLowerCase(), null);
    }

    private String[] wrapTextByWidth(Graphics2D g2d, String text, int maxWidthPx) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
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
        try {
            return ImageIO.read(new File("res/icons/backpack.png"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private BufferedImage loadInteractIcon() {
        try {
            return ImageIO.read(new File("res/icons/interact.png"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private java.util.Map<String, BufferedImage> loadWorldBanners() {
        java.util.Map<String, BufferedImage> map = new java.util.HashMap<>();
        map.put("Hometown", loadBanner("res/ui/hometown.png"));
        map.put("World 2 - Alpha Village", loadBanner("res/ui/alpha-village.png"));
        map.put("World 3 - Beta City", loadBanner("res/ui/beta-city.png"));
        map.put("Corrupted Beta City", loadBanner("res/ui/the-collapse-of-beta-city.png"));
        return map;
    }

    private java.util.Map<String, BufferedImage> loadDialoguePortraits() {
        java.util.Map<String, BufferedImage> map = new java.util.HashMap<>();
        map.put("player", loadBanner("res/characters/player/player-fw.png"));
        map.put("professor alfred", loadBanner("res/characters/professor-alfred/profalfred-fw.png"));
        map.put("general edrian", loadBanner("res/characters/gen-ed/gened-fw.png"));
        map.put("chief rei", loadBanner("res/characters/chief-rei/chiefrei-fw.png"));
        return map;
    }

    private BufferedImage loadBanner(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (Exception ignored) {
            return null;
        }
    }

    private BufferedImage getWorldBanner(String worldName) {
        return worldBanners.getOrDefault(worldName, null);
    }

    private void drawStringShadow(Graphics2D g2d, String text, int x, int y, int dx, int dy, Color shadowColor) {
        Color old = g2d.getColor();
        g2d.setColor(shadowColor);
        g2d.drawString(text, x + dx, y + dy);
        g2d.setColor(old);
    }

    private void drawImageShadow(Graphics2D g2d, BufferedImage image, int x, int y, int w, int h, int dx, int dy, float alpha) {
        if (image == null) {
            return;
        }
        Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.drawImage(image, x + dx, y + dy, w, h, null);
        g2d.setComposite(oldComposite);
    }
}
