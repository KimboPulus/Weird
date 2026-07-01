package com.kimbopulus.weird;

import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.sim.Bear;
import com.kimbopulus.weird.sim.DeathEvent;
import com.kimbopulus.weird.sim.Rabbit;
import com.kimbopulus.weird.sim.RabbitSex;
import com.kimbopulus.weird.ui.ToolMode;

public final class SimulationSmokeCheck {
    private SimulationSmokeCheck() {
    }

    public static void main(String[] args) {
        Simulation simulation = new Simulation(24, 16, 42L);
        simulation.seedPlants(80);
        simulation.seedRabbits(15);
        simulation.seedWolves(2);
        simulation.seedHumans(6);
        require(!hasMaleRabbit(simulation) && hasFemaleRabbit(simulation),
                "Fresh rabbit seeds should start female only.");

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
        require(hasMaleRabbit(simulation) && hasFemaleRabbit(simulation),
                "Restart should restore a mixed rabbit starter population.");
        require(simulation.tickCount() == 0, "Restart should reset the tick count.");
        require(simulation.count(OrganismKind.PLANT) > 0, "Restart should restore plants.");
        require(simulation.count(OrganismKind.RABBIT) > 0, "Restart should restore rabbits.");
        require(simulation.count(OrganismKind.WOLF) > 0, "Restart should restore wolves.");
        require(simulation.count(OrganismKind.HUMAN) > 0, "Restart should restore humans.");
        require(!simulation.sanctuaryPlaced(), "Restart should allow a new sanctuary.");
        require(java.util.Arrays.stream(ToolMode.values()).noneMatch(mode -> mode.name().equals("PLANT")),
                "The manual plant tool should stay removed.");
        require(java.util.Arrays.stream(ToolMode.values()).noneMatch(mode -> mode.name().equals("RABBIT_FEMALE")),
                "The external female rabbit tool should stay removed.");

        System.out.printf("Smoke check passed: plants=%d rabbits=%d wolves=%d%n", plants, rabbits, wolves);
        checkHumanPlantingAndBearVisits();
        checkRabbitPairingAndWolfDeparture();
        checkRabbitPlacementSexes();
        checkDeathEvents();
        checkLocalWeatherAndLightning();
        checkWolfPairBreeding();
        checkRainCountersHeatWave();
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
        Position male = new Position(4, 4);
        Position female = new Position(5, 4);
        rabbits.placeOrganism(male, new Rabbit(RabbitSex.MALE));
        rabbits.placeOrganism(female, new Rabbit(RabbitSex.FEMALE));
        ((Rabbit) rabbits.organismAt(male)).tick(rabbits, male);
        require(rabbits.count(OrganismKind.RABBIT) >= 5,
                "A first male/female meeting should create a litter of three.");
        require(countMaleRabbits(rabbits) <= 1, "New rabbit litters should not create extra males.");

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

    private static void checkWolfPairBreeding() {
        Simulation simulation = new Simulation(10, 10, 33L);
        Position first = new Position(4, 4);
        Position second = new Position(5, 4);
        simulation.placeOrganism(first, new com.kimbopulus.weird.sim.Wolf());
        simulation.placeOrganism(second, new com.kimbopulus.weird.sim.Wolf());
        ((com.kimbopulus.weird.sim.Wolf) simulation.organismAt(first)).tick(simulation, first);
        require(simulation.count(OrganismKind.WOLF) >= 3, "Wolves should create offspring when they meet.");
    }

    private static void checkLocalWeatherAndLightning() {
        Simulation simulation = new Simulation(10, 10, 44L);
        Position center = new Position(4, 4);
        Position neighbor = new Position(5, 4);
        Position diagonal = new Position(5, 5);
        Position extended = new Position(6, 6);
        Position far = new Position(7, 7);
        double centerTemperature = simulation.grid().cellAt(center).temperature();
        double centerMoisture = simulation.grid().cellAt(center).moisture();
        double neighborTemperature = simulation.grid().cellAt(neighbor).temperature();
        double diagonalTemperature = simulation.grid().cellAt(diagonal).temperature();
        double extendedTemperature = simulation.grid().cellAt(extended).temperature();
        double farTemperature = simulation.grid().cellAt(far).temperature();

        require(simulation.rain(center), "Rain should affect the clicked square.");
        require(simulation.grid().cellAt(center).temperature() < centerTemperature,
                "Rain should cool the clicked square.");
        require(simulation.grid().cellAt(neighbor).temperature() < neighborTemperature,
                "Rain should affect the 4x4 patch around the click.");
        require(simulation.grid().cellAt(diagonal).temperature() < diagonalTemperature,
                "Rain should reach the 4x4 diagonal cells too.");
        require(simulation.grid().cellAt(extended).temperature() < extendedTemperature,
                "Rain should reach the far corner of the 4x4 patch too.");
        require(simulation.grid().cellAt(far).temperature() == farTemperature,
                "Rain should stay inside the 4x4 patch.");

        double afterRainTemperature = simulation.grid().cellAt(center).temperature();
        double afterRainMoisture = simulation.grid().cellAt(center).moisture();
        simulation.addHuman(center);
        require(simulation.drought(center), "Drought should affect the clicked square.");
        require(simulation.count(OrganismKind.HUMAN) == 0, "Direct drought clicks should kill creatures on the target cell.");
        require(simulation.grid().cellAt(center).temperature() > afterRainTemperature,
                "Drought should warm the clicked square.");
        require(simulation.grid().cellAt(center).moisture() < afterRainMoisture,
                "Drought should dry the clicked square after rain.");
        require(simulation.grid().cellAt(center).moisture() < centerMoisture,
                "Drought should leave the square drier than it started.");

        simulation.addHuman(neighbor);
        require(simulation.lightning(neighbor), "Lightning should strike an exact occupied square.");
        require(simulation.count(OrganismKind.HUMAN) == 0, "Lightning should remove the target organism.");
        require(simulation.recentDeathEvents().get(simulation.recentDeathEvents().size() - 1).cause() == com.kimbopulus.weird.sim.DeathCause.LIGHTNING,
                "Lightning deaths should be recorded with the lightning cause.");
    }

    private static void checkRainCountersHeatWave() {
        Simulation simulation = new Simulation(12, 12, 57L);
        Position center = new Position(5, 5);
        simulation.grid().dryAndWarmAll(0.0, 2.5);
        double afterHeatWave = simulation.grid().cellAt(center).temperature();

        require(simulation.rain(center), "Rain should still be usable after a heat spike.");
        double afterRain = simulation.grid().cellAt(center).temperature();
        require(afterRain <= afterHeatWave - 4.0,
                "Rain should cool a hot patch hard enough to counter a heat wave quickly.");

        for (int i = 0; i < 4; i++) {
            simulation.tick();
        }
        require(simulation.grid().cellAt(center).temperature() < afterRain,
                "Ongoing rain should keep cooling while the rain effect is active.");
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

    private static boolean hasMaleRabbit(Simulation simulation) {
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                if (simulation.organismAt(x, y) instanceof Rabbit rabbit && rabbit.male()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasFemaleRabbit(Simulation simulation) {
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                if (simulation.organismAt(x, y) instanceof Rabbit rabbit && rabbit.female()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int countMaleRabbits(Simulation simulation) {
        int count = 0;
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                if (simulation.organismAt(x, y) instanceof Rabbit rabbit && rabbit.male()) {
                    count++;
                }
            }
        }
        return count;
    }
}
