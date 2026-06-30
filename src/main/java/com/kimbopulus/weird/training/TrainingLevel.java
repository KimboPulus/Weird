package com.kimbopulus.weird.training;

import com.kimbopulus.weird.sim.PopulationSnapshot;

public enum TrainingLevel {
    STEADY_START(
            "Steady Start",
            "Keep plants 90-700",
            "Plants 90-700",
            TrainingDrill.BALANCE,
            20,
            new BalanceTarget(90, 700, 6, 70, 2, 10, 3, 10, 0, 2, 0.25, 0.78, 10.0, 34.0),
            BalanceBand.PLANTS
    ),
    MEMORY_SCAN(
            "Green Rhythm",
            "Keep plants 110-620 and rabbits 6-60",
            "Plants 110-620 and Rabbits 6-60",
            TrainingDrill.BALANCE,
            30,
            new BalanceTarget(110, 620, 6, 60, 2, 9, 3, 9, 0, 2, 0.30, 0.72, 12.0, 31.0),
            BalanceBand.PLANTS,
            BalanceBand.RABBITS
    ),
    PREDATOR_CHECK(
            "Predator Line",
            "Keep wolves 3-9",
            "Wolves 3-9",
            TrainingDrill.BALANCE,
            35,
            new BalanceTarget(120, 560, 5, 54, 3, 9, 3, 9, 0, 2, 0.34, 0.70, 13.0, 30.0),
            BalanceBand.WOLVES
    ),
    CANOPY_CONTROL(
            "Human Footprint",
            "Keep humans 3-8",
            "Humans 3-8",
            TrainingDrill.BALANCE,
            40,
            new BalanceTarget(130, 520, 5, 48, 3, 8, 3, 8, 0, 2, 0.38, 0.66, 14.0, 28.0),
            BalanceBand.HUMANS
    ),
    CLIMATE_CONTROL(
            "Climate Balance",
            "Keep moisture 42-64% and temp 16-26 C",
            "Moisture 42-64% and Temp 16-26 C",
            TrainingDrill.BALANCE,
            45,
            new BalanceTarget(140, 480, 5, 44, 3, 8, 4, 8, 0, 1, 0.42, 0.64, 16.0, 26.0),
            BalanceBand.MOISTURE,
            BalanceBand.TEMPERATURE
    ),
    FLEX_SHIFT(
            "Full Harmony",
            "Keep every band in range",
            "All bands in range",
            TrainingDrill.BALANCE,
            55,
            new BalanceTarget(150, 440, 5, 40, 3, 7, 4, 7, 0, 1, 0.44, 0.62, 18.0, 25.0),
            BalanceBand.PLANTS,
            BalanceBand.RABBITS,
            BalanceBand.WOLVES,
            BalanceBand.HUMANS,
            BalanceBand.BEARS,
            BalanceBand.MOISTURE,
            BalanceBand.TEMPERATURE
    );

    private final String title;
    private final String objective;
    private final String challenge;
    private final TrainingDrill drill;
    private final int target;
    private final BalanceTarget balanceTarget;
    private final BalanceBand[] objectiveBands;

    TrainingLevel(
            String title,
            String objective,
            String challenge,
            TrainingDrill drill,
            int target,
            BalanceTarget balanceTarget,
            BalanceBand... objectiveBands
    ) {
        this.title = title;
        this.objective = objective;
        this.challenge = challenge;
        this.drill = drill;
        this.target = target;
        this.balanceTarget = balanceTarget;
        this.objectiveBands = objectiveBands.clone();
    }

    public String title() {
        return title;
    }

    public String objective() {
        return objective;
    }

    public String challenge() {
        return challenge;
    }

    public TrainingDrill drill() {
        return drill;
    }

    public int target() {
        return target;
    }

    public BalanceTarget balanceTarget() {
        return balanceTarget;
    }

    public BalanceBand[] objectiveBands() {
        return objectiveBands.clone();
    }

    public boolean objectiveMatches(PopulationSnapshot snapshot) {
        return balanceTarget.matches(snapshot, objectiveBands);
    }

    public String objectiveStatus(PopulationSnapshot snapshot) {
        return balanceTarget.status(snapshot, "Objective in range", objectiveBands);
    }

    public TrainingLevel next() {
        TrainingLevel[] levels = values();
        return levels[(ordinal() + 1) % levels.length];
    }
}
