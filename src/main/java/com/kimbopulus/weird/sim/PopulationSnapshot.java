package com.kimbopulus.weird.sim;

public record PopulationSnapshot(
        int tick,
        Season season,
        int plants,
        int rabbits,
        int wolves,
        int humans,
        int bears,
        double averageMoisture,
        double averageFertility,
        double averageTemperature
) {
    public int totalOrganisms() {
        return plants + rabbits + wolves + humans + bears;
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
