package com.fearjosh.frontend.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.network.LeaderboardService;

import java.util.ArrayList;
import java.util.List;

/**
 * Leaderboard Screen - Menampilkan top scores dari backend.
 */
public class LeaderboardScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;

    private final FearJosh game;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont titleFont;
    private BitmapFont headerFont;
    private BitmapFont entryFont;
    private BitmapFont smallFont;
    private OrthographicCamera camera;
    private FitViewport viewport;

    // Leaderboard data
    private List<LeaderboardService.ScoreEntry> entries = new ArrayList<>();
    private boolean loading = true;
    private String errorMessage = null;
    private String currentDifficulty;

    // Difficulty filter tabs
    private static final String[] DIFFICULTY_TABS = {"ALL", "EASY", "MEDIUM", "HARD"};
    private int selectedTab = 0;

    // Animation
    private float fadeIn = 0f;

    public LeaderboardScreen(FearJosh game) {
        this(game, "ALL");
    }

    public LeaderboardScreen(FearJosh game, String initialDifficulty) {
        this.game = game;
        this.currentDifficulty = initialDifficulty;
        
        // Set initial tab based on difficulty
        for (int i = 0; i < DIFFICULTY_TABS.length; i++) {
            if (DIFFICULTY_TABS[i].equalsIgnoreCase(initialDifficulty)) {
                selectedTab = i;
                break;
            }
        }

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Create fonts
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.5f);
        titleFont.setColor(new Color(0.9f, 0.2f, 0.2f, 1f)); // Red horror theme

        headerFont = new BitmapFont();
        headerFont.getData().setScale(1.3f);
        headerFont.setColor(Color.YELLOW);

        entryFont = new BitmapFont();
        entryFont.getData().setScale(1.2f);
        entryFont.setColor(Color.WHITE);

        smallFont = new BitmapFont();
        smallFont.getData().setScale(1f);
        smallFont.setColor(Color.LIGHT_GRAY);

        System.out.println("[Leaderboard] Screen created, initial difficulty: " + currentDifficulty);
    }

    @Override
    public void show() {
        loadLeaderboard();
    }

    private void loadLeaderboard() {
        loading = true;
        errorMessage = null;
        entries.clear();

        String diffFilter = DIFFICULTY_TABS[selectedTab];
        currentDifficulty = diffFilter;

        LeaderboardService.getInstance().getLeaderboard(
            diffFilter.equals("ALL") ? null : diffFilter,
            20, // Top 20
            new LeaderboardService.LeaderboardCallback<List<LeaderboardService.ScoreEntry>>() {
                @Override
                public void onSuccess(List<LeaderboardService.ScoreEntry> result) {
                    entries = result;
                    loading = false;
                    System.out.println("[Leaderboard] Loaded " + result.size() + " entries");
                }

                @Override
                public void onError(String error) {
                    loading = false;
                    errorMessage = "Could not load leaderboard: " + error;
                    System.err.println("[Leaderboard] Error: " + error);
                }
            }
        );
    }

    @Override
    public void render(float delta) {
        // Update animations
        fadeIn = Math.min(1f, fadeIn + delta * 3f);

        // Handle input
        handleInput();

        // Clear screen
        Gdx.gl.glClearColor(0.05f, 0.02f, 0.02f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        
        // Draw background shapes
        drawBackground();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float alpha = fadeIn;

        // Draw title
        titleFont.setColor(0.9f, 0.2f, 0.2f, alpha);
        String title = "LEADERBOARD";
        GlyphLayout titleLayout = new GlyphLayout(titleFont, title);
        titleFont.draw(batch, title, 
            (VIRTUAL_WIDTH - titleLayout.width) / 2f, 
            VIRTUAL_HEIGHT - 30f);

        // Draw difficulty tabs
        drawDifficultyTabs(alpha);

        // Draw leaderboard content
        float startY = VIRTUAL_HEIGHT - 130f;
        float lineHeight = 28f;

        if (loading) {
            entryFont.setColor(0.7f, 0.7f, 0.7f, alpha);
            String loadText = "Loading...";
            GlyphLayout loadLayout = new GlyphLayout(entryFont, loadText);
            entryFont.draw(batch, loadText, 
                (VIRTUAL_WIDTH - loadLayout.width) / 2f, 
                startY);

        } else if (errorMessage != null) {
            entryFont.setColor(1f, 0.3f, 0.3f, alpha);
            GlyphLayout errLayout = new GlyphLayout(entryFont, errorMessage);
            entryFont.draw(batch, errorMessage, 
                (VIRTUAL_WIDTH - errLayout.width) / 2f, 
                startY);
            
            smallFont.setColor(0.6f, 0.6f, 0.6f, alpha);
            String hintText = "Make sure backend is running at localhost:8080";
            GlyphLayout hintLayout = new GlyphLayout(smallFont, hintText);
            smallFont.draw(batch, hintText, 
                (VIRTUAL_WIDTH - hintLayout.width) / 2f, 
                startY - 30f);

        } else if (entries.isEmpty()) {
            entryFont.setColor(0.6f, 0.6f, 0.6f, alpha);
            String emptyText = "No scores yet. Be the first to escape!";
            GlyphLayout emptyLayout = new GlyphLayout(entryFont, emptyText);
            entryFont.draw(batch, emptyText, 
                (VIRTUAL_WIDTH - emptyLayout.width) / 2f, 
                startY);

        } else {
            // Draw header
            headerFont.setColor(0.9f, 0.8f, 0.2f, alpha);
            headerFont.draw(batch, "RANK", 80f, startY);
            headerFont.draw(batch, "PLAYER", 180f, startY);
            headerFont.draw(batch, "TIME", 480f, startY);
            headerFont.draw(batch, "DIFFICULTY", 600f, startY);

            // Draw separator line
            startY -= 10f;

            // Draw entries
            startY -= lineHeight;
            int displayCount = Math.min(entries.size(), 15); // Show max 15 entries
            
            for (int i = 0; i < displayCount; i++) {
                LeaderboardService.ScoreEntry entry = entries.get(i);
                float y = startY - (i * lineHeight);

                // Highlight top 3
                if (i < 3) {
                    Color rankColor;
                    switch (i) {
                        case 0: rankColor = new Color(1f, 0.84f, 0f, alpha); break; // Gold
                        case 1: rankColor = new Color(0.75f, 0.75f, 0.75f, alpha); break; // Silver
                        case 2: rankColor = new Color(0.8f, 0.5f, 0.2f, alpha); break; // Bronze
                        default: rankColor = new Color(1f, 1f, 1f, alpha);
                    }
                    entryFont.setColor(rankColor);
                } else {
                    entryFont.setColor(0.9f, 0.9f, 0.9f, alpha);
                }

                // Rank
                String rankText = "#" + entry.rank;
                entryFont.draw(batch, rankText, 80f, y);

                // Username (truncate if too long)
                String username = entry.username;
                if (username.length() > 12) {
                    username = username.substring(0, 10) + "..";
                }
                entryFont.draw(batch, username, 180f, y);

                // Time
                entryFont.draw(batch, entry.completionTimeFormatted, 480f, y);

                // Difficulty (with color coding)
                Color diffColor;
                switch (entry.difficulty.toUpperCase()) {
                    case "EASY": diffColor = new Color(0.3f, 0.9f, 0.3f, alpha); break;
                    case "MEDIUM": diffColor = new Color(0.9f, 0.9f, 0.3f, alpha); break;
                    case "HARD": diffColor = new Color(0.9f, 0.3f, 0.3f, alpha); break;
                    default: diffColor = new Color(0.7f, 0.7f, 0.7f, alpha);
                }
                entryFont.setColor(diffColor);
                entryFont.draw(batch, entry.difficulty, 600f, y);
            }
        }

        // Draw navigation hint
        smallFont.setColor(0.5f, 0.5f, 0.5f, alpha);
        String navText = "[ESC/M] Main Menu    [LEFT/RIGHT] Change Difficulty    [R] Refresh";
        GlyphLayout navLayout = new GlyphLayout(smallFont, navText);
        smallFont.draw(batch, navText, 
            (VIRTUAL_WIDTH - navLayout.width) / 2f, 
            40f);

        batch.end();
    }

    private void drawBackground() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Dark gradient-like effect with rectangles
        for (int i = 0; i < 10; i++) {
            float shade = 0.02f + (i * 0.005f);
            shapeRenderer.setColor(shade, shade * 0.5f, shade * 0.5f, 1f);
            shapeRenderer.rect(0, i * 60f, VIRTUAL_WIDTH, 60f);
        }

        shapeRenderer.end();

        // Draw decorative border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.4f, 0.1f, 0.1f, fadeIn);
        shapeRenderer.rect(20f, 20f, VIRTUAL_WIDTH - 40f, VIRTUAL_HEIGHT - 40f);
        shapeRenderer.rect(25f, 25f, VIRTUAL_WIDTH - 50f, VIRTUAL_HEIGHT - 50f);
        shapeRenderer.end();
    }

    private void drawDifficultyTabs(float alpha) {
        float tabWidth = 100f;
        float tabHeight = 30f;
        float startX = (VIRTUAL_WIDTH - (DIFFICULTY_TABS.length * tabWidth + (DIFFICULTY_TABS.length - 1) * 10f)) / 2f;
        float tabY = VIRTUAL_HEIGHT - 85f;

        for (int i = 0; i < DIFFICULTY_TABS.length; i++) {
            float x = startX + i * (tabWidth + 10f);
            boolean selected = (i == selectedTab);

            // Tab background color
            if (selected) {
                headerFont.setColor(1f, 1f, 1f, alpha);
            } else {
                headerFont.setColor(0.5f, 0.5f, 0.5f, alpha);
            }

            // Draw tab text with brackets if selected
            String tabText = selected ? "[" + DIFFICULTY_TABS[i] + "]" : DIFFICULTY_TABS[i];
            GlyphLayout tabLayout = new GlyphLayout(headerFont, tabText);
            headerFont.draw(batch, tabText, 
                x + (tabWidth - tabLayout.width) / 2f, 
                tabY);
        }
    }

    private void handleInput() {
        // Back to menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || 
            Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            game.setScreen(new MainMenuScreen(game));
            return;
        }

        // Refresh
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            loadLeaderboard();
            return;
        }

        // Change difficulty tab
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            selectedTab = (selectedTab - 1 + DIFFICULTY_TABS.length) % DIFFICULTY_TABS.length;
            loadLeaderboard();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            selectedTab = (selectedTab + 1) % DIFFICULTY_TABS.length;
            loadLeaderboard();
        }

        // Number keys for quick tab selection
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) { selectedTab = 0; loadLeaderboard(); }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) { selectedTab = 1; loadLeaderboard(); }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) { selectedTab = 2; loadLeaderboard(); }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) { selectedTab = 3; loadLeaderboard(); }
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
        headerFont.dispose();
        entryFont.dispose();
        smallFont.dispose();
    }
}
