package cz.vsb.fei.donkeykong.controller;

import cz.vsb.fei.donkeykong.repository.GameLevelRepository;
import cz.vsb.fei.donkeykong.repository.GameResultRepository;
import cz.vsb.fei.donkeykong.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Whole stack (controller, JPA, in-memory H2) through MockMvc. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GameResultControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private GameResultRepository results;
    @Autowired
    private PlayerRepository players;
    @Autowired
    private GameLevelRepository levels;

    private static final String ANNA = """
            {"playerName":"Anna","score":900,"level":1,"duration":42.5,"deaths":2}
            """;

    private long create(String json) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/game-results").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    void createReturns201WithLocationAndCreatesPlayerAndLevel() throws Exception {
        mvc.perform(post("/api/v1/game-results").contentType(MediaType.APPLICATION_JSON).content(ANNA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/game-results/")))
                .andExpect(jsonPath("$.playerName").value("Anna"))
                .andExpect(jsonPath("$.score").value(900))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.duration").value(42.5))
                .andExpect(jsonPath("$.deaths").value(2))
                .andExpect(jsonPath("$.playedAt").exists());

        assertEquals(1, players.count());
        assertEquals(1, levels.count());
    }

    @Test
    void secondResultReusesTheSamePlayerAndLevel() throws Exception {
        create(ANNA);
        create(ANNA);

        assertEquals(2, results.count());
        assertEquals(1, players.count());
        assertEquals(1, levels.count());
    }

    @Test
    void clientSuppliedIdIsIgnored() throws Exception {
        long id = create("{\"id\":999,\"playerName\":\"Anna\",\"score\":5}");

        assertEquals(true, id != 999);
    }

    @Test
    void invalidInputIsRejectedWith400() throws Exception {
        for (String body : new String[]{
                "{\"playerName\":\"Anna\"}",                                // score missing
                "{\"playerName\":\"Anna\",\"score\":-1}",
                "{\"playerName\":\"  \",\"score\":1}",
                "{\"playerName\":\"" + "x".repeat(51) + "\",\"score\":1}",
                "{\"playerName\":\"Anna\",\"score\":1,\"level\":0}",
                "{\"playerName\":\"Anna\",\"score\":1,\"duration\":-3}",
                "{\"playerName\":\"Anna\",\"score\":1,\"deaths\":-1}",
                "{not json",
        }) {
            mvc.perform(post("/api/v1/game-results").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, results.count());
        assertEquals(0, players.count(), "a rejected request must not create players");
    }

    @Test
    void listIsNewestFirstAndSingleResultCanBeRead() throws Exception {
        create("{\"playerName\":\"Old\",\"score\":1,\"playedAt\":\"2026-01-01T10:00:00\"}");
        long newest = create("{\"playerName\":\"New\",\"score\":2,\"playedAt\":\"2026-02-01T10:00:00\"}");

        mvc.perform(get("/api/v1/game-results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].playerName").value("New"));
        mvc.perform(get("/api/v1/game-results/" + newest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(2));
        mvc.perform(get("/api/v1/game-results/424242")).andExpect(status().isNotFound());
    }

    @Test
    void filterByPlayerAndLast10() throws Exception {
        create(ANNA);
        create("{\"playerName\":\"Bob\",\"score\":5}");

        mvc.perform(get("/api/v1/game-results/player/Anna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/game-results/player/Nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/v1/game-results/last/10"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void playerNamesWithSpacesAndUnicodeAreFound() throws Exception {
        create("{\"playerName\":\"Jan Novák\",\"score\":5}");

        mvc.perform(get("/api/v1/game-results/player/{name}", "Jan Novák"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void updateChangesOnlyTheSentFields() throws Exception {
        long id = create(ANNA);

        mvc.perform(put("/api/v1/game-results/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"score\":1000,\"playerName\":\"Bob\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1000))
                .andExpect(jsonPath("$.playerName").value("Bob"))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.deaths").value(2));
    }

    @Test
    void updateRejectsInvalidValuesAndUnknownIds() throws Exception {
        long id = create(ANNA);

        mvc.perform(put("/api/v1/game-results/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"score\":-5}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/game-results/424242").contentType(MediaType.APPLICATION_JSON).content("{\"score\":5}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/game-results/" + id)).andExpect(jsonPath("$.score").value(900));
    }

    @Test
    void deleteOneAndDeleteAll() throws Exception {
        long id = create(ANNA);
        create(ANNA);

        mvc.perform(delete("/api/v1/game-results/" + id)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/game-results/" + id)).andExpect(status().isNotFound());
        assertEquals(1, results.count());

        mvc.perform(delete("/api/v1/game-results")).andExpect(status().isNoContent());
        assertEquals(0, results.count());
    }

    @Test
    void playerStatisticsAreServed() throws Exception {
        create(ANNA);
        create("{\"playerName\":\"Anna\",\"score\":300}");

        mvc.perform(get("/api/v1/players"))
                .andExpect(jsonPath("$[0].name").value("Anna"))
                .andExpect(jsonPath("$[0].gamesPlayed").value(2))
                .andExpect(jsonPath("$[0].highestScore").value(900))
                .andExpect(jsonPath("$[0].averageScore").value(600.0));
        mvc.perform(get("/api/v1/players/Anna")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/players/Nobody")).andExpect(status().isNotFound());
    }

    @Test
    void levelsAreServedByNumber() throws Exception {
        create(ANNA);

        mvc.perform(get("/api/v1/game-levels")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/game-levels/1")).andExpect(jsonPath("$.name").value("Level 1"));
        mvc.perform(get("/api/v1/game-levels/9")).andExpect(status().isNotFound());
    }
}
