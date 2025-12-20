package com.fearjosh.frontend.cutscene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.systems.AudioManager;

/**
 * Screen for displaying cutscenes with images/videos, dialogs, and music.
 * User presses SPACE to advance through dialogs.
 */
public class CutsceneScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;

    // Dialog box dimensions
    private static final float DIALOG_BOX_HEIGHT = 150f;
    private static final float DIALOG_BOX_PADDING = 20f;
    private static final float TEXT_PADDING = 15f;

    private final FearJosh game;
    private final CutsceneData cutsceneData;
    private final Screen nextScreen; // Screen to show after cutscene ends

    private OrthographicCamera camera;
    private FitViewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private BitmapFont speakerFont;

    private Texture contentTexture; // For image content
    private int currentDialogIndex;
    private boolean canAdvance; // Prevent skipping too fast
    private float timeSinceLastAdvance;
    private static final float ADVANCE_COOLDOWN = 0.2f;

    /**
     * Create a cutscene screen.
     * 
     * @param game         The main game instance
     * @param cutsceneData The cutscene data to display
     * @param nextScreen   The screen to show after cutscene completes (e.g.,
     *                     PlayScreen or MainMenu)
     */
    public CutsceneScreen(FearJosh game, CutsceneData cutsceneData, Screen nextScreen) {
        this.game = game;
        this.cutsceneData = cutsceneData;
        this.nextScreen = nextScreen;
        this.currentDialogIndex = 0;
        this.canAdvance = false;
        this.timeSinceLastAdvance = 0f;

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Create fonts for dialog
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.0f);

        speakerFont = new BitmapFont();
        speakerFont.setColor(new Color(1f, 0.8f, 0.2f, 1f)); // Yellow/gold color for speaker name
        speakerFont.getData().setScale(1.2f);

        // Load content if available
        if (cutsceneData.hasContent() && cutsceneData.getContentType() == CutsceneContentType.IMAGE) {
            try {
                contentTexture = new Texture(cutsceneData.getContentPath());
            } catch (Exception e) {
                System.err.println("[Cutscene] Failed to load image: " + cutsceneData.getContentPath());
                contentTexture = null;
            }
        }

        // Stop any background music that is currently playing
        AudioManager.getInstance().stopMusic();

        // Start cutscene music if available
        if (cutsceneData.hasMusic()) {
            AudioManager.getInstance().playMusic(cutsceneData.getMusicPath(), true);
        }

        System.out.println("[Cutscene] Started: " + cutsceneData.getCutsceneId());
        System.out.println("[Cutscene] Total dialogs: " + cutsceneData.getDialogCount());
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void render(float delta) {
        // Update cooldown timer
        timeSinceLastAdvance += delta;
        if (timeSinceLastAdvance >= ADVANCE_COOLDOWN) {
            canAdvance = true;
        }

        // Handle input - SPACE to advance
        if (canAdvance && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            advanceDialog();
        }

        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Render content (image/video background)
        batch.begin();
        if (contentTexture != null) {
            // Draw image scaled to fit screen
            float imageWidth = VIRTUAL_WIDTH;
            float imageHeight = VIRTUAL_HEIGHT - DIALOG_BOX_HEIGHT;
            batch.draw(contentTexture, 0, DIALOG_BOX_HEIGHT, imageWidth, imageHeight);
        }
        batch.end();

        // Render dialog box
        renderDialogBox();

        // Render current dialog text
        renderDialogText();
    }

    private void renderDialogBox() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Semi-transparent dark background for dialog box
        shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 0.9f);
        shapeRenderer.rect(
                DIALOG_BOX_PADDING,
                DIALOG_BOX_PADDING,
                VIRTUAL_WIDTH - (DIALOG_BOX_PADDING * 2),
                DIALOG_BOX_HEIGHT - (DIALOG_BOX_PADDING * 2));

        // Border
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.8f, 0.1f, 0.1f, 1f); // Red border (horror theme)
        Gdx.gl.glLineWidth(3);
        shapeRenderer.rect(
                DIALOG_BOX_PADDING,
                DIALOG_BOX_PADDING,
                VIRTUAL_WIDTH - (DIALOG_BOX_PADDING * 2),
                DIALOG_BOX_HEIGHT - (DIALOG_BOX_PADDING * 2));

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderDialogText() {
        CutsceneDialog currentDialog = cutsceneData.getDialog(currentDialogIndex);
        if (currentDialog == null)
            return;

        batch.begin();

        float textX = DIALOG_BOX_PADDING + TEXT_PADDING;
        float textY = DIALOG_BOX_HEIGHT - DIALOG_BOX_PADDING - TEXT_PADDING;
        float textWidth = VIRTUAL_WIDTH - (DIALOG_BOX_PADDING + TEXT_PADDING) * 2;

        // Draw speaker name if available
        if (currentDialog.hasSpeaker()) {
            speakerFont.draw(batch, currentDialog.getSpeakerName(),
                    textX, textY, textWidth, Align.left, false);
            textY -= 30f; // Move down for dialog text
        }

        // Draw dialog text with word wrapping
        font.draw(batch, currentDialog.getDialogText(),
                textX, textY, textWidth, Align.left, true);

        // Draw progress indicator (e.g., "Press SPACE to continue" or "3/10")
        String progressText;
        if (currentDialogIndex < cutsceneData.getDialogCount() - 1) {
            progressText = "Press SPACE to continue... (" +
                    (currentDialogIndex + 1) + "/" + cutsceneData.getDialogCount() + ")";
        } else {
            progressText = "Press SPACE to finish... (" +
                    cutsceneData.getDialogCount() + "/" + cutsceneData.getDialogCount() + ")";
        }

        font.setColor(0.7f, 0.7f, 0.7f, 1f);
        font.draw(batch, progressText,
                VIRTUAL_WIDTH - DIALOG_BOX_PADDING - TEXT_PADDING,
                DIALOG_BOX_PADDING + TEXT_PADDING + 20f,
                0, Align.right, false);
        font.setColor(Color.WHITE);

        batch.end();
    }

    private void advanceDialog() {
        currentDialogIndex++;
        canAdvance = false;
        timeSinceLastAdvance = 0f;

        if (currentDialogIndex >= cutsceneData.getDialogCount()) {
            // Cutscene finished
            endCutscene();
        } else {
            System.out.println("[Cutscene] Advanced to dialog " + (currentDialogIndex + 1) +
                    "/" + cutsceneData.getDialogCount());
        }
    }

    private void endCutscene() {
        System.out.println("[Cutscene] Finished: " + cutsceneData.getCutsceneId());

        // Stop music if it was playing
        if (cutsceneData.hasMusic()) {
            AudioManager.getInstance().stopMusic();
        }

        // Go to next screen
        if (nextScreen != null) {
            game.setScreen(nextScreen);
        }
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
        // Stop music when leaving screen
        if (cutsceneData.hasMusic()) {
            AudioManager.getInstance().stopMusic();
        }
    }

    @Override
    public void dispose() {
        if (contentTexture != null) {
            contentTexture.dispose();
        }
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        speakerFont.dispose();
    }
}
