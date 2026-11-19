package cz.vsb.fei.DonkeyKongFX.controller;

import cz.vsb.fei.DonkeyKongFX.dto.GameResult;
import cz.vsb.fei.DonkeyKongFX.service.DbServiceClient;
import cz.vsb.fei.DonkeyKongFX.service.DbServiceException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for the game: forwards every call to the database service.
 */
@RestController
@RequestMapping("/api/v1/game-results")
@Log4j2
public class GameResultController {

    @Autowired
    private DbServiceClient dbService;

    @GetMapping
    public List<GameResult> getAllGameResults() {
        return dbService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameResult> getGameResultById(@PathVariable Long id) {
        return ResponseEntity.of(dbService.findById(id));
    }

    @PostMapping
    public ResponseEntity<GameResult> createGameResult(@RequestBody GameResult gameResult) {
        GameResult saved = dbService.create(gameResult);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /** Fields that are not sent keep their value (the database service merges them). */
    @PutMapping("/{id}")
    public ResponseEntity<GameResult> updateGameResult(@PathVariable Long id, @RequestBody GameResult changes) {
        return ResponseEntity.of(dbService.update(id, changes));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGameResult(@PathVariable Long id) {
        return dbService.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/last/10")
    public List<GameResult> getLast10Games() {
        return dbService.findLast10();
    }

    @GetMapping("/player/{playerName}")
    public List<GameResult> getResultsByPlayer(@PathVariable String playerName) {
        return dbService.findByPlayer(playerName);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllGameResults() {
        dbService.deleteAll();
        return ResponseEntity.noContent().build();
    }

    /** 503 when the database service is down, its own 4xx for bad requests, 502 for anything else. */
    @ExceptionHandler(DbServiceException.class)
    public ResponseEntity<Void> onDbServiceFailure(DbServiceException e) {
        log.warn("Database service call failed: {} ({})", e.getMessage(), e.getCause() == null ? "" : e.getCause().getMessage());
        return ResponseEntity.status(e.getStatus()).build();
    }
}
