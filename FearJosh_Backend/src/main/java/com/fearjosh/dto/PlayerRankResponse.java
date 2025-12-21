package com.fearjosh.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerRankResponse {

    private String playerId;
    private String username;
    private String difficulty;
    private Long completionTimeSeconds;
    private String completionTimeFormatted;
    private Integer rank;
    private Integer totalPlayers;
}
