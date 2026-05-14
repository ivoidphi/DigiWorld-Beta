package digiworld.app;

import digiworld.audio.SoundManager;
import digiworld.battle.*;
import digiworld.core.*;
import digiworld.dialogue.*;
import digiworld.maps.*;
import digiworld.ui.*;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class GamePanel extends JPanel implements Runnable {
    private static final int TILE_SIZE = 32;
    private static final int RENDER_SCALE = 2;
    private static final int SCREEN_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
    private static final int SCREEN_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
    private static final int LOGICAL_WIDTH = Math.max(640, SCREEN_WIDTH / RENDER_SCALE);
    private static final int LOGICAL_HEIGHT = Math.max(360, SCREEN_HEIGHT / RENDER_SCALE);
    private static final double INTERACT_DISTANCE = TILE_SIZE * 1.75;

    private final InputHandler input;
    private final Camera camera;
    private final World[] worlds;
    private final BattleSystem battleSystem;
    private final GameUiRenderer uiRenderer;
    private final Inventory inventory;
    private final SoundManager soundManager;
    private final Random random;
    private int worldIndex;
    private final Player player;
    private final DoorManager doorManager;

    public int getWorldIndex() { return worldIndex; }
    public Player getPlayer() { return player; }

    private Thread gameThread;
    private long lastTimeNanos;
    private boolean interactionMenuOpen;
    private String interactionMessage;
    private Npc activeNpc;
    private GameState gameState;
    private int previousTileX;
    private int previousTileY;
    private final BufferedImage frameBuffer;
    private int pendingClickX;
    private int pendingClickY;
    private boolean clickPending;
    private final Set<String> seenNpcDialogues;
    private double windTimeSeconds;
    private final List<Particle> particles;
    private final String[] starterChoices;
    private final String[] enemyChoices;
    private int starterSelectionIndex;
    private int enemySelectionIndex;
    private boolean starterChosen;
    private final LinkedHashSet<Integer> selectedStarterIndices;
    private static final String PLAYER_NAME = "Player";
    private static final boolean CHEATS_ENABLED = false;
    private final DialogueController dialogueController;
    private boolean pendingDialogueEndAction;
    private String currentObjective;
    private String nextObjective;
    private enum ObjectiveTransitionPhase {
        IDLE,
        COMPLETE_FADE_IN,
        COMPLETE_HOLD,
        WAIT_CUTSCENE,
        COMPLETE_FADE_OUT,
        NEXT_FADE_IN
    }
    private ObjectiveTransitionPhase objectiveTransitionPhase;
    private boolean objectiveTransitioning;
    private String queuedObjective;
    private double objectivePhaseTimer;
    private double objectiveTextAlpha;
    private double objectiveCompleteAlpha;
    private double interactionCooldownTimer;
    private double autosaveTimer;
    private boolean hasTalkedToProfessor;
    private boolean hasTalkedToGeneral;
    private boolean starterSelectionDone;
    private boolean alphaBossDefeated;
    private boolean hasTalkedToChiefRei;
    private boolean hasChallengeTicket;
    private boolean trialCompleted;
    private boolean collapseStarted;
    private boolean betaCityUnlocked;
    private boolean labReturnDialogueDone;
    private boolean hasGWatch;
    private boolean hasMechDriver;
    private int beastCards;
    private static final class BushTile {
        final int x;
        final int y;

        BushTile(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    private final List<BushTile> validEncounterBushes;
    private int hiddenVineratopsTileX;
    private int hiddenVineratopsTileY;
    private boolean hiddenVineratopsTargetActive;
    private boolean hiddenVineratopsEncounterTriggered;
    private boolean vineratopsTrackedBattleActive;
    private boolean scanMarkerVisible;
    private double scanMarkerTimer;
    private double scanMarkerAlpha;
    private double scanPulseTime;
    private double scanUiFlashTimer;
    private boolean aceJazzDefeated;
    private boolean tutorialBattleDone;
    private boolean defeatedWildVineratops;
    private boolean defeatedWildZyuugor;
    private boolean defeatedPirrot;
    private boolean defeatedVoltchu;
    private boolean defeatedNokami;
    private boolean defeatedShadefox;
    private boolean defeatedKyoflare;
    private boolean battledWoltrix;
    private int profAlfredState;
    private int genEdState;
    private int chiefReiState;
    private int aceJazzState;
    private int trialmasterState;
    private int aldrichState;
    private Npc speechBubbleNpc;
    private String speechBubbleText;
    private double speechBubbleTimer;
    private double fadeAlpha;
    private double fadeTarget;
    private double fadeSpeed;
    private boolean movementLocked;
    private double encounterCooldownTimer;
    private boolean alphaTutorialTriggered;
    private boolean bossArenaActive;
    private int currentBossWorldIndex;
    private String currentBossName;
    private final QuestManager questManager;
    private boolean alphaArrivalDialogueDone;
    private boolean autoCloseDialogueWhenFinished;
    private double autoCloseDialogueTimer;
    private boolean aldrichObjectiveTriggered;
    private boolean pendingStage1ReturnCutscene;
    private boolean pendingBetaIntroDialogue;
    private static final int HOMETOWN_TELEPORT_DOOR_TILE_X = 29;
    private static final int HOMETOWN_TELEPORT_DOOR_TILE_Y = 17;
    private boolean teleportDoorLockedMessageShown;
    private boolean teleportInProgress;
    private double teleportDoorProximity;

    private boolean corruptionCutsceneActive;
    private double corruptionCutsceneTimer;
    private double corruptionShakeIntensity;
    private double corruptionGlitchIntensity;
    private double corruptionFadeProgress;
    private boolean corruptionMapLoaded;

    private boolean announcerDialogueActive;
    private DialogueController announcerDialogueController;
    private String[] announcerSpeakers;
    private String[] announcerLines;
    private int announcerPageIndex;

    private boolean sequentialBattlesActive;
    private int sequentialBattleIndex;
    private String[] sequentialBattleOrder;

    private double worldBannerTimer;
    private double worldBannerAlpha;

    public GamePanel() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);

        input = new InputHandler();
        addKeyListener(input);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pendingClickX = e.getX() * LOGICAL_WIDTH / Math.max(1, getWidth());
                pendingClickY = e.getY() * LOGICAL_HEIGHT / Math.max(1, getHeight());
                clickPending = true;
            }
        });

        camera = new Camera();
        worlds = createWorlds();
        battleSystem = new BattleSystem();
        soundManager = SoundManager.getInstance();
        battleSystem.setSoundManager(soundManager);
        uiRenderer = new GameUiRenderer(LOGICAL_WIDTH, LOGICAL_HEIGHT, PLAYER_NAME, battleSystem);
        inventory = new Inventory();
        random = new Random();
        worldIndex = 0;
        player = new Player(worlds[0].getSpawnTileX() * TILE_SIZE, worlds[0].getSpawnTileY() * TILE_SIZE, TILE_SIZE, input, TILE_SIZE);
        doorManager = new DoorManager(this, TILE_SIZE);
        player.setDoorManager(doorManager);
        frameBuffer = new BufferedImage(LOGICAL_WIDTH, LOGICAL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        interactionMessage = "";
        activeNpc = null;
        gameState = GameState.EXPLORATION;
        previousTileX = (int) player.getX() / TILE_SIZE;
        previousTileY = (int) player.getY() / TILE_SIZE;
        clickPending = false;
        seenNpcDialogues = new HashSet<>();
        windTimeSeconds = 0.0;
        particles = new ArrayList<>();
        starterChoices = new String[]{
                "Kyoflare", "Nokami", "Vineratops", "Voltchu", "Zyuugor",
                "Pirrot", "Gekuma", "Shadefox", "Kingmantis", "All Mighty"
        };
        enemyChoices = new String[]{
                "Kyoflare", "Nokami", "Vineratops", "Voltchu", "Zyuugor",
                "Pirrot", "Gekuma", "Shadefox", "Kingmantis", "Woltrix", "All Mighty"
        };
        starterSelectionIndex = 0;
        enemySelectionIndex = 0;
        starterChosen = false;
        selectedStarterIndices = new LinkedHashSet<>();
        dialogueController = new DialogueController();
        pendingDialogueEndAction = false;
        questManager = new QuestManager();
        currentObjective = questManager.objectiveForStage();
        nextObjective = null;
        objectiveTransitionPhase = ObjectiveTransitionPhase.IDLE;
        objectiveTransitioning = false;
        queuedObjective = null;
        objectivePhaseTimer = 0.0;
        objectiveTextAlpha = 1.0;
        objectiveCompleteAlpha = 0.0;
        interactionCooldownTimer = 0.0;
        autosaveTimer = 0.0;
        hasTalkedToProfessor = false;
        hasTalkedToGeneral = false;
        starterSelectionDone = false;
        alphaBossDefeated = false;
        hasTalkedToChiefRei = false;
        hasChallengeTicket = false;
        trialCompleted = false;
        collapseStarted = false;
        betaCityUnlocked = false;
        labReturnDialogueDone = false;
        hasGWatch = false;
        hasMechDriver = false;
        beastCards = 0;
        validEncounterBushes = new ArrayList<>();
        hiddenVineratopsTileX = -1;
        hiddenVineratopsTileY = -1;
        hiddenVineratopsTargetActive = false;
        hiddenVineratopsEncounterTriggered = false;
        vineratopsTrackedBattleActive = false;
        scanMarkerVisible = false;
        scanMarkerTimer = 0.0;
        scanMarkerAlpha = 0.0;
        scanPulseTime = 0.0;
        scanUiFlashTimer = 0.0;
        aceJazzDefeated = false;
        tutorialBattleDone = false;
        defeatedWildVineratops = false;
        defeatedWildZyuugor = false;
        defeatedPirrot = false;
        defeatedVoltchu = false;
        defeatedNokami = false;
        defeatedShadefox = false;
        defeatedKyoflare = false;
        battledWoltrix = false;
        profAlfredState = 0;
        genEdState = 0;
        chiefReiState = 0;
        aceJazzState = 0;
        trialmasterState = 0;
        aldrichState = 0;
        speechBubbleNpc = null;
        speechBubbleText = "";
        speechBubbleTimer = 0.0;
        fadeAlpha = 0.0;
        fadeTarget = 0.0;
        fadeSpeed = 3.0;
        movementLocked = false;
        encounterCooldownTimer = 0.0;
        alphaTutorialTriggered = false;
        bossArenaActive = false;
        currentBossWorldIndex = -1;
        currentBossName = null;
        alphaArrivalDialogueDone = false;
        autoCloseDialogueWhenFinished = false;
        autoCloseDialogueTimer = 0.0;
        aldrichObjectiveTriggered = false;
        pendingStage1ReturnCutscene = false;
        pendingBetaIntroDialogue = false;
        teleportDoorLockedMessageShown = false;
        teleportInProgress = false;
        teleportDoorProximity = 0.0;

        corruptionCutsceneActive = false;
        corruptionCutsceneTimer = 0.0;
        corruptionShakeIntensity = 0.0;
        corruptionGlitchIntensity = 0.0;
        corruptionFadeProgress = 0.0;
        corruptionMapLoaded = false;

        announcerDialogueActive = false;
        announcerDialogueController = new DialogueController();
        announcerSpeakers = null;
        announcerLines = null;
        announcerPageIndex = 0;

        sequentialBattlesActive = false;
        sequentialBattleIndex = 0;
        sequentialBattleOrder = null;

        worldBannerTimer = 0.0;
        worldBannerAlpha = 0.0;
        soundManager.playWorldMusic(worlds[worldIndex].getName());
    }

    public void startGame() {
        requestFocusInWindow();
        gameThread = new Thread(this, "game-loop");
        gameThread.start();
    }

    @Override
    public void run() {
        lastTimeNanos = System.nanoTime();
        while (Thread.currentThread() == gameThread) {
            long now = System.nanoTime();
            double deltaSeconds = (now - lastTimeNanos) / 1_000_000_000.0;
            lastTimeNanos = now;

            updateGame(Math.min(deltaSeconds, 0.05));
            repaint();

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void updateGame(double deltaSeconds) {
        windTimeSeconds += deltaSeconds;
        interactionCooldownTimer = Math.max(0.0, interactionCooldownTimer - deltaSeconds);
        scanUiFlashTimer = Math.max(0.0, scanUiFlashTimer - deltaSeconds);
        scanPulseTime += deltaSeconds;
        updateScanMarker(deltaSeconds);
        autosaveTimer += deltaSeconds;
        if (autosaveTimer >= 60.0) {
            autosaveTimer = 0.0;
            saveProgress();
        }
        updateObjectiveTransition(deltaSeconds);
        updateCorruptionCutscene(deltaSeconds);
        updateAnnouncerDialogue(deltaSeconds);
        World current = worlds[worldIndex];

        if (fadeAlpha > 0.001) {
            fadeAlpha += (fadeTarget - fadeAlpha) * fadeSpeed * deltaSeconds;
            if (Math.abs(fadeTarget - fadeAlpha) < 0.001) {
                fadeAlpha = fadeTarget;
                if (fadeTarget <= 0.001 && movementLocked) {
                    movementLocked = false;
                }
            }
        }

        if (gameState == GameState.BATTLE) {
            if (clickPending) {
                battleSystem.handleClick(pendingClickX, pendingClickY);
                clickPending = false;
            }
            battleSystem.update(deltaSeconds);
            String result = battleSystem.handleInput(input);
            if (!battleSystem.isActive()) {
                encounterCooldownTimer = 2.0 + random.nextDouble() * 3.0;

                // --- LEVEL UP SYSTEM FIX: Capture the actual leveled-up object ---
                BattleCreature caughtCreature = battleSystem.consumeCaughtCreature();
                if (caughtCreature != null) {
                    inventory.addBeast(caughtCreature);
                }

                String msg = battleSystem.getMessage();
                applyBattleOutcomeProgress();
                boolean vineratopsJustWon = false;
                if (vineratopsTrackedBattleActive) {
                    if (msg != null && msg.toLowerCase().contains("won")) {
                        vineratopsJustWon = true;
                    }
                    vineratopsTrackedBattleActive = false;
                }
                boolean bossWon = msg != null && msg.toLowerCase().contains("won");
                if (bossArenaActive && bossWon) {
                    handleBossVictory();
                } else if (bossArenaActive && !bossWon) {
                    handleBossDefeat();
                }
                if (gameState == GameState.BATTLE && !battleSystem.isActive()) {
                    gameState = GameState.EXPLORATION;
                    interactionMenuOpen = false;
                    activeNpc = null;
                }
                if (!battleSystem.isActive()) {
                    interactionMessage = vineratopsJustWon ? "Wild Vineratops defeated." : (bossArenaActive ? interactionMessage : msg);
                    soundManager.playWorldMusic(current.getName());
                }
            }
        } else if (gameState == GameState.INVENTORY) {
            handleInventoryInput();
        } else if (gameState == GameState.NPC_MENU) {
            handleMenuInput();
        } else if (gameState == GameState.DIALOGUE) {
            handleDialogueInputAndTyping(deltaSeconds);
        } else if (gameState == GameState.STARTER_SELECT) {
            handleStarterSelectionInput();
        } else if (gameState == GameState.ENEMY_SELECT) {
            handleEnemySelectionInput();
        } else {
            if (movementLocked) {
                updateCamera(current);
                input.endFrame();
                return;
            }

            encounterCooldownTimer = Math.max(0.0, encounterCooldownTimer - deltaSeconds);

            if (pendingDialogueEndAction) {
                pendingDialogueEndAction = false;
                executeDialogueEndAction();
                refreshObjectiveFromProgress();
            }

            if (input.consumeJustPressed(KeyEvent.VK_B)) {
                gameState = GameState.INVENTORY;
                interactionMessage = "";
                return;
            }
            if (input.consumeJustPressed(KeyEvent.VK_G)) {
                triggerGWatchScan(current);
            }
            double beforeX = player.getX();
            double beforeY = player.getY();
            player.update(deltaSeconds, current);
            if (gameState == GameState.EXPLORATION) doorManager.check();
            spawnMovementParticles(current, beforeX, beforeY);
            updateHometownTeleportDoor(current);
            Npc nearbyNpc = getNearbyNpc(current);
            if (input.consumeJustPressed(KeyEvent.VK_E) && nearbyNpc != null) {
                if (interactionCooldownTimer > 0.0) {
                    interactionMessage = "Please wait before interacting again.";
                    input.endFrame();
                    return;
                }
                interactionCooldownTimer = 0.35;
                activeNpc = nearbyNpc;
                activeNpc.beginInteractionFacing(player.getCenterX(), player.getCenterY());
                startNpcDialogue(nearbyNpc, current);
            }
            checkWildEncounter(current);
            checkBossTrigger(current);
            maybeStartAlphaArrivalDialogue(current);
            updateAldrichProximityObjective(current);
            updateTrialmasterProximityObjective(current);
            if (battledWoltrix && questManager.isStage(QuestManager.STAGE_REACHED_GLITCH_AREA)) {
                handleGlitchBossVictory();
            }
            refreshObjectiveFromProgress();
        }

        if (gameState != GameState.BATTLE && gameState != GameState.DIALOGUE && gameState != GameState.STARTER_SELECT) {
            current.update(deltaSeconds, player);
        }
        updateCamera(current);
        updateParticles(deltaSeconds);
        updateSpeechBubble(deltaSeconds);

        input.endFrame();
    }

    private void applyBattleOutcomeProgress() {
        String enemy = battleSystem.consumeLastResolvedEnemyName();
        boolean won = battleSystem.consumeLastBattleWon();
        if (!won || enemy == null || enemy.isBlank()) {
            return;
        }
        String key = enemy.trim().toLowerCase();
        if ("voltchu".equals(key)) {
            defeatedVoltchu = true;
            if (worldIndex == 1 && !tutorialBattleDone) {
                tutorialBattleDone = true;
            }
        } else if ("vineratops".equals(key)) {
            defeatedWildVineratops = true;
            hiddenVineratopsTargetActive = false;
            hiddenVineratopsEncounterTriggered = false;
            hiddenVineratopsTileX = -1;
            hiddenVineratopsTileY = -1;
            if (questManager.isStage(QuestManager.STAGE_TALKED_CHIEF_REI)) {
                questManager.setQuestStage(QuestManager.STAGE_COMPLETED_TUTORIAL);
                setObjective("Reach the Heart of the Forest");
            }
        } else if ("zyuugor".equals(key)) {
            defeatedWildZyuugor = true;
        } else if ("pirrot".equals(key)) {
            defeatedPirrot = true;
        } else if ("nokami".equals(key)) {
            defeatedNokami = true;
        } else if ("shadefox".equals(key)) {
            defeatedShadefox = true;
        } else if ("kyoflare".equals(key)) {
            defeatedKyoflare = true;
        } else if ("woltrix".equals(key)) {
            battledWoltrix = true;
        }
        refreshObjectiveFromProgress();
    }

    private void refreshObjectiveFromProgress() {
        if (questManager.isStage(QuestManager.STAGE_COMPLETED_TUTORIAL) && aldrichObjectiveTriggered) {
            setObjective("Defeat Aldrich");
            return;
        }
        setObjective(questManager.objectiveForStage());
    }

    private void setObjective(String objective) {
        if (objective == null || objective.isBlank()) {
            return;
        }
        if (objective.equals(currentObjective)) {
            return;
        }
        if (objectiveTransitioning) {
            queuedObjective = objective;
            return;
        }
        startObjectiveTransition(objective);
    }

    private void startObjectiveTransition(String objective) {
        if (objective == null || objective.isBlank()) {
            return;
        }
        if (objectiveTransitioning) {
            return;
        }
        nextObjective = objective;
        objectiveTransitioning = true;
        objectiveTransitionPhase = ObjectiveTransitionPhase.COMPLETE_FADE_IN;
        objectivePhaseTimer = 0.0;
        objectiveTextAlpha = 0.0;
        objectiveCompleteAlpha = 0.0;
    }

    private void updateObjectiveTransition(double deltaSeconds) {
        switch (objectiveTransitionPhase) {
            case IDLE -> {
                objectiveTextAlpha = 1.0;
                objectiveCompleteAlpha = 0.0;
                if (!objectiveTransitioning && queuedObjective != null && !queuedObjective.isBlank() && !queuedObjective.equals(currentObjective)) {
                    String queued = queuedObjective;
                    queuedObjective = null;
                    startObjectiveTransition(queued);
                }
            }
            case COMPLETE_FADE_IN -> {
                objectiveCompleteAlpha = Math.min(1.0, objectiveCompleteAlpha + (deltaSeconds / 0.10));
                if (objectiveCompleteAlpha >= 0.999) {
                    objectiveCompleteAlpha = 1.0;
                    objectiveTransitionPhase = ObjectiveTransitionPhase.COMPLETE_HOLD;
                    objectivePhaseTimer = 0.45;
                }
            }
            case COMPLETE_HOLD -> {
                objectivePhaseTimer = Math.max(0.0, objectivePhaseTimer - deltaSeconds);
                if (objectivePhaseTimer <= 0.0) {
                    objectiveTransitionPhase = isCutscenePlaying()
                            ? ObjectiveTransitionPhase.WAIT_CUTSCENE
                            : ObjectiveTransitionPhase.COMPLETE_FADE_OUT;
                }
            }
            case WAIT_CUTSCENE -> {
                objectiveCompleteAlpha = 1.0;
                if (!isCutscenePlaying()) {
                    objectiveTransitionPhase = ObjectiveTransitionPhase.COMPLETE_FADE_OUT;
                }
            }
            case COMPLETE_FADE_OUT -> {
                objectiveCompleteAlpha = Math.max(0.0, objectiveCompleteAlpha - (deltaSeconds / 0.10));
                if (objectiveCompleteAlpha <= 0.001) {
                    objectiveCompleteAlpha = 0.0;
                    if (nextObjective != null && !nextObjective.isBlank()) {
                        currentObjective = nextObjective;
                        nextObjective = null;
                    }
                    objectiveTransitionPhase = ObjectiveTransitionPhase.NEXT_FADE_IN;
                    objectiveTextAlpha = 0.0;
                }
            }
            case NEXT_FADE_IN -> {
                objectiveTextAlpha = Math.min(1.0, objectiveTextAlpha + (deltaSeconds / 0.12));
                if (objectiveTextAlpha >= 0.999) {
                    objectiveTextAlpha = 1.0;
                    objectiveTransitionPhase = ObjectiveTransitionPhase.IDLE;
                    objectiveTransitioning = false;
                }
            }
        }
    }

    private boolean isCutscenePlaying() {
        return gameState == GameState.DIALOGUE || movementLocked;
    }

    private void updateSpeechBubble(double deltaSeconds) {
        if (speechBubbleTimer <= 0.0) {
            return;
        }
        speechBubbleTimer = Math.max(0.0, speechBubbleTimer - deltaSeconds);
        if (speechBubbleTimer <= 0.0) {
            speechBubbleNpc = null;
            speechBubbleText = "";
        }
    }

    private void updateCamera(World current) {
        if (gameState == GameState.DIALOGUE && activeNpc != null) {
            int focusX = (player.getCenterX() + activeNpc.getCenterX()) / 2;
            int focusY = (player.getCenterY() + activeNpc.getCenterY()) / 2;
            camera.followPoint(focusX, focusY, current, LOGICAL_WIDTH, LOGICAL_HEIGHT, TILE_SIZE);
            return;
        }
        camera.follow(player, current, LOGICAL_WIDTH, LOGICAL_HEIGHT, TILE_SIZE);
    }

    private void startDialogue(DialogueSequence sequence, String postDialogueAction) {
        dialogueController.start(sequence);
        interactionMenuOpen = false;
        gameState = GameState.DIALOGUE;
        autoCloseDialogueWhenFinished = false;
        autoCloseDialogueTimer = 0.0;
        pendingDialogueEndAction = postDialogueAction != null && !postDialogueAction.isEmpty();
        if (pendingDialogueEndAction) {
            this.postDialogueAction = postDialogueAction;
        }
    }

    private void startNpcDialogue(Npc npc, World world) {
        String npcName = npc.getName().toLowerCase();
        String worldName = world.getName().toLowerCase();

        if (npcName.contains("prof alfred") || npcName.contains("professor")) {
            handleProfAlfredDialogue(npc, worldName);
        } else if (npcName.contains("gen ed") || npcName.contains("general")) {
            handleGenEdDialogue(npc, worldName);
        } else if (npcName.contains("chief rei")) {
            handleChiefReiDialogue(npc);
        } else if (npcName.contains("ace") || npcName.contains("jazz")) {
            handleAceJazzDialogue(npc);
        } else if (npcName.contains("trialmaster")) {
            handleTrialmasterDialogue(npc);
        } else if (npcName.contains("aldrich") || npcName.contains("alpha")) {
            handleAldrichDialogue(npc);
        } else if (npcName.contains("boss rhonn") || npcName.contains("shopkeeper")) {
            handleShopkeeperDialogue(npc);
        } else if (npcName.contains("glitch")) {
            handleGlitchDialogue(npc);
        } else {
            showNpcSpeechBubble(npc, "I got nothing for you buddy", 2.0);
            npc.endInteraction();
            activeNpc = null;
        }
    }

    private void handleProfAlfredDialogue(Npc npc, String worldName) {
        if (worldName.contains("hometown")) {
            if (questManager.isStage(QuestManager.STAGE_GAME_START)) {
                DialogueSequence script = DialogueFactory.createSequence(
                        new String[]{"Professor Alfred", "Professor Alfred", "Professor Alfred", PLAYER_NAME},
                        new String[]{
                                "Hello and welcome Mr. gaming prodigy and 10-time world champion " + PLAYER_NAME + " to the gaming lab where you will be testing our new revolutionary game \"DigiWorld\".",
                                "Oh, where are my manners!? I’m Professor Alfred, and I’m in charge of developing this game.",
                                "You will be transported into a world full of creatures called \"Mecha Beasts\", fuse with them, create a bond and battle together.",
                                "Thanks for the explanation, Professor, but may I know what is the reason the government funded to create this game?"
                        }
                );
                startDialogue(script, "SET_OBJECTIVE:Talk to General Edrian|SET_FLAG:hasTalkedToProfessor|SET_NPC_STATE:profAlfredState:1");
            } else if (questManager.isStage(QuestManager.STAGE_TALKED_PROF)) {
                DialogueSequence script = DialogueFactory.createSequence(
                        new String[]{"Professor Alfred"},
                        new String[]{"Please speak with General Edrian first."}
                );
                startDialogue(script, null);
            } else if (questManager.isStage(QuestManager.STAGE_TALKED_GEN)) {
                DialogueSequence script = DialogueFactory.createSequence(
                        new String[]{"Professor Alfred", "Professor Alfred"},
                        new String[]{
                                "Excellent! Now before we transport you, you will need this G-Watch, Mech-driver and Beast-Cards.",
                                "Choose 3 out of these 10 Mecha Beasts:"
                        }
                );
                startDialogue(script, "WAIT_AND_OPEN_STARTER_SELECT|SET_NPC_STATE:profAlfredState:2");
            } else if (questManager.isStage(QuestManager.STAGE_SELECTED_STARTERS)) {
                DialogueSequence script = DialogueFactory.createSequence(
                        new String[]{"Professor Alfred"},
                        new String[]{"The teleport door is ready. Step inside to enter DigiWorld."}
                );
                startDialogue(script, null);
            } else if (questManager.isStage(QuestManager.STAGE_DEFEATED_ALDRICH)) {
                DialogueSequence script = DialogueFactory.createSequence(
                        new String[]{PLAYER_NAME, "Professor Alfred", PLAYER_NAME, "Professor Alfred"},
                        new String[]{
                                "Professor! I'm back! Stage one is complete!",
                                "Excellent work! Stage one is complete. And no bugs either. You handled yourself well, " + PLAYER_NAME + "!",
                                "Don't act proud! You never told me the pain would feel real. I thought I was going to die!",
                                "But you did not. That is the point. This game is meant to be lived, not just played. Now rest. Tomorrow, the next test awaits."
                        }
                );
                startDialogue(script, "SET_OBJECTIVE:Enter Beta City|SET_FLAG:labReturnDialogueDone");
            }
        } else if (worldName.contains("alpha")) {
            if (questManager.isStage(QuestManager.STAGE_TALKED_CHIEF_REI)) {
                DialogueSequence script = DialogueFactory.createSequence(
                        new String[]{"Professor Alfred"},
                        new String[]{"Use your G-Watch to locate Wild Vineratops. Defeat it, then proceed to the heart of the Mystic Forest."}
                );
                startDialogue(script, null);
            } else {
                DialogueSequence script = DialogueFactory.createSequence(
                        new String[]{"Professor Alfred"},
                        new String[]{"Welcome to the beta test. Follow your G-Watch route to the Alpha Beast. You may feel pain in DigiWorld, but you will not die."}
                );
                startDialogue(script, null);
            }
        }
    }

    private void handleGenEdDialogue(Npc npc, String worldName) {
        if (worldName.contains("hometown")) {
            if (!questManager.isStage(QuestManager.STAGE_TALKED_PROF)) {
                showNpcSpeechBubble(npc, "You should speak with Professor Alfred first.", 2.0);
                npc.endInteraction();
                activeNpc = null;
                return;
            }
            if (genEdState == 0 && questManager.isStage(QuestManager.STAGE_TALKED_PROF)) {
                DialogueSequence script = DialogueFactory.createSequence(
                        new String[]{"General Edrian", "General Edrian", "General Edrian", "General Edrian", "General Edrian", PLAYER_NAME},
                        new String[]{
                                "I shall be the one to answer that question player, I was actually intrigued by gaming.",
                                "Seeing players think like they’re chess masters or generals and coordinate their upper body in tapping their devices to win, it gave me an idea.",
                                "What if we create a game where you become the character and fight but in a digital world?",
                                "This way gamers could play digital games while still being physically active, and they will be able to experience the feeling of being their character.",
                                "This could create a new legacy for \"Gamers\".",
                                "That doesn't fully answer my question but oh well. Let's get this started."
                        }
                );
                startDialogue(script, "SET_OBJECTIVE:Return to Professor Alfred|SET_FLAG:hasTalkedToGeneral|SET_NPC_STATE:genEdState:1");
            } else {
                DialogueSequence script = DialogueFactory.createSequence(
                        new String[]{"General Edrian"},
                        new String[]{"Good luck out there, " + PLAYER_NAME + "."}
                );
                startDialogue(script, null);
            }
        }
    }

    private void handleChiefReiDialogue(Npc npc) {
        if (!questManager.isStage(QuestManager.STAGE_ENTERED_DIGIWORLD)) {
            showNpcSpeechBubble(npc, "You are not ready yet.", 2.0);
            npc.endInteraction();
            activeNpc = null;
            return;
        }
        if (chiefReiState == 0 && questManager.isStage(QuestManager.STAGE_ENTERED_DIGIWORLD)) {
            DialogueSequence script = DialogueFactory.createSequence(
                    new String[]{"Chief Rei", PLAYER_NAME, "Chief Rei", PLAYER_NAME, "Chief Rei", PLAYER_NAME},
                    new String[]{
                            "Welcome, traveler. I am Chief Rei, guardian of this village. What is it you seek?",
                            "I was told there is an Alpha Beast here.",
                            "That is true. But tell me… are you a challenger, or just another treasure hunter?",
                            "I… I guess I am a challenger.",
                            "Then follow the Mystic Forest. The path will test you before you reach the Alpha. Be prepared.",
                            "Thank you, Chief."
                    }
            );
            startDialogue(script, "SET_OBJECTIVE:Locate the Wild Vineratops using your G-Watch|SET_FLAG:hasTalkedToChiefRei|SET_NPC_STATE:chiefReiState:1");
        } else {
            DialogueSequence script = DialogueFactory.createSequence(
                    new String[]{"Chief Rei"},
                    new String[]{"Follow the Mystic Forest. Use your G-Watch and face the trial."}
            );
            startDialogue(script, null);
        }
    }

    private void handleAceJazzDialogue(Npc npc) {
        if (!questManager.isStage(QuestManager.STAGE_RETURNED_TO_LAB)) {
            showNpcSpeechBubble(npc, "You are not ready yet.", 2.0);
            npc.endInteraction();
            activeNpc = null;
            return;
        }
        if (aceJazzState == 0) {
            DialogueSequence script = DialogueFactory.createSequence(
                    new String[]{"Ace Jazz"},
                    new String[]{"I am the Ace Trainer Jazz. Battle me and win for only the strongest may pass! BEAST CARD ON! HENSHIN!"}
            );
            startDialogue(script, "START_BOSS:ace_jazz|SET_OBJECTIVE:Defeat Ace Jazz");
        } else if (aceJazzState == 1) {
            DialogueSequence script = DialogueFactory.createSequence(
                    new String[]{"Ace Jazz"},
                    new String[]{"You've surpassed us all. Take this Challenge Ticket, you've earned it. With it, you're worthy of the Tournament Trial."}
            );
            startDialogue(script, "GIVE_ITEM:challenge_ticket|SET_FLAG:hasChallengeTicket|SET_NPC_STATE:aceJazzState:2|SET_OBJECTIVE:Enter Tournament Trial");
        } else {
            DialogueSequence script = DialogueFactory.createSequence(
                    new String[]{"Ace Jazz"},
                    new String[]{"The Tournament Trial awaits. Good luck."}
            );
            startDialogue(script, null);
        }
    }

    private void handleTrialmasterDialogue(Npc npc) {
        if (!inventory.hasItem("Challenge Ticket")) {
            showNpcSpeechBubble(npc, "Scram. Go get the Challenge Ticket from Ace Jazz first, then maybe you’ll be worthy enough to face me.", 2.8);
            npc.endInteraction();
            activeNpc = null;
            return;
        }
        if (trialmasterState == 0) {
            DialogueSequence script = DialogueFactory.createSequence(
                    new String[]{"Trialmaster", PLAYER_NAME, "Trialmaster", PLAYER_NAME, "Trialmaster"},
                    new String[]{
                            "Welcome, challenger! I am Trialmaster! Will you defeat me to qualify for the tournament, or start all over again?",
                            "Wait… Professor? Is that you? Are you also testing the game, or is this some NPC of you?",
                            "IIII… d-d-don’t know who this Professor you’re talking about!",
                            "Seriously!? You’re a bad liar.",
                            "Enough talk! Prove your strength first, then I’ll answer. BEAST CARD ON! HENSHIN!"
                    }
            );
            startDialogue(script, "START_BOSS:trialmaster|SET_OBJECTIVE:Defeat Trialmaster");
        } else if (trialmasterState == 1) {
            DialogueSequence script = DialogueFactory.createSequence(
                    new String[]{"Trialmaster", "Trialmaster", "Trialmaster", "Trialmaster", PLAYER_NAME, "Trialmaster"},
                    new String[]{
                            "You’ve beaten me… and qualified for the tournament!",
                            "And yes, I’m the Professor. In this game, I’m “Trialmaster.”",
                            "I transported in to personally monitor the test and ensure no glitches slip through.",
                            "I sense something bad is coming, and I had to see it for myself instead of just observing you.",
                            "But why would something bad happen? You’re a genius, you’ve planned everything for this game!",
                            "True, but there’s been tension between the staff and the General even before the beta started. Reasons I can’t share with you… for now. Let’s continue testing and see what unfolds."
                    }
            );
            startDialogue(script, "SET_FLAG:trialCompleted|SET_NPC_STATE:trialmasterState:2|SET_OBJECTIVE:Wait for the Tournament to Begin");
        } else {
            DialogueSequence script = DialogueFactory.createSequence(
                    new String[]{"Trialmaster"},
                    new String[]{"Wait for the Tournament to Begin."}
            );
            startDialogue(script, null);
        }
    }

    private void handleAldrichDialogue(Npc npc) {
        if (!questManager.isStage(QuestManager.STAGE_COMPLETED_TUTORIAL)) {
            showNpcSpeechBubble(npc, "You are not ready yet.", 2.0);
            npc.endInteraction();
            activeNpc = null;
            return;
        }
        if (aldrichState == 0) {
            DialogueSequence script = DialogueFactory.createSequence(
                    new String[]{"Aldrich", "Player", "Aldrich"},
                    new String[]{
                            "Who dares step into my domain?",
                            "My name is " + PLAYER_NAME + ". I came to challenge you.",
                            "Then face me. I am Altair, the Alpha. And this is my partner, the Alpha Beast, Gekuma. BEAST CARD ON! HENSHIN!"
                    }
            );
            startDialogue(script, "START_BOSS:aldrich|SET_OBJECTIVE:Defeat Aldrich");
        } else {
            DialogueSequence script = DialogueFactory.createSequence(
                    new String[]{"Aldrich"},
                    new String[]{"Come back when your bond with your beasts is stronger, fool."}
            );
            startDialogue(script, null);
        }
    }

    private void handleShopkeeperDialogue(Npc npc) {
        DialogueSequence script = DialogueFactory.createSequence(
                new String[]{"Boss Rhonn"},
                new String[]{"Welcome to my shop. Take a look at what I have."}
        );
        startDialogue(script, null);
    }

    private void handleGlitchDialogue(Npc npc) {
        if (!questManager.isStage(QuestManager.STAGE_TOURNAMENT_STARTED)) {
            showNpcSpeechBubble(npc, "You are not ready yet.", 2.0);
            npc.endInteraction();
            activeNpc = null;
            return;
        }
        questManager.setQuestStage(QuestManager.STAGE_REACHED_GLITCH_AREA);
        setObjective("Defeat Glitch");
        DialogueSequence script = DialogueFactory.createSequence(
                new String[]{"Unknown Speaker", "Player", "Glitch"},
                new String[]{
                        "Call me Glitch. I'm the real-world savior. If you win, I'll stop. If I win, I want your autograph.",
                        "Let's do this then!",
                        "But you will have to fight my beasts first!"
                }
        );
        startDialogue(script, "START_SEQUENTIAL_BATTLES");
    }

    private String postDialogueAction;

    private void executeDialogueEndAction() {
        if (postDialogueAction == null || postDialogueAction.isEmpty()) {
            return;
        }

        String[] actions = postDialogueAction.split("\\|");
        for (String action : actions) {
            action = action.trim();
            if (action.startsWith("SET_OBJECTIVE:")) {
                setObjective(action.substring("SET_OBJECTIVE:".length()));
            } else if (action.startsWith("SET_FLAG:")) {
                String flag = action.substring("SET_FLAG:".length());
                switch (flag) {
                    case "hasTalkedToProfessor" -> {
                        hasTalkedToProfessor = true;
                        questManager.setQuestStage(QuestManager.STAGE_TALKED_PROF);
                    }
                    case "hasTalkedToGeneral" -> {
                        hasTalkedToGeneral = true;
                        questManager.setQuestStage(QuestManager.STAGE_TALKED_GEN);
                    }
                    case "starterSelectionDone" -> starterSelectionDone = true;
                    case "alphaBossDefeated" -> {
                        alphaBossDefeated = true;
                        questManager.setQuestStage(QuestManager.STAGE_DEFEATED_ALDRICH);
                    }
                    case "hasTalkedToChiefRei" -> {
                        hasTalkedToChiefRei = true;
                        questManager.setQuestStage(QuestManager.STAGE_TALKED_CHIEF_REI);
                    }
                    case "hasChallengeTicket" -> hasChallengeTicket = true;
                    case "trialCompleted" -> {
                        trialCompleted = true;
                        questManager.setQuestStage(QuestManager.STAGE_DEFEATED_TRIALMASTER);
                    }
                    case "collapseStarted" -> {
                        collapseStarted = true;
                        questManager.setQuestStage(QuestManager.STAGE_TOURNAMENT_STARTED);
                    }
                    case "betaCityUnlocked" -> betaCityUnlocked = true;
                    case "labReturnDialogueDone" -> {
                        labReturnDialogueDone = true;
                        questManager.setQuestStage(QuestManager.STAGE_RETURNED_TO_LAB);
                    }
                    case "aceJazzDefeated" -> aceJazzDefeated = true;
                    case "tutorialBattleDone" -> tutorialBattleDone = true;
                }
            } else if (action.startsWith("SET_NPC_STATE:")) {
                String[] parts = action.substring("SET_NPC_STATE:".length()).split(":");
                if (parts.length == 2) {
                    String npcStateName = parts[0];
                    int newState = Integer.parseInt(parts[1]);
                    switch (npcStateName) {
                        case "profAlfredState" -> profAlfredState = newState;
                        case "genEdState" -> genEdState = newState;
                        case "chiefReiState" -> chiefReiState = newState;
                        case "aceJazzState" -> aceJazzState = newState;
                        case "trialmasterState" -> trialmasterState = newState;
                        case "aldrichState" -> aldrichState = newState;
                    }
                }
            } else if (action.startsWith("WAIT_AND_OPEN_STARTER_SELECT")) {
                if (!questManager.isStage(QuestManager.STAGE_TALKED_GEN) || starterSelectionDone) {
                    interactionMessage = "You are not ready yet.";
                    continue;
                }
                movementLocked = true;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                gameState = GameState.STARTER_SELECT;
                movementLocked = false;
                interactionMessage = "Professor Alfred: Choose 3 out of 10 Mecha Beasts.";
            } else if (action.startsWith("START_BOSS:")) {
                String bossKey = action.substring("START_BOSS:".length());
                switch (bossKey) {
                    case "ace_jazz" -> {
                        aceJazzState = 1;
                        startBossBattle(worlds[worldIndex], "Pirrot");
                    }
                    case "aldrich" -> {
                        aldrichState = 1;
                        startBossBattle(worlds[worldIndex], "Gekuma");
                    }
                    case "trialmaster" -> startBossBattle(worlds[worldIndex], "Voltchu");
                    case "glitch_woltrix" -> startBossBattle(worlds[worldIndex], "Woltrix");
                }
            } else if (action.startsWith("GIVE_ITEM:")) {
                String item = action.substring("GIVE_ITEM:".length());
                interactionMessage = "Received: " + item + "!";
                if ("challenge_ticket".equalsIgnoreCase(item)) {
                    inventory.addItem("Challenge Ticket");
                }
            } else if (action.equals("TELEPORT_TO_BETA")) {
                beginBetaCityPreparationTransition();
            } else if (action.equals("TELEPORT_TO_LAB")) {
                movementLocked = true;
                teleportWithFade(0);
            } else if (action.equals("MARK_LAB_RETURN_DONE")) {
                labReturnDialogueDone = true;
            } else if (action.equals("TELEPORT_TO_LAB_STAGE1")) {
                pendingStage1ReturnCutscene = true;
                movementLocked = true;
                teleportWithFade(0);
            } else if (action.equals("STAGE2_TRANSPORT_TO_BETA")) {
                pendingBetaIntroDialogue = true;
                beginBetaCityPreparationTransition();
            } else if (action.equals("TRIGGER_ENDING")) {
                triggerEndingSequence();
            } else if (action.equals("START_CORRUPTION_CUTSCENE")) {
                startCorruptionCutscene();
            } else if (action.equals("START_SEQUENTIAL_BATTLES")) {
                startSequentialBattles();
            }
        }

        postDialogueAction = null;
    }

    private void handleDialogueInputAndTyping(double deltaSeconds) {
        if (input.consumeJustPressed(KeyEvent.VK_ESCAPE)) {
            endDialogue(true);
            return;
        }
        if (dialogueController.getCurrentPage().getText().isEmpty()) {
            endDialogue(false);
            return;
        }
        dialogueController.update(deltaSeconds);

        if (autoCloseDialogueWhenFinished && dialogueController.isLineFinished()) {
            autoCloseDialogueTimer = Math.max(0.0, autoCloseDialogueTimer - deltaSeconds);
            if (autoCloseDialogueTimer <= 0.0) {
                endDialogue(false);
                return;
            }
        }

        if (input.consumeJustPressed(KeyEvent.VK_ENTER) || input.consumeJustPressed(KeyEvent.VK_SPACE) || input.consumeJustPressed(KeyEvent.VK_E)) {
            if (dialogueController.advance()) {
                endDialogue(false);
            }
        }
    }

    private void endDialogue(boolean interrupted) {
        if (activeNpc != null) {
            activeNpc.endInteraction();
        }
        if (!interrupted && pendingDialogueEndAction) {
            executeDialogueEndAction();
        } else if (interrupted) {
            interactionMessage = "Dialogue interrupted.";
        }

        activeNpc = null;
        if (gameState == GameState.DIALOGUE) {
            gameState = GameState.EXPLORATION;
        }
        if (!teleportInProgress) {
            movementLocked = false;
        }
    }

    private Npc getNearbyNpc(World world) {
        Npc npc = world.getClosestNpcInRange(player.getCenterX(), player.getCenterY(), INTERACT_DISTANCE);
        if (npc != null && "Glitch".equalsIgnoreCase(npc.getName()) && announcerDialogueActive) {
            return null;
        }
        return npc;
    }

    private void handleMenuInput() {
        World current = worlds[worldIndex];

        if (input.consumeJustPressed(KeyEvent.VK_ESCAPE) || input.consumeJustPressed(KeyEvent.VK_E)) {
            if (activeNpc != null) {
                activeNpc.endInteraction();
            }
            interactionMenuOpen = false;
            activeNpc = null;
            gameState = GameState.EXPLORATION;
            interactionMessage = "";
            return;
        }

        if (input.consumeJustPressed(KeyEvent.VK_1)) {
            if (isProfessorAlfred(activeNpc, current)) {
                interactionMessage = "Use the teleport door in the lab to enter DigiWorld.";
            } else if (activeNpc != null && hasDialogue(activeNpc)) {
                showNpcDialogue(activeNpc, current);
            }
            if (activeNpc != null) {
                activeNpc.endInteraction();
            }
            interactionMenuOpen = false;
            activeNpc = null;
            gameState = GameState.EXPLORATION;
            return;
        }

        if (input.consumeJustPressed(KeyEvent.VK_2)) {
            if (canGoPreviousWorld()) {
                worldIndex--;
                interactionMessage = "Teleported to " + worlds[worldIndex].getName();
                teleportWithFade(worldIndex);
            }
            if (activeNpc != null) {
                activeNpc.endInteraction();
            }
            interactionMenuOpen = false;
            activeNpc = null;
            gameState = GameState.EXPLORATION;
        }
    }

    // --- LEVEL UP SYSTEM FIX: Creates real objects, saves them to bag ---
    private void handleStarterSelectionInput() {
        if (input.consumeJustPressed(KeyEvent.VK_ESCAPE)) {
            interactionMessage = "Select exactly 3 Mecha Beasts to continue.";
            return;
        }
        if (input.consumeJustPressed(KeyEvent.VK_LEFT) || input.consumeJustPressed(KeyEvent.VK_A)) {
            starterSelectionIndex = (starterSelectionIndex - 1 + starterChoices.length) % starterChoices.length;
            return;
        }
        if (input.consumeJustPressed(KeyEvent.VK_RIGHT) || input.consumeJustPressed(KeyEvent.VK_D)) {
            starterSelectionIndex = (starterSelectionIndex + 1) % starterChoices.length;
            return;
        }
        if (input.consumeJustPressed(KeyEvent.VK_UP) || input.consumeJustPressed(KeyEvent.VK_W)) {
            starterSelectionIndex = (starterSelectionIndex - 1 + starterChoices.length) % starterChoices.length;
            return;
        }
        if (input.consumeJustPressed(KeyEvent.VK_DOWN) || input.consumeJustPressed(KeyEvent.VK_S)) {
            starterSelectionIndex = (starterSelectionIndex + 1) % starterChoices.length;
            return;
        }
        if (input.consumeJustPressed(KeyEvent.VK_1)) starterSelectionIndex = 0;
        if (input.consumeJustPressed(KeyEvent.VK_2) && starterChoices.length > 1) starterSelectionIndex = 1;
        if (input.consumeJustPressed(KeyEvent.VK_3) && starterChoices.length > 2) starterSelectionIndex = 2;
        if (input.consumeJustPressed(KeyEvent.VK_4) && starterChoices.length > 3) starterSelectionIndex = 3;
        if (input.consumeJustPressed(KeyEvent.VK_5) && starterChoices.length > 4) starterSelectionIndex = 4;
        if (input.consumeJustPressed(KeyEvent.VK_6) && starterChoices.length > 5) starterSelectionIndex = 5;
        if (input.consumeJustPressed(KeyEvent.VK_7) && starterChoices.length > 6) starterSelectionIndex = 6;
        if (input.consumeJustPressed(KeyEvent.VK_8) && starterChoices.length > 7) starterSelectionIndex = 7;
        if (input.consumeJustPressed(KeyEvent.VK_9) && starterChoices.length > 8) starterSelectionIndex = 8;
        if (input.consumeJustPressed(KeyEvent.VK_ENTER) || input.consumeJustPressed(KeyEvent.VK_SPACE)) {
            if (selectedStarterIndices.contains(starterSelectionIndex)) {
                selectedStarterIndices.remove(starterSelectionIndex);
                interactionMessage = starterChoices[starterSelectionIndex] + " removed. Pick 3 beasts.";
                return;
            }
            if (selectedStarterIndices.size() >= 3) {
                interactionMessage = "You can only choose 3 starters.";
                return;
            }
            selectedStarterIndices.add(starterSelectionIndex);
            if (selectedStarterIndices.size() < 3) {
                interactionMessage = starterChoices[starterSelectionIndex] + " selected (" + selectedStarterIndices.size() + "/3).";
                return;
            }

            for (Integer idx : selectedStarterIndices) {
                String beastName = starterChoices[idx];
                BattleCreature beast = BeastCatalog.createCreature(beastName);
                inventory.addBeast(beast);
            }

            prepareBattlePartyFromInventory();
            starterChosen = true;
            starterSelectionDone = true;
            hasGWatch = true;
            hasMechDriver = true;
            beastCards = Math.max(beastCards, 5);
            questManager.setQuestStage(QuestManager.STAGE_SELECTED_STARTERS);
            setObjective("Go to the teleport door");
            selectedStarterIndices.clear();
            interactionMessage = "G-Watch, Mech-driver, Beast-Cards, and selected Mecha Beasts received.";
            movementLocked = false;
            startPostStarterSelectionDialogue();
        }
    }

    private void startPostStarterSelectionDialogue() {
        Npc professor = findNpcByName(worlds[worldIndex], "Professor Alfred");
        if (professor != null) {
            activeNpc = professor;
            activeNpc.beginInteractionFacing(player.getCenterX(), player.getCenterY());
        }
        DialogueSequence script = DialogueFactory.createSequence(
                new String[]{"Professor Alfred", "Professor Alfred", "Professor Alfred", "Professor Alfred", PLAYER_NAME, "Professor Alfred", PLAYER_NAME},
                new String[]{
                        "Welcome to the beta test. Your first mission is to reach Alpha Village and challenge the Alpha Beast.",
                        "Your G Watch has the map, so just follow the route.",
                        "Since this is still in beta, you might encounter bugs. Report them if you do.",
                        "One more thing… you will feel pain, just like in real life. But remember, you will not die.",
                        "Wait… what? Pain? You didn’t say anything about that!",
                        "Good luck. The future of gaming and your legacy are in your hands.",
                        "Did he just cut me off? Ugh, fine. No turning back now."
                }
        );
        startDialogue(script, "SET_OBJECTIVE:Enter DigiWorld");
    }

    private Npc findNpcByName(World world, String name) {
        if (world == null || name == null || name.isBlank()) {
            return null;
        }
        for (Npc npc : world.getNpcs()) {
            if (npc != null && name.equalsIgnoreCase(npc.getName())) {
                return npc;
            }
        }
        return null;
    }

    private void handleEnemySelectionInput() {
        if (input.consumeJustPressed(KeyEvent.VK_ESCAPE)) {
            gameState = GameState.EXPLORATION;
            interactionMessage = "Battle cancelled.";
            return;
        }
        if (input.consumeJustPressed(KeyEvent.VK_LEFT) || input.consumeJustPressed(KeyEvent.VK_A)) {
            enemySelectionIndex = (enemySelectionIndex - 1 + enemyChoices.length) % enemyChoices.length;
            return;
        }
        if (input.consumeJustPressed(KeyEvent.VK_RIGHT) || input.consumeJustPressed(KeyEvent.VK_D)) {
            enemySelectionIndex = (enemySelectionIndex + 1) % enemyChoices.length;
            return;
        }
        if (input.consumeJustPressed(KeyEvent.VK_1)) enemySelectionIndex = 0;
        if (input.consumeJustPressed(KeyEvent.VK_2) && enemyChoices.length > 1) enemySelectionIndex = 1;
        if (input.consumeJustPressed(KeyEvent.VK_3) && enemyChoices.length > 2) enemySelectionIndex = 2;
        if (input.consumeJustPressed(KeyEvent.VK_4) && enemyChoices.length > 3) enemySelectionIndex = 3;
        if (input.consumeJustPressed(KeyEvent.VK_5) && enemyChoices.length > 4) enemySelectionIndex = 4;
        if (input.consumeJustPressed(KeyEvent.VK_6) && enemyChoices.length > 5) enemySelectionIndex = 5;
        if (input.consumeJustPressed(KeyEvent.VK_7) && enemyChoices.length > 6) enemySelectionIndex = 6;
        if (input.consumeJustPressed(KeyEvent.VK_8) && enemyChoices.length > 7) enemySelectionIndex = 7;
        if (input.consumeJustPressed(KeyEvent.VK_9) && enemyChoices.length > 8) enemySelectionIndex = 8;
        if (input.consumeJustPressed(KeyEvent.VK_0) && enemyChoices.length > 9) enemySelectionIndex = 9;
        if (input.consumeJustPressed(KeyEvent.VK_ENTER) || input.consumeJustPressed(KeyEvent.VK_SPACE)) {
            if (!prepareBattlePartyFromInventory()) {
                return;
            }
            battleSystem.setOwnedBeasts(inventory.getOwnedBeastNames());
            battleSystem.startWildBattle(enemyChoices[enemySelectionIndex]);
            gameState = GameState.BATTLE;
            interactionMessage = "";
        }
    }

    private void handleInventoryInput() {
        if (input.consumeJustPressed(KeyEvent.VK_ESCAPE) || input.consumeJustPressed(KeyEvent.VK_B)) {
            gameState = GameState.EXPLORATION;
            return;
        }
        if (input.consumeJustPressed(KeyEvent.VK_UP) || input.consumeJustPressed(KeyEvent.VK_W)) {
            inventory.moveSelection(-1);
            return;
        }
        if (input.consumeJustPressed(KeyEvent.VK_DOWN) || input.consumeJustPressed(KeyEvent.VK_S)) {
            inventory.moveSelection(1);
            return;
        }
        if (input.consumeJustPressed(KeyEvent.VK_E) || input.consumeJustPressed(KeyEvent.VK_ENTER) || input.consumeJustPressed(KeyEvent.VK_SPACE)) {
            interactionMessage = inventory.toggleEquippedSelected();
        }
    }

    // --- LEVEL UP SYSTEM FIX: Uses actual trained BattleCreature objects ---
    private boolean prepareBattlePartyFromInventory() {
        BattleCreature[] equipped = inventory.getEquippedBeasts();
        if (equipped == null || equipped.length < 1) {
            interactionMessage = "Your Mecha Beasts need recovery first.";
            gameState = GameState.EXPLORATION;
            return false;
        }
        if (equipped.length < 3 && questManager.atLeast(QuestManager.STAGE_SELECTED_STARTERS)) {
            interactionMessage = "Equip 3 beasts in backpack before battle.";
            gameState = GameState.EXPLORATION;
            return false;
        }
        battleSystem.setPlayerCreatures(equipped);
        return true;
    }

    private boolean canGoNextWorld() {
        return worldIndex < worlds.length - 1;
    }

    private boolean canGoPreviousWorld() {
        return worldIndex > 0;
    }

    private void respawnAtWorldStart() {
        World world = worlds[worldIndex];
        player.teleportToTile(world.getSpawnTileX(), world.getSpawnTileY());
        previousTileX = (int) player.getX() / TILE_SIZE;
        previousTileY = (int) player.getY() / TILE_SIZE;
        encounterCooldownTimer = 1.5;
    }

    public void teleportWithFade(int targetWorldIndex) {
        World world = worlds[targetWorldIndex];
        teleportWithFade(targetWorldIndex, world.getSpawnTileX(), world.getSpawnTileY());
    }

    public void teleportWithFade(int targetWorldIndex, int spawnTileX, int spawnTileY) {
        if (teleportInProgress) {
            return;
        }
        if (!canEnterWorld(targetWorldIndex)) {
            interactionMessage = "You are not ready yet.";
            return;
        }
        teleportInProgress = true;
        saveProgress();
        movementLocked = true;
        fadeTarget = 1.0;
        fadeAlpha = Math.max(fadeAlpha, 0.01);
        soundManager.playTeleportSound();
        new Thread(() -> {
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            synchronized (this) {
                worldIndex = targetWorldIndex;
                if (worldIndex == 1 && questManager.isStage(QuestManager.STAGE_SELECTED_STARTERS)) {
                    questManager.setQuestStage(QuestManager.STAGE_ENTERED_DIGIWORLD);
                    hiddenVineratopsTargetActive = false;
                    hiddenVineratopsEncounterTriggered = false;
                } else if (worldIndex == 2) {
                    if (pendingBetaIntroDialogue) {
                        questManager.setQuestStage(QuestManager.STAGE_RETURNED_TO_LAB); // stage 8 runtime
                        startBetaCityIntroDialogue();
                    }
                }
                World world = worlds[worldIndex];
                if (worldIndex == 1) {
                    rebuildValidEncounterBushes(world);
                } else if (worldIndex == 0 && pendingStage1ReturnCutscene) {
                    startStage1ReturnAndStage2Intro();
                }
                player.teleportToTile(spawnTileX, spawnTileY);
                previousTileX = (int) player.getX() / TILE_SIZE;
                previousTileY = (int) player.getY() / TILE_SIZE;
                encounterCooldownTimer = 1.5;
                camera.follow(player, world, LOGICAL_WIDTH, LOGICAL_HEIGHT, TILE_SIZE);
                soundManager.playWorldMusic(world.getName());
                fadeTarget = 0.0;
                teleportInProgress = false;
            }
        }).start();
    }

    private boolean canEnterWorld(int targetWorldIndex) {
        if (targetWorldIndex <= worldIndex) {
            return true;
        }
        if (targetWorldIndex == 1) {
            return questManager.isStage(QuestManager.STAGE_SELECTED_STARTERS) && hasRequiredStarterLoadout();
        }
        if (targetWorldIndex == 2) {
            return pendingBetaIntroDialogue || questManager.atLeast(QuestManager.STAGE_RETURNED_TO_LAB);
        }
        if (targetWorldIndex == 3) {
            return questManager.atLeast(QuestManager.STAGE_DEFEATED_TRIALMASTER);
        }
        if (targetWorldIndex == WorldIndex.HOUSE_1) {
            return true;
        }
        return false;
    }

    private void updateHometownTeleportDoor(World current) {
        if (!"Hometown".equalsIgnoreCase(current.getName())) {
            teleportDoorProximity = 0.0;
            teleportDoorLockedMessageShown = false;
            return;
        }
        int centerX = HOMETOWN_TELEPORT_DOOR_TILE_X * TILE_SIZE + TILE_SIZE / 2;
        int centerY = HOMETOWN_TELEPORT_DOOR_TILE_Y * TILE_SIZE + TILE_SIZE;
        double dx = player.getCenterX() - centerX;
        double dy = player.getCenterY() - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        teleportDoorProximity = Math.max(0.0, Math.min(1.0, 1.0 - (distance / (TILE_SIZE * 4.0))));
        if (!isInsideTeleportDoorTrigger()) {
            teleportDoorLockedMessageShown = false;
            return;
        }
        if (!questManager.atLeast(QuestManager.STAGE_SELECTED_STARTERS)) {
            if (!teleportDoorLockedMessageShown) {
                interactionMessage = "Teleport Door is locked. Complete Professor Alfred's setup first.";
                teleportDoorLockedMessageShown = true;
            }
            player.teleportToTile(HOMETOWN_TELEPORT_DOOR_TILE_X + 1, HOMETOWN_TELEPORT_DOOR_TILE_Y + 2);
            return;
        }
        if (!teleportInProgress && gameState == GameState.EXPLORATION) {
            teleportWithFade(1);
        }
    }

    private void maybeStartAlphaArrivalDialogue(World current) {
        if (!"World 2 - Alpha Village".equalsIgnoreCase(current.getName())) {
            return;
        }
        if (alphaArrivalDialogueDone || !questManager.isStage(QuestManager.STAGE_ENTERED_DIGIWORLD)) {
            return;
        }
        if (gameState != GameState.EXPLORATION || teleportInProgress || bossArenaActive) {
            return;
        }
        alphaArrivalDialogueDone = true;
        movementLocked = true;
        DialogueSequence script = DialogueFactory.createSequence(
                new String[]{"Player"},
                new String[]{"Hello? Anyone here?"}
        );
        startDialogue(script, "SET_OBJECTIVE:Find Chief Rei");
        autoCloseDialogueWhenFinished = true;
        autoCloseDialogueTimer = 0.8;
    }

    private void maybeStartLabReturnCutscene(World current) {
        if (!"Hometown".equalsIgnoreCase(current.getName())) {
            return;
        }
        if (!questManager.isStage(QuestManager.STAGE_DEFEATED_ALDRICH) || labReturnDialogueDone) {
            return;
        }
        if (gameState != GameState.EXPLORATION || teleportInProgress || bossArenaActive) {
            return;
        }
        Npc professor = findNpcByName(current, "Professor Alfred");
        if (professor != null) {
            activeNpc = professor;
            activeNpc.beginInteractionFacing(player.getCenterX(), player.getCenterY());
        }
        DialogueSequence script = DialogueFactory.createSequence(
                new String[]{"Professor Alfred", "You", "Professor Alfred", "You"},
                new String[]{
                        "Excellent work. Stage one is complete. And no bugs either. You handled yourself well.",
                        "Don’t act proud! You never told me the pain would feel real. I thought I was going to die!",
                        "But you did not. That is the point. This game is meant to be lived, not just played. Now rest. Tomorrow, the next test awaits.",
                        "Tch…"
                }
        );
        startDialogue(script, "SET_OBJECTIVE:Talk to Chief Rei|MARK_LAB_RETURN_DONE");
    }

    private void startStage1ReturnAndStage2Intro() {
        pendingStage1ReturnCutscene = false;
        movementLocked = true;
        DialogueSequence script = DialogueFactory.createSequence(
                new String[]{"Professor Alfred", "Player", "Professor Alfred", "Player", "Professor Alfred", "Player", "Professor Alfred"},
                new String[]{
                        "Excellent work. Stage one is complete. And no bugs either. You handled yourself well.",
                        "Don’t act proud! You never told me the pain would feel real. I thought I was going to die!",
                        "But you did not. That is the point. This game is meant to be lived, not just played. Now rest. Tomorrow, the next test awaits.",
                        "Tch…",
                        "Welcome back, Player. We’re about to begin the full test. Are you ready?",
                        "Yes… somewhat.",
                        "Alright then, transport!"
                }
        );
        startDialogue(script, "STAGE2_TRANSPORT_TO_BETA");
    }

    private void startBetaCityIntroDialogue() {
        movementLocked = true;
        DialogueSequence script = DialogueFactory.createSequence(
                new String[]{"Professor Alfred", "Professor Alfred", "Player"},
                new String[]{
                        "You are now in Beta City! Your goal: defeat the Ace Trainer to earn a Challenge Ticket for the Tournament Trial.",
                        "Head to the Tournament Hall to register. Good luck, and hopefully… everything goes smoothly.",
                        "Alright! Let’s go!"
                }
        );
        startDialogue(script, "SET_OBJECTIVE:Defeat Ace Trainer Jazz");
        pendingBetaIntroDialogue = false;
    }

    private void updateAldrichProximityObjective(World current) {
        if (!"World 2 - Alpha Village".equalsIgnoreCase(current.getName())) {
            return;
        }
        if (!questManager.isStage(QuestManager.STAGE_COMPLETED_TUTORIAL) || aldrichObjectiveTriggered) {
            return;
        }
        Npc aldrich = findNpcByName(current, "Aldrich");
        if (aldrich == null) {
            return;
        }
        int playerTileX = player.getCenterX() / TILE_SIZE;
        int playerTileY = player.getCenterY() / TILE_SIZE;
        int aldrichTileX = aldrich.getCenterX() / TILE_SIZE;
        int aldrichTileY = aldrich.getCenterY() / TILE_SIZE;
        int dx = Math.abs(playerTileX - aldrichTileX);
        int dy = Math.abs(playerTileY - aldrichTileY);
        if (Math.max(dx, dy) <= 5) {
            setObjective("Defeat Aldrich");
            aldrichObjectiveTriggered = true;
        }
    }

    private boolean isInsideTeleportDoorTrigger() {
        int doorX = HOMETOWN_TELEPORT_DOOR_TILE_X * TILE_SIZE;
        int doorY = HOMETOWN_TELEPORT_DOOR_TILE_Y * TILE_SIZE;
        int triggerW = TILE_SIZE * 2;
        int triggerH = TILE_SIZE * 2;
        int px = player.getCenterX();
        int py = player.getCenterY();
        return px >= doorX && px <= doorX + triggerW && py >= doorY && py <= doorY + triggerH;
    }

    private void drawHometownTeleportDoorPlaceholder(Graphics2D g2d, World current) {
        if (!"Hometown".equalsIgnoreCase(current.getName())) {
            return;
        }
        int x = HOMETOWN_TELEPORT_DOOR_TILE_X * TILE_SIZE - camera.getX();
        int y = HOMETOWN_TELEPORT_DOOR_TILE_Y * TILE_SIZE - camera.getY();
        int w = TILE_SIZE * 2;
        int h = TILE_SIZE * 2;
        g2d.setColor(new Color(0, 0, 0, 210));
        g2d.fillRoundRect(x, y, w, h, 8, 8);
        g2d.setColor(new Color(60, 60, 80));
        g2d.drawRoundRect(x, y, w, h, 8, 8);
        double glow = Math.sin(windTimeSeconds * 3.0) * 0.3 + 0.7;
        g2d.setColor(new Color(60, 60, 80, (int) (80 * glow)));
        g2d.setStroke(new java.awt.BasicStroke(2f));
        g2d.drawRoundRect(x - 1, y - 1, w + 2, h + 2, 10, 10);
        g2d.setStroke(new java.awt.BasicStroke(1f));
        int opening = (int) Math.round((w - 12) * teleportDoorProximity);
        int energyX = x + (w - opening) / 2;
        g2d.setColor(new Color(40, 40, 60, 180));
        g2d.fillRoundRect(energyX, y + 7, opening, h - 14, 6, 6);
        g2d.setColor(new Color(120, 120, 140, 200));
        g2d.drawString("Teleport Door", x - 4, y - 4);
    }

    private void beginBetaCityPreparationTransition() {
        if (teleportInProgress) {
            return;
        }
        teleportInProgress = true;
        movementLocked = true;
        saveProgress();
        fadeTarget = 1.0;
        fadeAlpha = Math.max(fadeAlpha, 0.01);
        soundManager.playTeleportSound();
        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            synchronized (this) {
                teleportInProgress = false;
                teleportWithFade(2);
            }
        }).start();
    }

    private void startCorruptionCutscene() {
        corruptionCutsceneActive = true;
        corruptionCutsceneTimer = 0.0;
        corruptionShakeIntensity = 0.0;
        corruptionGlitchIntensity = 0.0;
        corruptionFadeProgress = 0.0;
        corruptionMapLoaded = false;
        movementLocked = true;
        soundManager.playGlitchSound();
    }

    private void updateCorruptionCutscene(double deltaSeconds) {
        if (!corruptionCutsceneActive) {
            return;
        }
        corruptionCutsceneTimer += deltaSeconds;
        double t = corruptionCutsceneTimer;

        if (t < 0.5) {
            corruptionShakeIntensity = t / 0.5 * 8.0;
        } else if (t < 2.0) {
            corruptionShakeIntensity = 8.0 + (t - 0.5) / 1.5 * 4.0;
            corruptionGlitchIntensity = Math.min(1.0, (t - 0.5) / 1.5);
        } else if (t < 3.0) {
            corruptionShakeIntensity = 12.0 * (1.0 - (t - 2.0) / 1.0);
            corruptionGlitchIntensity = 1.0;
            corruptionFadeProgress = Math.min(1.0, (t - 2.0) / 1.0);
        } else {
            corruptionShakeIntensity = 0.0;
            corruptionGlitchIntensity = 0.0;
            corruptionFadeProgress = 1.0;
            if (!corruptionMapLoaded) {
                corruptionMapLoaded = true;
                loadCorruptedBetaCity();
            }
        }
    }

    private void loadCorruptedBetaCity() {
        fadeTarget = 1.0;
        fadeAlpha = 1.0;
        new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            synchronized (this) {
                worldIndex = 3;
                corruptionCutsceneActive = false;
                corruptionShakeIntensity = 0.0;
                corruptionGlitchIntensity = 0.0;
                corruptionFadeProgress = 0.0;
                World world = worlds[worldIndex];
                player.teleportToTile(world.getSpawnTileX(), world.getSpawnTileY());
                previousTileX = (int) player.getX() / TILE_SIZE;
                previousTileY = (int) player.getY() / TILE_SIZE;
                encounterCooldownTimer = 1.5;
                camera.follow(player, world, LOGICAL_WIDTH, LOGICAL_HEIGHT, TILE_SIZE);
                soundManager.playWorldMusic(world.getName());
                questManager.setQuestStage(QuestManager.STAGE_TOURNAMENT_STARTED);
                setObjective("Find the source of the glitch");
                fadeTarget = 0.0;
                teleportInProgress = false;
                worldBannerTimer = 4.0;
                worldBannerAlpha = 0.0;
                movementLocked = false;
                startAnnouncerDialogue();
            }
        }).start();
    }

    private void startAnnouncerDialogue() {
        announcerSpeakers = new String[]{
                "Announcer", "Player",
                "Announcer",
                "Trialmaster",
                "Unknown Speaker",
                "G-Watch",
                "Player",
                "Professor Alfred",
                "Player"
        };
        announcerLines = new String[]{
                "IT'S TIME FOR THE TOURNA– BZZZT–KRZZHH–VRRRTTCHH—",
                "Huh? A glitch?",
                "BZZZZZT–KRZZZHHHH–VVVRRRTTTCCCHHHHHHH–KRRRRAAAASHHHH–SHHHHHHHHHHHHhhhhh",
                "THE CITY IS COLLAPSING!",
                "Professor, I'm logging you out for safety. Now it's time to destroy Beta City.",
                "*ringing*",
                "Professor, what's happening!?",
                "Someone hacked the game. I'm back in the real world. Player, I need you to stop them and stall for time so I can kick them out and restore the game!",
                "Alright! I'm not letting this world die!"
        };
        announcerDialogueActive = true;
        announcerPageIndex = 0;
        announcerDialogueController = new DialogueController();
        announcerDialogueController.start(DialogueFactory.createSequence(
                new String[]{announcerSpeakers[0]},
                new String[]{announcerLines[0]}
        ));
    }

    private void updateAnnouncerDialogue(double deltaSeconds) {
        if (!announcerDialogueActive) {
            return;
        }
        announcerDialogueController.update(deltaSeconds);
        if (input.consumeJustPressed(KeyEvent.VK_ESCAPE)) {
            announcerDialogueActive = false;
            return;
        }
        if (input.consumeJustPressed(KeyEvent.VK_ENTER) || input.consumeJustPressed(KeyEvent.VK_SPACE) || input.consumeJustPressed(KeyEvent.VK_E)) {
            if (announcerDialogueController.advance()) {
                announcerPageIndex++;
                if (announcerPageIndex < announcerSpeakers.length) {
                    announcerDialogueController.start(DialogueFactory.createSequence(
                            new String[]{announcerSpeakers[announcerPageIndex]},
                            new String[]{announcerLines[announcerPageIndex]}
                    ));
                } else {
                    announcerDialogueActive = false;
                }
            }
        }
    }

    private void updateTrialmasterProximityObjective(World current) {
        if (!"World 3 - Beta City".equalsIgnoreCase(current.getName())) {
            return;
        }
        if (!questManager.isStage(QuestManager.STAGE_ENTERED_BETA_CITY)) {
            return;
        }
        Npc trialmaster = findNpcByName(current, "Trialmaster");
        if (trialmaster == null) {
            return;
        }
        int playerTileX = player.getCenterX() / TILE_SIZE;
        int playerTileY = player.getCenterY() / TILE_SIZE;
        int npcTileX = trialmaster.getCenterX() / TILE_SIZE;
        int npcTileY = trialmaster.getCenterY() / TILE_SIZE;
        int dx = Math.abs(playerTileX - npcTileX);
        int dy = Math.abs(playerTileY - npcTileY);
        if (Math.max(dx, dy) <= 5) {
            questManager.setQuestStage(QuestManager.STAGE_ENTERED_TOURNAMENT_HALL);
            setObjective("Challenge Trialmaster");
        }
    }

    private void startSequentialBattles() {
        sequentialBattlesActive = true;
        sequentialBattleIndex = 0;
        sequentialBattleOrder = new String[]{"Nokami", "Shadefox", "Kyoflare"};
        startNextSequentialBattle();
    }

    private void startNextSequentialBattle() {
        if (sequentialBattleIndex >= sequentialBattleOrder.length) {
            sequentialBattlesActive = false;
            triggerFinalGlitchBossBattle();
            return;
        }
        String beast = sequentialBattleOrder[sequentialBattleIndex];
        if (!prepareBattlePartyFromInventory()) {
            sequentialBattlesActive = false;
            return;
        }
        bossArenaActive = true;
        currentBossWorldIndex = worldIndex;
        currentBossName = beast.toLowerCase();
        battleSystem.setOwnedBeasts(inventory.getOwnedBeastNames());
        battleSystem.startWildBattle(beast);
        gameState = GameState.BATTLE;
        soundManager.playCombatMusic();
    }

    private void triggerFinalGlitchBossBattle() {
        DialogueSequence script = DialogueFactory.createSequence(
                new String[]{"Glitch", "Player"},
                new String[]{
                        "BEAST CARD ON! HENSHIN!",
                        "Let's do this!"
                }
        );
        startDialogue(script, "START_BOSS:glitch_woltrix");
    }

    private void handleGlitchBossVictory() {
        questManager.setQuestStage(QuestManager.STAGE_DEFEATED_GLITCH);
        setObjective("Return to the Real World");
        corruptionShakeIntensity = 0.0;
        corruptionGlitchIntensity = 0.0;
        activeNpc = findNpcByName(worlds[3], "Glitch");
        DialogueSequence script = DialogueFactory.createSequence(
                new String[]{"Glitch", "Player", "Professor Alfred", "Player"},
                new String[]{
                        "You're as good as they say. I'll keep my promise—but soon you'll know the truth.",
                        "I'll be ready.",
                        "Transporting you back! We've found the culprit, but we can't go to the authorities. Check your mail; we'll meet somewhere safe.",
                        "Wonder what will happen now, well I'll just go to sleep for now."
                }
        );
        startDialogue(script, "TRIGGER_ENDING");
    }

    private void triggerEndingSequence() {
        movementLocked = true;
        fadeTarget = 1.0;
        fadeAlpha = Math.max(fadeAlpha, 0.01);
        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            synchronized (this) {
                questManager.setQuestStage(QuestManager.STAGE_ENDING_TRIGGERED);
                saveProgress();
                movementLocked = false;
                interactionMessage = "Thanks for playing DigiWorld!";
            }
        }).start();
    }

    private boolean hasRequiredStarterLoadout() {
        if (!hasGWatch || !hasMechDriver || inventory.getOwnedBeastNames().size() < 3) {
            return false;
        }
        return inventory.getEquippedBeastNames().length >= 3;
    }

    private void saveProgress() {
        Map<String, String> data = new HashMap<>();
        data.put("worldIndex", String.valueOf(worldIndex));
        data.put("questStage", String.valueOf(questManager.getQuestStage()));
        data.put("objective", currentObjective == null ? "" : currentObjective);
        data.put("starterChosen", String.valueOf(starterChosen));
        data.put("starterSelectionDone", String.valueOf(starterSelectionDone));
        data.put("hasTalkedToProfessor", String.valueOf(hasTalkedToProfessor));
        data.put("hasTalkedToGeneral", String.valueOf(hasTalkedToGeneral));
        data.put("hasTalkedToChiefRei", String.valueOf(hasTalkedToChiefRei));
        data.put("hasChallengeTicket", String.valueOf(hasChallengeTicket));
        data.put("trialCompleted", String.valueOf(trialCompleted));
        data.put("alphaBossDefeated", String.valueOf(alphaBossDefeated));
        data.put("aceJazzDefeated", String.valueOf(aceJazzDefeated));
        data.put("collapseStarted", String.valueOf(collapseStarted));
        data.put("battledWoltrix", String.valueOf(battledWoltrix));
        data.put("hasGWatch", String.valueOf(hasGWatch));
        data.put("hasMechDriver", String.valueOf(hasMechDriver));
        data.put("beastCards", String.valueOf(beastCards));
        data.put("ownedBeasts", String.join(",", inventory.getOwnedBeastNames()));
        data.put("equippedBeasts", String.join(",", inventory.getEquippedBeastNames()));
        SaveManager.save(data);
    }


    private World[] createWorlds() {
        String aldrichBase = findAldrichSpriteBaseDir();
        World[] built = new World[]{
                new World("Hometown", 46, 36, TILE_SIZE, 23, 18, new Npc[]{
                        new Npc(
                                "Professor Alfred",
                                TILE_SIZE, TILE_SIZE,
                                new Color(214, 93, 177),
                                new int[][]{{24, 18}, {26, 18}, {26, 20}, {24, 20}},
                                "res/characters/professor-alfred/profalfred-fw.png",
                                "res/characters/professor-alfred/profalfred-b.png",
                                "res/characters/professor-alfred/profalfred-l.png",
                                "res/characters/professor-alfred/profalfred-r.png"
                        ),
                        new Npc(
                                "General Edrian",
                                TILE_SIZE, TILE_SIZE,
                                new Color(245, 132, 92),
                                new int[][]{{20, 17}, {18, 17}, {18, 20}, {20, 20}},
                                "res/characters/gen-ed/gened-fw.png",
                                "res/characters/gen-ed/gened-b.png",
                                "res/characters/gen-ed/gened-l.png",
                                "res/characters/gen-ed/gened-r.png"
                        )
                }),
                new World("World 2 - Alpha Village", 50, 38, TILE_SIZE, 25, 19, new Npc[]{
                        new Npc(
                                "Chief Rei",
                                TILE_SIZE, TILE_SIZE,
                                new Color(93, 177, 214),
                                new int[][]{{26, 19}, {28, 19}, {28, 21}, {26, 21}},
                                "res/characters/chief-rei/chiefrei-fw.png",
                                "res/characters/chief-rei/chiefrei-b.png",
                                "res/characters/chief-rei/chiefrei-l.png",
                                "res/characters/chief-rei/chiefrei-r.png"
                        ),
                        new Npc(
                                "Aldrich",
                                TILE_SIZE, TILE_SIZE,
                                new Color(200, 80, 80),
                                new int[][]{
                                        {digiworld.maps.AlphaVillageTileMap.HEART_CENTER_X, digiworld.maps.AlphaVillageTileMap.HEART_CENTER_Y},
                                        {digiworld.maps.AlphaVillageTileMap.HEART_CENTER_X, digiworld.maps.AlphaVillageTileMap.HEART_CENTER_Y},
                                        {digiworld.maps.AlphaVillageTileMap.HEART_CENTER_X, digiworld.maps.AlphaVillageTileMap.HEART_CENTER_Y},
                                        {digiworld.maps.AlphaVillageTileMap.HEART_CENTER_X, digiworld.maps.AlphaVillageTileMap.HEART_CENTER_Y}
                                },
                                aldrichBase + "/aldrich-fw.png",
                                aldrichBase + "/aldrich-b.png",
                                aldrichBase + "/aldrich-l.png",
                                aldrichBase + "/aldrich-r.png"
                        )
                }),
                new World("World 3 - Beta City", 56, 42, TILE_SIZE, 28, 21, new Npc[]{
                        new Npc(
                                "Ace Jazz",
                                TILE_SIZE, TILE_SIZE,
                                new Color(230, 160, 75),
                                new int[][]{{28, 27}, {28, 27}, {28, 27}, {28, 27}},
                                "res/characters/ace-jazz/acejazz-fw.png",
                                "res/characters/ace-jazz/acejazz-b.png",
                                "res/characters/ace-jazz/acejazz-l.png",
                                "res/characters/ace-jazz/acejazz-r.png"
                        ),
                        new Npc(
                                "Trialmaster",
                                TILE_SIZE, TILE_SIZE,
                                new Color(180, 130, 214),
                                new int[][]{{28, 12}, {28, 12}, {28, 12}, {28, 12}},
                                "res/characters/trialmaster/trialmaster-fw.png",
                                "res/characters/trialmaster/trialmaster-b.png",
                                "res/characters/trialmaster/trialmaster-l.png",
                                "res/characters/trialmaster/trialmaster-r.png"
                        )
                }),
                new World("Corrupted Beta City", 56, 42, TILE_SIZE, 28, 21, new Npc[]{
                        new Npc(
                                "Glitch",
                                TILE_SIZE, TILE_SIZE,
                                new Color(128, 214, 104),
                                new int[][]{{28, 21}, {28, 21}, {28, 21}, {28, 21}},
                                "res/characters/glitch-ron/glitchron-fw.png",
                                "res/characters/glitch-ron/glitchron-b.png",
                                "res/characters/glitch-ron/glitchron-l.png",
                                "res/characters/glitch-ron/glitchron-r.png"
                        )
                }),
                new World("House 1", 46, 36, TILE_SIZE, 25, 19, new Npc[]{})
        };
        for (World world : built) {
            WorldTileMapRegistry.apply(world);
        }
        return built;
    }

    private String findAldrichSpriteBaseDir() {
        File root = new File("res/characters");
        if (!root.exists() || !root.isDirectory()) {
            return "res/characters/aldrich";
        }
        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs == null) {
            return "res/characters/aldrich";
        }
        for (File dir : dirs) {
            String lower = dir.getName().toLowerCase();
            if (!lower.contains("aldrich")) {
                continue;
            }
            File fw = new File(dir, "aldrich-fw.png");
            if (fw.exists()) {
                return dir.getPath().replace("\\", "/");
            }
        }
        return "res/characters/aldrich";
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D scene = frameBuffer.createGraphics();
        scene.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        scene.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        scene.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        scene.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        World current = worlds[worldIndex];
        if (gameState == GameState.BATTLE) {
            battleSystem.render(scene, LOGICAL_WIDTH, LOGICAL_HEIGHT);
        } else {
            current.renderTiles(scene, camera, TILE_SIZE, LOGICAL_WIDTH, LOGICAL_HEIGHT, windTimeSeconds);
            current.renderWindLines(scene, camera, LOGICAL_WIDTH, LOGICAL_HEIGHT, windTimeSeconds);
            current.drawStructuresBefore(scene, camera, (int) player.getY() + player.getSize());
            for (Npc npc : current.getNpcs()) {
                if ("Glitch".equalsIgnoreCase(npc.getName()) && announcerDialogueActive) continue;
                npc.render(scene, camera);
            }
            renderParticles(scene, true);
            player.render(scene, camera);
            renderParticles(scene, false);
            current.drawStructuresAfter(scene, camera, (int) player.getY() + player.getSize());
            drawDebugHitboxes(scene);
            drawScanMarker(scene);
            drawHometownTeleportDoorPlaceholder(scene, current);
            if (bossArenaActive) {
                drawBossArena(scene, current);
            }
            drawNearbyNpcNametags(scene, current);
            drawHud(scene, current);
            drawCoordinateHud(scene, current);
            drawNpcSpeechBubble(scene);
            drawCorruptionOverlay(scene);
            if (gameState == GameState.DIALOGUE) {
                drawDialogueAboveNpc(scene, current);
            }
            if (announcerDialogueActive) {
                drawAnnouncerDialogue(scene);
            }
            if (interactionMenuOpen) {
                drawMenu(scene);
            }
            if (gameState == GameState.INVENTORY) {
                inventory.render(scene, LOGICAL_WIDTH, LOGICAL_HEIGHT);
            } else if (gameState == GameState.STARTER_SELECT) {
                drawRpgBeastSelection(scene, "Professor Alfred", "Choose 3 out of 10 Mecha Beasts", starterChoices, starterSelectionIndex);
            } else if (gameState == GameState.ENEMY_SELECT) {
                drawRpgBeastSelection(scene, "Wild Bush Encounter", "Choose which beast to fight", enemyChoices, enemySelectionIndex);
            }
        }
        scene.dispose();

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int shakeX = 0;
        int shakeY = 0;
        if (corruptionShakeIntensity > 0.5) {
            shakeX = (int) ((random.nextDouble() - 0.5) * corruptionShakeIntensity * 3);
            shakeY = (int) ((random.nextDouble() - 0.5) * corruptionShakeIntensity * 3);
        }
        if (gameState == GameState.DIALOGUE && activeNpc != null) {
            drawZoomedDialogueScene(g2d);
        } else {
            g2d.drawImage(frameBuffer, shakeX, shakeY, SCREEN_WIDTH, SCREEN_HEIGHT, null);
        }
        if (fadeAlpha > 0.01) {
            g2d.setColor(new Color(0, 0, 0, (int) (255 * fadeAlpha)));
            g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        }
        g2d.dispose();
    }

    private void drawZoomedDialogueScene(Graphics2D g2d) {
        double zoom = 1.18;
        int srcW = (int) Math.round(LOGICAL_WIDTH / zoom);
        int srcH = (int) Math.round(LOGICAL_HEIGHT / zoom);
        int npcScreenX = (int) activeNpc.getX() - camera.getX() + activeNpc.getSize() / 2;
        int playerScreenX = (int) player.getX() - camera.getX() + player.getSize() / 2;
        int npcScreenY = (int) activeNpc.getY() - camera.getY() + activeNpc.getSize() / 2;
        int playerScreenY = (int) player.getY() - camera.getY() + player.getSize() / 2;
        int focusX = (npcScreenX + playerScreenX) / 2;
        int focusY = (npcScreenY + playerScreenY) / 2;
        int sx1 = Math.max(0, Math.min(LOGICAL_WIDTH - srcW, focusX - srcW / 2));
        int sy1 = Math.max(0, Math.min(LOGICAL_HEIGHT - srcH, focusY - srcH / 2));
        int sx2 = sx1 + srcW;
        int sy2 = sy1 + srcH;
        g2d.drawImage(frameBuffer, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, sx1, sy1, sx2, sy2, null);
    }

    private void drawHud(Graphics2D g2d, World current) {
        double worldBannerAlpha = 1.0;
        if (worldBannerTimer > 0.0) {
            worldBannerTimer = Math.max(0.0, worldBannerTimer - 0.016);
            if (worldBannerTimer < 2.0) {
                worldBannerAlpha = worldBannerTimer / 2.0;
            }
        }
        uiRenderer.drawHud(
                g2d,
                current,
                interactionMenuOpen,
                getNearbyNpc(current) != null,
                interactionMessage,
                currentObjective,
                objectiveTextAlpha,
                objectiveCompleteAlpha,
                hasGWatch,
                scanUiFlashTimer > 0.0,
                worldBannerAlpha
        );
    }

    private void drawCoordinateHud(Graphics2D g2d, World current) {
        int playerPixelX = (int) Math.round(player.getX());
        int playerPixelY = (int) Math.round(player.getY());
        int tileX = player.getCenterX() / TILE_SIZE;
        int tileY = player.getCenterY() / TILE_SIZE;
        TileType currentTile = current.getTile(tileX, tileY);

        String[] lines = {
                current.getName(),
                "Tile: " + tileX + ", " + tileY,
                "Pixel: " + playerPixelX + ", " + playerPixelY,
                "Ground: " + currentTile.name()
        };

        g2d.setFont(UIFont.regular(10f));
        int paddingX = 8;
        int lineHeight = 12;
        int boxWidth = 0;
        for (String line : lines) {
            boxWidth = Math.max(boxWidth, g2d.getFontMetrics().stringWidth(line));
        }
        boxWidth += paddingX * 2;
        int boxHeight = 8 + lines.length * lineHeight;
        int boxX = LOGICAL_WIDTH - boxWidth - 10;
        int boxY = 10;

        g2d.setColor(new Color(0, 0, 0, 175));
        g2d.fillRoundRect(boxX + 2, boxY + 2, boxWidth, boxHeight, 8, 8);
        g2d.setColor(new Color(12, 18, 24, 220));
        g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);
        g2d.setColor(new Color(214, 229, 236, 220));
        g2d.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);

        for (int i = 0; i < lines.length; i++) {
            int textY = boxY + 16 + i * lineHeight;
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.drawString(lines[i], boxX + paddingX + 1, textY + 1);
            g2d.setColor(i == 0 ? new Color(255, 232, 150) : new Color(240, 244, 248));
            g2d.drawString(lines[i], boxX + paddingX, textY);
        }
    }

    private void spawnMovementParticles(World world, double beforeX, double beforeY) {
        double dx = player.getX() - beforeX;
        double dy = player.getY() - beforeY;
        double moveLen = Math.sqrt(dx * dx + dy * dy);
        if (moveLen < 0.01) {
            return;
        }

        int centerTileX = player.getCenterX() / TILE_SIZE;
        int centerTileY = player.getCenterY() / TILE_SIZE;
        TileType standingTile = world.getTile(centerTileX, centerTileY);
        double dirX = dx / moveLen;
        double dirY = dy / moveLen;

        if (standingTile == TileType.GRASS_BUSH) {
            if (random.nextDouble() < 0.75) {
                double emitDirX = dirX;
                double emitDirY = dirY;
                if (dirY > 0.2) {
                    emitDirX = -dirX;
                    emitDirY = -dirY;
                }
                double px = player.getCenterX() + emitDirX * (player.getSize() * 0.48);
                double py = player.getCenterY() + emitDirY * (player.getSize() * 0.38) - 6;
                spawnLeafParticle(px, py, emitDirX, emitDirY);
                if (random.nextDouble() < 0.5) {
                    spawnLeafParticle(px + random.nextDouble() * 6 - 3, py + random.nextDouble() * 4 - 2, emitDirX, emitDirY);
                }
            }
        } else if (standingTile == TileType.GRASS1 || standingTile == TileType.GRASS2 || standingTile == TileType.GRASS3) {
            if (random.nextDouble() < 0.55) {
                double footY = player.getY() + player.getSize() - 2;
                spawnFootParticle(player.getX() + 8 + random.nextDouble() * 6, footY);
                spawnFootParticle(player.getX() + player.getSize() - 14 + random.nextDouble() * 6, footY);
            }
        }
    }

    private void spawnLeafParticle(double x, double y, double dirX, double dirY) {
        double spreadX = (random.nextDouble() - 0.5) * 20.0;
        double spreadY = (random.nextDouble() - 0.5) * 12.0;
        addParticle(new Particle(
                x, y,
                dirX * (28 + random.nextDouble() * 20) + spreadX * 0.2,
                dirY * (12 + random.nextDouble() * 12) - 14 + spreadY * 0.2,
                0.32 + random.nextDouble() * 0.22,
                new Color(118, 204, 92, 210),
                3 + random.nextInt(3),
                false
        ));
    }

    private void spawnFootParticle(double x, double y) {
        addParticle(new Particle(
                x, y,
                (random.nextDouble() - 0.5) * 10.0,
                -6 - random.nextDouble() * 8.0,
                0.22 + random.nextDouble() * 0.18,
                new Color(95, 185, 76, 185),
                2 + random.nextInt(2),
                true
        ));
    }

    private void addParticle(Particle p) {
        synchronized (particles) {
            particles.add(p);
        }
    }

    private void updateParticles(double deltaSeconds) {
        synchronized (particles) {
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.life -= deltaSeconds;
                if (p.life <= 0) {
                    it.remove();
                    continue;
                }
                p.x += p.vx * deltaSeconds;
                p.y += p.vy * deltaSeconds;
                p.vy += 18.0 * deltaSeconds;
            }
        }
    }

    private void renderParticles(Graphics2D g2d, boolean behindPlayerLayer) {
        List<Particle> snapshot;
        synchronized (particles) {
            snapshot = new ArrayList<>(particles);
        }
        for (Particle p : snapshot) {
            if (p.behindPlayer != behindPlayerLayer) {
                continue;
            }
            int sx = (int) Math.round(p.x) - camera.getX();
            int sy = (int) Math.round(p.y) - camera.getY();
            if (sx < -6 || sy < -6 || sx > LOGICAL_WIDTH + 6 || sy > LOGICAL_HEIGHT + 6) {
                continue;
            }
            g2d.setColor(p.color);
            g2d.fillOval(sx, sy, p.size, p.size);
        }
    }

    private static class Particle {
        private double x;
        private double y;
        private double vx;
        private double vy;
        private double life;
        private final Color color;
        private final int size;
        private final boolean behindPlayer;

        private Particle(double x, double y, double vx, double vy, double life, Color color, int size, boolean behindPlayer) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.color = color;
            this.size = size;
            this.behindPlayer = behindPlayer;
        }
    }

    private void drawNearbyNpcNametags(Graphics2D g2d, World current) {
        if (interactionMenuOpen) {
            return;
        }
        Npc nearbyNpc = getNearbyNpc(current);
        if (nearbyNpc == null) {
            return;
        }
        drawNpcNametag(g2d, nearbyNpc);
        uiRenderer.drawInteractBubble(g2d, nearbyNpc, player, camera);
    }

    private void drawNpcNametag(Graphics2D g2d, Npc npc) {
        uiRenderer.drawNpcNametag(g2d, npc, camera);
    }

    private void showNpcSpeechBubble(Npc npc, String text, double durationSeconds) {
        if (npc == null || text == null || text.isBlank()) {
            return;
        }
        speechBubbleNpc = npc;
        speechBubbleText = text;
        speechBubbleTimer = Math.max(0.6, durationSeconds);
    }

    private void drawNpcSpeechBubble(Graphics2D g2d) {
        uiRenderer.drawNpcSpeechBubble(g2d, speechBubbleNpc, speechBubbleTimer, speechBubbleText, camera);
    }

    private void drawMenu(Graphics2D g2d) {
        uiRenderer.drawMenu(
                g2d,
                activeNpc,
                worlds[worldIndex],
                starterSelectionDone,
                canGoNextWorld(),
                canGoPreviousWorld(),
                activeNpc != null && hasDialogue(activeNpc),
                hasSeenDialogue(activeNpc)
        );
    }

    private boolean canTeleportNext(Npc npc, World world) {
        return npc != null
                && "Professor Alfred".equalsIgnoreCase(npc.getName());
    }

    private boolean isProfessorAlfred(Npc npc, World world) {
        return canTeleportNext(npc, world);
    }

    private boolean hasDialogue(Npc npc) {
        return npc != null && "General Edrian".equalsIgnoreCase(npc.getName());
    }

    private boolean isGenEd(Npc npc, World world) {
        return npc != null
                && "Hometown".equalsIgnoreCase(world.getName())
                && "General Edrian".equalsIgnoreCase(npc.getName());
    }

    private boolean hasSeenDialogue(Npc npc) {
        return npc != null && seenNpcDialogues.contains(npc.getName());
    }

    private void showNpcDialogue(Npc npc, World world) {
        if (npc == null) {
            return;
        }
        if ("General Edrian".equalsIgnoreCase(npc.getName()) && "Hometown".equalsIgnoreCase(world.getName())) {
            interactionMessage = "General Edrian: Gaming can build strategy and discipline. DigiWorld was made so gamers can think and move like true commanders.";
            seenNpcDialogues.add(npc.getName());
            return;
        }
        interactionMessage = npc.getName() + ": ...";
    }

    private void checkBossTrigger(World current) {
        if (bossArenaActive) {
            return;
        }

        String name = current.getName();
        int playerTileX = (int) player.getX() / TILE_SIZE;
        int playerTileY = (int) player.getY() / TILE_SIZE;
    }

    private void startBossBattle(World current, String bossName) {
        if (!isBossUnlockedByQuest(bossName)) {
            interactionMessage = "You are not ready yet.";
            return;
        }
        if (!prepareBattlePartyFromInventory()) {
            return;
        }
        bossArenaActive = true;
        currentBossWorldIndex = worldIndex;
        currentBossName = bossName == null ? null : bossName.trim().toLowerCase();
        interactionMessage = "Boss battle: " + bossName;
        battleSystem.setOwnedBeasts(inventory.getOwnedBeastNames());
        battleSystem.startWildBattle(bossName);
        gameState = GameState.BATTLE;
        soundManager.playCombatMusic();
    }

    private boolean isBossUnlockedByQuest(String bossName) {
        String normalized = bossName == null ? "" : bossName.trim().toLowerCase();
        if ("gekuma".equals(normalized) || "aldrich".equals(normalized)) {
            return questManager.atLeast(QuestManager.STAGE_COMPLETED_TUTORIAL);
        }
        if ("ace jazz".equals(normalized) || "pirrot".equals(normalized)) {
            return questManager.isStage(QuestManager.STAGE_RETURNED_TO_LAB);
        }
        if ("voltchu".equals(normalized) || "trialmaster".equals(normalized)) {
            return questManager.atLeast(QuestManager.STAGE_ENTERED_BETA_CITY) && inventory.hasItem("Challenge Ticket");
        }
        if ("woltrix".equals(normalized)) {
            return questManager.atLeast(QuestManager.STAGE_REACHED_GLITCH_AREA);
        }
        return true;
    }

    private void handleBossVictory() {
        if (currentBossWorldIndex < 0) {
            return;
        }

        if (sequentialBattlesActive) {
            sequentialBattleIndex++;
            if (sequentialBattleIndex < sequentialBattleOrder.length) {
                startNextSequentialBattle();
            } else {
                sequentialBattlesActive = false;
                bossArenaActive = false;
                currentBossWorldIndex = -1;
                currentBossName = null;
                triggerFinalGlitchBossBattle();
            }
            return;
        }

        if (currentBossWorldIndex == 1) {
            alphaBossDefeated = true;
            questManager.setQuestStage(QuestManager.STAGE_DEFEATED_ALDRICH);
            interactionMessage = "Teleportation instability detected...";
            DialogueSequence script = DialogueFactory.createSequence(
                    new String[]{"Aldrich"},
                    new String[]{
                            "What a strange challenger…"
                    }
            );
            startDialogue(script, "TELEPORT_TO_LAB_STAGE1");
        } else if (currentBossWorldIndex == 2) {
            if ("pirrot".equals(currentBossName)) {
                questManager.setQuestStage(QuestManager.STAGE_ENTERED_BETA_CITY);
                inventory.addItem("Challenge Ticket");
                DialogueSequence script = DialogueFactory.createSequence(
                        new String[]{"Ace Jazz"},
                        new String[]{"You've surpassed us all. Take this Challenge Ticket, you've earned it. With it, you're worthy of the Tournament Trial."}
                );
                startDialogue(script, "SET_OBJECTIVE:Go to the Tournament Hall");
            } else if ("voltchu".equals(currentBossName)) {
                questManager.setQuestStage(QuestManager.STAGE_DEFEATED_TRIALMASTER);
                setObjective("Wait for the Tournament to Begin");
                activeNpc = findNpcByName(worlds[2], "Trialmaster");
                DialogueSequence script = DialogueFactory.createSequence(
                        new String[]{"Trialmaster", "Trialmaster", "Trialmaster", "Trialmaster", "Player", "Trialmaster", "Trialmaster"},
                        new String[]{
                                "You've beaten me… and qualified for the tournament!",
                                "And yes, I'm the Professor. In this game, I'm \"Trialmaster.\"",
                                "I transported in to personally monitor the test and ensure no glitches slip through.",
                                "I sense something bad is coming, and I had to see it for myself instead of just observing you.",
                                "But why would something bad happen? You're a genius, you've planned everything for this game!",
                                "True, but there's been tension between the staff and the General even before the beta started.",
                                "Reasons I can't share with you… for now. Let's continue testing and see what unfolds."
                        }
                );
                startDialogue(script, "START_CORRUPTION_CUTSCENE");
            }
        } else if ("woltrix".equals(currentBossName)) {
            handleGlitchBossVictory();
        }

        bossArenaActive = false;
        currentBossWorldIndex = -1;
        currentBossName = null;
        saveProgress();
    }

    private void handleBossDefeat() {
        if (currentBossWorldIndex == 1 && questManager.isStage(QuestManager.STAGE_COMPLETED_TUTORIAL)) {
            interactionMessage = "Come back when your bond with your beasts is stronger, fool.";

            BattleCreature[] equipped = inventory.getEquippedBeasts();
            if (equipped != null && equipped.length >= 3) {
                battleSystem.setPlayerCreatures(equipped);
            }

            player.teleportToTile(25, 14);
            setObjective("Defeat Aldrich");
        }
        bossArenaActive = false;
        currentBossWorldIndex = -1;
        currentBossName = null;
    }

    private void triggerBetaCityTournament(World current) {
        interactionMessage = "Tournament started. Reach the Trial Hall.";
    }

    private void triggerCollapseEvent(World current) {
        collapseStarted = true;
        interactionMessage = "The Collapse has begun. Reach the final zone.";
    }

    private void checkWildEncounter(World current) {
        if (encounterCooldownTimer > 0.0) {
            return;
        }

        if ("World 2 - Alpha Village".equalsIgnoreCase(current.getName())
                && !questManager.isStage(QuestManager.STAGE_TALKED_CHIEF_REI)) {
            return;
        }

        if (questManager.isStage(QuestManager.STAGE_TALKED_CHIEF_REI)
                && hiddenVineratopsTargetActive
                && !hiddenVineratopsEncounterTriggered
                && "World 2 - Alpha Village".equalsIgnoreCase(current.getName())) {
            int playerTileX = player.getCenterX() / TILE_SIZE;
            int playerTileY = player.getCenterY() / TILE_SIZE;
            int dx = playerTileX - hiddenVineratopsTileX;
            int dy = playerTileY - hiddenVineratopsTileY;
            if (dx == 0 && dy == 0) {
                if (!prepareBattlePartyFromInventory()) {
                    return;
                }
                hiddenVineratopsEncounterTriggered = true;
                scanMarkerVisible = false;
                startVineratopsEncounterTransition();
                return;
            }
        }

        if ("World 2 - Alpha Village".equalsIgnoreCase(current.getName())) {
            return;
        }

        int currentTileX = (int) player.getX() / TILE_SIZE;
        int currentTileY = (int) player.getY() / TILE_SIZE;
        if (currentTileX == previousTileX && currentTileY == previousTileY) {
            return;
        }

        previousTileX = currentTileX;
        previousTileY = currentTileY;

        TileType standingTile = current.getTile(currentTileX, currentTileY);
        if (standingTile != TileType.GRASS_BUSH) {
            return;
        }

        if (!starterChosen) {
            return;
        }

        int encounterChancePercent = 100;
        if (random.nextInt(100) < encounterChancePercent) {
            if (!prepareBattlePartyFromInventory()) {
                return;
            }
            battleSystem.setOwnedBeasts(inventory.getOwnedBeastNames());
            battleSystem.startWildBattle(pickRandomWildEnemy());
            gameState = GameState.BATTLE;
            soundManager.playCombatMusic();
            if (activeNpc != null) {
                activeNpc.endInteraction();
            }
            interactionMenuOpen = false;
            activeNpc = null;
            interactionMessage = "";
        }
    }

    private void startVineratopsEncounterTransition() {
        movementLocked = true;
        vineratopsTrackedBattleActive = true;
        fadeTarget = 1.0;
        fadeAlpha = Math.max(fadeAlpha, 0.01);
        new Thread(() -> {
            try {
                Thread.sleep(450);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            synchronized (this) {
                battleSystem.setOwnedBeasts(inventory.getOwnedBeastNames());
                battleSystem.startWildBattle("Vineratops");
                gameState = GameState.BATTLE;
                interactionMessage = "Wild Vineratops appeared!";
                soundManager.playCombatMusic();
                fadeTarget = 0.0;
            }
        }).start();
    }

    private String pickRandomWildEnemy() {
        List<String> pool = new ArrayList<>();
        for (String enemy : enemyChoices) {
            if (!"All Mighty".equalsIgnoreCase(enemy)) {
                pool.add(enemy);
            }
        }
        if (pool.isEmpty()) {
            return "Voltchu";
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private void drawRpgBeastSelection(Graphics2D g2d, String title, String subtitle, String[] choices, int selectedIndex) {
        uiRenderer.drawRpgBeastSelection(g2d, title, subtitle, choices, selectedIndex, selectedStarterIndices, gameState);
    }

    private void drawDialogueAboveNpc(Graphics2D g2d, World current) {
        uiRenderer.drawDialogueAboveNpc(g2d, activeNpc, dialogueController, player, camera, current);
    }

    private String[] wrapAnnouncerText(Graphics2D g2d, String text, int maxWidthPx) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
        }
        java.awt.FontMetrics fm = g2d.getFontMetrics(UIFont.regular(10f));
        java.util.List<String> lines = new java.util.ArrayList<>();
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

    private void drawCorruptionOverlay(Graphics2D g2d) {
        String worldName = worlds[worldIndex].getName();
        boolean isCorrupted = "Corrupted Beta City".equalsIgnoreCase(worldName);
        double intensity = isCorrupted ? 1.0 : corruptionGlitchIntensity;
        if (intensity < 0.01) {
            return;
        }
        double flicker = Math.sin(windTimeSeconds * 12.0) * 0.5 + 0.5;
        if (random.nextDouble() < 0.08 * intensity) {
            int sliceY = random.nextInt(LOGICAL_HEIGHT);
            int sliceH = 2 + random.nextInt(8);
            int offset = (random.nextInt(20) - 10) * (int) flicker;
            g2d.drawImage(frameBuffer, offset, sliceY, offset + LOGICAL_WIDTH, sliceY + sliceH,
                    0, sliceY, LOGICAL_WIDTH, sliceY + sliceH, null);
        }
        if (random.nextDouble() < 0.04 * intensity) {
            g2d.setColor(new Color(255, 0, 0, (int) (40 * intensity)));
            g2d.fillRect(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);
        }
        if (random.nextDouble() < 0.06 * intensity) {
            int scanY = random.nextInt(LOGICAL_HEIGHT);
            g2d.setColor(new Color(255, 255, 255, (int) (60 * intensity)));
            g2d.fillRect(0, scanY, LOGICAL_WIDTH, 1 + random.nextInt(3));
        }
        if (random.nextDouble() < 0.03 * intensity) {
            int tileX = random.nextInt(LOGICAL_WIDTH / 32) * 32;
            int tileY = random.nextInt(LOGICAL_HEIGHT / 32) * 32;
            g2d.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256), 80));
            g2d.fillRect(tileX, tileY, 32, 32);
        }
    }

    private void drawAnnouncerDialogue(Graphics2D g2d) {
        if (!announcerDialogueActive || announcerDialogueController == null) {
            return;
        }
        DialoguePage page = announcerDialogueController.getCurrentPage();
        if (page == null) {
            return;
        }
        String speaker = page.getSpeaker() != null ? page.getSpeaker() : "";
        String typed = announcerDialogueController.getVisibleText();
        String[] wrapped = wrapAnnouncerText(g2d, typed, 300);
        int boxWidth = 320;
        int lineHeight = 14;
        int footerHeight = announcerDialogueController.isLineFinished() ? 16 : 0;
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
        int x = (LOGICAL_WIDTH - boxWidth) / 2;
        int y = 20;
        g2d.setColor(new Color(0, 0, 0, 220));
        g2d.fillRoundRect(x + 2, y + 2, boxWidth, boxHeight, 8, 8);
        g2d.fillRoundRect(x, y, boxWidth, boxHeight, 8, 8);
        g2d.setColor(new Color(230, 230, 240));
        g2d.drawRoundRect(x, y, boxWidth, boxHeight, 8, 8);
        g2d.setColor(new Color(255, 200, 100));
        g2d.setFont(UIFont.bold(11f));
        g2d.drawString(speaker, x + 10, y + 14);
        g2d.setColor(Color.WHITE);
        g2d.setFont(UIFont.regular(10f));
        for (int i = 0; i < visibleLines.length; i++) {
            g2d.drawString(visibleLines[i], x + 10, y + 32 + i * lineHeight);
        }
        if (announcerDialogueController.isLineFinished()) {
            g2d.setColor(new Color(200, 200, 215));
            g2d.drawString("ENTER/SPACE/E continue | ESC skip", x + 10, y + boxHeight - 6);
        }
    }

    private void drawBossArena(Graphics2D g2d, World current) {
        int arenaSize = TILE_SIZE * 6;
        int centerX = current.getWidth() / 2 * TILE_SIZE;
        int centerY = 6 * TILE_SIZE;

        int screenX1 = centerX - arenaSize - camera.getX();
        int screenY1 = centerY - arenaSize - camera.getY();
        int screenX2 = centerX + arenaSize - camera.getX();
        int screenY2 = centerY + arenaSize - camera.getY();

        g2d.setColor(new Color(255, 50, 50, 140));
        g2d.setStroke(new java.awt.BasicStroke(3));
        g2d.drawRect(screenX1, screenY1, arenaSize * 2, arenaSize * 2);

        double pulse = Math.sin(windTimeSeconds * 4.0) * 0.3 + 0.7;
        g2d.setColor(new Color(255, 50, 50, (int) (60 * pulse)));
        g2d.fillRect(screenX1, screenY1, arenaSize * 2, arenaSize * 2);
    }

    private void triggerGWatchScan(World current) {
        if (!hasGWatch || !questManager.isStage(QuestManager.STAGE_TALKED_CHIEF_REI)) {
            interactionMessage = "No scan target available.";
            return;
        }
        if (!hiddenVineratopsTargetActive) {
            BushTile target = selectEncounterBush(current);
            if (target == null) {
                interactionMessage = "Scan failed. Try again.";
                return;
            }
            hiddenVineratopsTileX = target.x;
            hiddenVineratopsTileY = target.y;
            hiddenVineratopsTargetActive = true;
            hiddenVineratopsEncounterTriggered = false;
        }
        scanMarkerVisible = true;
        scanMarkerTimer = 3.0;
        scanMarkerAlpha = 0.72;
        scanUiFlashTimer = 0.35;
        interactionMessage = "G-Watch scan complete. Move to the ping.";
    }

    private BushTile selectEncounterBush(World current) {
        if (!"World 2 - Alpha Village".equalsIgnoreCase(current.getName())) {
            return null;
        }
        rebuildValidEncounterBushes(current);
        if (validEncounterBushes.isEmpty()) {
            return new BushTile(digiworld.maps.AlphaVillageTileMap.TRACKED_BUSH_X, digiworld.maps.AlphaVillageTileMap.TRACKED_BUSH_Y);
        }
        return validEncounterBushes.get(0);
    }

    private void rebuildValidEncounterBushes(World current) {
        validEncounterBushes.clear();
        if (!"World 2 - Alpha Village".equalsIgnoreCase(current.getName())) {
            return;
        }
        WorldTileMapRegistry.apply(current);
        validEncounterBushes.add(new BushTile(digiworld.maps.AlphaVillageTileMap.TRACKED_BUSH_X, digiworld.maps.AlphaVillageTileMap.TRACKED_BUSH_Y));
    }

    private void updateScanMarker(double deltaSeconds) {
        if (!scanMarkerVisible) {
            return;
        }
        scanMarkerTimer = Math.max(0.0, scanMarkerTimer - deltaSeconds);
        if (scanMarkerTimer > 0.7) {
            scanMarkerAlpha = Math.min(0.72, scanMarkerAlpha + deltaSeconds * 1.8);
        } else {
            scanMarkerAlpha = Math.max(0.0, scanMarkerAlpha - deltaSeconds * 1.2);
        }
        if (scanMarkerTimer <= 0.0 || scanMarkerAlpha <= 0.01) {
            scanMarkerVisible = false;
            scanMarkerAlpha = 0.0;
        }
    }

    private void drawScanMarker(Graphics2D g2d) {
        if (!scanMarkerVisible || !hiddenVineratopsTargetActive || hiddenVineratopsTileX < 0 || hiddenVineratopsTileY < 0) {
            return;
        }
        int px = hiddenVineratopsTileX * TILE_SIZE + TILE_SIZE / 2 - camera.getX();
        int py = hiddenVineratopsTileY * TILE_SIZE + TILE_SIZE / 2 - camera.getY();
        if (px < -60 || py < -60 || px > LOGICAL_WIDTH + 60 || py > LOGICAL_HEIGHT + 60) {
            return;
        }

        Graphics2D c = (Graphics2D) g2d.create();
        c.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, (float) scanMarkerAlpha));
        double pulse = 0.85 + Math.sin(scanPulseTime * 2.2) * 0.15;
        int r = (int) Math.round(18 * pulse);
        c.setColor(new Color(255, 70, 70, 140));
        c.fillOval(px - r, py - r, r * 2, r * 2);
        c.setColor(new Color(255, 70, 70, 220));
        c.setStroke(new java.awt.BasicStroke(2f));
        c.drawOval(px - r - 6, py - r - 6, (r + 6) * 2, (r + 6) * 2);
        c.drawLine(px - 8, py, px + 8, py);
        c.drawLine(px, py - 8, px, py + 8);
        c.dispose();
    }

    private void drawDebugHitboxes(Graphics2D g) {
        int px = (int) player.getX() - camera.getX();
        int py = (int) player.getY() - camera.getY();
        int ps = player.getSize();
        g.setColor(Color.RED);
        g.drawRect(px, py, ps, ps);

        for (Structure s : worlds[worldIndex].getStructures()) {
            Rectangle r = s.getCollisionRect();
            int sx = r.x - camera.getX();
            int sy = r.y - camera.getY();
            g.setColor(Color.GREEN);
            g.drawRect(sx, sy, r.width, r.height);
        }

        // Door hitboxes
        for (DoorEntry d : doorManager.getDoors()) {
            Rectangle r = d.getRect();
            int dx = r.x - camera.getX();
            int dy = r.y - camera.getY();
            g.setColor(Color.BLUE);
            g.drawRect(dx, dy, r.width, r.height);
        }
    }

}

