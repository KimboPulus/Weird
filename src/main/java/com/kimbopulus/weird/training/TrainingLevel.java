package com.kimbopulus.weird.training;

public enum TrainingLevel {
    STEADY_START(
            "Steady Start",
            "Hold the board in range",
            TrainingDrill.BALANCE,
            20,
            new BalanceTarget(90, 700, 6, 70, 2, 10, 3, 10, 0, 2, 0.25, 0.78, 10.0, 34.0)
    ),
    MEMORY_SCAN(
            "Green Rhythm",
            "Keep plants and rabbits steady",
            TrainingDrill.BALANCE,
            30,
            new BalanceTarget(110, 620, 6, 60, 2, 9, 3, 9, 0, 2, 0.30, 0.72, 12.0, 31.0)
    ),
    PREDATOR_CHECK(
            "Predator Line",
            "Keep wolves in the chain",
            TrainingDrill.BALANCE,
            35,
            new BalanceTarget(120, 560, 5, 54, 3, 9, 3, 9, 0, 2, 0.34, 0.70, 13.0, 30.0)
    ),
    CANOPY_CONTROL(
            "Human Footprint",
            "Keep humans useful, not dominant",
            TrainingDrill.BALANCE,
            40,
            new BalanceTarget(130, 520, 5, 48, 3, 8, 3, 8, 0, 2, 0.38, 0.66, 14.0, 28.0)
    ),
    CLIMATE_CONTROL(
            "Climate Balance",
            "Hold moisture and temperature steady",
            TrainingDrill.BALANCE,
            45,
            new BalanceTarget(140, 480, 5, 44, 3, 8, 4, 8, 0, 1, 0.42, 0.64, 16.0, 26.0)
    ),
    FLEX_SHIFT(
            "Full Harmony",
            "Hold every band inside the line",
            TrainingDrill.BALANCE,
            55,
            new BalanceTarget(150, 440, 5, 40, 3, 7, 4, 7, 0, 1, 0.44, 0.62, 18.0, 25.0)
    );

    private final String title;
    private final String objective;
    private final TrainingDrill drill;
    private final int target;
    private final BalanceTarget balanceTarget;

    TrainingLevel(String title, String objective, TrainingDrill drill, int target, BalanceTarget balanceTarget) {
        this.title = title;
        this.objective = objective;
        this.drill = drill;
        this.target = target;
        this.balanceTarget = balanceTarget;
    }

    public String title() {
        return title;
    }

    public String objective() {
        return objective;
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

    public TrainingLevel next() {
        TrainingLevel[] levels = values();
        return levels[(ordinal() + 1) % levels.length];
    }
}
