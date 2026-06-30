package com.kimbopulus.weird.sim;

import java.util.List;

public final class Human extends Organism {
    public Human() {
        super(100);
    }

    @Override
    protected void update(Simulation simulation, Position position) {
        Position wolf = first(simulation.neighborsWithKind(position, OrganismKind.WOLF));
        if (wolf != null) {
            simulation.removeOrganism(wolf, DeathCause.HUMAN_ATTACK);
            spendEnergy(4);
            return;
        }

        Position current = position;
        if (simulation.random().nextDouble() < 0.72) {
            List<Position> passable = simulation.passableNeighbors(position, OrganismKind.RABBIT);
            if (!passable.isEmpty() && simulation.moveAnimal(position, passable.get(0), OrganismKind.RABBIT)) {
                current = passable.get(0);
            }
        }

        if (simulation.canPlantsSpread() && simulation.random().nextDouble() < 0.08) {
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

    private Position first(List<Position> positions) {
        return positions.isEmpty() ? null : positions.get(0);
    }
}
