package com.fearjosh.frontend.cutscene;

public class CutsceneDialog {
    private final String speakerName;
    private final String dialogText;

    public CutsceneDialog(String speakerName, String dialogText) {
        this.speakerName = speakerName;
        this.dialogText = dialogText;
    }

    public CutsceneDialog(String dialogText) {
        this.speakerName = null;
        this.dialogText = dialogText;
    }

    public String getSpeakerName() {
        return speakerName;
    }

    public String getDialogText() {
        return dialogText;
    }

    public boolean hasSpeaker() {
        return speakerName != null && !speakerName.isEmpty();
    }
}
