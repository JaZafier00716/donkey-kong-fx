package cz.vsb.fei.DonkeyKongFX.controller;

import cz.vsb.fei.DonkeyKongFX.dto.GameResult;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Sorting of the results table; unknown columns fall back to the play date.
 */
public final class GameResultSorting {
    private GameResultSorting() {
    }

    public static String normalizeSort(String sort) {
        return switch (sort == null ? "" : sort) {
            case "playerName", "score", "level", "duration", "deaths", "playedAt" -> sort;
            default -> "playedAt";
        };
    }

    public static String normalizeDirection(String direction) {
        return "asc".equalsIgnoreCase(direction) ? "asc" : "desc";
    }

    /** Missing values always come last, whichever the direction. */
    public static Comparator<GameResult> comparator(String sort, String direction) {
        boolean ascending = "asc".equals(normalizeDirection(direction));
        return switch (normalizeSort(sort)) {
            case "playerName" -> nullsLast(GameResult::getPlayerName, String.CASE_INSENSITIVE_ORDER, ascending);
            case "score" -> nullsLast(GameResult::getScore, Comparator.<Integer>naturalOrder(), ascending);
            case "level" -> nullsLast(GameResult::getLevel, Comparator.<Integer>naturalOrder(), ascending);
            case "duration" -> nullsLast(GameResult::getDuration, Comparator.<Double>naturalOrder(), ascending);
            case "deaths" -> nullsLast(GameResult::getDeaths, Comparator.<Integer>naturalOrder(), ascending);
            default -> nullsLast(GameResult::getPlayedAt, Comparator.<LocalDateTime>naturalOrder(), ascending);
        };
    }

    private static <T> Comparator<GameResult> nullsLast(Function<GameResult, T> extractor, Comparator<T> valueOrder, boolean ascending) {
        Comparator<T> directed = ascending ? valueOrder : valueOrder.reversed();
        return Comparator.comparing(extractor, Comparator.nullsLast(directed));
    }
}
