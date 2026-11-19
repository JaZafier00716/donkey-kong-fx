package vsb.cz.fei.donkeykongfx.gameobjects;

/**
 * Pure geometry rules for landing on a platform and bumping the head under one.
 * All values are in design units (y grows downwards); {@code scale} is the design-to-pixel factor
 * of the level (1 in the game). Kept free of JavaFX types so the rules can be unit-tested.
 */
public final class PlatformCollision {
    /** How far (design units) a landed object sinks into the platform so that contact is kept every frame. */
    public static final double RESTING_PENETRATION = 0.01;

    private PlatformCollision() {
    }

    /** Tolerance when the feet are compared with a platform top. */
    public static double verticalTolerance(double scale) {
        return scale;
    }

    /** Minimum horizontal overlap that counts as standing on a platform. */
    public static double horizontalTolerance(double scale) {
        return scale * 0.1;
    }

    /** Highest step that is climbed without jumping (platforms are slightly sloped). */
    public static double maxStep(double scale) {
        return scale * 4;
    }

    /**
     * @return true if an object whose feet moved from {@code prevBottom} to {@code currBottom} lands on a
     * platform whose top edge is at {@code platformTop}
     */
    public static boolean landsOn(double prevBottom, double currBottom, double platformTop,
                                  double overlapX, boolean movingDown, double scale) {
        if (overlapX <= horizontalTolerance(scale)) {
            return false;
        }
        double tolerance = verticalTolerance(scale);
        double maxStep = maxStep(scale);

        boolean crossedFromAbove = prevBottom <= platformTop + tolerance
                && currBottom >= platformTop - tolerance
                && movingDown;

        boolean step = prevBottom <= platformTop + maxStep
                && currBottom >= platformTop - tolerance
                && Math.abs(currBottom - prevBottom) <= maxStep + tolerance;

        return crossedFromAbove || step;
    }

    /**
     * @return true if an object moving up with its head at {@code currTop} (was {@code prevTop}) has just
     * crossed the underside of a platform ({@code platformBottom}) it overlaps horizontally
     */
    public static boolean hitsCeiling(double prevTop, double currTop, double platformBottom,
                                      double overlapX, boolean movingUp) {
        return movingUp && overlapX > 0 && prevTop >= platformBottom && currTop <= platformBottom;
    }
}
