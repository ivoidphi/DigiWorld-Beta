# DigiWorld AI Agent Guidelines

## Architecture Overview
DigiWorld is a Java Swing-based RPG game with a modular package structure:
- `digiworld.app`: Core game loop (GamePanel) and frame management
- `digiworld.core`: Game entities (Player, Npc, World, Camera)
- `digiworld.battle`: Combat system (BattleSystem, BattleCreature, moves/types)
- `digiworld.ui`: Rendering and UI components (GameUiRenderer, Inventory)
- `digiworld.maps`: Tile-based world definitions (HometownTileMap, etc.)
- `digiworld.dialogue`: NPC interaction system
- `digiworld.audio`: Sound management (singleton SoundManager)

GamePanel (2300+ lines) serves as the central hub, managing game state, input, rendering, and transitions between exploration, battle, and dialogue modes.

## Key Constants & Conventions
- **Tile System**: TILE_SIZE = 32 pixels, RENDER_SCALE = 2x for crisp upscaling
- **Resolution**: Logical 640x360 min, scaled to full screen; frame buffer renders at logical size then upscaled
- **Assets**: res/ directory with subfolders (beasts/, characters/, icons/, sounds/, tiles/, ui/)
- **Sprites**: Beasts use -b.png (back) / -fw.png (front); characters add -l.png (left) / -r.png (right)
- **SoundManager**: Singleton pattern for music/effects; world music mapped by name (e.g., "Hometown" -> hometown-loop.wav)

## Development Workflow
- **Build**: Compile with `javac -cp . src/**/*.java` from project root
- **Run**: `java -cp . ScreenTitle` launches title screen, transitions to GameFrame/GamePanel
- **IDE**: IntelliJ IDEA (untitled.iml present); full-screen undecorated windows require testing on target display
- **Debugging**: GamePanel has extensive state flags; use println for battle/dialogue flows

## Code Patterns
- **State Management**: GamePanel uses 50+ boolean flags for quests/progress (e.g., `alphaBossDefeated`, `hasChallengeTicket`)
- **Input Handling**: Custom InputHandler class; mouse clicks normalized to logical coordinates
- **Rendering**: Double-buffered JPanel with frame buffer; paintComponent upscales and draws
- **Battle Integration**: BattleSystem injected into GamePanel; switches gameState to BATTLE
- **Dialogue System**: DialogueController manages sequences; auto-close timers for cutscenes

## Data Flows
- Input → GamePanel.update() → handle exploration/battle/dialogue logic
- Rendering: GamePanel.paintComponent() → draw world tiles/entities → overlay UI via GameUiRenderer
- World Transitions: worlds[] array indexed by worldIndex; teleport via door interactions
- Save System: Properties file (savegame.properties) for persistence

## Dependencies & Integration
- Pure Java AWT/Swing; no external libraries
- Audio: javax.sound for WAV playback; clips managed to prevent overlap
- Maps: Procedural tile generation in WorldTileMapRegistry; tile types enum-based

## Common Pitfalls
- Coordinate Systems: Mix of screen (mouse), logical (rendering), and tile (world) coordinates
- Threading: Game loop runs in separate thread; UI updates must be thread-safe
- Asset Loading: Paths relative to project root (e.g., "res/beasts/gekuma/gekuma-fw.png")
- State Synchronization: Battle outcomes update GamePanel flags; ensure consistency</content>
<parameter name="filePath">C:\Users\sungohoon\IdeaProjects\DigiWorld-Beta\AGENTS.md
