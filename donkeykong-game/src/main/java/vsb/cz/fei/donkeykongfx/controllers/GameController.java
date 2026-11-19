package vsb.cz.fei.donkeykongfx.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import lombok.extern.log4j.Log4j2;
import vsb.cz.fei.donkeykongfx.DrawingTimer;
import vsb.cz.fei.donkeykongfx.GameSaveStore;
import vsb.cz.fei.donkeykongfx.GameState;
import vsb.cz.fei.donkeykongfx.I18n;
import vsb.cz.fei.donkeykongfx.levels.Level;
import vsb.cz.fei.donkeykongfx.score.Score;
import vsb.cz.fei.donkeykongfx.score.ScoreRestClient;
import vsb.cz.fei.donkeykongfx.settings.KeyBindings;

import java.io.IOException;

@Log4j2
public class GameController extends SettingsAffected {
    /** Longest simulated step; a longer frame (window drag, debugger) must not let sprites skip through platforms. */
    private static final double MAX_FRAME_SECONDS = 0.05;
    /** The 96 px game-over title was designed for a 1000 px wide window. */
    private static final double TITLE_FONT_AT_1000_PX = 96;

    private Level level;
    private long lastFrame = 0;

    @FXML
    private BorderPane pauseMenu;
    @FXML
    private BorderPane gameOverContainer;

    @FXML
    private Text endText;

    @FXML
    private Button menuButton;

    @FXML
    private Button menuButton1;

    @FXML
    private Button optionsButton;

    @FXML
    private Button quitButton;

    @FXML
    private Button quitButton1;

    @FXML
    private Button resumeButton;

    @FXML
    private Button restartButton;

    @FXML
    void onMenuButton() {
        try {
            leaveGame();
            getApp().switchToMenu();
        } catch (Exception e) {
            printAlert(e);
        }
    }

    @FXML
    void onOptionsButton() {
        try {
            leaveGame();
            getApp().switchToSettingsFromGame();
        } catch (Exception e) {
            printAlert(e);
        }
    }

    @FXML
    void onQuitButton() {
        leaveGame();
        Platform.exit();
    }

    /** Stops the loop and keeps the progress, unless the game is already over. */
    private void leaveGame() {
        stop();
        if (!isGameOverVisible()) {
            saveGame();
        }
    }

    @FXML
    void onResumeButton() {
        togglePauseMenu();
    }

    @FXML
    void onRestartButton() {
        try {
            stop();
            String playerName = level.getPlayer().getPlayerName();
            level = new Level(playerName);
            startGame(playerName);
        } catch (Exception e) {
            printAlert(e);
        }
    }

    void togglePauseMenu() {
        if (level == null || isGameOverVisible()) {
            return;
        }
        boolean show = !pauseMenu.isVisible();
        pauseMenu.setVisible(show);
        pauseMenu.setDisable(!show);
        pauseMenu.setOpacity(show ? 1.0 : 0.0);
        if (show) {
            pauseMenu.toFront();
            releaseAllKeys(); // a key released while the menu is open would otherwise stay "pressed"
        } else {
            pauseMenu.toBack();
        }
        level.setPause(show);
    }

    private void showGameOver(Level.GameOverReason reason) {
        gameOverContainer.setVisible(true);
        gameOverContainer.setDisable(false);
        gameOverContainer.setOpacity(1.0);
        gameOverContainer.toFront();

        String playerName = level.getPlayer().getPlayerName();
        if (reason == Level.GameOverReason.PLAYER_WON) {
            endText.setText(I18n.get("game.won"));
            Score score = new Score(playerName, level.getPlayer().getScore());
            // the HTTP call may take seconds when the score service is down, so keep it off the UI thread
            ScoreRestClient.getDefault()
                    .saveAsync(score, level.getCurrentLevelNumber(), level.getDurationSeconds(), level.getDeathCount())
                    .whenComplete((ignored, error) -> {
                        if (error == null) {
                            log.info("Saved score: {} - {}", score.getNickName(), score.getScore());
                        } else {
                            log.warn("Score service unavailable; the result was not saved", error);
                        }
                    });
        } else {
            endText.setText(I18n.get("game.over"));
            log.info("Game over for player {}", playerName);
        }
        deleteSave();
    }

    private void hideGameOver() {
        gameOverContainer.setVisible(false);
        gameOverContainer.setDisable(true);
        gameOverContainer.setOpacity(0.0);
        gameOverContainer.toBack();
    }

    private boolean isGameOverVisible() {
        return gameOverContainer != null && gameOverContainer.isVisible();
    }

    // key state flags to support simultaneous keys
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;

