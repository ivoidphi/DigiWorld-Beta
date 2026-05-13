package digiworld.app;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public final class SaveManager {
    private static final String SAVE_PATH = "savegame.properties";

    private SaveManager() {
    }

    public static synchronized void save(Map<String, String> data) {
        Properties properties = new Properties();
        properties.putAll(data);
        try (FileOutputStream out = new FileOutputStream(SAVE_PATH)) {
            properties.store(out, "DigiWorld Autosave");
        } catch (IOException e) {
            System.err.println("[SaveManager] Failed to save.");
            e.printStackTrace();
        }
    }
}
