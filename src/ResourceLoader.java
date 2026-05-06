import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class ResourceLoader {
    private ResourceLoader() {
    }

    private static File resolveResourcePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
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

    public static BufferedImage loadImage(String path) {
        File file = resolveResourcePath(path);
        if (file == null) {
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                System.err.println("[ResourceLoader] Could not decode image: " + path + " (cwd="
                        + new File(".").getAbsolutePath() + ")");
            }
            return image;
        } catch (IOException e) {
            System.err.println("[ResourceLoader] Failed to load image: " + path + " (cwd="
                    + new File(".").getAbsolutePath() + "): " + e.getMessage());
            return null;
        }
    }

    public static Font loadFont(String path, float size) {
        File file = resolveResourcePath(path);
        if (file == null) {
            return new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size));
        }
        try {
            return Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(Font.PLAIN, size);
        } catch (Exception e) {
            System.err.println("[ResourceLoader] Failed to load font: " + path + " (cwd="
                    + new File(".").getAbsolutePath() + "): " + e.getMessage());
            return new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size));
        }
    }
}
