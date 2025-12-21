package com.fearjosh.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameScoreResponse {

    private Long id;
    private String playerId;
    private String username;
    private String difficulty;
    private Long completionTimeSeconds;
    private String completionTimeFormatted;
    private LocalDateTime completedAt;
    private Integer rank;

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
}
