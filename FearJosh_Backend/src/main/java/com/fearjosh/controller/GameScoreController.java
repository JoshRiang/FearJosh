package com.fearjosh.controller;

import com.fearjosh.dto.*;
import com.fearjosh.service.GameScoreService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scores")
@CrossOrigin(origins = "*")
public class GameScoreController {

    @Autowired
    private GameScoreService gameScoreService;

    @PostMapping
    public ResponseEntity<ApiResponse<GameScoreResponse>> submitScore(
            @Valid @RequestBody GameScoreRequest request) {
        GameScoreResponse response = gameScoreService.submitScore(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Score submitted successfully!", response));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getLeaderboard(
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "10") int limit) {
        
        LeaderboardResponse leaderboard;
        if (difficulty == null || difficulty.isEmpty() || difficulty.equalsIgnoreCase("ALL")) {
            leaderboard = gameScoreService.getGlobalLeaderboard(limit);
        } else {
            leaderboard = gameScoreService.getLeaderboardByDifficulty(difficulty.toUpperCase(), limit);
        }
        
        return ResponseEntity.ok(ApiResponse.success(leaderboard));
    }

    @GetMapping("/leaderboard/global")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getGlobalLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        LeaderboardResponse leaderboard = gameScoreService.getGlobalLeaderboard(limit);
        return ResponseEntity.ok(ApiResponse.success(leaderboard));
    }

    @GetMapping("/rank/{playerId}")
    public ResponseEntity<ApiResponse<PlayerRankResponse>> getPlayerRank(
            @PathVariable String playerId) {
        PlayerRankResponse rankResponse = gameScoreService.getPlayerRank(playerId);
        return ResponseEntity.ok(ApiResponse.success(rankResponse));
    }

    @GetMapping("/rank/{playerId}/global")
    public ResponseEntity<ApiResponse<PlayerRankResponse>> getPlayerGlobalRank(
            @PathVariable String playerId) {
        PlayerRankResponse rankResponse = gameScoreService.getPlayerGlobalRank(playerId);
        return ResponseEntity.ok(ApiResponse.success(rankResponse));
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<ApiResponse<GameScoreResponse>> getPlayerScore(
            @PathVariable String playerId) {
        GameScoreResponse score = gameScoreService.getScoreByPlayerId(playerId);
        return ResponseEntity.ok(ApiResponse.success(score));
    }

    @GetMapping("/exists/{playerId}")
    public ResponseEntity<ApiResponse<Boolean>> checkPlayerExists(
            @PathVariable String playerId) {
        boolean exists = gameScoreService.playerExists(playerId);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<GameScoreResponse>>> searchByUsername(
            @RequestParam String username) {
        List<GameScoreResponse> scores = gameScoreService.searchByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(scores));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<GameScoreResponse>>> getAllScores() {
        List<GameScoreResponse> scores = gameScoreService.getAllScores();
        return ResponseEntity.ok(ApiResponse.success(scores));
    }

    @DeleteMapping("/{playerId}")
    public ResponseEntity<ApiResponse<Void>> deleteScore(@PathVariable String playerId) {
        gameScoreService.deleteScore(playerId);
        return ResponseEntity.ok(ApiResponse.success("Score deleted successfully!", null));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("FearJosh Backend is running!"));
    }
}
