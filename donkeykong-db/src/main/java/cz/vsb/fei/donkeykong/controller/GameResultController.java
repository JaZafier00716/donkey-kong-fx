package cz.vsb.fei.donkeykong.controller;

import cz.vsb.fei.donkeykong.entity.GameResult;
import cz.vsb.fei.donkeykong.entity.GameLevel;
import cz.vsb.fei.donkeykong.entity.Player;
import cz.vsb.fei.donkeykong.repository.GameResultRepository;
import cz.vsb.fei.donkeykong.repository.GameLevelRepository;
import cz.vsb.fei.donkeykong.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/game-results")
public class GameResultController {
    /** Longest accepted player name (the game limits the input field to 12). */
    static final int MAX_PLAYER_NAME_LENGTH = 50;

    @Autowired
    private GameResultRepository gameResultRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GameLevelRepository gameLevelRepository;

    /**
     * Get all game results
     */
    @GetMapping
    public ResponseEntity<List<GameResultDto>> getAllGameResults() {
        List<GameResult> results = gameResultRepository.findAllByOrderByPlayedAtDesc();
        return ResponseEntity.ok(results.stream().map(GameResultController::toDto).toList());
    }

    /**
     * Get game result by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<GameResultDto> getGameResultById(@PathVariable Long id) {
        Optional<GameResult> result = gameResultRepository.findById(id);
        return result.map(GameResultController::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Create a new game result; the player and the level are created on first use.
     */
    @PostMapping
    @Transactional
    public ResponseEntity<GameResultDto> createGameResult(@RequestBody GameResultRequest request) {
        validate(request);
        if (request.score() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "score is required");
        }
        GameResult gameResult = new GameResult();
        gameResult.setScore(request.score());
        gameResult.setPlayedAt(request.playedAt() != null ? request.playedAt() : LocalDateTime.now());
        gameResult.setDuration(request.duration());
        gameResult.setDeaths(request.deaths());
        gameResult.setPlayer(findOrCreatePlayer(request.playerName()));
        gameResult.setGameLevel(findOrCreateLevel(request.level()));

        GameResult saved = gameResultRepository.save(gameResult);
        return ResponseEntity.created(URI.create("/api/v1/game-results/" + saved.getId())).body(toDto(saved));
    }

    /**
     * Update an existing game result; fields that are not sent keep their value.
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<GameResultDto> updateGameResult(@PathVariable Long id, @RequestBody GameResultRequest request) {
        validate(request);
        return gameResultRepository.findById(id).map(gameResult -> {
            if (request.playerName() != null) {
                gameResult.setPlayer(findOrCreatePlayer(request.playerName()));
            }
            if (request.score() != null) {
                gameResult.setScore(request.score());
            }
            if (request.playedAt() != null) {
                gameResult.setPlayedAt(request.playedAt());
            }
            if (request.level() != null) {
                gameResult.setGameLevel(findOrCreateLevel(request.level()));
            }
            if (request.duration() != null) {
                gameResult.setDuration(request.duration());
            }
            if (request.deaths() != null) {
                gameResult.setDeaths(request.deaths());
            }
            return ResponseEntity.ok(toDto(gameResultRepository.save(gameResult)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Delete a game result by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGameResult(@PathVariable Long id) {
        if (gameResultRepository.existsById(id)) {
            gameResultRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get last 10 games
     */
    @GetMapping("/last/10")
    public ResponseEntity<List<GameResultDto>> getLast10Games() {
        List<GameResult> results = gameResultRepository.findTop10ByOrderByPlayedAtDesc();
        return ResponseEntity.ok(results.stream().map(GameResultController::toDto).toList());
    }

    /**
     * Get game results by player name
     */
    @GetMapping("/player/{playerName}")
    public ResponseEntity<List<GameResultDto>> getResultsByPlayer(@PathVariable String playerName) {
        List<GameResult> results = gameResultRepository.findByPlayer_NameOrderByPlayedAtDesc(playerName);
        return ResponseEntity.ok(results.stream().map(GameResultController::toDto).toList());
    }

    /**
     * Delete all game results
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllGameResults() {
        gameResultRepository.deleteAll();
        return ResponseEntity.noContent().build();
    }

    private static GameResultDto toDto(GameResult result) {
        return new GameResultDto(
                result.getId(),
                result.getPlayer() == null ? null : result.getPlayer().getName(),
                result.getScore(),
                result.getPlayedAt(),
                result.getGameLevel() == null ? null : result.getGameLevel().getLevelNumber(),
                result.getDuration(),
                result.getDeaths()
        );
    }

    private static void validate(GameResultRequest request) {
        String problem = request.problem();
        if (problem != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, problem);
        }
    }

    /** Returns the player with this name, creating it on first use; a missing name means "anonymous". */
    private Player findOrCreatePlayer(String name) {
        if (name == null) {
            return null;
        }
        return playerRepository.findByName(name).orElseGet(() -> playerRepository.save(new Player(name)));
    }

    private GameLevel findOrCreateLevel(Integer levelNumber) {
        if (levelNumber == null) {
            return null;
        }
        return gameLevelRepository.findByLevelNumber(levelNumber)
                .orElseGet(() -> gameLevelRepository.save(new GameLevel(levelNumber, "Level " + levelNumber)));
    }

    /** Two requests creating the same new player or level at once: the loser should simply retry. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Void> onConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    public record GameResultDto(
            Long id,
            String playerName,
            Integer score,
            LocalDateTime playedAt,
            Integer level,
            Double duration,
            Integer deaths
    ) {
    }
}
