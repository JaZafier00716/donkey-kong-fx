package vsb.cz.fei.donkeykongfx.controllers;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import lombok.extern.log4j.Log4j2;
import vsb.cz.fei.donkeykongfx.DrawingTimer;
import vsb.cz.fei.donkeykongfx.GameSaveStore;
import vsb.cz.fei.donkeykongfx.GameState;
import vsb.cz.fei.donkeykongfx.gameobjects.entities.donkeykong.DonkeyKong;
import vsb.cz.fei.donkeykongfx.levels.Level;
import vsb.cz.fei.donkeykongfx.score.Leaderboard;
import vsb.cz.fei.donkeykongfx.score.Score;
import vsb.cz.fei.donkeykongfx.score.ScoreRestClient;

import java.io.IOException;
import java.util.List;

@Log4j2
public class MenuController extends ResizableController {
    private DonkeyKong menuDonkeyKong;
    private long lastFrame = 0;
    private static final int MAX_PLAYER_NAME_LENGTH = 12;
    private static final int LEADERBOARD_SIZE = 5;
    private String savedPlayerName;


    @FXML
    private TableColumn<Score, String> columnNickName;

    @FXML
    private TableColumn<Score, Integer> columnScore;

    @FXML
    private TableView<Score> scoreTable;

    @FXML
    private StackPane gameContainer;

    @FXML
    private BorderPane mainMenu;

    @FXML
    private Button optionsButton;

    @FXML
    private Button quitButton;

    @FXML
    private Button startButton;

    @FXML
    private Button continueButton;

    @FXML
    private TextField playerNameField;


    @FXML
    void onBtnOptions() {
        try {
            if (getTimer() != null) {
                stop();
            }
            if (getApp() == null) {
                throw new IllegalStateException("App has not been initialized");
            }
            log.debug("Opening settings from menu");
            getApp().switchToSettingsFromMenu();
        } catch (Exception e) {
            log.warn("Handled error while opening settings from menu", e);
            printAlert(e);
        }
    }

    @FXML
    void onBtnQuit() {
        log.info("Quit requested from menu");
        Platform.exit();
    }

    @FXML
    void onBtnStart() {
        if (playerNameField.getText().isBlank()) {
            playerNameField.setStyle("-fx-prompt-text-fill: red;");
            log.warn("Start blocked because player name is blank");
            return;
        }
        try {
            if (getTimer() != null) {
                stop();
            }
            if (getApp() == null) {
                throw new IllegalStateException("App has not been initialized");
            }
            deleteSave();
            log.info("Starting new game for player {}", playerNameField.getText());
            getApp().switchToGame(playerNameField.getText());
        } catch (Exception e) {
            log.warn("Handled error while starting a new game", e);
            printAlert(e);
        }
    }

    @FXML
    void onBtnContinue() {
        try {
            if (getTimer() != null) {
                stop();
            }
            if (getApp() == null) {
                throw new IllegalStateException("App has not been initialized");
            }
            if (!canContinueCurrentPlayer()) {
                updateContinueButton();
                return;
            }
            log.info("Continuing game for player {}", playerNameField.getText());
            getApp().continueGame(playerNameField.getText());
        } catch (Exception e) {
            log.warn("Handled error while continuing game", e);
            printAlert(e);
        }
    }