    @FXML
    void initialize() {
        installCanvasSizing();
        loadKeyBindings();
        keyboardListener(keyBindings);

        // the title was sized for a 1000 px wide window; shrink it with narrower windows
        canvaContainer.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double width = newWidth.doubleValue();
            endText.setWrappingWidth(width);
            endText.setStyle("-fx-font-size: " + Math.min(TITLE_FONT_AT_1000_PX, TITLE_FONT_AT_1000_PX * width / 1000) + ";");
        });
    }

    void keyboardListener(KeyBindings keyBindings) {
        canvas.setFocusTraversable(true);

        canvas.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                togglePauseMenu();
                return;
            }
            if (level == null || level.getPlayer() == null) {
                return;
            }

            String action = keyBindings.getActionForKey(event.getCode());
            if (action == null) {
                return;
            }

            switch (action) {
                case "move_left" -> {
                    leftPressed = true;
                    rightPressed = false;
                }
                case "move_right" -> {
                    rightPressed = true;
                    leftPressed = false;
                }
                case "climb_up" -> {
                    upPressed = true;
                    downPressed = false;
                }
                case "climb_down" -> {
                    downPressed = true;
                    upPressed = false;
                }
                case "jump" -> level.getPlayer().jump();
                default -> { /* ignore other keys */ }
            }

            updateMovementFromKeys();
        });

        canvas.setOnKeyReleased(event -> {
            // update key state even if player is not present to avoid stuck state when player is created later
            String action = keyBindings.getActionForKey(event.getCode());
            if (action == null) {
                return;
            }

            switch (action) {
                case "move_left" -> leftPressed = false;
                case "move_right" -> rightPressed = false;
                case "climb_up" -> upPressed = false;
                case "climb_down" -> downPressed = false;
                default -> { /* ignore other keys */ }
            }

            updateMovementFromKeys();
        });
    }

    private void releaseAllKeys() {
        leftPressed = false;
        rightPressed = false;
        upPressed = false;
        downPressed = false;
        updateMovementFromKeys();
    }

    // compute current direction from pressed keys and send to player
    private void updateMovementFromKeys() {
        if (level == null || level.getPlayer() == null) return;

        int dx = 0;
        int dy = 0;

        if (leftPressed && !rightPressed) dx = -1;
        else if (rightPressed && !leftPressed) dx = 1;

        if (upPressed && !downPressed) dy = -1;
        else if (downPressed && !upPressed) dy = 1;

        level.getPlayer().setMovementDirection(dx, dy);
    }

    public void startGame(String playerName) {
        log.debug("Starting game loop for player {}", playerName);
        hideGameOver();
        // ensure canvas has focus so key events are received
        canvas.requestFocus();

        if (level == null) {
            level = new Level(playerName);
        }
        releaseAllKeys();
        lastFrame = 0;

        level.setOnGameOver(reason -> Platform.runLater(() -> {
            stop();
            showGameOver(reason);
        }));
        level.startAutonomousEntities();

        start(new DrawingTimer(canvas, (gc, w, h, now) -> {
            double delta = lastFrame == 0 ? 0 : Math.min((now - lastFrame) / 1_000_000_000D, MAX_FRAME_SECONDS);
            lastFrame = now;

            beginDesignSpace(gc, w, h);
            level.draw(gc);
            level.update(delta);
            endDesignSpace(gc);

            drawFps(gc, delta);
        }));
    }

    public void continueGame(String playerName) {
        log.debug("Attempting to continue game for player {}", playerName);
        loadGame(playerName);
        startGame(playerName);
    }

    public void saveGame() {
        if (level == null) {
            return;
        }
        try {
            GameSaveStore.getDefault().save(level.toGameState());
            log.debug("Game state saved for player {}", level.getPlayer().getPlayerName());
        } catch (IOException e) {
            log.warn("Saving game state failed, user can continue without persisted state", e);
            printAlert(e);
        }
    }

    public void loadGame(String playerName) {
        GameSaveStore store = GameSaveStore.getDefault();
        if (!store.exists()) {
            log.debug("No save file found, starting from fresh state");
            return;
        }
        try {
            GameState state = store.load();
            if (level == null) {
                level = new Level(state.playerName);
            }
            level.fromGameState(state);
            log.info("Game state loaded for player {}", state.playerName);
        } catch (IOException | RuntimeException e) {
            // an unreadable (for example older-version) save is useless: drop it and start fresh
            log.warn("Loading game state failed, starting a new game", e);
            deleteSave();
            level = new Level(playerName);
        }
    }

    public void deleteSave() {
        GameSaveStore.getDefault().delete();
    }

    @Override
    public void stop() {
        if (level != null) {
            level.stopAutonomousEntities();
        }
        super.stop();
    }
}
