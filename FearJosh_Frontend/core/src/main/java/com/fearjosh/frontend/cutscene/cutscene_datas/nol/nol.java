
package com.fearjosh.frontend.cutscene.cutscene_datas.nol;

import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.cutscene.CutsceneAnimationType;
import com.fearjosh.frontend.cutscene.CutsceneData;
import com.fearjosh.frontend.cutscene.CutsceneManager;
import com.fearjosh.frontend.cutscene.CutsceneData.Builder;
import com.fearjosh.frontend.cutscene.CutsceneLayer;
import com.fearjosh.frontend.screen.MainMenuScreen;

public class nol {
    CutsceneData multilayer = new CutsceneData.Builder("multilayer")
            // Background - slow pan
            .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_1.jpg")
                    .pan(CutsceneAnimationType.PAN_RIGHT, 80f)
                    .duration(15.0f)
                    .build())
            // Middle layer - zoom
            .addLayer(new CutsceneLayer.Builder("Cutscene/0/0_2.png")
                    .position(0.2f, 0.3f)
                    .zoom(CutsceneAnimationType.ZOOM_IN, 0.3f)
                    .duration(15.0f)
                    .build())
            .addDialog(
                    "Josh adalah seorang siswa pendiam dan tidak berdaya. Selama bertahun-tahun, ia menjadi korban bullying di sekolahnya.")
            .addDialog(
                    "Tidak ada yang membantunya, tidak teman-temannya, tidak guru-gurunya.")
            .build();
}
