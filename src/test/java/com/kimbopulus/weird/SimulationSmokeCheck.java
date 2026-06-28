package com.kimbopulus.weird;

import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.sim.Bear;
import com.kimbopulus.weird.sim.DeathEvent;

public final class SimulationSmokeCheck {
    private SimulationSmokeCheck() {
    }

    public static void main(String[] args) {
        Simulation simulation = new Simulation(24, 16, 42L);
        simulation.seedPlants(80);
        simulation.seedRabbits(15);
        simulation.seedWolves(2);
        simulation.seedHumans(6);

        for (int i = 0; i < 240; i++) {
            simulation.tick();
        }

        require(simulation.tickCount() == 240, "Unexpected tick count.");

        int plants = simulation.count(OrganismKind.PLANT);
        int rabbits = simulation.count(OrganismKind.RABBIT);
        int wolves = simulation.count(OrganismKind.WOLF);
        require(plants + rabbits + wolves > 0, "The terrarium should not be empty after the smoke run.");
        require(populationByScan(simulation, OrganismKind.PLANT) == plants, "Plant counter should match the board.");
        require(populationByScan(simulation, OrganismKind.RABBIT) == rabbits, "Rabbit counter should match the board.");
        require(populationByScan(simulation, OrganismKind.WOLF) == wolves, "Wolf counter should match the board.");
        require(populationByScan(simulation, OrganismKind.HUMAN) == simulation.count(OrganismKind.HUMAN),
                "Human counter should match the board.");
        require(simulation.oldestAnimal() != null, "A surviving animal should be tracked as notable.");
        Position upgradeTarget = new Position(10, 8);
        double moistureBefore = simulation.grid().cellAt(upgradeTarget).moisture();
        simulation.rain(upgradeTarget);
        simulation.rainBoost(upgradeTarget);
        require(simulation.grid().cellAt(upgradeTarget).moisture() > moistureBefore,
                "The rain upgrade should increase moisture.");
        double fertilityBefore = simulation.grid().cellAt(upgradeTarget).fertility();
        simulation.compost(upgradeTarget);
        simulation.compostBoost(upgradeTarget);
        require(simulation.grid().cellAt(upgradeTarget).fertility() > fertilityBefore,
                "The compost upgrade should increase fertility.");
        require(simulation.addSanctuary(new Position(2, 2)), "The first sanctuary should be accepted.");
        require(!simulation.addSanctuary(new Position(5, 5)), "Only one sanctuary should be allowed per run.");
        require(simulation.grid().cellAt(new Position(2, 2)).sanctuary(), "Sanctuary soil should be marked.");

        simulation.restart();
        require(simulation.tickCount() == 0, "Restart should reset the tick count.");
        require(simulation.count(OrganismKind.PLANT) > 0, "Restart should restore plants.");
        require(simulation.count(OrganismKind.RABBIT) > 0, "Restart should restore rabbits.");
        require(simulation.count(OrganismKind.WOLF) > 0, "Restart should restore wolves.");
        require(simulation.count(OrganismKind.HUMAN) > 0, "Restart should restore humans.");
        require(!simulation.sanctuaryPlaced(), "Restart should allow a new sanctuary.");

        System.out.printf("Smoke check passed: plants=%d rabbits=%d wolves=%d%n", plants, rabbits, wolves);
        checkHumanPlantingAndBearVisits();
        checkDeathEvents();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static int populationByScan(Simulation simulation, OrganismKind kind) {
        int count = 0;
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                if (simulation.organismAt(x, y) != null && simulation.organismAt(x, y).kind() == kind) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void checkHumanPlantingAndBearVisits() {
        Simulation simulation = new Simulation(28, 20, 73L);
        simulation.seedHumans(90);
        boolean sawBear = false;
        boolean sawHumanDeath = false;
        for (int i = 0; i < 180; i++) {
            simulation.tick();
            sawBear |= simulation.count(OrganismKind.BEAR) > 0;
            sawHumanDeath |= simulation.recentDeathEvents().stream()
                    .anyMatch(death -> death.kind() == OrganismKind.HUMAN);
        }
        require(simulation.count(OrganismKind.PLANT) > 0, "Humans should plant new plants.");
        require(sawBear, "A high human population should attract a visiting bear.");
        require(sawHumanDeath, "A visiting bear should be able to kill a human.");
    }

    private static void checkDeathEvents() {
        Simulation simulation = new Simulation(12, 10, 9L);
        Position rabbit = new Position(2, 2);
        Position bear = new Position(3, 3);
        simulation.addRabbit(rabbit);
        simulation.placeOrganism(bear, new Bear());
        simulation.removeOrganism(rabbit);
        simulation.removeOrganism(bear);
        require(simulation.recentDeathEvents().stream().map(DeathEvent::kind).toList()
                        .containsAll(java.util.List.of(OrganismKind.RABBIT, OrganismKind.BEAR)),
                "Animal removals should create death events.");
    }
}
