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
    private enum ScenePhase { INTRO, COMBAT, OUTRO, RESULT }
    private enum TurnPhase { PLAYER_TURN, ENEMY_THINKING, PLAYER_THINKING }
    private enum PlayerMenuMode { COMMAND, SWITCH_SELECT }

    private static final double THINK_DURATION_SECONDS = 2.0;
    private static final double INTRO_DURATION_SECONDS = 1.2;
    private static final double OUTRO_DURATION_SECONDS = 0.75;
    private static final double RESULT_DURATION_SECONDS = 1.8;
    private static final double HIT_ANIM_DURATION_SECONDS = 0.35;
    private static final int TEMP_RUN_SUCCESS_RATE_PERCENT = 100; // TEMP: set back to formula-based chance later

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
    private BattleCreature[] playerCreatures;
    private BufferedImage[] playerBattleSprites;
    private BufferedImage[] playerBattleSpritesHit;
    private int activePlayerIndex;
    private BattleCreature enemyCreature;
    private BattleType battleType;
    private final BattleMove[] playerMoves;
    private final int[] playerMoveCooldowns;

    private boolean active;
    private String message;
    private String pendingResultMessage;
    private String pendingCaughtCreatureName;
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
    private boolean[] ownedPlayerCreatures;
    private boolean openingBeastChoiceRequired;

    private BufferedImage enemyBattleSprite;
    private BufferedImage enemyBattleSpriteHit;
    private final BufferedImage battleBackground;
    private BufferedImage blurredBattleBackground;
    private final Map<String, BufferedImage> beastSpriteCache;
    private final Map<String, BufferedImage> beastSpriteHitCache;

    public BattleSystem() {
        playerMoves = new BattleMove[]{
                new BattleMove("Flare Slash", 45, 5, 1, StatusEffect.BURN, 0.25, BattleType.WILD),
                new BattleMove("Shock Drive", 38, 4, 0, StatusEffect.PARALYZE, 0.2, BattleType.WILD),
                new BattleMove("Fear Pulse", 30, 6, 2, StatusEffect.FEAR, 0.35, BattleType.WILD)
        };
        playerMoveCooldowns = new int[playerMoves.length];
        beastSpriteCache = new HashMap<>();
        beastSpriteHitCache = new HashMap<>();

        playerCreatures = new BattleCreature[0];
        playerBattleSprites = new BufferedImage[0];
        playerBattleSpritesHit = new BufferedImage[0];
        setPlayerParty(new String[]{"Nokami", "Vineratops", "Kyoflare"});
        activePlayerIndex = 0;

        enemyBattleSprite = null;
        enemyBattleSpriteHit = null;
        battleBackground = loadSprite("res/scenes/hometown-battlescene.png");
        actionButtons = new Rectangle[6];
        switchButtons = new Rectangle[playerCreatures.length];
        playerMenuMode = PlayerMenuMode.COMMAND;
        ownedPlayerCreatures = new boolean[playerCreatures.length];
        for (int i = 0; i < ownedPlayerCreatures.length; i++) {
            ownedPlayerCreatures[i] = true;
        }
        openingBeastChoiceRequired = false;
    }

    public void startWildBattle(String enemyBeastName) {
        enemyCreature = createWildByName(enemyBeastName);
        enemyBattleSprite = getBattleSpriteForCreature(enemyCreature.getName());
        enemyBattleSpriteHit = getHitSpriteForCreature(enemyCreature.getName());
        battleType = BattleType.WILD;
        for (int i = 0; i < playerMoveCooldowns.length; i++) {
            playerMoveCooldowns[i] = 0;
        }
        for (BattleCreature creature : playerCreatures) {
            creature.healToFull();
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
        pendingCaughtCreatureName = null;
        openingBeastChoiceRequired = true;
        playerMenuMode = PlayerMenuMode.SWITCH_SELECT;
        message = "Choose your beast for this battle.";
    }

    public String[] getStarterChoices() {
        return BeastCatalog.starterNames();
    }

    public BufferedImage getCreatureSprite(String creatureName) {
        return getBattleSpriteForCreature(creatureName);
    }

    public void setStarterBeast(String beastName) {
        if (beastName == null) {
            return;
        }
        for (int i = 0; i < playerCreatures.length; i++) {
            if (playerCreatures[i].getName().equalsIgnoreCase(beastName.trim())) {
                activePlayerIndex = i;
                return;
            }
        }
    }

    public void setPlayerParty(String[] beastNames) {
        if (beastNames == null || beastNames.length == 0) {
            return;
        }
        int partySize = Math.min(3, beastNames.length);
        BattleCreature[] newCreatures = new BattleCreature[partySize];
        BufferedImage[] newSprites = new BufferedImage[partySize];
        BufferedImage[] newHitSprites = new BufferedImage[partySize];
        for (int i = 0; i < partySize; i++) {
            String name = beastNames[i] == null || beastNames[i].isBlank() ? "Nokami" : beastNames[i].trim();
            newCreatures[i] = BeastCatalog.createCreature(name);
            newSprites[i] = getBattleSpriteForCreature(name);
            newHitSprites[i] = createRedTintedSprite(newSprites[i]);
        }
        playerCreatures = newCreatures;
        playerBattleSprites = newSprites;
        playerBattleSpritesHit = newHitSprites;
        activePlayerIndex = 0;
        ownedPlayerCreatures = new boolean[partySize];
        Arrays.fill(ownedPlayerCreatures, true);
        switchButtons = new Rectangle[partySize];
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

    public String consumeCaughtCreatureName() {
        String caught = pendingCaughtCreatureName;
        pendingCaughtCreatureName = null;
        return caught;
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
            if (input.consumeJustPressed(KeyEvent.VK_ESCAPE)) {
                playerMenuMode = PlayerMenuMode.COMMAND;
                message = "Choose an action.";
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
                    getActivePlayerCreature().recoverEnergy(2);
                    message = "Your turn. Choose an action.";
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
        g2d.fillOval(playerX - (GROUND_WIDTH_PADDING / 2), playerY + spriteSize + GROUND_Y_OFFSET, spriteSize + GROUND_WIDTH_PADDING, 56);
        g2d.fillOval(enemyX - (GROUND_WIDTH_PADDING / 2), enemyY + spriteSize + GROUND_Y_OFFSET, spriteSize + GROUND_WIDTH_PADDING, 56);

        int playerShakeY = getShakeOffset(playerHitTimer);
        int enemyShakeY = getShakeOffset(enemyHitTimer);
        drawBattleCharacter(g2d, getActivePlayerSprite(), playerX, playerY + playerShakeY, spriteSize, spriteSize, playerHitTimer > 0, new Color(255, 209, 102));
        drawBattleCharacter(g2d, enemyBattleSprite, enemyX, enemyY + enemyShakeY, spriteSize, spriteSize, enemyHitTimer > 0, new Color(220, 120, 155));

        g2d.setFont(UIFont.regular(12f));
        int panelWidth = Math.max(260, Math.min(360, screenWidth / 4));
        int panelHeight = 92;
        int enemyPanelX = clamp(enemyX + (spriteSize - panelWidth) / 2, 24, screenWidth - panelWidth - 24);
        int enemyPanelY = Math.max(24, enemyY - panelHeight - 12);
        drawStatusPanel(g2d, enemyPanelX, enemyPanelY, panelWidth, panelHeight, enemyCreature, true);

        int playerPanelX = clamp(playerX + (spriteSize - panelWidth) / 2, 24, screenWidth - panelWidth - 24);
        int playerPanelY = Math.min(battleBottom - panelHeight, playerY + spriteSize + 18);
        drawStatusPanel(g2d, playerPanelX, playerPanelY, panelWidth, panelHeight, getActivePlayerCreature(), true);

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
        message = "Select a beast to switch to (1-3, ESC cancel).";
    }

    private void trySwitchToIndex(int index) {
        if (index < 0 || index >= playerCreatures.length) {
            return;
        }
        if (!ownedPlayerCreatures[index]) {
            message = "You do not own " + playerCreatures[index].getName() + ".";
            return;
        }
        if (index == activePlayerIndex) {
            if (openingBeastChoiceRequired) {
                openingBeastChoiceRequired = false;
                playerMenuMode = PlayerMenuMode.COMMAND;
                message = "Your turn. Choose an action.";
                return;
            }
            message = playerCreatures[index].getName() + " is already active.";
            return;
        }
        activePlayerIndex = index;
        BattleCreature playerCreature = getActivePlayerCreature();
        playerCreature.recoverEnergy(4);
        playerMenuMode = PlayerMenuMode.COMMAND;
        if (openingBeastChoiceRequired) {
            openingBeastChoiceRequired = false;
            message = "Sent out " + playerCreature.getName() + ". Your turn.";
            return;
        }
        message = "Switched to " + playerCreature.getName() + ".";
        beginEnemyThinking();
    }

    private void useMove(int moveIndex) {
        BattleCreature playerCreature = getActivePlayerCreature();
        BattleMove move = playerMoves[moveIndex];
        if (playerMoveCooldowns[moveIndex] > 0) {
            message = move.getName() + " is cooling down (" + playerMoveCooldowns[moveIndex] + " turn).";
            return;
        }
        if (!playerCreature.spendEnergy(move.getEnergyCost())) {
            message = "Not enough energy for " + move.getName() + ".";
            return;
        }
        if (playerCreature.getStatusEffect() == StatusEffect.FEAR) {
            message = "Fear stops your attack this turn.";
            playerCreature.tickStatus();
            beginEnemyThinking();
            return;
        }
        if (playerCreature.getStatusEffect() == StatusEffect.PARALYZE && random.nextDouble() < 0.3) {
            message = "Paralyzed! You could not move.";
            playerCreature.tickStatus();
            beginEnemyThinking();
            return;
        }

        int damage = computeDamage(playerCreature, enemyCreature, move.getPower(), move.getType());
        enemyCreature.takeDamage(damage);
        enemyHitTimer = HIT_ANIM_DURATION_SECONDS;
        playerMoveCooldowns[moveIndex] = move.getCooldownTurns();

        StringBuilder turnMsg = new StringBuilder("Used " + move.getName() + " for " + damage + " damage.");
        if (move.getInflictEffect() != StatusEffect.NONE && random.nextDouble() < move.getEffectChance()) {
            enemyCreature.setStatus(move.getInflictEffect(), 2);
            turnMsg.append(" ").append(move.getInflictEffect().name()).append(" applied.");
        }
        if (move.getInflictEffect() == StatusEffect.NONE && random.nextDouble() < 0.2) {
            int aftershock = 3 + random.nextInt(4);
            enemyCreature.takeDamage(aftershock);
            turnMsg.append(" Aftershock +").append(aftershock).append(".");
        }
        message = turnMsg.toString();

        if (enemyCreature.isFainted()) {
            int expGain = 18 + enemyCreature.getLevel() * 6;
            boolean leveled = playerCreature.addExp(expGain);
            String result = "You won the fight! +" + expGain + " EXP.";
            if (leveled) {
                result += " Level up! Lv " + playerCreature.getLevel() + ".";
            }
            enterResult(result);
            return;
        }
        playerCreature.tickStatus();
        beginEnemyThinking();
    }

    private void runAttempt() {
        BattleCreature playerCreature = getActivePlayerCreature();
        if (battleType == BattleType.PLAYER) {
            message = "You cannot run from player battles.";
            return;
        }
        int chance = TEMP_RUN_SUCCESS_RATE_PERCENT;
        if (random.nextInt(100) < chance) {
            enterResult("You ran away safely.");
        } else {
            message = "Run failed!";
            beginEnemyThinking();
        }
    }

    private void catchAttempt() {
        if (enemyCreature.getHp() <= 0) {
            message = "Cannot catch at 0 HP.";
            return;
        }
        int catchChance = 100;
        if (random.nextInt(100) < catchChance) {
            pendingCaughtCreatureName = enemyCreature.getName();
            enterResult("Caught " + enemyCreature.getName() + "!");
        } else {
            message = "Catch failed!";
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
        if (enemyCreature.getStatusEffect() == StatusEffect.PARALYZE && random.nextDouble() < 0.3) {
            enemyCreature.tickStatus();
            turnPhase = TurnPhase.PLAYER_THINKING;
            thinkTimer = THINK_DURATION_SECONDS;
            message = "Enemy is paralyzed and cannot attack.";
            return;
        }

        BattleMove[] enemyMoves = {
                new BattleMove("Pulse Strike", 40, 0, 0, StatusEffect.NONE, 0, BattleType.WILD),
                new BattleMove("Burn Claw", 35, 0, 0, StatusEffect.BURN, 0.22, BattleType.WILD),
                new BattleMove("Shock Fang", 32, 0, 0, StatusEffect.PARALYZE, 0.18, BattleType.WILD)
        };
        BattleMove enemyMove = enemyMoves[random.nextInt(enemyMoves.length)];
        int damage = computeDamage(enemyCreature, playerCreature, enemyMove.getPower(), enemyMove.getType());
        playerCreature.takeDamage(damage);
        playerHitTimer = HIT_ANIM_DURATION_SECONDS;

        if (enemyMove.getInflictEffect() != StatusEffect.NONE && random.nextDouble() < enemyMove.getEffectChance()) {
            playerCreature.setStatus(enemyMove.getInflictEffect(), 2);
        }

        applyEndTurnStatusDamage(playerCreature);
        applyEndTurnStatusDamage(enemyCreature);
        playerCreature.tickStatus();
        enemyCreature.tickStatus();
        tickCooldowns();

        if (playerCreature.isFainted()) {
            playerCreature.healToFull();
            enterResult("You lost the battle.");
        } else {
            message = "Enemy used " + enemyMove.getName() + " for " + damage + " damage.";
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
            int burn = Math.max(1, creature.getMaxHp() / 14);
            creature.takeDamage(burn);
        }
    }

    private int computeDamage(BattleCreature attacker, BattleCreature defender, int movePower, BattleType attackType) {
        double levelPart = ((2.0 * attacker.getLevel() / 5.0) + 2.0);
        double core = ((levelPart * movePower * (attacker.getAttack() / (double) Math.max(1, defender.getDefense()))) / 50.0) + 2.0;
        double stab = 1.0;
        double typeEffect = (attackType == BattleType.WILD) ? 1.0 : 1.0;
        double crit = random.nextDouble() < 0.1 ? 1.5 : 1.0;
        double randomFactor = 0.85 + random.nextDouble() * 0.15;
        int dmg = (int) Math.round(core * stab * typeEffect * crit * randomFactor);
        return Math.max(2, dmg);
    }

    private void drawStatusPanel(Graphics2D g2d, int x, int y, int width, int height, BattleCreature creature, boolean showNumericHp) {
        g2d.setColor(new Color(18, 20, 28, 230));
        g2d.fillRoundRect(x, y, width, height, 12, 12);
        g2d.setColor(new Color(235, 235, 235));
        g2d.drawRoundRect(x, y, width, height, 12, 12);

        g2d.setColor(Color.WHITE);
        g2d.drawString(creature.getName() + " Lv." + creature.getLevel(), x + 12, y + 22);
        g2d.drawString("HP", x + 12, y + 44);
        int hpBarX = x + 42;
        int hpBarY = y + 32;
        int hpBarW = width - 56;
        drawBar(g2d, hpBarX, hpBarY, hpBarW, 12, creature.getHp() / (double) creature.getMaxHp(), getHpBarColor(creature.getHp() / (double) creature.getMaxHp()));

        g2d.setColor(Color.WHITE);
        g2d.drawString("EN", x + 12, y + 62);
        int enBarX = x + 42;
        int enBarY = y + 50;
        int enBarW = width - 56;
        drawBar(g2d, enBarX, enBarY, enBarW, 10, creature.getEnergy() / (double) creature.getMaxEnergy(), new Color(94, 164, 255));

        if (showNumericHp) {
            g2d.setColor(Color.WHITE);
            g2d.drawString(creature.getHp() + "/" + creature.getMaxHp(), x + width - 100, y + 78);
        }
        if (creature.getStatusEffect() != StatusEffect.NONE) {
            g2d.setColor(new Color(255, 230, 120));
            g2d.drawString(creature.getStatusEffect().name() + " " + creature.getStatusTurns() + "T", x + 12, y + 78);
        }
    }

    private void drawBar(Graphics2D g2d, int x, int y, int w, int h, double ratio, Color fillColor) {
        g2d.setColor(new Color(55, 55, 65));
        g2d.fillRoundRect(x, y, w, h, 8, 8);
        int fillW = (int) Math.round(w * clamp01(ratio));
        g2d.setColor(fillColor);
        g2d.fillRoundRect(x, y, Math.max(0, fillW), h, 8, 8);
        g2d.setColor(new Color(18, 18, 24));
        g2d.drawRoundRect(x, y, w, h, 8, 8);
    }

    private Color getHpBarColor(double hpRatio) {
        if (hpRatio > 0.5) return new Color(74, 214, 95);
        if (hpRatio > 0.2) return new Color(245, 204, 77);
        return new Color(224, 72, 72);
    }

    private void drawActionButtons(Graphics2D g2d, int commandBoxX, int commandBoxY, int commandBoxW) {
        int btnY = commandBoxY + 40;
        int gap = 8;
        int btnW = (commandBoxW - 24 - gap * 2) / 3;
        int btnH = 46;
        int startX = commandBoxX + 12;
        String[] labels = {"1 Skill A", "2 Skill B", "3 Skill C", "4 Catch", "5 Switch", "6 Run"};
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
            boolean owned = ownedPlayerCreatures[i];
            g2d.setColor(isActive ? new Color(54, 82, 120, 230) : new Color(26, 30, 42, 220));
            g2d.fillRoundRect(bx, by, btnW, btnH, 8, 8);
            g2d.setColor(new Color(215, 215, 220));
            g2d.drawRoundRect(bx, by, btnW, btnH, 8, 8);
            g2d.setColor(owned ? Color.WHITE : new Color(155, 155, 155));
            g2d.drawString((i + 1) + " " + creature.getName(), bx + 8, by + 18);
            g2d.drawString("HP " + creature.getHp() + "/" + creature.getMaxHp(), bx + 8, by + 38);
            g2d.drawString("EN " + creature.getEnergy() + "/" + creature.getMaxEnergy(), bx + 8, by + 54);
            if (!owned) {
                g2d.drawString("LOCKED", bx + 8, by + 72);
            } else if (isActive) {
                g2d.drawString("ACTIVE", bx + 8, by + 72);
            } else {
                g2d.drawString("Switch", bx + 8, by + 72);
            }
        }
    }

    private void drawBattleCharacter(Graphics2D g2d, BufferedImage sprite, int x, int y, int w, int h, boolean isHit, Color fallbackColor) {
        if (sprite != null) {
            g2d.drawImage(sprite, x, y, w, h, null);
        } else {
            g2d.setColor(fallbackColor);
            g2d.fillOval(x, y, w, h);
            g2d.setColor(new Color(255, 255, 255, 70));
            g2d.drawOval(x, y, w, h);
        }
        if (isHit) {
            BufferedImage hit = sprite == enemyBattleSprite ? enemyBattleSpriteHit : getActivePlayerSpriteHit();
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
        BufferedImage beastSprite = getBattleSpriteForCreature(activeCreature.getName());
        if (beastSprite != null) {
            return beastSprite;
        }
        return playerBattleSprites[activePlayerIndex];
    }

    private BufferedImage getActivePlayerSpriteHit() {
        BattleCreature activeCreature = getActivePlayerCreature();
        BufferedImage beastHit = getHitSpriteForCreature(activeCreature.getName());
        if (beastHit != null) {
            return beastHit;
        }
        return playerBattleSpritesHit[activePlayerIndex];
    }

    private BufferedImage getBattleSpriteForCreature(String creatureName) {
        String key = toAssetKey(creatureName);
        if (key.isEmpty()) {
            return null;
        }
        if (beastSpriteCache.containsKey(key)) {
            return beastSpriteCache.get(key);
        }
        BufferedImage sprite = loadBeastSprite(key);
        beastSpriteCache.put(key, sprite);
        return sprite;
    }

    private BufferedImage getHitSpriteForCreature(String creatureName) {
        String key = toAssetKey(creatureName);
        if (key.isEmpty()) {
            return null;
        }
        if (beastSpriteHitCache.containsKey(key)) {
            return beastSpriteHitCache.get(key);
        }
        BufferedImage base = getBattleSpriteForCreature(creatureName);
        BufferedImage hit = createRedTintedSprite(base);
        beastSpriteHitCache.put(key, hit);
        return hit;
    }

    private int getShakeOffset(double timer) {
        if (timer <= 0) return 0;
        return (timer * 50 % 2 < 1) ? -4 : 4;
    }

    private void enterResult(String resultMessage) {
        scenePhase = ScenePhase.OUTRO;
        outroTimer = OUTRO_DURATION_SECONDS;
        pendingResultMessage = resultMessage;
        message = "";
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
        if (blurredBattleBackground == null || blurredBattleBackground.getWidth() != screenWidth || blurredBattleBackground.getHeight() != screenHeight) {
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
        double core = ((levelPart * move.getPower() * (attacker.getAttack() / (double) Math.max(1, enemyCreature.getDefense()))) / 50.0) + 2.0;
        int minDamage = Math.max(2, (int) Math.round(core * 0.85));
        int maxDamage = Math.max(2, (int) Math.round(core * 1.5));
        return "DMG: " + minDamage + "-" + maxDamage;
    }

    private static BufferedImage loadBeastSprite(String key) {
        String base = "res/beasts/" + key + "/" + key;
        String[] candidates = new String[]{
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
        if (base == null) return null;
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
}
