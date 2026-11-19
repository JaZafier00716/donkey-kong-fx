package cz.vsb.fei.DonkeyKongFX.controller;

import cz.vsb.fei.DonkeyKongFX.config.LocaleConfig;
import cz.vsb.fei.DonkeyKongFX.dto.GameResult;
import cz.vsb.fei.DonkeyKongFX.service.DbServiceClient;
import cz.vsb.fei.DonkeyKongFX.service.DbServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Renders the real Thymeleaf pages against a mocked database client. */
@WebMvcTest({DashboardUIController.class, GameResultUIController.class})
@Import({LocaleConfig.class, GlobalModelAttributes.class})
@TestPropertySource(properties = {
        "donkeykong.db.base-url=http://db.example:9090",
        "spring.messages.fallback-to-system-locale=false"})
class UiControllersTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private DbServiceClient db;

    private Locale originalDefault;

    @BeforeEach
    void czechComputer() {
        originalDefault = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("cs-CZ"));
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalDefault);
    }

    private static GameResult result(long id, String name, int score, double duration, int minute) {
        GameResult r = new GameResult();
        r.setId(id);
        r.setPlayerName(name);
        r.setScore(score);
        r.setLevel(1);
        r.setDuration(duration);
        r.setDeaths(1);
        r.setPlayedAt(LocalDateTime.of(2026, 1, 1, 12, minute));
        return r;
    }

    @Test
    void dashboardShowsTheBestNumbers() throws Exception {
        when(db.findAll()).thenReturn(List.of(
                result(1, "Anna", 900, 30.5, 1), result(2, "Bob", 100, 20.25, 2), result(3, "Anna", 300, 40, 3)));

        mvc.perform(get("/ui/dashboard").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fastest Time")))
                .andExpect(content().string(containsString("20.250 s")))
                .andExpect(content().string(containsString(">900<")))
                .andExpect(content().string(containsString(">2<")))
                .andExpect(content().string(containsString("http://db.example:9090/swagger-ui.html")));
    }

    @Test
    void englishStaysEnglishOnAComputerWithAnotherSystemLanguage() throws Exception {
        when(db.findAll()).thenReturn(List.of());

        mvc.perform(get("/ui/dashboard").param("lang", "en"))
                .andExpect(content().string(containsString("Dashboard")))
                .andExpect(content().string(not(containsString("Přehled"))));
        mvc.perform(get("/ui/dashboard").param("lang", "cs"))
                .andExpect(content().string(containsString("Přehled")));
        mvc.perform(get("/ui/dashboard").param("lang", "de"))
                .andExpect(content().string(containsString("Übersicht")));
    }

    @Test
    void dashboardWithoutGamesShowsTheEmptyState() throws Exception {
        when(db.findAll()).thenReturn(List.of());

        mvc.perform(get("/").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No Games Yet")))
                .andExpect(content().string(containsString("N/A")));
    }

    @Test
    void dashboardExplainsWhenTheDatabaseIsDown() throws Exception {
        when(db.findAll()).thenThrow(new DbServiceException(HttpStatus.SERVICE_UNAVAILABLE, "down", null));

        mvc.perform(get("/ui/dashboard").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Is the database service running?")))
                .andExpect(content().string(not(containsString("No Games Yet"))));
    }

    @Test
    void resultsTableIsSortedByTheRequestedColumn() throws Exception {
        when(db.findAll()).thenReturn(List.of(
                result(1, "Anna", 300, 30, 1), result(2, "Bob", 900, 20, 2), result(3, "Cyril", 600, 10, 3)));

        String html = mvc.perform(get("/ui/game-results").param("lang", "en").param("sort", "score").param("direction", "desc"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(html.indexOf("Bob") < html.indexOf("Cyril"));
        org.junit.jupiter.api.Assertions.assertTrue(html.indexOf("Cyril") < html.indexOf("Anna"));
    }

    @Test
    void searchFiltersByPlayer() throws Exception {
        when(db.findByPlayer("Anna")).thenReturn(List.of(result(1, "Anna", 300, 30, 1)));

        mvc.perform(get("/ui/game-results").param("player", " Anna ").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Anna")));
        verify(db).findByPlayer("Anna");
    }

    @Test
    void confirmTextWithAnApostropheCannotBreakTheButton() throws Exception {
        when(db.findAll()).thenReturn(List.of(result(1, "Anna", 300, 30, 1)));

        mvc.perform(get("/ui/game-results").param("lang", "en"))
                .andExpect(content().string(containsString("data-confirm=\"Are you sure you want to delete this game result?\"")))
                .andExpect(content().string(containsString("onclick=\"return confirm(this.dataset.confirm)\"")));
    }

    @Test
    void invalidSortParametersFallBackToTheDefault() throws Exception {
        when(db.findAll()).thenReturn(List.of());

        mvc.perform(get("/ui/game-results").param("sort", "'; drop table").param("direction", "sideways"))
                .andExpect(status().isOk());
    }

    @Test
    void resultsPageExplainsWhenTheDatabaseIsDown() throws Exception {
        when(db.findAll()).thenThrow(new DbServiceException(HttpStatus.SERVICE_UNAVAILABLE, "down", null));

        mvc.perform(get("/ui/game-results").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Is the database service running?")));
    }

    @Test
    void deleteRedirectsBackToTheTableEvenWhenItFails() throws Exception {
        when(db.delete(5)).thenThrow(new DbServiceException(HttpStatus.SERVICE_UNAVAILABLE, "down", null));

        mvc.perform(post("/ui/game-results/5/delete")).andExpect(redirectedUrl("/ui/game-results"));
        mvc.perform(post("/ui/game-results/delete-all")).andExpect(redirectedUrl("/ui/game-results"));
        verify(db).deleteAll();
    }
}
