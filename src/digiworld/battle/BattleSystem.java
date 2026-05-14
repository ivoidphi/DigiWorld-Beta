package digiworld.battle;

import digiworld.app.*;
import digiworld.audio.SoundManager;
import digiworld.core.*;
import digiworld.dialogue.*;
import digiworld.ui.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.imageio.ImageIO;

public class BattleSystem {
    private enum ScenePhase {
        INTRO, COMBAT, OUTRO, RESULT
    }

    private enum TurnPhase {
        PLAYER_TURN, ENEMY_THINKING, PLAYER_THINKING
    }

    private enum PlayerMenuMode {
        COMMAND, SWITCH_SELECT
    }

    private static final double THINK_DURATION_SECONDS = 2.0;
    private static final double INTRO_DURATION_SECONDS = 1.2;
    private static final double OUTRO_DURATION_SECONDS = 0.75;
    private static final double RESULT_DURATION_SECONDS = 1.8;
    private static final double HIT_ANIM_DURATION_SECONDS = 0.35;
    private static final int TEMP_RUN_SUCCESS_RATE_PERCENT = 100;

    private static final int COMMAND_BOX_HEIGHT = 154;
    private static final int COMMAND_BOX_SIDE_MARGIN = 16;
    private static final int COMMAND_BOX_BOTTOM_MARGIN = 16;
    private static final int PLAYER_X_OFFSET_FROM_CENTER = -70;
    private static final int ENEMY_X_OFFSET_FROM_CENTER = 70;
    private static final double PLAYER_Y_RATIO = 0.58;
    private static final double ENEMY_Y_RATIO = 0.34;
    private static final int GROUND_WIDTH_PADDING = 48;
    private static final int GROUND_Y_OFFSET = -12;

    private final Random random = new Random();
    private SoundManager soundManager;
    private BattleCreature[] playerCreatures;
    private BufferedImage[] playerBattleSprites;
    private BufferedImage[] playerBattleSpritesHit;
    private int activePlayerIndex;
    private BattleCreature enemyCreature;
    private BattleType battleType;
    private BattleMove[] playerMoves;
    private int[] playerMoveCooldowns;

    private boolean active;
    private String message;
    private String pendingResultMessage;

    private BattleCreature pendingCaughtCreature;

    private ScenePhase scenePhase;
    private TurnPhase turnPhase;
    private double introTimer;
    private double outroTimer;
    private double resultTimer;
    private double thinkTimer;
    private double playerHitTimer;
    private double enemyHitTimer;
    private Rectangle[] actionButtons;
    private Rectangle[] switchButtons;
    private PlayerMenuMode playerMenuMode;
    private int switchSelectionIndex;
    private boolean[] ownedPlayerCreatures;
    private boolean openingBeastChoiceRequired;
    private boolean forceSwitchChoiceRequired;
    private String lastResolvedEnemyName;
    private boolean lastBattleWon;
    private int lastCoinsEarned;
    private java.util.List<String> lastLeveledUpNames;

    private BufferedImage enemyBattleSprite;
    private BufferedImage enemyBattleSpriteHit;
    private final BufferedImage battleBackground;
    private BufferedImage blurredBattleBackground;
    private final Map<String, BufferedImage> beastSpriteCache;
    private final Map<String, BufferedImage> beastSpriteHitCache;

    public BattleSystem() {
        playerMoves = BeastCatalog.movesFor("Nokami");
        playerMoveCooldowns = new int[playerMoves.length];
        beastSpriteCache = new HashMap<>();
        beastSpriteHitCache = new HashMap<>();

        playerCreatures = new BattleCreature[0];
        playerBattleSprites = new BufferedImage[0];
        playerBattleSpritesHit = new BufferedImage[0];
        activePlayerIndex = 0;

        enemyBattleSprite = null;
        enemyBattleSpriteHit = null;
        battleBackground = loadSprite("res/scenes/hometown-battlescene.png");
        actionButtons = new Rectangle[6];
        switchButtons = new Rectangle[playerCreatures.length];
        playerMenuMode = PlayerMenuMode.COMMAND;
        switchSelectionIndex = 0;
        ownedPlayerCreatures = new boolean[playerCreatures.length];

        openingBeastChoiceRequired = false;
        forceSwitchChoiceRequired = false;
        lastResolvedEnemyName = "";
        lastBattleWon = false;
        lastCoinsEarned = 0;
        lastLeveledUpNames = new java.util.ArrayList<>();
    }

