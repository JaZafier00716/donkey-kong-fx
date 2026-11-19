package vsb.cz.fei.donkeykongfx.levels;

import javafx.geometry.Point2D;
import org.junit.jupiter.api.Test;
import vsb.cz.fei.donkeykongfx.GameState;
import vsb.cz.fei.donkeykongfx.gameobjects.entities.EntityState;
import vsb.cz.fei.donkeykongfx.gameobjects.entities.barrel.Barrel;
import vsb.cz.fei.donkeykongfx.gameobjects.entities.player.Player;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real level (platforms, ladders, physics) without a window: only drawing needs a display.
 */
class LevelTest {

    private static void run(Level level, double seconds, double dt) {
        for (double t = 0; t < seconds; t += dt) {
            level.update(dt);
        }
    }

    @Test
    void levelAlwaysUsesTheFixedDesignSize() {
        Level level = new Level("tester");

        assertEquals(224, level.getWidth());
        assertEquals(256, level.getHeight());
        assertEquals(1.0, level.getScale());
    }

    @Test
    void playerFallsOntoThePlatformAndStaysThere() {
        Level level = new Level("tester");
        Player player = level.getPlayer();

        run(level, 3, 1.0 / 60);

        assertTrue(player.isOnGround(), "player should have landed");
        double landedY = player.getPosition().getY();
        run(level, 2, 1.0 / 60);
        assertEquals(landedY, player.getPosition().getY(), 0.01, "player stays on the platform");
        assertTrue(landedY < 256, "inside the play field");
    }

    @Test
    void groundContactIsStableWhileStandingStill() {
        for (double dt : new double[]{1.0 / 30, 1.0 / 60, 1.0 / 144}) {
            Level level = new Level("tester");
            run(level, 3, dt);
            for (int frame = 0; frame < 20; frame++) {
                level.update(dt);
                assertTrue(level.getPlayer().isOnGround(), "lost ground contact at dt=" + dt + ", frame " + frame);
            }
        }
    }

    @Test
    void jumpHeightDoesNotDependOnTheFrameRate() {
        double at30 = jumpHeight(1.0 / 30);
        double at60 = jumpHeight(1.0 / 60);
        double at144 = jumpHeight(1.0 / 144);

        assertTrue(at60 > 20 && at60 < 40, "a jump rises about one and a half platform gaps, was " + at60);
        assertEquals(at60, at30, 4.0);
        assertEquals(at60, at144, 4.0);
    }

    private static double jumpHeight(double dt) {
        Level level = new Level("tester");
        Player player = level.getPlayer();
        run(level, 3, dt);
        assertTrue(player.isOnGround());
        double groundY = player.getPosition().getY();

        player.jump();
        double minY = groundY;
        for (double t = 0; t < 1.5; t += dt) {
            level.update(dt);
            minY = Math.min(minY, player.getPosition().getY());
        }
        return groundY - minY;
    }

    @Test
    void reachingThePrincessEndsTheGameExactlyOnce() {
        Level level = new Level("tester");
        List<Level.GameOverReason> reasons = new ArrayList<>();
        level.setOnGameOver(reasons::add);

        // the player overlaps the princess for many frames before the UI reacts
        level.getPlayer().setPosition(new Point2D(98, 8));
        run(level, 0.5, 1.0 / 60);

        assertEquals(List.of(Level.GameOverReason.PLAYER_WON), reasons);
    }

    @Test
    void losingTheLastLifeEndsTheGameExactlyOnce() {
        Level level = new Level("tester");
        List<Level.GameOverReason> reasons = new ArrayList<>();
        level.setOnGameOver(reasons::add);

        level.getPlayer().getHealth().setLifes(0);
        level.getPlayer().getHealth().setLifes(0);

        assertEquals(List.of(Level.GameOverReason.PLAYER_DEAD), reasons);
    }

