package vsb.cz.fei.donkeykongfx.gameobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovableTypeTest {
    private static final double DT = 1.0 / 60;
    private final MovableType walker = new MovableType(0, 20, 20, 100, 36, true, true);
    private final MovableType floater = new MovableType(0, 20, 20, 100, 0, false, false);

    @Test
    void horizontalVelocityFollowsTheDirection() {
        assertEquals(20, walker.horizontalVelocity(1));
        assertEquals(-20, walker.horizontalVelocity(-1));
        assertEquals(0, walker.horizontalVelocity(0));
    }

    @Test
    void gravityAcceleratesAnObjectInTheAir() {
        double vy = walker.verticalVelocity(0, 0, false, false, DT);

        assertEquals(20 * DT * MovableType.PHYSICS_SCALE, vy, 1e-12);
        assertTrue(walker.verticalVelocity(vy, 0, false, false, DT) > vy);
    }

    @Test
    void standingOnGroundStopsFalling() {
        assertEquals(0, walker.verticalVelocity(15, 0, false, true, DT));
    }

    @Test
    void fallSpeedIsCapped() {
        double vy = 0;
        for (int i = 0; i < 10_000; i++) {
            vy = walker.verticalVelocity(vy, 0, false, false, DT);
        }
        assertEquals(100, vy);
    }

    @Test
    void objectsWithoutGravityKeepTheirVelocity() {
        assertEquals(0, floater.verticalVelocity(0, 0, false, false, DT));
    }

    @Test
    void climbingMovesAtBaseSpeedInTheRequestedDirectionAndHoldsStillWithoutInput() {
        assertEquals(-20, walker.verticalVelocity(5, -1, true, false, DT));
        assertEquals(20, walker.verticalVelocity(5, 1, true, false, DT));
        assertEquals(0, walker.verticalVelocity(5, 0, true, false, DT));
    }

    @Test
    void speedsAreIndependentOfAnyWindowSize() {
        // regression: the step used to be multiplied by the pixel scale of the window
        double step = walker.horizontalVelocity(1) * DT * MovableType.PHYSICS_SCALE;
        assertEquals(20 * DT * 3.125, step, 1e-12);
    }
}
