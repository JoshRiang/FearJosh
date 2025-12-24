package com.fearjosh.frontend.cutscene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.fearjosh.frontend.core.GameManager;
import com.fearjosh.frontend.systems.AudioManager;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class InGameCutscene {

    public enum CutsceneType {
        GYM_ENCOUNTER,
        OBJECTIVE_CHANGE,
        ESCAPE_SUCCESS,
        CUSTOM
    }

    private boolean isActive;
    private CutsceneType currentType;
    private int currentPhase;
    private float phaseTimer;
    private boolean isComplete;

    private List<DialogLine> dialogLines;
    private int currentDialogIndex;
    private String currentDisplayText;
    private float textRevealTimer;
    private static final float TEXT_REVEAL_SPEED = 0.03f;
    private boolean textFullyRevealed;

    private float fadeAlpha;
    private float shakeAmount;
    private float shakeTimer;
    
    private float joshX, joshY;
    private float joshTargetX, joshTargetY;
    private float joshAlpha;
    private boolean joshVisible;
    private float joshAnimTimer;
    private static final float JOSH_SPRITE_SIZE = 150f;
    private static final float JOSH_FACE_SIZE = 200f;

    private Texture injuredTexture;
    private Texture joshFaceTexture;
    private Texture joshSpriteTexture;
    
    private Animation<TextureRegion> joshAnimation;
    private static final int JOSH_SPRITE_FRAMES = 4;
    private static final float JOSH_ANIM_FRAME_DURATION = 0.15f;

    private Runnable onComplete;

    public InGameCutscene() {
        isActive = false;
        isComplete = false;
        dialogLines = new ArrayList<>();
        currentDialogIndex = 0;
        currentDisplayText = "";
        textRevealTimer = 0f;
        textFullyRevealed = false;
        fadeAlpha = 0f;
        shakeAmount = 0f;
        shakeTimer = 0f;
        joshAlpha = 0f;
        joshVisible = false;
    }

    public void startCutscene(CutsceneType type, Runnable onComplete) {
        this.currentType = type;
        this.onComplete = onComplete;
        this.isActive = true;
        this.isComplete = false;
        this.currentPhase = 0;
        this.phaseTimer = 0f;
        this.currentDialogIndex = 0;
        this.fadeAlpha = 0f;
        this.textFullyRevealed = false;
        this.currentDisplayText = "";

        dialogLines.clear();
        setupCutscene(type);

        GameManager.getInstance().setCurrentState(GameManager.GameState.CUTSCENE);

        System.out.println("[InGameCutscene] Started: " + type);
    }

    private void setupCutscene(CutsceneType type) {
        switch (type) {
            case GYM_ENCOUNTER:
                setupGymEncounter();
                break;
            case OBJECTIVE_CHANGE:
                setupObjectiveChange();
                break;
            case ESCAPE_SUCCESS:
                setupEscapeSuccess();
                break;
            default:
                break;
        }
    }

    private void setupGymEncounter() {
        System.out.println("[InGameCutscene] Setting up GYM_ENCOUNTER cutscene...");
        
        try {
            
            
            if (joshSpriteTexture == null) {
                if (Gdx.files.internal("Sprite/Enemy/josh_chasing_down.png").exists()) {
                    joshSpriteTexture = new Texture("Sprite/Enemy/josh_chasing_down.png");
                    
                    int frameWidth = joshSpriteTexture.getWidth() / JOSH_SPRITE_FRAMES;
                    int frameHeight = joshSpriteTexture.getHeight();
                    TextureRegion[][] frames = TextureRegion.split(joshSpriteTexture, frameWidth, frameHeight);
                    joshAnimation = new Animation<>(JOSH_ANIM_FRAME_DURATION, frames[0]);
                    joshAnimation.setPlayMode(Animation.PlayMode.LOOP);
                    
                    System.out.println("[InGameCutscene] Loaded josh_chasing_down.png as 4-frame sprite sheet");
                } else if (Gdx.files.internal("josh.png").exists()) {
                    joshSpriteTexture = new Texture("josh.png");
                    joshAnimation = null;
                    System.out.println("[InGameCutscene] Loaded josh.png fallback for cutscene (no animation)");
                }
            }
            
            if (joshFaceTexture == null) {
                if (Gdx.files.internal("joshInvertedFace.png").exists()) {
                    joshFaceTexture = new Texture("joshInvertedFace.png");
                    System.out.println("[InGameCutscene] Loaded joshInvertedFace.png for jumpscare");
                }
            }
        } catch (Exception e) {
            System.err.println("[InGameCutscene] Could not load textures: " + e.getMessage());
            e.printStackTrace();
        }
        
        joshVisible = false;
        joshAlpha = 0f;
        joshAnimTimer = 0f;
        joshY = 600f;
        joshTargetY = 200f;

        dialogLines.add(new DialogLine(null, "*Suara langkah kaki menggema di gym...*", 2.5f));
        dialogLines.add(new DialogLine(null, "*Kamu merasakan hawa dingin yang aneh...*", 2.0f));
        dialogLines.add(new DialogLine(null, "GRRRRAAAAAHHH!!!", 1.5f, true));
        dialogLines.add(new DialogLine("Josh", "...", 1.0f));
        dialogLines.add(new DialogLine(null, "*Josh menerkammu dengan kecepatan luar biasa!*", 2.0f));
        dialogLines.add(new DialogLine(null, "*Kamu pingsan...*", 3.0f, true, true));

        try {
            String soundPath = "Audio/Effect/monster_roar_sound_effect.wav";
            if (Gdx.files.internal(soundPath).exists()) {
                AudioManager.getInstance().playSound(soundPath);
                System.out.println("[InGameCutscene] Playing monster roar sound");
            } else {
                System.err.println("[InGameCutscene] Sound file not found: " + soundPath);
            }
        } catch (Exception e) {
            System.err.println("[InGameCutscene] Could not play sound: " + e.getMessage());
        }
        
        System.out.println("[InGameCutscene] GYM_ENCOUNTER setup complete, " + dialogLines.size() + " dialog lines");
    }

    private void setupObjectiveChange() {
        dialogLines.add(new DialogLine(null, "*Kamu terbangun di lantai yang dingin...*", 2.5f));
        dialogLines.add(new DialogLine("Jonatan", "Ugh... kepala ku...", 2.0f));
        dialogLines.add(new DialogLine("Jonatan", "Josh... dia sudah tidak bisa diselamatkan.", 3.0f));
        dialogLines.add(new DialogLine("Jonatan", "Aku harus keluar dari sini!", 2.5f));
        dialogLines.add(new DialogLine(null, "Objektif baru: KABUR dari sekolah!", 3.0f));
        dialogLines.add(new DialogLine(null, "Temukan kunci untuk membuka pintu keluar.", 3.0f));
    }

    private void setupEscapeSuccess() {
        dialogLines.add(new DialogLine(null, "*Pintu terbuka...*", 2.0f));
        dialogLines.add(new DialogLine("Jonatan", "Akhirnya... aku bebas!", 2.5f));
        dialogLines.add(new DialogLine(null, "Kamu berhasil kabur dari sekolah.", 3.0f));
    }

    public void update(float delta) {
        if (!isActive || isComplete)
            return;

        phaseTimer += delta;
        
        if (currentType == CutsceneType.GYM_ENCOUNTER) {
            joshAnimTimer += delta;
            
            if (currentDialogIndex >= 2) {
                joshVisible = true;
                if (joshAlpha < 1f) {
                    joshAlpha += delta * 2f;
                    if (joshAlpha > 1f) joshAlpha = 1f;
                }
            }
        }

        if (currentDialogIndex < dialogLines.size()) {
            DialogLine currentLine = dialogLines.get(currentDialogIndex);
            
            if (!textFullyRevealed) {
                textRevealTimer += delta;
                int charsToShow = (int) (textRevealTimer / TEXT_REVEAL_SPEED);
                if (charsToShow >= currentLine.text.length()) {
                    currentDisplayText = currentLine.text;
                    textFullyRevealed = true;
                } else {
                    currentDisplayText = currentLine.text.substring(0, charsToShow);
                }
            }

            if (currentLine.screenShake && phaseTimer < 0.5f) {
                shakeAmount = 5f * (1f - phaseTimer * 2f);
                shakeTimer += delta * 30f;
            } else {
                shakeAmount = 0f;
            }

            if (currentLine.fadeToBlack) {
                fadeAlpha = Math.min(1f, phaseTimer / 2f);
            }

            boolean shouldAdvance = false;
            if (currentLine.duration > 0 && phaseTimer >= currentLine.duration && textFullyRevealed) {
                shouldAdvance = true;
            } else if (textFullyRevealed && 
                    (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || 
                     Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                     Gdx.input.justTouched())) {
                shouldAdvance = true;
            }

            if (shouldAdvance) {
                nextDialog();
            }
        } else {
            completeCutscene();
        }
    }

    private void nextDialog() {
        currentDialogIndex++;
        phaseTimer = 0f;
        textRevealTimer = 0f;
        textFullyRevealed = false;
        currentDisplayText = "";
        shakeAmount = 0f;

        if (currentDialogIndex >= dialogLines.size()) {
            completeCutscene();
        }
    }

    private void completeCutscene() {
        isComplete = true;
        isActive = false;
        fadeAlpha = 0f;
        shakeAmount = 0f;

        System.out.println("[InGameCutscene] Complete: " + currentType);
        
        Runnable completionCallback = onComplete;
        CutsceneType completedType = currentType;

        if (completionCallback != null) {
            completionCallback.run();
        }
        
        if (!isActive) {
            GameManager gm = GameManager.getInstance();
            if (gm.getCurrentState() == GameManager.GameState.CUTSCENE) {
                if (gm.hasMetJosh()) {
                    gm.setCurrentState(GameManager.GameState.PLAYING);
                    System.out.println("[InGameCutscene] State restored to PLAYING");
                } else {
                    gm.setCurrentState(GameManager.GameState.STORY);
                    System.out.println("[InGameCutscene] State restored to STORY");
                }
            }
        }
    }

    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font,
            float screenWidth, float screenHeight) {
        if (!isActive)
            return;

        float shakeX = shakeAmount > 0 ? (float) Math.sin(shakeTimer) * shakeAmount : 0f;
        float shakeY = shakeAmount > 0 ? (float) Math.cos(shakeTimer * 1.3f) * shakeAmount : 0f;

        if (fadeAlpha > 0) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, fadeAlpha);
            shapeRenderer.rect(0, 0, screenWidth, screenHeight);
            shapeRenderer.end();
        }

        if (currentDialogIndex < dialogLines.size() && fadeAlpha < 0.9f) {
            DialogLine currentLine = dialogLines.get(currentDialogIndex);
            
            if (currentType == CutsceneType.GYM_ENCOUNTER && joshVisible && joshSpriteTexture != null) {
                batch.begin();
                batch.setColor(1f, 1f, 1f, joshAlpha);
                
                if (currentDialogIndex == 2 && joshFaceTexture != null) {
                    float faceX = (screenWidth - JOSH_FACE_SIZE) / 2f + shakeX * 2f;
                    float faceY = screenHeight * 0.4f + shakeY * 2f;
                    batch.draw(joshFaceTexture, faceX, faceY, JOSH_FACE_SIZE, JOSH_FACE_SIZE);
                }
                else if (currentDialogIndex >= 2) {
                    float animSpeed = 800f;
                    if (joshY > joshTargetY) {
                        joshY -= animSpeed * Gdx.graphics.getDeltaTime();
                        if (joshY < joshTargetY) joshY = joshTargetY;
                    }
                    
                    float distanceProgress = 1f - (joshY - joshTargetY) / 400f;
                    float scale = 1f + distanceProgress * 0.8f;
                    scale = Math.max(1f, Math.min(scale, 2f));
                    
                    float spriteW = JOSH_SPRITE_SIZE * scale;
                    float spriteH = JOSH_SPRITE_SIZE * scale;
                    float spriteX = (screenWidth - spriteW) / 2f + shakeX;
                    float spriteY = joshY + shakeY;
                    
                    if (joshAnimation != null) {
                        TextureRegion frame = joshAnimation.getKeyFrame(joshAnimTimer);
                        batch.draw(frame, spriteX, spriteY, spriteW, spriteH);
                    } else {
                        batch.draw(joshSpriteTexture, spriteX, spriteY, spriteW, spriteH);
                    }
                }
                
                batch.setColor(Color.WHITE);
                batch.end();
            }
            
            float boxHeight = 120f;
            float boxY = 30f + shakeY;
            float boxPadding = 30f;
            float boxX = boxPadding + shakeX;
            float boxWidth = screenWidth - boxPadding * 2;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.05f, 0.05f, 0.08f, 0.9f);
            shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            Gdx.gl.glLineWidth(2f);
            shapeRenderer.setColor(0.7f, 0.1f, 0.1f, 1f);
            shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);
            shapeRenderer.end();
            Gdx.gl.glLineWidth(1f);

            batch.begin();
            
            float textY = boxY + boxHeight - 25f;
            if (currentLine.speaker != null && !currentLine.speaker.isEmpty()) {
                font.setColor(0.9f, 0.3f, 0.3f, 1f);
                font.draw(batch, currentLine.speaker + ":", boxX + 20f, textY);
                textY -= 30f;
            }

            font.setColor(Color.WHITE);
            font.draw(batch, currentDisplayText, boxX + 20f, textY);

            if (textFullyRevealed) {
                font.setColor(0.5f, 0.5f, 0.5f, 0.7f);
                String hint = "[Tekan SPACE untuk lanjut]";
                GlyphLayout layout = new GlyphLayout(font, hint);
                font.draw(batch, hint, boxX + boxWidth - layout.width - 20f, boxY + 25f);
            }

            batch.end();
        }

        if (currentType == CutsceneType.GYM_ENCOUNTER && injuredTexture != null && 
            currentDialogIndex >= 4 && fadeAlpha > 0.3f) {
            batch.begin();
            batch.setColor(1f, 1f, 1f, Math.min(1f, fadeAlpha * 1.5f));
            float imgW = 300f;
            float imgH = 300f;
            batch.draw(injuredTexture, 
                    (screenWidth - imgW) / 2f + shakeX, 
                    (screenHeight - imgH) / 2f + shakeY, 
                    imgW, imgH);
            batch.setColor(Color.WHITE);
            batch.end();
        }
    }

    public float getShakeX() {
        return shakeAmount > 0 ? (float) Math.sin(shakeTimer) * shakeAmount : 0f;
    }

    public float getShakeY() {
        return shakeAmount > 0 ? (float) Math.cos(shakeTimer * 1.3f) * shakeAmount : 0f;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void skip() {
        completeCutscene();
    }

    public void dispose() {
        if (injuredTexture != null) {
            injuredTexture.dispose();
            injuredTexture = null;
        }
        if (joshFaceTexture != null) {
            joshFaceTexture.dispose();
            joshFaceTexture = null;
        }
        if (joshSpriteTexture != null) {
            joshSpriteTexture.dispose();
            joshSpriteTexture = null;
        }
    }

    private static class DialogLine {
        String speaker;
        String text;
        float duration;
        boolean screenShake;
        boolean fadeToBlack;

        DialogLine(String speaker, String text, float duration) {
            this(speaker, text, duration, false, false);
        }

        DialogLine(String speaker, String text, float duration, boolean screenShake) {
            this(speaker, text, duration, screenShake, false);
        }

        DialogLine(String speaker, String text, float duration, boolean screenShake, boolean fadeToBlack) {
            this.speaker = speaker;
            this.text = text;
            this.duration = duration;
            this.screenShake = screenShake;
            this.fadeToBlack = fadeToBlack;
        }
    }
}
