package vsb.cz.fei.donkeykongfx.gameobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformCollisionTest {
    private static final double SCALE = 1;
    private static final double PLATFORM_TOP = 100;

    @Test
    void fallingThroughThePlatformTopLands() {
        assertTrue(PlatformCollision.landsOn(98, 102, PLATFORM_TOP, 8, true, SCALE));
    }

    @Test
    void movingUpThroughThePlatformFromBelowDoesNotLandUnlessItIsASmallStep() {
        // feet well below the platform and rising: not a landing
        assertFalse(PlatformCollision.landsOn(140, 130, PLATFORM_TOP, 8, false, SCALE));
    }

    @Test
    void walkingUpASlopeStepLands() {
        // feet 3 units below the top of the next sloped tile: within the step height
        assertTrue(PlatformCollision.landsOn(103, 103, PLATFORM_TOP, 8, false, SCALE));
    }

    @Test
    void noHorizontalOverlapNeverLands() {
        assertFalse(PlatformCollision.landsOn(98, 102, PLATFORM_TOP, 0, true, SCALE));
        assertFalse(PlatformCollision.landsOn(98, 102, PLATFORM_TOP, 0.05, true, SCALE));
    }

    @Test
    void stillAboveThePlatformDoesNotLand() {
        assertFalse(PlatformCollision.landsOn(80, 85, PLATFORM_TOP, 8, true, SCALE));
    }

    @Test
    void ceilingIsHitOnlyWhenMovingUpAcrossTheUndersideWhileOverlapping() {
        assertTrue(PlatformCollision.hitsCeiling(112, 108, 110, 5, true));
        assertFalse(PlatformCollision.hitsCeiling(112, 108, 110, 5, false), "moving down");
        assertFalse(PlatformCollision.hitsCeiling(112, 108, 110, 0, true), "no overlap");
        assertFalse(PlatformCollision.hitsCeiling(120, 115, 110, 5, true), "has not reached the platform yet");
        assertFalse(PlatformCollision.hitsCeiling(105, 100, 110, 5, true), "already above the platform");
    }

    @Test
    void tolerancesFollowTheLevelScale() {
        assertTrue(PlatformCollision.maxStep(2) == 2 * PlatformCollision.maxStep(1));
        assertTrue(PlatformCollision.verticalTolerance(3) == 3);
    }
}
