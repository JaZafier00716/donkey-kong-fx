package vsb.cz.fei.donkeykongfx.controllers;

import javafx.scene.control.Alert;
import lombok.extern.log4j.Log4j2;
import vsb.cz.fei.donkeykongfx.I18n;
import vsb.cz.fei.donkeykongfx.settings.KeyBindings;
import vsb.cz.fei.donkeykongfx.settings.KeyBindingsException;
import vsb.cz.fei.donkeykongfx.settings.KeyBindingsRepository;

@Log4j2
public abstract class SettingsAffected extends ResizableController{
    protected KeyBindings keyBindings;

    protected void loadKeyBindings() {
        try {
            keyBindings = new KeyBindings(KeyBindingsRepository.loadKeyBindings());
        } catch (KeyBindingsException | IllegalArgumentException e) {
            log.warn("Loading key bindings failed; using defaults", e);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(I18n.get("alert.loadingKeyBindingsProblem"));
            alert.getDialogPane().setContentText(e.getMessage() + "\n" + I18n.get("alert.defaultKeyBindings"));
            alert.showAndWait();
            keyBindings = new KeyBindings();
        }
    }

    protected void storeKeyBindings() {
        try {
            KeyBindingsRepository.saveKeyBindings(keyBindings.getKeys());
        } catch (KeyBindingsException e) {
            log.warn("Saving key bindings failed", e);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(I18n.get("alert.savingKeyBindingsProblem"));
            alert.getDialogPane().setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
