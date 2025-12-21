package com.fearjosh.service;

import com.fearjosh.dto.*;
import com.fearjosh.exception.ResourceNotFoundException;
import com.fearjosh.exception.DuplicateResourceException;
import com.fearjosh.model.GameScore;
import com.fearjosh.repository.GameScoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GameScoreService {

    @Autowired
    private GameScoreRepository gameScoreRepository;

    /**
     * Submit a new game score (when player escapes successfully)
     */
    public GameScoreResponse submitScore(GameScoreRequest request) {
        // Check if player already has a score
        if (gameScoreRepository.existsByPlayerId(request.getPlayerId())) {
            // Update existing score if new one is better
            GameScore existingScore = gameScoreRepository.findByPlayerId(request.getPlayerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Score not found"));
            
            // Only update if same difficulty and better time, or different difficulty
            if (existingScore.getDifficulty().equals(request.getDifficulty())) {
                if (request.getCompletionTimeSeconds() < existingScore.getCompletionTimeSeconds()) {
                    existingScore.setCompletionTimeSeconds(request.getCompletionTimeSeconds());
                    existingScore.setCompletionTimeFormatted(formatTime(request.getCompletionTimeSeconds()));
                    existingScore.setUsername(request.getUsername());
                    GameScore savedScore = gameScoreRepository.save(existingScore);
                    return convertToResponse(savedScore);
                } else {
                    // Return existing score if new time is not better
                    return convertToResponse(existingScore);
                }
            } else {
                // Different difficulty - update everything
                existingScore.setDifficulty(request.getDifficulty());
                existingScore.setCompletionTimeSeconds(request.getCompletionTimeSeconds());
                existingScore.setCompletionTimeFormatted(formatTime(request.getCompletionTimeSeconds()));
                existingScore.setUsername(request.getUsername());
                GameScore savedScore = gameScoreRepository.save(existingScore);
                return convertToResponse(savedScore);
            }
        }

        // Create new score
        GameScore gameScore = new GameScore(
                request.getPlayerId(),
                request.getUsername(),
                request.getDifficulty(),
                request.getCompletionTimeSeconds(),
                formatTime(request.getCompletionTimeSeconds())
        );

        GameScore savedScore = gameScoreRepository.save(gameScore);
        return convertToResponse(savedScore);
    }

    /**
     * Get leaderboard by difficulty
     */
    public LeaderboardResponse getLeaderboardByDifficulty(String difficulty, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<GameScore> scores = gameScoreRepository.findByDifficultyOrderByCompletionTimeSecondsAsc(difficulty, pageable);
        
        List<GameScoreResponse> leaderboard = new ArrayList<>();
        int rank = 1;
        for (GameScore score : scores) {
            GameScoreResponse response = convertToResponse(score);
            response.setRank(rank++);
            leaderboard.add(response);
        }

        long totalPlayers = gameScoreRepository.countByDifficulty(difficulty);
        
        return new LeaderboardResponse(difficulty, (int) totalPlayers, leaderboard);
    }

    /**
     * Get global leaderboard (all difficulties)
     */
    public LeaderboardResponse getGlobalLeaderboard(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<GameScore> scores = gameScoreRepository.findAllByOrderByCompletionTimeSecondsAsc(pageable);
        
        List<GameScoreResponse> leaderboard = new ArrayList<>();
        int rank = 1;
        for (GameScore score : scores) {
            GameScoreResponse response = convertToResponse(score);
            response.setRank(rank++);
            leaderboard.add(response);
        }

        long totalPlayers = gameScoreRepository.count();
        
        return new LeaderboardResponse("ALL", (int) totalPlayers, leaderboard);
    }

    /**
     * Get player's rank and score
     */
    public PlayerRankResponse getPlayerRank(String playerId) {
        GameScore score = gameScoreRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with ID: " + playerId));

        int rank = gameScoreRepository.getPlayerRank(score.getDifficulty(), score.getCompletionTimeSeconds());
        long totalPlayers = gameScoreRepository.countByDifficulty(score.getDifficulty());

        return new PlayerRankResponse(
                score.getPlayerId(),
                score.getUsername(),
                score.getDifficulty(),
                score.getCompletionTimeSeconds(),
                score.getCompletionTimeFormatted(),
                rank,
                (int) totalPlayers
        );
    }

    /**
     * Get player's global rank
     */
    public PlayerRankResponse getPlayerGlobalRank(String playerId) {
        GameScore score = gameScoreRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with ID: " + playerId));

        int rank = gameScoreRepository.getPlayerGlobalRank(score.getCompletionTimeSeconds());
        long totalPlayers = gameScoreRepository.count();

        return new PlayerRankResponse(
                score.getPlayerId(),
                score.getUsername(),
                "ALL",
                score.getCompletionTimeSeconds(),
                score.getCompletionTimeFormatted(),
                rank,
                (int) totalPlayers
        );
    }

    /**
     * Get score by player ID
     */
    public GameScoreResponse getScoreByPlayerId(String playerId) {
        GameScore score = gameScoreRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with ID: " + playerId));
        return convertToResponse(score);
    }

    /**
     * Search players by username
     */
    public List<GameScoreResponse> searchByUsername(String username) {
        List<GameScore> scores = gameScoreRepository.findByUsernameContainingIgnoreCase(username);
        return scores.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all scores (for admin purposes)
     */
    public List<GameScoreResponse> getAllScores() {
        return gameScoreRepository.findAllByOrderByCompletionTimeSecondsAsc()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete a score by player ID
     */
    public void deleteScore(String playerId) {
        GameScore score = gameScoreRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with ID: " + playerId));
        gameScoreRepository.delete(score);
    }

    /**
     * Check if player exists
     */
    public boolean playerExists(String playerId) {
        return gameScoreRepository.existsByPlayerId(playerId);
    }

    /**
     * Format time in seconds to readable format (MM:SS or HH:MM:SS)
     */
    private String formatTime(Long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    /**
     * Convert GameScore entity to response DTO
     */
    private GameScoreResponse convertToResponse(GameScore score) {
        return new GameScoreResponse(
                score.getId(),
                score.getPlayerId(),
                score.getUsername(),
                score.getDifficulty(),
                score.getCompletionTimeSeconds(),
                score.getCompletionTimeFormatted(),
                score.getCompletedAt()
        );
    }
}
