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

    /**
     * Find a game score by player ID
     */
    Optional<GameScore> findByPlayerId(String playerId);

    /**
     * Check if a player ID exists
     */
    boolean existsByPlayerId(String playerId);

    /**
     * Find all scores by difficulty, ordered by completion time (fastest first)
     */
    List<GameScore> findByDifficultyOrderByCompletionTimeSecondsAsc(String difficulty);

    /**
     * Find top N scores by difficulty
     */
    List<GameScore> findByDifficultyOrderByCompletionTimeSecondsAsc(String difficulty, Pageable pageable);

    /**
     * Find all scores ordered by completion time (fastest first)
     */
    List<GameScore> findAllByOrderByCompletionTimeSecondsAsc();

    /**
     * Find top N scores across all difficulties
     */
    List<GameScore> findAllByOrderByCompletionTimeSecondsAsc(Pageable pageable);

    /**
     * Count total players by difficulty
     */
    long countByDifficulty(String difficulty);

    /**
     * Get rank of a specific player in a difficulty
     */
    @Query("SELECT COUNT(g) + 1 FROM GameScore g WHERE g.difficulty = :difficulty AND g.completionTimeSeconds < :time")
    int getPlayerRank(@Param("difficulty") String difficulty, @Param("time") Long completionTimeSeconds);

    /**
     * Get rank of a specific player across all difficulties
     */
    @Query("SELECT COUNT(g) + 1 FROM GameScore g WHERE g.completionTimeSeconds < :time")
    int getPlayerGlobalRank(@Param("time") Long completionTimeSeconds);

    /**
     * Find scores by username (case-insensitive partial match)
     */
    List<GameScore> findByUsernameContainingIgnoreCase(String username);

    /**
     * Find best score for each difficulty by a player
     */
    @Query("SELECT g FROM GameScore g WHERE g.playerId = :playerId ORDER BY g.completionTimeSeconds ASC")
    List<GameScore> findBestScoresByPlayerId(@Param("playerId") String playerId);
}
