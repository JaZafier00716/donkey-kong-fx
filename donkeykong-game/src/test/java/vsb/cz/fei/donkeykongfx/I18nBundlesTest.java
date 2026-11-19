package vsb.cz.fei.donkeykongfx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class I18nBundlesTest {
    private static final List<String> BUNDLES = List.of("messages_en", "messages_cs", "messages_cs_CZ", "messages_de");

    private static Properties load(String name) throws IOException {
        try (InputStream in = I18nBundlesTest.class.getResourceAsStream("/" + name + ".properties")) {
            assertNotNull(in, name + ".properties is missing");
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        }
    }

    @Test
    void everyLanguageDefinesExactlyTheKeysOfTheDefaultBundle() throws IOException {
        Set<String> expected = new TreeSet<>(load("messages").stringPropertyNames());

        for (String bundle : BUNDLES) {
            Set<String> keys = new TreeSet<>(load(bundle).stringPropertyNames());
            Set<String> missing = new TreeSet<>(expected);
            missing.removeAll(keys);
            Set<String> extra = new TreeSet<>(keys);
            extra.removeAll(expected);
            assertTrue(missing.isEmpty(), bundle + " is missing keys " + missing);
            assertTrue(extra.isEmpty(), bundle + " has keys unknown to the default bundle " + extra);
        }
    }

    @Test
    void noTranslationIsEmpty() throws IOException {
        for (String bundle : BUNDLES) {
            load(bundle).forEach((key, value) ->
                    assertFalse(value.toString().isBlank(), bundle + ": empty text for " + key));
        }
    }

    @Test
    void messageFormatPlaceholdersAreKeptInEveryLanguage() throws IOException {
        for (String bundle : BUNDLES) {
            String score = load(bundle).getProperty("game.score");
            assertTrue(score.contains("{0}"), bundle + ": game.score needs the {0} placeholder");
            assertTrue(MessageFormat.format(score, 120).contains("120"));
        }
    }

    @Test
    void switchingTheLanguageChangesTheTexts() {
        try {
            I18n.setLocaleTag("de");
            String german = I18n.get("menu.start");
            I18n.setLocaleTag("en");
            String english = I18n.get("menu.start");

            assertEquals("Start", english);
            assertFalse(german.isBlank());
        } finally {
            I18n.setLocaleTag("en");
        }
    }

    @Test
    void blankLanguageTagFallsBackToEnglish() {
        I18n.setLocaleTag(" ");

        assertEquals("en", I18n.locale().getLanguage());
    }

    @Test
    void everyKeyBindingActionHasATranslatedLabel() throws IOException {
        List<String> actions = List.of(vsb.cz.fei.donkeykongfx.settings.KeyBindings.MOVE_LEFT,
                vsb.cz.fei.donkeykongfx.settings.KeyBindings.MOVE_RIGHT,
                vsb.cz.fei.donkeykongfx.settings.KeyBindings.CLIMB_UP,
                vsb.cz.fei.donkeykongfx.settings.KeyBindings.CLIMB_DOWN,
                vsb.cz.fei.donkeykongfx.settings.KeyBindings.JUMP);
        for (String bundle : BUNDLES) {
            Properties properties = load(bundle);
            for (String action : actions) {
                String label = properties.getProperty("action." + action);
                assertNotNull(label, bundle + " has no label for " + action);
                assertFalse(label.isBlank());
            }
        }
    }

    @Test
    void unknownActionFallsBackToItsId() {
        I18n.setLocaleTag("en");
        assertEquals("Jump", I18n.action("jump"));
        assertEquals("teleport", I18n.action("teleport"));
    }
}
