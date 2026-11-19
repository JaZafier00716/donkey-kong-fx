package vsb.cz.fei.donkeykongfx.controllers;

import javafx.geometry.Dimension2D;
import lombok.Getter;
import lombok.ToString;

/**
 * Size of the play field in design units plus the factor between design units and sprite pixels.
 * <p>
 * Game logic always runs with the design size (see {@code Level}), so {@link #getScale()} is 1 there.
 * The window size is handled only by the renderer (see {@code Letterbox}).
 */
@ToString
public class ResizableDimension {
    /** Height in design units for which {@code scale == 1}: 32 tiles of 8 pixels. */
    public static final double DESIGN_HEIGHT = 32 * 8;

    private final Dimension2D dimension;
    @Getter
    private final double scale;

    public ResizableDimension(Dimension2D dimension) {
        this.dimension = dimension;
        this.scale = dimension.getHeight() / DESIGN_HEIGHT;
    }

    public ResizableDimension(double width, double height) {
        this(new Dimension2D(width, height));
    }

    public double getWidth() {
        return dimension.getWidth();
    }

    public double getHeight() {
        return dimension.getHeight();
    }
}
