package cz.vsb.fei.donkeykong.repository;

import cz.vsb.fei.donkeykong.entity.GameLevel;
import cz.vsb.fei.donkeykong.entity.GameResult;
import cz.vsb.fei.donkeykong.entity.Player;
import cz.vsb.fei.donkeykong.repository.PlayerRepository.PlayerStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class RepositoriesTest {
    @Autowired
    private PlayerRepository players;
    @Autowired
    private GameLevelRepository levels;
    @Autowired
    private GameResultRepository results;

    private Player anna;
    private Player bob;
    private GameLevel level1;
    private final LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void data() {
        anna = players.save(new Player("Anna"));
        bob = players.save(new Player("Bob"));
        level1 = levels.save(new GameLevel(1, "Level 1"));
        players.save(new Player("Carl")); // never played
        result(anna, 300, 0);
        result(anna, 900, 1);
        result(bob, 100, 2);
    }

    private GameResult result(Player player, int score, int minutes) {
        GameResult r = new GameResult();
        r.setPlayer(player);
        r.setGameLevel(level1);
        r.setScore(score);
        r.setPlayedAt(base.plusMinutes(minutes));
        r.setDuration(10.0 + score);
        r.setDeaths(0);
        return results.save(r);
    }

    @Test
    void resultsAreListedNewestFirst() {
        List<GameResult> all = results.findAllByOrderByPlayedAtDesc();

        assertEquals(List.of(100, 900, 300), all.stream().map(GameResult::getScore).toList());
    }

    @Test
    void last10ReturnsAtMostTenNewestResults() {
        for (int i = 0; i < 15; i++) {
            result(bob, i, 10 + i);
        }

        List<GameResult> last = results.findTop10ByOrderByPlayedAtDesc();

        assertEquals(10, last.size());
        assertEquals(14, last.get(0).getScore());
    }

    @Test
    void resultsOfOnePlayerAreFilteredByName() {
        assertEquals(List.of(900, 300),
                results.findByPlayer_NameOrderByPlayedAtDesc("Anna").stream().map(GameResult::getScore).toList());
        assertEquals(List.of(900, 300),
                results.findByPlayer_NameOrderByScoreDesc("Anna").stream().map(GameResult::getScore).toList());
        assertTrue(results.findByPlayer_NameOrderByPlayedAtDesc("Nobody").isEmpty());
    }

    @Test
    void playerNamesAreUnique() {
        assertThrows(DataIntegrityViolationException.class, () -> players.saveAndFlush(new Player("Anna")));
    }

    @Test
    void levelNumbersAreUnique() {
        assertThrows(DataIntegrityViolationException.class, () -> levels.saveAndFlush(new GameLevel(1, "Again")));
    }

    @Test
    void lookupsByNameAndLevelNumber() {
        assertEquals(anna.getId(), players.findByName("Anna").orElseThrow().getId());
        assertTrue(players.findByName("anna").isEmpty(), "names are case sensitive");
        assertEquals(level1.getId(), levels.findByLevelNumber(1).orElseThrow().getId());
        assertEquals(List.of("Anna", "Bob", "Carl"), players.findAllByOrderByNameAsc().stream().map(Player::getName).toList());
    }

    @Test
    void playerStatsAreComputedInOneQueryIncludingPlayersWithoutGames() {
        List<PlayerStats> stats = players.findAllStats();

        assertEquals(List.of("Anna", "Bob", "Carl"), stats.stream().map(PlayerStats::getName).toList());
        PlayerStats a = stats.get(0);
        assertEquals(2, a.getGamesPlayed());
        assertEquals(900, a.getHighestScore());
        assertEquals(600.0, a.getAverageScore(), 1e-9);
        PlayerStats carl = stats.get(2);
        assertEquals(0, carl.getGamesPlayed());
        assertNull(carl.getHighestScore());
        assertEquals(0.0, carl.getAverageScore(), 1e-9);
    }

    @Test
    void playerStatsForOneName() {
        assertEquals(1, players.findStatsByName("Bob").orElseThrow().getGamesPlayed());
        assertTrue(players.findStatsByName("Nobody").isEmpty());
    }

    @Test
    void deletingAResultKeepsPlayerAndLevel() {
        results.deleteAll();

        assertEquals(0, results.count());
        assertEquals(3, players.count());
        assertEquals(1, levels.count());
    }
}
