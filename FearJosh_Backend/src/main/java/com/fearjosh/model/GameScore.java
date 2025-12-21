package com.fearjosh.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    // Constructor without id and timestamp (for creating new records)
    public GameScore(String playerId, String username, String difficulty, 
                     Long completionTimeSeconds, String completionTimeFormatted) {
        this.playerId = playerId;
        this.username = username;
        this.difficulty = difficulty;
        this.completionTimeSeconds = completionTimeSeconds;
        this.completionTimeFormatted = completionTimeFormatted;
    }
}
