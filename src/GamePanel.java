import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
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
    private final Inventory inventory;
    private final Random random;
    private int worldIndex;
    private final Player player;

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
    private static final String PLAYER_NAME = "Chief Rei";
    private static final boolean CHEATS_ENABLED = false;
    private final DialogueController dialogueController;
    private boolean dialogueFromGenEd;
    private boolean dialogueFromAlfredIntro;
    private boolean dialogueFromAlfredStarter;
    private boolean alfredIntroCompleted;
    private boolean genEdDialogueCompleted;
    private boolean starterSequenceCompleted;
    private int storyStage;
    private boolean pendingAlphaArrivalDialogue;
    private Npc speechBubbleNpc;
    private String speechBubbleText;
    private double speechBubbleTimer;

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
        inventory = new Inventory();
        random = new Random();
        worldIndex = 0;
        player = new Player(worlds[0].getSpawnTileX() * TILE_SIZE, worlds[0].getSpawnTileY() * TILE_SIZE, TILE_SIZE, input, TILE_SIZE);
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
                "Pirrot", "Gekuma", "Shadefox", "Kingmantis"
        };
        enemyChoices = new String[]{
                "Kyoflare", "Nokami", "Vineratops", "Voltchu", "Zyuugor",
                "Pirrot", "Gekuma", "Shadefox", "Kingmantis", "Woltrix"
        };
        starterSelectionIndex = 0;
        enemySelectionIndex = 0;
        starterChosen = false;
        selectedStarterIndices = new LinkedHashSet<>();
        dialogueController = new DialogueController();
        dialogueFromGenEd = false;
        dialogueFromAlfredIntro = false;
        dialogueFromAlfredStarter = false;
        alfredIntroCompleted = false;
        genEdDialogueCompleted = false;
        starterSequenceCompleted = false;
        storyStage = 0;
        pendingAlphaArrivalDialogue = false;
        speechBubbleNpc = null;
        speechBubbleText = "";
        speechBubbleTimer = 0.0;
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
        World current = worlds[worldIndex];

        if (gameState == GameState.BATTLE) {
            if (clickPending) {
                battleSystem.handleClick(pendingClickX, pendingClickY);
                clickPending = false;
            }
            battleSystem.update(deltaSeconds);
            String result = battleSystem.handleInput(input);
            if (!battleSystem.isActive()) {
                String caughtCreature = battleSystem.consumeCaughtCreatureName();
                if (caughtCreature != null && !caughtCreature.isBlank()) {
                    inventory.addBeast(caughtCreature);
                }
                gameState = GameState.EXPLORATION;
                interactionMenuOpen = false;
                activeNpc = null;
                interactionMessage = battleSystem.getMessage();
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
            if (pendingAlphaArrivalDialogue && "World 2 - Alpha Village".equalsIgnoreCase(current.getName())) {
                startAlphaArrivalDialogue(current);
                return;
            }
            if (input.consumeJustPressed(KeyEvent.VK_B)) {
                gameState = GameState.INVENTORY;
                interactionMessage = "";
                return;
            }
            double beforeX = player.getX();
            double beforeY = player.getY();
            player.update(deltaSeconds, current);
            spawnMovementParticles(current, beforeX, beforeY);
            Npc nearbyNpc = getNearbyNpc(current);
            if (input.consumeJustPressed(KeyEvent.VK_E) && nearbyNpc != null) {
                activeNpc = nearbyNpc;
                if (isProfessorAlfred(activeNpc, current)) {
                    if ("Hometown".equalsIgnoreCase(current.getName()) && storyStage == 0) {
                        DialogueSequence script = DialogueFactory.createSequence(
                                new String[]{"Prof Alfred", PLAYER_NAME, "Prof Alfred"},
                                new String[]{
                                        "Hello and welcome Mr. gaming prodigy and 10-time world champion " + PLAYER_NAME + " to the gaming lab where you will be testing our new revolutionary game \"DigiWorld\". Oh, where are my manners!? I'm Professor Ai-P, and I'm in charge of developing this game. You will be transported into a world full of creatures called \"Mecha Beasts\", fuse with them, create a bond and battle together.",
                                        "Thanks for the explanation, Professor, but may I know what is the reason the government funded to create this game?",
                                        "Go ask Gen Ed over there. He can explain that part better."
                                }
                        );
                        activeNpc.beginInteractionFacing(player.getCenterX(), player.getCenterY());
                        startDialogue(script, false, true, false);
                    } else if ("Hometown".equalsIgnoreCase(current.getName()) && storyStage == 2 && !starterSequenceCompleted) {
                        DialogueSequence script = DialogueFactory.createSequence(
                                new String[]{"Prof Alfred"},
                                new String[]{"Excellent! Now before we transport you, you will need this G-Watch, Mech-driver and Beast-Cards. Choose 3 out of these 10 Mecha Beasts."}
                        );
                        activeNpc.beginInteractionFacing(player.getCenterX(), player.getCenterY());
                        startDialogue(script, false, false, true);
                    } else if ("Hometown".equalsIgnoreCase(current.getName()) && starterSequenceCompleted) {
                        interactionMenuOpen = true;
                        gameState = GameState.NPC_MENU;
                        interactionMessage = "Prof Alfred: Choose destination.";
                    } else if ("World 2 - Alpha Village".equalsIgnoreCase(current.getName()) && starterSequenceCompleted) {
                        interactionMenuOpen = true;
                        gameState = GameState.NPC_MENU;
                        interactionMessage = "Prof Alfred: Choose destination.";
                    } else {
                        showNpcSpeechBubble(activeNpc, "I got nothing for you buddy", 2.6);
                        activeNpc.endInteraction();
                        activeNpc = null;
                    }
                } else if (isGenEd(activeNpc, current)) {
                    if (storyStage == 1) {
                        DialogueSequence script = DialogueFactory.createSequence(
                                new String[]{"Gen Ed", PLAYER_NAME},
                                new String[]{
                                        "I shall be the one to answer that question Mr. " + PLAYER_NAME + ", I was actually intrigued by gaming. Seeing players think like they're chess masters or generals and coordinate their upper body in tapping their devices to win, it gave me an idea. What if we create a game where you become the character and fight but in a digital world? This way gamers could play digital games while still being physically active, and they will be able to experience the feeling of being their character. This could create a new legacy for \"Gamers\".",
                                        "That doesn't fully answer my question but oh well. Let's get this started."
                                }
                        );
                        activeNpc.beginInteractionFacing(player.getCenterX(), player.getCenterY());
                        startDialogue(script, true, false, false);
                    } else {
                        showNpcSpeechBubble(activeNpc, "I got nothing for you buddy", 2.6);
                        activeNpc.endInteraction();
                        activeNpc = null;
                    }
                } else {
                    interactionMenuOpen = true;
                    gameState = GameState.NPC_MENU;
                    interactionMessage = activeNpc.getName() + " in " + current.getName() + ": Choose action.";
                }
            }
            checkWildEncounter(current);
        }

        if (gameState != GameState.BATTLE && gameState != GameState.DIALOGUE) {
            current.update(deltaSeconds);
        }
        updateCamera(current);
        updateParticles(deltaSeconds);
        updateSpeechBubble(deltaSeconds);

        input.endFrame();
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

    private void startDialogue(DialogueSequence sequence, boolean fromGenEd, boolean fromAlfredIntro, boolean fromAlfredStarter) {
        dialogueController.start(sequence);
        dialogueFromGenEd = fromGenEd;
        dialogueFromAlfredIntro = fromAlfredIntro;
        dialogueFromAlfredStarter = fromAlfredStarter;
        interactionMenuOpen = false;
        gameState = GameState.DIALOGUE;
    }

    private void startAlphaArrivalDialogue(World world) {
        Npc chiefRei = findNpcByName(world, "Chief Rei");
        if (chiefRei != null) {
            activeNpc = chiefRei;
            activeNpc.beginInteractionFacing(player.getCenterX(), player.getCenterY());
        }
        DialogueSequence script = DialogueFactory.createSequence(
                new String[]{"Prof Alfred", PLAYER_NAME, "Prof Alfred", PLAYER_NAME, PLAYER_NAME, "Chief Rei", PLAYER_NAME},
                new String[]{
                        "Welcome to the beta test. Your first mission is to reach Alpha Village and challenge the Alpha Beast. Your G Watch has the map, so just follow the route. Since this is still in beta, you might encounter bugs. Report them if you do. One more thing... you will feel pain, just like in real life. But remember, you will not die.",
                        "Wait... what? Pain? You didn't say anything about that!",
                        "Good luck. The future of gaming and your legacy are in your hands.",
                        "Did he just cut me off? Ugh, fine. No turning back now.",
                        "Hello? Anyone here?",
                        "Welcome, traveler. I am Chief Rei, guardian of this village. The Alpha Beast is beyond the Mystic Forest. Be prepared.",
                        "Thank you, Chief."
                }
        );
        pendingAlphaArrivalDialogue = false;
        startDialogue(script, false, false, false);
    }

    private Npc findNpcByName(World world, String name) {
        if (world == null || name == null) {
            return null;
        }
        for (Npc npc : world.getNpcs()) {
            if (name.equalsIgnoreCase(npc.getName())) {
                return npc;
            }
        }
        return null;
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
        if (!interrupted) {
            if (dialogueFromGenEd) {
                genEdDialogueCompleted = true;
                storyStage = 2;
            }
            if (dialogueFromAlfredIntro) {
                alfredIntroCompleted = true;
                storyStage = 1;
            }
            if (dialogueFromAlfredStarter) {
                gameState = GameState.STARTER_SELECT;
                interactionMessage = "";
                return;
            }
        } else {
            interactionMessage = "Dialogue interrupted.";
        }

        activeNpc = null;
        gameState = GameState.EXPLORATION;
    }

    private Npc getNearbyNpc(World world) {
        return world.getClosestNpcInRange(player.getCenterX(), player.getCenterY(), INTERACT_DISTANCE);
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
            if (isProfessorAlfred(activeNpc, current) && "Hometown".equalsIgnoreCase(current.getName()) && !starterSequenceCompleted) {
                gameState = GameState.STARTER_SELECT;
                interactionMenuOpen = false;
                interactionMessage = "Prof Alfred: Choose your 3 starter beasts.";
                return;
            } else if (isProfessorAlfred(activeNpc, current) && starterSequenceCompleted) {
                worldIndex = "Hometown".equalsIgnoreCase(current.getName()) ? 1 : 0;
                respawnAtWorldStart();
                interactionMessage = "Teleported to " + worlds[worldIndex].getName();
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
            if (isProfessorAlfred(activeNpc, current) && starterSequenceCompleted) {
                worldIndex = "Hometown".equalsIgnoreCase(current.getName()) ? 0 : 1;
                respawnAtWorldStart();
                interactionMessage = "Teleported to " + worlds[worldIndex].getName();
                if (activeNpc != null) {
                    activeNpc.endInteraction();
                }
                interactionMenuOpen = false;
                activeNpc = null;
                gameState = GameState.EXPLORATION;
                return;
            }
            if (activeNpc != null && hasDialogue(activeNpc)) {
                showNpcDialogue(activeNpc, current);
                if (activeNpc != null) {
                    activeNpc.endInteraction();
                }
                interactionMenuOpen = false;
                activeNpc = null;
                gameState = GameState.EXPLORATION;
                return;
            }
            if (canGoPreviousWorld()) {
                worldIndex--;
                respawnAtWorldStart();
                interactionMessage = "Teleported to " + worlds[worldIndex].getName();
            }
            if (activeNpc != null) {
                activeNpc.endInteraction();
            }
            interactionMenuOpen = false;
            activeNpc = null;
            gameState = GameState.EXPLORATION;
        }
    }

    private void handleStarterSelectionInput() {
        if (input.consumeJustPressed(KeyEvent.VK_ESCAPE)) {
            gameState = GameState.EXPLORATION;
            if (activeNpc != null) {
                activeNpc.endInteraction();
            }
            activeNpc = null;
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

            String[] party = new String[3];
            int i = 0;
            for (Integer idx : selectedStarterIndices) {
                String beast = starterChoices[idx];
                party[i++] = beast;
                inventory.addBeast(beast);
            }
            battleSystem.setPlayerParty(party);
            battleSystem.setStarterBeast(party[0]);
            starterChosen = true;
            starterSequenceCompleted = true;
            selectedStarterIndices.clear();
            interactionMessage = "3 starters chosen. Teleporting to Alpha Village.";
            worldIndex = 1;
            respawnAtWorldStart();
            pendingAlphaArrivalDialogue = true;
            gameState = GameState.EXPLORATION;
            if (activeNpc != null) {
                activeNpc.endInteraction();
            }
            activeNpc = null;
        }
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
        if (input.consumeJustPressed(KeyEvent.VK_ENTER)) {
            String selected = inventory.getSelectedBeast();
            if (!selected.isEmpty()) {
                interactionMessage = "Selected beast: " + selected;
            }
            gameState = GameState.EXPLORATION;
        }
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
    }

    private World[] createWorlds() {
        return new World[]{
                new World("Hometown", 46, 36, TILE_SIZE, 23, 18, new Npc[]{
                        new Npc(
                                "Prof Alfred",
                                TILE_SIZE,
                                TILE_SIZE,
                                new Color(214, 93, 177),
                                new int[][]{{24, 18}, {26, 18}, {26, 20}, {24, 20}},
                                "res/characters/professor-alfred/profalfred-fw.png",
                                "res/characters/professor-alfred/profalfred-b.png",
                                "res/characters/professor-alfred/profalfred-l.png",
                                "res/characters/professor-alfred/profalfred-r.png"
                        ),
                        new Npc(
                                "Gen Ed",
                                TILE_SIZE,
                                TILE_SIZE,
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
                                TILE_SIZE,
                                TILE_SIZE,
                                new Color(93, 177, 214),
                                new int[][]{{26, 19}, {28, 19}, {28, 21}, {26, 21}},
                                "res/characters/chief-rei/chiefrei-fw.png",
                                "res/characters/chief-rei/chiefrei-b.png",
                                "res/characters/chief-rei/chiefrei-l.png",
                                "res/characters/chief-rei/chiefrei-r.png"
                        ),
                        new Npc("Prof Alfred", TILE_SIZE, TILE_SIZE, new Color(214, 93, 177), new int[][]{{22, 18}, {24, 18}, {24, 20}, {22, 20}},
                                "res/characters/professor-alfred/profalfred-fw.png",
                                "res/characters/professor-alfred/profalfred-b.png",
                                "res/characters/professor-alfred/profalfred-l.png",
                                "res/characters/professor-alfred/profalfred-r.png"
                        ),
                        new Npc("Gen Ed", TILE_SIZE, TILE_SIZE, new Color(245, 132, 92), new int[][]{{20, 17}, {18, 17}, {18, 20}, {20, 20}},
                                "res/characters/gen-ed/gened-fw.png",
                                "res/characters/gen-ed/gened-b.png",
                                "res/characters/gen-ed/gened-l.png",
                                "res/characters/gen-ed/gened-r.png"
                        )
                }),
                new World("World 3 - Beta City", 56, 42, TILE_SIZE, 28, 21, new Npc[]{
                        new Npc("Mira", TILE_SIZE, TILE_SIZE, new Color(230, 160, 75), new int[][]{{29, 21}, {31, 21}, {31, 23}, {29, 23}})
                }),
                new World("World 4 - Collapse Zone", 60, 46, TILE_SIZE, 30, 23, new Npc[]{
                        new Npc("Kiro", TILE_SIZE, TILE_SIZE, new Color(128, 214, 104), new int[][]{{31, 23}, {33, 23}, {33, 25}, {31, 25}})
                })
        };
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
            for (Npc npc : current.getNpcs()) {
                npc.render(scene, camera);
            }
            player.render(scene, camera);
            renderParticles(scene);
            drawNearbyNpcNametags(scene, current);
            drawHud(scene, current);
            drawNpcSpeechBubble(scene);
            if (gameState == GameState.DIALOGUE && activeNpc != null) {
                drawDialogueAboveNpc(scene);
            }
            if (interactionMenuOpen) {
                drawMenu(scene);
            }
            if (gameState == GameState.INVENTORY) {
                inventory.render(scene, LOGICAL_WIDTH, LOGICAL_HEIGHT);
            } else if (gameState == GameState.STARTER_SELECT) {
                drawRpgBeastSelection(scene, "Professor Alfred", "Choose your starter beast", starterChoices, starterSelectionIndex);
            } else if (gameState == GameState.ENEMY_SELECT) {
                drawRpgBeastSelection(scene, "Wild Bush Encounter", "Choose which beast to fight", enemyChoices, enemySelectionIndex);
            }
        }
        scene.dispose();

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (gameState == GameState.DIALOGUE && activeNpc != null) {
            drawZoomedDialogueScene(g2d);
        } else {
            g2d.drawImage(frameBuffer, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, null);
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
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(8, 8, 340, 48);
        g2d.setColor(Color.WHITE);
        g2d.setFont(UIFont.regular(10f));
        g2d.drawString(current.getName(), 16, 26);
        g2d.drawString("Move: WASD/Arrows | Interact: E | Inventory: B", 16, 44);

        if (!interactionMenuOpen && getNearbyNpc(current) != null) {
            g2d.setColor(new Color(255, 255, 255, 230));
            g2d.drawString("Press E near NPC", 16, 64);
        }
        g2d.setColor(new Color(255, 255, 255, 230));
        g2d.drawString("Bush tile: choose an enemy beast to battle", 16, 82);
        if (!interactionMessage.isEmpty()) {
            g2d.setColor(new Color(255, 255, 255, 230));
            g2d.drawString(interactionMessage, 16, LOGICAL_HEIGHT - 12);
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
        particles.add(new Particle(
                x, y,
                dirX * (28 + random.nextDouble() * 20) + spreadX * 0.2,
                dirY * (12 + random.nextDouble() * 12) - 14 + spreadY * 0.2,
                0.32 + random.nextDouble() * 0.22,
                new Color(118, 204, 92, 210),
                3 + random.nextInt(3)
        ));
    }

    private void spawnFootParticle(double x, double y) {
        particles.add(new Particle(
                x, y,
                (random.nextDouble() - 0.5) * 10.0,
                -6 - random.nextDouble() * 8.0,
                0.22 + random.nextDouble() * 0.18,
                new Color(95, 185, 76, 185),
                2 + random.nextInt(2)
        ));
    }

    private void updateParticles(double deltaSeconds) {
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

    private void renderParticles(Graphics2D g2d) {
        for (Particle p : particles) {
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

        private Particle(double x, double y, double vx, double vy, double life, Color color, int size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.color = color;
            this.size = size;
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
    }

    private void drawNpcNametag(Graphics2D g2d, Npc npc) {
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

    private void showNpcSpeechBubble(Npc npc, String text, double durationSeconds) {
        if (npc == null || text == null || text.isBlank()) {
            return;
        }
        speechBubbleNpc = npc;
        speechBubbleText = text;
        speechBubbleTimer = Math.max(0.6, durationSeconds);
    }

    private void drawNpcSpeechBubble(Graphics2D g2d) {
        if (speechBubbleNpc == null || speechBubbleTimer <= 0.0 || speechBubbleText.isBlank()) {
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
        x = Math.max(6, Math.min(LOGICAL_WIDTH - boxWidth - 6, x));
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

    private void drawMenu(Graphics2D g2d) {
        int boxWidth = 360;
        int boxHeight = 110;
        int x = (LOGICAL_WIDTH - boxWidth) / 2;
        int y = LOGICAL_HEIGHT - boxHeight - 14;

        g2d.setColor(new Color(0, 0, 0, 220));
        g2d.fillRoundRect(x, y, boxWidth, boxHeight, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(x, y, boxWidth, boxHeight, 8, 8);
        g2d.setFont(UIFont.regular(12f));
        String menuNpcName = activeNpc != null ? activeNpc.getName() : "NPC";
        g2d.drawString(menuNpcName + " Menu", x + 14, y + 24);
        if (isProfessorAlfred(activeNpc, worlds[worldIndex]) && !starterSequenceCompleted) {
            g2d.drawString("1: Choose 3 starter beasts", x + 14, y + 50);
        } else if (isProfessorAlfred(activeNpc, worlds[worldIndex]) && starterSequenceCompleted) {
            if ("Hometown".equalsIgnoreCase(worlds[worldIndex].getName())) {
                g2d.drawString("1: Go to Alpha Village", x + 14, y + 50);
            } else {
                g2d.drawString("1: Go to Hometown", x + 14, y + 50);
            }
        } else if (activeNpc != null && canTeleportNext(activeNpc, worlds[worldIndex]) && canGoNextWorld()) {
            g2d.drawString("1: Go to Alpha Village", x + 14, y + 50);
        } else if (activeNpc != null && hasDialogue(activeNpc)) {
            g2d.drawString("1: Talk", x + 14, y + 50);
            g2d.drawString("2: See dialogue again", x + 14, y + 70);
        } else if (activeNpc != null && hasDialogue(activeNpc)) {
            g2d.drawString("2: " + (hasSeenDialogue(activeNpc) ? "See dialogue again" : "Talk"), x + 14, y + 70);
        } else if (canGoPreviousWorld()) {
            g2d.drawString("2: Go to previous world", x + 14, y + 70);
        }
        g2d.drawString("E or ESC: Close", x + 14, y + 90);
    }

    private boolean canTeleportNext(Npc npc, World world) {
        return npc != null
                && "Prof Alfred".equalsIgnoreCase(npc.getName());
    }

    private boolean isProfessorAlfred(Npc npc, World world) {
        return canTeleportNext(npc, world);
    }

    private boolean hasDialogue(Npc npc) {
        return npc != null && "Gen Ed".equalsIgnoreCase(npc.getName());
    }

    private boolean isGenEd(Npc npc, World world) {
        return npc != null
                && "Hometown".equalsIgnoreCase(world.getName())
                && "Gen Ed".equalsIgnoreCase(npc.getName());
    }

    private boolean hasSeenDialogue(Npc npc) {
        return npc != null && seenNpcDialogues.contains(npc.getName());
    }

    private void showNpcDialogue(Npc npc, World world) {
        if (npc == null) {
            return;
        }
        if ("Gen Ed".equalsIgnoreCase(npc.getName()) && "Hometown".equalsIgnoreCase(world.getName())) {
            interactionMessage = "Gen Ed: Gaming can build strategy and discipline. DigiWorld was made so gamers can think and move like true commanders.";
            seenNpcDialogues.add(npc.getName());
            return;
        }
        interactionMessage = npc.getName() + ": ...";
    }

    private void checkWildEncounter(World current) {
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
            gameState = GameState.ENEMY_SELECT;
            enemySelectionIndex = 0;
            if (activeNpc != null) {
                activeNpc.endInteraction();
            }
            interactionMenuOpen = false;
            activeNpc = null;
            interactionMessage = "";
        }
    }

    private void drawRpgBeastSelection(Graphics2D g2d, String title, String subtitle, String[] choices, int selectedIndex) {
        int boxWidth = Math.min(760, LOGICAL_WIDTH - 40);
        int boxHeight = Math.min(420, LOGICAL_HEIGHT - 30);
        int x = (LOGICAL_WIDTH - boxWidth) / 2;
        int y = (LOGICAL_HEIGHT - boxHeight) / 2;

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
            g2d.drawString("WASD/Arrows/1-9 move | ENTER add/remove | Pick 3 beasts | ESC cancel", x + 16, y + boxHeight - 10);
        } else {
            g2d.drawString("A/D or 1-3 choose | ENTER confirm | ESC cancel", x + 16, y + boxHeight - 10);
        }
    }

    private void drawDialogueAboveNpc(Graphics2D g2d) {
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
        boolean speakerIsPlayer = PLAYER_NAME.equalsIgnoreCase(currentSpeaker);
        int anchorX = speakerIsPlayer
                ? (int) player.getX() - camera.getX() + player.getSize() / 2
                : (int) activeNpc.getX() - camera.getX() + activeNpc.getSize() / 2;
        int anchorY = speakerIsPlayer
                ? (int) player.getY() - camera.getY()
                : (int) activeNpc.getY() - camera.getY();
        int x = anchorX - boxWidth / 2;
        int y = anchorY - boxHeight - 22;
        x = Math.max(8, Math.min(LOGICAL_WIDTH - boxWidth - 8, x));
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

}
