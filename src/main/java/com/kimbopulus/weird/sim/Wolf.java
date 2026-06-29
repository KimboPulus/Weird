package com.kimbopulus.weird.sim;

public final class Wolf extends Animal {
    private int rabbitsEaten;
    private boolean bred;

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
    protected void update(Simulation simulation, Position position) {
        if (tryMate(simulation, position)) {
            spendEnergy(reproductionCost());
            return;
        }
        super.update(simulation, position);
    }

    @Override
    protected void onFoodConsumed(Simulation simulation, Position position, Position food) {
        rabbitsEaten++;
    }

    @Override
    protected boolean shouldDepartAfterFood(Simulation simulation, Position position, Position food) {
        return rabbitsEaten >= 3;
    }

    private boolean tryMate(Simulation simulation, Position position) {
        if (bred) {
            return false;
        }

        for (Position neighbor : simulation.neighborsWithKind(position, OrganismKind.WOLF)) {
            if (!(simulation.organismAt(neighbor) instanceof Wolf partner) || partner.bred) {
                continue;
            }

            var spots = simulation.emptyNeighbors(position);
            if (spots.isEmpty()) {
                return false;
            }

            bred = true;
            partner.bred = true;
            Wolf offspring = new Wolf();
            if (simulation.placeOrganism(spots.get(0), offspring)) {
                simulation.recordBirth(OrganismKind.WOLF, position);
                return true;
            }
            bred = false;
            partner.bred = false;
        }
        return false;
    }
}
