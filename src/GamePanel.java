import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
        } else {
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
                activeNpc.beginInteractionFacing(player.getCenterX(), player.getCenterY());
                interactionMenuOpen = true;
                gameState = GameState.NPC_MENU;
                if (hasDialogue(activeNpc)) {
                    interactionMessage = hasSeenDialogue(activeNpc)
                            ? activeNpc.getName() + ": Want to see my dialogue again?"
                            : activeNpc.getName() + ": I have something to tell you.";
                } else {
                    interactionMessage = activeNpc.getName() + " in " + current.getName() + ": Choose action.";
                }
            }
            checkWildEncounter(current);
        }

        if (gameState != GameState.BATTLE) {
            current.update(deltaSeconds);
            camera.follow(player, current, LOGICAL_WIDTH, LOGICAL_HEIGHT, TILE_SIZE);
        }
        updateParticles(deltaSeconds);

        input.endFrame();
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
            if (canTeleportNext(activeNpc, current) && canGoNextWorld()) {
                worldIndex++;
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
                new World("World 2 - Beta City", 50, 38, TILE_SIZE, 25, 19, new Npc[]{
                        new Npc("Jax", TILE_SIZE, TILE_SIZE, new Color(93, 177, 214), new int[][]{{26, 19}, {28, 19}, {28, 21}, {26, 21}})
                }),
                new World("World 3 - Gamma Ruins", 56, 42, TILE_SIZE, 28, 21, new Npc[]{
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
            if (interactionMenuOpen) {
                drawMenu(scene);
            }
            if (gameState == GameState.INVENTORY) {
                inventory.render(scene, LOGICAL_WIDTH, LOGICAL_HEIGHT);
            }
        }
        scene.dispose();

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.drawImage(frameBuffer, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, null);
        g2d.dispose();
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
        g2d.drawString("Wild battles trigger in bush tiles", 16, 82);
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
        if (activeNpc != null && canTeleportNext(activeNpc, worlds[worldIndex]) && canGoNextWorld()) {
            g2d.drawString("1: Go to Beta City", x + 14, y + 50);
        } else if (activeNpc != null && hasDialogue(activeNpc)) {
            g2d.drawString("1: Talk", x + 14, y + 50);
        }
        if (activeNpc != null && hasDialogue(activeNpc)) {
            g2d.drawString("2: " + (hasSeenDialogue(activeNpc) ? "See dialogue again" : "Talk"), x + 14, y + 70);
        } else if (canGoPreviousWorld()) {
            g2d.drawString("2: Go to previous world", x + 14, y + 70);
        }
        g2d.drawString("E or ESC: Close", x + 14, y + 90);
    }

    private boolean canTeleportNext(Npc npc, World world) {
        return npc != null
                && "Hometown".equalsIgnoreCase(world.getName())
                && "Prof Alfred".equalsIgnoreCase(npc.getName());
    }

    private boolean hasDialogue(Npc npc) {
        return npc != null && "Gen Ed".equalsIgnoreCase(npc.getName());
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

        int encounterChancePercent = 100;
        if (random.nextInt(100) < encounterChancePercent) {
            battleSystem.startWildBattle();
            gameState = GameState.BATTLE;
            if (activeNpc != null) {
                activeNpc.endInteraction();
            }
            interactionMenuOpen = false;
            activeNpc = null;
            interactionMessage = "";
        }
    }
}
