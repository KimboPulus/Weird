package com.kimbopulus.weird.sim;

public final class Wolf extends Animal {
    public Wolf() {
        super(45, 5, 78, 28, 68, 36, OrganismKind.RABBIT);
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

