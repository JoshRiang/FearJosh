package com.fearjosh.frontend.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import java.util.HashMap;
import java.util.Map;

public class AudioManager {

    private static AudioManager INSTANCE;

    // Volume
    private float masterVolume = 1.0f;
    private float musicVolume = 0.7f;
    private float sfxVolume = 0.8f;

    // Caches
    private Map<String, Music> musicCache = new HashMap<>();
    private Map<String, Sound> soundCache = new HashMap<>();

    // Current music
    private Music currentMusic;
    private String currentMusicPath;

    // Mute
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

    // MUSIC

    public void playMusic(String musicPath, boolean loop) {
        if (musicMuted)
            return;

        if (!Gdx.files.internal(musicPath).exists()) {
            System.err.println("[AudioManager] Music file not found: " + musicPath);
            System.err.println("[AudioManager] Please add the file to assets folder");
            return;
        }

        if (currentMusic != null && !musicPath.equals(currentMusicPath)) {
            currentMusic.stop();
        }

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

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
            currentMusicPath = null;
        }
    }

    public String getCurrentMusicPath() {
        return currentMusicPath;
    }

    public void pauseMusic() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.pause();
        }
    }

    public void resumeMusic() {
        if (currentMusic != null && !currentMusic.isPlaying()) {
            currentMusic.play();
        }
    }

    public boolean isMusicPlaying() {
        return currentMusic != null && currentMusic.isPlaying();
    }

    // SFX

    public long playSound(String soundPath) {
        return playSound(soundPath, 1.0f);
    }

    public long playSound(String soundPath, float volumeMultiplier) {
        if (sfxMuted)
            return -1;

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

    public long loopSound(String soundPath) {
        if (sfxMuted)
            return -1;

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

    public void stopSound(String soundPath, long soundId) {
        Sound sound = soundCache.get(soundPath);
        if (sound != null) {
            sound.stop(soundId);
        }
    }

    public void stopAllSounds(String soundPath) {
        Sound sound = soundCache.get(soundPath);
        if (sound != null) {
            sound.stop();
        }
    }

    // VOLUME CONTROL

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

    // MUTE CONTROL

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

    // CLEANUP

    public void dispose() {
        for (Music music : musicCache.values()) {
            music.dispose();
        }
        musicCache.clear();

        for (Sound sound : soundCache.values()) {
            sound.dispose();
        }
        soundCache.clear();

        currentMusic = null;
        currentMusicPath = null;

        System.out.println("[AudioManager] All audio resources disposed");
    }
}
