package com.kimbopulus.weird.sim;

public record PopulationSnapshot(
        int tick,
        Season season,
        int plants,
        int rabbits,
        int wolves,
        double averageMoisture,
        double averageFertility,
        double averageTemperature
) {
    public int totalOrganisms() {
        return plants + rabbits + wolves;
    }

    public OrganismKind mostCommonKind() {
        if (plants >= rabbits && plants >= wolves) {
            return OrganismKind.PLANT;
        }
        if (rabbits >= wolves) {
            return OrganismKind.RABBIT;
        }
        return OrganismKind.WOLF;
    }
}

