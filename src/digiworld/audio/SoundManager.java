package digiworld.audio;

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
    private static SoundManager instance;
    private Clip currentClip;
    private String currentMusicPath = "";
    private final List<Clip> activeEffectClips;
    private final Map<String, String> worldMusicByName;
    private final Map<String, String> skillSoundByBeastName;
    private final String defaultSkillSoundPath;
    private final String combatMusicPath;

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    public SoundManager() {
        worldMusicByName = new HashMap<>();
        worldMusicByName.put("Hometown", "res/sounds/hometown-loop.wav");
        worldMusicByName.put("World 2 - Alpha Village", "res/sounds/alpha-village-loop.wav");
        worldMusicByName.put("World 3 - Beta City", "res/sounds/beta-city-loop.wav");
        worldMusicByName.put("World 4 - Collapse Zone", "res/sounds/collapse-zone-loop.wav");
        combatMusicPath = "res/sounds/combat-loop.wav";

        activeEffectClips = new ArrayList<>();
        skillSoundByBeastName = new HashMap<>();
        skillSoundByBeastName.put("gekuma", "res/sounds/skills/gekuma-skill.wav");
        skillSoundByBeastName.put("kingmantis", "res/sounds/skills/kingmantis-skill.wav");
        skillSoundByBeastName.put("woltrix", "res/sounds/skills/woltrix-skill.wav");
        skillSoundByBeastName.put("pirrot", "res/sounds/skills/pirrot-skill.wav");
        skillSoundByBeastName.put("zyuugor", "res/sounds/skills/zyuugor-skill.wav");
        skillSoundByBeastName.put("voltchu", "res/sounds/skills/voltchu-skill.wav");
        skillSoundByBeastName.put("shadefox", "res/sounds/skills/shadefox-skill.wav");
        skillSoundByBeastName.put("nokami", "res/sounds/skills/nokami-skill.wav");
        skillSoundByBeastName.put("kyoflare", "res/sounds/skills/kyoflare-skill.wav");
        skillSoundByBeastName.put("vineratops", "res/sounds/skills/vineratops-skill.wav");
        skillSoundByBeastName.put("all mighty", "res/sounds/skills/all-mighty-skill.wav");
        defaultSkillSoundPath = "res/sounds/skills/default-skill.wav";
    }

    public void playTitleMusic() {
        playMusic("res/sounds/bgmusic.wav");
    }

    public void playWorldMusic(String worldName) {
        String musicPath = worldMusicByName.getOrDefault(worldName, "res/sounds/overworld-loop.wav");
        playMusic(musicPath);
    }

    public void playCombatMusic() {
        playMusic(combatMusicPath);
    }

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

    public synchronized void playMusic(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        if (path.equals(currentMusicPath) && currentClip != null && currentClip.isRunning()) {
            return;
        }

        stopMusic();

        try {
            File audioFile = resolveResourceFile(path);
            if (!audioFile.exists()) {
                System.err.println("[SoundManager] File not found: " + path);
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            audioStream.close();
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();

            currentClip = clip;
            currentMusicPath = path;
            System.out.println("[SoundManager] Playing music: " + path);
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            System.err.println("[SoundManager] Failed to play sound: " + path);
            e.printStackTrace();
        }
    }

    public void playSkillSoundForBeast(String beastName) {
        if (beastName == null || beastName.isBlank()) {
            return;
        }
        String normalizedBeastName = beastName.trim().toLowerCase();
        String skillPath = skillSoundByBeastName.getOrDefault(normalizedBeastName, defaultSkillSoundPath);
        playSoundEffect(skillPath);
    }

    private void playSoundEffect(String path) {
        try {
            File audioFile = resolveResourceFile(path);
            if (!audioFile.exists()) {
                System.err.println("[SoundManager] Sound file not found: " + path);
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
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            System.err.println("[SoundManager] Failed to play sound effect: " + path);
            e.printStackTrace();
        }
    }

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
