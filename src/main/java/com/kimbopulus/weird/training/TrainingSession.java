package com.kimbopulus.weird.training;

import com.kimbopulus.weird.progression.ProgressionProfile;
import com.kimbopulus.weird.sim.PopulationSnapshot;
import com.kimbopulus.weird.sim.Simulation;

import java.util.function.LongSupplier;

public final class TrainingSession {
    private static final long DEFAULT_FAILURE_GRACE_MS = 30_000L;
    private static final long PLANT_FAILURE_GRACE_MS = 60_000L;

    private final ProgressionProfile progression;
    private final LongSupplier clock;
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
    private String dangerDetail;
    private long dangerStartedAtMillis = -1L;
    private long dangerPausedAtMillis = -1L;
    private String failureDetail;
    private int gardenerActions;

    public TrainingSession() {
        this(ProgressionProfile.loadDefault(), System::currentTimeMillis);
    }

    public TrainingSession(ProgressionProfile progression) {
        this(progression, System::currentTimeMillis);
    }

    public TrainingSession(ProgressionProfile progression, LongSupplier clock) {
        this.progression = progression;
        this.clock = clock;
    }

    public void update(Simulation simulation) {
        PopulationSnapshot current = simulation.currentSnapshot();
        if (levelFailed) {
            return;
        }
        updateDanger(current, !levelComplete);
        if (levelFailed || levelComplete) {
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

    public boolean gameComplete() {
        return levelComplete && level == TrainingLevel.FLEX_SHIFT;
    }

    public int lastLevelReward() {
        return lastLevelReward;
    }

    public boolean levelFailed() {
        return levelFailed;
    }

    public String dangerWarning() {
        if (levelFailed || dangerReason == null || dangerDetail == null) {
            return null;
        }
        return dangerDetail + " | " + dangerCountdownLabel();
    }

    public String dangerDetail() {
        return dangerDetail;
    }

    public String dangerAction() {
        return actionForCategory(dangerReason);
    }

    public String dangerCountdownLabel() {
        if (levelFailed || dangerReason == null || dangerStartedAtMillis < 0L) {
            return null;
        }
        long graceMs = failureGraceMs(dangerReason);
        long remainingMs = Math.max(0L, graceMs - (warningClockMillis() - dangerStartedAtMillis));
        return String.format("%.1fs to fix", remainingMs / 1000.0);
    }

    public String failureReason() {
        if (failureDetail != null) {
            return failureDetail;
        }
        return dangerDetail == null ? "The ecosystem collapsed." : dangerDetail + " stayed out of range too long.";
    }

    public String failureAction() {
        return actionForCategory(dangerReason);
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
                return "Start with Rain or Compost, then watch the board react.";
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

    public boolean objectiveOnTrack(PopulationSnapshot snapshot) {
        return level.objectiveMatches(snapshot);
    }

    public String objectiveStatus(PopulationSnapshot snapshot) {
        return level.objectiveStatus(snapshot);
    }

    public String balanceStatus(PopulationSnapshot snapshot) {
        return level.balanceTarget().status(snapshot);
    }

    public String currentSummary(PopulationSnapshot snapshot) {
        return level.balanceTarget().currentSummary(snapshot);
    }

    public String balanceGuide(PopulationSnapshot snapshot, int boardCells) {
        return level.balanceTarget().guide(snapshot, boardCells);
    }

    public void noteAction(String tool, String target) {
        gardenerActions++;
        feedback = tool + " used.";
    }

    public void pauseDangerClock() {
        if (dangerPausedAtMillis < 0L) {
            dangerPausedAtMillis = clock.getAsLong();
        }
    }

    public void resumeDangerClock() {
        if (dangerPausedAtMillis < 0L) {
            return;
        }
        if (dangerStartedAtMillis >= 0L) {
            dangerStartedAtMillis += Math.max(0L, clock.getAsLong() - dangerPausedAtMillis);
        }
        dangerPausedAtMillis = -1L;
    }

    public boolean advanceLevel() {
        if (!levelComplete || levelFailed || gameComplete()) {
            return false;
        }
        level = level.next();
        stableTicks = 0;
        levelProgress = 0;
        levelComplete = false;
        failureDetail = null;
        dangerTicks = 0;
        dangerReason = null;
        dangerDetail = null;
        dangerStartedAtMillis = -1L;
        dangerPausedAtMillis = -1L;
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
        dangerDetail = null;
        dangerStartedAtMillis = -1L;
        dangerPausedAtMillis = -1L;
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
        dangerDetail = null;
        dangerStartedAtMillis = -1L;
        dangerPausedAtMillis = -1L;
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

    private void updateDanger(PopulationSnapshot snapshot, boolean allowFailure) {
        long now = warningClockMillis();
        if (dangerReason != null) {
            String activeDetail = level.balanceTarget().statusForCategory(snapshot, dangerReason);
            if (activeDetail != null) {
                dangerTicks++;
                dangerDetail = activeDetail;
                maybeFailDanger(allowFailure, now);
                return;
            }
        }

        String currentDanger = dangerReason(snapshot);
        if (currentDanger == null) {
            dangerTicks = 0;
            dangerReason = null;
            dangerDetail = null;
            dangerStartedAtMillis = -1L;
            return;
        }

        String currentDetail = level.balanceTarget().status(snapshot);
        if (currentDanger.equals(dangerReason)) {
            dangerTicks++;
        } else {
            dangerReason = currentDanger;
            dangerTicks = 1;
            dangerStartedAtMillis = now;
        }
        dangerDetail = currentDetail;
        maybeFailDanger(allowFailure, now);
    }

    private void maybeFailDanger(boolean allowFailure, long now) {
        long graceMs = failureGraceMs(dangerReason);
        if (allowFailure && dangerStartedAtMillis >= 0L && now - dangerStartedAtMillis >= graceMs) {
            levelFailed = true;
            failureDetail = dangerDetail == null
                    ? "The ecosystem collapsed."
                    : dangerDetail + String.format(" stayed out of range for %.1f seconds.", graceMs / 1000.0);
            feedback = "Level lost: " + failureDetail + "\nRestart to try again.";
        }
    }

    private long warningClockMillis() {
        return dangerPausedAtMillis >= 0L ? dangerPausedAtMillis : clock.getAsLong();
    }

    private long failureGraceMs(String category) {
        return "Plants low".equals(category) || "Plants high".equals(category)
                ? PLANT_FAILURE_GRACE_MS
                : DEFAULT_FAILURE_GRACE_MS;
    }

    private String dangerReason(PopulationSnapshot snapshot) {
        return level.balanceTarget().category(snapshot);
    }

    private boolean levelOnTrack(PopulationSnapshot snapshot) {
        return level.objectiveMatches(snapshot);
    }

    private void awardPoints(int points) {
        score += points;
        progression.addFocusXp(points);
    }

    private String actionForCategory(String category) {
        if (category == null) {
            return null;
        }
        return switch (category) {
            case "Plants low" -> "Fix: Rain or Compost.";
            case "Plants high" -> "Fix: Drought dense patch or add Rabbit.";
            case "Rabbits low" -> "Fix: Add Rabbit.";
            case "Rabbits high" -> "Fix: Add Wolf.";
            case "Wolves low" -> "Fix: Add Wolf.";
            case "Wolves high" -> "Fix: Stop wolves. Add Rabbits.";
            case "Humans low" -> "Fix: Add Human.";
            case "Humans high" -> "Fix: Add Bear.";
            case "Bears low" -> "Fix: Add Bear if humans spike.";
            case "Bears high" -> "Fix: Stop bears. Let them leave.";
            case "Moisture low" -> "Fix: Rain driest 4 x 4.";
            case "Moisture high" -> "Fix: Drought wettest 4 x 4.";
            case "Temperature low" -> "Fix: Drought cold patch.";
            case "Temperature high" -> "Fix: Rain hottest 4 x 4.";
            default -> null;
        };
    }
}
