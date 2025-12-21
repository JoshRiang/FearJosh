package com.fearjosh.frontend.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.cutscene.CutsceneManager;
import com.fearjosh.frontend.difficulty.GameDifficulty;
import com.fearjosh.frontend.systems.AudioManager;

/**
 * Simple main menu screen with Play, Settings, and Quit.
 */
public class MainMenuScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;

    private final FearJosh game;
    private Stage stage;
    private FitViewport viewport;
    private OrthographicCamera uiCamera;

    private Skin skin;
    private BitmapFont font;

    private Texture buttonTex;
    private Texture buttonTexOver;
    private Texture buttonTexDown;
    private Texture cardTex;
    private Texture backgroundTex;
    private com.badlogic.gdx.graphics.g2d.SpriteBatch batch;
    private boolean isActive = true;

    public MainMenuScreen(FearJosh game) {
        this.game = game;

        uiCamera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, uiCamera);
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f); // Slightly larger text

        // Load background image
        batch = new com.badlogic.gdx.graphics.g2d.SpriteBatch();
        try {
            backgroundTex = new Texture("UI/main_menu_background.jpg");
        } catch (Exception e) {
            System.err.println("[MainMenu] Background image not found, using black background");
            backgroundTex = null;
        }

        // Create button textures with red borders (horror theme)
        buttonTex = createRoundedTextureWithBorder(320, 56, 14,
                new Color(0.15f, 0.15f, 0.18f, 0.95f), // Dark gray background
                new Color(0.8f, 0.1f, 0.1f, 1f), 2); // Red border
        buttonTexOver = createRoundedTextureWithBorder(320, 56, 14,
                new Color(0.2f, 0.2f, 0.23f, 0.95f), // Lighter on hover
                new Color(1f, 0.2f, 0.2f, 1f), 3); // Brighter red border
        buttonTexDown = createRoundedTextureWithBorder(320, 56, 14,
                new Color(0.1f, 0.1f, 0.13f, 0.95f), // Darker when pressed
                new Color(0.6f, 0.05f, 0.05f, 1f), 2); // Dark red border

        skin = new Skin();
        skin.add("default-font", font);
        skin.add("button-up", buttonTex);
        skin.add("button-down", buttonTexDown);

        TextButton.TextButtonStyle tbs = new TextButton.TextButtonStyle();
        tbs.font = font;
        tbs.fontColor = new Color(0.95f, 0.95f, 0.96f, 1f);
        tbs.overFontColor = Color.WHITE;
        tbs.up = new NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(buttonTex, 12, 12, 12, 12));
        tbs.over = new NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(buttonTexOver, 12, 12, 12, 12));
        tbs.down = new NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(buttonTexDown, 12, 12, 12, 12));
        skin.add("default", tbs);

        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = font;
        ls.fontColor = Color.WHITE;
        skin.add("default", ls);

        CheckBox.CheckBoxStyle checkStyle = new CheckBox.CheckBoxStyle();
        checkStyle.font = font;
        checkStyle.fontColor = Color.WHITE;
        Pixmap checkboxPixmap = new Pixmap(24, 24, Pixmap.Format.RGBA8888);
        checkboxPixmap.setColor(Color.DARK_GRAY);
        checkboxPixmap.fillRectangle(0, 0, 24, 24);
        checkStyle.checkboxOff = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new Texture(checkboxPixmap));
        checkboxPixmap.setColor(Color.GREEN);
        checkboxPixmap.fillRectangle(4, 4, 16, 16);
        checkStyle.checkboxOn = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new Texture(checkboxPixmap));
        checkboxPixmap.dispose();
        skin.add("default", checkStyle);

        buildUI();
    }

    private void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Center layout - no card background (transparent, background image shows
        // through)
        root.center().padTop(120f); // Move buttons down

        Table menuTable = new Table();
        menuTable.defaults().pad(6f);
        // No background - let the background image show through

        // Title and subtitle are part of the background image, so we skip them
        // Or you can add them if you want text overlay

        // Difficulty status label
        GameDifficulty diff = GameManager.getInstance().getDifficulty();
        String diffText = "Difficulty: " + diff.name().substring(0, 1).toUpperCase()
                + diff.name().substring(1).toLowerCase();
        Label difficultyLabel = new Label(diffText, skin);
        difficultyLabel.setColor(new Color(0.9f, 0.9f, 0.95f, 1f));

        // Determine session state
        boolean hasSession = GameManager.getInstance().hasActiveSession();

        TextButton playBtn = new TextButton(hasSession ? "New Game" : "New Game", skin);
        TextButton resumeBtn = hasSession ? new TextButton("Resume", skin) : null;
        TextButton settingsBtn = new TextButton("Settings", skin);
        TextButton quitBtn = new TextButton("Quit", skin);

        float btnWidth = 360f;
        float btnHeight = 60f;

        // Add elements with spacing
        menuTable.add(playBtn).width(btnWidth).height(btnHeight).pad(8f).row();
        if (resumeBtn != null) {
            menuTable.add(resumeBtn).width(btnWidth).height(btnHeight).pad(8f).row();
        }
        menuTable.add(settingsBtn).width(btnWidth).height(btnHeight).pad(8f).row();
        menuTable.add(quitBtn).width(btnWidth).height(btnHeight).pad(8f).row();
        menuTable.add(difficultyLabel).padTop(24f).row();

        root.add(menuTable);

        CheckBox testingCheckbox = new CheckBox(" Testing", skin);
        testingCheckbox.setChecked(GameManager.getInstance().isTestingMode());
        testingCheckbox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameManager.getInstance().setTestingMode(testingCheckbox.isChecked());
            }
        });

        Table bottomRightTable = new Table();
        bottomRightTable.setFillParent(true);
        bottomRightTable.bottom().right();
        bottomRightTable.add(testingCheckbox).pad(20f);
        stage.addActor(bottomRightTable);

        playBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // STATE CHECK: hanya proses jika state == MAIN_MENU
                if (!GameManager.getInstance().isInMenu())
                    return;
                if (!isActive)
                    return;
                isActive = false;

                // NEW GAME - creates fresh session
                GameManager.getInstance().startNewGame(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
                GameManager.getInstance().setCurrentState(GameManager.GameState.PLAYING);

                // Chain cutscenes: 0_1 -> 0_2 -> 0_3 -> 0_4 -> 0_5 -> 0_6 -> 0_7 -> 0_8 -> 0_9
                // -> game
                // Screen playScreen = new PlayScreen(game);
                // Screen cutscene9 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_9", playScreen);
                // Screen cutscene8 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_8", cutscene9);
                // Screen cutscene7 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_7", cutscene8);
                // Screen cutscene6 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_6", cutscene7);
                // Screen cutscene5 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_5", cutscene6);
                // Screen cutscene4 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_4", cutscene5);
                // Screen cutscene3 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_3", cutscene4);
                // Screen cutscene2 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_2", cutscene3);
                // Screen cutscene1 = CutsceneManager.getInstance().createCutsceneScreen(game, "0_1", cutscene2);
                // // Black screen with cutscene 0_1 music
                // Screen blackTransition = new BlackTransitionScreen(game, cutscene1, 8f,
                //         "Audio/Music/sad_cutscene_piano_violin.wav");
                // game.setScreen(blackTransition);

                System.out.println("[MainMenu] NEW GAME clicked - cutscene chain: 0_1 -> 0_9 -> game");
            }
        });

        if (resumeBtn != null) {
            resumeBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    // STATE CHECK: hanya proses jika state == MAIN_MENU
                    if (!GameManager.getInstance().isInMenu())
                        return;
                    if (!isActive)
                        return;
                    isActive = false;

                    // RESUME - restore existing session WITHOUT reset
                    GameManager gm = GameManager.getInstance();
                    gm.resumeSession(); // Restore progress
                    gm.setCurrentState(GameManager.GameState.PLAYING);
                    game.setScreen(new PlayScreen(game));
                    System.out.println("[MainMenu] RESUME clicked - continuing existing session");
                }
            });
        }

        settingsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // STATE CHECK: hanya proses jika state == MAIN_MENU
                if (!GameManager.getInstance().isInMenu())
                    return;
                if (!isActive)
                    return;
                isActive = false;

                // Settings tidak ubah state (tetap MAIN_MENU)
                game.setScreen(new SettingsScreen(game));
            }
        });

        quitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // STATE CHECK: hanya proses jika state == MAIN_MENU
                if (!GameManager.getInstance().isInMenu())
                    return;
                if (!isActive)
                    return;
                Gdx.app.exit();
            }
        });

        // Keyboard navigation: Up/Down to change focus, Enter to press, Esc to quit
        stage.setKeyboardFocus(playBtn);
        stage.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            private final TextButton[] buttons = new TextButton[] { playBtn, settingsBtn, quitBtn };
            private int idx = 0;

            @Override
            public boolean keyDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, int keycode) {
                if (keycode == com.badlogic.gdx.Input.Keys.DOWN) {
                    idx = (idx + 1) % buttons.length;
                    stage.setKeyboardFocus(buttons[idx]);
                    return true;
                } else if (keycode == com.badlogic.gdx.Input.Keys.UP) {
                    idx = (idx - 1 + buttons.length) % buttons.length;
                    stage.setKeyboardFocus(buttons[idx]);
                    return true;
                } else if (keycode == com.badlogic.gdx.Input.Keys.ENTER
                        || keycode == com.badlogic.gdx.Input.Keys.SPACE) {
                    // simulate click
                    TextButton b = buttons[idx];
                    b.toggle();
                    b.fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
                    return true;
                } else if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
                    Gdx.app.exit();
                    return true;
                }
                return false;
            }
        });
    }

    private Texture createRoundedTexture(int w, int h, int radius, Color color) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        pm.setColor(0, 0, 0, 0);
        pm.fill();
        pm.setColor(color);
        // Draw rounded rect manually
        // center rects
        pm.fillRectangle(radius, 0, w - 2 * radius, h);
        pm.fillRectangle(0, radius, w, h - 2 * radius);
        // corners
        fillCircle(pm, radius, radius, radius, color);
        fillCircle(pm, w - radius - 1, radius, radius, color);
        fillCircle(pm, radius, h - radius - 1, radius, color);
        fillCircle(pm, w - radius - 1, h - radius - 1, radius, color);

        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return t;
    }

    private void fillCircle(Pixmap pm, int cx, int cy, int r, Color color) {
        pm.setColor(color);
        pm.fillCircle(cx, cy, r);
    }

    /**
     * Create rounded texture with colored border (horror theme)
     */
    private Texture createRoundedTextureWithBorder(int w, int h, int radius, Color bgColor, Color borderColor,
            int borderWidth) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        pm.setColor(0, 0, 0, 0);
        pm.fill();

        // Draw border first (slightly larger radius)
        pm.setColor(borderColor);
        pm.fillRectangle(radius, 0, w - radius * 2, h);
        pm.fillRectangle(0, radius, w, h - radius * 2);
        for (int i = 0; i < 4; i++) {
            int cx = (i % 2 == 0) ? radius : w - radius;
            int cy = (i < 2) ? radius : h - radius;
            fillCircle(pm, cx, cy, radius, borderColor);
        }

        // Draw background on top (smaller to show border)
        int innerRadius = radius - borderWidth;
        pm.setColor(bgColor);
        pm.fillRectangle(radius, borderWidth, w - radius * 2, h - borderWidth * 2);
        pm.fillRectangle(borderWidth, radius, w - borderWidth * 2, h - radius * 2);
        for (int i = 0; i < 4; i++) {
            int cx = (i % 2 == 0) ? radius : w - radius;
            int cy = (i < 2) ? radius : h - radius;
            fillCircle(pm, cx, cy, innerRadius, bgColor);
        }

        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    @Override
    public void show() {
        isActive = true;
        // SET STATE ke MAIN_MENU saat screen ditampilkan
        GameManager.getInstance().setCurrentState(GameManager.GameState.MAIN_MENU);
        Gdx.input.setInputProcessor(stage);

        // Play main menu music (looping)
        AudioManager.getInstance().playMusic("Audio/Music/main_menu_music.wav", true);

        // Play background ambient sound (fire and glitch effects)
        AudioManager.getInstance().playMusic("Audio/background_menu.wav", true);
    }

    @Override
    public void render(float delta) {
        // DOUBLE CHECK: hanya render jika state == MAIN_MENU
        if (!GameManager.getInstance().isInMenu())
            return;
        if (!isActive)
            return;

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw background image if available
        if (backgroundTex != null) {
            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            batch.draw(backgroundTex, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            batch.end();
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        // CRITICAL: Remove InputProcessor when leaving menu
        // This prevents invisible menu from handling clicks
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
        isActive = false;
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        font.dispose();
        buttonTex.dispose();
        buttonTexOver.dispose();
        buttonTexDown.dispose();
        if (cardTex != null) {
            cardTex.dispose();
        }
        if (backgroundTex != null) {
            backgroundTex.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
    }
}
