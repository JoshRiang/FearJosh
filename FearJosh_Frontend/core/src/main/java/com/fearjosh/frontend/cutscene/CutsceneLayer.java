package com.fearjosh.frontend.cutscene;

public class CutsceneLayer {
    private final String imagePath;
    private final float startX;
    private final float startY;
    private final float startScale;
    private final CutsceneAnimationType zoomAnimation;
    private final CutsceneAnimationType panAnimation;
    private final float animationDuration;
    private final float zoomAmount;
    private final float panAmount;

    public static class Builder {
        private String imagePath;
        private float startX = 0f;
        private float startY = 0f;
        private float startScale = 1.0f;
        private CutsceneAnimationType zoomAnimation = CutsceneAnimationType.NONE;
        private CutsceneAnimationType panAnimation = CutsceneAnimationType.NONE;
        private float animationDuration = 5.0f;
        private float zoomAmount = 0.3f;
        private float panAmount = 200f;

        public Builder(String imagePath) {
            this.imagePath = imagePath;
        }

        public Builder position(float x, float y) {
            this.startX = x;
            this.startY = y;
            return this;
        }

        public Builder scale(float scale) {
            this.startScale = scale;
            return this;
        }

        public Builder zoom(CutsceneAnimationType zoomType, float amount) {
            if (zoomType == CutsceneAnimationType.ZOOM_IN || zoomType == CutsceneAnimationType.ZOOM_OUT) {
                this.zoomAnimation = zoomType;
                this.zoomAmount = amount;
            }
            return this;
        }

        public Builder pan(CutsceneAnimationType panType, float amount) {
            if (panType == CutsceneAnimationType.PAN_LEFT ||
                    panType == CutsceneAnimationType.PAN_RIGHT ||
                    panType == CutsceneAnimationType.PAN_UP ||
                    panType == CutsceneAnimationType.PAN_DOWN) {
                this.panAnimation = panType;
                this.panAmount = amount;
            }
            return this;
        }

        public Builder duration(float seconds) {
            this.animationDuration = seconds;
            return this;
        }

        public CutsceneLayer build() {
            return new CutsceneLayer(this);
        }
    }

    private CutsceneLayer(Builder builder) {
        this.imagePath = builder.imagePath;
        this.startX = builder.startX;
        this.startY = builder.startY;
        this.startScale = builder.startScale;
        this.zoomAnimation = builder.zoomAnimation;
        this.panAnimation = builder.panAnimation;
        this.animationDuration = builder.animationDuration;
        this.zoomAmount = builder.zoomAmount;
        this.panAmount = builder.panAmount;
    }

    public String getImagePath() {
        return imagePath;
    }

    public float getStartX() {
        return startX;
    }

    public float getStartY() {
        return startY;
    }

    public float getStartScale() {
        return startScale;
    }

    public CutsceneAnimationType getZoomAnimation() {
        return zoomAnimation;
    }

    public CutsceneAnimationType getPanAnimation() {
        return panAnimation;
    }

    public float getAnimationDuration() {
        return animationDuration;
    }

    public float getZoomAmount() {
        return zoomAmount;
    }

    public float getPanAmount() {
        return panAmount;
    }

    public boolean hasZoomAnimation() {
        return zoomAnimation != CutsceneAnimationType.NONE;
    }

    public boolean hasPanAnimation() {
        return panAnimation != CutsceneAnimationType.NONE;
    }
}
