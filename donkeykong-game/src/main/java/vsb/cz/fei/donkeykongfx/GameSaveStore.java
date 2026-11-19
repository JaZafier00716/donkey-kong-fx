package vsb.cz.fei.donkeykongfx;

import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Reads and writes the single save game file ({@code state.bin} in the working directory).
 */
@Log4j2
public class GameSaveStore {
    private static final GameSaveStore DEFAULT = new GameSaveStore(new File("./state.bin"));

    private final File file;

    public GameSaveStore(File file) {
        this.file = file;
    }

    public static GameSaveStore getDefault() {
        return DEFAULT;
    }

    public boolean exists() {
        return file.exists();
    }

    public void save(GameState state) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(state);
        }
    }

    /**
     * @throws IOException when the file is missing, unreadable or was written by an incompatible version
     */
    public GameState load() throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (GameState) in.readObject();
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("Save file " + file + " is not a valid game state", e);
        }
    }

    public void delete() {
        if (file.exists() && !file.delete()) {
            log.warn("Failed to delete save file at {}", file.getAbsolutePath());
        }
    }
}