    public void setSoundManager(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    public void startWildBattle(String enemyBeastName) {
        startWildBattle(enemyBeastName, 1);
    }

    public void startWildBattle(String enemyBeastName, int level) {
        enemyCreature = BeastCatalog.createCreature(enemyBeastName, level);
        enemyBattleSprite = getBattleSpriteForCreature(enemyCreature.getName(), false);
        enemyBattleSpriteHit = getHitSpriteForCreature(enemyCreature.getName(), false);
        battleType = BattleType.WILD;
        for (int i = 0; i < playerMoveCooldowns.length; i++) {
            playerMoveCooldowns[i] = 0;
        }
        active = true;
        scenePhase = ScenePhase.INTRO;
        turnPhase = TurnPhase.PLAYER_TURN;
        introTimer = INTRO_DURATION_SECONDS;
        outroTimer = 0.0;
        resultTimer = 0.0;
        thinkTimer = 0.0;
        playerHitTimer = 0.0;
        enemyHitTimer = 0.0;
        pendingResultMessage = "";
        pendingCaughtCreature = null;
        openingBeastChoiceRequired = true;
        forceSwitchChoiceRequired = false;
        playerMenuMode = PlayerMenuMode.SWITCH_SELECT;

        switchSelectionIndex = 0;
        for (int i = 0; i < playerCreatures.length; i++) {
            if (ownedPlayerCreatures[i] && !playerCreatures[i].isFainted()) {
                switchSelectionIndex = i;
                break;
            }
        }

        message = "Choose your beast for this battle.";
        lastResolvedEnemyName = "";
        lastBattleWon = false;
        lastCoinsEarned = 0;
        lastLeveledUpNames = new java.util.ArrayList<>();
    }

    public void startTrainerBattle(String enemyBeastName) {
        startWildBattle(enemyBeastName);
        battleType = BattleType.PLAYER;
    }

    public String consumeLastResolvedEnemyName() {
        String out = lastResolvedEnemyName;
        lastResolvedEnemyName = "";
        return out;
    }

    public boolean consumeLastBattleWon() {
        boolean out = lastBattleWon;
        lastBattleWon = false;
        return out;
    }

    public int consumeCoinsEarned() {
        int out = lastCoinsEarned;
        lastCoinsEarned = 0;
        return out;
    }

    public java.util.List<String> consumeLeveledUpNames() {
        java.util.List<String> out = new java.util.ArrayList<>(lastLeveledUpNames);
        lastLeveledUpNames.clear();
        return out;
    }

    public BattleCreature consumeCaughtCreature() {
        BattleCreature caught = pendingCaughtCreature;
        pendingCaughtCreature = null;
        return caught;
    }

    public String[] getStarterChoices() {
        return BeastCatalog.starterNames();
    }

    public BufferedImage getCreatureSprite(String creatureName) {
        return getBattleSpriteForCreature(creatureName, false);
    }

    public void setStarterBeast(String beastName) {
        if (beastName == null) return;
        for (int i = 0; i < playerCreatures.length; i++) {
            if (playerCreatures[i].getName().equalsIgnoreCase(beastName.trim())) {
                activePlayerIndex = i;
                return;
            }
        }
    }

    public void setPlayerCreatures(BattleCreature[] creatures) {
        if (creatures == null || creatures.length == 0) return;
        this.playerCreatures = creatures;
        this.playerBattleSprites = new BufferedImage[creatures.length];
        this.playerBattleSpritesHit = new BufferedImage[creatures.length];
        this.ownedPlayerCreatures = new boolean[creatures.length];
        this.switchButtons = new Rectangle[creatures.length];

        for (int i = 0; i < creatures.length; i++) {
            this.ownedPlayerCreatures[i] = true;
            String name = creatures[i].getName();
            this.playerBattleSprites[i] = getBattleSpriteForCreature(name, true);
            this.playerBattleSpritesHit[i] = createRedTintedSprite(this.playerBattleSprites[i]);
        }
        this.activePlayerIndex = 0;
        refreshActiveMoves();
    }

    public BattleCreature[] getPlayerCreatures() {
        return this.playerCreatures;
    }

    public void setOwnedBeasts(Set<String> ownedBeasts) {
        Set<String> normalized = new HashSet<>();
        if (ownedBeasts != null) {
            for (String name : ownedBeasts) {
                if (name != null && !name.isBlank()) {
                    normalized.add(name.trim().toLowerCase());
                }
            }
        }
        for (int i = 0; i < playerCreatures.length; i++) {
            ownedPlayerCreatures[i] = normalized.contains(playerCreatures[i].getName().toLowerCase());
        }
        boolean hasOwned = false;
        for (boolean owned : ownedPlayerCreatures) {
            if (owned) {
                hasOwned = true;
                break;
            }
        }
        if (!hasOwned) {
            ownedPlayerCreatures[activePlayerIndex] = true;
        }
    }

    public boolean isActive() {
        return active;
    }

    public String getMessage() {
        return message;
    }

    public void handleClick(int logicalX, int logicalY) {
        if (!active || scenePhase != ScenePhase.COMBAT || turnPhase != TurnPhase.PLAYER_TURN) {
            return;
        }
        if (playerMenuMode == PlayerMenuMode.SWITCH_SELECT) {
            for (int i = 0; i < switchButtons.length; i++) {
                if (switchButtons[i] != null && switchButtons[i].contains(logicalX, logicalY)) {
                    trySwitchToIndex(i);
                    return;
                }
            }
            return;
        }
        if (actionButtons == null) {
            return;
        }
        for (int i = 0; i < actionButtons.length; i++) {
            if (actionButtons[i] != null && actionButtons[i].contains(logicalX, logicalY)) {
                runActionIndex(i);
                return;
            }
        }
    }

    public String handleInput(InputHandler input) {
        if (!active || scenePhase != ScenePhase.COMBAT || turnPhase != TurnPhase.PLAYER_TURN) {
            return "none";
        }
        if (playerMenuMode == PlayerMenuMode.SWITCH_SELECT) {
            if (input.consumeJustPressed(KeyEvent.VK_ESCAPE) && !getActivePlayerCreature().isFainted() && !openingBeastChoiceRequired && !forceSwitchChoiceRequired) {
                playerMenuMode = PlayerMenuMode.COMMAND;
                message = "Choose an action.";
                return "continue";
            }
            if (input.consumeJustPressed(KeyEvent.VK_LEFT) || input.consumeJustPressed(KeyEvent.VK_A)
                    || input.consumeJustPressed(KeyEvent.VK_UP) || input.consumeJustPressed(KeyEvent.VK_W)) {
                switchSelectionIndex = (switchSelectionIndex - 1 + playerCreatures.length) % playerCreatures.length;
                message = "Select a beast to switch to (ENTER confirm, ESC cancel).";
                return "continue";
            }
            if (input.consumeJustPressed(KeyEvent.VK_RIGHT) || input.consumeJustPressed(KeyEvent.VK_D)
                    || input.consumeJustPressed(KeyEvent.VK_DOWN) || input.consumeJustPressed(KeyEvent.VK_S)) {
                switchSelectionIndex = (switchSelectionIndex + 1) % playerCreatures.length;
                message = "Select a beast to switch to (ENTER confirm, ESC cancel).";
                return "continue";
            }
            if (input.consumeJustPressed(KeyEvent.VK_ENTER) || input.consumeJustPressed(KeyEvent.VK_SPACE)) {
                trySwitchToIndex(switchSelectionIndex);
                return "continue";
            }
            if (input.consumeJustPressed(KeyEvent.VK_1)) { trySwitchToIndex(0); return "continue"; }
            if (input.consumeJustPressed(KeyEvent.VK_2)) { trySwitchToIndex(1); return "continue"; }
            if (input.consumeJustPressed(KeyEvent.VK_3)) { trySwitchToIndex(2); return "continue"; }
            return "none";
        }
        if (input.consumeJustPressed(KeyEvent.VK_1)) { runActionIndex(0); return "continue"; }
        if (input.consumeJustPressed(KeyEvent.VK_2)) { runActionIndex(1); return "continue"; }
        if (input.consumeJustPressed(KeyEvent.VK_3)) { runActionIndex(2); return "continue"; }
        if (input.consumeJustPressed(KeyEvent.VK_4)) { runActionIndex(3); return "continue"; }
        if (input.consumeJustPressed(KeyEvent.VK_5)) { runActionIndex(4); return "continue"; }
        if (input.consumeJustPressed(KeyEvent.VK_6)) { runActionIndex(5); return "continue"; }
        return "none";
    }

    public void update(double deltaSeconds) {
        playerHitTimer = Math.max(0.0, playerHitTimer - deltaSeconds);
        enemyHitTimer = Math.max(0.0, enemyHitTimer - deltaSeconds);

        if (!active) {
            return;
        }
        if (scenePhase == ScenePhase.INTRO) {
            introTimer = Math.max(0.0, introTimer - deltaSeconds);
            if (introTimer <= 0.0) {
                scenePhase = ScenePhase.COMBAT;
                message = "Your turn. Choose an action.";
            }
            return;
        }
        if (scenePhase == ScenePhase.OUTRO) {
            outroTimer = Math.max(0.0, outroTimer - deltaSeconds);
            if (outroTimer <= 0.0) {
                scenePhase = ScenePhase.RESULT;
                resultTimer = RESULT_DURATION_SECONDS;
                message = pendingResultMessage;
            }
            return;
        }
        if (scenePhase == ScenePhase.RESULT) {
            resultTimer = Math.max(0.0, resultTimer - deltaSeconds);
            if (resultTimer <= 0.0) {
                active = false;
            }
            return;
        }
        if (turnPhase == TurnPhase.ENEMY_THINKING || turnPhase == TurnPhase.PLAYER_THINKING) {
            thinkTimer = Math.max(0.0, thinkTimer - deltaSeconds);
            if (thinkTimer <= 0.0) {
                if (turnPhase == TurnPhase.ENEMY_THINKING) {
                    enemyTurn();
                } else {
                    turnPhase = TurnPhase.PLAYER_TURN;
                    if (!getActivePlayerCreature().isFainted()) {
                        getActivePlayerCreature().recoverEnergy(getActivePlayerCreature().getEnergyRegen());
                        message = "Your turn. Choose an action.";
                    }
                }
            }
        }
    }

    public void render(Graphics2D g2d, int screenWidth, int screenHeight) {
        drawBattleBackground(g2d, screenWidth, screenHeight);
        if (scenePhase == ScenePhase.RESULT) {
            drawCenteredBanner(g2d, screenWidth, screenHeight, message);
            return;
        }

        int commandBoxY = screenHeight - COMMAND_BOX_BOTTOM_MARGIN - COMMAND_BOX_HEIGHT;
        int battleTop = 20;
        int battleBottom = commandBoxY - 16;
        int battleHeight = Math.max(220, battleBottom - battleTop);
        int spriteSize = Math.max(120, Math.min(200, screenHeight / 5));
        int playerTargetX = screenWidth / 2 - spriteSize + PLAYER_X_OFFSET_FROM_CENTER;
        int enemyTargetX = screenWidth / 2 + ENEMY_X_OFFSET_FROM_CENTER;
        int playerY = battleTop + (int) (battleHeight * PLAYER_Y_RATIO) - spriteSize / 2;
        int enemyY = battleTop + (int) (battleHeight * ENEMY_Y_RATIO) - spriteSize / 2;
        int playerX = playerTargetX;
        int enemyX = enemyTargetX;

        if (scenePhase == ScenePhase.INTRO) {
            double p = 1.0 - (introTimer / INTRO_DURATION_SECONDS);
            p = clamp01(p);
            playerX = (int) Math.round((-spriteSize - 40) + (playerTargetX + spriteSize + 40) * p);
            enemyX = (int) Math.round((screenWidth + 40) + (enemyTargetX - screenWidth - 40) * p);
        } else if (scenePhase == ScenePhase.OUTRO) {
            double p = 1.0 - (outroTimer / OUTRO_DURATION_SECONDS);
            p = clamp01(p);
            playerX = (int) Math.round(playerTargetX + (-spriteSize - 40 - playerTargetX) * p);
            enemyX = (int) Math.round(enemyTargetX + (screenWidth + 40 - enemyTargetX) * p);
        }

        g2d.setColor(new Color(47, 118, 87));
        g2d.fillOval(playerX - (GROUND_WIDTH_PADDING / 2), playerY + spriteSize + GROUND_Y_OFFSET,
                spriteSize + GROUND_WIDTH_PADDING, 56);
        g2d.fillOval(enemyX - (GROUND_WIDTH_PADDING / 2), enemyY + spriteSize + GROUND_Y_OFFSET,
                spriteSize + GROUND_WIDTH_PADDING, 56);

        int playerShakeY = getShakeOffset(playerHitTimer);
        int enemyShakeY = getShakeOffset(enemyHitTimer);
        drawBattleCharacter(g2d, getActivePlayerSprite(), playerX, playerY + playerShakeY, spriteSize, spriteSize,
                playerHitTimer > 0, new Color(255, 209, 102), true);
        drawBattleCharacter(g2d, enemyBattleSprite, enemyX, enemyY + enemyShakeY, spriteSize, spriteSize,
                enemyHitTimer > 0, new Color(220, 120, 155), false);

        g2d.setFont(UIFont.regular(12f));

        int panelWidth = 220;

        int enemyPanelH = 56;
        int enemyPanelX = 40;
        int enemyPanelY = 40;
        drawStatusPanel(g2d, enemyPanelX, enemyPanelY, panelWidth, enemyPanelH, enemyCreature, false);

        int playerPanelH = 74;
        int playerPanelX = screenWidth - panelWidth - 40;
        int playerPanelY = battleBottom - playerPanelH - 12;
        drawStatusPanel(g2d, playerPanelX, playerPanelY, panelWidth, playerPanelH, getActivePlayerCreature(), true);

        int commandBoxX = COMMAND_BOX_SIDE_MARGIN;
        int commandBoxW = screenWidth - COMMAND_BOX_SIDE_MARGIN * 2;
        g2d.setColor(new Color(0, 0, 0, 230));
        g2d.fillRoundRect(commandBoxX, commandBoxY, commandBoxW, COMMAND_BOX_HEIGHT, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(commandBoxX, commandBoxY, commandBoxW, COMMAND_BOX_HEIGHT, 10, 10);

        if (scenePhase == ScenePhase.INTRO) {
            drawCenteredBanner(g2d, screenWidth, screenHeight, message);
        } else {
            g2d.drawString(message, 32, commandBoxY + 24);
            if (playerMenuMode == PlayerMenuMode.SWITCH_SELECT) {
                drawSwitchButtons(g2d, commandBoxX, commandBoxY, commandBoxW);
            } else {
                drawActionButtons(g2d, commandBoxX, commandBoxY, commandBoxW);
            }
        }
    }

    private void runActionIndex(int actionIndex) {
        if (actionIndex >= 0 && actionIndex <= 2) {
            useMove(actionIndex);
            return;
        }
        if (actionIndex == 3) {
            catchAttempt();
            return;
        }
        if (actionIndex == 4) {
            switchBeast();
            return;
        }
        if (actionIndex == 5) {
            runAttempt();
        }
    }

    private void switchBeast() {
        playerMenuMode = PlayerMenuMode.SWITCH_SELECT;
        switchSelectionIndex = Math.max(0, Math.min(playerCreatures.length - 1, activePlayerIndex));
        message = "Select a beast to switch to (WASD/Arrows, ENTER confirm, ESC cancel).";
    }

    private void trySwitchToIndex(int index) {
        if (index < 0 || index >= playerCreatures.length) {
            return;
        }
        if (!ownedPlayerCreatures[index]) {
            message = "You do not own " + playerCreatures[index].getName() + ".";
            return;
        }
        if (playerCreatures[index].isFainted()) {
            message = playerCreatures[index].getName() + " has 0 HP and cannot battle!";
            return;
        }
        if (index == activePlayerIndex && !openingBeastChoiceRequired && !forceSwitchChoiceRequired) {
            message = playerCreatures[index].getName() + " is already active.";
            return;
        }

        activePlayerIndex = index;
        refreshActiveMoves();
        BattleCreature playerCreature = getActivePlayerCreature();
        playerCreature.recoverEnergy(playerCreature.getEnergyRegen());
        playerMenuMode = PlayerMenuMode.COMMAND;

        if (openingBeastChoiceRequired) {
            openingBeastChoiceRequired = false;
            message = "Sent out " + playerCreature.getName() + ". Your turn.";
            return;
        }

        if (forceSwitchChoiceRequired) {
            forceSwitchChoiceRequired = false;
            message = "Sent out " + playerCreature.getName() + ". Your turn.";
            return;
        }

        message = "Switched to " + playerCreature.getName() + ".";

        tickCooldowns();
        beginEnemyThinking();
    }

    private void useMove(int moveIndex) {
        BattleCreature playerCreature = getActivePlayerCreature();
        BattleMove move = playerMoves[moveIndex];

        if (playerMoveCooldowns[moveIndex] > 0) {
            message = move.getName() + " is cooling down! Turn skipped.";
            tickCooldowns();
            beginEnemyThinking();
            return;
        }
        if (playerCreature.getStatusEffect() == StatusEffect.FEAR) {
            message = "Fear stops your attack this turn.";
            playerCreature.tickStatus();
            tickCooldowns();
            beginEnemyThinking();
            return;
        }
        if (playerCreature.getStatusEffect() == StatusEffect.PARALYZE && random.nextDouble() < 0.5) {
            message = "Paralyzed! You could not move.";
            playerCreature.tickStatus();
            tickCooldowns();
            beginEnemyThinking();
            return;
        }

        if (!playerCreature.spendEnergy(move.getEnergyCost())) {
            message = "Not enough energy for " + move.getName() + ".";
            return;
        }

        tickCooldowns();
        playerMoveCooldowns[moveIndex] = move.getCooldownTurns();

        if (soundManager != null) {
            soundManager.playSkillSoundForBeast(playerCreature.getName());
        }

        int damage = computeDamage(playerCreature, enemyCreature, move.getPower(), move.getType(), move);
        enemyCreature.takeDamage(damage);
        enemyHitTimer = HIT_ANIM_DURATION_SECONDS;

        if (soundManager != null) {
            soundManager.playSkillSoundForBeast(playerCreature.getName());
        }

        double eff = typeEffectiveness(move.getType(), enemyCreature.getElement());
        String effText = "";
        if (eff > 1.0) {
            effText = " Super Effective!";
        } else if (eff < 1.0) {
            effText = " Not Very Effective.";
        }

        StringBuilder turnMsg = new StringBuilder("Used " + move.getName() + " for " + damage + " DMG." + effText);
        if (move.getInflictEffect() != StatusEffect.NONE && random.nextDouble() < move.getEffectChance()) {
            enemyCreature.setStatus(move.getInflictEffect(), 2);
            turnMsg.append(" ").append(move.getInflictEffect().name()).append(" applied.");
        }
        applySpecialMoveEffect(playerCreature, enemyCreature, move, turnMsg);
        message = turnMsg.toString();

        if (enemyCreature.isFainted()) {
            int expGain = expYieldForLevel(enemyCreature.getLevel());
            int coinGain = coinYieldForLevel(enemyCreature.getLevel());
            lastLeveledUpNames.clear();
            lastCoinsEarned = coinGain;

            for (int i = 0; i < playerCreatures.length; i++) {
                if (ownedPlayerCreatures[i] && playerCreatures[i] != null) {
                    if (playerCreatures[i].addExp(expGain)) {
                        lastLeveledUpNames.add(playerCreatures[i].getName());
                    }
                }
            }

            StringBuilder result = new StringBuilder("You won! +" + expGain + " EXP each, +" + coinGain + " coins.");
            if (!lastLeveledUpNames.isEmpty()) {
                result.append(" ");
                for (int li = 0; li < lastLeveledUpNames.size(); li++) {
                    if (li > 0) result.append(", ");
                    result.append(lastLeveledUpNames.get(li));
                }
                result.append(" leveled up!");
            }
            lastResolvedEnemyName = enemyCreature != null ? enemyCreature.getName() : "";
            lastBattleWon = true;
            enterResult(result.toString());
            return;
        }

        playerCreature.tickStatus();
        beginEnemyThinking();
    }

    private void runAttempt() {
        if (battleType == BattleType.PLAYER) {
            message = "You cannot run from trainer battles.";
            return;
        }
        int chance = TEMP_RUN_SUCCESS_RATE_PERCENT;
        if (random.nextInt(100) < chance) {
            lastResolvedEnemyName = enemyCreature != null ? enemyCreature.getName() : "";
            lastBattleWon = false;
            enterResult("You ran away safely.");
        } else {
            message = "Run failed!";
            tickCooldowns();
            beginEnemyThinking();
        }
    }

    private void catchAttempt() {
        if (battleType == BattleType.PLAYER) {
            message = "You cannot catch a trainer's Mecha Beast!";
            return;
        }
        if (enemyCreature.getHp() <= 0) {
            message = "Cannot catch at 0 HP.";
            return;
        }
        double maxHp = Math.max(1, enemyCreature.getMaxHp());
        double currentHp = Math.max(1, enemyCreature.getHp());
        double rawChance = (((3.0 * maxHp - 2.0 * currentHp) * 1.0) / (3.0 * maxHp)) * 0.75;
        int catchChance = (int) Math.round(Math.max(0.05, Math.min(0.95, rawChance)) * 100.0);
        if (random.nextInt(100) < catchChance) {
            pendingCaughtCreature = enemyCreature;
            lastResolvedEnemyName = enemyCreature.getName();
            lastBattleWon = false;
            enterResult("Caught " + enemyCreature.getName() + "!");
        } else {
            message = "Catch failed! (" + catchChance + "%)";
            tickCooldowns();
            beginEnemyThinking();
        }
    }

    private void enemyTurn() {
        BattleCreature playerCreature = getActivePlayerCreature();
        if (enemyCreature.getStatusEffect() == StatusEffect.FEAR) {
            enemyCreature.tickStatus();
            turnPhase = TurnPhase.PLAYER_THINKING;
            thinkTimer = THINK_DURATION_SECONDS;
            message = "Enemy is afraid and skips the turn.";
            return;
        }
        if (enemyCreature.getStatusEffect() == StatusEffect.PARALYZE && random.nextDouble() < 0.5) {
            enemyCreature.tickStatus();
            turnPhase = TurnPhase.PLAYER_THINKING;
            thinkTimer = THINK_DURATION_SECONDS;
            message = "Enemy is paralyzed and cannot attack.";
            return;
        }

        BattleMove[] enemyMoves = BeastCatalog.movesFor(enemyCreature.getName());
        BattleMove enemyMove = enemyMoves[random.nextInt(enemyMoves.length)];
        if (soundManager != null) {
            soundManager.playSkillSoundForBeast(enemyCreature.getName());
        }
        int damage = computeDamage(enemyCreature, playerCreature, enemyMove.getPower(), enemyMove.getType(), enemyMove);
        playerCreature.takeDamage(damage);
        playerHitTimer = HIT_ANIM_DURATION_SECONDS;
        if (soundManager != null) {
            soundManager.playSkillSoundForBeast(enemyCreature.getName());
        }

        if (enemyMove.getInflictEffect() != StatusEffect.NONE && random.nextDouble() < enemyMove.getEffectChance()) {
            playerCreature.setStatus(enemyMove.getInflictEffect(), 2);
        }

        applyEndTurnStatusDamage(playerCreature);
        applyEndTurnStatusDamage(enemyCreature);
        playerCreature.tickStatus();
        enemyCreature.tickStatus();

        if (playerCreature.isFainted()) {
            boolean hasAlive = false;
            for (int i = 0; i < playerCreatures.length; i++) {
                if (ownedPlayerCreatures[i] && !playerCreatures[i].isFainted()) {
                    hasAlive = true;
                    break;
                }
            }
            if (hasAlive) {
                message = playerCreature.getName() + " fainted! Choose another beast.";
                playerMenuMode = PlayerMenuMode.SWITCH_SELECT;

                for (int i = 0; i < playerCreatures.length; i++) {
                    if (ownedPlayerCreatures[i] && !playerCreatures[i].isFainted()) {
                        switchSelectionIndex = i;
                        break;
                    }
                }

                turnPhase = TurnPhase.PLAYER_TURN;
                forceSwitchChoiceRequired = true;
            } else {
                lastResolvedEnemyName = enemyCreature != null ? enemyCreature.getName() : "";
                lastBattleWon = false;
                enterResult("All your beasts fainted. You lost.");
            }
        } else {
            double eff = typeEffectiveness(enemyMove.getType(), playerCreature.getElement());
            String effText = "";
            if (eff > 1.0) {
                effText = " Super Effective!";
            } else if (eff < 1.0) {
                effText = " Not Very Effective.";
            }
            message = "Enemy used " + enemyMove.getName() + " for " + damage + " DMG." + effText;
            turnPhase = TurnPhase.PLAYER_THINKING;
            thinkTimer = THINK_DURATION_SECONDS;
        }
    }

    private void beginEnemyThinking() {
        turnPhase = TurnPhase.ENEMY_THINKING;
        thinkTimer = THINK_DURATION_SECONDS;
    }

    private void tickCooldowns() {
        for (int i = 0; i < playerMoveCooldowns.length; i++) {
            if (playerMoveCooldowns[i] > 0) {
                playerMoveCooldowns[i]--;
            }
        }
    }

    private void applyEndTurnStatusDamage(BattleCreature creature) {
        if (creature.getStatusEffect() == StatusEffect.BURN) {
            int burn = Math.max(1, (int) Math.round(creature.getMaxHp() * 0.50));
            creature.takeDamage(burn);
        }
    }

    private int computeDamage(BattleCreature attacker, BattleCreature defender, int movePower, BeastElement attackType,
                              BattleMove move) {
        if (attacker != null && attacker.isAllMighty()) {
            return Math.max(1, defender.getHp());
        }
        double levelPart = ((2.0 * attacker.getLevel() / 5.0) + 2.0);
        double core = ((levelPart * movePower * (attacker.getAttack() / (double) Math.max(1, defender.getDefense())))
                / 50.0) + 2.0;

        double stab = 1.0;
        if (attackType != null && attacker.getElement() != null) {
            if (!attackType.name().toUpperCase().equals("NEUTRAL") && attackType.name().equals(attacker.getElement().name())) {
                stab = 1.5;
            }
        }

        double typeEffect = typeEffectiveness(attackType, defender.getElement());
        double critRate = 0.25;
        if (move != null && move.getMoveEffect() == MoveEffect.HIGH_CRIT) {
            critRate = Math.min(1.0, critRate + move.getEffectValue());
        }
        double crit = random.nextDouble() < critRate ? 1.5 : 1.0;
        double randomFactor = 0.85 + random.nextDouble() * 0.15;
        int dmg = (int) Math.round(core * stab * typeEffect * crit * randomFactor);
        return Math.max(2, dmg);
    }

    private void drawStatusPanel(Graphics2D g2d, int x, int y, int width, int height, BattleCreature creature, boolean showNumericHp) {
        g2d.setColor(new Color(35, 40, 45, 240));
        g2d.fillRoundRect(x, y, width, height, 12, 12);
        g2d.setColor(new Color(60, 65, 75));
        g2d.drawRoundRect(x, y, width, height, 12, 12);
        g2d.setColor(new Color(235, 235, 235));
        g2d.drawRoundRect(x + 2, y + 2, width - 4, height - 4, 10, 10);

        g2d.setColor(Color.WHITE);
        g2d.drawString(creature.getName(), x + 14, y + 22);

        String lvText = "Lv" + creature.getLevel();
        int lvWidth = g2d.getFontMetrics().stringWidth(lvText);
        g2d.drawString(lvText, x + width - 14 - lvWidth, y + 22);

        g2d.setColor(new Color(240, 190, 80));
        g2d.drawString("HP", x + 14, y + 38);
        int barX = x + 38;
        int barW = width - 52;
        drawBar(g2d, barX, y + 30, barW, 6, creature.getHp() / (double) creature.getMaxHp(), getHpBarColor(creature.getHp() / (double) creature.getMaxHp()));

        g2d.setColor(new Color(110, 180, 255));
        g2d.drawString("EN", x + 14, y + 48);
        drawBar(g2d, barX, y + 42, barW, 4, creature.getEnergy() / (double) creature.getMaxEnergy(), new Color(94, 164, 255));

        if (showNumericHp) {
            g2d.setColor(Color.WHITE);
            String hpText = creature.getHp() + " / " + creature.getMaxHp();
            int textWidth = g2d.getFontMetrics().stringWidth(hpText);
            g2d.drawString(hpText, x + width - 14 - textWidth, y + 64);
        }

        if (creature.getStatusEffect() != StatusEffect.NONE) {
            g2d.setColor(new Color(230, 80, 80));
            String status = creature.getStatusEffect().name();
            if(status.length() > 3) {
                status = status.substring(0, 3);
            }
            int statusWidth = g2d.getFontMetrics().stringWidth(status);
            g2d.drawString(status, x + width - 14 - lvWidth - statusWidth - 8, y + 22);
        }
    }

    private void drawBar(Graphics2D g2d, int x, int y, int w, int h, double ratio, Color fillColor) {
        g2d.setColor(new Color(45, 45, 55));
        g2d.fillRect(x - 1, y - 1, w + 2, h + 2);

        g2d.setColor(new Color(85, 90, 95));
        g2d.fillRect(x, y, w, h);

        int fillW = (int) Math.round(w * clamp01(ratio));
        g2d.setColor(fillColor);
        g2d.fillRect(x, y, Math.max(0, fillW), h);

        if(fillW > 0) {
            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.fillRect(x, y, fillW, 1);
        }
    }

    private Color getHpBarColor(double hpRatio) {
        if (hpRatio > 0.5)
            return new Color(74, 214, 95);
        if (hpRatio > 0.2)
            return new Color(245, 204, 77);
        return new Color(224, 72, 72);
    }

    private void drawActionButtons(Graphics2D g2d, int commandBoxX, int commandBoxY, int commandBoxW) {
        int btnY = commandBoxY + 40;
        int gap = 8;
        int btnW = (commandBoxW - 24 - gap * 2) / 3;
        int btnH = 46;
        int startX = commandBoxX + 12;
        String[] labels = {
                "1 " + playerMoves[0].getName(),
                "2 " + playerMoves[1].getName(),
                "3 " + playerMoves[2].getName(),
                "4 Catch", "5 Switch", "6 Run"
        };
        for (int i = 0; i < 6; i++) {
            int row = i / 3;
            int col = i % 3;
            int bx = startX + col * (btnW + gap);
            int by = btnY + row * (btnH + 8);
            actionButtons[i] = new Rectangle(bx, by, btnW, btnH);
            g2d.setColor(new Color(26, 30, 42, 220));
            g2d.fillRoundRect(bx, by, btnW, btnH, 8, 8);
            g2d.setColor(new Color(215, 215, 220));
            g2d.drawRoundRect(bx, by, btnW, btnH, 8, 8);
            g2d.setColor(Color.WHITE);
            g2d.drawString(labels[i], bx + 8, by + 15);

            if (i <= 2) {
                BattleMove move = playerMoves[i];
                g2d.drawString("EN " + move.getEnergyCost() + " CD " + playerMoveCooldowns[i], bx + 8, by + 27);
                g2d.drawString(getDamageEstimateText(move), bx + 8, by + 40);
            } else if (i == 3) {
                g2d.drawString("Beast Card", bx + 8, by + 29);
            } else if (i == 4) {
                g2d.drawString("Choose beast", bx + 8, by + 29);
            } else {
                String runHint = battleType == BattleType.PLAYER ? "No PvP escape" : "Escape";
                g2d.drawString(runHint, bx + 8, by + 29);
            }
        }
    }

    private void drawSwitchButtons(Graphics2D g2d, int commandBoxX, int commandBoxY, int commandBoxW) {
        int btnY = commandBoxY + 40;
        int gap = 8;
        int btnW = (commandBoxW - 24 - gap * 2) / 3;
        int btnH = 96;
        int startX = commandBoxX + 12;
        for (int i = 0; i < playerCreatures.length; i++) {
            int bx = startX + i * (btnW + gap);
            int by = btnY;
            switchButtons[i] = new Rectangle(bx, by, btnW, btnH);
            BattleCreature creature = playerCreatures[i];
            boolean isActive = i == activePlayerIndex;
            boolean isCursor = i == switchSelectionIndex;
            boolean owned = ownedPlayerCreatures[i];
            boolean isFainted = creature.isFainted();

            g2d.setColor(isActive ? new Color(54, 82, 120, 230) : new Color(26, 30, 42, 220));
            g2d.fillRoundRect(bx, by, btnW, btnH, 8, 8);
            g2d.setColor(new Color(215, 215, 220));
            g2d.drawRoundRect(bx, by, btnW, btnH, 8, 8);
            if (isCursor) {
                g2d.setColor(new Color(120, 180, 255));
                g2d.drawRoundRect(bx + 1, by + 1, btnW - 2, btnH - 2, 8, 8);
            }
            g2d.setColor(owned ? Color.WHITE : new Color(155, 155, 155));
            g2d.drawString((i + 1) + " " + creature.getName(), bx + 8, by + 18);
            g2d.drawString("HP " + creature.getHp() + "/" + creature.getMaxHp(), bx + 8, by + 38);
            g2d.drawString("EN " + creature.getEnergy() + "/" + creature.getMaxEnergy(), bx + 8, by + 54);
            if (!owned) {
                g2d.drawString("LOCKED", bx + 8, by + 72);
            } else if (isFainted) {
                g2d.setColor(new Color(224, 72, 72));
                g2d.drawString("FAINTED", bx + 8, by + 72);
            } else if (isActive) {
                g2d.drawString("ACTIVE", bx + 8, by + 72);
            } else {
                g2d.drawString("Switch", bx + 8, by + 72);
            }
        }
    }

    private void drawBattleCharacter(Graphics2D g2d, BufferedImage sprite, int x, int y, int w, int h, boolean isHit,
                                     Color fallbackColor, boolean isPlayer) {
        if (sprite != null) {
            g2d.drawImage(sprite, x, y, w, h, null);
        } else {
            g2d.setColor(fallbackColor);
            g2d.fillOval(x, y, w, h);
            g2d.setColor(new Color(255, 255, 255, 70));
            g2d.drawOval(x, y, w, h);
        }
        if (isHit) {
            BufferedImage hit = isPlayer ? getActivePlayerSpriteHit() : enemyBattleSpriteHit;
            if (hit != null) {
                g2d.drawImage(hit, x, y, w, h, null);
            }
        }
    }

    private BattleCreature getActivePlayerCreature() {
        return playerCreatures[activePlayerIndex];
    }

    private BufferedImage getActivePlayerSprite() {
        BattleCreature activeCreature = getActivePlayerCreature();
        BufferedImage beastSprite = getBattleSpriteForCreature(activeCreature.getName(), true);
        if (beastSprite != null) {
            return beastSprite;
        }
        return playerBattleSprites[activePlayerIndex];
    }

    private BufferedImage getActivePlayerSpriteHit() {
        BattleCreature activeCreature = getActivePlayerCreature();
        BufferedImage beastHit = getHitSpriteForCreature(activeCreature.getName(), true);
        if (beastHit != null) {
            return beastHit;
        }
        return playerBattleSpritesHit[activePlayerIndex];
    }

    private BufferedImage getBattleSpriteForCreature(String creatureName, boolean isPlayer) {
        String key = toAssetKey(creatureName);
        if (key.isEmpty()) {
            return null;
        }
        String cacheKey = key + (isPlayer ? "_back" : "_front");
        if (beastSpriteCache.containsKey(cacheKey)) {
            return beastSpriteCache.get(cacheKey);
        }
        BufferedImage sprite = loadBeastSprite(key, isPlayer);
        beastSpriteCache.put(cacheKey, sprite);
        return sprite;
    }

    private BufferedImage getHitSpriteForCreature(String creatureName, boolean isPlayer) {
        String key = toAssetKey(creatureName);
        if (key.isEmpty()) {
            return null;
        }
        String cacheKey = key + (isPlayer ? "_back_hit" : "_front_hit");
        if (beastSpriteHitCache.containsKey(cacheKey)) {
            return beastSpriteHitCache.get(cacheKey);
        }
        BufferedImage base = getBattleSpriteForCreature(creatureName, isPlayer);
        BufferedImage hit = createRedTintedSprite(base);
        beastSpriteHitCache.put(cacheKey, hit);
        return hit;
    }

    private int getShakeOffset(double timer) {
        if (timer <= 0)
            return 0;
        return (timer * 50 % 2 < 1) ? -4 : 4;
    }

    private void enterResult(String resultMessage) {
        scenePhase = ScenePhase.OUTRO;
        outroTimer = OUTRO_DURATION_SECONDS;
        pendingResultMessage = resultMessage;
        message = "";

        // Auto-heal all player's mecha beasts to full at the end of the battle
        if (playerCreatures != null) {
            for (BattleCreature creature : playerCreatures) {
                if (creature != null) {
                    creature.healToFull();
                }
            }
        }
    }

    private void drawCenteredBanner(Graphics2D g2d, int screenWidth, int screenHeight, String text) {
        Font prev = g2d.getFont();
        g2d.setFont(UIFont.bold(18f));
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int boxW = Math.min(screenWidth - 40, textWidth + 48);
        int boxH = 58;
        int x = (screenWidth - boxW) / 2;
        int y = (screenHeight - boxH) / 2;
        g2d.setColor(new Color(0, 0, 0, 210));
        g2d.fillRoundRect(x, y, boxW, boxH, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(x, y, boxW, boxH, 10, 10);
        g2d.drawString(text, x + (boxW - textWidth) / 2, y + 36);
        g2d.setFont(prev);
    }

    private void drawBattleBackground(Graphics2D g2d, int screenWidth, int screenHeight) {
        if (battleBackground == null) {
            g2d.setColor(new Color(22, 24, 37));
            g2d.fillRect(0, 0, screenWidth, screenHeight);
            return;
        }
        if (blurredBattleBackground == null || blurredBattleBackground.getWidth() != screenWidth
                || blurredBattleBackground.getHeight() != screenHeight) {
            blurredBattleBackground = buildBlurredBackground(battleBackground, screenWidth, screenHeight);
        }
        g2d.drawImage(blurredBattleBackground, 0, 0, null);
    }

    private BufferedImage buildBlurredBackground(BufferedImage source, int width, int height) {
        BufferedImage base = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = base.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        BufferedImage out = applyBoxBlur(base, 1);
        Graphics2D gd = out.createGraphics();
        gd.setColor(new Color(0, 0, 0, 70));
        gd.fillRect(0, 0, width, height);
        gd.dispose();
        return out;
    }

    private BufferedImage applyBoxBlur(BufferedImage src, int radius) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = 0, r = 0, g = 0, b = 0, c = 0;
                for (int oy = -radius; oy <= radius; oy++) {
                    int py = clamp(y + oy, 0, h - 1);
                    for (int ox = -radius; ox <= radius; ox++) {
                        int px = clamp(x + ox, 0, w - 1);
                        int argb = src.getRGB(px, py);
                        a += (argb >>> 24) & 0xFF;
                        r += (argb >>> 16) & 0xFF;
                        g += (argb >>> 8) & 0xFF;
                        b += argb & 0xFF;
                        c++;
                    }
                }
                out.setRGB(x, y, ((a / c) << 24) | ((r / c) << 16) | ((g / c) << 8) | (b / c));
            }
        }
        return out;
    }

