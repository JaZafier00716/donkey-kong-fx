package vsb.cz.fei.donkeykongfx.settings;

import javafx.scene.input.KeyCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Stores key bindings ({@code keybindings.cfg}) and the language ({@code settings.cfg}) as
 * properties files in the working directory. Both are created on first change; the game starts
 * with defaults when they are missing.
 */
public final class KeyBindingsRepository {
    private static final File KEY_BINDINGS_FILE = new File("keybindings.cfg");
    private static final File SETTINGS_FILE = new File("settings.cfg");
    private static final String LANGUAGE_TAG_KEY = "languageTag";
    private static final String DEFAULT_LANGUAGE = "en";

    private KeyBindingsRepository() {
    }

    public static void saveKeyBindings(Map<String, KeyCode> keys) throws KeyBindingsException {
        saveKeyBindings(keys, KEY_BINDINGS_FILE);
    }

    public static HashMap<String, KeyCode> loadKeyBindings() throws KeyBindingsException {
        return loadKeyBindings(KEY_BINDINGS_FILE);
    }

    public static void saveLanguageTag(String languageTag) throws KeyBindingsException {
        saveLanguageTag(languageTag, SETTINGS_FILE);
    }

    public static String loadLanguageTag() throws KeyBindingsException {
        return loadLanguageTag(SETTINGS_FILE);
    }

    static void saveKeyBindings(Map<String, KeyCode> keys, File file) throws KeyBindingsException {
        Properties properties = new Properties();
        new TreeMap<>(keys).forEach((action, keyCode) -> {
            if (keyCode != null) {
                properties.setProperty(action, keyCode.getName());
            }
        });
        store(properties, file, "DonkeyKongFX key bindings");
    }

    /** @return the stored bindings, or an empty map when nothing was saved yet */
    static HashMap<String, KeyCode> loadKeyBindings(File file) throws KeyBindingsException {
        if (!file.exists()) {
            return new HashMap<>();
        }
        return parseKeyBindings(read(file));
    }

    /**
     * @throws KeyBindingsException for an unknown key name, or when two actions use the same key
     */
    static HashMap<String, KeyCode> parseKeyBindings(Properties properties) throws KeyBindingsException {
        HashMap<String, KeyCode> keys = new HashMap<>();
        HashMap<KeyCode, String> owners = new HashMap<>();
        for (String action : properties.stringPropertyNames()) {
            String keyName = properties.getProperty(action).trim();
            KeyCode keyCode = KeyCode.getKeyCode(keyName);
            if (keyCode == null) {
                throw new KeyBindingsException("Unknown key '" + keyName + "' for action " + action);
            }
            String other = owners.put(keyCode, action);
            if (other != null) {
                throw new KeyBindingsException("Key " + keyName + " is used by both " + other + " and " + action);
            }
            keys.put(action, keyCode);
        }
        return keys;
    }

    static void saveLanguageTag(String languageTag, File file) throws KeyBindingsException {
        Properties properties = file.exists() ? read(file) : new Properties();
        properties.setProperty(LANGUAGE_TAG_KEY, languageTag == null || languageTag.isBlank() ? DEFAULT_LANGUAGE : languageTag);
        store(properties, file, "DonkeyKongFX local settings");
    }

    static String loadLanguageTag(File file) throws KeyBindingsException {
        if (!file.exists()) {
            return DEFAULT_LANGUAGE;
        }
        String languageTag = read(file).getProperty(LANGUAGE_TAG_KEY);
        return languageTag == null || languageTag.isBlank() ? DEFAULT_LANGUAGE : languageTag;
    }

    private static Properties read(File file) throws KeyBindingsException {
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            throw new KeyBindingsException("Could not read " + file.getName(), e);
        }
        return properties;
    }

    private static void store(Properties properties, File file, String comment) throws KeyBindingsException {
        try (OutputStream out = new FileOutputStream(file)) {
            properties.store(out, comment);
        } catch (IOException e) {
            throw new KeyBindingsException("Could not write " + file.getName(), e);
        }
    }
}
