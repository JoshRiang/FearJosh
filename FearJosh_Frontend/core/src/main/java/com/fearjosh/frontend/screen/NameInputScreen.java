package com.fearjosh.frontend.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.cutscene.CutsceneManager;
import com.fearjosh.frontend.systems.AudioManager;

import java.util.Random;

/**
 * Screen untuk memasukkan nama dan Player ID sebelum memulai game baru
 */
public class NameInputScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;
    private static final int MAX_NAME_LENGTH = 15;
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_ID_LENGTH = 20;
    private static final int MIN_ID_LENGTH = 4;

    private final FearJosh game;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private FitViewport viewport;
    
    private BitmapFont titleFont;
    private BitmapFont labelFont;
    private BitmapFont inputFont;
    private BitmapFont hintFont;
    private GlyphLayout glyphLayout;
    
    private Texture backgroundTex;
    
    // Input fields
    private StringBuilder playerName;
    private StringBuilder playerId;
    private int activeField = 0; // 0 = name, 1 = player ID
    
    private boolean cursorVisible = true;
    private float cursorTimer = 0f;
    private static final float CURSOR_BLINK_RATE = 0.5f;
    
    private String errorMessage = "";
    private float errorTimer = 0f;
    
    private InputAdapter textInputProcessor;

    public NameInputScreen(FearJosh game) {
        this.game = game;
        
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        glyphLayout = new GlyphLayout();
        
        // Fonts
        titleFont = new BitmapFont();
        titleFont.setColor(Color.WHITE);
        titleFont.getData().setScale(2.2f);
        
        labelFont = new BitmapFont();
        labelFont.setColor(Color.LIGHT_GRAY);
        labelFont.getData().setScale(1.3f);
        
        inputFont = new BitmapFont();
        inputFont.setColor(Color.WHITE);
        inputFont.getData().setScale(1.6f);
        
        hintFont = new BitmapFont();
        hintFont.setColor(Color.GRAY);
        hintFont.getData().setScale(1.0f);
        
        playerName = new StringBuilder();
        playerId = new StringBuilder(generateRandomId());
        
        // Load background
        try {
            backgroundTex = new Texture("UI/main_menu_background.png");
        } catch (Exception e) {
            backgroundTex = null;
        }
        
        setupInputProcessor();
    }
    
    private String generateRandomId() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder("player_");
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    private void setupInputProcessor() {
        textInputProcessor = new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                StringBuilder currentField = activeField == 0 ? playerName : playerId;
                int maxLength = activeField == 0 ? MAX_NAME_LENGTH : MAX_ID_LENGTH;
                
                // Valid characters
                boolean validChar;
                if (activeField == 0) {
                    // Name: letters, digits, space, underscore
                    validChar = Character.isLetterOrDigit(character) || character == ' ' || character == '_';
                } else {
                    // Player ID: letters, digits, underscore (no spaces)
                    validChar = Character.isLetterOrDigit(character) || character == '_';
                }
                
                if (validChar && currentField.length() < maxLength) {
                    currentField.append(character);
                    errorMessage = "";
                    return true;
                }
                return false;
            }
            
            @Override
            public boolean keyDown(int keycode) {
                StringBuilder currentField = activeField == 0 ? playerName : playerId;
                
                if (keycode == Input.Keys.BACKSPACE) {
                    if (currentField.length() > 0) {
                        currentField.deleteCharAt(currentField.length() - 1);
                        errorMessage = "";
                    }
                    return true;
                }
                
                if (keycode == Input.Keys.TAB || keycode == Input.Keys.DOWN || keycode == Input.Keys.UP) {
                    // Switch between fields
                    activeField = (activeField + 1) % 2;
                    return true;
                }
                
                if (keycode == Input.Keys.ENTER) {
                    submitForm();
                    return true;
                }
                
                if (keycode == Input.Keys.ESCAPE) {
                    game.setScreen(new MainMenuScreen(game));
                    return true;
                }
                
                return false;
            }
        };
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(textInputProcessor);
        
        if (!AudioManager.getInstance().isMusicPlaying()) {
            AudioManager.getInstance().playMusic("Audio/Music/main_menu_music.wav", true);
        }
    }

    @Override
    public void render(float delta) {
        // Update cursor blink
        cursorTimer += delta;
        if (cursorTimer >= CURSOR_BLINK_RATE) {
            cursorTimer = 0f;
            cursorVisible = !cursorVisible;
        }
        
        // Update error timer
        if (errorTimer > 0) {
            errorTimer -= delta;
            if (errorTimer <= 0) {
                errorMessage = "";
            }
        }
        
        // Clear screen
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);
        
        batch.begin();
        
        // Background
        if (backgroundTex != null) {
            batch.setColor(0.25f, 0.25f, 0.3f, 1f);
            batch.draw(backgroundTex, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            batch.setColor(Color.WHITE);
        }
        
        // Title
        String title = "PROFIL PEMAIN";
        glyphLayout.setText(titleFont, title);
        float titleX = (VIRTUAL_WIDTH - glyphLayout.width) / 2f;
        titleFont.draw(batch, title, titleX, VIRTUAL_HEIGHT - 50f);
        
        batch.end();
        
        // Draw input fields
        float boxWidth = 450f;
        float boxHeight = 50f;
        float boxX = (VIRTUAL_WIDTH - boxWidth) / 2f;
        
        // === NAME FIELD ===
        float nameBoxY = VIRTUAL_HEIGHT - 160f;
        drawInputField(boxX, nameBoxY, boxWidth, boxHeight, "Nama Pemain:", 
                      playerName.toString(), activeField == 0, 
                      playerName.length() >= MIN_NAME_LENGTH);
        
        // === PLAYER ID FIELD ===
        float idBoxY = VIRTUAL_HEIGHT - 280f;
        drawInputField(boxX, idBoxY, boxWidth, boxHeight, "Player ID (untuk update rekor):", 
                      playerId.toString(), activeField == 1,
                      playerId.length() >= MIN_ID_LENGTH);
        
        batch.begin();
        
        // Hint for Player ID
        hintFont.setColor(new Color(0.6f, 0.6f, 0.7f, 1f));
        String idHint = "* Gunakan ID yang sama untuk mengupdate rekor sebelumnya";
        glyphLayout.setText(hintFont, idHint);
        hintFont.draw(batch, idHint, boxX, idBoxY - 55f);
        
        String idHint2 = "* ID baru akan di-generate otomatis, atau masukkan ID lamamu";
        glyphLayout.setText(hintFont, idHint2);
        hintFont.draw(batch, idHint2, boxX, idBoxY - 75f);
        
        // Navigation hint
        hintFont.setColor(Color.WHITE);
        String navHint = "TAB/↑↓ = Pindah kolom  |  ENTER = Lanjut  |  ESC = Kembali";
        glyphLayout.setText(hintFont, navHint);
        hintFont.draw(batch, navHint, (VIRTUAL_WIDTH - glyphLayout.width) / 2f, 80f);
        
        // Error message
        if (!errorMessage.isEmpty()) {
            hintFont.setColor(Color.RED);
            glyphLayout.setText(hintFont, errorMessage);
            hintFont.draw(batch, errorMessage, (VIRTUAL_WIDTH - glyphLayout.width) / 2f, 50f);
        }
        
        batch.end();
    }
    
    private void drawInputField(float x, float y, float width, float height, 
                                String label, String value, boolean isActive, boolean isValid) {
        batch.begin();
        
        // Label
        labelFont.setColor(isActive ? Color.WHITE : Color.LIGHT_GRAY);
        glyphLayout.setText(labelFont, label);
        labelFont.draw(batch, label, x, y + height + 25f);
        
        batch.end();
        
        // Box background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (isActive) {
            shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 0.95f);
        } else {
            shapeRenderer.setColor(0.1f, 0.1f, 0.12f, 0.85f);
        }
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.end();
        
        // Box border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        if (isActive) {
            shapeRenderer.setColor(0.4f, 0.6f, 0.9f, 1f); // Blue highlight
        } else if (isValid) {
            shapeRenderer.setColor(0.3f, 0.7f, 0.3f, 1f); // Green if valid
        } else {
            shapeRenderer.setColor(0.4f, 0.4f, 0.45f, 1f); // Gray
        }
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        batch.begin();
        
        // Input text
        String displayText = value;
        if (isActive && cursorVisible) {
            displayText += "|";
        }
        
        inputFont.setColor(Color.WHITE);
        glyphLayout.setText(inputFont, displayText);
        float textX = x + 15f;
        float textY = y + (height + glyphLayout.height) / 2f;
        inputFont.draw(batch, displayText, textX, textY);
        
        // Character count
        int maxLen = label.contains("ID") ? MAX_ID_LENGTH : MAX_NAME_LENGTH;
        String charCount = value.length() + "/" + maxLen;
        hintFont.setColor(isValid ? new Color(0.4f, 0.8f, 0.4f, 1f) : new Color(0.6f, 0.6f, 0.6f, 1f));
        glyphLayout.setText(hintFont, charCount);
        hintFont.draw(batch, charCount, x + width - glyphLayout.width - 10f, y + height + 5f);
        
        batch.end();
    }
    
    private void submitForm() {
        String name = playerName.toString().trim();
        String id = playerId.toString().trim();
        
        if (name.length() < MIN_NAME_LENGTH) {
            errorMessage = "Nama minimal " + MIN_NAME_LENGTH + " karakter!";
            errorTimer = 3f;
            activeField = 0;
            return;
        }
        
        if (id.length() < MIN_ID_LENGTH) {
            errorMessage = "Player ID minimal " + MIN_ID_LENGTH + " karakter!";
            errorTimer = 3f;
            activeField = 1;
            return;
        }
        
        // Set player info in GameManager
        GameManager.getInstance().setPlayerInfo(name, id);
        
        System.out.println("[NameInput] Player submitted - Name: " + name + ", ID: " + id);
        
        startNewGame();
    }
    
    private void startNewGame() {
        GameManager.getInstance().startNewGame(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        GameManager.getInstance().setCurrentState(GameManager.GameState.STORY);

        Screen playScreen = new PlayScreen(game);

        if (GameManager.getInstance().isTestingMode()) {
            GameManager.getInstance().setCurrentState(GameManager.GameState.PLAYING);
            GameManager.getInstance().setHasMetJosh(true);
            game.setScreen(playScreen);
            System.out.println("[NameInput] NEW GAME - TESTING MODE: skipping cutscenes");
        } else {
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
            System.out.println("[NameInput] NEW GAME - cutscene chain -> STORY mode");
        }
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
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (titleFont != null) titleFont.dispose();
        if (labelFont != null) labelFont.dispose();
        if (inputFont != null) inputFont.dispose();
        if (hintFont != null) hintFont.dispose();
        if (backgroundTex != null) backgroundTex.dispose();
    }
}
