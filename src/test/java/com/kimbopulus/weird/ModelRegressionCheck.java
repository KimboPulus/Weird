package com.kimbopulus.weird;

import com.kimbopulus.weird.progression.ProgressionProfile;
import com.kimbopulus.weird.progression.ShopItem;
import com.kimbopulus.weird.settings.GameSettings;
import com.kimbopulus.weird.sim.Bear;
import com.kimbopulus.weird.sim.Cell;
import com.kimbopulus.weird.sim.DeathCause;
import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.PopulationSnapshot;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Rabbit;
import com.kimbopulus.weird.sim.RabbitSex;
import com.kimbopulus.weird.sim.Season;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.sim.WorldGrid;
import com.kimbopulus.weird.training.FocusRule;
import com.kimbopulus.weird.training.TrainingSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public final class ModelRegressionCheck {
    private ModelRegressionCheck() {
    }

    public static void main(String[] args) throws Exception {
        checkCellRules();
        checkGridRules();
        checkSimulationPlacements();
        checkSimulationMovementAndSearch();
        checkSimulationHistoryAndEvents();
        checkTrainingLabelsAndReset();
        checkProgressionPersistence();
        checkSettingsPersistence();
        System.out.println("Model regression check passed.");
    }

    private static void checkCellRules() {
        Cell cell = new Cell(0.25, 12.0, 0.35);
        cell.addRain(2.0);
        cell.dry(4.0);
        cell.warm(80.0);
        cell.cool(120.0);
        cell.addFertility(0.9);
        cell.spendFertility(2.0);

        require(cell.moisture() == 0.0, "Moisture should clamp at the floor.");
        require(cell.temperature() == -20.0, "Temperature should clamp at the floor.");
        require(cell.fertility() == 0.0, "Fertility should clamp at the floor.");

        Cell healthy = new Cell(0.6, 21.0, 0.7);
        Cell poor = new Cell(0.1, 2.0, 0.1);
        require(healthy.plantGrowthFactor() > poor.plantGrowthFactor(), "Good soil should support more growth.");

        healthy.makeSanctuary();
        healthy.dry(0.5);
        healthy.cool(20.0);
        healthy.spendFertility(0.5);
        healthy.stabilizeSanctuary();
        require(healthy.sanctuary(), "Sanctuary should stay marked.");
        require(healthy.moisture() == 0.62 && healthy.temperature() == 21.0 && healthy.fertility() >= 0.62,
                "Sanctuary should stabilize the cell.");
    }

    private static void checkGridRules() {
        WorldGrid grid = new WorldGrid(5, 4, new Random(11L));
        require(grid.contains(new Position(0, 0)), "Grid should contain the origin.");
        require(!grid.contains(new Position(-1, 0)), "Negative coordinates should be outside the grid.");
        require(grid.neighbors(new Position(0, 0), new Random(2L)).size() == 2, "Corner cells should have two neighbors.");
        require(grid.neighbors(new Position(2, 2), new Random(2L)).size() == 4, "Interior cells should have four neighbors.");

        Position center = new Position(2, 2);
        double beforeMoisture = grid.cellAt(center).moisture();
        double beforeFertility = grid.cellAt(center).fertility();
        grid.rainAround(center, 1, 0.25);
        grid.fertilizeAround(center, 1, 0.2);
        require(grid.cellAt(center).moisture() > beforeMoisture, "Rain should raise moisture.");
        require(grid.cellAt(center).fertility() > beforeFertility, "Fertilizer should raise fertility.");
        grid.dryAround(center, 1, 0.5);
        grid.spendFertilityAround(center, 1, 0.5);
        require(grid.cellAt(center).moisture() <= beforeMoisture, "Drying should lower moisture again.");
        require(grid.cellAt(center).fertility() <= beforeFertility, "Spending fertility should lower fertility again.");

        grid.createSanctuary(new Position(3, 2));
        require(grid.cellAt(new Position(3, 2)).sanctuary(), "Sanctuary corner should be marked.");
        require(grid.cellAt(new Position(4, 3)).sanctuary(), "Sanctuary should cover the 2x2 block.");

        double sanctuaryMoisture = grid.cellAt(new Position(3, 2)).moisture();
        grid.applySeason(Season.SUMMER);
        require(grid.cellAt(new Position(3, 2)).moisture() == sanctuaryMoisture,
                "Sanctuary cells should resist seasonal drift.");
    }

    private static void checkSimulationPlacements() {
        Simulation simulation = new Simulation(8, 8, 5L);
        require(simulation.addPlant(new Position(1, 1)), "Plant should place.");
        require(simulation.addHuman(new Position(2, 1)), "Human should place.");
        require(simulation.addBear(new Position(3, 1)), "Bear should place.");
        require(simulation.addRabbit(new Position(1, 2), RabbitSex.FEMALE), "Female rabbit should place.");
        require(simulation.addRabbit(new Position(2, 2), RabbitSex.MALE), "Male rabbit should place.");
        require(simulation.addWolf(new Position(3, 2)), "Wolf should place.");

        require(simulation.count(OrganismKind.PLANT) == 1, "Plant counter should update.");
        require(simulation.count(OrganismKind.HUMAN) == 1, "Human counter should update.");
        require(simulation.count(OrganismKind.BEAR) == 1, "Bear counter should update.");
        require(simulation.count(OrganismKind.RABBIT) == 2, "Rabbit counter should update.");
        require(simulation.count(OrganismKind.WOLF) == 1, "Wolf counter should update.");

        require(!simulation.addPlant(new Position(1, 1)), "Occupied cells should reject another organism.");
        require(simulation.removeOrganism(new Position(1, 1)) instanceof com.kimbopulus.weird.sim.Plant,
                "Removing a plant should return the removed organism.");
        require(simulation.count(OrganismKind.PLANT) == 0, "Plant counter should decrement.");

        simulation.removeOrganism(new Position(2, 1), DeathCause.HUMAN_ATTACK);
        require(simulation.count(OrganismKind.HUMAN) == 0, "Human counter should decrement.");
        require(simulation.recentDeathEvents().get(simulation.recentDeathEvents().size() - 1).cause() == DeathCause.HUMAN_ATTACK,
                "Explicit death causes should be recorded.");
    }

    private static void checkSimulationMovementAndSearch() {
        Simulation simulation = new Simulation(10, 10, 7L);
        Position from = new Position(2, 2);
        Position plantTarget = new Position(3, 2);
        Position rabbitTarget = new Position(4, 2);

        simulation.placeOrganism(from, new Rabbit(RabbitSex.FEMALE));
        simulation.addPlant(plantTarget);
        require(simulation.moveAnimal(from, plantTarget, OrganismKind.RABBIT), "Rabbit should move into a plant cell.");
        require(simulation.organismAt(plantTarget) instanceof Rabbit, "Rabbit should occupy the new cell.");
        require(simulation.count(OrganismKind.PLANT) == 0, "Moving through a plant should consume it.");

        simulation.addPlant(rabbitTarget);
        require(!simulation.moveOrganism(plantTarget, rabbitTarget), "moveOrganism should reject occupied targets.");
        require(simulation.organismAt(plantTarget) instanceof Rabbit, "Failed moves should not move the rabbit.");

        simulation.addPlant(new Position(6, 6));
        simulation.addPlant(new Position(6, 7));
        require(simulation.visibleWithKind(new Position(5, 6), OrganismKind.PLANT, 2).size() >= 2,
                "Visible search should find nearby plants.");

        int cleared = simulation.clearPatch(new Position(6, 6));
        require(cleared > 0, "Clear patch should remove local plants.");
        require(simulation.count(OrganismKind.PLANT) == 1, "Clear patch should update the plant count.");
        require(simulation.organismAt(rabbitTarget) instanceof com.kimbopulus.weird.sim.Plant,
                "Clear patch should leave the distant plant intact.");

        require(simulation.canPlantsSpread(), "A nearly empty grid should still allow growth.");
        for (int y = 0; y < 10 && simulation.count(OrganismKind.PLANT) < 60; y++) {
            for (int x = 0; x < 10 && simulation.count(OrganismKind.PLANT) < 60; x++) {
                simulation.addPlant(new Position(x, y));
            }
        }
        require(simulation.count(OrganismKind.PLANT) >= 60, "The grid should be close enough to capacity.");
        require(!simulation.canPlantsSpread(), "A saturated grid should stop allowing more spread.");
    }

    private static void checkSimulationHistoryAndEvents() {
        Simulation simulation = new Simulation(6, 6, 19L);
        simulation.addRabbit(new Position(1, 1), RabbitSex.FEMALE);
        simulation.addBear(new Position(2, 2));
        simulation.addPlant(new Position(3, 3));
        simulation.removeOrganism(new Position(1, 1));
        simulation.removeOrganism(new Position(2, 2));
        require(simulation.recentDeathEvents().size() == 2, "Death events should accumulate.");

        simulation.clearDeathEvents();
        require(simulation.recentDeathEvents().isEmpty(), "Death events should be clearable.");

        for (int i = 0; i < 90; i++) {
            simulation.recordBirth(OrganismKind.RABBIT, new Position(0, 0));
        }
        require(simulation.recentBirthEvents().size() == 80, "Birth history should cap at 80 entries.");

        Simulation notable = new Simulation(6, 6, 21L);
        notable.addRabbit(new Position(1, 1), RabbitSex.FEMALE);
        notable.addBear(new Position(2, 2));
        require(notable.oldestAnimal() != null, "A populated simulation should track a notable animal.");

        for (int i = 0; i < 400; i++) {
            simulation.tick();
        }
        require(simulation.tickCount() == 400, "Tick count should match the number of updates.");
        require(simulation.recentHistory(500).size() == 320, "History should keep the most recent 320 snapshots.");
        require(simulation.recentHistory(5).size() == 5, "Recent history should return the requested slice.");
    }

    private static void checkTrainingLabelsAndReset() {
        TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
        require("Need rabbits".equals(training.balanceStatus(new PopulationSnapshot(0, Season.SPRING, 200, 3, 2, 3, 0, 0.5, 0.5, 21.0))),
                "Low rabbits should be called out first.");
        require("Need wolves".equals(training.balanceStatus(new PopulationSnapshot(0, Season.SPRING, 200, 10, 1, 3, 0, 0.5, 0.5, 21.0))),
                "Low wolves should be called out.");
        require("Too many plants".equals(training.balanceStatus(new PopulationSnapshot(0, Season.SPRING, 1201, 10, 2, 3, 0, 0.5, 0.5, 21.0))),
                "Plant overgrowth should be flagged.");
        require("Need humans".equals(training.balanceStatus(new PopulationSnapshot(0, Season.SPRING, 200, 10, 2, 2, 0, 0.5, 0.5, 21.0))),
                "Low humans should be called out.");
        require("Balance: steady".equals(training.balanceStatus(new PopulationSnapshot(0, Season.SPRING, 320, 20, 4, 4, 0, 0.5, 0.5, 21.0))),
                "Healthy populations should read as steady.");

        training.noteAction("Rain", "cell 1,1");
        require(training.feedback().equals("Rain used."), "Tool feedback should echo the action.");
        require(training.contextHint().equals("Try one tool on the board.") || training.contextHint().equals("Watch what changes after each tool."),
                "Early guidance should be present.");
        training.reset();
        require(training.score() == 0 && training.streak() == 0, "Reset should clear run progress.");
        require(training.feedback().equals("Hold the ecosystem steady."), "Reset should restore the default feedback.");
        require(training.focusRule() == FocusRule.NORMAL, "Reset should restore the normal focus rule.");
    }

    private static void checkProgressionPersistence() throws IOException {
        Path file = Files.createTempFile("weird-progression", ".properties");
        try {
            ProgressionProfile profile = ProgressionProfile.load(file);
            profile.addFocusXp(125);
            require(profile.buy(ShopItem.RAIN_BARREL), "The profile should buy an affordable item.");

            ProgressionProfile reloaded = ProgressionProfile.load(file);
            require(reloaded.totalScore() == 125, "Progression total score should persist.");
            require(reloaded.tokens() == 65, "Progression tokens should persist after a purchase.");
            require(reloaded.owns(ShopItem.RAIN_BARREL), "Purchased upgrades should persist.");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void checkSettingsPersistence() throws IOException {
        Path file = Files.createTempFile("weird-settings", ".properties");
        try {
            GameSettings settings = GameSettings.load(file);
            settings.setAudioEnabled(false);
            settings.setMusicVolume(88);
            settings.setEffectsVolume(12);
            settings.setIntroSeen(true);

            GameSettings reloaded = GameSettings.load(file);
            require(!reloaded.audioEnabled(), "Audio setting should persist.");
            require(reloaded.musicVolume() == 88, "Music volume should persist.");
            require(reloaded.effectsVolume() == 12, "Effects volume should persist.");
            require(reloaded.introSeen(), "Intro flag should persist.");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
