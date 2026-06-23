package com.kimbopulus.weird.sim;

public final class Rabbit extends Animal {
    public Rabbit() {
        super(22, 2, 42, 11, 34, 18, OrganismKind.PLANT);
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

