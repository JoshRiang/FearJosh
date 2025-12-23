package com.fearjosh.frontend.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.cutscene.CutsceneManager;
import com.fearjosh.frontend.difficulty.GameDifficulty;
import com.fearjosh.frontend.systems.AudioManager;
import com.fearjosh.frontend.ui.MenuButton;

/**
 * Main menu screen with PNG-based buttons.
 * Uses MenuButton class for New Game, Resume (if session exists), Settings, Quit.
 */
public class MainMenuScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;

    private final FearJosh game;
    private SpriteBatch batch;
    private OrthographicCamera uiCamera;
    private FitViewport viewport;
    
    private Texture backgroundTex;
    private BitmapFont font; // Fallback font for difficulty label and Resume text
    private GlyphLayout glyphLayout;
    
    // PNG-based menu buttons
    private MenuButton newGameBtn;
    private MenuButton resumeBtn; // Only shown if hasActiveSession
    private MenuButton settingsBtn;
    private MenuButton quitBtn;
    
    private boolean hasSession;
    private boolean isActive = true;

    public MainMenuScreen(FearJosh game) {
        this.game = game;

        uiCamera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, uiCamera);
        batch = new SpriteBatch();
        
        // Fallback font for difficulty label
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
        glyphLayout = new GlyphLayout();

        // Load background image
        try {
            backgroundTex = new Texture("UI/main_menu_background.png");
        } catch (Exception e) {
            System.err.println("[MainMenu] Background image not found, using black background");
            backgroundTex = null;
        }

        // Check session state
        hasSession = GameManager.getInstance().hasActiveSession();
        
        // Create PNG-based buttons
        createButtons();
    }

    private void createButtons() {
        float centerX = VIRTUAL_WIDTH / 2f;
        float startY = VIRTUAL_HEIGHT * 0.55f; // Start from upper area
        
        // Use MenuButton constants for consistent sizing
        float btnW = MenuButton.MENU_BUTTON_WIDTH;
        float btnH = MenuButton.MENU_BUTTON_HEIGHT;
        
        int buttonIndex = 0;
        
        // New Game button
        newGameBtn = new MenuButton(
            "menu/mainmenu_newgame.png",
            null,
            centerX, MenuButton.getStackedY(startY, buttonIndex),
            btnW, btnH
        );
        buttonIndex++;
        
        // Resume button (only if session exists)
        if (hasSession) {
            resumeBtn = new MenuButton(
                "menu/mainmenu_newgame.png", // Reuse newgame as placeholder texture
                null,
                centerX, MenuButton.getStackedY(startY, buttonIndex),
                btnW, btnH
            );
            buttonIndex++;
        }
        
        // Settings button
        settingsBtn = new MenuButton(
            "menu/mainmenu_settings.png",
            null,
            centerX, MenuButton.getStackedY(startY, buttonIndex),
            btnW, btnH
        );
        buttonIndex++;
        
        // Quit button
        quitBtn = new MenuButton(
            "menu/mainmenu_quit.png",
            null,
            centerX, MenuButton.getStackedY(startY, buttonIndex),
            btnW, btnH
        );
    }

    @Override
    public void show() {
        isActive = true;
        GameManager.getInstance().setCurrentState(GameManager.GameState.MAIN_MENU);
        
        // Play main menu music (looping)
        AudioManager.getInstance().playMusic("Audio/Music/main_menu_music.wav", true);
        
        // Play background ambient sound
        AudioManager.getInstance().playMusic("Audio/background_menu.wav", true);
    }

    @Override
    public void render(float delta) {
        if (!GameManager.getInstance().isInMenu()) return;
        if (!isActive) return;

        // Clear screen
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update camera
        uiCamera.update();
        batch.setProjectionMatrix(uiCamera.combined);

        // Handle input
        handleInput();

        // Update buttons
        float mouseX = getMouseX();
        float mouseY = getMouseY();
        
        newGameBtn.update(mouseX, mouseY);
        if (resumeBtn != null) resumeBtn.update(mouseX, mouseY);
        settingsBtn.update(mouseX, mouseY);
        quitBtn.update(mouseX, mouseY);

        // Render
        batch.begin();
        
        // Draw background
        if (backgroundTex != null) {
            batch.draw(backgroundTex, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        }
        
        // Draw buttons
        newGameBtn.render(batch);
        if (resumeBtn != null) {
            // Resume uses text overlay since no PNG exists
            resumeBtn.render(batch);
            // Draw "LANJUT" text on top
            font.setColor(Color.WHITE);
            glyphLayout.setText(font, "LANJUT");
            float textX = resumeBtn.getBounds().x + (resumeBtn.getBounds().width - glyphLayout.width) / 2f;
            float textY = resumeBtn.getBounds().y + (resumeBtn.getBounds().height + glyphLayout.height) / 2f;
            font.draw(batch, "LANJUT", textX, textY);
        }
        settingsBtn.render(batch);
        quitBtn.render(batch);
        
        // Draw difficulty label at BOTTOM-LEFT corner (fixed position)
        GameDifficulty diff = GameManager.getInstance().getDifficulty();
        String diffName = diff.name();
        String diffDisplay = diffName.equals("EASY") ? "Mudah" : diffName.equals("NORMAL") ? "Normal" : "Sulit";
        String diffText = "Kesulitan: " + diffDisplay;
        font.setColor(new Color(0.9f, 0.9f, 0.95f, 1f));
        float diffMargin = 12f;
        float diffX = diffMargin; // LEFT edge with margin
        float diffY = diffMargin + font.getCapHeight(); // BOTTOM edge with safe margin
        font.draw(batch, diffText, diffX, diffY);
        
        // Draw testing mode indicator (bottom-right corner) - ALWAYS VISIBLE
        if (GameManager.getInstance().isTestingMode()) {
            font.setColor(Color.YELLOW);
            String testingText = "[MODE UJI AKTIF] Tekan T untuk ganti";
            glyphLayout.setText(font, testingText);
            font.draw(batch, testingText, VIRTUAL_WIDTH - glyphLayout.width - 20f, 30f);
        } else {
            font.setColor(new Color(0.5f, 0.5f, 0.5f, 0.8f));
            String testingText = "Tekan T untuk Mode Uji";
            glyphLayout.setText(font, testingText);
            font.draw(batch, testingText, VIRTUAL_WIDTH - glyphLayout.width - 20f, 30f);
        }
        
        batch.end();

        // Handle button clicks
        if (Gdx.input.justTouched()) {
            handleClicks(mouseX, mouseY);
        }
        
        // Handle keyboard shortcuts
        handleKeyboard();
    }
    
    private float getMouseX() {
        return viewport.unproject(new com.badlogic.gdx.math.Vector2(Gdx.input.getX(), Gdx.input.getY())).x;
    }
    
    private float getMouseY() {
        return viewport.unproject(new com.badlogic.gdx.math.Vector2(Gdx.input.getX(), Gdx.input.getY())).y;
    }
    
    private void handleInput() {
        // Toggle testing mode with T key
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            GameManager.getInstance().setTestingMode(!GameManager.getInstance().isTestingMode());
        }
    }
    
    private void handleClicks(float mouseX, float mouseY) {
        if (!isActive) return;
        
        // New Game
        if (newGameBtn.isClicked(mouseX, mouseY)) {
            isActive = false;
            startNewGame();
            return;
        }
        
        // Resume
        if (resumeBtn != null && resumeBtn.isClicked(mouseX, mouseY)) {
            isActive = false;
            resumeGame();
            return;
        }
        
        // Settings
        if (settingsBtn.isClicked(mouseX, mouseY)) {
            isActive = false;
            game.setScreen(new SettingsScreen(game));
            return;
        }
        
        // Quit
        if (quitBtn.isClicked(mouseX, mouseY)) {
            Gdx.app.exit();
        }
    }
    
    private void handleKeyboard() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
        
        // Enter to start new game
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (!isActive) return;
            isActive = false;
            startNewGame();
        }
    }
    
    private void startNewGame() {
        // NEW GAME - creates fresh session
        GameManager.getInstance().startNewGame(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        GameManager.getInstance().setCurrentState(GameManager.GameState.STORY);

        Screen playScreen = new PlayScreen(game);

        // If testing mode, skip cutscenes
        if (GameManager.getInstance().isTestingMode()) {
            GameManager.getInstance().setCurrentState(GameManager.GameState.PLAYING);
            GameManager.getInstance().setHasMetJosh(true);
            game.setScreen(playScreen);
            System.out.println("[MainMenu] NEW GAME - TESTING MODE: skipping cutscenes");
        } else {
            // Chain cutscenes: 0_1 -> 0_2 -> ... -> 0_9 -> game
            Screen cutscene9 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_9", playScreen);
            Screen cutscene8 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_8", cutscene9);
            Screen cutscene7 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_7", cutscene8);
            Screen cutscene6 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_6", cutscene7);
            Screen cutscene5 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_5", cutscene6);
            Screen cutscene4 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_4", cutscene5);
            Screen cutscene3 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_3", cutscene4);
            Screen cutscene2 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_2", cutscene3);
            Screen cutscene1 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_1", cutscene2);
            Screen blackTransition = new BlackTransitionScreen(game, cutscene1, 8f,
                    "Audio/Music/sad_cutscene_piano_violin.wav");
            game.setScreen(blackTransition);
            System.out.println("[MainMenu] NEW GAME - cutscene chain -> STORY mode");
        }
    }
    
    private void resumeGame() {
        GameManager gm = GameManager.getInstance();
        gm.resumeSession();
        gm.setCurrentState(GameManager.GameState.PLAYING);
        game.setScreen(new PlayScreen(game));
        System.out.println("[MainMenu] RESUME - continuing existing session");
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
    public void hide() {
        isActive = false;
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (backgroundTex != null) backgroundTex.dispose();
        if (newGameBtn != null) newGameBtn.dispose();
        if (resumeBtn != null) resumeBtn.dispose();
        if (settingsBtn != null) settingsBtn.dispose();
        if (quitBtn != null) quitBtn.dispose();
    }
}
