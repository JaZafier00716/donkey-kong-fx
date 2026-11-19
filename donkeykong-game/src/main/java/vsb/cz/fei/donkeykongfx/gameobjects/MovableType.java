package vsb.cz.fei.donkeykongfx.gameobjects;

import javafx.geometry.Point2D;

/**
 * Movement profile of a moving object (speeds, gravity, jump) and the physics step itself.
 * <p>
 * All values are in design units, so movement does not depend on the window size. The numbers were
 * tuned in the original 800 px high window, therefore every speed is multiplied by
 * {@link #PHYSICS_SCALE}.
 */
public record MovableType(
        double initSpeed,
        double baseSpeed,
        double gravityScale,
        double maxFallSpeed,
        double jumpImpulse,
        boolean affectedByGravity,
        boolean canJump
) {
    /** Original window height (800 px) divided by the design height (256): keeps the tuned game feel. */
    public static final double PHYSICS_SCALE = 800.0 / 256.0;

    public void apply(MovableGameObject obj, double dt) {
        obj.setPrevPosition(obj.getPosition());

        int dirX = obj.getDirectionX(); // 1 - right, 0 - standStill, -1 - left
        obj.setVelocityX(horizontalVelocity(dirX));

        double vy = verticalVelocity(obj.getVelocityY(), obj.getDirectionY(), obj.isOnLadder(), obj.isOnGround(), dt);
        obj.setVelocityY(vy);

        Point2D newPos = obj.getPosition().add(
                obj.getVelocityX() * dt * PHYSICS_SCALE,
                obj.getVelocityY() * dt * PHYSICS_SCALE);
        obj.setPosition(newPos);
    }

    /** Horizontal velocity for a direction: 1 right, -1 left, 0 standing still. */
    public double horizontalVelocity(int dirX) {
        return dirX == 0 ? 0 : Math.copySign(baseSpeed, dirX);
    }

    /**
     * Vertical velocity after {@code dt} seconds.
     *
     * @param vy       current vertical velocity (positive is down)
     * @param dirY     1 - down, 0 - no vertical movement, -1 - up (only used on a ladder)
     * @param onLadder whether the object is climbing
     * @param onGround whether the object stands on a platform
     */
    public double verticalVelocity(double vy, int dirY, boolean onLadder, boolean onGround, double dt) {
        if (onLadder) {
            vy = dirY == 0 ? 0 : Math.copySign(baseSpeed, dirY);
        } else if (affectedByGravity && !onGround) {
            vy += gravityScale * dt * PHYSICS_SCALE;
        } else if (onGround) {
            vy = 0;
        }
        return Math.max(-maxFallSpeed, Math.min(maxFallSpeed, vy));
    }
}
