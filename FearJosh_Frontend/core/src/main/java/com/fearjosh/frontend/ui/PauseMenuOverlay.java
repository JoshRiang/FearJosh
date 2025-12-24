package com.fearjosh.frontend.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class PauseMenuOverlay {

    public enum PauseState {
        HIDDEN,
        SHOWING,
        CONFIRM_QUIT
    }

    private PauseState state = PauseState.HIDDEN;

    private MenuButton resumeButton;
    private MenuButton quitToMenuButton;
    private MenuButton confirmQuitButton;
    private MenuButton cancelButton;

    private Texture dialogPanelTexture;
    private BitmapFont warningFont;
    private GlyphLayout layout;

    private Runnable onConfirmQuit;
    private Runnable onResume;

    private float screenWidth;
    private float screenHeight;

    private boolean inputConsumed = false;

    public PauseMenuOverlay(float screenWidth, float screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        initializeButtons();
        createDialogPanelTexture();
        
        warningFont = new BitmapFont();
        warningFont.setColor(Color.WHITE);
        warningFont.getData().setScale(1.1f);
        layout = new GlyphLayout();
    }

    private void initializeButtons() {
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        
        float btnW = MenuButton.MENU_BUTTON_WIDTH;
        float btnH = MenuButton.MENU_BUTTON_HEIGHT;
        float spacing = MenuButton.MENU_BUTTON_SPACING;
        
        resumeButton = new MenuButton("menu/pause_resume.png", null, 
                centerX, centerY + btnH / 2f + spacing / 2f, btnW, btnH);
        quitToMenuButton = new MenuButton("menu/pause_quit_to_menu.png", null, 
                centerX, centerY - btnH / 2f - spacing / 2f, btnW, btnH);
        
        float dialogBtnW = MenuButton.DIALOG_BUTTON_WIDTH;
        float dialogBtnH = MenuButton.DIALOG_BUTTON_HEIGHT;
        float dialogSpacing = MenuButton.DIALOG_BUTTON_SPACING;
        
        cancelButton = new MenuButton("menu/dialog_cancel.png", null, 
                centerX - dialogBtnW / 2f - dialogSpacing / 2f, centerY - 60f, dialogBtnW, dialogBtnH);
        confirmQuitButton = new MenuButton("menu/dialog_confirm.png", null, 
                centerX + dialogBtnW / 2f + dialogSpacing / 2f, centerY - 60f, dialogBtnW, dialogBtnH);
    }

    private void createDialogPanelTexture() {
        int w = 500, h = 200;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        
        pm.setColor(0.1f, 0.1f, 0.12f, 0.95f);
        pm.fillRectangle(0, 0, w, h);
        
        pm.setColor(0.7f, 0.15f, 0.15f, 1f);
        pm.drawRectangle(0, 0, w, h);
        pm.drawRectangle(1, 1, w - 2, h - 2);
        
        dialogPanelTexture = new Texture(pm);
        pm.dispose();
    }

    public void show() {
        state = PauseState.SHOWING;
        inputConsumed = true;
    }

    public void hide() {
        state = PauseState.HIDDEN;
    }

    public boolean isActive() {
        return state != PauseState.HIDDEN;
    }

    public boolean isShowingConfirmDialog() {
        return state == PauseState.CONFIRM_QUIT;
    }

    public void setOnConfirmQuit(Runnable callback) {
        this.onConfirmQuit = callback;
    }

    public void setOnResume(Runnable callback) {
        this.onResume = callback;
    }

    public void update(OrthographicCamera uiCamera) {
        if (state == PauseState.HIDDEN) return;

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();
        Vector3 uiCoords = uiCamera.unproject(new Vector3(mouseX, mouseY, 0));

        if (!Gdx.input.justTouched() && !Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)) {
            inputConsumed = false;
        }

        if (state == PauseState.SHOWING) {
            resumeButton.update(uiCoords.x, uiCoords.y);
            quitToMenuButton.update(uiCoords.x, uiCoords.y);

            if (!inputConsumed && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                doResume();
                return;
            }

            if (!inputConsumed && Gdx.input.justTouched()) {
                if (resumeButton.isClicked(uiCoords.x, uiCoords.y)) {
                    doResume();
                    return;
                }
                if (quitToMenuButton.isClicked(uiCoords.x, uiCoords.y)) {
                    state = PauseState.CONFIRM_QUIT;
                    inputConsumed = true;
                    return;
                }
            }
            
            if (!inputConsumed && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || 
                                    Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
                doResume();
                return;
            }
            
        } else if (state == PauseState.CONFIRM_QUIT) {
            cancelButton.update(uiCoords.x, uiCoords.y);
            confirmQuitButton.update(uiCoords.x, uiCoords.y);

            if (!inputConsumed && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                state = PauseState.SHOWING;
                inputConsumed = true;
                return;
            }

            if (!inputConsumed && Gdx.input.justTouched()) {
                if (cancelButton.isClicked(uiCoords.x, uiCoords.y)) {
                    state = PauseState.SHOWING;
                    inputConsumed = true;
                    return;
                }
                if (confirmQuitButton.isClicked(uiCoords.x, uiCoords.y)) {
                    doConfirmQuit();
                    return;
                }
            }
        }
    }

    private void doResume() {
        state = PauseState.HIDDEN;
        if (onResume != null) {
            onResume.run();
        }
    }

    private void doConfirmQuit() {
        state = PauseState.HIDDEN;
        if (onConfirmQuit != null) {
            onConfirmQuit.run();
        }
    }

    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, 
                       float screenWidth, float screenHeight) {
        if (state == PauseState.HIDDEN) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();

        batch.begin();

        if (state == PauseState.SHOWING) {
            warningFont.getData().setScale(2f);
            warningFont.setColor(0.9f, 0.2f, 0.2f, 1f);
            layout.setText(warningFont, "JEDA");
            warningFont.draw(batch, "JEDA", 
                    screenWidth / 2f - layout.width / 2f, 
                    screenHeight / 2f + 140f);
            warningFont.getData().setScale(1.1f);
            warningFont.setColor(Color.WHITE);

            if (resumeButton != null) {
                resumeButton.render(batch);
            }
            if (quitToMenuButton != null) {
                quitToMenuButton.render(batch);
            }
            
            renderFallbackButtons(batch, screenWidth, screenHeight);
            
        } else if (state == PauseState.CONFIRM_QUIT) {
            renderConfirmDialog(batch, screenWidth, screenHeight);
        }

        batch.end();
    }

    private void renderFallbackButtons(SpriteBatch batch, float screenWidth, float screenHeight) {
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        
        float btnH = MenuButton.MENU_BUTTON_HEIGHT;
        float spacing = MenuButton.MENU_BUTTON_SPACING;
        
        if (resumeButton.getRenderWidth() == MenuButton.MENU_BUTTON_WIDTH) {
            warningFont.setColor(resumeButton.isHovered() ? Color.YELLOW : Color.WHITE);
            layout.setText(warningFont, "LANJUT");
            float btnCenterY = centerY + btnH / 2f + spacing / 2f;
            warningFont.draw(batch, "LANJUT", centerX - layout.width / 2f, btnCenterY + layout.height / 2f);
        }
        
        if (quitToMenuButton.getRenderWidth() == MenuButton.MENU_BUTTON_WIDTH) {
            warningFont.setColor(quitToMenuButton.isHovered() ? Color.YELLOW : Color.WHITE);
            layout.setText(warningFont, "KELUAR KE MENU");
            float btnCenterY = centerY - btnH / 2f - spacing / 2f;
            warningFont.draw(batch, "KELUAR KE MENU", centerX - layout.width / 2f, btnCenterY + layout.height / 2f);
        }
        
        warningFont.setColor(Color.WHITE);
    }

    private void renderConfirmDialog(SpriteBatch batch, float screenWidth, float screenHeight) {
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        
        if (dialogPanelTexture != null) {
            float panelW = dialogPanelTexture.getWidth();
            float panelH = dialogPanelTexture.getHeight();
            batch.draw(dialogPanelTexture, centerX - panelW / 2f, centerY - panelH / 2f + 20f);
        }
        
        warningFont.getData().setScale(1.4f);
        warningFont.setColor(0.9f, 0.2f, 0.2f, 1f);
        layout.setText(warningFont, "PERINGATAN");
        warningFont.draw(batch, "PERINGATAN", centerX - layout.width / 2f, centerY + 90f);
        
        warningFont.getData().setScale(1f);
        warningFont.setColor(Color.WHITE);
        
        String line1 = "Progress TIDAK akan tersimpan.";
        String line2 = "Jika kamu keluar ke menu,";
        String line3 = "sesi game ini akan hilang.";
        
        layout.setText(warningFont, line1);
        warningFont.draw(batch, line1, centerX - layout.width / 2f, centerY + 50f);
        
        layout.setText(warningFont, line2);
        warningFont.draw(batch, line2, centerX - layout.width / 2f, centerY + 25f);
        
        layout.setText(warningFont, line3);
        warningFont.draw(batch, line3, centerX - layout.width / 2f, centerY);
        
        if (cancelButton != null) {
            cancelButton.render(batch);
        }
        if (confirmQuitButton != null) {
            confirmQuitButton.render(batch);
        }
        
        renderDialogFallbackButtons(batch, centerX, centerY);
    }

    private void renderDialogFallbackButtons(SpriteBatch batch, float centerX, float centerY) {
        float dialogBtnW = MenuButton.DIALOG_BUTTON_WIDTH;
        float dialogSpacing = MenuButton.DIALOG_BUTTON_SPACING;
        
        warningFont.setColor(cancelButton.isHovered() ? Color.YELLOW : Color.LIGHT_GRAY);
        layout.setText(warningFont, "BATAL");
        float cancelX = centerX - dialogBtnW / 2f - dialogSpacing / 2f;
        warningFont.draw(batch, "BATAL", cancelX - layout.width / 2f, centerY - 50f);
        
        warningFont.setColor(confirmQuitButton.isHovered() ? Color.RED : new Color(0.8f, 0.3f, 0.3f, 1f));
        layout.setText(warningFont, "KELUAR");
        float confirmX = centerX + dialogBtnW / 2f + dialogSpacing / 2f;
        warningFont.draw(batch, "KELUAR", confirmX - layout.width / 2f, centerY - 50f);
        
        warningFont.setColor(Color.WHITE);
    }

    public void dispose() {
        if (resumeButton != null) resumeButton.dispose();
        if (quitToMenuButton != null) quitToMenuButton.dispose();
        if (cancelButton != null) cancelButton.dispose();
        if (confirmQuitButton != null) confirmQuitButton.dispose();
        if (dialogPanelTexture != null) dialogPanelTexture.dispose();
        if (warningFont != null) warningFont.dispose();
    }
}