    private BattleCreature createWildByName(String name) {
        return BeastCatalog.createCreature(name);
    }

    private String getDamageEstimateText(BattleMove move) {
        if (move == null || enemyCreature == null) {
            return "DMG: 0-0";
        }
        BattleCreature attacker = getActivePlayerCreature();
        double levelPart = ((2.0 * attacker.getLevel() / 5.0) + 2.0);
        double core = ((levelPart * move.getPower()
                * (attacker.getAttack() / (double) Math.max(1, enemyCreature.getDefense()))) / 50.0) + 2.0;

        double stab = 1.0;
        if (move.getType() != null && attacker.getElement() != null) {
            if (!move.getType().name().toUpperCase().equals("NEUTRAL") && move.getType().name().equals(attacker.getElement().name())) {
                stab = 1.5;
            }
        }

        double eff = typeEffectiveness(move.getType(), enemyCreature.getElement());

        int minDamage = Math.max(2, (int) Math.round(core * stab * eff * 0.85));
        int maxDamage = Math.max(2, (int) Math.round(core * stab * eff * 1.0));

        String effMarker = "";
        if (eff > 1.0) effMarker = " (SE)";
        if (eff < 1.0) effMarker = " (NVE)";

        return "DMG: " + minDamage + "-" + maxDamage + effMarker;
    }

