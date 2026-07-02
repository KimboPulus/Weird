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
import com.kimbopulus.weird.training.TrainingSession;
import com.kimbopulus.weird.ui.TerrariumPanel;
import com.kimbopulus.weird.ui.ToolMode;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

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
        checkProgressionNormalization();
        checkSettingsPersistence();
        checkSpriteResources();
        checkSpriteContent();
        checkRabbitStarvationDeath();
        checkRabbitReproductionCostsEnergy();
        checkLightningStrikeCostsAndRecordsDeath();
        checkMechanicPopupReset();
        checkToolCosts();
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

        Position humanStart = new Position(1, 4);
        Position humanPlant = new Position(2, 4);
        simulation.placeOrganism(humanStart, new com.kimbopulus.weird.sim.Human());
        simulation.addPlant(humanPlant);
        require(simulation.moveAnimal(humanStart, humanPlant, OrganismKind.RABBIT),
                "Humans should be able to walk through plants too.");
        require(simulation.organismAt(humanPlant) instanceof com.kimbopulus.weird.sim.Human,
                "A human should occupy the plant cell after moving.");
        require(simulation.count(OrganismKind.PLANT) == 2,
                "Humans should not destroy plants when they step onto them.");

        Position humanExit = new Position(3, 4);
        require(simulation.moveAnimal(humanPlant, humanExit, OrganismKind.RABBIT),
                "Humans should be able to leave a plant-covered square.");
        require(simulation.organismAt(humanPlant) instanceof com.kimbopulus.weird.sim.Plant,
                "The plant should return after a human leaves the square.");

        Position wolfStart = new Position(4, 4);
        Position wolfPlant = new Position(5, 4);
        simulation.placeOrganism(wolfStart, new com.kimbopulus.weird.sim.Wolf());
        simulation.addPlant(wolfPlant);
        require(simulation.moveAnimal(wolfStart, wolfPlant, OrganismKind.RABBIT),
                "Wolves should be able to cross plant cells.");
        require(simulation.count(OrganismKind.PLANT) == 3,
                "Wolves should not destroy plants when they step onto them.");

        Position bearStart = new Position(7, 4);
        Position bearPlant = new Position(8, 4);
        simulation.placeOrganism(bearStart, new com.kimbopulus.weird.sim.Bear());
        simulation.addPlant(bearPlant);
        require(simulation.moveAnimal(bearStart, bearPlant, OrganismKind.HUMAN),
                "Bears should be able to cross plant cells.");
        require(simulation.count(OrganismKind.PLANT) == 4,
                "Bears should not destroy plants when they step onto them.");

        simulation.addPlant(new Position(6, 6));
        simulation.addPlant(new Position(6, 7));
        require(simulation.visibleWithKind(new Position(5, 6), OrganismKind.PLANT, 2).size() >= 2,
                "Visible search should find nearby plants.");

        int cleared = simulation.clearPatch(new Position(6, 6));
        require(cleared > 0, "Clear patch should remove local plants.");
        require(simulation.count(OrganismKind.PLANT) == 4, "Clear patch should update the plant count.");
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
        require("Keep plants 90-700".equals(training.objective()),
                "The first level objective should be blunt.");
        require("Plants 90-700".equals(training.challengeText()),
                "The first level challenge should show the target range directly.");
        require(training.balanceStatus(new PopulationSnapshot(0, Season.SPRING, 60, 10, 4, 4, 0, 0.50, 0.50, 21.0)).startsWith("Plants low"),
                "Low plants should be called out.");
        require(training.balanceStatus(new PopulationSnapshot(0, Season.SPRING, 780, 10, 4, 4, 0, 0.50, 0.50, 21.0)).startsWith("Plants high"),
                "High plants should be called out.");
        require(training.balanceStatus(new PopulationSnapshot(0, Season.SPRING, 320, 2, 4, 4, 0, 0.50, 0.50, 21.0)).startsWith("Rabbits low"),
                "Low rabbits should be called out.");
        require(training.balanceStatus(new PopulationSnapshot(0, Season.SPRING, 320, 20, 1, 4, 0, 0.50, 0.50, 21.0)).startsWith("Wolves low"),
                "Low wolves should be called out.");
        require(training.balanceStatus(new PopulationSnapshot(0, Season.SPRING, 320, 20, 4, 2, 0, 0.50, 0.50, 21.0)).startsWith("Humans low"),
                "Low humans should be called out.");
        require(training.balanceStatus(new PopulationSnapshot(0, Season.SPRING, 320, 20, 4, 4, 0, 0.10, 0.50, 21.0)).startsWith("Moisture low"),
                "Low moisture should be called out.");
        require(training.balanceStatus(new PopulationSnapshot(0, Season.SPRING, 320, 20, 4, 4, 0, 0.50, 0.50, 40.0)).startsWith("Temperature high"),
                "High temperature should be called out.");
        require("Balance: steady".equals(training.balanceStatus(new PopulationSnapshot(0, Season.SPRING, 320, 20, 4, 4, 0, 0.50, 0.50, 21.0))),
                "Healthy populations should read as steady.");
        String guide = training.balanceGuide(new PopulationSnapshot(0, Season.SPRING, 320, 20, 4, 4, 0, 0.50, 0.50, 21.0), 988);
        require(guide.contains("Fail if:") && guide.contains("outside") && guide.contains("%"),
                "Balance guidance should show the exact failure bands.");
        require(training.currentSummary(new PopulationSnapshot(0, Season.SPRING, 320, 20, 4, 4, 0, 0.50, 0.50, 21.0)).contains("Plants 320"),
                "Current summary should expose the population counts.");

        training.noteAction("Rain", "cell 1,1");
        require(training.feedback().equals("Rain used."), "Tool feedback should echo the action.");
        require(training.contextHint().contains("click") || training.contextHint().contains("square"),
                "Early guidance should describe the click-based tools.");
        training.reset();
        require(training.score() == 0 && training.streak() == 0, "Reset should clear run progress.");
        require(training.feedback().equals("Hold the ecosystem steady."), "Reset should restore the default feedback.");
        require(training.objectiveStatus(new PopulationSnapshot(0, Season.SPRING, 320, 20, 4, 4, 0, 0.50, 0.50, 21.0)).equals("Objective in range"),
                "The displayed objective should report when its range is satisfied.");
    }

    private static void checkProgressionNormalization() throws IOException {
        Path file = Files.createTempFile("weird-progression-normalize", ".properties");
        try {
            Files.writeString(file, """
                    focusXp=1200
                    totalScore=1200
                    tokens=1900
                    owned.RAIN_BARREL=true
                    owned.RICH_COMPOST=true
                    owned.SANCTUARY=true
                    """);

            ProgressionProfile profile = ProgressionProfile.load(file);
            require(profile.totalScore() == 1200, "Total score should be preserved.");
            require(profile.tokens() == 200, "Loaded tokens should be capped from an inflated save.");
            require(!profile.owns(ShopItem.RAIN_BARREL), "Shop ownership should not be restored from disk.");
            require(!profile.owns(ShopItem.RICH_COMPOST), "Shop ownership should not be restored from disk.");
            require(!profile.owns(ShopItem.SANCTUARY), "Shop ownership should not be restored from disk.");

            ProgressionProfile reloaded = ProgressionProfile.load(file);
            require(reloaded.tokens() == 200, "The normalized token cap should persist back to disk.");
            require(!reloaded.owns(ShopItem.RAIN_BARREL), "The cleaned save should not keep shop ownership.");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void checkProgressionPersistence() throws IOException {
        Path file = Files.createTempFile("weird-progression", ".properties");
        try {
            ProgressionProfile profile = ProgressionProfile.load(file);
            profile.addFocusXp(420);
            require(profile.buy(ShopItem.RAIN_BARREL), "The profile should buy an affordable item.");
            require(profile.owns(ShopItem.RAIN_BARREL), "The item should be owned during the run.");

            ProgressionProfile reloaded = ProgressionProfile.load(file);
            require(reloaded.totalScore() == 420, "Progression total score should persist.");
            require(reloaded.tokens() == 10, "Progression tokens should persist after a purchase.");
            require(!reloaded.owns(ShopItem.RAIN_BARREL), "Purchased upgrades should reset when the game reloads.");

            profile.resetPurchases();
            ProgressionProfile cleared = ProgressionProfile.load(file);
            require(!cleared.owns(ShopItem.RAIN_BARREL), "Resetting the shop should clear owned items.");
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

    private static void checkSpriteResources() throws IOException {
        require(resourceExists("/com/kimbopulus/weird/sprites/rabbit.png"), "Rabbit sprite should be on the classpath.");
        require(resourceExists("/com/kimbopulus/weird/sprites/wolf.png"), "Wolf sprite should be on the classpath.");
        require(resourceExists("/com/kimbopulus/weird/sprites/human.png"), "Human sprite should be on the classpath.");
        require(resourceExists("/com/kimbopulus/weird/sprites/bear.png"), "Bear sprite should be on the classpath.");
    }

    private static boolean resourceExists(String path) throws IOException {
        try (var input = TerrariumPanel.class.getResourceAsStream(path)) {
            return input != null;
        }
    }

    private static void checkSpriteContent() throws IOException {
        require(hasOpaquePixel("/com/kimbopulus/weird/sprites/bear.png"), "Bear sprite should contain visible pixels.");
        require(hasOpaquePixel("/com/kimbopulus/weird/sprites/human.png"), "Human sprite should contain visible pixels.");
        require(hasWhiteCorners("/com/kimbopulus/weird/sprites/bear.png"), "Bear sprite should keep a white square background.");
        require(hasWhiteCorners("/com/kimbopulus/weird/sprites/human.png"), "Human sprite should keep a white square background.");
    }

    private static void checkMechanicPopupReset() throws ReflectiveOperationException {
        TerrariumPanel panel = new TerrariumPanel(new Simulation(8, 8, 12L));
        panel.showMechanicPopup("restart-tip", "Restart tip", "This should show once.");
        panel.showMechanicPopup("restart-tip", "Restart tip", "This should stay deduped.");
        require(mechanicPopupCount(panel) == 1, "Duplicate popup keys should still dedupe.");

        panel.resetMechanicPopups();
        require(mechanicPopupCount(panel) == 0, "Reset should clear popup cards.");

        panel.showMechanicPopup("restart-tip", "Restart tip", "This should show again after reset.");
        require(mechanicPopupCount(panel) == 1, "Reset should let the same popup key appear again.");
    }

    private static void checkToolCosts() {
        require(ToolMode.LIGHTNING.tokenCost() == 10, "Lightning should cost 10 tokens.");
    }

    private static boolean hasOpaquePixel(String path) throws IOException {
        try (var input = TerrariumPanel.class.getResourceAsStream(path)) {
            if (input == null) {
                return false;
            }
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                return false;
            }
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int alpha = (image.getRGB(x, y) >>> 24) & 0xff;
                    if (alpha > 20) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static int mechanicPopupCount(TerrariumPanel panel) throws ReflectiveOperationException {
        Field field = TerrariumPanel.class.getDeclaredField("mechanicPopups");
        field.setAccessible(true);
        return ((java.util.List<?>) field.get(panel)).size();
    }

    private static boolean hasWhiteCorners(String path) throws IOException {
        try (var input = TerrariumPanel.class.getResourceAsStream(path)) {
            if (input == null) {
                return false;
            }
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                return false;
            }
            int[][] corners = {
                    {0, 0},
                    {image.getWidth() - 1, 0},
                    {0, image.getHeight() - 1},
                    {image.getWidth() - 1, image.getHeight() - 1}
            };
            for (int[] corner : corners) {
                int argb = image.getRGB(corner[0], corner[1]);
                int alpha = (argb >>> 24) & 0xff;
                int red = (argb >>> 16) & 0xff;
                int green = (argb >>> 8) & 0xff;
                int blue = argb & 0xff;
                if (alpha < 250 || red < 245 || green < 245 || blue < 245) {
                    return false;
                }
            }
            return true;
        }
    }

    private static void checkRabbitStarvationDeath() {
        Simulation simulation = new Simulation(6, 6, 99L);
        Position position = new Position(2, 2);
        require(simulation.addRabbit(position, RabbitSex.FEMALE), "Rabbit should place for starvation test.");
        Rabbit rabbit = (Rabbit) simulation.organismAt(position);
        setEnergy(rabbit, 1);

        simulation.tick();

        require(simulation.count(OrganismKind.RABBIT) == 0, "A rabbit with no energy should die on its own.");
        require(simulation.recentDeathEvents().stream().anyMatch(death -> death.kind() == OrganismKind.RABBIT),
                "Starvation should record a rabbit death event.");
    }

    private static void checkRabbitReproductionCostsEnergy() {
        Simulation simulation = new Simulation(8, 8, 101L);
        Position male = new Position(3, 3);
        Position female = new Position(4, 3);
        require(simulation.addRabbit(male, RabbitSex.MALE), "Male rabbit should place.");
        require(simulation.addRabbit(female, RabbitSex.FEMALE), "Female rabbit should place.");

        Rabbit maleRabbit = (Rabbit) simulation.organismAt(male);
        int before = maleRabbit.energy();
        maleRabbit.tick(simulation, male);

        require(maleRabbit.energy() < before, "Mating should spend rabbit energy.");
        require(simulation.count(OrganismKind.RABBIT) >= 3, "A mating pair should create offspring.");
    }

    private static void checkLightningStrikeCostsAndRecordsDeath() {
        Simulation simulation = new Simulation(8, 8, 202L);
        Position target = new Position(2, 2);
        require(simulation.addHuman(target), "Lightning target should place.");
        require(simulation.lightning(target), "Lightning should remove the target.");
        require(simulation.count(OrganismKind.HUMAN) == 0, "Lightning should update the population count.");
        require(simulation.recentDeathEvents().get(simulation.recentDeathEvents().size() - 1).cause() == DeathCause.LIGHTNING,
                "Lightning deaths should be recorded with their cause.");
    }

    private static void setEnergy(Rabbit rabbit, int value) {
        try {
            var field = com.kimbopulus.weird.sim.Organism.class.getDeclaredField("energy");
            field.setAccessible(true);
            field.setInt(rabbit, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
