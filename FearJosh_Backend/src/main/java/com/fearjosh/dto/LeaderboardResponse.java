package com.fearjosh.dto;

import java.util.List;

public class LeaderboardResponse {

    private String difficulty;
    private int totalPlayers;
    private List<GameScoreResponse> leaderboard;

    public LeaderboardResponse() {}

    public LeaderboardResponse(String difficulty, int totalPlayers, List<GameScoreResponse> leaderboard) {
        this.difficulty = difficulty;
        this.totalPlayers = totalPlayers;
        this.leaderboard = leaderboard;
    }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getTotalPlayers() { return totalPlayers; }
    public void setTotalPlayers(int totalPlayers) { this.totalPlayers = totalPlayers; }

    public List<GameScoreResponse> getLeaderboard() { return leaderboard; }
    public void setLeaderboard(List<GameScoreResponse> leaderboard) { this.leaderboard = leaderboard; }
}