    private void refreshActiveMoves() {
        BattleCreature creature = getActivePlayerCreature();
        playerMoves = BeastCatalog.movesFor(creature.getName());
        playerMoveCooldowns = new int[playerMoves.length];
    }

    private int expYieldForLevel(int level) {
        return switch (Math.max(1, Math.min(20, level))) {
            case 1 -> 5;
            case 2 -> 8;
            case 3 -> 15;
            case 4 -> 20;
            case 5 -> 25;
            case 6 -> 30;
            case 7 -> 35;
            case 8 -> 45;
            case 9 -> 50;
            case 10 -> 55;
            case 11 -> 60;
            case 12 -> 70;
            case 13 -> 80;
            case 14 -> 90;
            case 15 -> 110;
            case 16 -> 130;
            case 17 -> 150;
            case 18 -> 170;
            case 19 -> 190;
            default -> 5000;
        };
    }

    private int coinYieldForLevel(int level) {
        return expYieldForLevel(level) * 2;
    }

    private void applySpecialMoveEffect(BattleCreature attacker, BattleCreature defender, BattleMove move,
                                        StringBuilder turnMsg) {
        switch (move.getMoveEffect()) {
            case HEAL_SELF_PERCENT_MAX_HP -> {
                int heal = (int) Math.round(attacker.getMaxHp() * move.getEffectValue());
                int healed = attacker.heal(heal);
                turnMsg.append(" Healed ").append(Math.max(1, healed)).append(" HP.");
            }
            case LOWER_TARGET_ATTACK_PERCENT -> {
                defender.lowerAttack(move.getEffectValue(), Math.max(1, move.getEffectTurns()));
                turnMsg.append(" Target ATK down.");
            }
            case LOWER_TARGET_DEFENSE_PERCENT -> {
                defender.lowerDefense(move.getEffectValue(), Math.max(1, move.getEffectTurns()));
                turnMsg.append(" Target DEF down.");
            }
            case AFTERSHOCK_PERCENT_TARGET_MAX_HP -> {
                if (random.nextDouble() <= move.getEffectChance()) {
                    int bonus = (int) Math.round(defender.getMaxHp() * move.getEffectValue());
                    defender.takeDamage(Math.max(1, bonus));
                    turnMsg.append(" Aftershock +").append(Math.max(1, bonus)).append(".");
                }
            }
            case EXTRA_HITS -> {
                for (int i = 0; i < move.getExtraHits(); i++) {
                    int hit = computeDamage(attacker, defender,
                            Math.max(1, (int) Math.round(move.getPower() * move.getExtraHitPowerRatio())),
                            move.getType(), null);
                    defender.takeDamage(hit);
                    turnMsg.append(" Extra hit ").append(i + 1).append(" +").append(hit).append(".");
                    if (defender.isFainted()) {
                        break;
                    }
                }
            }
            case EXTRA_HITS_CHANCE -> {
                if (random.nextDouble() <= move.getEffectValue()) {
                    int hit = computeDamage(attacker, defender,
                            Math.max(1, (int) Math.round(move.getPower() * move.getExtraHitPowerRatio())),
                            move.getType(), null);
                    defender.takeDamage(hit);
                    turnMsg.append(" Follow-up hit +").append(hit).append(".");
                }
            }
            default -> {
            }
        }
    }

