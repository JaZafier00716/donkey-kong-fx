package cz.vsb.fei.DonkeyKongFX.controller;

import cz.vsb.fei.DonkeyKongFX.dto.GameResult;
import cz.vsb.fei.DonkeyKongFX.service.DbServiceClient;
import cz.vsb.fei.DonkeyKongFX.service.DbServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The REST proxy the game talks to: checks status codes and that calls reach the database client. */
@WebMvcTest(GameResultController.class)
class GameResultControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private DbServiceClient db;

    private static GameResult result(long id, String name, int score) {
        GameResult r = new GameResult();
        r.setId(id);
        r.setPlayerName(name);
        r.setScore(score);
        return r;
    }

    @Test
    void getAllReturnsTheList() throws Exception {
        when(db.findAll()).thenReturn(List.of(result(1, "Anna", 900), result(2, "Bob", 5)));

        mvc.perform(get("/api/v1/game-results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].playerName").value("Anna"));
    }

    @Test
    void getOneReturns404ForUnknownIds() throws Exception {
        when(db.findById(1)).thenReturn(Optional.of(result(1, "Anna", 900)));
        when(db.findById(2)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/game-results/1")).andExpect(status().isOk()).andExpect(jsonPath("$.score").value(900));
        mvc.perform(get("/api/v1/game-results/2")).andExpect(status().isNotFound());
    }

    @Test
    void postReturns201WithTheStoredResult() throws Exception {
        when(db.create(any())).thenReturn(result(9, "Anna", 900));

        mvc.perform(post("/api/v1/game-results").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"Anna\",\"score\":900,\"level\":1,\"duration\":1.5,\"deaths\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
        verify(db).create(any(GameResult.class));
    }

    @Test
    void postPassesOnAValidationErrorOfTheDatabaseService() throws Exception {
        when(db.create(any())).thenThrow(new DbServiceException(HttpStatus.BAD_REQUEST, "bad", null));

        mvc.perform(post("/api/v1/game-results").contentType(MediaType.APPLICATION_JSON).content("{\"score\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unreachableDatabaseServiceIs503() throws Exception {
        when(db.findAll()).thenThrow(new DbServiceException(HttpStatus.SERVICE_UNAVAILABLE, "down", null));
        when(db.create(any())).thenThrow(new DbServiceException(HttpStatus.SERVICE_UNAVAILABLE, "down", null));

        mvc.perform(get("/api/v1/game-results")).andExpect(status().isServiceUnavailable());
        mvc.perform(post("/api/v1/game-results").contentType(MediaType.APPLICATION_JSON).content("{\"score\":1}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void putUpdatesOrReturns404() throws Exception {
        when(db.update(eq(1L), any())).thenReturn(Optional.of(result(1, "Anna", 1000)));
        when(db.update(eq(2L), any())).thenReturn(Optional.empty());

        mvc.perform(put("/api/v1/game-results/1").contentType(MediaType.APPLICATION_JSON).content("{\"score\":1000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.score").value(1000));
        mvc.perform(put("/api/v1/game-results/2").contentType(MediaType.APPLICATION_JSON).content("{\"score\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204Or404() throws Exception {
        when(db.delete(1)).thenReturn(true);
        when(db.delete(2)).thenReturn(false);

        mvc.perform(delete("/api/v1/game-results/1")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/game-results/2")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/game-results")).andExpect(status().isNoContent());
        verify(db).deleteAll();
    }

    @Test
    void playerAndLast10EndpointsAreForwarded() throws Exception {
        when(db.findByPlayer("Jan Novák")).thenReturn(List.of(result(1, "Jan Novák", 1)));
        when(db.findLast10()).thenReturn(List.of());

        mvc.perform(get("/api/v1/game-results/player/{name}", "Jan Novák"))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/game-results/last/10")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void nonNumericIdIsABadRequest() throws Exception {
        mvc.perform(get("/api/v1/game-results/abc")).andExpect(status().isBadRequest());
        verify(db, org.mockito.Mockito.never()).findById(anyLong());
    }
}
