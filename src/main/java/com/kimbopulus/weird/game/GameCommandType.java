package com.kimbopulus.weird.game;

import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.RabbitSex;
import com.kimbopulus.weird.sim.Simulation;

public enum GameCommandType {
    RAIN,
    DROUGHT,
    COMPOST,
    HUMAN,
    BEAR,
    RABBIT,
    WOLF,
    LIGHTNING,
    SANCTUARY;

    public boolean applyTo(Simulation simulation, Position position) {
        return switch (this) {
            case RAIN -> simulation.rain(position);
            case DROUGHT -> simulation.drought(position);
            case COMPOST -> simulation.compost(position);
            case HUMAN -> simulation.addHuman(position);
            case BEAR -> simulation.addBear(position);
            case RABBIT -> simulation.addRabbit(position, RabbitSex.MALE);
            case WOLF -> simulation.addWolf(position);
            case LIGHTNING -> simulation.lightning(position);
            case SANCTUARY -> simulation.addSanctuary(position);
        };
    }
}
