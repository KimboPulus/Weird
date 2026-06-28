package com.kimbopulus.weird.training;

public enum TrainingLevel {
    STEADY_START("Steady Start", "Hold broad balance", TrainingDrill.BALANCE, 20),
    MEMORY_SCAN("Green Rhythm", "Balance plants and rabbits", TrainingDrill.BALANCE, 30),
    PREDATOR_CHECK("Predator Line", "Balance wolves with prey", TrainingDrill.BALANCE, 35),
    CANOPY_CONTROL("Human Footprint", "Balance people and plant growth", TrainingDrill.BALANCE, 40),
    CLIMATE_CONTROL("Climate Balance", "Balance climate and residents", TrainingDrill.BALANCE, 45),
    FLEX_SHIFT("Full Harmony", "Hold strict balance", TrainingDrill.BALANCE, 55);

    private final String title;
    private final String objective;
    private final TrainingDrill drill;
    private final int target;

    TrainingLevel(String title, String objective, TrainingDrill drill, int target) {
        this.title = title;
        this.objective = objective;
        this.drill = drill;
        this.target = target;
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

    public TrainingLevel next() {
        TrainingLevel[] levels = values();
        return levels[(ordinal() + 1) % levels.length];
    }
}
