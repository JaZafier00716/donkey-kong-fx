package vsb.cz.fei.donkeykongfx.score;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreRestClientTest {
    private HttpServer server;
    private String baseUrl;
    private int status;
    private String responseBody;
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> requestPath = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestPath.set(exchange.getRequestURI().getPath());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void loadParsesTheScoresAndIgnoresUnknownFieldsAndIncompleteRows() throws ScoreException {
        status = 200;
        responseBody = """
                [{"id":1,"playerName":"Anna","score":900,"level":1,"duration":12.5,"deaths":0,"playedAt":"2026-01-01T10:00:00","extra":true},
                 {"id":2,"playerName":"Bob","score":300},
                 {"id":3,"playerName":null,"score":50},
                 {"id":4,"playerName":"NoScore"}]
                """;

        List<Score> scores = new ScoreRestClient(baseUrl).load();

        assertEquals(2, scores.size());
        assertEquals("Anna", scores.get(0).getNickName());
        assertEquals(900, scores.get(0).getScore());
        assertEquals("GET", requestMethod.get());
        assertEquals("/api/v1/game-results", requestPath.get());
    }

    @Test
    void trailingSlashInTheBaseUrlIsHarmless() throws ScoreException {
        status = 200;
        responseBody = "[]";

        new ScoreRestClient(baseUrl + "/").load();

        assertEquals("/api/v1/game-results", requestPath.get());
    }

    @Test
    void loadFailsOnAServerError() {
        status = 503;
        responseBody = "";

        ScoreException e = assertThrows(ScoreException.class, () -> new ScoreRestClient(baseUrl).load());

        assertTrue(e.getMessage().contains("503"));
    }

    @Test
    void loadFailsOnGarbage() {
        status = 200;
        responseBody = "not json";

        assertThrows(ScoreException.class, () -> new ScoreRestClient(baseUrl).load());
    }

    @Test
    void loadFailsWhenNobodyIsListening() {
        server.stop(0);

        assertThrows(ScoreException.class, () -> new ScoreRestClient(baseUrl).load());
    }

    @Test
    void savePostsAllFieldsAsJson() throws Exception {
        status = 201;
        responseBody = "{\"id\":7}";

        new ScoreRestClient(baseUrl).save(new Score("Anna", 900), 1, 42.5, 2);

        assertEquals("POST", requestMethod.get());
        JsonNode json = new ObjectMapper().readTree(requestBody.get());
        assertEquals("Anna", json.get("playerName").asText());
        assertEquals(900, json.get("score").asInt());
        assertEquals(1, json.get("level").asInt());
        assertEquals(42.5, json.get("duration").asDouble());
        assertEquals(2, json.get("deaths").asInt());
    }

    @Test
    void saveReportsAnUnexpectedStatus() {
        status = 400;
        responseBody = "";

        assertThrows(ScoreException.class, () -> new ScoreRestClient(baseUrl).save(new Score("Anna", 1), 1, 1.0, 0));
    }

    @Test
    void anonymousScoresAreNotSent() throws ScoreException {
        status = 201;
        responseBody = "{}";

        new ScoreRestClient(baseUrl).save(new Score("  ", 100), 1, 1.0, 0);

        assertEquals(null, requestMethod.get(), "no request expected");
    }

    @Test
    void asyncCallsCompleteExceptionallyWhenTheServiceIsDown() {
        server.stop(0);
        ScoreRestClient client = new ScoreRestClient(baseUrl);

        var failure = assertThrows(java.util.concurrent.ExecutionException.class, () -> client.loadAsync().get());

        assertTrue(failure.getCause() instanceof ScoreException);
    }

    @Test
    void asyncLoadDeliversTheScores() throws Exception {
        status = 200;
        responseBody = "[{\"playerName\":\"Anna\",\"score\":5}]";

        assertEquals(1, new ScoreRestClient(baseUrl).loadAsync().get().size());
    }
}
