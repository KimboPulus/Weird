package com.kimbopulus.weird.sim;

public final class Cell {
    private double moisture;
    private double temperature;
    private double fertility;

    public Cell(double moisture, double temperature, double fertility) {
        reset(moisture, temperature, fertility);
    }

    public double moisture() {
        return moisture;
    }

    public double temperature() {
        return temperature;
    }

    public double fertility() {
        return fertility;
    }

    public void addRain(double amount) {
        moisture = clamp01(moisture + amount);
    }

    public void dry(double amount) {
        moisture = clamp01(moisture - amount);
    }

    public void warm(double amount) {
        temperature = clamp(temperature + amount, -20.0, 45.0);
    }

    public void cool(double amount) {
        temperature = clamp(temperature - amount, -20.0, 45.0);
    }

    public void addFertility(double amount) {
        fertility = clamp01(fertility + amount);
    }

    public void spendFertility(double amount) {
        fertility = clamp01(fertility - amount);
    }

    public double plantGrowthFactor() {
        double temperatureFit = 1.0 - Math.min(1.0, Math.abs(temperature - 21.0) / 24.0);
        return clamp01(moisture * 0.55 + fertility * 0.3 + temperatureFit * 0.15);
    }

    public void driftToward(Cell other, double amount) {
        moisture += (other.moisture - moisture) * amount;
        temperature += (other.temperature - temperature) * amount;
        fertility += (other.fertility - fertility) * amount;
        moisture = clamp01(moisture);
        temperature = clamp(temperature, -20.0, 45.0);
        fertility = clamp01(fertility);
    }

    public void reset(double moisture, double temperature, double fertility) {
        this.moisture = clamp01(moisture);
        this.temperature = clamp(temperature, -20.0, 45.0);
        this.fertility = clamp01(fertility);
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
