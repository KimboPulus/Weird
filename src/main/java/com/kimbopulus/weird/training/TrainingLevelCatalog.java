package com.kimbopulus.weird.training;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public final class TrainingLevelCatalog {
    private static final String RESOURCE = "/com/kimbopulus/weird/training/training-levels.properties";

    private TrainingLevelCatalog() {
    }

    public static List<TrainingLevelSpec> loadDefault() {
        Properties properties = new Properties();
        try (InputStream input = TrainingLevelCatalog.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing level catalog: " + RESOURCE);
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read level catalog.", exception);
        }

        int count = Integer.parseInt(properties.getProperty("levels"));
        List<TrainingLevelSpec> specs = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            String prefix = "level." + i + ".";
            specs.add(new TrainingLevelSpec(
                    i,
                    required(properties, prefix + "title"),
                    required(properties, prefix + "objective"),
                    required(properties, prefix + "challenge"),
                    Integer.parseInt(required(properties, prefix + "target")),
                    parseBands(required(properties, prefix + "bands"))
            ));
        }
        return List.copyOf(specs);
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing level catalog key: " + key);
        }
        return value.trim();
    }

    private static List<BalanceBand> parseBands(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(BalanceBand::valueOf)
                .toList();
    }
}
