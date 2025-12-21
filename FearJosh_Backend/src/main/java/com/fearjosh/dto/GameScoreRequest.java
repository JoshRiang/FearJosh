package com.fearjosh.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
