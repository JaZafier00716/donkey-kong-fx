package vsb.cz.fei.donkeykongfx.settings;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeyBindingsTest {

    @Test
    void defaultsUseArrowKeysAndSpace() {
        KeyBindings keys = new KeyBindings();

        assertEquals(KeyCode.LEFT, keys.getKeyForAction(KeyBindings.MOVE_LEFT));
        assertEquals(KeyCode.RIGHT, keys.getKeyForAction(KeyBindings.MOVE_RIGHT));
        assertEquals(KeyCode.UP, keys.getKeyForAction(KeyBindings.CLIMB_UP));
        assertEquals(KeyCode.DOWN, keys.getKeyForAction(KeyBindings.CLIMB_DOWN));
        assertEquals(KeyCode.SPACE, keys.getKeyForAction(KeyBindings.JUMP));
        assertEquals(Set.of("move_left", "move_right", "climb_up", "climb_down", "jump"), keys.getAllActions());
    }

    @Test
    void lookupByKeyFindsTheAction() {
        KeyBindings keys = new KeyBindings();

        assertEquals("jump", keys.getActionForKey(KeyCode.SPACE));
        assertNull(keys.getActionForKey(KeyCode.Q));
    }

    @Test
    void rebindingToAFreeKeyWorks() {
        KeyBindings keys = new KeyBindings();

        keys.setKeyForAction(KeyBindings.JUMP, KeyCode.W);

        assertEquals(KeyCode.W, keys.getKeyForAction("jump"));
        assertNull(keys.getActionForKey(KeyCode.SPACE));
    }

    @Test
    void rebindingAnActionToItsOwnKeyIsAllowed() {
        KeyBindings keys = new KeyBindings();

        keys.setKeyForAction(KeyBindings.JUMP, KeyCode.SPACE);

        assertEquals(KeyCode.SPACE, keys.getKeyForAction("jump"));
    }

    @Test
    void aKeyCannotTriggerTwoActions() {
        KeyBindings keys = new KeyBindings();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> keys.setKeyForAction(KeyBindings.JUMP, KeyCode.LEFT));

        assertEquals(KeyCode.SPACE, keys.getKeyForAction("jump"), "unchanged after the rejected change");
        assertEquals(true, e.getMessage().contains("move_left"));
    }

    @Test
    void savedBindingsOverrideTheDefaultsAndMissingActionsKeepTheirDefault() {
        KeyBindings keys = new KeyBindings(Map.of("move_left", KeyCode.A, "move_right", KeyCode.D));

        assertEquals(KeyCode.A, keys.getKeyForAction("move_left"));
        assertEquals(KeyCode.D, keys.getKeyForAction("move_right"));
        assertEquals(KeyCode.SPACE, keys.getKeyForAction("jump"));
    }

    @Test
    void unknownActionsInASavedFileAreIgnored() {
        KeyBindings keys = new KeyBindings(Map.of("fly", KeyCode.F));

        assertNull(keys.getKeyForAction("fly"));
        assertEquals(5, keys.getAllActions().size());
    }

    @Test
    void savedBindingsThatCollideWithTheDefaultsOfOtherActionsAreRejected() {
        // jump is moved to LEFT but move_left keeps its default LEFT
        assertThrows(IllegalArgumentException.class, () -> new KeyBindings(Map.of("jump", KeyCode.LEFT)));
    }

    @Test
    void getKeysReturnsACopy() {
        KeyBindings keys = new KeyBindings();

        keys.getKeys().put("jump", KeyCode.Z);

        assertEquals(KeyCode.SPACE, keys.getKeyForAction("jump"));
    }
}
