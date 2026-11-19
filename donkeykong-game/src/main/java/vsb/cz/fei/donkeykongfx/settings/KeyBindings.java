package vsb.cz.fei.donkeykongfx.settings;

import javafx.scene.input.KeyCode;
import lombok.ToString;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Mapping of game actions to keys. One key can trigger only one action.
 */
@ToString
public class KeyBindings {
    public static final String MOVE_LEFT = "move_left";
    public static final String MOVE_RIGHT = "move_right";
    public static final String CLIMB_UP = "climb_up";
    public static final String CLIMB_DOWN = "climb_down";
    public static final String JUMP = "jump";

    private final Map<String, KeyCode> keys = new TreeMap<>();

    /** Default bindings. */
    public KeyBindings() {
        keys.put(MOVE_LEFT, KeyCode.LEFT);
        keys.put(MOVE_RIGHT, KeyCode.RIGHT);
        keys.put(CLIMB_UP, KeyCode.UP);
        keys.put(CLIMB_DOWN, KeyCode.DOWN);
        keys.put(JUMP, KeyCode.SPACE);
    }

    /**
     * Defaults overridden by {@code customKeys}, so an action missing from a saved file still works.
     *
     * @throws IllegalArgumentException if two actions would end up on the same key
     */
    public KeyBindings(Map<String, KeyCode> customKeys) {
        this();
        customKeys.forEach((action, key) -> {
            if (keys.containsKey(action)) { // ignore actions this version of the game does not know
                keys.put(action, key);
            }
        });
        if (keys.values().stream().distinct().count() != keys.size()) {
            throw new IllegalArgumentException("Two actions share the same key: " + keys);
        }
    }

    /** Copy of the current bindings, safe to store. */
    public Map<String, KeyCode> getKeys() {
        return new TreeMap<>(keys);
    }

    public Set<String> getAllActions() {
        return keys.keySet();
    }

    public KeyCode getKeyForAction(String action) {
        return keys.get(action);
    }

    public String getActionForKey(KeyCode keyCode) {
        for (Map.Entry<String, KeyCode> entry : keys.entrySet()) {
            if (entry.getValue() == keyCode) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * @throws IllegalArgumentException if the key is already used by another action
     */
    public void setKeyForAction(String action, KeyCode keyCode) {
        String owner = getActionForKey(keyCode);
        if (owner != null && !owner.equals(action)) {
            throw new IllegalArgumentException("Key " + keyCode.getName() + " is already assigned to action " + owner);
        }
        keys.put(action, keyCode);
    }
}
