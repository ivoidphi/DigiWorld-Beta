import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class WorldSoundManager {
    private Clip currentClip;
    private String currentWorldName;

    public void playMusicForWorld(String worldName) {
        String path = getWavPathForWorld(worldName);
        if (path == null || path.isBlank()) {
            stopMusic();
            currentWorldName = null;
            return;
        }

        if (Objects.equals(currentWorldName, worldName) && currentClip != null && currentClip.isRunning()) {
            return;
        }

        stopMusic();
        Clip clip = loadLoopingClip(path);
        if (clip != null) {
            currentClip = clip;
            currentWorldName = worldName;
            currentClip.loop(Clip.LOOP_CONTINUOUSLY);
            currentClip.start();
        }
    }

    public void stopMusic() {
        if (currentClip == null) {
            return;
        }
        currentClip.stop();
        currentClip.close();
        currentClip = null;
    }

    private Clip loadLoopingClip(String path) {
        try {
            File file = resolveResourcePath(path);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            return clip;
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            System.err.println("[WorldSoundManager] Failed to load music: " + path + "; " + e.getMessage());
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
}
