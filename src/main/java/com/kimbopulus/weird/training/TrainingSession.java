package com.kimbopulus.weird.training;

import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.PopulationSnapshot;
import com.kimbopulus.weird.sim.Simulation;

import java.util.List;

public final class TrainingSession {
    private static final int RECALL_LOOKBACK = 24;
    private static final int RECALL_INTERVAL = 42;
    private static final int PROMPT_LIFETIME = 34;

    private int score;
    private int streak;
    private int stableTicks;
    private int drillProgress;
    private int lastPromptTick = -RECALL_INTERVAL;
    private String feedback = "Watch the food chain, then answer the memory prompts.";
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

    public void answer(OrganismKind selected) {
        if (prompt == null) {
            feedback = "No recall prompt is active right now.";
            return;
        }

        if (selected == prompt.answer()) {
            streak++;
            score += 10 + Math.min(10, streak);
            feedback = "Correct. Streak " + streak + ".";
            if (drill == TrainingDrill.RECALL) {
                completeDrill("Recall drill complete.");
            }
        } else {
            streak = 0;
            feedback = "Not this time. The answer was " + label(prompt.answer()) + ".";
        }
        prompt = null;
    }

    public void noteAction(String tool, String target) {
        feedback = tool + " used. " + target;
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

        if (prompt != null || current.tick() - lastPromptTick < RECALL_INTERVAL || current.tick() < RECALL_LOOKBACK) {
            return;
        }

        List<PopulationSnapshot> history = simulation.recentHistory(RECALL_LOOKBACK + 2);
        if (history.size() <= RECALL_LOOKBACK) {
            return;
        }

        PopulationSnapshot past = history.get(0);
        prompt = new TrainingPrompt(
                "Recall: most common " + RECALL_LOOKBACK + " ticks ago?",
                past.mostCommonKind(),
                current.tick()
        );
        lastPromptTick = current.tick();
    }

    private boolean isBalanced(PopulationSnapshot snapshot) {
        return snapshot.plants() >= 180
                && snapshot.plants() <= 1100
                && snapshot.rabbits() >= 12
                && snapshot.rabbits() <= 105
                && snapshot.wolves() >= 2
                && snapshot.wolves() <= 16;
    }

    private String label(OrganismKind kind) {
        return switch (kind) {
            case PLANT -> "plants";
            case RABBIT -> "rabbits";
            case WOLF -> "wolves";
        };
    }
}
