package com.fearjosh.frontend.cutscene;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.ObjectMap;
import com.fearjosh.frontend.FearJosh;

/**
 * Manager for cutscenes. Stores all cutscene data and provides methods to play
 * them.
 * Use singleton pattern for easy access throughout the game.
 */
public class CutsceneManager {
    private static CutsceneManager instance;

    private final ObjectMap<String, CutsceneData> cutscenes;

    private CutsceneManager() {
        cutscenes = new ObjectMap<>();
        initializeCutscenes();
    }

    public static CutsceneManager getInstance() {
        if (instance == null) {
            instance = new CutsceneManager();
        }
        return instance;
    }

    /**
     * Initialize all cutscenes in the game.
     * Add your cutscenes here using the builder pattern.
     */
    private void initializeCutscenes() {
        // Example: Intro cutscene
        CutsceneData introCutscene = new CutsceneData.Builder("intro")
                .withImage("Cutscenes/intro_image.png")
                .withMusic("Audio/Music/intro_music.wav")
                .addDialog("Narrator", "Welcome to FearJosh...")
                .addDialog("Narrator", "A horror game where you must escape from Josh.")
                .addDialog("Narrator", "Find items, solve puzzles, and survive.")
                .build();

        cutscenes.put("intro", introCutscene);

        // Example: Game over cutscene
        CutsceneData gameOverCutscene = new CutsceneData.Builder("game_over")
                .withImage("Cutscenes/game_over_image.png")
                .withMusic("Audio/Music/game_over_music.wav")
                .addDialog("Josh", "You cannot escape me...")
                .addDialog("You have been caught. Try again?")
                .build();

        cutscenes.put("game_over", gameOverCutscene);

        // Example: Victory cutscene
        CutsceneData victoryCutscene = new CutsceneData.Builder("victory")
                .withImage("Cutscenes/victory_image.png")
                .withMusic("Audio/Music/victory_music.wav")
                .addDialog("Narrator", "You escaped!")
                .addDialog("Narrator", "Congratulations on surviving FearJosh.")
                .addDialog("Narrator", "But remember... he's always watching.")
                .build();

        cutscenes.put("victory", victoryCutscene);

        // Example: Dialog-only cutscene (no image)
        CutsceneData tutorialCutscene = new CutsceneData.Builder("tutorial")
                .addDialog("Tutorial", "THIS IS A CUTSCENE TEST")
                .addDialog("Tutorial", "Use WASD to move.")
                .addDialog("Tutorial", "Press E to interact with objects.")
                .addDialog("Tutorial", "Press Q to use items from your inventory.")
                .addDialog("Tutorial", "Press 1-7 to select inventory slots.")
                .addDialog("Tutorial", "Find batteries to recharge your flashlight.")
                .addDialog("Tutorial", "Good luck!")
                .build();

        cutscenes.put("tutorial", tutorialCutscene);

        System.out.println("[CutsceneManager] Initialized " + cutscenes.size + " cutscenes");
    }

    /**
     * Register a new cutscene.
     * 
     * @param cutsceneId   Unique ID for the cutscene
     * @param cutsceneData The cutscene data
     */
    public void registerCutscene(String cutsceneId, CutsceneData cutsceneData) {
        cutscenes.put(cutsceneId, cutsceneData);
        System.out.println("[CutsceneManager] Registered cutscene: " + cutsceneId);
    }

    /**
     * Get cutscene data by ID.
     * 
     * @param cutsceneId The cutscene ID
     * @return The cutscene data, or null if not found
     */
    public CutsceneData getCutscene(String cutsceneId) {
        return cutscenes.get(cutsceneId);
    }

    /**
     * Play a cutscene by ID.
     * 
     * @param game       The main game instance
     * @param cutsceneId The cutscene ID to play
     * @param nextScreen The screen to show after cutscene ends
     * @return true if cutscene was found and started, false otherwise
     */
    public boolean playCutscene(FearJosh game, String cutsceneId, Screen nextScreen) {
        CutsceneData data = cutscenes.get(cutsceneId);
        if (data == null) {
            System.err.println("[CutsceneManager] Cutscene not found: " + cutsceneId);
            return false;
        }

        System.out.println("[CutsceneManager] Playing cutscene: " + cutsceneId);
        game.setScreen(new CutsceneScreen(game, data, nextScreen));
        return true;
    }

    /**
     * Check if a cutscene exists.
     * 
     * @param cutsceneId The cutscene ID
     * @return true if cutscene exists, false otherwise
     */
    public boolean hasCutscene(String cutsceneId) {
        return cutscenes.containsKey(cutsceneId);
    }

    /**
     * Get the number of registered cutscenes.
     * 
     * @return The number of cutscenes
     */
    public int getCutsceneCount() {
        return cutscenes.size;
    }
}
