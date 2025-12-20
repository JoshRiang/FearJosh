package com.fearjosh.frontend.cutscene;

/**
 * Represents a single dialog entry in a cutscene.
 * Each dialog has a speaker name and the text to display.
 */
public class CutsceneDialog {
    private final String speakerName;
    private final String dialogText;

    /**
     * Create a dialog with speaker name and text.
     * 
     * @param speakerName Name of the speaker (e.g., "Josh", "Narrator", "Player")
     * @param dialogText  The text to display
     */
    public CutsceneDialog(String speakerName, String dialogText) {
        this.speakerName = speakerName;
        this.dialogText = dialogText;
    }

    /**
     * Create a dialog with only text (no speaker name shown).
     * 
     * @param dialogText The text to display
     */
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
