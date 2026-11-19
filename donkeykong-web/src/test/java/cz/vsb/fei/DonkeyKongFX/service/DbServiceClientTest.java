package cz.vsb.fei.DonkeyKongFX.service;

import cz.vsb.fei.DonkeyKongFX.config.RestTemplateConfig;
import cz.vsb.fei.DonkeyKongFX.dto.GameResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DbServiceClientTest {
    private static final String BASE = "http://db:8080";
    private static final String RESULTS = BASE + "/api/v1/game-results";

    private MockRestServiceServer server;
    private DbServiceClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplateConfig().restTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new DbServiceClient(restTemplate, BASE + "/"); // trailing slash must not matter
    }

    @Test
    void findAllReadsTheList() {
        server.expect(requestTo(RESULTS)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"id\":1,\"playerName\":\"Anna\",\"score\":900,\"unknown\":1}]", MediaType.APPLICATION_JSON));

        List<GameResult> results = client.findAll();

        assertEquals(1, results.size());
        assertEquals("Anna", results.get(0).getPlayerName());
        assertEquals(900, results.get(0).getScore());
        server.verify();
    }

    @Test
    void emptyBodyMeansEmptyList() {
        server.expect(requestTo(RESULTS)).andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertTrue(client.findAll().isEmpty());
    }

    @Test
    void playerNamesAreEncodedAsOnePathSegment() {
        server.expect(requestTo(RESULTS + "/player/Jan%20Nov%C3%A1k")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(RESULTS + "/player/a%2Fb%3Fc%25")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.findByPlayer("Jan Novák");
        client.findByPlayer("a/b?c%");

        server.verify();
    }

    @Test
    void findLast10UsesItsOwnEndpoint() {
        server.expect(requestTo(RESULTS + "/last/10")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.findLast10();

        server.verify();
    }

    @Test
    void findByIdReturnsEmptyForUnknownIds() {
        server.expect(requestTo(RESULTS + "/7")).andRespond(withSuccess("{\"id\":7,\"score\":5}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(RESULTS + "/8")).andRespond(withResourceNotFound());

        assertEquals(5, client.findById(7).orElseThrow().getScore());
        assertTrue(client.findById(8).isEmpty());
    }

    @Test
    void createPostsTheResultAndReturnsTheStoredOne() {
        server.expect(requestTo(RESULTS)).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"playerName\":\"Anna\",\"score\":900}"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":5,\"playerName\":\"Anna\",\"score\":900}"));
        GameResult toSave = new GameResult();
        toSave.setPlayerName("Anna");
        toSave.setScore(900);

        GameResult saved = client.create(toSave);

        assertEquals(5, saved.getId());
    }

    @Test
    void updateSendsPutAndReportsUnknownIds() {
        server.expect(requestTo(RESULTS + "/3")).andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{\"id\":3,\"score\":10}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(RESULTS + "/4")).andExpect(method(HttpMethod.PUT)).andRespond(withResourceNotFound());

        assertEquals(10, client.update(3, new GameResult()).orElseThrow().getScore());
        assertTrue(client.update(4, new GameResult()).isEmpty());
    }

    @Test
    void deleteReportsWhetherSomethingWasDeleted() {
        server.expect(requestTo(RESULTS + "/3")).andExpect(method(HttpMethod.DELETE)).andRespond(withNoContent());
        server.expect(requestTo(RESULTS + "/4")).andExpect(method(HttpMethod.DELETE)).andRespond(withResourceNotFound());

        assertTrue(client.delete(3));
        assertFalse(client.delete(4));
    }

    @Test
    void deleteAllDeletesTheCollection() {
        server.expect(requestTo(RESULTS)).andExpect(method(HttpMethod.DELETE)).andRespond(withNoContent());

        client.deleteAll();

        server.verify();
    }

    @Test
    void unreachableServiceBecomes503() {
        server.expect(requestTo(RESULTS)).andRespond(withException(new IOException("connection refused")));

        DbServiceException e = assertThrows(DbServiceException.class, () -> client.findAll());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
    }

    @Test
    void serverErrorsBecome502AndClientErrorsArePassedOn() {
        server.expect(requestTo(RESULTS)).andRespond(withServerError());
        server.expect(requestTo(RESULTS)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(DbServiceException.class, () -> client.findAll()).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(DbServiceException.class, () -> client.create(new GameResult())).getStatus());
    }

    @Test
    void notFoundOfAListIsAnErrorNotAnEmptyList() {
        server.expect(requestTo(RESULTS)).andRespond(withResourceNotFound());

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(DbServiceException.class, () -> client.findAll()).getStatus());
    }
}
