package com.fearjosh.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_scores")
public class GameScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, unique = true)
    private String playerId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String difficulty;

    @Column(name = "completion_time_seconds", nullable = false)
    private Long completionTimeSeconds;

    @Column(name = "completion_time_formatted", nullable = false)
    private String completionTimeFormatted;

    @CreationTimestamp
    @Column(name = "completed_at", updatable = false)
    private LocalDateTime completedAt;

    // Default constructor
    public GameScore() {}

    // Constructor without id and timestamp (for creating new records)
    public GameScore(String playerId, String username, String difficulty, 
                     Long completionTimeSeconds, String completionTimeFormatted) {
        this.playerId = playerId;
        this.username = username;
        this.difficulty = difficulty;
        this.completionTimeSeconds = completionTimeSeconds;
        this.completionTimeFormatted = completionTimeFormatted;
    }

    // Full constructor
    public GameScore(Long id, String playerId, String username, String difficulty,
                     Long completionTimeSeconds, String completionTimeFormatted, LocalDateTime completedAt) {
        this.id = id;
        this.playerId = playerId;
        this.username = username;
        this.difficulty = difficulty;
        this.completionTimeSeconds = completionTimeSeconds;
        this.completionTimeFormatted = completionTimeFormatted;
        this.completedAt = completedAt;
    }

    // Getters and Setters
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
}
