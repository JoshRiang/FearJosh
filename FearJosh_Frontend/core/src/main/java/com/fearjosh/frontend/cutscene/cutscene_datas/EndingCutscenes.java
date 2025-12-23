package com.fearjosh.frontend.cutscene.cutscene_datas;

import com.fearjosh.frontend.cutscene.CutsceneAnimationType;
import com.fearjosh.frontend.cutscene.CutsceneData;
import com.fearjosh.frontend.cutscene.CutsceneLayer;
import com.fearjosh.frontend.cutscene.CutsceneManager;

/**
 * Ending cutscene data.
 * Plays when player successfully escapes from the school.
 */
public class EndingCutscenes {

    /**
     * Register all ending cutscenes to CutsceneManager.
     */
    public static void registerCutscenes(CutsceneManager manager) {
        // GOOD ENDING - Player escapes
        CutsceneData goodEnding = new CutsceneData.Builder("ending_good")
                .withFadeIn(1.0f)
                .withMusic("Audio/Music/victory_music.wav")
                .addLayer(new CutsceneLayer.Builder("Cutscene/ending.png")
                        .zoom(CutsceneAnimationType.ZOOM_OUT, 0.1f)
                        .scale(0.5f)
                        .duration(20.0f)
                        .build())
                .addDialog("Narrator", 
                        "Kamu berhasil keluar dari sekolah...")
                .addDialog("Narrator", 
                        "Matahari pagi menyambutmu.")
                .addDialog("Jonatan", 
                        "Maaf Josh... aku tidak bisa menyelamatkanmu.")
                .addDialog("Jonatan", 
                        "Tapi setidaknya... aku masih hidup.")
                .addDialog("Narrator", 
                        "Kamu meninggalkan sekolah itu dengan hati yang berat.")
                .addDialog("Narrator", 
                        "Kenangan akan Josh akan selalu menghantuimu...")
                .addDialog(null, 
                        "SELESAI")
                .addDialog(null, 
                        "Terima kasih sudah bermain FEAR JOSH!")
                .withFadeOut(3.0f)
                .build();

        manager.registerCutscene("ending_good", goodEnding);

        // BAD ENDING - Game Over (alternative ending variant)
        CutsceneData badEnding = new CutsceneData.Builder("ending_bad")
                .withFadeIn(1.0f)
                .withMusic("Audio/Music/game_over_music.wav")
                .addLayer(new CutsceneLayer.Builder("Sprite/Player/jonatan_injured.png")
                        .zoom(CutsceneAnimationType.ZOOM_IN, 0.15f)
                        .scale(0.5f)
                        .duration(15.0f)
                        .build())
                .addDialog("Narrator", 
                        "Josh akhirnya menangkapmu...")
                .addDialog("Narrator", 
                        "Tidak ada yang tersisa dari Jonatan.")
                .addDialog("Josh", 
                        "...")
                .addDialog(null, 
                        "TAMAT")
                .withFadeOut(2.0f)
                .build();

        manager.registerCutscene("ending_bad", badEnding);

        System.out.println("[EndingCutscenes] Registered ending cutscenes");
    }
}
