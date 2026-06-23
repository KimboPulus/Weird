package com.kimbopulus.weird.sim;

public enum Season {
    SPRING(0.08, 0.04),
    SUMMER(-0.02, 0.08),
    AUTUMN(0.03, -0.02),
    WINTER(-0.05, -0.1);

    private final double moistureShift;
    private final double temperatureShift;

    Season(double moistureShift, double temperatureShift) {
        this.moistureShift = moistureShift;
        this.temperatureShift = temperatureShift;
    }

    public double moistureShift() {
        return moistureShift;
    }

    public double temperatureShift() {
        return temperatureShift;
    }

    public Season next() {
        Season[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}

