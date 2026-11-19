package cz.vsb.fei.DonkeyKongFX.controller;

import cz.vsb.fei.DonkeyKongFX.dto.GameResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameResultStatisticsTest {

    private static GameResult result(String name, Integer score, Double duration) {
        GameResult r = new GameResult();
        r.setPlayerName(name);
        r.setScore(score);
        r.setDuration(duration);
        return r;
    }

    @Test
    void emptyListHasNoHighlights() {
        GameResultStatistics stats = GameResultStatistics.of(List.of());

        assertEquals(0, stats.totalGames());
        assertNull(stats.fastestGame());
        assertNull(stats.fastestTime());
        assertNull(stats.highestScoreGame());
        assertNull(stats.mostGamesPlayer());
    }

    @Test
    void picksFastestHighestAndBusiestPlayer() {
        GameResultStatistics stats = GameResultStatistics.of(List.of(
                result("Anna", 300, 30.0), result("Bob", 900, 61.5), result("Anna", 100, 12.3456)));

        assertEquals(3, stats.totalGames());
        assertEquals(12.3456, stats.fastestGame().getDuration());
        assertEquals("12.346 s", stats.fastestTime());
        assertEquals("Bob", stats.highestScoreGame().getPlayerName());
        assertEquals("Anna", stats.mostGamesPlayer());
        assertEquals(2L, stats.mostGamesCount());
    }

    @Test
    void incompleteResultsAreSkippedInsteadOfFailing() {
        GameResultStatistics stats = GameResultStatistics.of(List.of(
                result(null, null, null), result("  ", 5, null), result("Bob", 7, 9.0)));

        assertEquals(3, stats.totalGames());
        assertEquals(7, stats.highestScoreGame().getScore());
        assertEquals("Bob", stats.mostGamesPlayer());
    }

    @Test
    void tieForMostGamesGoesToTheAlphabeticallyFirstPlayer() {
        for (int i = 0; i < 5; i++) {
            List<GameResult> results = new ArrayList<>(List.of(result("Zoe", 1, 1.0), result("Adam", 1, 1.0)));
            if (i % 2 == 0) {
                results = results.reversed();
            }
            assertEquals("Adam", GameResultStatistics.of(results).mostGamesPlayer());
        }
    }

    @Test
    void sortingByScoreWorksInBothDirectionsAndKeepsMissingValuesLast() {
        List<GameResult> results = new ArrayList<>(List.of(result("a", 5, null), result("b", null, null), result("c", 9, null)));

        results.sort(GameResultSorting.comparator("score", "asc"));
        assertEquals(List.of("a", "c", "b"), results.stream().map(GameResult::getPlayerName).toList());

        results.sort(GameResultSorting.comparator("score", "desc"));
        assertEquals(List.of("c", "a", "b"), results.stream().map(GameResult::getPlayerName).toList());
    }

    @Test
    void sortingByNameIgnoresCase() {
        List<GameResult> results = new ArrayList<>(List.of(result("bob", 1, null), result("Anna", 1, null), result("Cyril", 1, null)));

        results.sort(GameResultSorting.comparator("playerName", "asc"));

        assertEquals(List.of("Anna", "bob", "Cyril"), results.stream().map(GameResult::getPlayerName).toList());
    }

    @Test
    void sortingByDateAndFallbackForUnknownColumns() {
        GameResult early = result("early", 1, null);
        early.setPlayedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        GameResult late = result("late", 1, null);
        late.setPlayedAt(LocalDateTime.of(2026, 6, 1, 0, 0));
        List<GameResult> results = new ArrayList<>(List.of(early, late));

        results.sort(GameResultSorting.comparator("nonsense", "desc"));

        assertEquals("late", results.get(0).getPlayerName());
        assertEquals("playedAt", GameResultSorting.normalizeSort("nonsense"));
        assertEquals("playedAt", GameResultSorting.normalizeSort(null));
        assertEquals("desc", GameResultSorting.normalizeDirection("sideways"));
        assertEquals("asc", GameResultSorting.normalizeDirection("ASC"));
    }
}
