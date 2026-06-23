package com.kimbopulus.weird.sim;

public final class Rabbit extends Animal {
    public Rabbit() {
        super(18, 3, 36, 7, 32, 21, 1, 0.09, OrganismKind.PLANT);
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
