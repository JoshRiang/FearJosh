package com.fearjosh.frontend.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Tutorial overlay system for displaying tutorial messages during STORY state.
 * Shows dialog-style text overlays teaching player the controls.
 */
public class TutorialOverlay {

    private static TutorialOverlay instance;

    // Tutorial steps
    private List<TutorialStep> steps;
    private int currentStepIndex;
    private boolean isActive;
    private boolean isComplete;

    // Display timing
    private float displayTimer;
    private float fadeAlpha;
    private static final float FADE_DURATION = 0.5f;

    // State
    private boolean waitingForInput;

    public static TutorialOverlay getInstance() {
        if (instance == null) {
            instance = new TutorialOverlay();
        }
        return instance;
    }

    private TutorialOverlay() {
        steps = new ArrayList<>();
        currentStepIndex = 0;
        isActive = false;
        isComplete = false;
        displayTimer = 0f;
        fadeAlpha = 0f;
        waitingForInput = false;

        initializeTutorialSteps();
    }

    private void initializeTutorialSteps() {
        // Tutorial steps based on game requirements
        steps.add(new TutorialStep(
                "Gunakan WASD untuk bergerak.",
                "Movement",
                3.0f));

        steps.add(new TutorialStep(
                "Tekan F untuk menyalakan/mematikan senter.",
                "Flashlight",
                3.0f));

        steps.add(new TutorialStep(
                "Tekan E untuk berinteraksi dengan objek.",
                "(Interaksi akan aktif setelah kamu bertemu Josh)",
                3.0f));

        steps.add(new TutorialStep(
                "Tekan Q untuk menggunakan item dari inventory.",
                "Gunakan tombol 1-7 untuk memilih slot.",
                3.0f));

        steps.add(new TutorialStep(
                "Masuklah ke dalam sekolah...",
                "Temukan Josh.",
                4.0f));
    }

    /**
     * Start the tutorial sequence
     */
    public void start() {
        currentStepIndex = 0;
        isActive = true;
        isComplete = false;
        displayTimer = 0f;
        fadeAlpha = 0f;
        waitingForInput = false;
        System.out.println("[Tutorial] Started");
    }

    /**
     * Reset tutorial state (for new game)
     */
    public void reset() {
        currentStepIndex = 0;
        isActive = false;
        isComplete = false;
        displayTimer = 0f;
        fadeAlpha = 0f;
        waitingForInput = false;
    }

    /**
     * Skip the entire tutorial
     */
    public void skip() {
        isActive = false;
        isComplete = true;
        System.out.println("[Tutorial] Skipped");
    }

    /**
     * Update tutorial state
     */
    public void update(float delta) {
        if (!isActive || isComplete)
            return;

        if (currentStepIndex >= steps.size()) {
            isComplete = true;
            isActive = false;
            System.out.println("[Tutorial] Complete");
            return;
        }

        TutorialStep currentStep = steps.get(currentStepIndex);
        displayTimer += delta;

        // Fade in
        if (displayTimer < FADE_DURATION) {
            fadeAlpha = displayTimer / FADE_DURATION;
        } else {
            fadeAlpha = 1f;
        }

        // Auto-advance or wait for input
        if (currentStep.autoAdvance) {
            if (displayTimer >= currentStep.displayDuration) {
                nextStep();
            }
        } else {
            waitingForInput = displayTimer >= 0.5f; // Small delay before allowing skip
            if (waitingForInput && (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                    || Gdx.input.isKeyJustPressed(Input.Keys.ENTER))) {
                nextStep();
            }
        }
    }

    private void nextStep() {
        currentStepIndex++;
        displayTimer = 0f;
        fadeAlpha = 0f;
        waitingForInput = false;

        if (currentStepIndex >= steps.size()) {
            isComplete = true;
            isActive = false;
            System.out.println("[Tutorial] Complete");
        } else {
            System.out.println("[Tutorial] Step " + (currentStepIndex + 1) + "/" + steps.size());
        }
    }

    /**
     * Render tutorial overlay
     */
    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font,
            float screenWidth, float screenHeight) {
        if (!isActive || isComplete || currentStepIndex >= steps.size())
            return;

        TutorialStep currentStep = steps.get(currentStepIndex);

        // Draw semi-transparent background box at bottom
        float boxHeight = 100f;
        float boxY = 20f;
        float boxPadding = 20f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f * fadeAlpha);
        shapeRenderer.rect(boxPadding, boxY, screenWidth - boxPadding * 2, boxHeight);
        shapeRenderer.end();

        // Draw border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.8f, 0.1f, 0.1f, fadeAlpha);
        shapeRenderer.rect(boxPadding, boxY, screenWidth - boxPadding * 2, boxHeight);
        shapeRenderer.end();

        // Draw text
        batch.begin();
        font.setColor(1f, 1f, 1f, fadeAlpha);

        // Main text
        GlyphLayout mainLayout = new GlyphLayout(font, currentStep.mainText);
        float mainX = (screenWidth - mainLayout.width) / 2f;
        float mainY = boxY + boxHeight - 25f;
        font.draw(batch, currentStep.mainText, mainX, mainY);

        // Subtitle text
        if (currentStep.subtitle != null && !currentStep.subtitle.isEmpty()) {
            font.setColor(0.8f, 0.8f, 0.8f, fadeAlpha * 0.8f);
            GlyphLayout subLayout = new GlyphLayout(font, currentStep.subtitle);
            float subX = (screenWidth - subLayout.width) / 2f;
            float subY = boxY + boxHeight - 55f;
            font.draw(batch, currentStep.subtitle, subX, subY);
        }

        // Continue hint
        if (!currentStep.autoAdvance && waitingForInput) {
            font.setColor(0.6f, 0.6f, 0.6f, fadeAlpha * 0.7f);
            String hintText = "[SPACE / ENTER untuk lanjut]";
            GlyphLayout hintLayout = new GlyphLayout(font, hintText);
            float hintX = (screenWidth - hintLayout.width) / 2f;
            float hintY = boxY + 25f;
            font.draw(batch, hintText, hintX, hintY);
        }

        batch.end();
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isComplete() {
        return isComplete;
    }

    /**
     * Tutorial step data class
     */
    private static class TutorialStep {
        String mainText;
        String subtitle;
        float displayDuration;
        boolean autoAdvance;

        TutorialStep(String mainText, String subtitle, float duration) {
            this.mainText = mainText;
            this.subtitle = subtitle;
            this.displayDuration = duration;
            this.autoAdvance = true; // Auto-advance by default for smoother experience
        }

        @SuppressWarnings("unused") // Alternative constructor for custom auto-advance behavior
        TutorialStep(String mainText, String subtitle, float duration, boolean autoAdvance) {
            this.mainText = mainText;
            this.subtitle = subtitle;
            this.displayDuration = duration;
            this.autoAdvance = autoAdvance;
        }
    }
}
