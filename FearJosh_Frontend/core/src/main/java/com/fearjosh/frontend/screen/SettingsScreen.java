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
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.difficulty.GameDifficulty;
import com.fearjosh.frontend.systems.AudioManager;
import com.fearjosh.frontend.ui.MenuButton;

public class SettingsScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;
    
    private static final float SETTINGS_BUTTON_WIDTH = 200f;
    private static final float SETTINGS_BUTTON_HEIGHT = 56f;
    private static final float SETTINGS_BUTTON_SPACING = 16f;

    private final FearJosh game;
    private SpriteBatch batch;
    private OrthographicCamera uiCamera;
    private FitViewport viewport;
    
    private BitmapFont font;
    private GlyphLayout glyphLayout;
    private Texture backgroundTex;
    
    private MenuButton easyBtn;
    private MenuButton normalBtn;
    private MenuButton hardBtn;
    private MenuButton backBtn;
    
    private Texture selectionHighlightTex;
    
    private boolean isActive = true;
    
    private Stage dialogStage;
    private Skin dialogSkin;
    private boolean showingDialog = false;
    private GameDifficulty pendingDifficulty = null;
    private Texture dialogCardTex;
    private Texture dialogBtnUpTex;
    private Texture dialogBtnOverTex;
    private Texture dialogBtnDownTex;

    public SettingsScreen(FearJosh game) {
        this.game = game;
        
        uiCamera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, uiCamera);
        batch = new SpriteBatch();
        
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.5f);
        glyphLayout = new GlyphLayout();
        
        try {
            backgroundTex = new Texture("UI/main_menu_background.png");
        } catch (Exception e) {
            backgroundTex = null;
        }
        
        selectionHighlightTex = createSelectionHighlight();
        createButtons();
        setupDialogStage();
    }
    
    private void createButtons() {
        float centerX = VIRTUAL_WIDTH / 2f;
        float startY = VIRTUAL_HEIGHT * 0.55f;
        
        easyBtn = new MenuButton(
            "menu/settings_easy.png",
            null,
            centerX, startY,
            SETTINGS_BUTTON_WIDTH, SETTINGS_BUTTON_HEIGHT
        );
        
        normalBtn = new MenuButton(
            "menu/settings_normal.png",
            null,
            centerX, startY - (SETTINGS_BUTTON_HEIGHT + SETTINGS_BUTTON_SPACING),
            SETTINGS_BUTTON_WIDTH, SETTINGS_BUTTON_HEIGHT
        );
        
        hardBtn = new MenuButton(
            "menu/settings_hard.png",
            null,
            centerX, startY - 2 * (SETTINGS_BUTTON_HEIGHT + SETTINGS_BUTTON_SPACING),
            SETTINGS_BUTTON_WIDTH, SETTINGS_BUTTON_HEIGHT
        );
        
        backBtn = new MenuButton(
            "menu/settings_back.png",
            null,
            centerX, startY - 3.5f * (SETTINGS_BUTTON_HEIGHT + SETTINGS_BUTTON_SPACING),
            SETTINGS_BUTTON_WIDTH, SETTINGS_BUTTON_HEIGHT
        );
    }
    
    private Texture createSelectionHighlight() {
        int w = (int) SETTINGS_BUTTON_WIDTH + 20;
        int h = (int) SETTINGS_BUTTON_HEIGHT + 10;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(new Color(0.2f, 0.6f, 1f, 0.3f));
        pm.fill();
        pm.setColor(new Color(0.3f, 0.7f, 1f, 0.8f));
        pm.drawRectangle(0, 0, w, h);
        pm.drawRectangle(1, 1, w - 2, h - 2);
        Texture tex = new Texture(pm);
        pm.dispose();
        return tex;
    }
    
    private void setupDialogStage() {
        dialogStage = new Stage(viewport);
        dialogSkin = new Skin();
        dialogSkin.add("default-font", new BitmapFont());
        
        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = dialogSkin.get("default-font", BitmapFont.class);
        dialogSkin.add("default", ls);
        
        dialogCardTex = createRoundedTexture(480, 280, 16, new Color(0.15f, 0.15f, 0.18f, 0.98f));
        dialogBtnUpTex = createRoundedTexture(140, 44, 22, new Color(1f, 1f, 1f, 0.1f));
        dialogBtnOverTex = createRoundedTexture(140, 44, 22, new Color(1f, 1f, 1f, 0.2f));
        dialogBtnDownTex = createRoundedTexture(140, 44, 22, new Color(0.04f, 0.52f, 1f, 0.9f));
        
        TextButton.TextButtonStyle tbs = new TextButton.TextButtonStyle();
        tbs.font = dialogSkin.get("default-font", BitmapFont.class);
        tbs.fontColor = Color.WHITE;
        tbs.up = new NinePatchDrawable(new NinePatch(dialogBtnUpTex, 22, 22, 22, 22));
        tbs.over = new NinePatchDrawable(new NinePatch(dialogBtnOverTex, 22, 22, 22, 22));
        tbs.down = new NinePatchDrawable(new NinePatch(dialogBtnDownTex, 22, 22, 22, 22));
        dialogSkin.add("default", tbs);
    }
    
    private Texture createRoundedTexture(int w, int h, int radius, Color color) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        drawRoundedRect(pm, 0, 0, w, h, radius);
        Texture tex = new Texture(pm);
        pm.dispose();
        return tex;
    }
    
    private void drawRoundedRect(Pixmap pm, int x, int y, int w, int h, int r) {
        pm.fillRectangle(x + r, y, w - 2 * r, h);
        pm.fillRectangle(x, y + r, r, h - 2 * r);
        pm.fillRectangle(x + w - r, y + r, r, h - 2 * r);
        pm.fillCircle(x + r, y + r, r);
        pm.fillCircle(x + w - r, y + r, r);
        pm.fillCircle(x + r, y + h - r, r);
        pm.fillCircle(x + w - r, y + h - r, r);
    }

    @Override
    public void show() {
        AudioManager.getInstance().playMusic("Audio/Music/main_menu_music.wav", true);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        uiCamera.update();
        batch.setProjectionMatrix(uiCamera.combined);
        
        if (showingDialog) {
            Gdx.input.setInputProcessor(dialogStage);
            dialogStage.act(delta);
            dialogStage.draw();
            return;
        }
        
        Gdx.input.setInputProcessor(null);
        
        float mouseX = getMouseX();
        float mouseY = getMouseY();
        
        easyBtn.update(mouseX, mouseY);
        normalBtn.update(mouseX, mouseY);
        hardBtn.update(mouseX, mouseY);
        backBtn.update(mouseX, mouseY);
        
        batch.begin();
        
        if (backgroundTex != null) {
            batch.draw(backgroundTex, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        }
        
        font.setColor(Color.WHITE);
        font.getData().setScale(2f);
        glyphLayout.setText(font, "PENGATURAN");
        font.draw(batch, "PENGATURAN", (VIRTUAL_WIDTH - glyphLayout.width) / 2f, VIRTUAL_HEIGHT - 60f);
        
        font.getData().setScale(1.2f);
        font.setColor(new Color(0.8f, 0.8f, 0.85f, 1f));
        glyphLayout.setText(font, "Pilih Kesulitan:");
        font.draw(batch, "Pilih Kesulitan:", (VIRTUAL_WIDTH - glyphLayout.width) / 2f, VIRTUAL_HEIGHT * 0.68f);
        
        GameDifficulty current = GameManager.getInstance().getDifficulty();
        MenuButton selectedBtn = getButtonForDifficulty(current);
        if (selectedBtn != null && selectionHighlightTex != null) {
            float hx = selectedBtn.getBounds().x - 10f;
            float hy = selectedBtn.getBounds().y - 5f;
            batch.draw(selectionHighlightTex, hx, hy);
        }
        
        easyBtn.render(batch);
        normalBtn.render(batch);
        hardBtn.render(batch);
        backBtn.render(batch);
        
        font.getData().setScale(1f);
        font.setColor(new Color(0.5f, 0.8f, 0.5f, 1f));
        String currentText = "Current: " + current.name().substring(0, 1).toUpperCase() 
                           + current.name().substring(1).toLowerCase();
        float margin = 12f;
        float currentX = margin;
        float currentY = margin + font.getCapHeight();
        font.draw(batch, currentText, currentX, currentY);
        
        batch.end();
        
        if (Gdx.input.justTouched() && isActive) {
            handleClicks(mouseX, mouseY);
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
        }
    }
    
    private float getMouseX() {
        return viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())).x;
    }
    
    private float getMouseY() {
        return viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY())).y;
    }
    
    private MenuButton getButtonForDifficulty(GameDifficulty diff) {
        switch (diff) {
            case EASY: return easyBtn;
            case MEDIUM: return normalBtn;
            case HARD: return hardBtn;
            default: return normalBtn;
        }
    }
    
    private void handleClicks(float mouseX, float mouseY) {
        if (easyBtn.isClicked(mouseX, mouseY)) {
            attemptChangeDifficulty(GameDifficulty.EASY);
            return;
        }
        
        if (normalBtn.isClicked(mouseX, mouseY)) {
            attemptChangeDifficulty(GameDifficulty.MEDIUM);
            return;
        }
        
        if (hardBtn.isClicked(mouseX, mouseY)) {
            attemptChangeDifficulty(GameDifficulty.HARD);
            return;
        }
        
        if (backBtn.isClicked(mouseX, mouseY)) {
            game.setScreen(new MainMenuScreen(game));
        }
    }
    
    private void attemptChangeDifficulty(GameDifficulty newDiff) {
        GameManager gm = GameManager.getInstance();
        
        if (gm.getDifficulty() == newDiff) {
            return;
        }
        
        if (gm.difficultyChangeRequiresNewGame()) {
            pendingDifficulty = newDiff;
            showConfirmationDialog();
        } else {
            gm.setDifficulty(newDiff);
            System.out.println("[Settings] Difficulty changed to " + newDiff);
        }
    }
    
    private void showConfirmationDialog() {
        showingDialog = true;
        dialogStage.clear();
        
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(new NinePatchDrawable(new NinePatch(
            createRoundedTexture(1, 1, 0, new Color(0f, 0f, 0f, 0.7f)), 0, 0, 0, 0)));
        
        Table dialog = new Table();
        dialog.setBackground(new NinePatchDrawable(new NinePatch(dialogCardTex, 16, 16, 16, 16)));
        dialog.pad(28f);
        
        Label title = new Label("Warning", dialogSkin);
        title.setColor(new Color(1f, 0.4f, 0.3f, 1f));
        title.setFontScale(1.3f);
        
        Label message = new Label(
            "Changing difficulty will start a\nNEW GAME and delete your\ncurrent progress.\n\nContinue?",
            dialogSkin);
        message.setColor(Color.WHITE);
        message.setAlignment(Align.center);
        
        TextButton confirmBtn = new TextButton("New Game", dialogSkin);
        TextButton cancelBtn = new TextButton("Cancel", dialogSkin);
        
        dialog.add(title).padBottom(16f).row();
        dialog.add(message).padBottom(24f).row();
        
        Table buttons = new Table();
        buttons.add(cancelBtn).width(140f).height(44f).pad(6f);
        buttons.add(confirmBtn).width(140f).height(44f).pad(6f);
        dialog.add(buttons).row();
        
        overlay.add(dialog);
        dialogStage.addActor(overlay);
        
        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showingDialog = false;
                pendingDifficulty = null;
                dialogStage.clear();
            }
        });
        
        confirmBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showingDialog = false;
                if (pendingDifficulty != null) {
                    GameManager gm = GameManager.getInstance();
                    gm.changeDifficultyAndStartNewGame(pendingDifficulty, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
                    gm.setCurrentState(GameManager.GameState.PLAYING);
                    game.setScreen(new PlayScreen(game));
                }
                pendingDifficulty = null;
            }
        });
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
        if (selectionHighlightTex != null) selectionHighlightTex.dispose();
        if (easyBtn != null) easyBtn.dispose();
        if (normalBtn != null) normalBtn.dispose();
        if (hardBtn != null) hardBtn.dispose();
        if (backBtn != null) backBtn.dispose();
        if (dialogStage != null) dialogStage.dispose();
        if (dialogSkin != null) dialogSkin.dispose();
        if (dialogCardTex != null) dialogCardTex.dispose();
        if (dialogBtnUpTex != null) dialogBtnUpTex.dispose();
        if (dialogBtnOverTex != null) dialogBtnOverTex.dispose();
        if (dialogBtnDownTex != null) dialogBtnDownTex.dispose();
    }
}
