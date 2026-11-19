package cz.vsb.fei.DonkeyKongFX.service;

import cz.vsb.fei.DonkeyKongFX.dto.GameResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The only place that knows how to talk to {@code donkeykong-db}. Controllers use it instead of a
 * raw {@link RestTemplate} so URL building and error translation live in one spot.
 * <p>
 * Failures become {@link DbServiceException}: an unreachable service is {@code 503}, an error
 * answer of the database service is passed on ({@code 4xx} as is, {@code 5xx} as {@code 502}).
 * A {@code 404} for a single result is reported as an empty {@link Optional} (or {@code false}) instead.
 */
@Component
public class DbServiceClient {
    private static final String RESULTS = "/api/v1/game-results";

    private final RestTemplate restTemplate;
    private final String resultsUrl;

    public DbServiceClient(RestTemplate restTemplate, @Value("${donkeykong.db.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.resultsUrl = trimmed + RESULTS;
    }

    public List<GameResult> findAll() {
        return list(resultsUrl);
    }

    public List<GameResult> findLast10() {
        return list(resultsUrl + "/last/10");
    }

    public List<GameResult> findByPlayer(String playerName) {
        return list(resultsUrl + "/player/{name}", playerName);
    }

    public Optional<GameResult> findById(long id) {
        return optional(() -> restTemplate.getForObject(resultsUrl + "/{id}", GameResult.class, id));
    }

    public GameResult create(GameResult result) {
        return call(() -> restTemplate.postForObject(resultsUrl, result, GameResult.class));
    }

    /** @return the updated result, or empty if there is no result with this id */
    public Optional<GameResult> update(long id, GameResult changes) {
        return optional(() -> restTemplate.exchange(resultsUrl + "/{id}", HttpMethod.PUT,
                new HttpEntity<>(changes), GameResult.class, id).getBody());
    }

    /** @return false if there was no result with this id */
    public boolean delete(long id) {
        try {
            call(() -> {
                restTemplate.delete(resultsUrl + "/{id}", id);
                return null;
            });
            return true;
        } catch (DbServiceException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw e;
        }
    }

    public void deleteAll() {
        call(() -> {
            restTemplate.delete(resultsUrl);
            return null;
        });
    }

    private List<GameResult> list(String url, Object... uriVariables) {
        return call(() -> {
            GameResult[] results = restTemplate.getForObject(url, GameResult[].class, uriVariables);
            return results == null ? List.<GameResult>of() : Arrays.asList(results);
        });
    }

    /** Like {@link #call} but an answer of 404 (or an empty body) means "no such result". */
    private Optional<GameResult> optional(Supplier<GameResult> request) {
        try {
            return Optional.ofNullable(call(request));
        } catch (DbServiceException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpStatusCodeException e) {
            throw new DbServiceException(passThrough(e.getStatusCode()),
                    "Database service answered " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new DbServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Database service is not reachable", e);
        } catch (RestClientException e) {
            throw new DbServiceException(HttpStatus.BAD_GATEWAY, "Unexpected answer of the database service", e);
        }
    }

    private static HttpStatus passThrough(HttpStatusCode status) {
        return status.is4xxClientError() ? HttpStatus.valueOf(status.value()) : HttpStatus.BAD_GATEWAY;
    }
}
