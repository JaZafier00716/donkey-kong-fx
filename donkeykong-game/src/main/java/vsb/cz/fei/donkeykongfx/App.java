package vsb.cz.fei.donkeykongfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import vsb.cz.fei.donkeykongfx.controllers.GameController;
import vsb.cz.fei.donkeykongfx.controllers.MenuController;
import vsb.cz.fei.donkeykongfx.controllers.ResizableController;
import vsb.cz.fei.donkeykongfx.controllers.SettingsController;
import vsb.cz.fei.donkeykongfx.settings.KeyBindingsException;
import vsb.cz.fei.donkeykongfx.settings.KeyBindingsRepository;
import vsb.cz.fei.donkeykongfx.view.FitPane;
import vsb.cz.fei.donkeykongfx.view.WindowSizing;

import java.io.IOException;

/**
 * DonkeyKong game application and JavaFX entry point.
 * <p>
 * The game only contains game logic and UI; high scores are stored through the
 * {@code donkeykong-web} REST service (see {@code ScoreRestClient}).
 * <p>
 * One {@link Scene} lives for the whole run and only its root is swapped, so the window keeps its
 * size, maximised and full-screen state when moving between menu, game and settings.
 */
@Log4j2
public class App extends Application {
    /** Size the menu and settings layouts were designed for. */
    private static final double MENU_WIDTH = 1000;
    private static final double MENU_HEIGHT = 800;
    private static final double SETTINGS_WIDTH = 800;
    private static final double SETTINGS_HEIGHT = 600;
    private static final double MIN_WINDOW_WIDTH = 640;
    private static final double MIN_WINDOW_HEIGHT = 480;

    private enum SettingsReturnTarget {
        MENU,
        GAME
    }

    public static void main(String[] args) {
        launch(args);
    }

    private Scene scene;
    private ResizableController activeController;
    public static Font pressStartFont;
    @Getter
    private String currentPlayer = "Player";
    private SettingsReturnTarget settingsReturnTarget = SettingsReturnTarget.MENU;

    @Override
    public void start(Stage primaryStage) {
        log.info("Application startup initiated");
        try {
            loadLanguage();
            pressStartFont = Font.loadFont(getClass().getResourceAsStream("/fonts/PressStart2P.ttf"), 18);

            primaryStage.setTitle(I18n.get("app.title"));
            sizeWindowToScreen(primaryStage);

            scene = new Scene(new Pane());
            scene.getStylesheets().add(getClass().getResource("/application.css").toString());
            primaryStage.setScene(scene);

            switchToMenu();

            primaryStage.show();
            log.info("Primary stage is visible");
        } catch (Exception e) {
            log.fatal("Application failed during startup", e);
            Platform.exit();
        }
    }

    /** Opens at the designed size, or smaller if the screen cannot show it, centred on the primary screen. */
    private void sizeWindowToScreen(Stage stage) {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        WindowSizing.Size size = WindowSizing.initialSize(screen.getWidth(), screen.getHeight(), MENU_WIDTH, MENU_HEIGHT);
        WindowSizing.Size min = WindowSizing.minimumSize(screen.getWidth(), screen.getHeight(), MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT);
        stage.setWidth(size.width());
        stage.setHeight(size.height());
        stage.setMinWidth(Math.min(min.width(), size.width()));
        stage.setMinHeight(Math.min(min.height(), size.height()));
        stage.setX(screen.getMinX() + (screen.getWidth() - size.width()) / 2);
        stage.setY(screen.getMinY() + (screen.getHeight() - size.height()) / 2);
    }

    @Override
    public void stop() throws Exception {
        if (activeController != null) {
            activeController.stop();
        }
        super.stop();
    }

    private void loadLanguage() {
        try {
            I18n.setLocaleTag(KeyBindingsRepository.loadLanguageTag());
        } catch (KeyBindingsException e) {
            log.warn("Loading language failed, using default language", e);
            I18n.setLocaleTag("en");
        }
    }

    private FXMLLoader loader(String fxml) {
        return new FXMLLoader(getClass().getResource(fxml), I18n.bundle());
    }

    private void show(Parent root, ResizableController controller) {
        controller.setApp(this);
        activeController = controller;
        scene.setRoot(root);
    }

    public void switchToGame(String playerName) throws IOException {
        log.info("Switching to game for player {}", playerName);
        this.currentPlayer = playerName;
        FXMLLoader gameLoader = loader("/game.fxml");
        Parent root = gameLoader.load();
        GameController gc = gameLoader.getController();
        show(root, gc);
        gc.startGame(playerName);
    }

    public void continueGame(String playerName) throws IOException {
        this.currentPlayer = playerName;
        log.info("Continuing game for player {}", playerName);
        FXMLLoader gameLoader = loader("/game.fxml");
        Parent root = gameLoader.load();
        GameController gc = gameLoader.getController();
        show(root, gc);
        gc.continueGame(playerName);
    }

    public void switchToMenu() throws IOException {
        log.debug("Switching to menu scene");
        FXMLLoader menuLoader = loader("/menu.fxml");
        Parent root = menuLoader.load();
        MenuController mc = menuLoader.getController();
        show(new FitPane(root, MENU_WIDTH, MENU_HEIGHT), mc);
        mc.startMenu();
    }

    private void switchToSettings() throws IOException {
        log.debug("Switching to settings scene");
        FXMLLoader settingsLoader = loader("/options.fxml");
        Parent root = settingsLoader.load();
        SettingsController sc = settingsLoader.getController();
        show(new FitPane(root, SETTINGS_WIDTH, SETTINGS_HEIGHT), sc);
    }

    public void switchToSettingsFromMenu() throws IOException {
        settingsReturnTarget = SettingsReturnTarget.MENU;
        switchToSettings();
    }

    public void switchToSettingsFromGame() throws IOException {
        settingsReturnTarget = SettingsReturnTarget.GAME;
        switchToSettings();
    }

    public void returnFromSettings() throws IOException {
        if (settingsReturnTarget == SettingsReturnTarget.GAME) {
            continueGame(currentPlayer);
        } else {
            switchToMenu();
        }
    }

}
