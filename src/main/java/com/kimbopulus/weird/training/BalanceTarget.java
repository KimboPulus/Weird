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
        return inRange(snapshot.plants(), plantsMin, plantsMax)
                && inRange(snapshot.rabbits(), rabbitsMin, rabbitsMax)
                && inRange(snapshot.wolves(), wolvesMin, wolvesMax)
                && inRange(snapshot.humans(), humansMin, humansMax)
                && inRange(snapshot.bears(), bearsMin, bearsMax)
                && inRange(snapshot.averageMoisture(), moistureMin, moistureMax)
                && inRange(snapshot.averageTemperature(), temperatureMin, temperatureMax);
    }

    public String status(PopulationSnapshot snapshot) {
        String category = category(snapshot);
        if (category != null) {
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
            if (category.startsWith("Temperature")) {
                return category + String.format(" (%.1f C, range %.1f-%.1f C)",
                        snapshot.averageTemperature(),
                        temperatureMin,
                        temperatureMax);
            }
        }
        return "Balance: steady";
    }

    public String category(PopulationSnapshot snapshot) {
        if (snapshot.plants() < plantsMin) {
            return "Plants low";
        }
        if (snapshot.plants() > plantsMax) {
            return "Plants high";
        }
        if (snapshot.rabbits() < rabbitsMin) {
            return "Rabbits low";
        }
        if (snapshot.rabbits() > rabbitsMax) {
            return "Rabbits high";
        }
        if (snapshot.wolves() < wolvesMin) {
            return "Wolves low";
        }
        if (snapshot.wolves() > wolvesMax) {
            return "Wolves high";
        }
        if (snapshot.humans() < humansMin) {
            return "Humans low";
        }
        if (snapshot.humans() > humansMax) {
            return "Humans high";
        }
        if (snapshot.bears() < bearsMin) {
            return "Bears low";
        }
        if (snapshot.bears() > bearsMax) {
            return "Bears high";
        }
        if (snapshot.averageMoisture() < moistureMin) {
            return "Moisture low";
        }
        if (snapshot.averageMoisture() > moistureMax) {
            return "Moisture high";
        }
        if (snapshot.averageTemperature() < temperatureMin) {
            return "Temperature low";
        }
        if (snapshot.averageTemperature() > temperatureMax) {
            return "Temperature high";
        }
        return null;
    }

    public String guide(PopulationSnapshot snapshot, int boardCells) {
        int safeBoardCells = Math.max(1, boardCells);
        return String.format(
                "Current:<br>"
                        + "Plants %d | Rabbits %d | Wolves %d | Humans %d | Bears %d<br>"
                        + "Moisture %.0f%% | Temp %.1f C<br><br>"
                        + "Target:<br>"
                        + "Plants %s<br>"
                        + "Rabbits %s<br>"
                        + "Wolves %s<br>"
                        + "Humans %s<br>"
                        + "Bears %s<br>"
                        + "Moisture %.0f-%.0f%%<br>"
                        + "Temp %.1f-%.1f C",
                snapshot.plants(),
                snapshot.rabbits(),
                snapshot.wolves(),
                snapshot.humans(),
                snapshot.bears(),
                snapshot.averageMoisture() * 100.0,
                snapshot.averageTemperature(),
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
}
