package com.kimbopulus.weird.progression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ProgressionProfile {
    public static final int SANCTUARY_UNLOCK_XP = 100;

    private final Path savePath;
    private int focusXp;

    private ProgressionProfile(Path savePath, int focusXp) {
        this.savePath = savePath;
        this.focusXp = Math.max(0, focusXp);
    }

    public static ProgressionProfile loadDefault() {
        return load(Path.of("data", "progress.properties"));
    }

    public static ProgressionProfile inMemory() {
        return new ProgressionProfile(null, 0);
    }

    public static ProgressionProfile load(Path path) {
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException | NumberFormatException ignored) {
                return new ProgressionProfile(path, 0);
            }
        }

        int focusXp;
        try {
            focusXp = Integer.parseInt(properties.getProperty("focusXp", "0"));
        } catch (NumberFormatException exception) {
            focusXp = 0;
        }
        return new ProgressionProfile(path, focusXp);
    }

    public int focusXp() {
        return focusXp;
    }

    public boolean sanctuaryUnlocked() {
        return focusXp >= SANCTUARY_UNLOCK_XP;
    }

    public void addFocusXp(int amount) {
        if (amount <= 0) {
            return;
        }
        focusXp += amount;
        save();
    }

    private void save() {
        if (savePath == null) {
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("focusXp", Integer.toString(focusXp));
        try {
            Path parent = savePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream output = Files.newOutputStream(savePath)) {
                properties.store(output, "Weird progression");
            }
        } catch (IOException ignored) {
            // Progression failure must not interrupt a running simulation.
        }
    }
}
