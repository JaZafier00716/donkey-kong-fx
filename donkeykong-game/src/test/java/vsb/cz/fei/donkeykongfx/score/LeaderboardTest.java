package vsb.cz.fei.donkeykongfx.score;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardTest {

    @Test
    void keepsTheBestScoresHighestFirst() {
        List<Score> scores = List.of(new Score("a", 10), new Score("b", 500), new Score("c", 200),
                new Score("d", 300), new Score("e", 50), new Score("f", 400), new Score("g", 1));

        List<Score> top = Leaderboard.top(scores, 5);

        assertEquals(List.of("b", "f", "d", "c", "e"), top.stream().map(Score::getNickName).toList());
    }

    @Test
    void fewerScoresThanRowsShowsOnlyWhatExists() {
        List<Score> top = Leaderboard.top(List.of(new Score("a", 10), new Score("b", 20)), 5);

        assertEquals(2, top.size());
        assertEquals("b", top.get(0).getNickName());
    }

    @Test
    void noScoresShowsSortedPlaceholderRows() {
        List<Score> top = Leaderboard.top(List.of(), 5);

        assertEquals(5, top.size());
        for (int i = 1; i < top.size(); i++) {
            assertTrue(top.get(i - 1).getScore() >= top.get(i).getScore());
        }
    }
}
