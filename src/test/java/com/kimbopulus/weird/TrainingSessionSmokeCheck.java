package com.kimbopulus.weird;

import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.progression.ProgressionProfile;
import com.kimbopulus.weird.progression.ShopItem;
import com.kimbopulus.weird.training.TrainingDrill;
import com.kimbopulus.weird.training.TrainingPrompt;
import com.kimbopulus.weird.training.TrainingSession;

public final class TrainingSessionSmokeCheck {
    private TrainingSessionSmokeCheck() {
    }

    public static void main(String[] args) {
        checkLevelAdvance();
        checkLevelFailure();
        checkShopPurchases();

        Simulation simulation = new Simulation(32, 22, 12L);
        simulation.seedPlants(130);
        simulation.seedRabbits(24);
        simulation.seedWolves(3);
        simulation.seedHumans(6);

        TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
        require(training.drill() == TrainingDrill.BALANCE, "Training should start with the balance drill.");
        require(training.contextHint() != null, "The first level should offer contextual guidance.");
        training.noteAction("Rain", "test");
        require(training.contextHint().contains("Watch"), "Guidance should react to player practice.");
        TrainingPrompt prompt = null;
        for (int i = 0; i < 80; i++) {
            simulation.tick();
            training.update(simulation);
            if (training.prompt() != null) {
                prompt = training.prompt();
                break;
            }
        }

        require(prompt != null, "Training prompt did not appear.");
        require(training.drillProgress() >= 0, "Drill progress should be available.");
        int oldScore = training.score();
        require(prompt.lookback() == 10, "Recall should begin with a short lookback.");
        require(prompt.choices().size() == 3, "Recall should offer three trend choices.");
        training.answer(prompt.answerIndex());
        require(training.score() > oldScore, "Correct answer should increase score.");
        require(training.progression().focusXp() == training.score(), "Earned score should become persistent XP.");
        require(training.streak() == 1, "Correct answer should start a streak.");

        answerNextPromptCorrectly(simulation, training);
        TrainingPrompt harderPrompt = waitForPrompt(simulation, training, 100);
        require(harderPrompt.lookback() == 20, "A recall streak should increase the lookback.");

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
        require(training.drill() == TrainingDrill.BALANCE, "Every level should train ecosystem balance.");
        require(training.score() >= 45, "Level completion should award points.");
    }

    private static void answerNextPromptCorrectly(Simulation simulation, TrainingSession training) {
        TrainingPrompt prompt = waitForPrompt(simulation, training, 100);
        training.answer(prompt.answerIndex());
    }

    private static TrainingPrompt waitForPrompt(Simulation simulation, TrainingSession training, int limit) {
        for (int i = 0; i < limit; i++) {
            simulation.tick();
            training.update(simulation);
            if (training.levelComplete()) {
                training.advanceLevel();
            }
            if (training.prompt() != null) {
                return training.prompt();
            }
        }
        throw new IllegalStateException("Training prompt did not appear within the expected interval.");
    }

    private static void checkShopPurchases() {
        ProgressionProfile profile = ProgressionProfile.inMemory();
        require(!profile.buy(ShopItem.RAIN_BARREL), "An upgrade should require enough tokens.");
        profile.addFocusXp(100);
        require(profile.buy(ShopItem.RAIN_BARREL), "An affordable upgrade should be purchased.");
        require(profile.owns(ShopItem.RAIN_BARREL), "Purchased upgrades should be owned.");
        require(profile.tokens() == 40, "A purchase should deduct its token cost.");
        require(profile.totalScore() == 100, "Spending tokens must not reduce total score.");
        require(!profile.buy(ShopItem.RAIN_BARREL), "An upgrade cannot be purchased twice.");
    }

    private static void checkLevelFailure() {
        Simulation simulation = new Simulation(48, 32, 91L);
        simulation.seedPlants(220);
        simulation.seedRabbits(48);
        simulation.seedWolves(4);
        simulation.seedHumans(6);
        TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
        removeSpecies(simulation, com.kimbopulus.weird.sim.OrganismKind.WOLF);

        for (int i = 0; i < 6; i++) {
            simulation.tick();
            training.update(simulation);
        }
        require(training.dangerWarning() != null, "A sustained crisis should show a warning.");

        for (int i = 6; i < 14; i++) {
            simulation.tick();
            training.update(simulation);
        }
        require(training.levelFailed(), "An unresolved extinction should lose the level.");
        require(training.restartLevel(), "A failed level should be restartable.");
        require(!training.levelFailed(), "Restart should clear the failed state.");
        require(training.levelNumber() == 1, "Restart should keep the current level.");
    }

    private static void removeSpecies(
            Simulation simulation,
            com.kimbopulus.weird.sim.OrganismKind kind
    ) {
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                if (simulation.organismAt(x, y) != null && simulation.organismAt(x, y).kind() == kind) {
                    simulation.removeOrganism(new com.kimbopulus.weird.sim.Position(x, y));
                }
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
