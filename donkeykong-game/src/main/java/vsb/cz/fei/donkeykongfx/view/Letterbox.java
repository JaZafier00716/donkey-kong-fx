package vsb.cz.fei.donkeykongfx.view;

/**
 * Uniform "fit and centre" mapping of a fixed design area onto a window of any size.
 * <p>
 * The game logic lives in design coordinates only; this class tells the renderer how to scale and
 * where to place the design area so that its aspect ratio is kept and the remaining space becomes
 * letterbox bars. It has no JavaFX dependency, so it can be unit-tested.
 *
 * @param scale   pixels per design unit
 * @param offsetX left edge of the design area in window pixels
 * @param offsetY top edge of the design area in window pixels
 * @param viewWidth  width of the design area in window pixels
 * @param viewHeight height of the design area in window pixels
 */
public record Letterbox(double scale, double offsetX, double offsetY, double viewWidth, double viewHeight) {

    /**
     * Computes the largest centred rectangle with the design aspect ratio that fits the window.
     * A window with no area yields a zero scale instead of NaN/Infinity values.
     */
    public static Letterbox fit(double windowWidth, double windowHeight, double designWidth, double designHeight) {
        if (designWidth <= 0 || designHeight <= 0) {
            throw new IllegalArgumentException("Design size must be positive");
        }
        if (windowWidth <= 0 || windowHeight <= 0) {
            return new Letterbox(0, 0, 0, 0, 0);
        }
        double scale = Math.min(windowWidth / designWidth, windowHeight / designHeight);
        double viewWidth = designWidth * scale;
        double viewHeight = designHeight * scale;
        return new Letterbox(scale, (windowWidth - viewWidth) / 2, (windowHeight - viewHeight) / 2, viewWidth, viewHeight);
    }

    /** Converts a window x coordinate (for example a mouse position) to design coordinates. */
    public double toDesignX(double windowX) {
        return scale == 0 ? 0 : (windowX - offsetX) / scale;
    }

    /** Converts a window y coordinate to design coordinates. */
    public double toDesignY(double windowY) {
        return scale == 0 ? 0 : (windowY - offsetY) / scale;
    }
}
