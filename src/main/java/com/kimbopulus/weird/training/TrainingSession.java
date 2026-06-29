package com.kimbopulus.weird.training;

import com.kimbopulus.weird.progression.ProgressionProfile;
import com.kimbopulus.weird.sim.PopulationSnapshot;
import com.kimbopulus.weird.sim.Simulation;

public final class TrainingSession {
    private final ProgressionProfile progression;
    private int score;
    private int stableTicks;
    private int levelProgress;
    private String feedback = "Hold the ecosystem steady.";
    private TrainingLevel level = TrainingLevel.STEADY_START;
    private boolean levelComplete;
    private int lastLevelReward;
    private boolean levelFailed;
    private int dangerTicks;
    private String dangerReason;
    private String failureDetail;
    private int gardenerActions;

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
    }

    public int score() {
        return score;
    }

    public int streak() {
        return stableTicks;
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

    public String challengeText() {
        return level.challenge();
    }

    public int memorySpan() {
        return 0;
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
        if (failureDetail != null) {
            return failureDetail;
        }
        return dangerReason == null ? "The ecosystem collapsed." : dangerReason + ".";
    }

    public String contextHint() {
        if (levelFailed) {
            return "Restart the run and try a smaller move.";
        }
        if (dangerWarning() != null) {
            return "Fix the band that is out of range first.";
        }
        if (level == TrainingLevel.STEADY_START) {
            if (gardenerActions == 0) {
                return "Start with Rain, Compost, or one planted creature.";
            }
            if (gardenerActions < 3) {
                return "Watch the current values after each click.";
            }
            return "Keep every line inside the target band.";
        }
        if (level == TrainingLevel.CLIMATE_CONTROL) {
            return "Rain cools and wets a 4 x 4 patch. Drought dries and warms a 4 x 4 patch.";
        }
        return "Hold the target bands in range.";
    }

    public String feedback() {
        return feedback;
    }

    public ProgressionProfile progression() {
        return progression;
    }

    public String focusGoal() {
        return level.title() + ": " + level.objective();
    }

    public String balanceStatus(PopulationSnapshot snapshot) {
        return level.balanceTarget().status(snapshot);
    }

    public String balanceGuide(PopulationSnapshot snapshot, int boardCells) {
        return level.balanceTarget().guide(snapshot, boardCells);
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
        stableTicks = 0;
        levelProgress = 0;
        levelComplete = false;
        failureDetail = null;
        dangerTicks = 0;
        dangerReason = null;
        gardenerActions = 0;
        feedback = "Level " + levelNumber() + " started.";
        return true;
    }

    public boolean restartLevel() {
        if (!levelFailed) {
            return false;
        }
        score = Math.max(0, score - 15);
        stableTicks = 0;
        levelProgress = 0;
        levelComplete = false;
        levelFailed = false;
        dangerTicks = 0;
        dangerReason = null;
        failureDetail = null;
        gardenerActions = 0;
        feedback = "Level restarted. -15 run score.";
        return true;
    }

    public void reset() {
        score = 0;
        stableTicks = 0;
        levelProgress = 0;
        feedback = "Hold the ecosystem steady.";
        level = TrainingLevel.STEADY_START;
        levelComplete = false;
        lastLevelReward = 0;
        levelFailed = false;
        dangerTicks = 0;
        dangerReason = null;
        failureDetail = null;
        gardenerActions = 0;
    }

    private void updateStability(PopulationSnapshot snapshot) {
        if (level.balanceTarget().matches(snapshot)) {
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
        boolean onTrack = levelOnTrack(snapshot);
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
            failureDetail = dangerReason == null ? "The ecosystem collapsed." : level.balanceTarget().status(snapshot);
            feedback = "Level lost: " + failureDetail + "\nRestart to try again.";
        }
    }

    private String dangerReason(PopulationSnapshot snapshot) {
        return level.balanceTarget().category(snapshot);
    }

    private boolean levelOnTrack(PopulationSnapshot snapshot) {
        return level.balanceTarget().matches(snapshot);
    }

    private void awardPoints(int points) {
        score += points;
        progression.addFocusXp(points);
    }
}
