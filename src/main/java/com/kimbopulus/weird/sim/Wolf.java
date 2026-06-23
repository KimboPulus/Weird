package com.kimbopulus.weird.sim;

public final class Wolf extends Animal {
    public Wolf() {
        super(84, 2, 112, 34, 98, 56, 8, 0.04, OrganismKind.RABBIT);
    }

    @Override
    public OrganismKind kind() {
        return OrganismKind.WOLF;
    }

    @Override
    protected Animal createOffspring() {
        return new Wolf();
    }
}
