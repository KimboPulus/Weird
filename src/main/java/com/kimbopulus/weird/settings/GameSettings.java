package com.kimbopulus.weird.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class GameSettings {
    private final Path savePath;
    private boolean audioEnabled;
    private int musicVolume;
    private int effectsVolume;
    private boolean introSeen;

    private GameSettings(Path savePath, boolean audioEnabled, int musicVolume, int effectsVolume, boolean introSeen) {
        this.savePath = savePath;
        this.audioEnabled = audioEnabled;
        this.musicVolume = clamp(musicVolume);
        this.effectsVolume = clamp(effectsVolume);
        this.introSeen = introSeen;
    }

    public static GameSettings loadDefault() {
        return load(Path.of("data", "settings.properties"));
    }

    public static GameSettings load(Path path) {
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException ignored) {
                return defaults(path);
            }
        }
        return new GameSettings(
                path,
                Boolean.parseBoolean(properties.getProperty("audioEnabled", "true")),
                parse(properties.getProperty("musicVolume"), 15),
                parse(properties.getProperty("effectsVolume"), 70),
                Boolean.parseBoolean(properties.getProperty("introSeen", "false"))
        );
    }

    public static GameSettings inMemory() {
        return defaults(null);
    }

    public boolean audioEnabled() {
        return audioEnabled;
    }

    public int musicVolume() {
        return musicVolume;
    }

    public int effectsVolume() {
        return effectsVolume;
    }

    public boolean introSeen() {
        return introSeen;
    }

    public void setAudioEnabled(boolean audioEnabled) {
        this.audioEnabled = audioEnabled;
        save();
    }

    public void setMusicVolume(int musicVolume) {
        this.musicVolume = clamp(musicVolume);
        save();
    }

    public void setEffectsVolume(int effectsVolume) {
        this.effectsVolume = clamp(effectsVolume);
        save();
    }

    public void setIntroSeen(boolean introSeen) {
        this.introSeen = introSeen;
        save();
    }

    private void save() {
        if (savePath == null) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("audioEnabled", Boolean.toString(audioEnabled));
        properties.setProperty("musicVolume", Integer.toString(musicVolume));
        properties.setProperty("effectsVolume", Integer.toString(effectsVolume));
        properties.setProperty("introSeen", Boolean.toString(introSeen));
        try {
            Files.createDirectories(savePath.getParent());
            try (OutputStream output = Files.newOutputStream(savePath)) {
                properties.store(output, "Weird settings");
            }
        } catch (IOException ignored) {
            // Settings persistence is optional and must not stop the game.
        }
    }

    private static GameSettings defaults(Path path) {
        return new GameSettings(path, true, 15, 70, false);
    }

    private static int parse(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
