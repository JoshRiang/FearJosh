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

/**
 * Pause menu overlay with PNG buttons.
 * Features:
 * - Resume button
 * - Quit to Menu button (with confirmation warning)
 * 
 * Flow:
 * 1. Player presses ESC or clicks pause button -> PAUSED state
 * 2. Shows Resume and Quit to Menu buttons
 * 3. Quit to Menu shows confirmation dialog
 * 4. Confirm quit -> goes to FRESH main menu (no Resume option)
 */
public class PauseMenuOverlay {

    public enum PauseState {
        HIDDEN,         // Not showing
        SHOWING,        // Showing pause menu
        CONFIRM_QUIT    // Showing quit confirmation dialog
    }

    private PauseState state = PauseState.HIDDEN;

    // Buttons
    private MenuButton resumeButton;
    private MenuButton quitToMenuButton;
    
    // Confirmation dialog buttons
    private MenuButton confirmQuitButton;
    private MenuButton cancelButton;

    // Dialog panel texture
    private Texture dialogPanelTexture;
    
    // Font for warning text
    private BitmapFont warningFont;
    private GlyphLayout layout;

    // Callback when user confirms quit
    private Runnable onConfirmQuit;
    private Runnable onResume;

    // Screen dimensions
    private float screenWidth;
    private float screenHeight;

    // Input debounce
    private boolean inputConsumed = false;

    public PauseMenuOverlay(float screenWidth, float screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        // Initialize buttons
        initializeButtons();
        
        // Create dialog panel texture
        createDialogPanelTexture();
        
        // Warning font
        warningFont = new BitmapFont();
        warningFont.setColor(Color.WHITE);
        warningFont.getData().setScale(1.1f);
        layout = new GlyphLayout();
    }

    private void initializeButtons() {
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        
        // Use MenuButton constants for consistent sizing
        float btnW = MenuButton.MENU_BUTTON_WIDTH;
        float btnH = MenuButton.MENU_BUTTON_HEIGHT;
        float spacing = MenuButton.MENU_BUTTON_SPACING;
        
        // Pause menu buttons (stacked vertically)
        // Resume slightly above center
        resumeButton = new MenuButton("menu/pause_resume.png", null, 
                centerX, centerY + btnH / 2f + spacing / 2f, btnW, btnH);
        // Quit to Menu below
        quitToMenuButton = new MenuButton("menu/pause_quit_to_menu.png", null, 
                centerX, centerY - btnH / 2f - spacing / 2f, btnW, btnH);
        
        // Confirmation dialog buttons (smaller, side by side)
        float dialogBtnW = MenuButton.DIALOG_BUTTON_WIDTH;
        float dialogBtnH = MenuButton.DIALOG_BUTTON_HEIGHT;
        float dialogSpacing = MenuButton.DIALOG_BUTTON_SPACING;
        
        // Cancel on left, Quit on right
        cancelButton = new MenuButton("menu/dialog_cancel.png", null, 
                centerX - dialogBtnW / 2f - dialogSpacing / 2f, centerY - 60f, dialogBtnW, dialogBtnH);
        confirmQuitButton = new MenuButton("menu/dialog_confirm.png", null, 
                centerX + dialogBtnW / 2f + dialogSpacing / 2f, centerY - 60f, dialogBtnW, dialogBtnH);
        
        // If textures don't exist, fallback will be handled in render
    }

    private void createDialogPanelTexture() {
        // Create a simple dark panel texture
        int w = 500, h = 200;
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        
        // Dark background with red border
        pm.setColor(0.1f, 0.1f, 0.12f, 0.95f);
        pm.fillRectangle(0, 0, w, h);
        
        // Red border
        pm.setColor(0.7f, 0.15f, 0.15f, 1f);
        pm.drawRectangle(0, 0, w, h);
        pm.drawRectangle(1, 1, w - 2, h - 2);
        
        dialogPanelTexture = new Texture(pm);
        pm.dispose();
    }

    /**
     * Show the pause menu
     */
    public void show() {
        state = PauseState.SHOWING;
        inputConsumed = true; // Prevent immediate click-through
    }

    /**
     * Hide the pause menu
     */
    public void hide() {
        state = PauseState.HIDDEN;
    }

    /**
     * Check if pause menu is active
     */
    public boolean isActive() {
        return state != PauseState.HIDDEN;
    }

    /**
     * Check if showing confirmation dialog
     */
    public boolean isShowingConfirmDialog() {
        return state == PauseState.CONFIRM_QUIT;
    }

    /**
     * Set callback when user confirms quit
     */
    public void setOnConfirmQuit(Runnable callback) {
        this.onConfirmQuit = callback;
    }

    /**
     * Set callback when user resumes
     */
    public void setOnResume(Runnable callback) {
        this.onResume = callback;
    }

    /**
     * Update pause menu
     */
    public void update(OrthographicCamera uiCamera) {
        if (state == PauseState.HIDDEN) return;

        // Get mouse position in UI coordinates
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();
        Vector3 uiCoords = uiCamera.unproject(new Vector3(mouseX, mouseY, 0));

        // Reset input consumed flag after a frame
        if (!Gdx.input.justTouched() && !Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)) {
            inputConsumed = false;
        }

