package com.kimbopulus.weird.training;

public enum TrainingDrill {
    BALANCE("Balance Run", "Keep all three species active for 35 ticks."),
    RECALL("Recall Check", "Answer the next memory prompt correctly."),
    PREDATORS("Predator Watch", "Keep at least 3 wolves alive for 30 ticks."),
    OVERGROWTH("Canopy Control", "Keep plants below 900 for 30 ticks.");

    private final String title;
    private final String goal;

    TrainingDrill(String title, String goal) {
        this.title = title;
        this.goal = goal;
    }

    public String title() {
        return title;
    }

    public String goal() {
        return goal;
    }

    public TrainingDrill next() {
        TrainingDrill[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
