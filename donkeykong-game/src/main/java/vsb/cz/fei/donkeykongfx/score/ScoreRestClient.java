package vsb.cz.fei.donkeykongfx.score;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST client for the {@code donkeykong-web} service, which stores and lists game results.
 * <p>
 * The base URL comes from the {@code DONKEYKONG_WEB_URL} environment variable
 * (default {@value #DEFAULT_BASE_URL}).
 */
@Log4j2
public class ScoreRestClient {
    public static final String DEFAULT_BASE_URL = "http://localhost:8081";
    private static final String RESULTS_PATH = "/api/v1/game-results";

    private static final ExecutorService BACKGROUND = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "score-client");
        thread.setDaemon(true); // never keep the JVM alive after the window is closed
        return thread;
    });
    private static final ScoreRestClient DEFAULT = new ScoreRestClient(
            System.getenv().getOrDefault("DONKEYKONG_WEB_URL", DEFAULT_BASE_URL));

    private final URI resultsUri;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ScoreRestClient(String baseUrl) {
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.resultsUri = URI.create(trimmed + RESULTS_PATH);
    }

    public static ScoreRestClient getDefault() {
        return DEFAULT;
    }

    /** Loads all scores from the remote service. Blocks, so do not call it on the UI thread. */
    public List<Score> load() throws ScoreException {
        try {
            HttpRequest request = HttpRequest.newBuilder(resultsUri)
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ScoreException("Failed to load scores. Status: " + response.statusCode());
            }

            List<ScoreDTO> dtos = objectMapper.readValue(response.body(), new TypeReference<>() {});
            List<Score> scores = new ArrayList<>();
            for (ScoreDTO dto : dtos) {
                if (dto.getPlayerName() != null && dto.getScore() != null) {
                    scores.add(new Score(dto.getPlayerName(), dto.getScore()));
                }
            }
            return scores;
        } catch (IOException e) {
            throw new ScoreException("Load failed: " + describe(e), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScoreException("Load interrupted", e);
        }
    }

    /** Saves one finished game with its metadata. Blocks, so do not call it on the UI thread. */
    public void save(Score score, Integer level, Double duration, Integer deaths) throws ScoreException {
        if (score.getNickName() == null || score.getNickName().isBlank()) {
            return;
        }
        try {
            ScoreDTO dto = new ScoreDTO();
            dto.setPlayerName(score.getNickName());
            dto.setScore(score.getScore());
            dto.setLevel(level);
            dto.setDuration(duration);
            dto.setDeaths(deaths);

            HttpRequest request = HttpRequest.newBuilder(resultsUri)
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(dto)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new ScoreException("Failed to save score. Status: " + response.statusCode());
            }
            log.debug("Score saved: {} - {}", score.getNickName(), score.getScore());
        } catch (IOException e) {
            throw new ScoreException("Save failed: " + describe(e), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScoreException("Save interrupted", e);
        }
    }

    public CompletableFuture<List<Score>> loadAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return load();
            } catch (ScoreException e) {
                throw new CompletionException(e);
            }
        }, BACKGROUND);
    }

    public CompletableFuture<Void> saveAsync(Score score, Integer level, Double duration, Integer deaths) {
        return CompletableFuture.runAsync(() -> {
            try {
                save(score, level, duration, deaths);
            } catch (ScoreException e) {
                throw new CompletionException(e);
            }
        }, BACKGROUND);
    }

    private static String describe(Throwable e) {
        String message = e.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return e.getClass().getSimpleName();
    }

    /** JSON shape of a game result as exchanged with the web service. */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoreDTO {
        private Long id;
        private String playerName;
        private Integer score;
        private Integer level;
        private Double duration;
        private Integer deaths;
    }
}
