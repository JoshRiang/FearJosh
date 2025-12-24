package com.fearjosh.repository;

import com.fearjosh.model.GameScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameScoreRepository extends JpaRepository<GameScore, Long> {

    Optional<GameScore> findByPlayerId(String playerId);

    boolean existsByPlayerId(String playerId);

    List<GameScore> findByDifficultyOrderByCompletionTimeSecondsAsc(String difficulty);

    List<GameScore> findByDifficultyOrderByCompletionTimeSecondsAsc(String difficulty, Pageable pageable);

    List<GameScore> findAllByOrderByCompletionTimeSecondsAsc();

    List<GameScore> findAllByOrderByCompletionTimeSecondsAsc(Pageable pageable);

    long countByDifficulty(String difficulty);

    @Query("SELECT COUNT(g) + 1 FROM GameScore g WHERE g.difficulty = :difficulty AND g.completionTimeSeconds < :time")
    int getPlayerRank(@Param("difficulty") String difficulty, @Param("time") Long completionTimeSeconds);

    @Query("SELECT COUNT(g) + 1 FROM GameScore g WHERE g.completionTimeSeconds < :time")
    int getPlayerGlobalRank(@Param("time") Long completionTimeSeconds);

    List<GameScore> findByUsernameContainingIgnoreCase(String username);

    @Query("SELECT g FROM GameScore g WHERE g.playerId = :playerId ORDER BY g.completionTimeSeconds ASC")
    List<GameScore> findBestScoresByPlayerId(@Param("playerId") String playerId);
}
