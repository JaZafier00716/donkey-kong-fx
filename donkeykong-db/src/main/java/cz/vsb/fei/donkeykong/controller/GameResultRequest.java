package cz.vsb.fei.donkeykong.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Body of {@code POST}/{@code PUT /api/v1/game-results}. Unknown fields such as {@code id} (which
 * clients may echo back from a previous response) are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameResultRequest(
        String playerName,
        Integer score,
        LocalDateTime playedAt,
        Integer level,
        Double duration,
        Integer deaths
) {
    /**
     * @return the first problem with the values that are present, or {@code null} if they are all acceptable
     */
    public String problem() {
        if (playerName != null && playerName.isBlank()) {
            return "playerName must not be blank";
        }
        if (playerName != null && playerName.length() > GameResultController.MAX_PLAYER_NAME_LENGTH) {
            return "playerName must have at most " + GameResultController.MAX_PLAYER_NAME_LENGTH + " characters";
        }
        if (score != null && score < 0) {
            return "score must not be negative";
        }
        if (level != null && level < 1) {
            return "level must be at least 1";
        }
        if (duration != null && (duration < 0 || duration.isNaN() || duration.isInfinite())) {
            return "duration must be a non-negative number of seconds";
        }
        if (deaths != null && deaths < 0) {
            return "deaths must not be negative";
        }
        return null;
    }
}
