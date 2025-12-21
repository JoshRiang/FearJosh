package com.fearjosh.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GameScoreRequest {

    @NotBlank(message = "Player ID is required")
    private String playerId;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Difficulty is required")
    private String difficulty;

    @NotNull(message = "Completion time is required")
    @Min(value = 1, message = "Completion time must be at least 1 second")
    private Long completionTimeSeconds;

    public GameScoreRequest() {}

    public GameScoreRequest(String playerId, String username, String difficulty, Long completionTimeSeconds) {
        this.playerId = playerId;
        this.username = username;
        this.difficulty = difficulty;
        this.completionTimeSeconds = completionTimeSeconds;
    }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Long getCompletionTimeSeconds() { return completionTimeSeconds; }
    public void setCompletionTimeSeconds(Long completionTimeSeconds) { this.completionTimeSeconds = completionTimeSeconds; }
}
