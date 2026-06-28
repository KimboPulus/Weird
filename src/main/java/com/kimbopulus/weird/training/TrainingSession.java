package com.kimbopulus.weird.training;

import com.kimbopulus.weird.progression.ProgressionProfile;
import com.kimbopulus.weird.sim.PopulationSnapshot;
import com.kimbopulus.weird.sim.Simulation;
import java.util.List;
import java.util.Random;

public final class TrainingSession {
    private static final int BASE_RECALL_INTERVAL = 40;
    private static final List<String> TREND_CHOICES = List.of("Rising", "Stable", "Falling");

    private final Random random = new Random();
    private final ProgressionProfile progression;
    private int score;
    private int streak;
    private int stableTicks;
    private int levelProgress;
    private int lastPromptTick = -BASE_RECALL_INTERVAL;
    private String feedback = "Hold the ecosystem steady.";
    private TrainingPrompt prompt;
    private TrainingLevel level = TrainingLevel.STEADY_START;
    private FocusRule focusRule = FocusRule.NORMAL;
    private boolean levelComplete;
    private int lastLevelReward;
    private boolean levelFailed;
    private int dangerTicks;
    private String dangerReason;
    private int gardenerActions;
    private int recallAnswers;

    public TrainingSession() {
        this(ProgressionProfile.loadDefault());
    }

    public TrainingSession(ProgressionProfile progression) {
        this.progression = progression;
    }

    public void update(Simulation simulation) {
        PopulationSnapshot current = simulation.currentSnapshot();
        if (levelComplete || levelFailed) {
            return;
        }
        updateDanger(current);
        if (levelFailed) {
            return;
        }
        updateStability(current);
        updateLevel(current);
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
        return levelProgress;
    }

    public TrainingDrill drill() {
        return level.drill();
    }

    public int drillTarget() {
        return level.target();
    }

    public int levelNumber() {
        return level.ordinal() + 1;
    }

    public int levelCount() {
        return TrainingLevel.values().length;
    }

    public String levelTitle() {
        return level.title();
    }

    public String objective() {
        return level.objective();
    }

    public FocusRule focusRule() {
        return focusRule;
    }

    public int memorySpan() {
        return recallLookback();
    }

    public boolean levelComplete() {
        return levelComplete;
    }

    public int lastLevelReward() {
        return lastLevelReward;
    }

    public boolean levelFailed() {
        return levelFailed;
    }

    public String dangerWarning() {
        if (levelFailed || dangerTicks < 5 || dangerReason == null) {
            return null;
        }
        return "Warning: " + dangerReason + " (" + dangerTicks + "/14)";
    }

    public String failureReason() {
        return dangerReason == null ? "The ecosystem collapsed." : dangerReason + ".";
    }

    public String contextHint() {
        if (levelFailed) {
            return "Try a different tool strategy after restarting.";
        }
        if (dangerWarning() != null) {
            return "Fix the warning before the level is lost.";
        }
        if (level == TrainingLevel.STEADY_START) {
            if (gardenerActions == 0) {
                return "Try one tool on the board.";
            }
            if (gardenerActions < 3) {
                return "Watch what changes after each tool.";
            }
            return "Hold balance until the bar fills.";
        }
        if (level == TrainingLevel.MEMORY_SCAN && recallAnswers == 0) {
            return "Watch the trend lines before recall appears.";
        }
        return null;
    }

    public String feedback() {
        return feedback;
    }

    public TrainingPrompt prompt() {
        return prompt;
    }

    public ProgressionProfile progression() {
        return progression;
    }

    public String focusGoal() {
        return level.title() + ": " + level.objective();
    }

    public String balanceStatus(PopulationSnapshot snapshot) {
        if (isBalanced(snapshot)) {
            return "Balance: steady";
        }
        if (snapshot.rabbits() < 12) {
            return "Need rabbits";
        }
        if (snapshot.wolves() < 2) {
            return "Need wolves";
        }
        if (snapshot.plants() > 1100) {
            return "Too many plants";
        }
        if (snapshot.rabbits() > 105) {
            return "Too many rabbits";
        }
        return "Balance: changing";
    }

    public void answer(int selectedIndex) {
        if (prompt == null) {
            feedback = "No recall prompt is active right now.";
            return;
        }

        recallAnswers++;
        if (selectedIndex == prompt.answerIndex()) {
            streak++;
            awardPoints(10 + Math.min(10, streak));
            feedback = "Correct. Streak " + streak + ".";
            if (level.drill() == TrainingDrill.RECALL) {
                levelProgress++;
                if (levelProgress >= level.target()) {
                    completeLevel();
                }
            }
        } else {
            streak = 0;
            feedback = "Answer: " + prompt.answerLabel() + ".";
        }
        prompt = null;
    }

    public void noteAction(String tool, String target) {
        gardenerActions++;
        feedback = tool + " used.";
    }

    public boolean advanceLevel() {
        if (!levelComplete || levelFailed) {
            return false;
        }
        level = level.next();
        levelProgress = 0;
        levelComplete = false;
        prompt = null;
        focusRule = chooseFocusRule();
        feedback = "Level " + levelNumber() + " started.";
        return true;
    }

