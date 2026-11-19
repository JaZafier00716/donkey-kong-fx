package vsb.cz.fei.donkeykongfx.view;

import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;

/**
 * Shows a fixed-size layout (menu, settings) scaled uniformly into whatever space is available,
 * centred, so the whole screen stays reachable on small monitors and does not float in a corner on
 * large ones.
 */
public class FitPane extends Region {
    private final Node content;
    private final double designWidth;
    private final double designHeight;
    private final Scale scale = new Scale(1, 1, 0, 0);

    public FitPane(Node content, double designWidth, double designHeight) {
        this.content = content;
        this.designWidth = designWidth;
        this.designHeight = designHeight;
        content.getTransforms().add(scale);
        getChildren().add(content);
        setStyle("-fx-background-color: #121212;");
    }

    @Override
    protected void layoutChildren() {
        Letterbox box = Letterbox.fit(getWidth(), getHeight(), designWidth, designHeight);
        scale.setX(box.scale());
        scale.setY(box.scale());
        content.resize(designWidth, designHeight);
        content.setLayoutX(box.offsetX());
        content.setLayoutY(box.offsetY());
    }

    @Override
    protected double computePrefWidth(double height) {
        return designWidth;
    }

    @Override
    protected double computePrefHeight(double width) {
        return designHeight;
    }

    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 0;
    }
}
