package com.kimbopulus.weird.sim;

public final class Wolf extends Animal {
    private int rabbitsEaten;

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

    @Override
    protected void onFoodConsumed(Simulation simulation, Position position, Position food) {
        rabbitsEaten++;
    }

    @Override
    protected boolean shouldDepartAfterFood(Simulation simulation, Position position, Position food) {
        return rabbitsEaten >= 3;
    }
}
