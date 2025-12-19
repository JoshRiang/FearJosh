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
import com.fearjosh.frontend.difficulty.GameDifficulty;

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
    private boolean isActive = true;

    public MainMenuScreen(FearJosh game) {
        this.game = game;

        uiCamera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, uiCamera);
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        font = new BitmapFont();
        font.setColor(Color.WHITE);

        // Create rounded textures for a more polished look (inspired by Apple HIG)
        buttonTex = createRoundedTexture(320, 56, 14,
                new Color(0.2f, 0.2f, 0.23f, 1f));
        buttonTexOver = createRoundedTexture(320, 56, 14,
                new Color(0.24f, 0.24f, 0.27f, 1f));
        buttonTexDown = createRoundedTexture(320, 56, 14,
                new Color(0.16f, 0.16f, 0.19f, 1f));
        cardTex = createRoundedTexture(600, 360, 18,
                new Color(0.08f, 0.08f, 0.1f, 0.85f));

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
        checkStyle.checkboxOff = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new Texture(checkboxPixmap));
        checkboxPixmap.setColor(Color.GREEN);
        checkboxPixmap.fillRectangle(4, 4, 16, 16);
        checkStyle.checkboxOn = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new Texture(checkboxPixmap));
        checkboxPixmap.dispose();
        skin.add("default", checkStyle);

        buildUI();
    }

    private void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Background subtle dark overlay
        root.center();

        // Card container to reflect Apple HIG cleanliness
        Table card = new Table();
        card.defaults().pad(6f);
        card.setBackground(new NinePatchDrawable(new com.badlogic.gdx.graphics.g2d.NinePatch(cardTex, 18, 18, 18, 18)));

        Label title = new Label("FearJosh", skin);
        title.setColor(new Color(1f, 1f, 1f, 1f));

        Label subtitle = new Label("A stealth-horror experience", skin);
        subtitle.setColor(new Color(0.85f, 0.85f, 0.9f, 1f));

        // Difficulty status label
        GameDifficulty diff = GameManager.getInstance().getDifficulty();
        String diffText = "Difficulty: " + diff.name().substring(0, 1).toUpperCase()
                + diff.name().substring(1).toLowerCase();
        Label difficultyLabel = new Label(diffText, skin);
        difficultyLabel.setColor(new Color(0.8f, 0.8f, 0.85f, 1f));

        // Determine session state
        boolean hasSession = GameManager.getInstance().hasActiveSession();

        TextButton playBtn = new TextButton(hasSession ? "New Game" : "New Game", skin);
        TextButton resumeBtn = hasSession ? new TextButton("Resume", skin) : null;
        TextButton settingsBtn = new TextButton("Settings", skin);
        TextButton quitBtn = new TextButton("Quit", skin);

        float btnWidth = 320f;
        float btnHeight = 56f;

        card.add(title).padTop(18f).padBottom(6f).row();
        card.add(subtitle).padBottom(6f).row();
        card.add(difficultyLabel).padBottom(18f).row();
        card.add(playBtn).width(btnWidth).height(btnHeight).pad(6f).row();
        if (resumeBtn != null) {
            card.add(resumeBtn).width(btnWidth).height(btnHeight).pad(6f).row();
        }
        card.add(settingsBtn).width(btnWidth).height(btnHeight).pad(6f).row();
        card.add(quitBtn).width(btnWidth).height(btnHeight).padBottom(18f).row();

        root.add(card).width(600f).height(360f);
        
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
                if (!GameManager.getInstance().isInMenu()) return;
                if (!isActive) return;
                isActive = false;
                
                GameManager.getInstance().resetNewGame(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
                GameManager.getInstance().setCurrentState(GameManager.GameState.PLAYING);
                game.setScreen(new PlayScreen(game));
            }
        });

        if (resumeBtn != null) {
            resumeBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    // STATE CHECK: hanya proses jika state == MAIN_MENU
                    if (!GameManager.getInstance().isInMenu()) return;
                    if (!isActive) return;
                    isActive = false;
                    
                    GameManager gm = GameManager.getInstance();
                    gm.setCurrentState(GameManager.GameState.PLAYING);
                    game.setScreen(new PlayScreen(game));
                }
            });
        }

        settingsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // STATE CHECK: hanya proses jika state == MAIN_MENU
                if (!GameManager.getInstance().isInMenu()) return;
                if (!isActive) return;
                isActive = false;
                
                // Settings tidak ubah state (tetap MAIN_MENU)
                game.setScreen(new SettingsScreen(game));
            }
        });

        quitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // STATE CHECK: hanya proses jika state == MAIN_MENU
                if (!GameManager.getInstance().isInMenu()) return;
                if (!isActive) return;
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

    @Override
    public void show() {
        isActive = true;
        // SET STATE ke MAIN_MENU saat screen ditampilkan
        GameManager.getInstance().setCurrentState(GameManager.GameState.MAIN_MENU);
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        // DOUBLE CHECK: hanya render jika state == MAIN_MENU
        if (!GameManager.getInstance().isInMenu()) return;
        if (!isActive) return;
        
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        font.dispose();
        buttonTex.dispose();
        buttonTexOver.dispose();
        buttonTexDown.dispose();
        cardTex.dispose();
    }
}