    private double typeEffectiveness(BeastElement moveType, BeastElement defenderType) {
        if (moveType == null || defenderType == null) return 1.0;
        String mType = moveType.name().toUpperCase();
        String dType = defenderType.name().toUpperCase();

        if (mType.equals("NEUTRAL") || dType.equals("NEUTRAL")) return 1.0;

        if (mType.equals("FIRE") && (dType.equals("GRASS") || dType.equals("WIND") || dType.equals("STEEL"))) return 2.0;
        if (mType.equals("FIRE") && (dType.equals("WATER") || dType.equals("EARTH"))) return 0.5;

        if (mType.equals("WATER") && (dType.equals("FIRE") || dType.equals("EARTH"))) return 2.0;
        if (mType.equals("WATER") && (dType.equals("GRASS") || dType.equals("ELECTRIC"))) return 0.5;

        if (mType.equals("GRASS") && (dType.equals("WATER") || dType.equals("EARTH"))) return 2.0;
        if (mType.equals("GRASS") && (dType.equals("FIRE") || dType.equals("WIND"))) return 0.5;

        if (mType.equals("ELECTRIC") && (dType.equals("WATER") || dType.equals("WIND"))) return 2.0;
        if (mType.equals("ELECTRIC") && (dType.equals("EARTH") || dType.equals("STEEL"))) return 0.5;

        if (mType.equals("EARTH") && (dType.equals("ELECTRIC") || dType.equals("STEEL") || dType.equals("FIRE"))) return 2.0;
        if (mType.equals("EARTH") && (dType.equals("WATER") || dType.equals("GRASS") || dType.equals("DARK"))) return 0.5;

        if (mType.equals("WIND") && (dType.equals("GRASS") || dType.equals("FIGHTING"))) return 2.0;
        if (mType.equals("WIND") && (dType.equals("FIRE") || dType.equals("ELECTRIC"))) return 0.5;

        if (mType.equals("FIGHTING") && (dType.equals("DARK") || dType.equals("STEEL"))) return 2.0;
        if (mType.equals("FIGHTING") && (dType.equals("WIND") || dType.equals("PSYCHIC"))) return 0.5;

        if (mType.equals("DARK") && (dType.equals("EARTH") || dType.equals("WIND"))) return 2.0;
        if (mType.equals("DARK") && (dType.equals("FIGHTING") || dType.equals("STEEL"))) return 0.5;

        if (mType.equals("STEEL") && (dType.equals("DARK") || dType.equals("ELECTRIC"))) return 2.0;
        if (mType.equals("STEEL") && (dType.equals("FIRE") || dType.equals("EARTH"))) return 0.5;

        return 1.0;
    }

