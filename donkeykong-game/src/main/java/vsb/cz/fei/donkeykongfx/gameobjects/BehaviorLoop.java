package vsb.cz.fei.donkeykongfx.gameobjects;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;

/**
 * Background thread that calls an entity's {@code update} about 60 times a second.
 * <p>
 * Every update runs while holding {@code lock} (the level), so it can never interleave with the
 * level's own update and render passes on the JavaFX thread.
 */
public final class BehaviorLoop {
    private final String name;
    private final Object lock;
    private final long sleepMillis;
    private final double maxDeltaSeconds;
    private final DoubleConsumer update;
    private final BooleanSupplier finished;

    private volatile boolean running = false;
    private volatile boolean paused = false;
    private Thread thread;

    public BehaviorLoop(String name, Object lock, long sleepMillis, double maxDeltaSeconds,
                        DoubleConsumer update, BooleanSupplier finished) {
        this.name = name;
        this.lock = lock;
        this.sleepMillis = sleepMillis;
        this.maxDeltaSeconds = maxDeltaSeconds;
        this.update = update;
        this.finished = finished;
    }

    public synchronized void start() {
        if (thread != null && thread.isAlive()) {
            return;
        }
        running = true;
        thread = new Thread(this::run, name);
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    private void run() {
        long lastTick = System.nanoTime();
        while (running && !finished.getAsBoolean()) {
            long now = System.nanoTime();
            double deltaTime = (now - lastTick) / 1_000_000_000D;
            lastTick = now;

            if (!paused) {
                synchronized (lock) {
                    update.accept(Math.min(deltaTime, maxDeltaSeconds));
                }
            }

            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }
}
