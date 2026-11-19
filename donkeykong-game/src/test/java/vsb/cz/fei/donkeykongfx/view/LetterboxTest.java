package vsb.cz.fei.donkeykongfx.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LetterboxTest {
    private static final double DESIGN_W = 224;
    private static final double DESIGN_H = 256;
    private static final double EPS = 1e-9;

    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({
            "1280, 720",
            "1366, 768",
            "1920, 1080",
            "2560, 1440",
            "3840, 2160",
            "720, 1280",   // portrait
            "1080, 1920",  // portrait
            "300, 300",
            "224, 256",
    })
    void fitsInsideTheWindowKeepsAspectRatioAndIsCentred(double w, double h) {
        Letterbox box = Letterbox.fit(w, h, DESIGN_W, DESIGN_H);

        assertEquals(DESIGN_W / DESIGN_H, box.viewWidth() / box.viewHeight(), EPS, "aspect ratio");
        assertTrue(box.viewWidth() <= w + EPS && box.viewHeight() <= h + EPS, "inside the window");
        assertTrue(box.offsetX() >= -EPS && box.offsetY() >= -EPS);
        assertEquals(w, 2 * box.offsetX() + box.viewWidth(), EPS, "centred horizontally");
        assertEquals(h, 2 * box.offsetY() + box.viewHeight(), EPS, "centred vertically");
        // one dimension is always used completely: the design area is as large as possible
        assertTrue(Math.abs(box.viewWidth() - w) < EPS || Math.abs(box.viewHeight() - h) < EPS);
    }

    @Test
    void landscapeWindowIsLimitedByHeight() {
        Letterbox box = Letterbox.fit(1920, 1080, DESIGN_W, DESIGN_H);

        assertEquals(1080 / 256.0, box.scale(), EPS);
        assertEquals(0, box.offsetY(), EPS);
        assertEquals((1920 - 224 * 1080 / 256.0) / 2, box.offsetX(), EPS);
    }

    @Test
    void portraitWindowIsLimitedByWidth() {
        Letterbox box = Letterbox.fit(720, 1280, DESIGN_W, DESIGN_H);

        assertEquals(720 / 224.0, box.scale(), EPS);
        assertEquals(0, box.offsetX(), EPS);
        assertTrue(box.offsetY() > 0);
    }

    @Test
    void exactDesignSizeIsNotScaled() {
        Letterbox box = Letterbox.fit(DESIGN_W, DESIGN_H, DESIGN_W, DESIGN_H);

        assertEquals(1.0, box.scale(), EPS);
        assertEquals(0, box.offsetX(), EPS);
        assertEquals(0, box.offsetY(), EPS);
    }

    @ParameterizedTest
    @CsvSource({"1280, 720", "3840, 2160", "720, 1280"})
    void windowPointsMapBackToDesignCoordinates(double w, double h) {
        Letterbox box = Letterbox.fit(w, h, DESIGN_W, DESIGN_H);

        assertEquals(0, box.toDesignX(box.offsetX()), 1e-6);
        assertEquals(0, box.toDesignY(box.offsetY()), 1e-6);
        assertEquals(DESIGN_W, box.toDesignX(box.offsetX() + box.viewWidth()), 1e-6);
        assertEquals(DESIGN_H, box.toDesignY(box.offsetY() + box.viewHeight()), 1e-6);
        assertEquals(DESIGN_W / 2, box.toDesignX(w / 2), 1e-6, "window centre is the design centre");
        assertEquals(DESIGN_H / 2, box.toDesignY(h / 2), 1e-6);
    }

    @Test
    void emptyWindowGivesZeroScaleInsteadOfNaN() {
        Letterbox box = Letterbox.fit(0, 600, DESIGN_W, DESIGN_H);

        assertEquals(0, box.scale());
        assertEquals(0, box.toDesignX(10));
        assertEquals(0, box.toDesignY(10));
    }

    @Test
    void designSizeMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> Letterbox.fit(100, 100, 0, 256));
    }
}
