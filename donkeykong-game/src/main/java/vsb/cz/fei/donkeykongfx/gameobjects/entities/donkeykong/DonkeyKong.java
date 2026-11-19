package vsb.cz.fei.donkeykongfx.gameobjects.entities.donkeykong;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import vsb.cz.fei.donkeykongfx.controllers.ResizableDimension;
import vsb.cz.fei.donkeykongfx.gameobjects.*;

public class DonkeyKong extends MovableGameObject implements AutonomousEntity {
    /** Donkey Kong never moves, so his movement profile is all zeros. */
    private static final MovableType STATIONARY = new MovableType(0, 0, 0, 0, 0, false, false);

    private KongState kongState;
    private final AnimationData idle;
    private final AnimationData throwing;
    private final AnimationData drop;
    private final double customScale;
    private boolean canAddBarrel = true;
    private boolean canAddFlamyBoi = true;
    private final BehaviorLoop behaviorLoop;

    // Main menu constructor
    public DonkeyKong(ResizableDimension rd, int height, double scale, Point2D position) {
        super(rd, height, position, STATIONARY);
        kongState = KongState.IDLE;
        customScale = scale;
        setFrameDuration(0.2);
        this.behaviorLoop = new BehaviorLoop("DonkeyKong-" + Integer.toHexString(System.identityHashCode(this)), rd, 50, 0.1,
                this::update, this::isToBeRemoved);
        this.frameIndex = 0;
        this.idle = new AnimationData("/images/enemies/donkeykong/idle.png", 4, 1);
        this.throwing = new AnimationData("/images/enemies/donkeykong/throw.png", 3, 1);
        this.drop = new AnimationData("/images/enemies/donkeykong/drop.png", 2, 1);
    }

    // Game level constructor
    public DonkeyKong(ResizableDimension rd, int height, Point2D position) {
        super(rd, height, position, STATIONARY);
        kongState = KongState.DROPPING;
        customScale = -1;
        setFrameDuration(1.4); // effectively static
        this.behaviorLoop = new BehaviorLoop("DonkeyKong-" + Integer.toHexString(System.identityHashCode(this)), rd, 50, 0.1,
                this::update, this::isToBeRemoved);
        this.frameIndex = 0;
        this.idle = new AnimationData("/images/enemies/donkeykong/idle.png", 4, 1);
        this.throwing = new AnimationData("/images/enemies/donkeykong/throw.png", 3, 1);
        this.drop = new AnimationData("/images/enemies/donkeykong/drop.png", 2, 1);
    }

    /** Menu preview uses its own scale; inside a level the level scale applies. */
    private double spriteScale() {
        return customScale == -1 ? rd.getScale() : customScale;
    }

    @Override
    public synchronized Rectangle2D getBounds() {
        AnimationData currentAnim = switch (kongState) {
            case IDLE -> idle;
            case THROWING -> throwing;
            case DROPPING -> drop;
        };
        double scale = spriteScale();
        return new Rectangle2D(
                getPosition().getX() * scale,
                getPosition().getY() * scale,
                currentAnim.getSize().getWidth() * scale,
                currentAnim.getSize().getHeight() * scale
        );
    }

    @Override
    public synchronized void hitBy(Collisionable another) {

    }

    @Override
    protected synchronized void renderInternal(GraphicsContext gc) {
        AnimationData currentAnim = switch (kongState) {
            case IDLE -> idle;
            case THROWING -> throwing;
            case DROPPING -> drop;
        };

        drawSpriteFrame(
                gc,
                currentAnim,
                frameIndex,
                0,
                getPosition().getX() * spriteScale(),
                getPosition().getY() * spriteScale(),
                spriteScale(),
                false
        );
    }

    public synchronized void updateState(double deltaTime) {
        switch (kongState) {
            case IDLE -> frameIndex = ((frameIndex + 1)) % idle.getColCount();
            case THROWING -> {
                frameIndex = (frameIndex +1 ) % throwing.getColCount();
                if(frameIndex != 2) {
                    canAddBarrel = true;
                }
            }
            case DROPPING -> {
                frameIndex = (frameIndex +1) % drop.getColCount();
                if(frameIndex == 0) {
                    kongState = KongState.THROWING;
                    canAddFlamyBoi = true;
                }
            }
        }
    }

    public synchronized boolean getSpawnFlamyBoi() {
        return kongState == KongState.DROPPING && frameIndex == 1 && canAddFlamyBoi;
    }

    public synchronized void setSpawnFlamyBoi(boolean canAddFlamyBoi) {
        this.canAddFlamyBoi = canAddFlamyBoi;
    }

    public synchronized boolean getSpawnBarrel() {
        return kongState == KongState.THROWING && frameIndex == 1 && canAddBarrel;
    }

    public synchronized void setSpawnBarrel(boolean canAddBarrel) {
        this.canAddBarrel = canAddBarrel;
    }

    @Override
    public synchronized void update(double deltaTime) {
        updateTimer(deltaTime);
    }

    public synchronized String getStateName() {
        return kongState.name();
    }

    public synchronized void setStateByName(String state) {
        this.kongState = KongState.valueOf(state);
    }

    @Override
    public void startBehaviorThread() {
        behaviorLoop.start();
    }

    @Override
    public void stopBehaviorThread() {
        behaviorLoop.stop();
    }

    @Override
    public void setBehaviorPaused(boolean paused) {
        behaviorLoop.setPaused(paused);
    }
}
