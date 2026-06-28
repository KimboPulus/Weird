package com.kimbopulus.weird.audio;

public enum SoundCue {
    WATER(440, 0.14, 0.10),
    DRY(165, 0.12, 0.11),
    GROW(523, 0.16, 0.10),
    PLACE(330, 0.10, 0.09),
    WARNING(220, 0.22, 0.12),
    COMPLETE(659, 0.30, 0.11),
    LEVEL_UP(784, 0.42, 0.13),
    FAILURE(110, 0.45, 0.13),
    ANIMAL_DEATH(145, 0.34, 0.11),
    HUMAN_DEATH(92, 0.75, 0.16),
    RESTART(392, 0.24, 0.10);

    private final double frequency;
    private final double duration;
    private final double volume;

    SoundCue(double frequency, double duration, double volume) {
        this.frequency = frequency;
        this.duration = duration;
        this.volume = volume;
    }

    public double frequency() {
        return frequency;
    }

    public double duration() {
        return duration;
    }

    public double volume() {
        return volume;
    }
}
