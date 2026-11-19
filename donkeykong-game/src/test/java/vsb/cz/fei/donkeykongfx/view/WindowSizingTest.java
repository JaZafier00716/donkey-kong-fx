package vsb.cz.fei.donkeykongfx.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowSizingTest {

    @Test
    void largeScreenKeepsThePreferredSize() {
        WindowSizing.Size size = WindowSizing.initialSize(3840, 2120, 1000, 800);

        assertEquals(1000, size.width());
        assertEquals(800, size.height());
    }

    @Test
    void smallScreenShrinksTheWindowAndKeepsItsAspectRatio() {
        // 1366x768 laptop with a taskbar: 728 px of usable height
        WindowSizing.Size size = WindowSizing.initialSize(1366, 728, 1000, 800);

        assertTrue(size.height() <= 728 * WindowSizing.SCREEN_FRACTION);
        assertTrue(size.width() <= 1366 * WindowSizing.SCREEN_FRACTION);
        assertEquals(1000.0 / 800.0, size.width() / size.height(), 0.01);
    }

    @Test
    void portraitMonitorLimitsTheWidth() {
        WindowSizing.Size size = WindowSizing.initialSize(1080, 1860, 1000, 800);

        assertTrue(size.width() <= 1080 * WindowSizing.SCREEN_FRACTION);
        assertEquals(1000.0 / 800.0, size.width() / size.height(), 0.01);
    }

    @Test
    void unknownScreenFallsBackToThePreferredSize() {
        WindowSizing.Size size = WindowSizing.initialSize(0, 0, 1000, 800);

        assertEquals(1000, size.width());
        assertEquals(800, size.height());
    }

    @Test
    void minimumSizeNeverExceedsTheScreen() {
        WindowSizing.Size size = WindowSizing.minimumSize(500, 400, 640, 480);

        assertEquals(500, size.width());
        assertEquals(400, size.height());
        assertEquals(640, WindowSizing.minimumSize(1920, 1080, 640, 480).width());
    }
}
