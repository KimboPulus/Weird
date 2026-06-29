package com.kimbopulus.weird.training;

public enum TrainingDrill {
    BALANCE("Balance Run", "Keep the board inside the target bands."),
    PREDATORS("Predator Watch", "Keep wolves present without letting them take over."),
    OVERGROWTH("Canopy Control", "Keep plants under control for 30 ticks."),
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
