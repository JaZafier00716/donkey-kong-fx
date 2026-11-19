package cz.vsb.fei.donkeykong.controller;

import cz.vsb.fei.donkeykong.repository.PlayerRepository;
import cz.vsb.fei.donkeykong.repository.PlayerRepository.PlayerStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    @Autowired
    private PlayerRepository playerRepository;

    @GetMapping
    public ResponseEntity<List<PlayerDto>> getAllPlayers() {
        return ResponseEntity.ok(playerRepository.findAllStats().stream().map(PlayerController::toDto).toList());
    }

    @GetMapping("/{name}")
    public ResponseEntity<PlayerDto> getPlayer(@PathVariable String name) {
        return playerRepository.findStatsByName(name)
                .map(PlayerController::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static PlayerDto toDto(PlayerStats stats) {
        return new PlayerDto(stats.getId(), stats.getName(), (int) stats.getGamesPlayed(),
                stats.getHighestScore(), stats.getAverageScore());
    }

    public record PlayerDto(Long id, String name, int gamesPlayed, Integer highestScore, double averageScore) {
    }
}
