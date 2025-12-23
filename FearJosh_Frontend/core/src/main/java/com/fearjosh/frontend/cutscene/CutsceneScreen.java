package com.fearjosh.frontend.cutscene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.systems.AudioManager;

/**
 * Screen for displaying cutscenes with animated layered images, dialogs, and
 * music.
 * User presses SPACE to advance through dialogs.
 */
public class CutsceneScreen implements Screen {

    private static final float VIRTUAL_WIDTH = 800f;
    private static final float VIRTUAL_HEIGHT = 600f;

    // Dialog box dimensions
    private static final float DIALOG_BOX_HEIGHT = 150f;
    private static final float DIALOG_BOX_PADDING = 20f;
    private static final float TEXT_PADDING = 15f;

    // Layer animation state
    private class LayerState {
        Texture texture;
        float currentX;
        float currentY;
        float currentScale;
        float elapsedTime;

        LayerState(Texture texture, float startX, float startY, float startScale) {
            this.texture = texture;
            this.currentX = startX;
            this.currentY = startY;
            this.currentScale = startScale;
            this.elapsedTime = 0f;
        }
    }

    private final FearJosh game;
    private final CutsceneData cutsceneData;
    private final Screen nextScreen; // Screen to show after cutscene ends

    private OrthographicCamera camera;
    private FitViewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private BitmapFont speakerFont;

    private Texture contentTexture; // For legacy single image content
    private Array<LayerState> layerStates; // For animated layers
    private int currentDialogIndex;
    private boolean canAdvance; // Prevent skipping too fast
    private float timeSinceLastAdvance;
    private static final float ADVANCE_COOLDOWN = 0.2f;

    // Typing animation
    private static final float TYPING_SPEED = 15f; // Characters per second
    private float typingTimer;
    private int visibleCharacters;
    private boolean typingComplete;
    private Sound typingSound;
    private long typingSoundId; // Track sound instance for stopping

    // Fade transitions
    private enum FadeState {
        FADE_IN, NONE, FADE_OUT
    }

    private FadeState fadeState;
    private float fadeTimer;
    private float fadeAlpha; // 0 = transparent, 1 = black
    private float fadeInDuration; // From cutsceneData
    private float fadeOutDuration; // From cutsceneData

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
        this.layerStates = new Array<>();

        // Initialize typing animation
        this.typingTimer = 0f;
        this.visibleCharacters = 0;
        this.typingComplete = false;
        this.typingSoundId = -1;

        // Initialize fade transition from cutsceneData
        this.fadeInDuration = cutsceneData.getFadeInDuration();
        this.fadeOutDuration = cutsceneData.getFadeOutDuration();
        this.fadeState = (fadeInDuration > 0) ? FadeState.FADE_IN : FadeState.NONE;
        this.fadeTimer = 0f;
        this.fadeAlpha = (fadeInDuration > 0) ? 1.0f : 0f; // Start fully black if fade in enabled

        // Load typing sound effect
        try {
            this.typingSound = Gdx.audio.newSound(Gdx.files.internal("Audio/Effect/typing_sound_effect.wav"));
        } catch (Exception e) {
            System.err.println("[Cutscene] Failed to load typing sound: " + e.getMessage());
            this.typingSound = null;
        }

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

        // Load layered images if available
        if (cutsceneData.hasLayers()) {
            for (CutsceneLayer layer : cutsceneData.getLayers()) {
                try {
                    Texture texture = new Texture(layer.getImagePath());
                    // Store position as offset from center (can be negative)
                    float startX = layer.getStartX();
                    float startY = layer.getStartY();
                    LayerState state = new LayerState(texture, startX, startY, layer.getStartScale());
                    layerStates.add(state);
                    System.out.println("[Cutscene] Loaded layer: " + layer.getImagePath());
                } catch (Exception e) {
                    System.err.println("[Cutscene] Failed to load layer: " + layer.getImagePath());
                }
            }
        } else if (cutsceneData.hasContent() && cutsceneData.getContentType() == CutsceneContentType.IMAGE) {
            // Legacy single image support
            try {
                contentTexture = new Texture(cutsceneData.getContentPath());
            } catch (Exception e) {
                System.err.println("[Cutscene] Failed to load image: " + cutsceneData.getContentPath());
                contentTexture = null;
            }
        }

