package cz.vsb.fei.DonkeyKongFX.controller;

import cz.vsb.fei.DonkeyKongFX.service.DbServiceClient;
import cz.vsb.fei.DonkeyKongFX.service.DbServiceException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"", "/ui"})
@Log4j2
public class DashboardUIController {

    @Autowired
    private DbServiceClient dbService;

    @GetMapping({"", "/", "/dashboard"})
    public String index(Model model) {
        try {
            GameResultStatistics statistics = GameResultStatistics.of(dbService.findAll());
            model.addAttribute("fastestGame", statistics.fastestGame());
            model.addAttribute("fastestTime", statistics.fastestTime());
            model.addAttribute("highestScoreGame", statistics.highestScoreGame());
            model.addAttribute("mostGamesPlayer", statistics.mostGamesPlayer());
            model.addAttribute("mostGamesCount", statistics.mostGamesCount());
            model.addAttribute("totalGames", statistics.totalGames());
            log.info("Loaded {} game results for dashboard", statistics.totalGames());
        } catch (DbServiceException e) {
            log.error("Error loading dashboard statistics", e);
            model.addAttribute("error", "error.dbUnavailable");
        }
        return "index";
    }
}
