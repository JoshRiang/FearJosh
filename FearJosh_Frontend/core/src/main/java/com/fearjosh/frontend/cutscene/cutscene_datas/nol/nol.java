
package com.fearjosh.frontend.cutscene.cutscene_datas.nol;

import com.fearjosh.frontend.cutscene.CutsceneAnimationType;
import com.fearjosh.frontend.cutscene.CutsceneData;
import com.fearjosh.frontend.cutscene.CutsceneLayer;
import com.fearjosh.frontend.cutscene.CutsceneManager;

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
                .withFadeIn(3)
                .withMusic("Audio/Music/sad_cutscene_piano_violin.wav")
                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_1.jpg")
                        .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                        .duration(15.0f)
                        .build())
                // Middle layer - zoom
                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_1_1.png")
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
                .withFadeIn(0.5f)
                .withMusic("Audio/Cutscene/0/0_2.wav")
                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_2.png")
                        .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                        .scale(0.4f)
                        .duration(15.0f)
                        .build())
                // Middle layer - zoom
                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_2_1.png")
                        // .position(20, -60)
                        // .scale(0.3f)
                        .zoom(CutsceneAnimationType.ZOOM_OUT, 0.05f)
                        .duration(15.0f)
                        .build())
                .addDialog(
                        "Pada suatu malam, para siswa-siswi merayakan pesta kelulusan di Aula besar sekolah.")
                .addDialog(
                        "Malam yang meriah untuk semua orang, kecuali Josh.")
                .build();

        // Register menggunakan manager yang diberikan
        manager.registerCutscene("0_2", nol_dua);

        CutsceneData nol_tiga = new CutsceneData.Builder("0_3")
                // Background - slow pan
                // No music - continue from cutscene 2
                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_3.png")
                        .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                        .scale(0.4f)
                        .duration(15.0f)
                        .build())
                // Middle layer - zoom
                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_3_1.png")
                        // .position(20, -60)
                        // .scale(0.3f)
                        .zoom(CutsceneAnimationType.ZOOM_OUT, 0.05f)
                        .duration(15.0f)
                        .build())
                .addDialog(
                        "Josh dengan keputusasaan dan mata yang gelap,")
                .addDialog(
                        "mengunci seluruh pintu Aula dari luar.")
                .build();

        // Register menggunakan manager yang diberikan
        manager.registerCutscene("0_3", nol_tiga);

        CutsceneData nol_empat = new CutsceneData.Builder("0_4")
                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_4.png")
                        .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                        .scale(0.4f)
                        .duration(15.0f)
                        .build())
                .addDialog(
                        "Dia menyiram minyak yang banyak di sekitar Aula.")
                .addDialog(
                        "Dan menyalakan api.")
                .build();

        // Register menggunakan manager yang diberikan
        manager.registerCutscene("0_4", nol_empat);

        CutsceneData nol_lima = new CutsceneData.Builder("0_5")
                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_5.png")
                        .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                        .scale(0.4f)
                        .duration(15.0f)
                        .build())
                .addDialog(
                        "Api memakan 213 jiwa di malam itu,")
                .addDialog(
                        "termasuk Josh.")
                .addDialog(
                        "Sehingga warga sekitar menyebut malam itu adalah malam abu.")
                .build();

        // Register menggunakan manager yang diberikan
        manager.registerCutscene("0_5", nol_lima);

        CutsceneData nol_enam = new CutsceneData.Builder("0_6")
                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_6.jpg")
                        .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                        .scale(0.4f)
                        .duration(15.0f)
                        .build())
                .addDialog(
                        "Konon, Josh tidak benar-benar meninggal,")
                .addDialog(
                        "tapi berubah menjadi sosok monster api mengerikan.")
                .build();

        // Register menggunakan manager yang diberikan
        manager.registerCutscene("0_6", nol_enam);

        CutsceneData nol_tujuh = new CutsceneData.Builder("0_7")
                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_7.jpg")
                        .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                        .scale(0.4f)
                        .duration(15.0f)
                        .build())
                .addDialog(
                        "Tidak ada orang yang berani memasuki sekolah itu lagi,")
                .addDialog(
                        "Kini telah menjadi terbengkalai dan angker.")
                .build();

        // Register menggunakan manager yang diberikan
        manager.registerCutscene("0_7", nol_tujuh);
    }
}
