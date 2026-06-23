package com.kimbopulus.weird.sim;

public final class Rabbit extends Animal {
    public Rabbit() {
        super(16, 4, 34, 7, 31, 20, 1, 0.1, OrganismKind.PLANT);
    }

    @Override
    public OrganismKind kind() {
        return OrganismKind.RABBIT;
    }

    @Override
    protected Animal createOffspring() {
        return new Rabbit();
    }
}
