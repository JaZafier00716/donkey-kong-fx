package cz.vsb.fei.DonkeyKongFX.controller;

import cz.vsb.fei.DonkeyKongFX.dto.GameResult;
import cz.vsb.fei.DonkeyKongFX.service.DbServiceClient;
import cz.vsb.fei.DonkeyKongFX.service.DbServiceException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/ui/game-results")
@Log4j2
public class GameResultUIController {

    @Autowired
    private DbServiceClient dbService;

    /**
     * Table of all game results, optionally of one player, sorted by the chosen column.
     */
    @GetMapping({"", "/"})
    public String index(@RequestParam(required = false) String player,
                        @RequestParam(defaultValue = "playedAt") String sort,
                        @RequestParam(defaultValue = "desc") String direction,
                        Model model) {
        String safeSort = GameResultSorting.normalizeSort(sort);
        String safeDirection = GameResultSorting.normalizeDirection(direction);
        model.addAttribute("searchPlayer", player);
        model.addAttribute("sort", safeSort);
        model.addAttribute("direction", safeDirection);
        model.addAttribute("nextDirection", "asc".equals(safeDirection) ? "desc" : "asc");

        try {
            boolean filtered = player != null && !player.isBlank();
            List<GameResult> results = filtered ? dbService.findByPlayer(player.strip()) : dbService.findAll();
            List<GameResult> sorted = new ArrayList<>(results);
            sorted.sort(GameResultSorting.comparator(safeSort, safeDirection));

            model.addAttribute("gameResults", sorted);
            model.addAttribute("totalGames", sorted.size());
            sorted.stream()
                    .filter(result -> result.getScore() != null)
                    .max(Comparator.comparingInt(GameResult::getScore))
                    .ifPresent(best -> {
                        model.addAttribute("highestScore", best.getScore());
                        model.addAttribute("topPlayer", best.getPlayerName());
                    });
            log.info("Loaded {} game results", sorted.size());
        } catch (DbServiceException e) {
            log.error("Error loading game results", e);
            model.addAttribute("error", "error.dbUnavailable");
            model.addAttribute("gameResults", List.of());
        }
        return "game-results";
    }

    @PostMapping("/{id}/delete")
    public String deleteGameResult(@PathVariable Long id) {
        try {
            dbService.delete(id);
            log.info("Deleted game result with id: {}", id);
        } catch (DbServiceException e) {
            log.error("Error deleting game result with id: {}", id, e);
        }
        return "redirect:/ui/game-results";
    }

    @PostMapping("/delete-all")
    public String deleteAllGameResults() {
        try {
            dbService.deleteAll();
            log.info("Deleted all game results");
        } catch (DbServiceException e) {
            log.error("Error deleting all game results", e);
        }
        return "redirect:/ui/game-results";
    }
}
