package com.fearjosh.frontend.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.manager.GameDifficulty;
import com.fearjosh.frontend.manager.GameManager;

/**
 * Minimal placeholder Settings screen with a Back button.
 */
public class SettingsScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;

    private final Stage stage;
    private final FitViewport viewport;
    private final OrthographicCamera uiCamera;
    private final Skin skin;

    // Generated UI textures to dispose
    private Texture cardTex;
    private Texture pillUpTex;
    private Texture pillOverTex;
    private Texture pillDownTex;
    private Texture pillCheckedTex;

    public SettingsScreen(FearJosh game) {
        final FearJosh app = game;
        uiCamera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, uiCamera);
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        // Reuse Skin from MainMenu if available; otherwise create a basic one
        // For simplicity, create a temporary Skin using Scene2D default font
        skin = new Skin();
        skin.add("default-font", new com.badlogic.gdx.graphics.g2d.BitmapFont());
        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle ls = new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle();
        ls.font = (com.badlogic.gdx.graphics.g2d.BitmapFont) skin.get("default-font",
                com.badlogic.gdx.graphics.g2d.BitmapFont.class);
        skin.add("default", ls);
        com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle tbs = new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle();
        tbs.font = (com.badlogic.gdx.graphics.g2d.BitmapFont) skin.get("default-font",
                com.badlogic.gdx.graphics.g2d.BitmapFont.class);
        skin.add("default", tbs);

        Table root = new Table();
        root.setFillParent(true);
        root.center();
        stage.addActor(root);

        // Create Apple-style rounded card background and pill button styles
        NinePatchDrawable cardBg = createRoundedCardDrawable(new Color(0.13f, 0.13f, 0.13f, 0.88f), 18);
        com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle pillStyle = createPillButtonStyle();

        Label title = new Label("Settings", skin);
        title.setColor(Color.WHITE);
        title.setFontScale(1.2f);
        Label diffLabel = new Label("Difficulty:", skin);
        diffLabel.setColor(new Color(1f, 1f, 1f, 0.85f));
        diffLabel.setFontScale(1.0f);
        TextButton easyBtn = new TextButton("Easy", pillStyle);
        TextButton mediumBtn = new TextButton("Medium", pillStyle);
        TextButton hardBtn = new TextButton("Hard", pillStyle);
        TextButton back = new TextButton("Back", createPillButtonStyle());

        // Enable transform and center origin for click animation effects
        setupButtonTransform(easyBtn);
        setupButtonTransform(mediumBtn);
        setupButtonTransform(hardBtn);

        // Card container with background
        Table card = new Table();
        card.setBackground(cardBg);
        card.pad(24f);
        root.add(card).width(560f).height(360f).center().row();

        card.add(title).padBottom(22f).row();
        card.add(diffLabel).padBottom(12f).row();
        Table diffRow = new Table();
        diffRow.center();
        diffRow.add(easyBtn).width(160f).height(44f).pad(8f);
        diffRow.add(mediumBtn).width(160f).height(44f).pad(8f);
        diffRow.add(hardBtn).width(160f).height(44f).pad(8f);
        card.add(diffRow).padBottom(26f).row();
        card.add(back).width(220f).height(46f).padTop(8f).row();
        // Single-select button group for difficulty
        ButtonGroup<TextButton> group = new ButtonGroup<>(easyBtn, mediumBtn, hardBtn);
        group.setMaxCheckCount(1);
        group.setMinCheckCount(1);
        group.setUncheckLast(true);

        // Initialize selection based on current difficulty
        GameDifficulty current = GameManager.getInstance().getDifficulty();
        setCheckedForDifficulty(current, easyBtn, mediumBtn, hardBtn);

        easyBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameManager.getInstance().setDifficulty(GameDifficulty.EASY);
                setCheckedForDifficulty(GameDifficulty.EASY, easyBtn, mediumBtn, hardBtn);
                applyClickEffect(easyBtn);
            }
        });

        mediumBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameManager.getInstance().setDifficulty(GameDifficulty.MEDIUM);
                setCheckedForDifficulty(GameDifficulty.MEDIUM, easyBtn, mediumBtn, hardBtn);
                applyClickEffect(mediumBtn);
            }
        });

        hardBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameManager.getInstance().setDifficulty(GameDifficulty.HARD);
                setCheckedForDifficulty(GameDifficulty.HARD, easyBtn, mediumBtn, hardBtn);
                applyClickEffect(hardBtn);
            }
        });

        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                app.setScreen(new MainMenuScreen(app));
            }
        });
    }

    private void setupButtonTransform(TextButton b) {
        b.setTransform(true);
        b.setOrigin(Align.center);
    }

    private void applyClickEffect(final TextButton b) {
        // Subtle scale pulse; color handled by button style states
        b.clearActions();
        b.addAction(Actions.sequence(
                Actions.scaleTo(1.05f, 1.05f, 0.10f),
                Actions.scaleTo(1f, 1f, 0.10f)));
    }

    private void setCheckedForDifficulty(GameDifficulty diff, TextButton easy, TextButton medium, TextButton hard) {
        easy.setChecked(false);
        medium.setChecked(false);
        hard.setChecked(false);
        switch (diff) {
            case EASY:
                easy.setChecked(true);
                break;
            case MEDIUM:
                medium.setChecked(true);
                break;
            case HARD:
                hard.setChecked(true);
                break;
        }
    }

    private NinePatchDrawable createRoundedCardDrawable(Color color, int radius) {
        // Base pixmap used for scalable nine-patch card background
        int size = 64;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.SourceOver);
        pm.setColor(color);
        drawRoundedRect(pm, 0, 0, size, size, radius);
        cardTex = new Texture(pm);
        pm.dispose();
        NinePatch nine = new NinePatch(cardTex, radius, radius, radius, radius);
        return new NinePatchDrawable(nine);
    }

    private com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle createPillButtonStyle() {
        // Pill drawables for up/over/down/checked
        int w = 220, h = 48, r = h / 2;
        pillUpTex = createRoundedTexture(w, h, r, new Color(1f, 1f, 1f, 0.06f));
        pillOverTex = createRoundedTexture(w, h, r, new Color(1f, 1f, 1f, 0.12f));
        pillDownTex = createRoundedTexture(w, h, r, new Color(1f, 1f, 1f, 0.18f));
        pillCheckedTex = createRoundedTexture(w, h, r, new Color(0.04f, 0.52f, 1f, 0.92f)); // iOS blue

        com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle tbs = new com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle();
        tbs.font = (com.badlogic.gdx.graphics.g2d.BitmapFont) skin.get("default-font",
                com.badlogic.gdx.graphics.g2d.BitmapFont.class);
        tbs.fontColor = Color.WHITE;
        tbs.up = new NinePatchDrawable(new NinePatch(pillUpTex, r, r, r, r));
        tbs.over = new NinePatchDrawable(new NinePatch(pillOverTex, r, r, r, r));
        tbs.down = new NinePatchDrawable(new NinePatch(pillDownTex, r, r, r, r));
        tbs.checked = new NinePatchDrawable(new NinePatch(pillCheckedTex, r, r, r, r));
        tbs.checkedFontColor = Color.WHITE;
        return tbs;
    }

    private Texture createRoundedTexture(int w, int h, int radius, Color color) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        drawRoundedRect(pm, 0, 0, w, h, radius);
        Texture tex = new Texture(pm);
        pm.dispose();
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return tex;
    }

    private void drawRoundedRect(Pixmap pm, int x, int y, int w, int h, int r) {
        // Draw central rect
        pm.fillRectangle(x + r, y, w - 2 * r, h);
        // left/right rects
        pm.fillRectangle(x, y + r, r, h - 2 * r);
        pm.fillRectangle(x + w - r, y + r, r, h - 2 * r);
        // corners as filled circles
        pm.fillCircle(x + r, y + r, r);
        pm.fillCircle(x + w - r, y + r, r);
        pm.fillCircle(x + r, y + h - r, r);
        pm.fillCircle(x + w - r, y + h - r, r);
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
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
        // Dispose generated textures
        if (cardTex != null)
            cardTex.dispose();
        if (pillUpTex != null)
            pillUpTex.dispose();
        if (pillOverTex != null)
            pillOverTex.dispose();
        if (pillDownTex != null)
            pillDownTex.dispose();
        if (pillCheckedTex != null)
            pillCheckedTex.dispose();
    }
}
