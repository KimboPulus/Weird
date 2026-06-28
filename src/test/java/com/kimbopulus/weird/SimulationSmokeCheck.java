package com.kimbopulus.weird;

import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.sim.Bear;
import com.kimbopulus.weird.sim.DeathEvent;
import com.kimbopulus.weird.sim.Rabbit;
import com.kimbopulus.weird.sim.RabbitSex;

public final class SimulationSmokeCheck {
    private SimulationSmokeCheck() {
    }

    public static void main(String[] args) {
        Simulation simulation = new Simulation(24, 16, 42L);
        simulation.seedPlants(80);
        simulation.seedRabbits(15);
        simulation.seedWolves(2);
        simulation.seedHumans(6);
        require(allRabbitsFemale(simulation), "Fresh rabbit seeds should start female only.");

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
        simulation.drought(upgradeTarget);
        double fertilityBefore = simulation.grid().cellAt(upgradeTarget).fertility();
        simulation.compost(upgradeTarget);
        simulation.compostBoost(upgradeTarget);
        require(simulation.grid().cellAt(upgradeTarget).fertility() > fertilityBefore,
                "The compost upgrade should increase fertility.");
        require(simulation.addSanctuary(new Position(2, 2)), "The first sanctuary should be accepted.");
        require(!simulation.addSanctuary(new Position(5, 5)), "Only one sanctuary should be allowed per run.");
        require(simulation.grid().cellAt(new Position(2, 2)).sanctuary(), "Sanctuary soil should be marked.");

        simulation.restart();
        require(allRabbitsFemale(simulation), "Restart should restore female-only rabbit seeds.");
        require(simulation.tickCount() == 0, "Restart should reset the tick count.");
        require(simulation.count(OrganismKind.PLANT) > 0, "Restart should restore plants.");
        require(simulation.count(OrganismKind.RABBIT) > 0, "Restart should restore rabbits.");
        require(simulation.count(OrganismKind.WOLF) > 0, "Restart should restore wolves.");
        require(simulation.count(OrganismKind.HUMAN) > 0, "Restart should restore humans.");
        require(!simulation.sanctuaryPlaced(), "Restart should allow a new sanctuary.");

        System.out.printf("Smoke check passed: plants=%d rabbits=%d wolves=%d%n", plants, rabbits, wolves);
        checkHumanPlantingAndBearVisits();
        checkRabbitPairingAndWolfDeparture();
        checkRabbitPlacementSexes();
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
        simulation.addRabbit(rabbit, RabbitSex.FEMALE);
        simulation.placeOrganism(bear, new Bear());
        simulation.removeOrganism(rabbit);
        simulation.removeOrganism(bear);
        require(simulation.recentDeathEvents().stream().map(DeathEvent::kind).toList()
                        .containsAll(java.util.List.of(OrganismKind.RABBIT, OrganismKind.BEAR)),
                "Animal removals should create death events.");
    }

    private static void checkRabbitPlacementSexes() {
        Simulation simulation = new Simulation(8, 8, 31L);
        require(simulation.addRabbit(new Position(1, 1), RabbitSex.FEMALE), "Female rabbit should place.");
        require(simulation.addRabbit(new Position(2, 1), RabbitSex.MALE), "Male rabbit should place.");
        require(simulation.organismAt(1, 1) instanceof Rabbit female && female.female(),
                "Female rabbit placement should stay female.");
        require(simulation.organismAt(2, 1) instanceof Rabbit male && male.male(),
                "Male rabbit placement should stay male.");
    }

    private static void checkRabbitPairingAndWolfDeparture() {
        Simulation rabbits = new Simulation(10, 10, 21L);
        rabbits.seedPlants(12);
        Position male = new Position(4, 4);
        Position female = new Position(5, 4);
        rabbits.placeOrganism(male, new Rabbit(RabbitSex.MALE));
        rabbits.placeOrganism(female, new Rabbit(RabbitSex.FEMALE));
        for (int i = 0; i < 30 && rabbits.count(OrganismKind.RABBIT) < 5; i++) {
            rabbits.tick();
        }
        require(rabbits.count(OrganismKind.RABBIT) >= 5,
                "A first male/female meeting should create a litter of three.");

        Simulation wolves = new Simulation(10, 10, 22L);
        Position wolf = new Position(4, 4);
        com.kimbopulus.weird.sim.Wolf wolfOrganism = new com.kimbopulus.weird.sim.Wolf();
        wolves.placeOrganism(wolf, wolfOrganism);
        setKillCount(wolfOrganism, 2);
        wolves.placeOrganism(new Position(5, 4), new Rabbit(RabbitSex.FEMALE));
        for (int i = 0; i < 4 && wolves.count(OrganismKind.WOLF) > 0; i++) {
            wolves.tick();
        }
        require(wolves.count(OrganismKind.WOLF) == 0, "A wolf should leave after killing three rabbits.");
    }

    private static void setKillCount(com.kimbopulus.weird.sim.Wolf wolf, int value) {
        try {
            var field = com.kimbopulus.weird.sim.Wolf.class.getDeclaredField("rabbitsEaten");
            field.setAccessible(true);
            field.setInt(wolf, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static boolean allRabbitsFemale(Simulation simulation) {
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                if (simulation.organismAt(x, y) instanceof Rabbit rabbit && rabbit.male()) {
                    return false;
                }
            }
        }
        return true;
    }
}
