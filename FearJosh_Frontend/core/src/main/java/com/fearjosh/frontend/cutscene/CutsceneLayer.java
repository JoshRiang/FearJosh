package com.fearjosh.frontend.cutscene;

/**
 * Represents a single image layer in a cutscene with position and animation.
 * Multiple layers can be stacked to create parallax or complex visual effects.
 */
public class CutsceneLayer {
    private final String imagePath;
    private final float startX; // Starting X position (0-1, relative to screen width)
    private final float startY; // Starting Y position (0-1, relative to screen height)
    private final float startScale; // Starting scale (1.0 = normal size)
    private final CutsceneAnimationType zoomAnimation;
    private final CutsceneAnimationType panAnimation;
    private final float animationDuration; // Duration in seconds
    private final float zoomAmount; // How much to zoom (e.g., 0.5 = zoom from 1.0 to 1.5)
    private final float panAmount; // How much to pan in pixels

    /**
     * Builder for creating cutscene layers with various configurations.
     */
    public static class Builder {
        private String imagePath;
        private float startX = 0f;
        private float startY = 0f;
        private float startScale = 1.0f;
        private CutsceneAnimationType zoomAnimation = CutsceneAnimationType.NONE;
        private CutsceneAnimationType panAnimation = CutsceneAnimationType.NONE;
        private float animationDuration = 5.0f; // Default 5 seconds
        private float zoomAmount = 0.3f; // Default zoom 30%
        private float panAmount = 200f; // Default pan 200 pixels

        public Builder(String imagePath) {
            this.imagePath = imagePath;
        }

        /**
         * Set starting position (0-1 range, relative to screen).
         * 0,0 = bottom-left, 1,1 = top-right
         */
        public Builder position(float x, float y) {
            this.startX = x;
            this.startY = y;
            return this;
        }

        /**
         * Set starting scale (1.0 = normal, 0.5 = half size, 2.0 = double size).
         */
        public Builder scale(float scale) {
            this.startScale = scale;
            return this;
        }

        /**
         * Set zoom animation.
         * 
         * @param zoomType Type of zoom (ZOOM_IN or ZOOM_OUT)
         * @param amount   How much to zoom (0.3 = 30% zoom)
         */
        public Builder zoom(CutsceneAnimationType zoomType, float amount) {
            if (zoomType == CutsceneAnimationType.ZOOM_IN || zoomType == CutsceneAnimationType.ZOOM_OUT) {
                this.zoomAnimation = zoomType;
                this.zoomAmount = amount;
            }
            return this;
        }

        /**
         * Set pan/slide animation.
         * 
         * @param panType Type of pan (PAN_LEFT, PAN_RIGHT, PAN_UP, PAN_DOWN)
         * @param amount  How many pixels to pan
         */
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

        /**
         * Set animation duration in seconds.
         */
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
