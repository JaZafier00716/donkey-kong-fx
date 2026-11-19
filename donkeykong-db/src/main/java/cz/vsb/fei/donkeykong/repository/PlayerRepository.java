package cz.vsb.fei.donkeykong.repository;

import cz.vsb.fei.donkeykong.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByName(String name);

    List<Player> findAllByOrderByNameAsc();

    /** Per-player statistics computed by the database in one query. */
    interface PlayerStats {
        Long getId();

        String getName();

        long getGamesPlayed();

        Integer getHighestScore();

        double getAverageScore();
    }

    String STATS_QUERY = """
            select p.id as id, p.name as name, count(r) as gamesPlayed, max(r.score) as highestScore,
                   coalesce(avg(r.score), 0.0) as averageScore
            from Player p left join p.gameResults r
            """;

    @Query(STATS_QUERY + " group by p.id, p.name order by p.name")
    List<PlayerStats> findAllStats();

    @Query(STATS_QUERY + " where p.name = :name group by p.id, p.name")
    Optional<PlayerStats> findStatsByName(@Param("name") String name);
}
