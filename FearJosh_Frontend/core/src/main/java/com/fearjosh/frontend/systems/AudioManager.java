package com.fearjosh.frontend.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import java.util.HashMap;
import java.util.Map;

/**
 * AudioManager - Singleton untuk manage semua audio dalam game
 * Handles background music, sound effects, dan volume control
 */
public class AudioManager {

    private static AudioManager INSTANCE;

    // Volume settings (0.0 to 1.0)
    private float masterVolume = 1.0f;
    private float musicVolume = 0.7f;
    private float sfxVolume = 0.8f;

    // Audio caches
    private Map<String, Music> musicCache = new HashMap<>();
    private Map<String, Sound> soundCache = new HashMap<>();

    // Current playing music
    private Music currentMusic;
    private String currentMusicPath;

    // Mute states
    private boolean musicMuted = false;
    private boolean sfxMuted = false;

    private AudioManager() {
    }

    public static synchronized AudioManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AudioManager();
        }
        return INSTANCE;
    }

    // ============ MUSIC METHODS ============

    /**
     * Play background music (looping)
     * 
     * @param musicPath Path to music file (e.g., "Audio/Music/ambient.mp3")
     * @param loop      Whether to loop the music
     */
    public void playMusic(String musicPath, boolean loop) {
        if (musicMuted)
            return;

        // Check if file exists first
        if (!Gdx.files.internal(musicPath).exists()) {
            System.err.println("[AudioManager] Music file not found: " + musicPath);
            System.err.println("[AudioManager] Please add the file to assets folder");
            return;
        }

        // Stop current music if different
        if (currentMusic != null && !musicPath.equals(currentMusicPath)) {
            currentMusic.stop();
        }

        // Load and play new music
        try {
            Music music = musicCache.get(musicPath);
            if (music == null) {
                music = Gdx.audio.newMusic(Gdx.files.internal(musicPath));
                musicCache.put(musicPath, music);
            }

            music.setLooping(loop);
            music.setVolume(musicVolume * masterVolume);
            music.play();

            currentMusic = music;
            currentMusicPath = musicPath;

            System.out.println("[AudioManager] Playing music: " + musicPath);
        } catch (Exception e) {
            System.err.println("[AudioManager] Failed to load music: " + musicPath);
            e.printStackTrace();
        }
    }

    /**
     * Stop current music
     */
    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
            currentMusicPath = null;
        }
    }

    /**
     * Get the path of currently playing music
     * 
     * @return Current music path, or null if no music is playing
     */
    public String getCurrentMusicPath() {
        return currentMusicPath;
    }

    /**
     * Pause current music
     */
    public void pauseMusic() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.pause();
        }
    }

    /**
     * Resume paused music
     */
    public void resumeMusic() {
        if (currentMusic != null && !currentMusic.isPlaying()) {
            currentMusic.play();
        }
    }

    /**
     * Check if music is currently playing
     */
    public boolean isMusicPlaying() {
        return currentMusic != null && currentMusic.isPlaying();
    }

    // ============ SOUND EFFECTS METHODS ============

    /**
     * Play sound effect once
     * 
     * @param soundPath Path to sound file (e.g., "Audio/SFX/footstep.wav")
     * @return Sound ID for controlling playback
     */
    public long playSound(String soundPath) {
        return playSound(soundPath, 1.0f);
    }

    /**
     * Play sound effect with custom volume multiplier
     * 
     * @param soundPath        Path to sound file
     * @param volumeMultiplier Volume multiplier (0.0 to 1.0)
     * @return Sound ID for controlling playback
     */
    public long playSound(String soundPath, float volumeMultiplier) {
        if (sfxMuted)
            return -1;

        // Check if file exists first
        if (!Gdx.files.internal(soundPath).exists()) {
            System.err.println("[AudioManager] Sound file not found: " + soundPath);
            return -1;
        }

        try {
            Sound sound = soundCache.get(soundPath);
            if (sound == null) {
                sound = Gdx.audio.newSound(Gdx.files.internal(soundPath));
                soundCache.put(soundPath, sound);
            }

            float finalVolume = sfxVolume * masterVolume * volumeMultiplier;
            return sound.play(finalVolume);
        } catch (Exception e) {
            System.err.println("[AudioManager] Failed to load sound: " + soundPath);
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Play looping sound effect
     * 
     * @param soundPath Path to sound file
     * @return Sound ID for controlling playback
     */
    public long loopSound(String soundPath) {
        if (sfxMuted)
            return -1;

        // Check if file exists first
        if (!Gdx.files.internal(soundPath).exists()) {
            System.err.println("[AudioManager] Sound file not found: " + soundPath);
            return -1;
        }

        try {
            Sound sound = soundCache.get(soundPath);
            if (sound == null) {
                sound = Gdx.audio.newSound(Gdx.files.internal(soundPath));
                soundCache.put(soundPath, sound);
            }

            float finalVolume = sfxVolume * masterVolume;
            return sound.loop(finalVolume);
        } catch (Exception e) {
            System.err.println("[AudioManager] Failed to loop sound: " + soundPath);
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Stop specific sound instance
     */
    public void stopSound(String soundPath, long soundId) {
        Sound sound = soundCache.get(soundPath);
        if (sound != null) {
            sound.stop(soundId);
        }
    }

    /**
     * Stop all instances of a sound
     */
    public void stopAllSounds(String soundPath) {
        Sound sound = soundCache.get(soundPath);
        if (sound != null) {
            sound.stop();
        }
    }

    // ============ VOLUME CONTROL ============

    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0f, Math.min(1f, volume));
        updateMusicVolume();
    }

    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0f, Math.min(1f, volume));
        updateMusicVolume();
    }

    public void setSfxVolume(float volume) {
        this.sfxVolume = Math.max(0f, Math.min(1f, volume));
    }

    private void updateMusicVolume() {
        if (currentMusic != null) {
            currentMusic.setVolume(musicVolume * masterVolume);
        }
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public float getSfxVolume() {
        return sfxVolume;
    }

    // ============ MUTE CONTROL ============

    public void setMusicMuted(boolean muted) {
        this.musicMuted = muted;
        if (muted && currentMusic != null) {
            currentMusic.pause();
        } else if (!muted && currentMusic != null) {
            currentMusic.play();
        }
    }

    public void setSfxMuted(boolean muted) {
        this.sfxMuted = muted;
    }

    public boolean isMusicMuted() {
        return musicMuted;
    }

    public boolean isSfxMuted() {
        return sfxMuted;
    }

    // ============ CLEANUP ============

    /**
     * Dispose all audio resources
     */
    public void dispose() {
        // Dispose all music
        for (Music music : musicCache.values()) {
            music.dispose();
        }
        musicCache.clear();

        // Dispose all sounds
        for (Sound sound : soundCache.values()) {
            sound.dispose();
        }
        soundCache.clear();

        currentMusic = null;
        currentMusicPath = null;

        System.out.println("[AudioManager] All audio resources disposed");
    }
}
