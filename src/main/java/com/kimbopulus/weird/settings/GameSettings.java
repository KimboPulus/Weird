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

    private GameSettings(Path savePath, boolean audioEnabled, int musicVolume, int effectsVolume) {
        this.savePath = savePath;
        this.audioEnabled = audioEnabled;
        this.musicVolume = clamp(musicVolume);
        this.effectsVolume = clamp(effectsVolume);
    }

    public static GameSettings loadDefault() {
        Path path = Path.of("data", "settings.properties");
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
                parse(properties.getProperty("musicVolume"), 35),
                parse(properties.getProperty("effectsVolume"), 70)
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

    private void save() {
        if (savePath == null) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("audioEnabled", Boolean.toString(audioEnabled));
        properties.setProperty("musicVolume", Integer.toString(musicVolume));
        properties.setProperty("effectsVolume", Integer.toString(effectsVolume));
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
        return new GameSettings(path, true, 35, 70);
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