        if (state == PauseState.SHOWING) {
            // Update button hover states
            resumeButton.update(uiCoords.x, uiCoords.y);
            quitToMenuButton.update(uiCoords.x, uiCoords.y);

            // Handle ESC to resume
            if (!inputConsumed && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                doResume();
                return;
            }

            // Handle button clicks
            if (!inputConsumed && Gdx.input.justTouched()) {
                if (resumeButton.isClicked(uiCoords.x, uiCoords.y)) {
                    doResume();
                    return;
                }
                if (quitToMenuButton.isClicked(uiCoords.x, uiCoords.y)) {
                    // Show confirmation dialog
                    state = PauseState.CONFIRM_QUIT;
                    inputConsumed = true;
                    return;
                }
            }
            
            // Handle ENTER/SPACE for resume
            if (!inputConsumed && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || 
                                    Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
                doResume();
                return;
            }
            
        } else if (state == PauseState.CONFIRM_QUIT) {
            // Update dialog button hover states
            cancelButton.update(uiCoords.x, uiCoords.y);
            confirmQuitButton.update(uiCoords.x, uiCoords.y);

            // Handle ESC to cancel
            if (!inputConsumed && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                state = PauseState.SHOWING;
                inputConsumed = true;
                return;
            }

            // Handle button clicks
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

    /**
     * Render the pause menu
     */
    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, 
                       float screenWidth, float screenHeight) {
        if (state == PauseState.HIDDEN) return;

        // Draw semi-transparent backdrop
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();

        batch.begin();

        if (state == PauseState.SHOWING) {
            // Draw "PAUSED" title
            warningFont.getData().setScale(2f);
            warningFont.setColor(0.9f, 0.2f, 0.2f, 1f);
            layout.setText(warningFont, "JEDA");
            warningFont.draw(batch, "JEDA", 
                    screenWidth / 2f - layout.width / 2f, 
                    screenHeight / 2f + 140f);
            warningFont.getData().setScale(1.1f);
            warningFont.setColor(Color.WHITE);

            // Draw buttons (if textures loaded, otherwise fallback)
            if (resumeButton != null) {
                resumeButton.render(batch);
            }
            if (quitToMenuButton != null) {
                quitToMenuButton.render(batch);
            }
            
            // Fallback text if buttons have no textures
            renderFallbackButtons(batch, screenWidth, screenHeight);
            
        } else if (state == PauseState.CONFIRM_QUIT) {
            // Draw confirmation dialog
            renderConfirmDialog(batch, screenWidth, screenHeight);
        }

        batch.end();
    }

    private void renderFallbackButtons(SpriteBatch batch, float screenWidth, float screenHeight) {
        // Only render text fallback if button textures failed to load
        // Buttons now use fixed size from constants, so check getRenderWidth instead
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;
        
        float btnH = MenuButton.MENU_BUTTON_HEIGHT;
        float spacing = MenuButton.MENU_BUTTON_SPACING;
        
        // Resume button fallback - check if texture loaded (bounds width matches expected)
        if (resumeButton.getRenderWidth() == MenuButton.MENU_BUTTON_WIDTH) {
            // Texture loaded OR using fixed size - only show text if really no texture
            // We'll still draw text centered on button position
            warningFont.setColor(resumeButton.isHovered() ? Color.YELLOW : Color.WHITE);
            layout.setText(warningFont, "LANJUT");
            float btnCenterY = centerY + btnH / 2f + spacing / 2f;
            warningFont.draw(batch, "LANJUT", centerX - layout.width / 2f, btnCenterY + layout.height / 2f);
        }
        
        // Quit button fallback
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
        
        // Draw dialog panel
        if (dialogPanelTexture != null) {
            float panelW = dialogPanelTexture.getWidth();
            float panelH = dialogPanelTexture.getHeight();
            batch.draw(dialogPanelTexture, centerX - panelW / 2f, centerY - panelH / 2f + 20f);
        }
        
        // Warning title
        warningFont.getData().setScale(1.4f);
        warningFont.setColor(0.9f, 0.2f, 0.2f, 1f);
        layout.setText(warningFont, "PERINGATAN");
        warningFont.draw(batch, "PERINGATAN", centerX - layout.width / 2f, centerY + 90f);
        
        // Warning text
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
        
        // Draw dialog buttons
        if (cancelButton != null) {
            cancelButton.render(batch);
        }
        if (confirmQuitButton != null) {
            confirmQuitButton.render(batch);
        }
        
        // Fallback button text
        renderDialogFallbackButtons(batch, centerX, centerY);
    }

    private void renderDialogFallbackButtons(SpriteBatch batch, float centerX, float centerY) {
        float dialogBtnW = MenuButton.DIALOG_BUTTON_WIDTH;
        float dialogSpacing = MenuButton.DIALOG_BUTTON_SPACING;
        
        // Cancel button fallback - draw text centered on button position
        warningFont.setColor(cancelButton.isHovered() ? Color.YELLOW : Color.LIGHT_GRAY);
        layout.setText(warningFont, "BATAL");
        float cancelX = centerX - dialogBtnW / 2f - dialogSpacing / 2f;
        warningFont.draw(batch, "BATAL", cancelX - layout.width / 2f, centerY - 50f);
        
        // Confirm button fallback
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
