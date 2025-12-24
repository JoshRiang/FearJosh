
package com.fearjosh.frontend.cutscene.cutscene_datas.nol;

import com.fearjosh.frontend.cutscene.CutsceneAnimationType;
import com.fearjosh.frontend.cutscene.CutsceneData;
import com.fearjosh.frontend.cutscene.CutsceneLayer;
import com.fearjosh.frontend.cutscene.CutsceneManager;

public class nol {

        public static void registerCutscenes(CutsceneManager manager) {
                CutsceneData nol_satu = new CutsceneData.Builder("0_1")
                                .withFadeIn(3)
                                .withMusic("Audio/Music/sad_cutscene_piano_violin.wav")
                                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_1.jpg")
                                                .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                                                .duration(15.0f)
                                                .build())
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

                manager.registerCutscene("0_1", nol_satu);

                CutsceneData nol_dua = new CutsceneData.Builder("0_2")
                                .withFadeIn(0.5f)
                                .withMusic("Audio/Cutscene/0/0_2.wav")
                                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_2.png")
                                                .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                                                .scale(0.4f)
                                                .duration(15.0f)
                                                .build())
                                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_2_1.png")
                                                .zoom(CutsceneAnimationType.ZOOM_OUT, 0.05f)
                                                .duration(15.0f)
                                                .build())
                                .addDialog(
                                                "Pada suatu malam, para siswa-siswi merayakan pesta kelulusan di Aula besar sekolah.")
                                .addDialog(
                                                "Malam yang meriah untuk semua orang, kecuali Josh.")
                                .build();

                manager.registerCutscene("0_2", nol_dua);

                CutsceneData nol_tiga = new CutsceneData.Builder("0_3")
                                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_3.png")
                                                .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                                                .scale(0.4f)
                                                .duration(15.0f)
                                                .build())
                                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_3_1.png")
                                                .zoom(CutsceneAnimationType.ZOOM_OUT, 0.05f)
                                                .duration(15.0f)
                                                .build())
                                .addDialog(
                                                "Di luar Aula, mata Josh tiba-tiba menjadi hitam. Urat-urat wajahnya menonjol hitam.")
                                .addDialog(
                                                "Detak jantung kencang. THUMP. THUMP.")
                                .build();

                manager.registerCutscene("0_3", nol_tiga);

                CutsceneData nol_empat = new CutsceneData.Builder("0_4")
                                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_4.jpg")
                                                .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                                                .scale(0.4f)
                                                .duration(15.0f)
                                                .build())
                                .addDialog(
                                                "Siluet tubuh Josh meledak membesar.")
                                .addDialog(
                                                "Dan mulai menerkam ke semua orang  dengan membabi buta")
                                .build();

                manager.registerCutscene("0_4", nol_empat);

                CutsceneData nol_lima = new CutsceneData.Builder("0_5")
                                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_5.jpg")
                                                .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                                                .scale(0.4f)
                                                .duration(15.0f)
                                                .build())
                                .addDialog(
                                                "Meja terlempar. Siswa berlarian menabrak kamera. Layar merah.")
                                .addDialog(
                                                "Malam itu, teriakan di mana-mana, dan darah berhamburan")
                                .addDialog(
                                                "Gosip langsung tersebar, bahwa siswa bernama Josh yang melakukan semuanya itu")
                                .build();

                manager.registerCutscene("0_5", nol_lima);

                CutsceneData nol_enam = new CutsceneData.Builder("0_6")
                                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_6.jpg")
                                                .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                                                .scale(0.4f)
                                                .duration(15.0f)
                                                .build())
                                .addDialog(
                                                "Di luar, polisi menembakan lampu sorot ke arah gedung sekolah.")
                                .addDialog(
                                                "Membuat Josh menjadi lebih tenang, dan bersembunyi di dalam sekolah ")
                                .build();

                manager.registerCutscene("0_6", nol_enam);

                CutsceneData nol_tujuh = new CutsceneData.Builder("0_7")
                                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_7.jpg")
                                                .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                                                .scale(0.4f)
                                                .duration(15.0f)
                                                .build())
                                .addDialog(
                                                "Berkat upaya dari polisi, masih ada yang berhasil selamat dan keluar dari sekolah.")
                                .addDialog(
                                                "Namun, Josh dikunci di dalam sekolah, dan mayat-mayat masih belum bisa dievakuasi")
                                .withFadeOut(5)
                                .build();

                manager.registerCutscene("0_7", nol_tujuh);

                CutsceneData nol_delapan = new CutsceneData.Builder("0_8")
                                .withFadeIn(0.5f)
                                .withMusic("Cutscene/0/0_8.wav")
                                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_8.jpg")
                                                .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                                                .scale(0.4f)
                                                .duration(15.0f)
                                                .build())
                                .addDialog(
                                                "Besok harinya, Jonathan (kakak kandung Josh) khawatir dengan adiknya yang belum pulang.")
                                .addDialog(
                                                "Sehingga, iya langsung pergi ke sekolah untuk mencari Josh, yang konon katanya sudah berubah menjadi monster.")
                                .build();

                manager.registerCutscene("0_8", nol_delapan);

                CutsceneData nol_sembilan = new CutsceneData.Builder("0_9")
                                .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_9.jpg")
                                                .zoom(CutsceneAnimationType.ZOOM_IN, 0.05f)
                                                .scale(0.4f)
                                                .duration(15.0f)
                                                .build())
                                .addDialog("Jonathan",
                                                "Jangan khawatir Josh, kita akan pulang bersama")
                                .build();

                manager.registerCutscene("0_9", nol_sembilan);
        }
}
