package com.fearjosh.frontend.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Service untuk komunikasi dengan backend leaderboard API.
 * Menggunakan libGDX Net untuk HTTP requests.
 */
public class LeaderboardService {
    
    // Backend URL - Render.com deployment
    private static final String BASE_URL = "https://fearjosh.onrender.com/api";
    
    private static LeaderboardService instance;
    private final Json json;
    
    private LeaderboardService() {
        json = new Json();
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
     * Data class untuk submit score request
     */
    public static class ScoreSubmitRequest {
        public String playerId;
        public String username;
        public String difficulty;
        public long completionTimeSeconds;
        
        public ScoreSubmitRequest(String playerId, String username, String difficulty, long completionTimeSeconds) {
            this.playerId = playerId;
            this.username = username;
            this.difficulty = difficulty;
            this.completionTimeSeconds = completionTimeSeconds;
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
        
        String url = BASE_URL + "/scores";
        
        // Build JSON body
        String jsonBody = "{"
            + "\"playerId\":\"" + playerId + "\","
            + "\"username\":\"" + escapeJson(username) + "\","
            + "\"difficulty\":\"" + difficulty + "\","
            + "\"completionTimeSeconds\":" + completionTimeSeconds
            + "}";
        
        System.out.println("[LeaderboardService] Submitting score: " + jsonBody);
        
        Net.HttpRequest request = new HttpRequestBuilder()
            .newRequest()
            .method(Net.HttpMethods.POST)
            .url(url)
            .header("Content-Type", "application/json")
            .content(jsonBody)
            .build();
        
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                String responseString = httpResponse.getResultAsString();
                
                System.out.println("[LeaderboardService] Response: " + statusCode + " - " + responseString);
                
                if (statusCode >= 200 && statusCode < 300) {
                    try {
                        ScoreEntry entry = parseScoreEntry(responseString);
                        Gdx.app.postRunnable(() -> callback.onSuccess(entry));
                    } catch (Exception e) {
                        Gdx.app.postRunnable(() -> callback.onError("Parse error: " + e.getMessage()));
                    }
                } else {
                    Gdx.app.postRunnable(() -> callback.onError("Server error: " + statusCode));
                }
            }
            
            @Override
            public void failed(Throwable t) {
                System.err.println("[LeaderboardService] Request failed: " + t.getMessage());
                Gdx.app.postRunnable(() -> callback.onError("Connection failed: " + t.getMessage()));
            }
            
            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }
    
    /**
     * Ambil leaderboard berdasarkan difficulty
     */
    public void getLeaderboard(String difficulty, int limit, LeaderboardCallback<List<ScoreEntry>> callback) {
        String url = BASE_URL + "/scores/leaderboard?limit=" + limit;
        if (difficulty != null && !difficulty.isEmpty() && !difficulty.equals("ALL")) {
            url += "&difficulty=" + difficulty;
        }
        
        System.out.println("[LeaderboardService] Fetching leaderboard: " + url);
        
        Net.HttpRequest request = new HttpRequestBuilder()
            .newRequest()
            .method(Net.HttpMethods.GET)
            .url(url)
            .build();
        
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                String responseString = httpResponse.getResultAsString();
                
                System.out.println("[LeaderboardService] Leaderboard response: " + statusCode);
                
                if (statusCode >= 200 && statusCode < 300) {
                    try {
                        List<ScoreEntry> entries = parseLeaderboardResponse(responseString);
                        Gdx.app.postRunnable(() -> callback.onSuccess(entries));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Gdx.app.postRunnable(() -> callback.onError("Parse error: " + e.getMessage()));
                    }
                } else {
                    Gdx.app.postRunnable(() -> callback.onError("Server error: " + statusCode));
                }
            }
            
            @Override
            public void failed(Throwable t) {
                System.err.println("[LeaderboardService] Leaderboard request failed: " + t.getMessage());
                Gdx.app.postRunnable(() -> callback.onError("Connection failed: " + t.getMessage()));
            }
            
            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }
    
    /**
     * Cek ranking player
     */
    public void getPlayerRank(String playerId, LeaderboardCallback<ScoreEntry> callback) {
        String url = BASE_URL + "/scores/rank/" + playerId;
        
        Net.HttpRequest request = new HttpRequestBuilder()
            .newRequest()
            .method(Net.HttpMethods.GET)
            .url(url)
            .build();
        
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                String responseString = httpResponse.getResultAsString();
                
                if (statusCode >= 200 && statusCode < 300) {
                    try {
                        ScoreEntry entry = parsePlayerRankResponse(responseString);
                        Gdx.app.postRunnable(() -> callback.onSuccess(entry));
                    } catch (Exception e) {
                        Gdx.app.postRunnable(() -> callback.onError("Parse error: " + e.getMessage()));
                    }
                } else {
                    Gdx.app.postRunnable(() -> callback.onError("Server error: " + statusCode));
                }
            }
            
            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError("Connection failed: " + t.getMessage()));
            }
            
            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }
    
    /**
     * Check backend health
     */
    public void checkHealth(LeaderboardCallback<Boolean> callback) {
        String url = BASE_URL + "/scores/health";
        
        Net.HttpRequest request = new HttpRequestBuilder()
            .newRequest()
            .method(Net.HttpMethods.GET)
            .url(url)
            .build();
        
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                Gdx.app.postRunnable(() -> callback.onSuccess(statusCode == 200));
            }
            
            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onSuccess(false));
            }
            
            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onSuccess(false));
            }
        });
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
        
        // Response: {"difficulty":"MEDIUM","totalPlayers":10,"leaderboard":[...]}
        JsonValue leaderboard = root.get("leaderboard");
        if (leaderboard == null) {
            leaderboard = root; // Direct array
        }
        
        int rank = 1;
        for (JsonValue entry = leaderboard.child; entry != null; entry = entry.next) {
            ScoreEntry scoreEntry = parseScoreEntryFromJson(entry);
            scoreEntry.rank = rank++;
            entries.add(scoreEntry);
        }
        
        return entries;
    }
    
    private ScoreEntry parsePlayerRankResponse(String jsonString) {
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(jsonString);
        
        ScoreEntry entry = new ScoreEntry();
        entry.playerId = root.getString("playerId", "");
        entry.rank = root.getInt("rank", 0);
        entry.completionTimeSeconds = root.getLong("bestTimeSeconds", 0);
        entry.completionTimeFormatted = root.getString("bestTimeFormatted", "00:00");
        entry.difficulty = root.getString("difficulty", "");
        
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
}
