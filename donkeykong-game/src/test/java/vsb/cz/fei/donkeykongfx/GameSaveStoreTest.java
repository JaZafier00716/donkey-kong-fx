package vsb.cz.fei.donkeykongfx;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vsb.cz.fei.donkeykongfx.gameobjects.entities.EntityState;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSaveStoreTest {
    @TempDir
    File dir;

    @Test
    void savedStateCanBeLoadedAgain() throws IOException {
        GameSaveStore store = new GameSaveStore(new File(dir, "state.bin"));
        GameState state = new GameState();
        state.playerName = "Honza";
        state.score = 300;
        state.lives = 4;
        state.duration = 12.5;
        EntityState entity = new EntityState();
        entity.type = "Barrel";
        entity.positionX = 10;
        entity.positionY = 20;
        entity.state = "ROLLING";
        state.entities.add(entity);

        store.save(state);
        GameState loaded = store.load();

        assertTrue(store.exists());
        assertEquals("Honza", loaded.playerName);
        assertEquals(300, loaded.score);
        assertEquals(4, loaded.lives);
        assertEquals(12.5, loaded.duration);
        assertEquals(1, loaded.entities.size());
        assertEquals("ROLLING", loaded.entities.get(0).state);
    }

    @Test
    void corruptFileIsReportedAsIoException() throws IOException {
        File file = new File(dir, "state.bin");
        Files.writeString(file.toPath(), "this is not a serialized game");

        assertThrows(IOException.class, () -> new GameSaveStore(file).load());
    }

    @Test
    void deleteRemovesTheFileAndIsHarmlessWhenMissing() throws IOException {
        File file = new File(dir, "state.bin");
        GameSaveStore store = new GameSaveStore(file);
        store.save(new GameState());

        store.delete();
        store.delete();

        assertFalse(store.exists());
    }
}
