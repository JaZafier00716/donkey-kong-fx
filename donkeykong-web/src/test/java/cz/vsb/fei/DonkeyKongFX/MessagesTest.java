package cz.vsb.fei.DonkeyKongFX;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {

    private static Properties load(String name) throws IOException {
        try (InputStream in = MessagesTest.class.getResourceAsStream("/" + name + ".properties")) {
            assertNotNull(in, name + " is missing");
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        }
    }

    @Test
    void allLanguagesHaveTheSameKeysAndNoEmptyTexts() throws IOException {
        Set<String> expected = new TreeSet<>(load("messages").stringPropertyNames());

        for (String bundle : new String[]{"messages_cs", "messages_de"}) {
            Properties properties = load(bundle);
            Set<String> missing = new TreeSet<>(expected);
            missing.removeAll(properties.stringPropertyNames());
            Set<String> extra = new TreeSet<>(properties.stringPropertyNames());
            extra.removeAll(expected);
            assertTrue(missing.isEmpty(), bundle + " is missing " + missing);
            assertTrue(extra.isEmpty(), bundle + " has unknown keys " + extra);
            properties.forEach((key, value) -> assertFalse(value.toString().isBlank(), bundle + ": " + key + " is empty"));
        }
    }
}
