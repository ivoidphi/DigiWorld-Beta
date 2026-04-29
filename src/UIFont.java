import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;

public final class UIFont {
    public enum Preset {
        PIXELIFY,
        LUCIDA_CONSOLE,
        COURIER_NEW,
        CONSOLAS
    }

    private static Font baseFont;
    private static Preset currentPreset = Preset.COURIER_NEW;

    private UIFont() {
    }

    public static Font regular(float size) {
        return getBaseFont().deriveFont(Font.PLAIN, size);
    }

    public static Font bold(float size) {
        return getBaseFont().deriveFont(Font.BOLD, size);
    }

    public static void cyclePreset() {
        Preset[] values = Preset.values();
        int next = (currentPreset.ordinal() + 1) % values.length;
        currentPreset = values[next];
        baseFont = null;
    }

    public static String getPresetName() {
        return switch (currentPreset) {
            case PIXELIFY -> "PixelifySans";
            case LUCIDA_CONSOLE -> "Lucida Console";
            case COURIER_NEW -> "Courier New";
            case CONSOLAS -> "Consolas";
        };
    }

    private static Font getBaseFont() {
        if (baseFont != null) {
            return baseFont;
        }
        if (currentPreset == Preset.PIXELIFY) {
            try {
                baseFont = Font.createFont(Font.TRUETYPE_FONT, new File("res/fonts/PixelifySans.ttf"));
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(baseFont);
                return baseFont;
            } catch (FontFormatException | IOException e) {
                baseFont = new Font("Monospaced", Font.PLAIN, 12);
                return baseFont;
            }
        }
        if (currentPreset == Preset.LUCIDA_CONSOLE) {
            baseFont = new Font("Lucida Console", Font.PLAIN, 12);
            return baseFont;
        }
        if (currentPreset == Preset.COURIER_NEW) {
            baseFont = new Font("Courier New", Font.PLAIN, 12);
            return baseFont;
        }
        baseFont = new Font("Consolas", Font.PLAIN, 12);
        return baseFont;
    }
}
