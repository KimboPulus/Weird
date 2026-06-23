package com.kimbopulus.weird.training;

public enum TrainingDrill {
    BALANCE("Balance Run", "Keep all three species active for 35 ticks."),
    RECALL("Recall Check", "Answer the next recall prompt."),
    PREDATORS("Predator Watch", "Keep at least 3 wolves alive for 30 ticks."),
    OVERGROWTH("Canopy Control", "Keep plants below 900 for 30 ticks."),
    CLIMATE_ALERT("Weather Shift", "Restore safe moisture and temperature for 20 ticks.");

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

    public boolean urgent() {
        return this == CLIMATE_ALERT;
    }
}
