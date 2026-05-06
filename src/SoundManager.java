import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SoundManager {
    private Clip currentMusicClip;
    private Clip currentBattleClip;
    private Clip currentWalkClip;
    private String currentWorldName;
    private boolean inBattle;

    public void playMusicForWorld(String worldName) {
        String path = getWavPathForWorld(worldName);
        if (path == null || path.isBlank()) {
            stopMusic();
            currentWorldName = null;
            return;
        }

        if (Objects.equals(currentWorldName, worldName) && currentMusicClip != null && currentMusicClip.isRunning()) {
            return;
        }

        stopMusic();
        Clip clip = loadLoopingClip(path);
        if (clip != null) {
            currentMusicClip = clip;
            currentWorldName = worldName;
            currentMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            currentMusicClip.start();
        }
    }

    public void playBattleMusic() {
        if (inBattle && currentBattleClip != null && currentBattleClip.isRunning()) {
            return;
        }

        stopMusic();
        Clip clip = loadLoopingClip("res/audio/battle.wav");
        if (clip != null) {
            currentBattleClip = clip;
            inBattle = true;
            currentBattleClip.loop(Clip.LOOP_CONTINUOUSLY);
            currentBattleClip.start();
        }
    }

    public void stopBattleMusic() {
        if (currentBattleClip != null) {
            currentBattleClip.stop();
            currentBattleClip.close();
            currentBattleClip = null;
        }
        inBattle = false;
        // Resume world music if we were in a world
        if (currentWorldName != null) {
            playMusicForWorld(currentWorldName);
        }
    }

    public void playWalkSound() {
        if (currentWalkClip != null && currentWalkClip.isRunning()) {
            return; // Don't overlap walk sounds
        }

        Clip clip = loadClip("res/audio/walk-grass.wav");
        if (clip != null) {
            currentWalkClip = clip;
            currentWalkClip.start();
        }
    }

    public void playSkillSound(String beastName, int skillIndex) {
        String path = getSkillSoundPath(beastName, skillIndex);
        if (path != null) {
            Clip clip = loadClip(path);
            if (clip != null) {
                clip.start();
            }
        }
    }

    public void stopMusic() {
        if (currentMusicClip != null) {
            currentMusicClip.stop();
            currentMusicClip.close();
            currentMusicClip = null;
        }
        if (currentBattleClip != null) {
            currentBattleClip.stop();
            currentBattleClip.close();
            currentBattleClip = null;
        }
        inBattle = false;
    }

    private Clip loadLoopingClip(String path) {
        try {
            File file = resolveResourcePath(path);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            return clip;
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            System.err.println("[SoundManager] Failed to load music: " + path + "; " + e.getMessage());
            return null;
        }
    }

    private Clip loadClip(String path) {
        try {
            File file = resolveResourcePath(path);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            return clip;
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            System.err.println("[SoundManager] Failed to load sound: " + path + "; " + e.getMessage());
            return null;
        }
    }

    private File resolveResourcePath(String path) {
        if (path == null || path.isBlank()) {
            return new File(path);
        }
        String normalized = path.replace("/", File.separator);
        File candidate = new File(normalized);
        if (candidate.exists()) {
            return candidate;
        }
        File parentCandidate = new File(".." + File.separator + normalized);
        if (parentCandidate.exists()) {
            return parentCandidate;
        }
        return candidate;
    }

    private String getWavPathForWorld(String worldName) {
        if (worldName == null) {
            return null;
        }
        return switch (worldName) {
            // Put the Hometown WAV file in res/audio/hometown.wav
            case "Hometown" -> "res/audio/hometown.wav";
            // Put the Alpha Village WAV file in res/audio/alpha-village.wav
            case "World 2 - Alpha Village" -> "res/audio/alpha-village.wav";
            // Put the Beta City WAV file in res/audio/beta-city.wav
            case "World 3 - Beta City" -> "res/audio/beta-city.wav";
            // Put the Collapse Zone WAV file in res/audio/collapse-zone.wav
            case "World 4 - Collapse Zone" -> "res/audio/collapse-zone.wav";
            default -> null;
        };
    }

    private String getSkillSoundPath(String beastName, int skillIndex) {
        if (beastName == null || skillIndex < 0 || skillIndex > 2) {
            return null;
        }

        String normalizedName = beastName.toLowerCase().replace(" ", "-");
        String skillSuffix = switch (skillIndex) {
            case 0 -> "skill1";
            case 1 -> "skill2";
            case 2 -> "skill3";
            default -> null;
        };

        if (skillSuffix == null) {
            return null;
        }

        return "res/audio/beasts/" + normalizedName + "/" + skillSuffix + ".wav";
    }
}
