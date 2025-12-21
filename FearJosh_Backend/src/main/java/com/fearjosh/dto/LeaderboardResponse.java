package com.fearjosh.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {

    private String difficulty;
    private int totalPlayers;
    private List<GameScoreResponse> leaderboard;
}
