package com.kimbopulus.weird;

import com.kimbopulus.weird.progression.ProgressionProfile;
import com.kimbopulus.weird.progression.ShopItem;
import com.kimbopulus.weird.sim.PopulationSnapshot;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.RabbitSex;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.training.TrainingDrill;
import com.kimbopulus.weird.training.TrainingLevel;
import com.kimbopulus.weird.training.TrainingSession;

import java.util.concurrent.atomic.AtomicLong;

public final class TrainingSessionSmokeCheck {
    private TrainingSessionSmokeCheck() {
    }

    public static void main(String[] args) {
        checkLevelAdvance();
        checkLevelFailure();
        checkPlantOvergrowthFailure();
        checkRecoveredWarningClearsAfterCompletion();
        checkShopPurchases();
        checkObjectiveProgressUsesDisplayedGoal();
        checkBalanceGuide();

        Simulation simulation = new Simulation(32, 22, 12L);
        simulation.seedPlants(130);
        simulation.seedRabbits(24);
        simulation.seedWolves(3);
        simulation.seedHumans(6);

        TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
        require(training.drill() == TrainingDrill.BALANCE, "Training should start with the balance drill.");
        require(training.contextHint() != null, "The first level should offer contextual guidance.");
        training.noteAction("Rain", "test");
        require(training.contextHint().contains("click") || training.contextHint().contains("square"),
                "Guidance should describe the click-based tools.");

        for (int i = 0; i < 120; i++) {
            simulation.tick();
            training.update(simulation);
        }

        require(training.drillProgress() >= 0, "Drill progress should be available.");
        require(training.streak() >= 0, "Balance streak should be tracked.");

        System.out.printf("Training check passed: score=%d streak=%d%n", training.score(), training.streak());
    }

    private static void checkLevelAdvance() {
        Simulation simulation = new Simulation(48, 32, 31L);
        simulation.seedPlants(220);
        simulation.seedRabbits(48);
        simulation.seedWolves(4);
        simulation.seedHumans(6);
        TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());

        for (int i = 0; i < 20; i++) {
            training.update(simulation);
        }