    public boolean restartLevel() {
        if (!levelFailed) {
            return false;
        }
        score = Math.max(0, score - 15);
        streak = 0;
        stableTicks = 0;
        levelProgress = 0;
        lastPromptTick = -BASE_RECALL_INTERVAL;
        prompt = null;
        levelComplete = false;
        levelFailed = false;
        dangerTicks = 0;
        dangerReason = null;
        gardenerActions = 0;
        recallAnswers = 0;
        feedback = "Level restarted. -15 run score.";
        return true;
    }

    public void reset() {
        score = 0;
        streak = 0;
        stableTicks = 0;
        levelProgress = 0;
        lastPromptTick = -BASE_RECALL_INTERVAL;
        feedback = "Hold the ecosystem steady.";
        prompt = null;
        level = TrainingLevel.STEADY_START;
        focusRule = FocusRule.NORMAL;
        levelComplete = false;
        lastLevelReward = 0;
        levelFailed = false;
        dangerTicks = 0;
        dangerReason = null;
    }

    private void updateStability(PopulationSnapshot snapshot) {
        if (isBalanced(snapshot)) {
            stableTicks++;
            if (stableTicks > 0 && stableTicks % 25 == 0) {
                awardPoints(5);
                feedback = "Balance held. +5";
            }
        } else {
            stableTicks = 0;
        }
    }

    private void updateLevel(PopulationSnapshot snapshot) {
        if (level.drill() == TrainingDrill.RECALL) {
            return;
        }

        boolean onTrack = switch (level.drill()) {
            case BALANCE -> isBalanced(snapshot);
            case PREDATORS -> snapshot.wolves() >= 3;
            case OVERGROWTH -> snapshot.plants() < 900;
            case CLIMATE_ALERT -> snapshot.averageMoisture() >= 0.34
                    && snapshot.averageMoisture() <= 0.72
                    && snapshot.averageTemperature() >= 12.0
                    && snapshot.averageTemperature() <= 30.0;
            case RECALL -> false;
        };

        if (!onTrack) {
            levelProgress = 0;
            return;
        }

        levelProgress++;
        if (levelProgress >= level.target()) {
            completeLevel();
        }
    }

    private void completeLevel() {
        lastLevelReward = 40 + levelNumber() * 5;
        awardPoints(lastLevelReward);
        levelProgress = level.target();
        levelComplete = true;
        prompt = null;
        feedback = "Level complete. +" + lastLevelReward + " tokens.";
    }

    private void updateDanger(PopulationSnapshot snapshot) {
        String currentDanger = dangerReason(snapshot);
        if (currentDanger == null) {
            dangerTicks = 0;
            dangerReason = null;
            return;
        }

        if (currentDanger.equals(dangerReason)) {
            dangerTicks++;
        } else {
            dangerReason = currentDanger;
            dangerTicks = 1;
        }
        if (dangerTicks >= 14) {
            levelFailed = true;
            prompt = null;
            feedback = "Level lost. Restart to try again.";
        }
    }

    private String dangerReason(PopulationSnapshot snapshot) {
        if (snapshot.plants() < 70) {
            return "plants are near extinction";
        }
        if (snapshot.rabbits() < 6) {
            return "rabbits are near extinction";
        }
        if (snapshot.wolves() < 1) {
            return "wolves are extinct";
        }
        if (snapshot.rabbits() > 180) {
            return "rabbits are out of control";
        }
        if (snapshot.averageMoisture() < 0.14) {
            return "the soil is critically dry";
        }
        if (snapshot.averageMoisture() > 0.90) {
            return "the terrarium is flooded";
        }
        if (snapshot.averageTemperature() < 0.0 || snapshot.averageTemperature() > 40.0) {
            return "temperature is lethal";
        }
        return null;
    }

    private FocusRule chooseFocusRule() {
        if (level.ordinal() < TrainingLevel.CANOPY_CONTROL.ordinal()) {
            return FocusRule.NORMAL;
        }
        return random.nextBoolean() ? FocusRule.NORMAL : FocusRule.OPPOSITE;
    }

    private void awardPoints(int points) {
        score += points;
        progression.addFocusXp(points);
    }

    private void updatePrompt(Simulation simulation, PopulationSnapshot current) {
        if (prompt != null && current.tick() - prompt.createdAtTick() > promptLifetime()) {
            streak = 0;
            feedback = "Recall missed.";
            prompt = null;
        }

        int lookback = recallLookback();
        if (prompt != null || current.tick() - lastPromptTick < recallInterval() || current.tick() < lookback) {
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
        int actualTrend = newValue > oldValue + tolerance ? 0 : newValue < oldValue - tolerance ? 2 : 1;
        int answerIndex = focusRule == FocusRule.OPPOSITE ? oppositeTrend(actualTrend) : actualTrend;
        prompt = new TrainingPrompt(
                metric.label + " over " + lookback + " ticks?",
                TREND_CHOICES,
                answerIndex,
                current.tick(),
                lookback,
                focusRule
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

    private int recallInterval() {
        if (streak >= 5) {
            return 30;
        }
        if (streak >= 2) {
            return 35;
        }
        return BASE_RECALL_INTERVAL;
    }

    private int promptLifetime() {
        if (streak >= 5) {
            return 28;
        }
        if (streak >= 2) {
            return 34;
        }
        return 44;
    }

    private int oppositeTrend(int trend) {
        return trend == 0 ? 2 : trend == 2 ? 0 : 1;
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
