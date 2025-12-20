package com.fearjosh.frontend.cutscene;

import com.fearjosh.frontend.FearJosh;
import com.fearjosh.frontend.screen.MainMenuScreen;

/**
 * Example usage demonstrating how to use the cutscene system.
 * 
 * BASIC USAGE:
 * ============
 * 
 * 1. Create a cutscene using the Builder pattern:
 * 
 * CutsceneData myCutscene = new CutsceneData.Builder("my_cutscene_id")
 * .withImage("Cutscenes/my_image.png") // Optional: add image
 * .withMusic("Audio/Music/my_music.wav") // Optional: add music
 * .addDialog("Speaker", "Dialog text here...") // Add dialogs
 * .addDialog("Another dialog without speaker")
 * .build();
 * 
 * 2. Register it with CutsceneManager:
 * 
 * CutsceneManager.getInstance().registerCutscene("my_cutscene_id", myCutscene);
 * 
 * 3. Play it:
 * 
 * CutsceneManager.getInstance().playCutscene(game, "my_cutscene_id",
 * nextScreen);
 * 
 * 
 * EXAMPLES:
 * =========
 */
public class CutsceneUsageExample {

    /**
     * Example 1: Simple dialog-only cutscene (no image, no music)
     */
    public static void createSimpleCutscene() {
        CutsceneData simple = new CutsceneData.Builder("simple_dialog")
                .addDialog("Narrator", "This is a simple cutscene.")
                .addDialog("Narrator", "It has no image or music.")
                .addDialog("Just press SPACE to continue.")
                .build();

        CutsceneManager.getInstance().registerCutscene("simple_dialog", simple);
    }

    /**
     * Example 2: Cutscene with image and music
     */
    public static void createFullCutscene() {
        CutsceneData full = new CutsceneData.Builder("full_cutscene")
                .withImage("Cutscenes/story_image.png")
                .withMusic("Audio/Music/story_music.wav")
                .addDialog("Josh", "Welcome to my domain...")
                .addDialog("Josh", "You will never escape!")
                .addDialog("Player", "We'll see about that!")
                .build();

        CutsceneManager.getInstance().registerCutscene("full_cutscene", full);
    }

    /**
     * Example 3: Play cutscene from MainMenuScreen
     */
    public static void playIntroFromMenu(FearJosh game) {
        // Create the cutscene
        CutsceneData intro = new CutsceneData.Builder("game_intro")
                .withImage("Cutscenes/intro.png")
                .withMusic("Audio/Music/intro.wav")
                .addDialog("Narrator", "Many years ago, in a dark place...")
                .addDialog("Narrator", "A creature known as Josh was born.")
                .addDialog("Narrator", "Now you must face him.")
                .build();

        CutsceneManager.getInstance().registerCutscene("game_intro", intro);

        // Play it - after cutscene, go to PlayScreen
        // CutsceneManager.getInstance().playCutscene(game, "game_intro", new
        // PlayScreen(game));
    }

    /**
     * Example 4: Play cutscene before game over
     */
    public static void playGameOverCutscene(FearJosh game) {
        // This would typically be called from PlayScreen when player dies
        CutsceneData gameOver = new CutsceneData.Builder("death_cutscene")
                .withImage("Cutscenes/josh_caught_player.png")
                .withMusic("Audio/Music/death_music.wav")
                .addDialog("Josh", "I told you...")
                .addDialog("Josh", "You cannot escape me!")
                .addDialog("GAME OVER")
                .build();

        CutsceneManager.getInstance().registerCutscene("death_cutscene", gameOver);

        // Play it - after cutscene, return to main menu
        // CutsceneManager.getInstance().playCutscene(game, "death_cutscene", new
        // MainMenuScreen(game));
    }

    /**
     * Example 5: Room discovery cutscene
     */
    public static void createRoomDiscoveryCutscene(String roomName, String description) {
        CutsceneData roomDiscovery = new CutsceneData.Builder("room_" + roomName)
                .withImage("Cutscenes/Rooms/" + roomName + ".png")
                .addDialog("You entered: " + roomName)
                .addDialog(description)
                .build();

        CutsceneManager.getInstance().registerCutscene("room_" + roomName, roomDiscovery);
    }

    /**
     * Example 6: Item pickup story cutscene
     */
    public static void createItemPickupCutscene() {
        CutsceneData keyPickup = new CutsceneData.Builder("found_key")
                .withImage("Cutscenes/Items/key.png")
                .addDialog("Narrator", "You found a mysterious key.")
                .addDialog("Narrator", "Perhaps it opens one of the locked doors?")
                .build();

        CutsceneManager.getInstance().registerCutscene("found_key", keyPickup);
    }

    /**
     * INTEGRATION GUIDE:
     * ==================
     * 
     * In MainMenuScreen (Play button):
     * ---------------------------------
     * playBtn.addListener(new ClickListener() {
     * 
     * @Override
     *           public void clicked(InputEvent event, float x, float y) {
     *           // Play intro cutscene before starting game
     *           Screen playScreen = new PlayScreen(game);
     *           CutsceneManager.getInstance().playCutscene(game, "intro",
     *           playScreen);
     *           }
     *           });
     * 
     * 
     *           In PlayScreen (when player dies):
     *           ----------------------------------
     *           if (playerDied) {
     *           Screen mainMenu = new MainMenuScreen(game);
     *           CutsceneManager.getInstance().playCutscene(game, "game_over",
     *           mainMenu);
     *           }
     * 
     * 
     *           In PlayScreen (when entering new room):
     *           ----------------------------------------
     *           if (enteredNewRoom) {
     *           String roomId = "room_" + currentRoom.getName();
     *           if (CutsceneManager.getInstance().hasCutscene(roomId)) {
     *           // Return to current screen after cutscene
     *           CutsceneManager.getInstance().playCutscene(game, roomId, this);
     *           }
     *           }
     * 
     * 
     *           Creating cutscenes at runtime:
     *           ------------------------------
     *           CutsceneData dynamic = new CutsceneData.Builder("dynamic_cutscene")
     *           .addDialog("Speaker", "This was created at runtime!")
     *           .addDialog("You can create cutscenes based on game state.")
     *           .build();
     * 
     *           Screen nextScreen = this; // or whatever screen you want
     *           game.setScreen(new CutsceneScreen(game, dynamic, nextScreen));
     */
}
