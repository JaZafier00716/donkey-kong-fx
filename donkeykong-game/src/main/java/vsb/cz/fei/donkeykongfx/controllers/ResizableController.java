package vsb.cz.fei.donkeykongfx.controllers;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import vsb.cz.fei.donkeykongfx.App;
import vsb.cz.fei.donkeykongfx.DrawingTimer;
import vsb.cz.fei.donkeykongfx.I18n;
import vsb.cz.fei.donkeykongfx.view.Dialogs;
import vsb.cz.fei.donkeykongfx.levels.Level;
import vsb.cz.fei.donkeykongfx.view.Letterbox;

import java.io.PrintWriter;
import java.io.StringWriter;

@Log4j2
public abstract class ResizableController {

    @Getter
    @Setter
    private App app;
    @Getter
    private DrawingTimer timer;
    private static final Color BACKGROUND = Color.web("#121212");

    private Font fpsFont;
    private double fpsSum = 0;
    private double fpsCount = 0;
    private int averageFps = 0;

    void printAlert(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
          message = e.getClass().getSimpleName();
        }
        log.warn("Handled UI exception shown to the player: {}", message, e);
        Alert alert = Dialogs.style(new Alert(Alert.AlertType.WARNING));
        alert.setHeaderText(I18n.get("alert.loadingProblem"));
        alert.getDialogPane().setContentText(message);
        // expandable stacktrace detail
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        TextArea textArea = new TextArea(sw.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        alert.getDialogPane().setExpandableContent(textArea);
        alert.showAndWait();
    }

    @FXML
    protected StackPane canvaContainer;

    @FXML
    protected Canvas canvas; // optional in controllers that don't use a canvas

    /**
     * Makes the canvas always fill its container. The canvas is unmanaged so that its current size
     * never acts as a minimum size of the container (which would stop the window from shrinking).
     */
    protected void installCanvasSizing() {
        if (canvaContainer == null || canvas == null) {
            return;
        }
        canvas.setManaged(false);
        canvas.widthProperty().bind(canvaContainer.widthProperty());
        canvas.heightProperty().bind(canvaContainer.heightProperty());
    }

    /**
     * Clears the canvas and switches the context to design coordinates: scaled uniformly to fit the
     * canvas, centred, and clipped so nothing is drawn into the letterbox bars.
     * Must be paired with {@link #endDesignSpace(GraphicsContext)}.
     */
    protected void beginDesignSpace(GraphicsContext gc, double canvasWidth, double canvasHeight) {
        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, canvasWidth, canvasHeight);

        Letterbox box = Letterbox.fit(canvasWidth, canvasHeight, Level.DESIGN_WIDTH, Level.DESIGN_HEIGHT);
        gc.save();
        gc.translate(box.offsetX(), box.offsetY());
        gc.scale(box.scale(), box.scale());
        gc.beginPath();
        gc.rect(0, 0, Level.DESIGN_WIDTH, Level.DESIGN_HEIGHT);
        gc.clip();
    }

    protected void endDesignSpace(GraphicsContext gc) {
        gc.restore();
    }

    public void start(DrawingTimer timer) {
        if(this.timer != null) {
            try {
                this.timer.stop();
            } catch (Exception e) {
                log.warn("Timer stop failed while replacing with a new timer", e);
            }
        }
        this.timer = timer;
        if(this.timer != null) {
            this.timer.start();
        }
    }


    public void stop() {
        if(this.timer != null) {
            try {
                this.timer.stop();
            } catch (Exception e) {
                log.warn("Timer stop failed because timer was already stopped", e);
            }
        }
    }

    protected void drawFps(GraphicsContext gc, double delta) {
        if(gc == null) return;
        if(delta <= 0) return;

        int fps = calcFps(delta);
        if (fpsFont == null) {
            fpsFont = new Font("Arial", 10);
        }
        gc.setFont(fpsFont);
        gc.setFill(Color.WHITE);

        double w = gc.getCanvas().getWidth();
        gc.fillText(String.format("FPS: %3d", fps < 1000 ? fps : 999), w-50, 10);
    }

    private int calcFps(double delta) {
        fpsSum += 1 / delta;
        fpsCount += 1;
        if (fpsCount >= 100) {
            averageFps = (int) (fpsSum / fpsCount);
            fpsSum = 0;
            fpsCount = 0;
        }
        return averageFps;
    }
}
