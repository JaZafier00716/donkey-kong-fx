package vsb.cz.fei.donkeykongfx.view;

import javafx.scene.control.Dialog;

/**
 * Gives dialogs (which open in their own window and therefore do not inherit the stylesheet of the
 * game scene) the same arcade look as the rest of the game.
 */
public final class Dialogs {
    private static final String STYLESHEET = Dialogs.class.getResource("/application.css").toExternalForm();

    private Dialogs() {
    }

    public static <T extends Dialog<?>> T style(T dialog) {
        dialog.getDialogPane().getStylesheets().add(STYLESHEET);
        return dialog;
    }
}
