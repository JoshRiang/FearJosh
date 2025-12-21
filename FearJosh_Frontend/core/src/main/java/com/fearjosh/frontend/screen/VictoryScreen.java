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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.core.GameSession;
import com.fearjosh.frontend.network.LeaderboardService;

/**
 * Victory Screen - Ditampilkan saat pemain berhasil kabur dari sekolah.
 * Menampilkan waktu penyelesaian dan submit skor ke leaderboard.
 */
public class VictoryScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;

    private final FearJosh game;
    private SpriteBatch batch;
    private BitmapFont titleFont;
    private BitmapFont textFont;
    private BitmapFont smallFont;
    private OrthographicCamera camera;
    private FitViewport viewport;

    // Game completion data
    private final long completionTimeSeconds;
    private final String completionTimeFormatted;
    private final String difficulty;
    private final String playerId;

    // UI state
    private String playerName = "";
    private boolean nameEntered = false;
    private boolean submitting = false;
    private boolean submitted = false;
    private String submitMessage = "";
    private int playerRank = 0;

    // Animation
    private float fadeIn = 0f;
    private float pulseTimer = 0f;

    // Input handling
    private StringBuilder nameBuilder = new StringBuilder();
    private static final int MAX_NAME_LENGTH = 15;

    public VictoryScreen(FearJosh game, long completionTimeSeconds) {
        this.game = game;
        this.completionTimeSeconds = completionTimeSeconds;
        this.completionTimeFormatted = LeaderboardService.formatTime(completionTimeSeconds);
        this.difficulty = GameManager.getInstance().getDifficulty().name();
        this.playerId = LeaderboardService.generatePlayerId();

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        batch = new SpriteBatch();

        // Create fonts
        titleFont = new BitmapFont();
        titleFont.getData().setScale(3f);
        titleFont.setColor(Color.GREEN);

        textFont = new BitmapFont();
        textFont.getData().setScale(1.5f);
        textFont.setColor(Color.WHITE);

        smallFont = new BitmapFont();
        smallFont.getData().setScale(1.2f);
        smallFont.setColor(Color.LIGHT_GRAY);

        System.out.println("[Victory] Screen created - Time: " + completionTimeFormatted + ", Difficulty: " + difficulty);
    }

    @Override
    public void show() {
        // End the current game session
        GameSession session = GameManager.getInstance().getCurrentSession();
        if (session != null) {
            session.setActive(false);
        }
        GameManager.getInstance().setCurrentState(GameManager.GameState.MAIN_MENU);
    }

    @Override
    public void render(float delta) {
        // Update animations
        fadeIn = Math.min(1f, fadeIn + delta * 2f);
        pulseTimer += delta;

        // Handle input for name entry
        if (!nameEntered) {
            handleNameInput();
        }

        // Clear screen with dark background
        Gdx.gl.glClearColor(0.02f, 0.05f, 0.02f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        // Apply fade in
        float alpha = fadeIn;

        // Draw title with pulse effect
        float pulseScale = 1f + 0.05f * (float) Math.sin(pulseTimer * 3f);
        titleFont.getData().setScale(3f * pulseScale);
        titleFont.setColor(0.2f, 0.9f, 0.3f, alpha);
        
        String title = "YOU ESCAPED!";
        GlyphLayout titleLayout = new GlyphLayout(titleFont, title);
        titleFont.draw(batch, title, 
            (VIRTUAL_WIDTH - titleLayout.width) / 2f, 
            VIRTUAL_HEIGHT - 80f);

        // Reset title font scale
        titleFont.getData().setScale(3f);

        // Draw completion stats
        textFont.setColor(1f, 1f, 1f, alpha);
        
        String timeText = "Completion Time: " + completionTimeFormatted;
        GlyphLayout timeLayout = new GlyphLayout(textFont, timeText);
        textFont.draw(batch, timeText, 
            (VIRTUAL_WIDTH - timeLayout.width) / 2f, 
            VIRTUAL_HEIGHT - 180f);

        String diffText = "Difficulty: " + difficulty;
        GlyphLayout diffLayout = new GlyphLayout(textFont, diffText);
        textFont.draw(batch, diffText, 
            (VIRTUAL_WIDTH - diffLayout.width) / 2f, 
            VIRTUAL_HEIGHT - 220f);

        // Name entry section
        if (!nameEntered) {
            smallFont.setColor(0.8f, 0.8f, 0.8f, alpha);
            String promptText = "Enter your name for the leaderboard:";
            GlyphLayout promptLayout = new GlyphLayout(smallFont, promptText);
            smallFont.draw(batch, promptText, 
                (VIRTUAL_WIDTH - promptLayout.width) / 2f, 
                VIRTUAL_HEIGHT - 300f);

            // Draw name input box
            textFont.setColor(1f, 1f, 0.5f, alpha);
            String displayName = nameBuilder.toString();
            if ((int)(pulseTimer * 2) % 2 == 0) {
                displayName += "_"; // Blinking cursor
            }
            if (displayName.isEmpty()) {
                displayName = "_";
            }
            GlyphLayout nameLayout = new GlyphLayout(textFont, displayName);
            textFont.draw(batch, displayName, 
                (VIRTUAL_WIDTH - nameLayout.width) / 2f, 
                VIRTUAL_HEIGHT - 340f);

            smallFont.setColor(0.6f, 0.6f, 0.6f, alpha);
            String hintText = "Press ENTER to submit";
            GlyphLayout hintLayout = new GlyphLayout(smallFont, hintText);
            smallFont.draw(batch, hintText, 
                (VIRTUAL_WIDTH - hintLayout.width) / 2f, 
                VIRTUAL_HEIGHT - 380f);

        } else if (submitting) {
            // Submitting message
            textFont.setColor(1f, 1f, 0.5f, alpha);
            String submitText = "Submitting score...";
            GlyphLayout submitLayout = new GlyphLayout(textFont, submitText);
            textFont.draw(batch, submitText, 
                (VIRTUAL_WIDTH - submitLayout.width) / 2f, 
                VIRTUAL_HEIGHT - 320f);

        } else if (submitted) {
            // Show result
            if (playerRank > 0) {
                textFont.setColor(0.3f, 1f, 0.3f, alpha);
                String rankText = "Your Rank: #" + playerRank;
                GlyphLayout rankLayout = new GlyphLayout(textFont, rankText);
                textFont.draw(batch, rankText, 
                    (VIRTUAL_WIDTH - rankLayout.width) / 2f, 
                    VIRTUAL_HEIGHT - 300f);
            }

            smallFont.setColor(0.8f, 0.8f, 0.8f, alpha);
            GlyphLayout msgLayout = new GlyphLayout(smallFont, submitMessage);
            smallFont.draw(batch, submitMessage, 
                (VIRTUAL_WIDTH - msgLayout.width) / 2f, 
                VIRTUAL_HEIGHT - 340f);

            // Navigation options
            textFont.setColor(1f, 1f, 1f, alpha);
            String optionsText = "[L] View Leaderboard    [M] Main Menu";
            GlyphLayout optionsLayout = new GlyphLayout(textFont, optionsText);
            textFont.draw(batch, optionsText, 
                (VIRTUAL_WIDTH - optionsLayout.width) / 2f, 
                VIRTUAL_HEIGHT - 420f);
        }

        // Credits/story text at bottom
        smallFont.setColor(0.5f, 0.7f, 0.5f, alpha * 0.8f);
        String storyText = "You escaped the haunted school... but the nightmare lives on.";
        GlyphLayout storyLayout = new GlyphLayout(smallFont, storyText);
        smallFont.draw(batch, storyText, 
            (VIRTUAL_WIDTH - storyLayout.width) / 2f, 
            80f);

        batch.end();

        // Handle navigation after submission
        if (submitted && !submitting) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
                game.setScreen(new LeaderboardScreen(game, difficulty));
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.M) || 
                       Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                game.setScreen(new MainMenuScreen(game));
            }
        }
    }

    private void handleNameInput() {
        // Handle character input
        for (int i = 0; i < 256; i++) {
            if (Gdx.input.isKeyJustPressed(i)) {
                char c = getCharFromKeyCode(i);
                if (c != 0 && nameBuilder.length() < MAX_NAME_LENGTH) {
                    nameBuilder.append(c);
                }
            }
        }

        // Handle backspace
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && nameBuilder.length() > 0) {
            nameBuilder.deleteCharAt(nameBuilder.length() - 1);
        }

        // Handle enter to submit
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (nameBuilder.length() > 0) {
                playerName = nameBuilder.toString().trim();
                nameEntered = true;
                submitScore();
            }
        }

        // Allow skip with ESC (use default name)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            playerName = "Anonymous";
            nameEntered = true;
            submitScore();
        }
    }

    private char getCharFromKeyCode(int keyCode) {
        // Letters
        if (keyCode >= Input.Keys.A && keyCode <= Input.Keys.Z) {
            boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || 
                           Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
            char base = (char) ('a' + (keyCode - Input.Keys.A));
            return shift ? Character.toUpperCase(base) : base;
        }
        
        // Numbers
        if (keyCode >= Input.Keys.NUM_0 && keyCode <= Input.Keys.NUM_9) {
            return (char) ('0' + (keyCode - Input.Keys.NUM_0));
        }
        
        // Space
        if (keyCode == Input.Keys.SPACE) {
            return ' ';
        }
        
        // Underscore (shift + minus)
        if (keyCode == Input.Keys.MINUS && 
            (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))) {
            return '_';
        }

        return 0;
    }

    private void submitScore() {
        submitting = true;
        
        LeaderboardService.getInstance().submitScore(
            playerId, 
            playerName, 
            difficulty, 
            completionTimeSeconds,
            new LeaderboardService.LeaderboardCallback<LeaderboardService.ScoreEntry>() {
                @Override
                public void onSuccess(LeaderboardService.ScoreEntry result) {
                    submitting = false;
                    submitted = true;
                    playerRank = result.rank;
                    submitMessage = "Score submitted successfully!";
                    System.out.println("[Victory] Score submitted! Rank: " + playerRank);
                }

                @Override
                public void onError(String errorMessage) {
                    submitting = false;
                    submitted = true;
                    submitMessage = "Could not submit score (offline mode)";
                    System.err.println("[Victory] Submit error: " + errorMessage);
                }
            }
        );
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
        titleFont.dispose();
        textFont.dispose();
        smallFont.dispose();
    }
}
