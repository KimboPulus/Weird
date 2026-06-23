package com.kimbopulus.weird.sim;

public final class Wolf extends Animal {
    public Wolf() {
        super(52, 3, 78, 28, 68, 42, 5, 0.08, OrganismKind.RABBIT);
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
