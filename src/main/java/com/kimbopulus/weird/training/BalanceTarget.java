package com.kimbopulus.weird.training;

import com.kimbopulus.weird.sim.PopulationSnapshot;

public record BalanceTarget(
        int plantsMin,
        int plantsMax,
        int rabbitsMin,
        int rabbitsMax,
        int wolvesMin,
        int wolvesMax,
        int humansMin,
        int humansMax,
        int bearsMin,
        int bearsMax,
        double moistureMin,
        double moistureMax,
        double temperatureMin,
        double temperatureMax
) {
    public boolean matches(PopulationSnapshot snapshot) {
        return matches(snapshot, BalanceBand.values());
    }

    public boolean matches(PopulationSnapshot snapshot, BalanceBand... bands) {
        for (BalanceBand band : bands) {
            if (!matchesBand(snapshot, band)) {
                return false;
            }
        }
        return true;
    }

    public String status(PopulationSnapshot snapshot) {
        return status(snapshot, "Balance: steady", BalanceBand.values());
    }

    public String status(PopulationSnapshot snapshot, String steadyText, BalanceBand... bands) {
        String category = category(snapshot, bands);
        return category == null ? steadyText : formatCategory(snapshot, category);
    }

    public String category(PopulationSnapshot snapshot) {
        return category(snapshot, BalanceBand.values());
    }

    public String category(PopulationSnapshot snapshot, BalanceBand... bands) {
        for (BalanceBand band : bands) {
            String category = categoryForBand(snapshot, band);
            if (category != null) {
                return category;
            }
        }
        return null;
    }

    public String statusForCategory(PopulationSnapshot snapshot, String category) {
        if (category == null) {
            return null;
        }
        for (BalanceBand band : BalanceBand.values()) {
            String current = categoryForBand(snapshot, band);
            if (category.equals(current)) {
                return formatCategory(snapshot, current);
            }
        }
        return null;
    }

    public String currentSummary(PopulationSnapshot snapshot) {
        return String.format(
                "Plants %d   Rabbits %d<br>Wolves %d   Humans %d   Bears %d",
                snapshot.plants(),
                snapshot.rabbits(),
                snapshot.wolves(),
                snapshot.humans(),
                snapshot.bears()
        );
    }

    public String guide(PopulationSnapshot snapshot, int boardCells) {
        int safeBoardCells = Math.max(1, boardCells);
        return String.format(
                "<b><font size='+1'>Fail if:</font></b><br>"
                        + "Plants outside %s<br>"
                        + "Rabbits outside %s<br>"
                        + "Wolves outside %s<br>"
                        + "Humans outside %s<br>"
                        + "Bears outside %s<br>"
                        + "Moisture outside %.0f-%.0f%%<br>"
                        + "Temp outside %.1f-%.1f C",
                band(plantsMin, plantsMax, safeBoardCells),
                band(rabbitsMin, rabbitsMax, safeBoardCells),
                band(wolvesMin, wolvesMax, safeBoardCells),
                band(humansMin, humansMax, safeBoardCells),
                band(bearsMin, bearsMax, safeBoardCells),
                moistureMin * 100.0,
                moistureMax * 100.0,
                temperatureMin,
                temperatureMax
        );
    }

    private boolean matchesBand(PopulationSnapshot snapshot, BalanceBand band) {
        return switch (band) {
            case PLANTS -> inRange(snapshot.plants(), plantsMin, plantsMax);
            case RABBITS -> inRange(snapshot.rabbits(), rabbitsMin, rabbitsMax);
            case WOLVES -> inRange(snapshot.wolves(), wolvesMin, wolvesMax);
            case HUMANS -> inRange(snapshot.humans(), humansMin, humansMax);
            case BEARS -> inRange(snapshot.bears(), bearsMin, bearsMax);
            case MOISTURE -> inRange(snapshot.averageMoisture(), moistureMin, moistureMax);
            case TEMPERATURE -> inRange(snapshot.averageTemperature(), temperatureMin, temperatureMax);
        };
    }

    private String categoryForBand(PopulationSnapshot snapshot, BalanceBand band) {
        return switch (band) {
            case PLANTS -> rangeCategory(snapshot.plants(), plantsMin, plantsMax, "Plants");
            case RABBITS -> rangeCategory(snapshot.rabbits(), rabbitsMin, rabbitsMax, "Rabbits");
            case WOLVES -> rangeCategory(snapshot.wolves(), wolvesMin, wolvesMax, "Wolves");
            case HUMANS -> rangeCategory(snapshot.humans(), humansMin, humansMax, "Humans");
            case BEARS -> rangeCategory(snapshot.bears(), bearsMin, bearsMax, "Bears");
            case MOISTURE -> rangeCategory(snapshot.averageMoisture(), moistureMin, moistureMax, "Moisture");
            case TEMPERATURE -> rangeCategory(snapshot.averageTemperature(), temperatureMin, temperatureMax, "Temperature");
        };
    }

    private String formatCategory(PopulationSnapshot snapshot, String category) {
        if (category.startsWith("Plants")) {
            return category + String.format(" (%d, range %d-%d)", snapshot.plants(), plantsMin, plantsMax);
        }
        if (category.startsWith("Rabbits")) {
            return category + String.format(" (%d, range %d-%d)", snapshot.rabbits(), rabbitsMin, rabbitsMax);
        }
        if (category.startsWith("Wolves")) {
            return category + String.format(" (%d, range %d-%d)", snapshot.wolves(), wolvesMin, wolvesMax);
        }
        if (category.startsWith("Humans")) {
            return category + String.format(" (%d, range %d-%d)", snapshot.humans(), humansMin, humansMax);
        }
        if (category.startsWith("Bears")) {
            return category + String.format(" (%d, range %d-%d)", snapshot.bears(), bearsMin, bearsMax);
        }
        if (category.startsWith("Moisture")) {
            return category + String.format(" (%.0f%%, range %.0f-%.0f%%)",
                    snapshot.averageMoisture() * 100.0,
                    moistureMin * 100.0,
                    moistureMax * 100.0);
        }
        return category + String.format(" (%.1f C, range %.1f-%.1f C)",
                snapshot.averageTemperature(),
                temperatureMin,
                temperatureMax);
    }

    private static boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    private static boolean inRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    private static String band(int min, int max, int boardCells) {
        double minPercent = min * 100.0 / boardCells;
        double maxPercent = max * 100.0 / boardCells;
        return String.format("%d-%d (%d-%d%%)", min, max, Math.round(minPercent), Math.round(maxPercent));
    }

    private static String rangeCategory(int value, int min, int max, String label) {
        if (value < min) {
            return label + " low";
        }
        if (value > max) {
            return label + " high";
        }
        return null;
    }

    private static String rangeCategory(double value, double min, double max, String label) {
        if (value < min) {
            return label + " low";
        }
        if (value > max) {
            return label + " high";
        }
        return null;
    }
}