        require(training.levelNumber() == 1, "A completed level should wait for the player.");
        require(training.levelComplete(), "The first objective should enter the complete state.");
        require(training.advanceLevel(), "The player should be able to continue after completion.");
        require(training.levelNumber() == 2, "Next Level should advance to level 2.");
        require("Keep plants 110-620 and rabbits 6-60".equals(training.objective()),
                "The second level objective should be blunt.");
        require(training.drill() == TrainingDrill.BALANCE, "Every level should train ecosystem balance.");
        require(training.score() >= 45, "Level completion should award points.");
    }

    private static void checkLevelFailure() {
        Simulation simulation = new Simulation(48, 32, 91L);
        simulation.seedPlants(220);
        simulation.seedRabbits(48);
        simulation.seedWolves(4);
        simulation.seedHumans(6);
        AtomicLong now = new AtomicLong();
        TrainingSession training = new TrainingSession(ProgressionProfile.inMemory(), now::get);
        removeSpecies(simulation, OrganismKind.PLANT);
        removeSpecies(simulation, OrganismKind.RABBIT);
        removeSpecies(simulation, OrganismKind.WOLF);
        removeSpecies(simulation, OrganismKind.HUMAN);

        training.update(simulation);
        require(training.dangerWarning() != null, "A sustained crisis should show a warning.");
        require("15.0s to fix".equals(training.dangerCountdownLabel()),
                "The warning should start with a full 15 second grace period.");

        now.addAndGet(14_900L);
        training.update(simulation);
        require(!training.levelFailed(), "The level should not fail before the grace period expires.");

        now.addAndGet(100L);
        training.update(simulation);
        require(training.levelFailed(), "An unresolved extinction should lose the level.");
        require(training.failureReason().startsWith("Plants low ("),
                "A failed level should report the exact reason.");
        require(training.failureReason().contains("range 90-700"),
                "A failed level should include the target range.");
        require(training.failureReason().contains("15.0 seconds"),
                "A failed level should explain that the band stayed bad for 15 seconds.");
        require(training.restartLevel(), "A failed level should be restartable.");
        require(!training.levelFailed(), "Restart should clear the failed state.");
        require(training.levelNumber() == 1, "Restart should keep the current level.");
    }

    private static void checkPlantOvergrowthFailure() {
        Simulation simulation = new Simulation(48, 32, 123L);
        fillPlantsExcept(simulation, 1200);
        placeSupportAnimals(simulation);
        AtomicLong now = new AtomicLong();
        TrainingSession training = new TrainingSession(ProgressionProfile.inMemory(), now::get);

        training.update(simulation);
        require(training.dangerWarning() != null, "Excess plants should trigger a warning.");

        now.addAndGet(15_000L);
        training.update(simulation);
        require(training.levelFailed(), "Excess plants should lose the level.");
    }

    private static void checkRecoveredWarningClearsAfterCompletion() {
        Simulation simulation = new Simulation(48, 32, 211L);
        simulation.seedPlants(220);
        simulation.seedRabbits(48);
        simulation.seedWolves(4);
        simulation.seedHumans(6);
        AtomicLong now = new AtomicLong();
        TrainingSession training = new TrainingSession(ProgressionProfile.inMemory(), now::get);
        forceLevel(training, TrainingLevel.STEADY_START);
        try {
            setBooleanField(training, "levelComplete", true);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }

        removeSpecies(simulation, OrganismKind.WOLF);
        simulation.tick();
        training.update(simulation);
        require(training.dangerWarning() != null, "A completed level should still surface active warnings.");

        simulation.addWolf(new Position(0, 0));
        simulation.addWolf(new Position(1, 0));
        simulation.addWolf(new Position(2, 0));
        simulation.tick();
        training.update(simulation);
        require(training.dangerWarning() == null, "Recovering the board should clear the warning even after completion.");

        now.addAndGet(20_000L);
        training.update(simulation);
        require(!training.levelFailed(), "A completed level should not fail after the player already secured it.");
    }

    private static void checkShopPurchases() {
        ProgressionProfile profile = ProgressionProfile.inMemory();
        require(!profile.buy(ShopItem.RAIN_BARREL), "An upgrade should require enough tokens.");
        profile.addFocusXp(420);
        require(profile.buy(ShopItem.RAIN_BARREL), "An affordable upgrade should be purchased.");
        require(profile.owns(ShopItem.RAIN_BARREL), "Purchased upgrades should be owned.");
        require(profile.tokens() == 10, "A purchase should deduct its token cost.");
        require(profile.totalScore() == 420, "Spending tokens must not reduce total score.");
        require(!profile.buy(ShopItem.RAIN_BARREL), "An upgrade cannot be purchased twice.");
        profile.resetPurchases();
        require(!profile.owns(ShopItem.RAIN_BARREL), "Resetting the shop should clear owned items.");
    }

    private static void checkObjectiveProgressUsesDisplayedGoal() {
        Simulation simulation = new Simulation(48, 32, 52L);
        simulation.seedPlants(220);
        simulation.seedRabbits(18);
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                simulation.grid().cellAt(x, y).reset(0.82, 23.0, 0.42);
            }
        }

        TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
        forceLevel(training, TrainingLevel.MEMORY_SCAN);
        training.update(simulation);

        require(training.drillProgress() == 1,
                "Level progress should follow the displayed plants-and-rabbits objective.");
        require("Objective in range".equals(training.objectiveStatus(simulation.currentSnapshot())),
                "Objective status should confirm when the displayed goal is satisfied.");
        require(training.balanceStatus(simulation.currentSnapshot()).startsWith("Wolves low"),
                "Safety warnings should stay separate from the level objective.");
    }

    private static void checkBalanceGuide() {
        TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
        PopulationSnapshot balanced = new PopulationSnapshot(0, com.kimbopulus.weird.sim.Season.SPRING, 320, 20, 4, 4, 0, 0.50, 0.50, 21.0);
        require("Balance: steady".equals(training.balanceStatus(balanced)), "Healthy populations should read as steady.");
        require(training.currentSummary(balanced).contains("Wolves 4"),
                "The right panel summary should show the current population counts.");
        String guide = training.balanceGuide(balanced, 988);
        require(guide.contains("Fail if:") && guide.contains("outside") && guide.contains("%"),
                "Balance guidance should show the exact failure bands.");
        require(training.balanceStatus(new PopulationSnapshot(0, com.kimbopulus.weird.sim.Season.SPRING, 60, 20, 4, 4, 0, 0.50, 0.50, 21.0)).startsWith("Plants low"),
                "Low plants should be called out.");
    }

    private static void forceLevel(TrainingSession training, TrainingLevel level) {
        try {
            var field = TrainingSession.class.getDeclaredField("level");
            field.setAccessible(true);
            field.set(training, level);

            setBooleanField(training, "levelComplete", false);
            setBooleanField(training, "levelFailed", false);
            setIntField(training, "levelProgress", 0);
            setIntField(training, "stableTicks", 0);
            setIntField(training, "dangerTicks", 0);
            setField(training, "dangerReason", null);
            setField(training, "dangerDetail", null);
            setLongField(training, "dangerStartedAtMillis", -1L);
            setField(training, "failureDetail", null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void setIntField(TrainingSession training, String name, int value) throws ReflectiveOperationException {
        var field = TrainingSession.class.getDeclaredField(name);
        field.setAccessible(true);
        field.setInt(training, value);
    }

    private static void setBooleanField(TrainingSession training, String name, boolean value) throws ReflectiveOperationException {
        var field = TrainingSession.class.getDeclaredField(name);
        field.setAccessible(true);
        field.setBoolean(training, value);
    }

    private static void setLongField(TrainingSession training, String name, long value) throws ReflectiveOperationException {
        var field = TrainingSession.class.getDeclaredField(name);
        field.setAccessible(true);
        field.setLong(training, value);
    }

    private static void setField(TrainingSession training, String name, Object value) throws ReflectiveOperationException {
        var field = TrainingSession.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(training, value);
    }

    private static void removeSpecies(Simulation simulation, OrganismKind kind) {
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                if (simulation.organismAt(x, y) != null && simulation.organismAt(x, y).kind() == kind) {
                    simulation.removeOrganism(new Position(x, y));
                }
            }
        }
    }

    private static void fillPlantsExcept(Simulation simulation, int targetPlants) {
        int placed = 0;
        for (int y = 0; y < simulation.grid().height() && placed < targetPlants; y++) {
            for (int x = 0; x < simulation.grid().width() && placed < targetPlants; x++) {
                if (x < 4 && y < 4) {
                    continue;
                }
                if (simulation.addPlant(new Position(x, y))) {
                    placed++;
                }
            }
        }
    }

    private static void placeSupportAnimals(Simulation simulation) {
        simulation.addRabbit(new Position(0, 0), RabbitSex.MALE);
        simulation.addRabbit(new Position(1, 0), RabbitSex.FEMALE);
        simulation.addRabbit(new Position(2, 0), RabbitSex.FEMALE);
        simulation.addRabbit(new Position(3, 0), RabbitSex.FEMALE);
        simulation.addRabbit(new Position(0, 1), RabbitSex.FEMALE);
        simulation.addRabbit(new Position(1, 1), RabbitSex.FEMALE);
        simulation.addRabbit(new Position(2, 1), RabbitSex.FEMALE);
        simulation.addRabbit(new Position(3, 1), RabbitSex.FEMALE);
        simulation.addWolf(new Position(0, 2));
        simulation.addWolf(new Position(1, 2));
        simulation.addWolf(new Position(2, 2));
        simulation.addHuman(new Position(0, 3));
        simulation.addHuman(new Position(1, 3));
        simulation.addHuman(new Position(2, 3));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
