package com.kimbopulus.weird;

import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.Simulation;

public final class SimulationSmokeCheck {
    private SimulationSmokeCheck() {
    }

    public static void main(String[] args) {
        Simulation simulation = new Simulation(24, 16, 42L);
        simulation.seedPlants(80);
        simulation.seedRabbits(15);
        simulation.seedWolves(2);

        for (int i = 0; i < 240; i++) {
            simulation.tick();
        }

        require(simulation.tickCount() == 240, "Unexpected tick count.");

        int plants = simulation.count(OrganismKind.PLANT);
        int rabbits = simulation.count(OrganismKind.RABBIT);
        int wolves = simulation.count(OrganismKind.WOLF);
        require(plants + rabbits + wolves > 0, "The terrarium should not be empty after the smoke run.");
        require(simulation.oldestAnimal() != null, "A surviving animal should be tracked as notable.");

        simulation.restart();
        require(simulation.tickCount() == 0, "Restart should reset the tick count.");
        require(simulation.count(OrganismKind.PLANT) > 0, "Restart should restore plants.");
        require(simulation.count(OrganismKind.RABBIT) > 0, "Restart should restore rabbits.");
        require(simulation.count(OrganismKind.WOLF) > 0, "Restart should restore wolves.");

        System.out.printf("Smoke check passed: plants=%d rabbits=%d wolves=%d%n", plants, rabbits, wolves);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
