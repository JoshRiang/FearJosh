package com.fearjosh.frontend.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service untuk komunikasi dengan backend leaderboard API.
 * Menggunakan Java HttpURLConnection untuk HTTPS support.
 */
public class LeaderboardService {
    
    // Backend URL - Render.com deployment
    private static final String BASE_URL = "https://fearjosh.onrender.com/api";
    
    private static LeaderboardService instance;
    private final ExecutorService executor;
    
    private LeaderboardService() {
        executor = Executors.newFixedThreadPool(2);
    }
    
    public static synchronized LeaderboardService getInstance() {
        if (instance == null) {
            instance = new LeaderboardService();
        }
        return instance;
    }
    
    /**
     * Data class untuk score entry di leaderboard
     */
    public static class ScoreEntry {
        public long id;
        public String playerId;
        public String username;
        public String difficulty;
        public long completionTimeSeconds;
        public String completionTimeFormatted;
        public String completedAt;
        public int rank;
        
        @Override
        public String toString() {
            return rank + ". " + username + " - " + completionTimeFormatted + " (" + difficulty + ")";
        }
    }
    
    /**
     * Callback interface untuk async operations
     */
    public interface LeaderboardCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }
    
    /**
     * Submit skor pemain ke backend
     */
    public void submitScore(String playerId, String username, String difficulty, long completionTimeSeconds, 
                           LeaderboardCallback<ScoreEntry> callback) {
        
        executor.submit(() -> {
            try {
                String url = BASE_URL + "/scores";
                
                // Build JSON body
                String jsonBody = "{"
                    + "\"playerId\":\"" + escapeJson(playerId) + "\","
                    + "\"username\":\"" + escapeJson(username) + "\","
                    + "\"difficulty\":\"" + difficulty + "\","
                    + "\"completionTimeSeconds\":" + completionTimeSeconds
                    + "}";
                
                System.out.println("[LeaderboardService] Submitting score to: " + url);
                System.out.println("[LeaderboardService] Body: " + jsonBody);
                
                String response = doPostRequest(url, jsonBody);
                
                System.out.println("[LeaderboardService] Response: " + response);
                
                ScoreEntry entry = parseScoreEntry(response);
                postToMainThread(() -> callback.onSuccess(entry));
                
            } catch (Exception e) {
                System.err.println("[LeaderboardService] Submit error: " + e.getMessage());
                e.printStackTrace();
                postToMainThread(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    /**
     * Ambil leaderboard berdasarkan difficulty
     */
    public void getLeaderboard(String difficulty, int limit, LeaderboardCallback<List<ScoreEntry>> callback) {
        executor.submit(() -> {
            try {
                String url = BASE_URL + "/scores/leaderboard?limit=" + limit;
                if (difficulty != null && !difficulty.isEmpty() && !difficulty.equals("ALL")) {
                    url += "&difficulty=" + difficulty;
                }
                
                System.out.println("[LeaderboardService] Fetching leaderboard: " + url);
                
                String response = doGetRequest(url);
                
                System.out.println("[LeaderboardService] Leaderboard response received");
                
                List<ScoreEntry> entries = parseLeaderboardResponse(response);
                postToMainThread(() -> callback.onSuccess(entries));
                
            } catch (Exception e) {
                System.err.println("[LeaderboardService] Leaderboard error: " + e.getMessage());
                e.printStackTrace();
                postToMainThread(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    /**
     * Cek ranking player
     */
    public void getPlayerRank(String playerId, LeaderboardCallback<ScoreEntry> callback) {
        executor.submit(() -> {
            try {
                String url = BASE_URL + "/scores/rank/" + playerId;
                String response = doGetRequest(url);
                ScoreEntry entry = parsePlayerRankResponse(response);
                postToMainThread(() -> callback.onSuccess(entry));
            } catch (Exception e) {
                postToMainThread(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    /**
     * Check backend health
     */
    public void checkHealth(LeaderboardCallback<Boolean> callback) {
        executor.submit(() -> {
            try {
                String url = BASE_URL + "/scores/health";
                String response = doGetRequest(url);
                boolean success = response.contains("success");
                postToMainThread(() -> callback.onSuccess(success));
            } catch (Exception e) {
                postToMainThread(() -> callback.onSuccess(false));
            }
        });
    }
    
    // ==================== HTTP HELPERS ====================
    
    private String doGetRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(60000);
        
        int responseCode = conn.getResponseCode();
        
        if (responseCode >= 200 && responseCode < 300) {
            return readResponse(conn);
        } else {
            throw new Exception("Server error: " + responseCode);
        }
    }
    
    private String doPostRequest(String urlString, String jsonBody) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(60000);
        
        // Write body
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        
        if (responseCode >= 200 && responseCode < 300) {
            return readResponse(conn);
        } else {
            String errorBody = readErrorResponse(conn);
            throw new Exception("Server error " + responseCode + ": " + errorBody);
        }
    }
    
    private String readResponse(HttpURLConnection conn) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
    
    private String readErrorResponse(HttpURLConnection conn) {
        try {
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            return response.toString();
        } catch (Exception e) {
            return "Unknown error";
        }
    }
    
    private void postToMainThread(Runnable runnable) {
        if (Gdx.app != null) {
            Gdx.app.postRunnable(runnable);
        } else {
            runnable.run();
        }
    }
    
    // ==================== PARSING HELPERS ====================
    
    private ScoreEntry parseScoreEntry(String jsonString) {
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(jsonString);
        
        // Response wrapper: {"success":true,"message":"...","data":{...}}
        JsonValue data = root.get("data");
        if (data == null) {
            data = root; // Direct response
        }
        
        return parseScoreEntryFromJson(data);
    }
    
    private ScoreEntry parseScoreEntryFromJson(JsonValue json) {
        ScoreEntry entry = new ScoreEntry();
        entry.id = json.getLong("id", 0);
        entry.playerId = json.getString("playerId", "");
        entry.username = json.getString("username", "Unknown");
        entry.difficulty = json.getString("difficulty", "MEDIUM");
        entry.completionTimeSeconds = json.getLong("completionTimeSeconds", 0);
        entry.completionTimeFormatted = json.getString("completionTimeFormatted", "00:00");
        entry.completedAt = json.getString("completedAt", "");
        entry.rank = json.getInt("rank", 0);
        return entry;
    }
    
    private List<ScoreEntry> parseLeaderboardResponse(String jsonString) {
        List<ScoreEntry> entries = new ArrayList<>();
        
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(jsonString);
        
        // Response wrapper: {"success":true,"data":{"leaderboard":[...]}}
        JsonValue data = root.get("data");
        if (data == null) {
            data = root;
        }
        
        JsonValue leaderboard = data.get("leaderboard");
        if (leaderboard == null) {
            leaderboard = data; // Direct array
        }
        
        int rank = 1;
        for (JsonValue entry = leaderboard.child; entry != null; entry = entry.next) {
            ScoreEntry scoreEntry = parseScoreEntryFromJson(entry);
            if (scoreEntry.rank == 0) {
                scoreEntry.rank = rank;
            }
            rank++;
            entries.add(scoreEntry);
        }
        
        return entries;
    }
    
    private ScoreEntry parsePlayerRankResponse(String jsonString) {
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(jsonString);
        
        JsonValue data = root.get("data");
        if (data == null) {
            data = root;
        }
        
        ScoreEntry entry = new ScoreEntry();
        entry.playerId = data.getString("playerId", "");
        entry.rank = data.getInt("rank", 0);
        entry.completionTimeSeconds = data.getLong("bestTimeSeconds", 0);
        entry.completionTimeFormatted = data.getString("bestTimeFormatted", "00:00");
        entry.difficulty = data.getString("difficulty", "");
        
        return entry;
    }
    
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * Format seconds ke MM:SS string
     */
    public static String formatTime(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
    
    /**
     * Generate unique player ID (based on timestamp + random)
     */
    public static String generatePlayerId() {
        return "player_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
    
    /**
     * Shutdown executor when app closes
     */
    public void dispose() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
