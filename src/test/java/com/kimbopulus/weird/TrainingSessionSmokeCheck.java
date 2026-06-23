package com.kimbopulus.weird;

import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.training.TrainingPrompt;
import com.kimbopulus.weird.training.TrainingSession;

public final class TrainingSessionSmokeCheck {
    private TrainingSessionSmokeCheck() {
    }

    public static void main(String[] args) {
        Simulation simulation = new Simulation(32, 22, 12L);
        simulation.seedPlants(130);
        simulation.seedRabbits(24);
        simulation.seedWolves(3);

        TrainingSession training = new TrainingSession();
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
        int oldScore = training.score();
        training.answer(prompt.answer());
        require(training.score() > oldScore, "Correct answer should increase score.");
        require(training.streak() == 1, "Correct answer should start a streak.");

        System.out.printf("Training check passed: score=%d streak=%d%n", training.score(), training.streak());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}

