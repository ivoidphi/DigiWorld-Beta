import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class BitmapFont {
    private static final int BASE_SIZE = 12;
    private static final char FIRST_CHAR = 32;
    private static final char LAST_CHAR = 126;
    private static final Map<Character, BufferedImage> GLYPHS = new HashMap<>();
    private static final Map<Integer, Map<Character, BufferedImage>> TINTED_GLYPHS = new HashMap<>();
    private static int glyphWidth;
    private static int glyphHeight;
    private static int ascent;
    private static boolean initialized;

    private BitmapFont() {
    }

    public static void drawText(Graphics2D g2d, String text, int x, int y, Color color) {
        ensureInit();
        int cursorX = x;
        int baselineTop = y - ascent;
        Map<Character, BufferedImage> glyphMap = getGlyphMapForColor(color);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            BufferedImage glyph = glyphMap.get(ch);
            if (glyph == null) {
                glyph = glyphMap.get('?');
            }
            if (glyph != null) {
                g2d.drawImage(glyph, cursorX, baselineTop, null);
            }
            cursorX += glyphWidth;
        }
    }

    public static int measureWidth(String text) {
        ensureInit();
        return text.length() * glyphWidth;
    }

    public static int getLineHeight() {
        ensureInit();
        return glyphHeight;
    }

    private static void ensureInit() {
        if (initialized) {
            return;
        }
        Font base = loadBaseFont();
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gProbe = probe.createGraphics();
        gProbe.setFont(base);
        gProbe.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        FontMetrics fm = gProbe.getFontMetrics();
        glyphWidth = fm.charWidth('W');
        glyphHeight = fm.getHeight();
        ascent = fm.getAscent();
        gProbe.dispose();

        for (char ch = FIRST_CHAR; ch <= LAST_CHAR; ch++) {
            BufferedImage glyph = new BufferedImage(glyphWidth, glyphHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = glyph.createGraphics();
            gg.setFont(base);
            gg.setColor(Color.WHITE);
            gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            gg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            gg.drawString(String.valueOf(ch), 0, ascent);
            gg.dispose();
            GLYPHS.put(ch, glyph);
        }
        initialized = true;
    }

    private static Font loadBaseFont() {
        try {
            Font loaded = Font.createFont(Font.TRUETYPE_FONT, new File("res/fonts/PixelifySans.ttf"));
            return loaded.deriveFont(Font.PLAIN, BASE_SIZE);
        } catch (FontFormatException | IOException e) {
            return new Font("Monospaced", Font.PLAIN, BASE_SIZE);
        }
    }

    private static Map<Character, BufferedImage> getGlyphMapForColor(Color color) {
        if (color == null) {
            return GLYPHS;
        }
        int key = color.getRGB();
        if (key == Color.WHITE.getRGB()) {
            return GLYPHS;
        }
        Map<Character, BufferedImage> cached = TINTED_GLYPHS.get(key);
        if (cached != null) {
            return cached;
        }

        Map<Character, BufferedImage> tinted = new HashMap<>();
        for (Map.Entry<Character, BufferedImage> entry : GLYPHS.entrySet()) {
            BufferedImage src = entry.getValue();
            BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(src, 0, 0, null);
            g.setComposite(AlphaComposite.SrcAtop);
            g.setColor(color);
            g.fillRect(0, 0, out.getWidth(), out.getHeight());
            g.dispose();
            tinted.put(entry.getKey(), out);
        }
        TINTED_GLYPHS.put(key, tinted);
        return tinted;
    }
}