    private static BufferedImage loadBeastSprite(String key, boolean isPlayer) {
        String base = "res/beasts/" + key + "/" + key;

        if (isPlayer) {
            BufferedImage sprite = loadSprite(base + "-b.png");
            if (sprite != null) {
                return sprite;
            }
        }

        String[] candidates = new String[] {
                base + "-f.png",
                base + "-fw.png",
                base + "-b.png"
        };

        for (String path : candidates) {
            BufferedImage sprite = loadSprite(path);
            if (sprite != null) {
                return sprite;
            }
        }
        return null;
    }

    private static String toAssetKey(String creatureName) {
        if (creatureName == null || creatureName.isBlank()) {
            return "";
        }
        return creatureName.toLowerCase().replace(" ", "");
    }

    private static BufferedImage loadSprite(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            return null;
        }
    }

    private static BufferedImage createRedTintedSprite(BufferedImage base) {
        if (base == null)
            return null;
        BufferedImage tinted = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                int argb = base.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) {
                    tinted.setRGB(x, y, 0);
                } else {
                    int outA = Math.min(255, (int) (alpha * 0.65));
                    tinted.setRGB(x, y, (outA << 24) | (255 << 16) | (35 << 8) | 35);
                }
            }
        }
        return tinted;
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public void setVisible(boolean b) {
        throw new UnsupportedOperationException("Unimplemented method 'setVisible'");
    }
}