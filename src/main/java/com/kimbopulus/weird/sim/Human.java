package com.kimbopulus.weird.sim;

import java.util.List;

public final class Human extends Organism {
    public Human() {
        super(100);
    }

    @Override
    protected void update(Simulation simulation, Position position) {
        if (age() % 2 != 0) {
            return;
        }

        Position current = position;
        if (simulation.random().nextDouble() < 0.35) {
            List<Position> empty = simulation.emptyNeighbors(position);
            if (!empty.isEmpty() && simulation.moveOrganism(position, empty.get(0))) {
                current = empty.get(0);
            }
        }

        if (simulation.canPlantsSpread() && simulation.random().nextDouble() < 0.06) {
            List<Position> empty = simulation.emptyNeighbors(current);
            if (!empty.isEmpty()) {
                simulation.placeOrganism(empty.get(0), new Plant());
            }
        }
    }

    @Override
    public OrganismKind kind() {
        return OrganismKind.HUMAN;
    }
}
