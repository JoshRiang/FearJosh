package com.fearjosh.frontend.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.network.LeaderboardService;
import com.fearjosh.frontend.systems.KeyManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Leaderboard Screen - Menampilkan 3 kolom untuk EASY, MEDIUM, HARD
 */
public class LeaderboardScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 900f;
    private static final float VIRTUAL_HEIGHT = 600f;
    private static final int MAX_ENTRIES_PER_COLUMN = 10;

    /**
     * Tipe ending untuk menentukan apakah score perlu disubmit
     */
    public enum EndingType {
        VIEW_ONLY,    // Hanya lihat leaderboard (dari menu)
        ESCAPE,       // Berhasil kabur - submit score
        GAME_OVER     // Nyawa habis - tidak submit, hanya lihat
    }

    private final FearJosh game;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont titleFont;
    private BitmapFont columnHeaderFont;
    private BitmapFont entryFont;
    private BitmapFont smallFont;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private GlyphLayout glyphLayout;

    // Leaderboard data - 3 separate lists for each difficulty
    private List<LeaderboardService.ScoreEntry> easyEntries = new ArrayList<>();
    private List<LeaderboardService.ScoreEntry> mediumEntries = new ArrayList<>();
    private List<LeaderboardService.ScoreEntry> hardEntries = new ArrayList<>();
    
    private boolean loadingEasy = true;
    private boolean loadingMedium = true;
    private boolean loadingHard = true;
    private String errorMessage = null;

    // Animation
    private float fadeIn = 0f;

    // Ending mode
    private final EndingType endingType;
    private final long completionTimeSeconds;
    private boolean scoreSubmitted = false;
    private boolean isSubmitting = false;
    private int playerRank = -1;
    private String resultMessage = "";

    // Column dimensions
    private static final float COLUMN_WIDTH = 270f;
    private static final float COLUMN_GAP = 20f;
    private static final float COLUMN_START_X = 35f;

    public LeaderboardScreen(FearJosh game) {
        this(game, EndingType.VIEW_ONLY, 0);
    }

    public LeaderboardScreen(FearJosh game, String initialDifficulty) {
        this(game, EndingType.VIEW_ONLY, 0);
    }

    /**
     * Constructor untuk menampilkan leaderboard setelah ending
     */
    public LeaderboardScreen(FearJosh game, EndingType endingType, long completionTimeSeconds) {
        this.game = game;
        this.endingType = endingType;
        this.completionTimeSeconds = completionTimeSeconds;

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        glyphLayout = new GlyphLayout();

        // Create fonts
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.2f);

        columnHeaderFont = new BitmapFont();
        columnHeaderFont.getData().setScale(1.4f);

        entryFont = new BitmapFont();
        entryFont.getData().setScale(1.1f);

        smallFont = new BitmapFont();
        smallFont.getData().setScale(0.95f);

        System.out.println("[Leaderboard] Screen created with 3-column layout");
    }

    @Override
    public void show() {
        // If this is an escape ending, submit score first
        if (endingType == EndingType.ESCAPE && !scoreSubmitted) {
            submitScoreAndLoadLeaderboard();
        } else {
            loadAllLeaderboards();
        }
    }

    private void submitScoreAndLoadLeaderboard() {
        isSubmitting = true;
        
        GameManager gm = GameManager.getInstance();
        String playerId = gm.getPlayerId();
        String playerName = gm.getPlayerName();
        String difficulty = gm.getDifficulty().name();
        
        if (playerName == null || playerName.isEmpty()) {
            playerName = "Unknown";
        }
        if (playerId == null || playerId.isEmpty()) {
            playerId = "unknown_" + System.currentTimeMillis();
        }
        
        System.out.println("[Leaderboard] Submitting score - Player: " + playerName + 
                          ", Time: " + completionTimeSeconds + "s, Difficulty: " + difficulty);
        
        LeaderboardService.getInstance().submitScore(
            playerId, playerName, difficulty, completionTimeSeconds,
            new LeaderboardService.LeaderboardCallback<LeaderboardService.ScoreEntry>() {
                @Override
                public void onSuccess(LeaderboardService.ScoreEntry result) {
                    scoreSubmitted = true;
                    isSubmitting = false;
                    if (result != null) {
                        playerRank = result.rank;
                        resultMessage = "Skor tersimpan! Peringkat #" + playerRank + " di " + 
                                       GameManager.getInstance().getDifficulty().name();
                    } else {
                        resultMessage = "Skor berhasil disimpan!";
                    }
                    System.out.println("[Leaderboard] Score submitted! Rank: " + playerRank);
                    loadAllLeaderboards();
                }
                
                @Override
                public void onError(String errorMsg) {
                    isSubmitting = false;
                    scoreSubmitted = true;
                    resultMessage = "Gagal mengirim skor: " + errorMsg;
                    System.err.println("[Leaderboard] Submit error: " + errorMsg);
                    loadAllLeaderboards();
                }
            }
        );
    }

    private void loadAllLeaderboards() {
        loadingEasy = true;
        loadingMedium = true;
        loadingHard = true;
        errorMessage = null;
        
        // Load EASY leaderboard
        LeaderboardService.getInstance().getLeaderboard("EASY", MAX_ENTRIES_PER_COLUMN,
            new LeaderboardService.LeaderboardCallback<List<LeaderboardService.ScoreEntry>>() {
                @Override
                public void onSuccess(List<LeaderboardService.ScoreEntry> result) {
                    easyEntries = result != null ? result : new ArrayList<>();
                    loadingEasy = false;
                    System.out.println("[Leaderboard] EASY: " + easyEntries.size() + " entries");
                }
                @Override
                public void onError(String error) {
                    loadingEasy = false;
                    easyEntries.clear();
                    if (errorMessage == null) errorMessage = error;
                }
            });
        
        // Load MEDIUM leaderboard
        LeaderboardService.getInstance().getLeaderboard("MEDIUM", MAX_ENTRIES_PER_COLUMN,
            new LeaderboardService.LeaderboardCallback<List<LeaderboardService.ScoreEntry>>() {
                @Override
                public void onSuccess(List<LeaderboardService.ScoreEntry> result) {
                    mediumEntries = result != null ? result : new ArrayList<>();
                    loadingMedium = false;
                    System.out.println("[Leaderboard] MEDIUM: " + mediumEntries.size() + " entries");
                }
                @Override
                public void onError(String error) {
                    loadingMedium = false;
                    mediumEntries.clear();
                    if (errorMessage == null) errorMessage = error;
                }
            });
        
        // Load HARD leaderboard
        LeaderboardService.getInstance().getLeaderboard("HARD", MAX_ENTRIES_PER_COLUMN,
            new LeaderboardService.LeaderboardCallback<List<LeaderboardService.ScoreEntry>>() {
                @Override
                public void onSuccess(List<LeaderboardService.ScoreEntry> result) {
                    hardEntries = result != null ? result : new ArrayList<>();
                    loadingHard = false;
                    System.out.println("[Leaderboard] HARD: " + hardEntries.size() + " entries");
                }
                @Override
                public void onError(String error) {
                    loadingHard = false;
                    hardEntries.clear();
                    if (errorMessage == null) errorMessage = error;
                }
            });
    }

    private boolean isLoading() {
        return loadingEasy || loadingMedium || loadingHard || isSubmitting;
    }

    @Override
    public void render(float delta) {
        fadeIn = Math.min(1f, fadeIn + delta * 3f);

        handleInput();

        Gdx.gl.glClearColor(0.03f, 0.02f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        
        drawBackground();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float alpha = fadeIn;

        // Draw title
        drawTitle(alpha);

        // Draw player info if after ending
        if (endingType != EndingType.VIEW_ONLY) {
            drawPlayerInfo(alpha);
        }

        // Draw 3 columns
        float columnsStartY = endingType != EndingType.VIEW_ONLY ? VIRTUAL_HEIGHT - 130f : VIRTUAL_HEIGHT - 100f;
        
        if (isLoading()) {
            entryFont.setColor(0.7f, 0.7f, 0.7f, alpha);
            String loadText = isSubmitting ? "Mengirim skor..." : "Memuat leaderboard...";
            glyphLayout.setText(entryFont, loadText);
            entryFont.draw(batch, loadText, (VIRTUAL_WIDTH - glyphLayout.width) / 2f, VIRTUAL_HEIGHT / 2f);
        } else {
            // Draw EASY column
            drawColumn(COLUMN_START_X, columnsStartY, "EASY", easyEntries, 
                      new Color(0.3f, 0.9f, 0.3f, alpha), alpha);
            
            // Draw MEDIUM column
            drawColumn(COLUMN_START_X + COLUMN_WIDTH + COLUMN_GAP, columnsStartY, "MEDIUM", mediumEntries, 
                      new Color(0.9f, 0.9f, 0.3f, alpha), alpha);
            
            // Draw HARD column
            drawColumn(COLUMN_START_X + (COLUMN_WIDTH + COLUMN_GAP) * 2, columnsStartY, "HARD", hardEntries, 
                      new Color(0.9f, 0.3f, 0.3f, alpha), alpha);
        }

        // Draw error message if any
        if (errorMessage != null && !isLoading()) {
            smallFont.setColor(1f, 0.4f, 0.4f, alpha * 0.8f);
            glyphLayout.setText(smallFont, "Backend offline - " + errorMessage);
            smallFont.draw(batch, "Backend offline - " + errorMessage, 
                (VIRTUAL_WIDTH - glyphLayout.width) / 2f, 55f);
        }

        // Draw navigation hint
        smallFont.setColor(0.6f, 0.6f, 0.6f, alpha);
        String navText = "Tekan SPACE/ENTER/ESC untuk kembali ke menu  |  [R] Refresh";
        glyphLayout.setText(smallFont, navText);
        smallFont.draw(batch, navText, (VIRTUAL_WIDTH - glyphLayout.width) / 2f, 30f);

        batch.end();
    }

    private void drawTitle(float alpha) {
        String title;
        Color titleColor;
        
        if (endingType == EndingType.ESCAPE) {
            title = "BERHASIL KABUR!";
            titleColor = new Color(0.2f, 0.95f, 0.2f, alpha);
        } else if (endingType == EndingType.GAME_OVER) {
            title = "GAME OVER";
            titleColor = new Color(0.95f, 0.2f, 0.2f, alpha);
        } else {
            title = "LEADERBOARD";
            titleColor = new Color(0.95f, 0.85f, 0.2f, alpha);
        }
        
        titleFont.setColor(titleColor);
        glyphLayout.setText(titleFont, title);
        titleFont.draw(batch, title, (VIRTUAL_WIDTH - glyphLayout.width) / 2f, VIRTUAL_HEIGHT - 25f);
    }

    private void drawPlayerInfo(float alpha) {
        GameManager gm = GameManager.getInstance();
        String playerName = gm.getPlayerName();
        if (playerName == null || playerName.isEmpty()) playerName = "Unknown";
        
        // Player info line
        String playerInfo = "Pemain: " + playerName + "  |  Waktu: " + formatTime(completionTimeSeconds) + 
                           "  |  Kesulitan: " + gm.getDifficulty().name();
        smallFont.setColor(1f, 1f, 1f, alpha);
        glyphLayout.setText(smallFont, playerInfo);
        smallFont.draw(batch, playerInfo, (VIRTUAL_WIDTH - glyphLayout.width) / 2f, VIRTUAL_HEIGHT - 60f);
        
        // Result message
        if (!resultMessage.isEmpty()) {
            if (resultMessage.contains("Gagal")) {
                smallFont.setColor(1f, 0.4f, 0.4f, alpha);
            } else {
                smallFont.setColor(0.4f, 1f, 0.4f, alpha);
            }
            glyphLayout.setText(smallFont, resultMessage);
            smallFont.draw(batch, resultMessage, (VIRTUAL_WIDTH - glyphLayout.width) / 2f, VIRTUAL_HEIGHT - 80f);
        }
    }

    private void drawColumn(float x, float startY, String difficultyName, 
                           List<LeaderboardService.ScoreEntry> entries, Color headerColor, float alpha) {
        
        // Draw column background
        batch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.08f, 0.06f, 0.1f, 0.7f * alpha);
        float columnHeight = 380f;
        shapeRenderer.rect(x, startY - columnHeight, COLUMN_WIDTH, columnHeight);
        shapeRenderer.end();
        
        // Draw column border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(headerColor.r * 0.5f, headerColor.g * 0.5f, headerColor.b * 0.5f, alpha);
        shapeRenderer.rect(x, startY - columnHeight, COLUMN_WIDTH, columnHeight);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();
        
        // Draw header
        columnHeaderFont.setColor(headerColor);
        glyphLayout.setText(columnHeaderFont, difficultyName);
        columnHeaderFont.draw(batch, difficultyName, x + (COLUMN_WIDTH - glyphLayout.width) / 2f, startY - 10f);
        
        // Draw separator line text
        smallFont.setColor(headerColor.r * 0.7f, headerColor.g * 0.7f, headerColor.b * 0.7f, alpha);
        String headerLine = "─────────────────────";
        glyphLayout.setText(smallFont, headerLine);
        smallFont.draw(batch, headerLine, x + (COLUMN_WIDTH - glyphLayout.width) / 2f, startY - 30f);
        
        // Draw column headers
        float headerY = startY - 50f;
        smallFont.setColor(0.7f, 0.7f, 0.7f, alpha);
        smallFont.draw(batch, "#", x + 10f, headerY);
        smallFont.draw(batch, "Nama", x + 40f, headerY);
        smallFont.draw(batch, "Waktu", x + 180f, headerY);
        
        // Draw entries
        float entryY = headerY - 25f;
        float lineHeight = 28f;
        
        if (entries.isEmpty()) {
            entryFont.setColor(0.5f, 0.5f, 0.5f, alpha);
            glyphLayout.setText(entryFont, "Belum ada");
            entryFont.draw(batch, "Belum ada", x + (COLUMN_WIDTH - glyphLayout.width) / 2f, entryY - 50f);
        } else {
            String currentPlayerId = GameManager.getInstance().getPlayerId();
            boolean currentPlayerInTop10 = false;
            
            for (int i = 0; i < Math.min(entries.size(), MAX_ENTRIES_PER_COLUMN); i++) {
                LeaderboardService.ScoreEntry entry = entries.get(i);
                float y = entryY - (i * lineHeight);
                
                boolean isCurrentPlayer = currentPlayerId != null && 
                                          currentPlayerId.equals(entry.playerId);
                if (isCurrentPlayer) currentPlayerInTop10 = true;
                
                // Set color based on rank
                Color entryColor;
                if (isCurrentPlayer) {
                    entryColor = new Color(1f, 1f, 0.3f, alpha); // Highlight current player
                } else if (i == 0) {
                    entryColor = new Color(1f, 0.85f, 0f, alpha); // Gold
                } else if (i == 1) {
                    entryColor = new Color(0.8f, 0.8f, 0.8f, alpha); // Silver
                } else if (i == 2) {
                    entryColor = new Color(0.8f, 0.5f, 0.2f, alpha); // Bronze
                } else {
                    entryColor = new Color(0.9f, 0.9f, 0.9f, alpha);
                }
                
                entryFont.setColor(entryColor);
                
                // Rank
                String rankText = String.valueOf(entry.rank > 0 ? entry.rank : (i + 1));
                entryFont.draw(batch, rankText, x + 10f, y);
                
                // Name (truncate if needed)
                String name = entry.username;
                if (name == null) name = "???";
                if (name.length() > 10) name = name.substring(0, 9) + "..";
                entryFont.draw(batch, name, x + 40f, y);
                
                // Time
                String time = entry.completionTimeFormatted;
                if (time == null) time = formatTime(entry.completionTimeSeconds);
                entryFont.draw(batch, time, x + 180f, y);
                
                // Indicator for current player
                if (isCurrentPlayer) {
                    entryFont.draw(batch, "◄", x + COLUMN_WIDTH - 25f, y);
                }
            }
            
            // If current player is NOT in top 10 but just submitted a score for this difficulty, show their position
            String currentDifficulty = GameManager.getInstance().getDifficulty().name();
            if (!currentPlayerInTop10 && endingType == EndingType.ESCAPE && playerRank > 0 
                && difficultyName.equals(currentDifficulty)) {
                
                float bottomY = entryY - (MAX_ENTRIES_PER_COLUMN * lineHeight) - 10f;
                
                // Draw separator dots
                smallFont.setColor(0.5f, 0.5f, 0.5f, alpha);
                String dots = "· · ·";
                glyphLayout.setText(smallFont, dots);
                smallFont.draw(batch, dots, x + (COLUMN_WIDTH - glyphLayout.width) / 2f, bottomY);
                
                // Draw current player's position
                bottomY -= lineHeight;
                Color playerColor = new Color(1f, 1f, 0.3f, alpha);
                entryFont.setColor(playerColor);
                
                // Rank
                entryFont.draw(batch, String.valueOf(playerRank), x + 10f, bottomY);
                
                // Name
                String playerName = GameManager.getInstance().getPlayerName();
                if (playerName == null) playerName = "You";
                if (playerName.length() > 10) playerName = playerName.substring(0, 9) + "..";
                entryFont.draw(batch, playerName, x + 40f, bottomY);
                
                // Time
                entryFont.draw(batch, formatTime(completionTimeSeconds), x + 180f, bottomY);
                
                // Indicator
                entryFont.draw(batch, "◄", x + COLUMN_WIDTH - 25f, bottomY);
            }
        }
    }

    private void drawBackground() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Gradient background
        for (int i = 0; i < 12; i++) {
            float shade = 0.02f + (i * 0.003f);
            shapeRenderer.setColor(shade, shade * 0.6f, shade * 0.8f, 1f);
            shapeRenderer.rect(0, i * 50f, VIRTUAL_WIDTH, 50f);
        }
        
        shapeRenderer.end();

        // Decorative border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.3f, 0.2f, 0.4f, fadeIn);
        shapeRenderer.rect(15f, 15f, VIRTUAL_WIDTH - 30f, VIRTUAL_HEIGHT - 30f);
        shapeRenderer.end();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || 
            Gdx.input.isKeyJustPressed(Input.Keys.M) ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            returnToMainMenu();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            loadAllLeaderboards();
        }
    }

    private void returnToMainMenu() {
        if (endingType != EndingType.VIEW_ONLY) {
            resetGameState();
        }
        game.setScreen(new MainMenuScreen(game));
    }

    private void resetGameState() {
        GameManager gm = GameManager.getInstance();
        
        if (gm.getCurrentSession() != null) {
            gm.getCurrentSession().setActive(false);
        }
        
        gm.resetInventory();
        KeyManager.getInstance().reset();
        gm.setCurrentState(GameManager.GameState.MAIN_MENU);
        
        System.out.println("[Leaderboard] Game state reset");
    }

    private String formatTime(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        titleFont.dispose();
        columnHeaderFont.dispose();
        entryFont.dispose();
        smallFont.dispose();
    }
}
