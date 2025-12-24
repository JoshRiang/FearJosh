package com.fearjosh.frontend.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.List;

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

    public void start() {
        currentStepIndex = 0;
        isActive = true;
        isComplete = false;
        displayTimer = 0f;
        fadeAlpha = 0f;
        waitingForInput = false;
        System.out.println("[Tutorial] Started");
    }

    public void reset() {
        currentStepIndex = 0;
        isActive = false;
        isComplete = false;
        displayTimer = 0f;
        fadeAlpha = 0f;
        waitingForInput = false;
    }

    public void skip() {
        isActive = false;
        isComplete = true;
        System.out.println("[Tutorial] Skipped");
    }

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

        if (displayTimer < FADE_DURATION) {
            fadeAlpha = displayTimer / FADE_DURATION;
        } else {
            fadeAlpha = 1f;
        }

        if (currentStep.autoAdvance) {
            if (displayTimer >= currentStep.displayDuration) {
                nextStep();
            }
        } else {
            waitingForInput = displayTimer >= 0.5f;
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

    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font,
            float screenWidth, float screenHeight) {
        if (!isActive || isComplete || currentStepIndex >= steps.size())
            return;

        TutorialStep currentStep = steps.get(currentStepIndex);

        float boxHeight = 100f;
        float boxY = 20f;
        float boxPadding = 20f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f * fadeAlpha);
        shapeRenderer.rect(boxPadding, boxY, screenWidth - boxPadding * 2, boxHeight);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.8f, 0.1f, 0.1f, fadeAlpha);
        shapeRenderer.rect(boxPadding, boxY, screenWidth - boxPadding * 2, boxHeight);
        shapeRenderer.end();

        batch.begin();
        font.setColor(1f, 1f, 1f, fadeAlpha);

        GlyphLayout mainLayout = new GlyphLayout(font, currentStep.mainText);
        float mainX = (screenWidth - mainLayout.width) / 2f;
        float mainY = boxY + boxHeight - 25f;
        font.draw(batch, currentStep.mainText, mainX, mainY);

        if (currentStep.subtitle != null && !currentStep.subtitle.isEmpty()) {
            font.setColor(0.8f, 0.8f, 0.8f, fadeAlpha * 0.8f);
            GlyphLayout subLayout = new GlyphLayout(font, currentStep.subtitle);
            float subX = (screenWidth - subLayout.width) / 2f;
            float subY = boxY + boxHeight - 55f;
            font.draw(batch, currentStep.subtitle, subX, subY);
        }

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

    private static class TutorialStep {
        String mainText;
        String subtitle;
        float displayDuration;
        boolean autoAdvance;

        TutorialStep(String mainText, String subtitle, float duration) {
            this.mainText = mainText;
            this.subtitle = subtitle;
            this.displayDuration = duration;
            this.autoAdvance = true;
        }

        @SuppressWarnings("unused")
        TutorialStep(String mainText, String subtitle, float duration, boolean autoAdvance) {
            this.mainText = mainText;
            this.subtitle = subtitle;
            this.displayDuration = duration;
            this.autoAdvance = autoAdvance;
        }
    }
}
