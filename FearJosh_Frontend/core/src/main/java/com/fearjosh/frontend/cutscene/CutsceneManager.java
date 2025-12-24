package com.fearjosh.frontend.cutscene;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.ObjectMap;
import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.cutscene.cutscene_datas.EndingCutscenes;
import com.fearjosh.frontend.cutscene.cutscene_datas.nol.nol;

public class CutsceneManager {
    private static CutsceneManager instance;

    private final ObjectMap<String, CutsceneData> cutscenes;

    private CutsceneManager() {
        cutscenes = new ObjectMap<>();
        initializeCutscenes();
    }

    public static CutsceneManager getInstance() {
        if (instance == null) {
            instance = new CutsceneManager();
        }
        return instance;
    }

    private void initializeCutscenes() {
        CutsceneData introCutscene = new CutsceneData.Builder("intro")
                .addLayer(new CutsceneLayer.Builder("Cutscenes/intro_background.png")
                        .scale(1.5f)
                        .zoom(CutsceneAnimationType.ZOOM_OUT, 0.5f)
                        .duration(8.0f)
                        .build())
                .withMusic("Audio/Music/intro_music.wav")
                .addDialog("Narrator", "Selamat datang di FearJosh...")
                .addDialog("Narrator", "Game horor dimana kamu harus kabur dari Josh.")
                .addDialog("Narrator", "Temukan item, pecahkan teka-teki, dan bertahan hidup.")
                .build();

        cutscenes.put("intro", introCutscene);

        CutsceneData gameOverCutscene = new CutsceneData.Builder("game_over")
                .addLayer(new CutsceneLayer.Builder("Cutscenes/dark_hallway.png")
                        .position(0f, 0f)
                        .scale(1.2f)
                        .pan(CutsceneAnimationType.PAN_LEFT, 150f)
                        .duration(10.0f)
                        .build())
                .addLayer(new CutsceneLayer.Builder("Cutscenes/josh_face.png")
                        .position(0.3f, 0.2f)
                        .scale(0.8f)
                        .zoom(CutsceneAnimationType.ZOOM_IN, 0.4f)
                        .duration(10.0f)
                        .build())
                .withMusic("Audio/Music/game_over_music.wav")
                .addDialog("Josh", "Kamu tidak bisa lari dariku...")
                .addDialog("Kamu telah tertangkap. Coba lagi?")
                .build();

        cutscenes.put("game_over", gameOverCutscene);

        CutsceneData victoryCutscene = new CutsceneData.Builder("victory")
                .addLayer(new CutsceneLayer.Builder("Cutscenes/escape_door.png")
                        .position(0.5f, 0.5f)
                        .scale(1.3f)
                        .zoom(CutsceneAnimationType.ZOOM_OUT, 0.3f)
                        .pan(CutsceneAnimationType.PAN_RIGHT, 100f)
                        .duration(12.0f)
                        .build())
                .withMusic("Audio/Music/victory_music.wav")
                .addDialog("Narrator", "Kamu berhasil kabur!")
                .addDialog("Narrator", "Selamat atas keberhasilanmu bertahan di FearJosh.")
                .addDialog("Narrator", "Tapi ingat... dia selalu mengawasimu.")
                .build();

        cutscenes.put("victory", victoryCutscene);

        CutsceneData tutorialCutscene = new CutsceneData.Builder("tutorial")
                .addDialog("Tutorial", "Gunakan WASD untuk bergerak.")
                .addDialog("Tutorial", "Tekan E untuk berinteraksi dengan objek.")
                .addDialog("Tutorial", "Tekan Q untuk menggunakan item dari inventory.")
                .addDialog("Tutorial", "Tekan 1-7 untuk memilih slot inventory.")
                .addDialog("Tutorial", "Temukan baterai untuk mengisi ulang sentermu.")
                .addDialog("Tutorial", "Semoga berhasil!")
                .build();

        cutscenes.put("tutorial", tutorialCutscene);

        CutsceneData encounterCutscene = new CutsceneData.Builder("first_encounter")
                .addLayer(new CutsceneLayer.Builder("Cutscenes/corridor_background.png")
                        .position(0f, 0f)
                        .scale(1.0f)
                        .pan(CutsceneAnimationType.PAN_RIGHT, 80f)
                        .duration(15.0f)
                        .build())
                .addLayer(new CutsceneLayer.Builder("Cutscenes/shadows.png")
                        .position(0.2f, 0.1f)
                        .scale(1.1f)
                        .zoom(CutsceneAnimationType.ZOOM_IN, 0.3f)
                        .duration(15.0f)
                        .build())
                .addLayer(new CutsceneLayer.Builder("Cutscenes/player_silhouette.png")
                        .position(0.7f, 0.3f)
                        .scale(0.9f)
                        .pan(CutsceneAnimationType.PAN_LEFT, 120f)
                        .duration(15.0f)
                        .build())
                .withMusic("Audio/Music/tension_music.wav")
                .addDialog("Kamu merasakan ada yang mengawasimu...")
                .addDialog("Udara terasa makin dingin.")
                .addDialog("Kamu mendengar langkah kaki di belakangmu.")
                .addDialog("LARI!")
                .build();

        cutscenes.put("first_encounter", encounterCutscene);

        nol.registerCutscenes(this);
        EndingCutscenes.registerCutscenes(this);

        System.out.println("[CutsceneManager] Initialized " + cutscenes.size + " cutscenes");
    }

    public void registerCutscene(String cutsceneId, CutsceneData cutsceneData) {
        cutscenes.put(cutsceneId, cutsceneData);
        System.out.println("[CutsceneManager] Registered cutscene: " + cutsceneId);
    }

    public CutsceneData getCutscene(String cutsceneId) {
        return cutscenes.get(cutsceneId);
    }

    public boolean playCutscene(FearJosh game, String cutsceneId, Screen nextScreen) {
        CutsceneData data = cutscenes.get(cutsceneId);
        if (data == null) {
            System.err.println("[CutsceneManager] Cutscene not found: " + cutsceneId);
            return false;
        }

        System.out.println("[CutsceneManager] Playing cutscene: " + cutsceneId);
        game.setScreen(new CutsceneScreen(game, data, nextScreen));
        return true;
    }

    public Screen createCutsceneScreen(FearJosh game, String cutsceneId, Screen nextScreen) {
        CutsceneData data = cutscenes.get(cutsceneId);
        if (data == null) {
            System.err.println("[CutsceneManager] Cutscene not found: " + cutsceneId);
            return null;
        }

        return new CutsceneScreen(game, data, nextScreen);
    }

    public boolean hasCutscene(String cutsceneId) {
        return cutscenes.containsKey(cutsceneId);
    }

    public int getCutsceneCount() {
        return cutscenes.size;
    }
}
