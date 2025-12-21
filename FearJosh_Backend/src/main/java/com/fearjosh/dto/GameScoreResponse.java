package com.fearjosh.dto;

import java.time.LocalDateTime;

public class GameScoreResponse {

    private Long id;
    private String playerId;
    private String username;
    private String difficulty;
    private Long completionTimeSeconds;
    private String completionTimeFormatted;
    private LocalDateTime completedAt;
    private Integer rank;

    public GameScoreResponse() {}

    // Constructor without rank (for single score response)
    public GameScoreResponse(Long id, String playerId, String username, String difficulty,
                             Long completionTimeSeconds, String completionTimeFormatted,
                             LocalDateTime completedAt) {
        this.id = id;
        this.playerId = playerId;
        this.username = username;
        this.difficulty = difficulty;
        this.completionTimeSeconds = completionTimeSeconds;
        this.completionTimeFormatted = completionTimeFormatted;
        this.completedAt = completedAt;
    }

    // Full constructor
    public GameScoreResponse(Long id, String playerId, String username, String difficulty,
                             Long completionTimeSeconds, String completionTimeFormatted,
                             LocalDateTime completedAt, Integer rank) {
        this.id = id;
        this.playerId = playerId;
        this.username = username;
        this.difficulty = difficulty;
        this.completionTimeSeconds = completionTimeSeconds;
        this.completionTimeFormatted = completionTimeFormatted;
        this.completedAt = completedAt;
        this.rank = rank;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Long getCompletionTimeSeconds() { return completionTimeSeconds; }
    public void setCompletionTimeSeconds(Long completionTimeSeconds) { this.completionTimeSeconds = completionTimeSeconds; }

    public String getCompletionTimeFormatted() { return completionTimeFormatted; }
    public void setCompletionTimeFormatted(String completionTimeFormatted) { this.completionTimeFormatted = completionTimeFormatted; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
}
