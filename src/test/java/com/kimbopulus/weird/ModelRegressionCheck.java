package com.kimbopulus.weird;

import com.kimbopulus.weird.progression.ProgressionProfile;
import com.kimbopulus.weird.progression.ShopItem;
import com.kimbopulus.weird.game.GameCommand;
import com.kimbopulus.weird.game.GameCommandType;
import com.kimbopulus.weird.game.GameEventLog;
import com.kimbopulus.weird.game.GameEventType;
import com.kimbopulus.weird.game.ReplayLog;
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
import com.kimbopulus.weird.sim.WorldEvent;
import com.kimbopulus.weird.sim.WorldGrid;
import com.kimbopulus.weird.training.BalanceBand;
import com.kimbopulus.weird.training.BalanceTarget;
import com.kimbopulus.weird.training.TrainingLevelCatalog;
import com.kimbopulus.weird.training.TrainingLevelSpec;
import com.kimbopulus.weird.training.TrainingLevel;
import com.kimbopulus.weird.training.TrainingSession;
import com.kimbopulus.weird.ui.TerrariumPanel;
import com.kimbopulus.weird.ui.ToolMode;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
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
        checkDroughtDriesMoistureAggressively();
        checkRainAfterDroughtCoolsGlobalTemperature();
        checkDroughtAfterDroughtHeatsGlobalTemperature();
        checkTrainingLabelsAndReset();
        checkProgressionPersistence();
        checkProgressionNormalization();
        checkProgressionMinimumTokenGain();
        checkProgressionSpendTokenGuards();
        checkProgressionSanctuaryUnlockFlag();
        checkSettingsPersistence();
        checkSettingsDefaultValues();
        checkSettingsClampBounds();
        checkSpriteResources();
        checkSpriteContent();
        checkRabbitStarvationDeath();
        checkRabbitReproductionCostsEnergy();
        checkBearAttackRecordsCause();
        checkLightningStrikeCostsAndRecordsDeath();
        checkMechanicPopupReset();
        checkToolCosts();
        checkTrainingLevelWraps();
        checkTrainingLevelObjectiveBandCopy();
        checkBalanceTargetSelectiveMatch();
        checkBalanceTargetSelectiveStatus();
        checkWorldEventMetadata();
        checkShopItemMetadata();
        checkToolModeMetadata();
        checkWorldGridPatchClipsAtEdges();
        checkWorldGridGlobalEffectsRespectSanctuary();
        checkSimulationOutOfBoundsActionsFail();
        checkSimulationLightningRejectsEmptyTarget();
        checkSimulationClearPatchEmptyReturnsZero();
        checkSimulationPassableNeighborsIncludePlants();
        checkSimulationEmptyNeighborsExcludeOccupied();
        checkSimulationCurrentSnapshotLazyInit();
        checkReplayLogRoundTrip();
        checkEventLogBounds();
        checkTrainingLevelCatalog();
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

    private static void checkRainAfterDroughtCoolsGlobalTemperature() {
        Simulation simulation = new Simulation(6, 6, 23L);
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                simulation.grid().cellAt(x, y).reset(0.50, 20.0, 0.50);
            }
        }

        Position center = new Position(3, 3);
        require(simulation.drought(center), "Drought should apply to the test patch.");
        double beforeRain = averageTemperature(simulation);

        require(simulation.rain(center), "Rain should apply to the droughted patch.");
        double afterRain = averageTemperature(simulation);
        double expectedDrop = (11.2 * 16.0 / 36.0) + 2.5;
        double actualDrop = beforeRain - afterRain;

        require(Math.abs(actualDrop - expectedDrop) < 0.0001,
                "Rain on an active drought patch should cool the whole board by 2.5 extra degrees.");
    }

    private static void checkDroughtDriesMoistureAggressively() {
        Simulation simulation = new Simulation(6, 6, 22L);
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                simulation.grid().cellAt(x, y).reset(0.95, 20.0, 0.50);
            }
        }

        Position center = new Position(3, 3);
        require(simulation.drought(center), "Drought should apply to the test patch.");

        double averageMoisture = averageMoisture(simulation);
        double expectedWithGlobalDrying = (20.0 * 0.94) / 36.0;
        require(Math.abs(averageMoisture - expectedWithGlobalDrying) < 0.0001,
                "Drought should strip the 4 x 4 patch and lower total moisture by 1%.");
    }

    private static void checkDroughtAfterDroughtHeatsGlobalTemperature() {
        Simulation simulation = new Simulation(6, 6, 24L);
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                simulation.grid().cellAt(x, y).reset(0.50, 20.0, 0.50);
            }
        }

        Position center = new Position(3, 3);
        require(simulation.drought(center), "First drought should apply.");
        double beforeSecondDrought = averageTemperature(simulation);

        require(simulation.drought(center), "Second drought should apply.");
        double afterSecondDrought = averageTemperature(simulation);
        double expectedRise = (3.2 * 16.0 / 36.0) + 2.0;
        double actualRise = afterSecondDrought - beforeSecondDrought;

        require(Math.abs(actualRise - expectedRise) < 0.0001,
                "Second drought on the same patch should warm the whole board by 2 extra degrees.");
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

    private static void checkProgressionMinimumTokenGain() {
        ProgressionProfile profile = ProgressionProfile.inMemory();
        profile.addFocusXp(1);
        require(profile.totalScore() == 1, "Positive score gains should increase total score.");
        require(profile.tokens() == 1, "Very small score gains should still grant one token.");
    }

    private static void checkProgressionSpendTokenGuards() {
        ProgressionProfile profile = ProgressionProfile.inMemory();
        profile.addFocusXp(60);
        int before = profile.tokens();
        require(!profile.spendTokens(0), "Spending zero tokens should fail.");
        require(!profile.spendTokens(-5), "Spending a negative amount should fail.");
        require(!profile.spendTokens(before + 1), "Spending more tokens than owned should fail.");
        require(profile.tokens() == before, "Failed token spends must not change the balance.");
    }

    private static void checkProgressionSanctuaryUnlockFlag() {
        ProgressionProfile profile = ProgressionProfile.inMemory();
        profile.addFocusXp(600);
        require(profile.buy(ShopItem.SANCTUARY), "The sanctuary upgrade should be purchasable with enough tokens.");
        require(profile.sanctuaryUnlocked(), "Buying the sanctuary upgrade should flip the unlock flag.");
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

    private static void checkSettingsDefaultValues() {
        GameSettings settings = GameSettings.inMemory();
        require(settings.audioEnabled(), "In-memory settings should default audio to on.");
        require(settings.musicVolume() == 12, "Music volume should default to the quieter first-run level.");
        require(settings.effectsVolume() == 70, "Effects volume should default to the standard level.");
        require(!settings.introSeen(), "Intro should default to unseen.");
    }

    private static void checkSettingsClampBounds() {
        GameSettings settings = GameSettings.inMemory();
        settings.setMusicVolume(140);
        settings.setEffectsVolume(-20);
        require(settings.musicVolume() == 100, "Music volume should clamp to 100.");
        require(settings.effectsVolume() == 0, "Effects volume should clamp to 0.");
    }

    private static void checkSpriteResources() throws IOException {
        require(resourceExists("/com/kimbopulus/weird/sprites/rabbit.png"), "Rabbit sprite should be on the classpath.");
        require(resourceExists("/com/kimbopulus/weird/sprites/wolf.png"), "Wolf sprite should be on the classpath.");
        require(resourceExists("/com/kimbopulus/weird/sprites/human.png"), "Human sprite should be on the classpath.");
        require(resourceExists("/com/kimbopulus/weird/sprites/bear.png"), "Bear sprite should be on the classpath.");
        require(resourceExists("/com/kimbopulus/weird/media/game-complete.mp4"),
                "Completion video should be bundled on the classpath.");
        require(resourceExists("/com/kimbopulus/weird/audio/human-attack.wav"),
                "Human attack sound should be bundled on the classpath.");
        require(resourceExists("/com/kimbopulus/weird/effects/blood-splatter.png"),
                "Blood splatter effect should be bundled on the classpath.");
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
        require(hasTransparentCorners("/com/kimbopulus/weird/effects/blood-splatter.png"),
                "Blood splatter should have a transparent background.");
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

    private static void checkTrainingLevelWraps() {
        require(TrainingLevel.FLEX_SHIFT.next() == TrainingLevel.STEADY_START,
                "The final training level should wrap back to the first.");
    }

    private static void checkTrainingLevelObjectiveBandCopy() {
        BalanceBand[] bands = TrainingLevel.FLEX_SHIFT.objectiveBands();
        bands[0] = BalanceBand.PLANTS;
        require(TrainingLevel.FLEX_SHIFT.objectiveBands()[0] == BalanceBand.PLANTS
                        && TrainingLevel.FLEX_SHIFT.objectiveBands().length > 1
                        && TrainingLevel.FLEX_SHIFT.objectiveBands()[1] == BalanceBand.RABBITS,
                "Training level objective bands should be returned as a defensive copy.");
    }

    private static void checkBalanceTargetSelectiveMatch() {
        PopulationSnapshot snapshot = new PopulationSnapshot(0, Season.SPRING, 320, 20, 0, 4, 0, 0.50, 0.50, 21.0);
        require(TrainingLevel.MEMORY_SCAN.objectiveMatches(snapshot),
                "Level 2 should care only about plants and rabbits for its displayed objective.");
        require(!TrainingLevel.PREDATOR_CHECK.objectiveMatches(snapshot),
                "The predator level should fail its objective when wolves are missing.");
    }

    private static void checkBalanceTargetSelectiveStatus() {
        BalanceTarget target = TrainingLevel.MEMORY_SCAN.balanceTarget();
        PopulationSnapshot snapshot = new PopulationSnapshot(0, Season.SPRING, 320, 20, 0, 4, 0, 0.50, 0.50, 21.0);
        require("Objective in range".equals(target.status(snapshot, "Objective in range",
                        TrainingLevel.MEMORY_SCAN.objectiveBands())),
                "Selective objective status should ignore non-objective bands.");
    }

    private static void checkWorldEventMetadata() {
        for (WorldEvent event : WorldEvent.values()) {
            require(!event.title().isBlank(), "Every world event should have a title.");
            require(!event.description().isBlank(), "Every world event should have a description.");
        }
        require(WorldEvent.HEAT_WAVE.description().contains("dries"),
                "Heat wave text should explain the dry effect.");
    }

    private static void checkShopItemMetadata() {
        require(ShopItem.SANCTUARY.cost() > ShopItem.RAIN_BARREL.cost(),
                "Sanctuary should stay the most expensive shop item.");
        for (ShopItem item : ShopItem.values()) {
            require(!item.title().isBlank(), "Every shop item should have a title.");
            require(!item.description().isBlank(), "Every shop item should have a description.");
            require(item.cost() > 0, "Every shop item should cost a positive number of tokens.");
        }
    }

    private static void checkToolModeMetadata() {
        for (ToolMode mode : ToolMode.values()) {
            require(!mode.label().isBlank(), "Every tool should have a label.");
            require(!mode.description().isBlank(), "Every tool should have a hover description.");
        }
        require(ToolMode.RAIN.description().startsWith("Use when"),
                "Tool descriptions should explain when to use the tool.");
        require(ToolMode.LIGHTNING.tokenCost() == 10 && ToolMode.RAIN.tokenCost() == 0,
                "Only lightning should currently cost tokens.");
    }

    private static void checkWorldGridPatchClipsAtEdges() {
        WorldGrid grid = new WorldGrid(4, 4, new Random(17L));
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                grid.cellAt(x, y).reset(0.50, 20.0, 0.50);
            }
        }
        grid.rainPatch(new Position(-2, -1), 4, 4, 0.25);
        require(grid.cellAt(0, 0).moisture() > 0.50, "Patch effects should clip into the top-left corner.");
        require(grid.cellAt(3, 3).moisture() == 0.50, "Clipped patch effects should not spill to the far corner.");
    }

    private static void checkWorldGridGlobalEffectsRespectSanctuary() {
        WorldGrid grid = new WorldGrid(4, 4, new Random(18L));
        grid.createSanctuary(new Position(1, 1));
        double sanctuaryMoisture = grid.cellAt(1, 1).moisture();
        double sanctuaryTemperature = grid.cellAt(1, 1).temperature();
        grid.rainAll(0.20);
        grid.dryAndWarmAll(0.20, 3.0);
        require(grid.cellAt(1, 1).moisture() == sanctuaryMoisture,
                "Sanctuary cells should ignore global rain and drought drift.");
        require(grid.cellAt(1, 1).temperature() == sanctuaryTemperature,
                "Sanctuary cells should ignore global heat changes too.");
    }

    private static void checkSimulationOutOfBoundsActionsFail() {
        Simulation simulation = new Simulation(6, 6, 41L);
        Position outside = new Position(-1, -1);
        require(!simulation.addPlant(outside), "Out-of-bounds plant placement should fail.");
        require(!simulation.addHuman(outside), "Out-of-bounds human placement should fail.");
        require(!simulation.addBear(outside), "Out-of-bounds bear placement should fail.");
        require(!simulation.addRabbit(outside, RabbitSex.FEMALE), "Out-of-bounds rabbit placement should fail.");
        require(!simulation.addWolf(outside), "Out-of-bounds wolf placement should fail.");
        require(!simulation.rain(outside), "Out-of-bounds rain should fail.");
        require(!simulation.drought(outside), "Out-of-bounds drought should fail.");
        require(!simulation.lightning(outside), "Out-of-bounds lightning should fail.");
    }

    private static void checkSimulationLightningRejectsEmptyTarget() {
        Simulation simulation = new Simulation(6, 6, 42L);
        require(!simulation.lightning(new Position(2, 2)),
                "Lightning should reject empty cells.");
    }

    private static void checkSimulationClearPatchEmptyReturnsZero() {
        Simulation simulation = new Simulation(6, 6, 43L);
        require(simulation.clearPatch(new Position(3, 3)) == 0,
                "Clearing an empty patch should remove nothing.");
    }

    private static void checkSimulationPassableNeighborsIncludePlants() {
        Simulation simulation = new Simulation(6, 6, 44L);
        Position center = new Position(2, 2);
        Position plant = new Position(3, 2);
        Position empty = new Position(2, 3);
        Position blocked = new Position(1, 2);
        simulation.placeOrganism(center, new Rabbit(RabbitSex.FEMALE));
        simulation.addPlant(plant);
        simulation.addWolf(blocked);
        var neighbors = simulation.passableNeighbors(center, OrganismKind.RABBIT);
        require(neighbors.contains(plant), "Plant tiles should count as passable neighbors.");
        require(neighbors.contains(empty), "Empty tiles should count as passable neighbors.");
        require(!neighbors.contains(blocked), "Occupied predator tiles should not count as passable.");
    }

    private static void checkSimulationEmptyNeighborsExcludeOccupied() {
        Simulation simulation = new Simulation(6, 6, 45L);
        Position center = new Position(2, 2);
        Position occupied = new Position(3, 2);
        simulation.placeOrganism(center, new Rabbit(RabbitSex.FEMALE));
        simulation.addPlant(occupied);
        var neighbors = simulation.emptyNeighbors(center);
        require(!neighbors.contains(occupied), "Occupied tiles should not appear in empty-neighbor lists.");
        require(neighbors.size() == 3, "One occupied side should leave three empty neighbors.");
    }

    private static void checkSimulationCurrentSnapshotLazyInit() {
        Simulation simulation = new Simulation(6, 6, 46L);
        PopulationSnapshot snapshot = simulation.currentSnapshot();
        require(snapshot != null, "Current snapshot should be created lazily on first access.");
        require(simulation.recentHistory(5).size() == 1,
                "Lazy snapshot creation should populate the history with one entry.");
    }

    private static void checkReplayLogRoundTrip() throws IOException {
        ReplayLog log = new ReplayLog();
        log.add(new GameCommand(1, 0, GameCommandType.HUMAN, new Position(2, 2), true));
        log.add(new GameCommand(2, 0, GameCommandType.RABBIT, new Position(3, 2), true));
        log.add(new GameCommand(3, 0, GameCommandType.WOLF, new Position(4, 2), true));
        log.add(new GameCommand(4, 0, GameCommandType.LIGHTNING, new Position(4, 2), true));
        log.add(new GameCommand(5, 0, GameCommandType.BEAR, new Position(9, 9), false));

        Path file = Files.createTempFile("weird-replay", ".wrpl");
        try {
            log.save(file);
            ReplayLog loaded = ReplayLog.load(file);
            require(loaded.commands().size() == 5, "Replay should keep every recorded command.");
            require(loaded.commands().get(1).type() == GameCommandType.RABBIT,
                    "Replay should preserve command types.");
            require(loaded.commands().get(4).accepted() == false,
                    "Replay should preserve rejected commands for debugging.");

            Simulation replayed = new Simulation(8, 8, 88L);
            int applied = loaded.applyTo(replayed);
            require(applied == 4, "Replay should apply only accepted commands that affect the board.");
            require(replayed.count(OrganismKind.HUMAN) == 1, "Replay should add the human.");
            require(replayed.count(OrganismKind.RABBIT) == 1, "Replay should add the rabbit.");
            require(replayed.count(OrganismKind.WOLF) == 0, "Replay lightning should remove the wolf.");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void checkEventLogBounds() {
        GameEventLog log = new GameEventLog();
        for (int i = 0; i < 90; i++) {
            log.add(i, GameEventType.COMMAND, "Event " + i);
        }
        require(log.recent(200).size() == 80, "Event log should cap old entries.");
        require(log.recent(1).get(0).message().equals("Event 89"), "Event log should expose latest event.");
        log.clear();
        require(log.latest() == null && log.recent(10).isEmpty(), "Clearing event log should remove entries.");
    }

    private static void checkTrainingLevelCatalog() {
        List<TrainingLevelSpec> specs = TrainingLevelCatalog.loadDefault();
        TrainingLevel[] levels = TrainingLevel.values();
        require(specs.size() == levels.length, "Level catalog should mirror enum level count.");
        for (int i = 0; i < levels.length; i++) {
            TrainingLevel level = levels[i];
            TrainingLevelSpec spec = specs.get(i);
            require(spec.number() == i + 1, "Level catalog numbers should stay ordered.");
            require(spec.title().equals(level.title()), "Level catalog title should match runtime level.");
            require(spec.objective().equals(level.objective()), "Level catalog objective should match runtime level.");
            require(spec.challenge().equals(level.challenge()), "Level catalog challenge should match runtime level.");
            require(spec.target() == level.target(), "Level catalog target should match runtime level.");
            require(spec.objectiveBands().equals(Arrays.asList(level.objectiveBands())),
                    "Level catalog bands should match runtime level.");
        }
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

    private static boolean hasTransparentCorners(String path) throws IOException {
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
                int alpha = (image.getRGB(corner[0], corner[1]) >>> 24) & 0xff;
                if (alpha > 5) {
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

    private static void checkBearAttackRecordsCause() throws IOException {
        Simulation simulation = new Simulation(8, 8, 151L);
        Position bearPosition = new Position(2, 2);
        Position humanPosition = new Position(3, 2);
        Bear bear = new Bear();
        require(simulation.placeOrganism(bearPosition, bear), "Bear should place for attack test.");
        require(simulation.addHuman(humanPosition), "Human should place next to bear.");

        bear.tick(simulation, bearPosition);

        require(simulation.count(OrganismKind.HUMAN) == 0, "Bear attack should remove the human.");
        require(simulation.recentDeathEvents().stream().anyMatch(death ->
                        death.kind() == OrganismKind.HUMAN && death.cause() == DeathCause.BEAR_ATTACK),
                "Bear kills should be recorded as bear attacks.");
        require(resourceExists("/com/kimbopulus/weird/audio/bear-attack.wav"),
                "Bear attack sound should be bundled on the classpath.");
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

    private static double averageTemperature(Simulation simulation) {
        double total = 0.0;
        int cells = simulation.grid().width() * simulation.grid().height();
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                total += simulation.grid().cellAt(x, y).temperature();
            }
        }
        return total / cells;
    }

    private static double averageMoisture(Simulation simulation) {
        double total = 0.0;
        int cells = simulation.grid().width() * simulation.grid().height();
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                total += simulation.grid().cellAt(x, y).moisture();
            }
        }
        return total / cells;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
