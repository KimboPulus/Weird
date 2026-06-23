package com.kimbopulus.weird.training;

public enum TrainingLevel {
    STEADY_START("Steady Start", "Hold balance", TrainingDrill.BALANCE, 20),
    MEMORY_SCAN("Memory Scan", "Answer 2 recalls", TrainingDrill.RECALL, 2),
    PREDATOR_CHECK("Predator Check", "Keep 3 wolves", TrainingDrill.PREDATORS, 25),
    CANOPY_CONTROL("Canopy Control", "Keep plants below 900", TrainingDrill.OVERGROWTH, 25),
    CLIMATE_CONTROL("Climate Control", "Keep climate safe", TrainingDrill.CLIMATE_ALERT, 25),
    FLEX_SHIFT("Flex Shift", "Hold balance under changing recall rules", TrainingDrill.BALANCE, 40);

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
