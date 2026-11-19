package vsb.cz.fei.donkeykongfx.view;

/**
 * Picks a window size that fits the screen. Pure maths, no JavaFX dependency.
 */
public final class WindowSizing {

    /** Share of the usable screen area a freshly opened window may occupy. */
    public static final double SCREEN_FRACTION = 0.9;

    private WindowSizing() {
    }

    public record Size(double width, double height) {
    }

    /**
     * @param screenWidth     usable screen width (for example {@code Screen.getVisualBounds()})
     * @param screenHeight    usable screen height
     * @param preferredWidth  size the layout was designed for
     * @param preferredHeight size the layout was designed for
     * @return the preferred size, shrunk (keeping its aspect ratio) when it would not fit the screen
     */
    public static Size initialSize(double screenWidth, double screenHeight, double preferredWidth, double preferredHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return new Size(preferredWidth, preferredHeight);
        }
        double maxWidth = screenWidth * SCREEN_FRACTION;
        double maxHeight = screenHeight * SCREEN_FRACTION;
        double factor = Math.min(1.0, Math.min(maxWidth / preferredWidth, maxHeight / preferredHeight));
        return new Size(Math.floor(preferredWidth * factor), Math.floor(preferredHeight * factor));
    }

    /** Smallest window we allow, never larger than what the screen can show. */
    public static Size minimumSize(double screenWidth, double screenHeight, double minWidth, double minHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return new Size(minWidth, minHeight);
        }
        return new Size(Math.min(minWidth, screenWidth), Math.min(minHeight, screenHeight));
    }
}
