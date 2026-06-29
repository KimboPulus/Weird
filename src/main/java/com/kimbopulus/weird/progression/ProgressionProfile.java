package com.kimbopulus.weird.progression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.EnumSet;
import java.util.Set;

public final class ProgressionProfile {
    private final Path savePath;
    private final Set<ShopItem> purchases;
    private int totalScore;
    private int tokens;

    private ProgressionProfile(Path savePath, int totalScore, int tokens, Set<ShopItem> purchases) {
        this.savePath = savePath;
        this.totalScore = Math.max(0, totalScore);
        this.tokens = Math.max(0, tokens);
        this.purchases = EnumSet.noneOf(ShopItem.class);
        this.purchases.addAll(purchases);
    }

    public static ProgressionProfile loadDefault() {
        return load(Path.of("data", "progress.properties"));
    }

    public static ProgressionProfile inMemory() {
        return new ProgressionProfile(null, 0, 0, EnumSet.noneOf(ShopItem.class));
    }

    public static ProgressionProfile load(Path path) {
        Properties properties = new Properties();
        boolean hadLegacyPurchases = false;
        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException | NumberFormatException ignored) {
                return new ProgressionProfile(path, 0, 0, EnumSet.noneOf(ShopItem.class));
            }
        }

        int legacyScore = parseInt(properties.getProperty("focusXp"), 0);
        int totalScore = parseInt(properties.getProperty("totalScore"), legacyScore);
        int tokens = parseInt(properties.getProperty("tokens"), legacyScore);
        boolean normalizedTokens = false;
        if (totalScore > 0) {
            int cappedTokens = Math.min(tokens, Math.max(0, totalScore / 6));
            normalizedTokens = cappedTokens != tokens;
            tokens = cappedTokens;
        }
        for (ShopItem item : ShopItem.values()) {
            hadLegacyPurchases |= Boolean.parseBoolean(properties.getProperty("owned." + item.name(), "false"));
        }
        ProgressionProfile profile = new ProgressionProfile(path, totalScore, tokens, EnumSet.noneOf(ShopItem.class));
        if (normalizedTokens || hadLegacyPurchases) {
            profile.save();
        }
        return profile;
    }

    public int focusXp() {
        return totalScore;
    }

    public int totalScore() {
        return totalScore;
    }

    public int tokens() {
        return tokens;
    }

    public boolean sanctuaryUnlocked() {
        return owns(ShopItem.SANCTUARY);
    }

    public boolean owns(ShopItem item) {
        return purchases.contains(item);
    }

    public boolean buy(ShopItem item) {
        if (owns(item) || tokens < item.cost()) {
            return false;
        }
        if (!spendTokens(item.cost())) {
            return false;
        }
        purchases.add(item);
        save();
        return true;
    }

    public void addFocusXp(int amount) {
        if (amount <= 0) {
            return;
        }
        totalScore += amount;
        tokens += Math.max(1, amount / 6);
        save();
    }

    public boolean spendTokens(int amount) {
        if (amount <= 0 || tokens < amount) {
            return false;
        }
        tokens -= amount;
        save();
        return true;
    }

    public void resetPurchases() {
        if (purchases.isEmpty()) {
            return;
        }
        purchases.clear();
        save();
    }

    private void save() {
        if (savePath == null) {
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("focusXp", Integer.toString(totalScore));
        properties.setProperty("totalScore", Integer.toString(totalScore));
        properties.setProperty("tokens", Integer.toString(tokens));
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

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
