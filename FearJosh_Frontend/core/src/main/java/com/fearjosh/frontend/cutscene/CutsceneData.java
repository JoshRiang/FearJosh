package com.fearjosh.frontend.cutscene;

import com.badlogic.gdx.utils.Array;

enum CutsceneContentType {
    IMAGE,
    VIDEO,
    NONE
}

public class CutsceneData {
    private final String cutsceneId;
    private final CutsceneContentType contentType;
    private final String contentPath;
    private final String musicPath;
    private final Array<CutsceneDialog> dialogs;
    private final Array<CutsceneLayer> layers;
    private final float fadeInDuration;
    private final float fadeOutDuration;

    public static class Builder {
        private String cutsceneId;
        private CutsceneContentType contentType = CutsceneContentType.NONE;
        private String contentPath = null;
        private String musicPath = null;
        private Array<CutsceneDialog> dialogs = new Array<>();
        private Array<CutsceneLayer> layers = new Array<>();
        private float fadeInDuration = 0f;
        private float fadeOutDuration = 0f;

        public Builder(String cutsceneId) {
            this.cutsceneId = cutsceneId;
        }

        public Builder withImage(String imagePath) {
            this.contentType = CutsceneContentType.IMAGE;
            this.contentPath = imagePath;
            this.layers.add(new CutsceneLayer.Builder(imagePath).build());
            return this;
        }

        public Builder withVideo(String videoPath) {
            this.contentType = CutsceneContentType.VIDEO;
            this.contentPath = videoPath;
            return this;
        }

        public Builder addLayer(CutsceneLayer layer) {
            this.layers.add(layer);
            this.contentType = CutsceneContentType.IMAGE;
            return this;
        }

        public Builder withMusic(String musicPath) {
            this.musicPath = musicPath;
            return this;
        }

        public Builder withFadeIn(float duration) {
            this.fadeInDuration = duration;
            return this;
        }

        public Builder withFadeOut(float duration) {
            this.fadeOutDuration = duration;
            return this;
        }

        public Builder addDialog(CutsceneDialog dialog) {
            this.dialogs.add(dialog);
            return this;
        }

        public Builder addDialog(String speakerName, String dialogText) {
            this.dialogs.add(new CutsceneDialog(speakerName, dialogText));
            return this;
        }

        public Builder addDialog(String dialogText) {
            this.dialogs.add(new CutsceneDialog(dialogText));
            return this;
        }

        public Builder addDialogs(Array<CutsceneDialog> dialogs) {
            this.dialogs.addAll(dialogs);
            return this;
        }

        public CutsceneData build() {
            if (dialogs.size == 0) {
                throw new IllegalStateException("Cutscene must have at least one dialog");
            }
            return new CutsceneData(this);
        }
    }

    private CutsceneData(Builder builder) {
        this.cutsceneId = builder.cutsceneId;
        this.contentType = builder.contentType;
        this.contentPath = builder.contentPath;
        this.musicPath = builder.musicPath;
        this.dialogs = builder.dialogs;
        this.layers = builder.layers;
        this.fadeInDuration = builder.fadeInDuration;
        this.fadeOutDuration = builder.fadeOutDuration;
    }

    public String getCutsceneId() {
        return cutsceneId;
    }

    public CutsceneContentType getContentType() {
        return contentType;
    }

    public String getContentPath() {
        return contentPath;
    }

    public String getMusicPath() {
        return musicPath;
    }

    public Array<CutsceneDialog> getDialogs() {
        return dialogs;
    }

    public int getDialogCount() {
        return dialogs.size;
    }

    public CutsceneDialog getDialog(int index) {
        if (index >= 0 && index < dialogs.size) {
            return dialogs.get(index);
        }
        return null;
    }

    public boolean hasContent() {
        return contentType != CutsceneContentType.NONE && contentPath != null;
    }

    public boolean hasMusic() {
        return musicPath != null && !musicPath.isEmpty();
    }

    public Array<CutsceneLayer> getLayers() {
        return layers;
    }

    public boolean hasLayers() {
        return layers != null && layers.size > 0;
    }

    public float getFadeInDuration() {
        return fadeInDuration;
    }

    public float getFadeOutDuration() {
        return fadeOutDuration;
    }
}
