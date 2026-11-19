package vsb.cz.fei.donkeykongfx.score;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Picks the rows shown in the menu's score table.
 */
public final class Leaderboard {
    private Leaderboard() {
    }

    /**
     * @return the {@code size} best scores, highest first; when there are none (service offline,
     * empty database) {@code size} random placeholder rows so the table is never empty
     */
    public static List<Score> top(List<Score> scores, int size) {
        Comparator<Score> byScoreDescending = Comparator.comparingInt(Score::getScore).reversed();
        List<Score> best = scores.stream()
                .sorted(byScoreDescending)
                .limit(size)
                .collect(Collectors.toCollection(ArrayList::new));
        if (best.isEmpty()) {
            for (int i = 0; i < size; i++) {
                best.add(Score.generate());
            }
            best.sort(byScoreDescending);
        }
        return best;
    }
}
