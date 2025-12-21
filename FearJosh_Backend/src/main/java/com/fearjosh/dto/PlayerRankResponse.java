package com.fearjosh.dto;

public class PlayerRankResponse {

    private String playerId;
    private String username;
    private String difficulty;
    private Long completionTimeSeconds;
    private String completionTimeFormatted;
    private Integer rank;
    private Integer totalPlayers;

    public PlayerRankResponse() {}

    public PlayerRankResponse(String playerId, String username, String difficulty,
                              Long completionTimeSeconds, String completionTimeFormatted,
                              Integer rank, Integer totalPlayers) {
        this.playerId = playerId;
        this.username = username;
        this.difficulty = difficulty;
        this.completionTimeSeconds = completionTimeSeconds;
        this.completionTimeFormatted = completionTimeFormatted;
        this.rank = rank;
        this.totalPlayers = totalPlayers;
    }

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

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }

    public Integer getTotalPlayers() { return totalPlayers; }
    public void setTotalPlayers(Integer totalPlayers) { this.totalPlayers = totalPlayers; }
}