    @Test
    void pausedLevelDoesNotAdvance() {
        Level level = new Level("tester");
        level.setPause(true);
        Point2D before = level.getPlayer().getPosition();

        run(level, 1, 1.0 / 60);

        assertEquals(before, level.getPlayer().getPosition());
        assertEquals(0.001, level.getDurationSeconds(), 1e-9, "paused time does not count");
    }

    @Test
    void durationOnlyCountsRunningTime() {
        Level level = new Level("tester");

        run(level, 2, 0.1);

        assertEquals(2.0, level.getDurationSeconds(), 0.15);
    }

    @Test
    void collectingATokenAddsHundredPointsAndRemovesIt() {
        Level level = new Level("tester");
        long tokensBefore = level.toGameState().entities.stream().filter(e -> e.type.equals("Token")).count();

        level.getPlayer().setPosition(new Point2D(100 - 8, 50 - 14)); // token at (100, 50)
        level.update(1.0 / 60);
        level.update(1.0 / 60);

        assertEquals(100, level.getPlayer().getScore());
        long tokensAfter = level.toGameState().entities.stream().filter(e -> e.type.equals("Token")).count();
        assertEquals(tokensBefore - 1, tokensAfter);
    }

    @Test
    void saveAndRestorePreservesTheGame() {
        Level level = new Level("tester");
        run(level, 2, 1.0 / 60);
        level.getPlayer().setScore(450);
        level.getPlayer().getHealth().setLifes(3);
        GameState saved = level.toGameState();

        Level restored = new Level("other");
        restored.fromGameState(saved);

        assertEquals("tester", restored.getPlayer().getPlayerName());
        assertEquals(450, restored.getPlayer().getScore());
        assertEquals(3, restored.getPlayer().getHealth().getLifes());
        assertEquals(2, restored.getDeathCount());
        assertEquals(level.getPlayer().getPosition(), restored.getPlayer().getPosition());
        assertEquals(saved.entities.size(), restored.toGameState().entities.size());
        assertEquals(saved.duration, restored.getDurationSeconds(), 1e-9);
    }

    @Test
    void restoredStateKeepsOnlyKnownEntityTypes() {
        GameState state = new Level("tester").toGameState();
        EntityState unknown = new EntityState();
        unknown.type = "Alien";
        unknown.state = "default";
        state.entities.add(unknown);

        Level restored = new Level("tester");
        restored.fromGameState(state);

        assertNotNull(restored.getPlayer());
        assertTrue(restored.toGameState().entities.stream().noneMatch(e -> e.type.equals("Alien")));
    }

    @Test
    void aRollingBarrelRestoredFromASaveKeepsRollingInsteadOfRespawning() {
        Level level = new Level("tester");
        Barrel barrel = new Barrel(level, 32, new Point2D(120, 100));
        barrel.setStateByName("ROLLING");
        barrel.setDirectionX(1);
        barrel.setDirectionY(1);

        barrel.update(1.5); // longer than the one second spawn delay

        assertNotEquals(new Point2D(64, 59), barrel.getPosition(), "must not be teleported to the spawn point");
        assertTrue(barrel.getPosition().getX() > 120, "keeps rolling to the right");
    }

    @Test
    void aBarrelJustBelowThePlayersFeetCountsAsJumpedOverButADistantOneDoesNot() {
        Level level = new Level("tester");
        Player player = level.getPlayer();
        player.setPosition(new Point2D(100, 100));
        double feet = player.getBounds().getMaxY();

        Barrel near = barrelWithTopAt(level, 100, feet + 10);
        Barrel far = barrelWithTopAt(level, 100, feet + 80);

        assertTrue(near.collides(player.getJumpOverBounds()));
        assertFalse(far.collides(player.getJumpOverBounds()),
                "a barrel on a platform far below must not give points");
    }

    private static Barrel barrelWithTopAt(Level level, double x, double top) {
        Barrel barrel = new Barrel(level, 32, new Point2D(x, 0));
        barrel.setStateByName("ROLLING");
        double insetTop = barrel.getBounds().getMinY(); // bounds top for y = 0
        barrel.setPosition(new Point2D(x, top - insetTop));
        return barrel;
    }
}
