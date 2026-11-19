package vsb.cz.fei.donkeykongfx;

import vsb.cz.fei.donkeykongfx.gameobjects.entities.EntityState;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    public String playerName;
    public int score;
    public int lives;
    public double duration;

    public List<EntityState> entities = new ArrayList<>();
}
