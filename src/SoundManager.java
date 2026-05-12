import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SoundManager {

    // SINGLETON INSTANCE
    public static SoundManager instance;

    // CURRENT MUSIC
    private Clip currentClip;

    // CURRENT MUSIC PATH
    private String currentMusicPath = "";

    // ACTIVE SOUND EFFECTS
    private final List<Clip> activeEffectClips;

    // WORLD MUSIC
    private final Map<String, String> worldMusicByName;

    // SKILL SOUNDS
    private final Map<String, String> skillSoundByBeastName;

    private final String defaultSkillSoundPath;

    private final String combatMusicPath;

    // GET SINGLETON INSTANCE
    public static SoundManager getInstance() {

        if (instance == null) {

            instance = new SoundManager();
        }

        return instance;
    }

    // CONSTRUCTOR
    public SoundManager() {

        worldMusicByName = new HashMap<>();

        worldMusicByName.put(
                "Hometown",
                "res/sounds/hometown-loop.wav");

        worldMusicByName.put(
                "World 2 - Alpha Village",
                "res/sounds/alpha-village-loop.wav");

        worldMusicByName.put(
                "World 3 - Beta City",
                "res/sounds/beta-city-loop.wav");

        worldMusicByName.put(
                "World 4 - Collapse Zone",
                "res/sounds/collapse-zone-loop.wav");

        combatMusicPath = "res/sounds/combat-loop.wav";

        activeEffectClips = new ArrayList<>();

        // SKILL SOUNDS
        skillSoundByBeastName = new HashMap<>();

        skillSoundByBeastName.put(
                "gekuma",
                "res/sounds/skills/gekuma-skill.wav");

        skillSoundByBeastName.put(
                "kingmantis",
                "res/sounds/skills/kingmantis-skill.wav");

        skillSoundByBeastName.put(
                "woltrix",
                "res/sounds/skills/woltrix-skill.wav");

        skillSoundByBeastName.put(
                "pirrot",
                "res/sounds/skills/pirrot-skill.wav");

        skillSoundByBeastName.put(
                "zyuugor",
                "res/sounds/skills/zyuugor-skill.wav");

        skillSoundByBeastName.put(
                "voltchu",
                "res/sounds/skills/voltchu-skill.wav");

        skillSoundByBeastName.put(
                "shadefox",
                "res/sounds/skills/shadefox-skill.wav");

        skillSoundByBeastName.put(
                "nokami",
                "res/sounds/skills/nokami-skill.wav");

        skillSoundByBeastName.put(
                "kyoflare",
                "res/sounds/skills/kyoflare-skill.wav");

        skillSoundByBeastName.put(
                "vineratops",
                "res/sounds/skills/vineratops-skill.wav");

        skillSoundByBeastName.put(
                "all mighty",
                "res/sounds/skills/all-mighty-skill.wav");

        defaultSkillSoundPath = "res/sounds/skills/default-skill.wav";
    }

    // TITLE SCREEN MUSIC
    public void playTitleMusic() {

        playMusic("res/sounds/bgmusic.wav");
    }

    // WORLD MUSIC
    public void playWorldMusic(String worldName) {

        String musicPath = worldMusicByName.getOrDefault(
                worldName,
                "res/sounds/overworld-loop.wav");

        playMusic(musicPath);
    }

    // COMBAT MUSIC
    public void playCombatMusic() {

        playMusic(combatMusicPath);
    }

    // STOP MUSIC
    public void stopMusic() {

        try {

            if (currentClip != null) {

                currentClip.stop();

                currentClip.flush();

                currentClip.close();

                currentClip = null;
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // PLAY MUSIC
    private void playMusic(String path) {

        // DON'T RESTART SAME MUSIC
        if (path.equals(currentMusicPath)) {

            return;
        }

        currentMusicPath = path;

        // STOP OLD MUSIC
        stopMusic();

        try {

            File audioFile = resolveResourceFile(path);

            if (!audioFile.exists()) {

                System.err.println(
                        "[SoundManager] Sound file not found: "
                                + path);

                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);

            Clip clip = AudioSystem.getClip();

            clip.open(audioStream);

            clip.loop(Clip.LOOP_CONTINUOUSLY);

            clip.start();

            // SAVE CURRENT CLIP
            currentClip = clip;

            System.out.println(
                    "[SoundManager] Playing music: "
                            + path);

        } catch (
                IOException
                | UnsupportedAudioFileException
                | LineUnavailableException e) {

            System.err.println(
                    "[SoundManager] Failed to play sound: "
                            + path);

            e.printStackTrace();
        }
    }

    // PLAY SKILL SOUND
    public void playSkillSoundForBeast(String beastName) {

        if (beastName == null
                || beastName.isBlank()) {

            return;
        }

        String normalizedBeastName = beastName.trim().toLowerCase();

        String skillPath = skillSoundByBeastName.getOrDefault(
                normalizedBeastName,
                defaultSkillSoundPath);

        playSoundEffect(skillPath);
    }

    // PLAY SOUND EFFECT
    private void playSoundEffect(String path) {

        try {

            File audioFile = resolveResourceFile(path);

            if (!audioFile.exists()) {

                System.err.println(
                        "[SoundManager] Sound file not found: "
                                + path);

                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);

            Clip clip = AudioSystem.getClip();

            clip.open(audioStream);

            clip.start();

            activeEffectClips.add(clip);

            clip.addLineListener(event -> {

                if (event.getType() == LineEvent.Type.STOP) {

                    clip.close();

                    activeEffectClips.remove(clip);
                }
            });

        } catch (
                IOException
                | UnsupportedAudioFileException
                | LineUnavailableException e) {

            System.err.println(
                    "[SoundManager] Failed to play sound effect: "
                            + path);

            e.printStackTrace();
        }
    }

    // RESOLVE FILE PATH
    private File resolveResourceFile(String path) {

        Path resourcePath = Paths.get(path);

        if (resourcePath.isAbsolute()) {

            return resourcePath.toFile();
        }

        Path currentDir = Paths.get(System.getProperty("user.dir"));

        Path candidate = currentDir.resolve(path);

        if (Files.exists(candidate)) {

            return candidate.toFile();
        }

        Path searchDir = currentDir;

        while (searchDir != null) {

            candidate = searchDir.resolve(path);

            if (Files.exists(candidate)) {

                return candidate.toFile();
            }

            searchDir = searchDir.getParent();
        }

        return resourcePath.toFile();
    }
}