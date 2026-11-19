package vsb.cz.fei.donkeykongfx.settings;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyBindingsRepositoryTest {
    @TempDir
    File dir;

    @Test
    void missingFilesMeanDefaults() throws KeyBindingsException {
        assertTrue(KeyBindingsRepository.loadKeyBindings(new File(dir, "keybindings.cfg")).isEmpty());
        assertEquals("en", KeyBindingsRepository.loadLanguageTag(new File(dir, "settings.cfg")));
    }

    @Test
    void keyBindingsSurviveASaveAndLoadRoundTrip() throws KeyBindingsException {
        File file = new File(dir, "keybindings.cfg");
        Map<String, KeyCode> keys = new KeyBindings().getKeys();
        keys.put("move_left", KeyCode.A);
        keys.put("move_right", KeyCode.D);
        keys.put("climb_up", KeyCode.W);
        keys.put("climb_down", KeyCode.S);

        KeyBindingsRepository.saveKeyBindings(keys, file);

        assertEquals(keys, KeyBindingsRepository.loadKeyBindings(file));
    }

    @Test
    void everyKeyCanBeWrittenAndReadBackByName() {
        // the file stores KeyCode.getName(); a key whose name does not parse would corrupt the settings
        for (KeyCode code : KeyCode.values()) {
            if (code == KeyCode.UNDEFINED) {
                continue;
            }
            KeyCode parsed = KeyCode.getKeyCode(code.getName());
            // some keys share a display name (e.g. digit and numpad variants); they must at least parse
            assertTrue(parsed != null, "name of " + code + " is not parseable: " + code.getName());
        }
    }

    @Test
    void unknownKeyNameIsRejected() {
        Properties properties = new Properties();
        properties.setProperty("jump", "NotAKey");

        KeyBindingsException e = assertThrows(KeyBindingsException.class, () -> KeyBindingsRepository.parseKeyBindings(properties));

        assertTrue(e.getMessage().contains("NotAKey"));
    }

    @Test
    void twoActionsOnTheSameKeyAreRejected() {
        Properties properties = new Properties();
        properties.setProperty("jump", "Space");
        properties.setProperty("climb_up", "Space");

        assertThrows(KeyBindingsException.class, () -> KeyBindingsRepository.parseKeyBindings(properties));
    }

    @Test
    void handWrittenColonSyntaxWithSpacesIsAccepted() throws IOException, KeyBindingsException {
        File file = new File(dir, "keybindings.cfg");
        Files.writeString(file.toPath(), "move_left: A\nmove_right = D\n");

        HashMap<String, KeyCode> keys = KeyBindingsRepository.loadKeyBindings(file);

        assertEquals(KeyCode.A, keys.get("move_left"));
        assertEquals(KeyCode.D, keys.get("move_right"));
    }

    @Test
    void languageIsStoredAndBlankFallsBackToEnglish() throws KeyBindingsException {
        File file = new File(dir, "settings.cfg");

        KeyBindingsRepository.saveLanguageTag("cs", file);
        assertEquals("cs", KeyBindingsRepository.loadLanguageTag(file));

        KeyBindingsRepository.saveLanguageTag(" ", file);
        assertEquals("en", KeyBindingsRepository.loadLanguageTag(file));
    }
}