        System.out.println("[Cutscene] Created: " + cutsceneData.getCutsceneId());
        System.out.println("[Cutscene] Total dialogs: " + cutsceneData.getDialogCount());
        System.out.println("[Cutscene] Layers: " + layerStates.size);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);

        // Smart music handling:
        // - If cutscene has music, play it (stop previous if different)
        // - If cutscene has no music, continue playing previous music
        if (cutsceneData.hasMusic()) {
            String newMusicPath = cutsceneData.getMusicPath();
            String currentMusicPath = AudioManager.getInstance().getCurrentMusicPath();

            // Only change music if it's different from what's playing
            if (!newMusicPath.equals(currentMusicPath)) {
                AudioManager.getInstance().stopMusic();
                AudioManager.getInstance().playMusic(newMusicPath, true);
                System.out.println("[Cutscene] Playing new music: " + newMusicPath);
            } else {
                System.out.println("[Cutscene] Continuing same music: " + newMusicPath);
            }
        } else {
            // No music specified - continue previous music if any
            String currentMusicPath = AudioManager.getInstance().getCurrentMusicPath();
            if (currentMusicPath != null) {
                System.out.println("[Cutscene] Continuing previous music: " + currentMusicPath);
            } else {
                System.out.println("[Cutscene] No music playing");
            }
        }

        System.out.println("[Cutscene] Started: " + cutsceneData.getCutsceneId());
    }

    @Override
    public void render(float delta) {
        // Update fade transition
        if (fadeState == FadeState.FADE_IN && fadeInDuration > 0) {
            fadeTimer += delta;
            fadeAlpha = Math.max(0f, 1f - (fadeTimer / fadeInDuration));
            if (fadeTimer >= fadeInDuration) {
                fadeState = FadeState.NONE;
                fadeAlpha = 0f;
            }
        } else if (fadeState == FadeState.FADE_OUT && fadeOutDuration > 0) {
            fadeTimer += delta;
            fadeAlpha = Math.min(1f, fadeTimer / fadeOutDuration);
            if (fadeTimer >= fadeOutDuration) {
                // Fade out complete - transition to next screen
                actualEndCutscene();
                return;
            }
        }

        // Update cooldown timer
        timeSinceLastAdvance += delta;
        if (timeSinceLastAdvance >= ADVANCE_COOLDOWN) {
            canAdvance = true;
        }

        // Update typing animation
        if (!typingComplete) {
            typingTimer += delta;
            CutsceneDialog currentDialog = cutsceneData.getDialog(currentDialogIndex);
            if (currentDialog != null) {
                String fullText = currentDialog.getDialogText();
                int targetChars = (int) (typingTimer * TYPING_SPEED);
                visibleCharacters = Math.min(targetChars, fullText.length());

                // Start looping typing sound when dialog starts
                if (typingSoundId == -1 && typingSound != null && visibleCharacters > 0) {
                    typingSoundId = typingSound.loop(0.5f); // Loop at 50% volume
                }

                if (visibleCharacters >= fullText.length()) {
                    typingComplete = true;
                    // Stop typing sound when complete
                    if (typingSoundId != -1 && typingSound != null) {
                        typingSound.stop(typingSoundId);
                        typingSoundId = -1;
                    }
                }
            }
        }

        // Handle input - SPACE to advance
        if (canAdvance && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (!typingComplete) {
                // Skip typing animation
                CutsceneDialog currentDialog = cutsceneData.getDialog(currentDialogIndex);
                if (currentDialog != null) {
                    visibleCharacters = currentDialog.getDialogText().length();
                    typingComplete = true;
                    // Stop typing sound when skipping
                    if (typingSoundId != -1 && typingSound != null) {
                        typingSound.stop(typingSoundId);
                        typingSoundId = -1;
                    }
                }
            } else {
                // Advance to next dialog
                advanceDialog();
            }
        }

        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Update and render animated layers
        batch.begin();
        if (layerStates.size > 0) {
            renderAnimatedLayers(delta);
        } else if (contentTexture != null) {
            // Legacy single image rendering
            float imageWidth = VIRTUAL_WIDTH;
            float imageHeight = VIRTUAL_HEIGHT - DIALOG_BOX_HEIGHT;
            batch.draw(contentTexture, 0, DIALOG_BOX_HEIGHT, imageWidth, imageHeight);
        }
        batch.end();

        // Render dialog box
        renderDialogBox();

        // Render current dialog text
        renderDialogText();

        // Render fade overlay
        if (fadeAlpha > 0f) {
            renderFadeOverlay();
        }
    }

    private void renderAnimatedLayers(float delta) {
        float contentHeight = VIRTUAL_HEIGHT - DIALOG_BOX_HEIGHT;

        for (int i = 0; i < layerStates.size; i++) {
            LayerState state = layerStates.get(i);
            CutsceneLayer layer = cutsceneData.getLayers().get(i);

            // Update animation
            state.elapsedTime += delta;
            float progress = Math.min(state.elapsedTime / layer.getAnimationDuration(), 1.0f);

            // Calculate current position and scale based on animation
            float drawX = state.currentX;
            float drawY = state.currentY + DIALOG_BOX_HEIGHT;
            float drawScale = state.currentScale;

            // Apply zoom animation
            if (layer.hasZoomAnimation()) {
                float zoomProgress = progress;
                if (layer.getZoomAnimation() == CutsceneAnimationType.ZOOM_IN) {
                    drawScale = state.currentScale + (layer.getZoomAmount() * zoomProgress);
                } else if (layer.getZoomAnimation() == CutsceneAnimationType.ZOOM_OUT) {
                    drawScale = state.currentScale - (layer.getZoomAmount() * zoomProgress);
                }
            }

            // Apply pan animation
            if (layer.hasPanAnimation()) {
                float panProgress = progress;
                switch (layer.getPanAnimation()) {
                    case PAN_LEFT:
                        drawX = state.currentX - (layer.getPanAmount() * panProgress);
                        break;
                    case PAN_RIGHT:
                        drawX = state.currentX + (layer.getPanAmount() * panProgress);
                        break;
                    case PAN_UP:
                        drawY = state.currentY + DIALOG_BOX_HEIGHT + (layer.getPanAmount() * panProgress);
                        break;
                    case PAN_DOWN:
                        drawY = state.currentY + DIALOG_BOX_HEIGHT - (layer.getPanAmount() * panProgress);
                        break;
                    default:
                        // ZOOM_IN, ZOOM_OUT, NONE - not used for pan animation
                        break;
                }
            }

            // Draw the layer with current transformations
            float layerWidth = state.texture.getWidth() * drawScale;
            float layerHeight = state.texture.getHeight() * drawScale;

            // Always center-based: position is offset from center in pixels
            // Start from center of screen
            float centerX = VIRTUAL_WIDTH / 2;
            float centerY = DIALOG_BOX_HEIGHT + (contentHeight / 2);

            // Apply offset (can be negative for left/down)
            drawX = centerX + state.currentX - (layerWidth / 2);
            drawY = centerY + state.currentY - (layerHeight / 2);

            batch.draw(state.texture, drawX, drawY, layerWidth, layerHeight);
        }
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

        // Draw dialog text with typing animation
        String fullText = currentDialog.getDialogText();
        String displayText = fullText.substring(0, Math.min(visibleCharacters, fullText.length()));
        font.draw(batch, displayText,
                textX, textY, textWidth, Align.left, true);

        // Draw progress indicator (e.g., "Press SPACE to continue" or "3/10")
        String progressText;
        if (!typingComplete) {
            progressText = "Tekan SPASI untuk skip... (" +
                    (currentDialogIndex + 1) + "/" + cutsceneData.getDialogCount() + ")";
        } else if (currentDialogIndex < cutsceneData.getDialogCount() - 1) {
            progressText = "Tekan SPASI untuk lanjut... (" +
                    (currentDialogIndex + 1) + "/" + cutsceneData.getDialogCount() + ")";
        } else {
            progressText = "Tekan SPASI untuk selesai... (" +
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

    private void renderFadeOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, fadeAlpha);
        shapeRenderer.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void advanceDialog() {
        currentDialogIndex++;
        canAdvance = false;
        timeSinceLastAdvance = 0f;

        // Reset typing animation for next dialog
        typingTimer = 0f;
        visibleCharacters = 0;
        typingComplete = false;
        typingSoundId = -1;

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

        // Start fade out transition if enabled
        if (fadeOutDuration > 0) {
            fadeState = FadeState.FADE_OUT;
            fadeTimer = 0f;
        } else {
            // No fade out - end immediately
            actualEndCutscene();
        }
    }

    private void actualEndCutscene() {
        // Don't stop music - let it continue to next screen
        // Music will be managed by the next screen's show() method

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
        // Don't stop music - let it continue to next screen
    }

    @Override
    public void dispose() {
        if (contentTexture != null) {
            contentTexture.dispose();
        }
        // Dispose all layer textures
        for (LayerState state : layerStates) {
            if (state.texture != null) {
                state.texture.dispose();
            }
        }
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        speakerFont.dispose();
        if (typingSound != null) {
            typingSound.dispose();
        }
    }
}
