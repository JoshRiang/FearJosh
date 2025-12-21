
package com.fearjosh.frontend.cutscene.cutscene_datas.nol;

import com.fearjosh.frontend.cutscene.CutsceneAnimationType;
import com.fearjosh.frontend.cutscene.CutsceneData;
import com.fearjosh.frontend.cutscene.CutsceneManager;
import com.fearjosh.frontend.cutscene.CutsceneLayer;

/**
 * Cutscene data untuk chapter 0 (intro story).
 */
public class nol {

    /**
     * Register semua cutscene chapter 0 ke CutsceneManager.
     * Dipanggil dari CutsceneManager.initializeCutscenes().
     */
    public static void registerCutscenes(CutsceneManager manager) {
        CutsceneData nol_satu = new CutsceneData.Builder("0_1")
                // Background - slow pan
                .withMusic("Audio/Music/sad_cutscene_piano_violin.wav")
                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_1.jpg")
                        .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                        .duration(15.0f)
                        .build())
                // Middle layer - zoom
                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_2.png")
                        .position(20, -60)
                        .scale(0.3f)
                        .zoom(CutsceneAnimationType.ZOOM_OUT, 0.05f)
                        .duration(15.0f)
                        .build())
                .addDialog(
                        "Josh adalah seorang siswa pendiam dan tidak berdaya. ")
                .addDialog(
                        "Selama bertahun-tahun, ia menjadi korban bullying di sekolahnya.")
                .addDialog(
                        "Tidak ada yang membantunya, tidak teman-temannya, tidak guru-gurunya.")
                .build();

        // Register menggunakan manager yang diberikan
        manager.registerCutscene("0_1", nol_satu);
        CutsceneData nol_dua = new CutsceneData.Builder("0_2")
                // Background - slow pan
                .withMusic("Audio/Music/sad_cutscene_piano_violin.wav")
                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_1.jpg")
                        .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                        .duration(15.0f)
                        .build())
                // Middle layer - zoom
                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_2.png")
                        .position(20, -60)
                        .scale(0.3f)
                        .zoom(CutsceneAnimationType.ZOOM_OUT, 0.05f)
                        .duration(15.0f)
                        .build())
                .addDialog(
                        "Josh adalah seorang siswa pendiam dan tidak berdaya. ")
                .addDialog(
                        "Selama bertahun-tahun, ia menjadi korban bullying di sekolahnya.")
                .addDialog(
                        "Tidak ada yang membantunya, tidak teman-temannya, tidak guru-gurunya.")
                .build();

        // Register menggunakan manager yang diberikan
        manager.registerCutscene("0_2", nol_dua);
    }
}
