package com.kimbopulus.weird.sim;

public enum WorldEvent {
    CALM("Calm weather", "No unusual conditions."),
    RAIN_FRONT("Rain front", "Moisture rises across the terrarium."),
    HEAT_WAVE("Heat wave", "The board dries and warms for a while."),
    WILD_BLOOM("Wild bloom", "Fresh plants appear in open soil."),
    RABBIT_ARRIVAL("Rabbit arrival", "A small group of grazers enters."),
    WOLF_ARRIVAL("Wolf arrival", "New predators enter the food chain.");

    private final String title;
    private final String description;

    WorldEvent(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }
}
