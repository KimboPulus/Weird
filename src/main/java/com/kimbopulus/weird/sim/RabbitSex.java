package com.kimbopulus.weird.sim;

public enum RabbitSex {
    MALE,
    FEMALE;

    public static RabbitSex random() {
        return Math.random() < 0.5 ? MALE : FEMALE;
    }
}