    @FXML
    void initialize() {
        assert canvaContainer != null : "fx:id=\"canvaContainer\" was not injected: check your FXML file 'menu.fxml'.";
        assert canvas != null : "fx:id=\"canvas\" was not injected: check your FXML file 'menu.fxml'.";
        assert columnNickName != null : "fx:id=\"columnNickName\" was not injected: check your FXML file 'menu.fxml'.";
        assert columnScore != null : "fx:id=\"columnScore\" was not injected: check your FXML file 'menu.fxml'.";
        assert continueButton != null : "fx:id=\"continueButton\" was not injected: check your FXML file 'menu.fxml'.";
        assert gameContainer != null : "fx:id=\"gameContainer\" was not injected: check your FXML file 'menu.fxml'.";
        assert mainMenu != null : "fx:id=\"mainMenu\" was not injected: check your FXML file 'menu.fxml'.";
        assert optionsButton != null : "fx:id=\"optionsButton\" was not injected: check your FXML file 'menu.fxml'.";
        assert playerNameField != null : "fx:id=\"playerNameField\" was not injected: check your FXML file 'menu.fxml'.";
        assert quitButton != null : "fx:id=\"quitButton\" was not injected: check your FXML file 'menu.fxml'.";
        assert scoreTable != null : "fx:id=\"scoreTable\" was not injected: check your FXML file 'menu.fxml'.";
        assert startButton != null : "fx:id=\"startButton\" was not injected: check your FXML file 'menu.fxml'.";


        readSavedPlayerName();

        // Limit player name length
        playerNameField.setTextFormatter(new TextFormatter<String>(change -> {
            if(!change.getControlNewText().isBlank()) {
                playerNameField.setStyle("-fx-prompt-text-fill: black;");
            }
            if (change.getControlNewText().length() <= MAX_PLAYER_NAME_LENGTH) {
                return change;
            } else {
                return null;
            }
        }));
        playerNameField.textProperty().addListener((observable, oldValue, newValue) -> updateContinueButton());
        updateContinueButton();

        columnNickName.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getNickName()));
        columnScore.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getScore()));

        loadLeaderboard();
        installCanvasSizing();
    }

    /** Fetches the best scores without blocking the UI thread; shows made-up scores while offline. */
    private void loadLeaderboard() {
        ScoreRestClient.getDefault().loadAsync().whenComplete((scores, error) -> Platform.runLater(() -> {
            if (error != null) {
                log.warn("Leaderboard unavailable; using fallback scores: {}", error.getMessage());
            }
            scoreTable.getItems().setAll(Leaderboard.top(error == null ? scores : List.of(), LEADERBOARD_SIZE));
            log.debug("Loaded {} score rows into menu leaderboard", scoreTable.getItems().size());
        }));
    }

    /** Pre-fills the name of the saved game and enables "Continue"; an unreadable save is ignored. */
    private void readSavedPlayerName() {
        GameSaveStore store = GameSaveStore.getDefault();
        if (!store.exists()) {
            hideContinueButton();
            log.debug("No saved state found; continue button hidden");
            return;
        }
        try {
            GameState state = store.load();
            if (state.playerName == null || state.playerName.isEmpty()) {
                hideContinueButton();
                log.warn("Saved state exists but player name is missing; continue button hidden");
            } else {
                savedPlayerName = state.playerName;
                playerNameField.setText(state.playerName);
                log.debug("Loaded saved player name {} into menu", state.playerName);
            }
        } catch (IOException e) {
            hideContinueButton();
            log.warn("Saved state is unreadable and will be ignored", e);
        }
    }

    private boolean canContinueCurrentPlayer() {
        return savedPlayerName != null && savedPlayerName.equals(playerNameField.getText())
                && GameSaveStore.getDefault().exists();
    }

    private void updateContinueButton() {
        if (canContinueCurrentPlayer()) {
            continueButton.setDisable(false);
            continueButton.setVisible(true);
            continueButton.setManaged(true);
        } else {
            hideContinueButton();
        }
    }

    private void hideContinueButton() {
        continueButton.setDisable(true);
        continueButton.setVisible(false);
        continueButton.setManaged(false);
    }

    private void deleteSave() {
        GameSaveStore.getDefault().delete();
        savedPlayerName = null;
    }

    public void startMenu() {
        if (menuDonkeyKong == null) {
            // the preview is drawn at 4x in the same design coordinates as the game
            menuDonkeyKong = new DonkeyKong(new ResizableDimension(Level.DESIGN_WIDTH, Level.DESIGN_HEIGHT),
                    32, 4, new Point2D(0, 25));
        }

        lastFrame = 0;
        log.debug("Starting animated menu background");

        start(new DrawingTimer(canvas, (gc, cw, ch, now) -> {
            double delta = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000D;
            lastFrame = now;

            beginDesignSpace(gc, cw, ch);
            menuDonkeyKong.render(gc);
            menuDonkeyKong.updateTimer(delta); // advance animation frame
            endDesignSpace(gc);
        }));
    }
}
