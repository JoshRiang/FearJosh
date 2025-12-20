package com.fearjosh.frontend.cutscene;

import com.badlogic.gdx.utils.Array;

/**
 * Represents the content type for a cutscene story.
 */
enum CutsceneContentType {
    IMAGE, // Static image
    VIDEO, // Video file
    NONE // No visual content (just dialog)
}

/**
 * Contains all data for a single cutscene story.
 * A cutscene can have visual content (image/video), background music, and
 * multiple dialogs.
 */
public class CutsceneData {
    private final String cutsceneId;
    private final CutsceneContentType contentType;
    private final String contentPath; // Path to image or video file
    private final String musicPath; // Path to background music (optional)
    private final Array<CutsceneDialog> dialogs;

    /**
     * Builder pattern for creating CutsceneData with various configurations.
     */
    public static class Builder {
        private String cutsceneId;
        private CutsceneContentType contentType = CutsceneContentType.NONE;
        private String contentPath = null;
        private String musicPath = null;
        private Array<CutsceneDialog> dialogs = new Array<>();

        public Builder(String cutsceneId) {
            this.cutsceneId = cutsceneId;
        }

        /**
         * Set image as visual content.
         */
        public Builder withImage(String imagePath) {
            this.contentType = CutsceneContentType.IMAGE;
            this.contentPath = imagePath;
            return this;
        }

        /**
         * Set video as visual content.
         */
        public Builder withVideo(String videoPath) {
            this.contentType = CutsceneContentType.VIDEO;
            this.contentPath = videoPath;
            return this;
        }

        /**
         * Set background music.
         */
        public Builder withMusic(String musicPath) {
            this.musicPath = musicPath;
            return this;
        }

        /**
         * Add a single dialog.
         */
        public Builder addDialog(CutsceneDialog dialog) {
            this.dialogs.add(dialog);
            return this;
        }

        /**
         * Add a dialog with speaker name and text.
         */
        public Builder addDialog(String speakerName, String dialogText) {
            this.dialogs.add(new CutsceneDialog(speakerName, dialogText));
            return this;
        }

        /**
         * Add a dialog with only text (no speaker).
         */
        public Builder addDialog(String dialogText) {
            this.dialogs.add(new CutsceneDialog(dialogText));
            return this;
        }

        /**
         * Add multiple dialogs at once.
         */
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
}
