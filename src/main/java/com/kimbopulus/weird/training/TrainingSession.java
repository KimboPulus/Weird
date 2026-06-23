package com.kimbopulus.weird.training;

import com.kimbopulus.weird.sim.PopulationSnapshot;
import com.kimbopulus.weird.sim.Simulation;

import java.util.List;
import java.util.Random;

public final class TrainingSession {
    private static final int RECALL_INTERVAL = 38;
    private static final int PROMPT_LIFETIME = 34;
    private static final List<String> TREND_CHOICES = List.of("Rising", "Stable", "Falling");

    private final Random random = new Random();
    private int score;
    private int streak;
    private int stableTicks;
    private int drillProgress;
    private int lastPromptTick = -RECALL_INTERVAL;
    private String feedback = "Watch the food chain.";
    private TrainingPrompt prompt;
    private TrainingDrill drill = TrainingDrill.BALANCE;

    public void update(Simulation simulation) {
        PopulationSnapshot current = simulation.currentSnapshot();
        updateStability(current);
        updateDrill(current);
        updatePrompt(simulation, current);
    }

    public int score() {
        return score;
    }

    public int streak() {
        return streak;
    }

    public int stableTicks() {
        return stableTicks;
    }

    public int drillProgress() {
        return drillProgress;
    }

    public TrainingDrill drill() {
        return drill;
    }

    public int drillTarget() {
        return drill == TrainingDrill.BALANCE ? 35 : drill == TrainingDrill.RECALL ? 1 : 30;
    }

    public String feedback() {
        return feedback;
    }

    public TrainingPrompt prompt() {
        return prompt;
    }

    public String focusGoal() {
        return drill.title() + ": " + drill.goal();
    }

    public String balanceStatus(PopulationSnapshot snapshot) {
        if (isBalanced(snapshot)) {
            return "Balanced for " + stableTicks + " ticks";
        }
        if (snapshot.rabbits() < 12) {
            return "Rabbits are low";
        }
        if (snapshot.wolves() < 2) {
            return "Predators are missing";
        }
        if (snapshot.plants() > 1100) {
            return "Plants are crowding the board";
        }
        if (snapshot.rabbits() > 105) {
            return "Rabbits are overgrazing";
        }
        return "Balance is shifting";
    }

    public void answer(int selectedIndex) {
        if (prompt == null) {
            feedback = "No recall prompt is active right now.";
            return;
        }

        if (selectedIndex == prompt.answerIndex()) {
            streak++;
            score += 10 + Math.min(10, streak);
            feedback = "Correct. " + prompt.lookback() + "-tick recall held. Streak " + streak + ".";
            if (drill == TrainingDrill.RECALL) {
                completeDrill("Recall drill complete.");
            }
        } else {
            streak = 0;
            feedback = "Not this time. The trend was " + prompt.answerLabel().toLowerCase() + ".";
        }
        prompt = null;
    }

    public void noteAction(String tool, String target) {
        feedback = tool + " used. " + target;
    }

    public void reset() {
        score = 0;
        streak = 0;
        stableTicks = 0;
        drillProgress = 0;
        lastPromptTick = -RECALL_INTERVAL;
        feedback = "Watch the food chain.";
        prompt = null;
        drill = TrainingDrill.BALANCE;
    }

    private void updateStability(PopulationSnapshot snapshot) {
        if (isBalanced(snapshot)) {
            stableTicks++;
            if (stableTicks > 0 && stableTicks % 25 == 0) {
                score += 5;
                feedback = "Good control: balance held for " + stableTicks + " ticks.";
            }
        } else {
            stableTicks = 0;
        }
    }

    private void updateDrill(PopulationSnapshot snapshot) {
        if (drill == TrainingDrill.RECALL) {
            return;
        }

        boolean onTrack = switch (drill) {
            case BALANCE -> isBalanced(snapshot);
            case PREDATORS -> snapshot.wolves() >= 3;
            case OVERGROWTH -> snapshot.plants() < 900;
            case RECALL -> false;
        };

        if (!onTrack) {
            drillProgress = 0;
            return;
        }

        drillProgress++;
        int target = drillTarget();
        if (drillProgress >= target) {
            completeDrill(drill.title() + " complete.");
        }
    }

    private void completeDrill(String message) {
        score += 25;
        feedback = message + " New drill loaded.";
        drill = drill.next();
        drillProgress = 0;
    }

    private void updatePrompt(Simulation simulation, PopulationSnapshot current) {
        if (prompt != null && current.tick() - prompt.createdAtTick() > PROMPT_LIFETIME) {
            streak = 0;
            feedback = "Prompt expired. Keep scanning the board.";
            prompt = null;
        }

        int lookback = recallLookback();
        if (prompt != null || current.tick() - lastPromptTick < RECALL_INTERVAL || current.tick() < lookback) {
            return;
        }

        List<PopulationSnapshot> history = simulation.recentHistory(lookback + 1);
        if (history.size() <= lookback) {
            return;
        }

        PopulationSnapshot past = history.get(0);
        PopulationMetric metric = PopulationMetric.values()[random.nextInt(PopulationMetric.values().length)];
        int oldValue = metric.value(past);
        int newValue = metric.value(current);
        int tolerance = Math.max(2, (int) Math.ceil(Math.max(1, oldValue) * 0.06));
        int answerIndex = newValue > oldValue + tolerance ? 0 : newValue < oldValue - tolerance ? 2 : 1;
        prompt = new TrainingPrompt(
                "Over the last " + lookback + " ticks, were " + metric.label + " rising, stable, or falling?",
                TREND_CHOICES,
                answerIndex,
                current.tick(),
                lookback
        );
        lastPromptTick = current.tick();
    }

    private int recallLookback() {
        if (streak >= 5) {
            return 32;
        }
        if (streak >= 2) {
            return 20;
        }
        return 10;
    }

    private boolean isBalanced(PopulationSnapshot snapshot) {
        return snapshot.plants() >= 180
                && snapshot.plants() <= 1100
                && snapshot.rabbits() >= 12
                && snapshot.rabbits() <= 105
                && snapshot.wolves() >= 2
                && snapshot.wolves() <= 16;
    }

    private enum PopulationMetric {
        PLANTS("plants") {
            @Override
            int value(PopulationSnapshot snapshot) {
                return snapshot.plants();
            }
        },
        RABBITS("rabbits") {
            @Override
            int value(PopulationSnapshot snapshot) {
                return snapshot.rabbits();
            }
        },
        WOLVES("wolves") {
            @Override
            int value(PopulationSnapshot snapshot) {
                return snapshot.wolves();
            }
        };

        private final String label;

        PopulationMetric(String label) {
            this.label = label;
        }

        abstract int value(PopulationSnapshot snapshot);
    }
}
