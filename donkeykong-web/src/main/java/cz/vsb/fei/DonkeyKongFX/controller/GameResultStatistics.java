package cz.vsb.fei.DonkeyKongFX.controller;

import cz.vsb.fei.DonkeyKongFX.dto.GameResult;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Summary numbers shown on the dashboard, derived from the list of results.
 */
public record GameResultStatistics(
        int totalGames,
        GameResult fastestGame,
        GameResult highestScoreGame,
        String mostGamesPlayer,
        Long mostGamesCount
) {
    public static GameResultStatistics of(List<GameResult> results) {
        Optional<GameResult> fastest = results.stream()
                .filter(result -> result.getDuration() != null)
                .min(Comparator.comparingDouble(GameResult::getDuration));
        Optional<GameResult> highest = results.stream()
                .filter(result -> result.getScore() != null)
                .max(Comparator.comparingInt(GameResult::getScore));
        Map<String, Long> gamesByPlayer = results.stream()
                .filter(result -> result.getPlayerName() != null && !result.getPlayerName().isBlank())
                .collect(Collectors.groupingBy(GameResult::getPlayerName, Collectors.counting()));
        // most games wins; on a tie the alphabetically first name, so the page does not change on reload
        Optional<Map.Entry<String, Long>> mostGames = gamesByPlayer.entrySet().stream()
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())));

        return new GameResultStatistics(
                results.size(),
                fastest.orElse(null),
                highest.orElse(null),
                mostGames.map(Map.Entry::getKey).orElse(null),
                mostGames.map(Map.Entry::getValue).orElse(null));
    }

    /** Duration of the fastest game formatted for the page, for example {@code 12.345 s}. */
    public String fastestTime() {
        return fastestGame == null ? null : String.format(Locale.US, "%.3f s", fastestGame.getDuration());
    }
}
