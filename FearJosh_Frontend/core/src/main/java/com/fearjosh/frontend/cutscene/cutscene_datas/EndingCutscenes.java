package com.fearjosh.frontend.cutscene.cutscene_datas;

import com.fearjosh.frontend.cutscene.CutsceneAnimationType;
import com.fearjosh.frontend.cutscene.CutsceneData;
import com.fearjosh.frontend.cutscene.CutsceneLayer;
import com.fearjosh.frontend.cutscene.CutsceneManager;

public class EndingCutscenes {

    public static void registerCutscenes(CutsceneManager manager) {
        CutsceneData goodEnding = new CutsceneData.Builder("ending_good")
                .withFadeIn(1.0f)
                .addLayer(new CutsceneLayer.Builder("Cutscene/1/1_3.png")
                        .zoom(CutsceneAnimationType.ZOOM_OUT, 0.1f)
                        .scale(1.0f)
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

        CutsceneData badEnding = new CutsceneData.Builder("ending_bad")
                .withFadeIn(1.0f)
                .addLayer(new CutsceneLayer.Builder("Cutscene/1/1_g2.png")
                        .zoom(CutsceneAnimationType.ZOOM_IN, 0.15f)
                        .scale(1.0f)
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

        CutsceneData gameOverEnding = new CutsceneData.Builder("game_over_cutscene")
                .withFadeIn(0.5f)
                .addLayer(new CutsceneLayer.Builder("Cutscene/1/1_g1.png")
                        .scale(1.0f)
                        .duration(5.0f)
                        .build())
                .addDialog("", "Josh menangkapmu...")
                .addDialog("", "GAME OVER")
                .withFadeOut(1.5f)
                .build();

        manager.registerCutscene("game_over_cutscene", gameOverEnding);

        System.out.println("[EndingCutscenes] Registered ending cutscenes (good, bad, game_over)");
    }
}
