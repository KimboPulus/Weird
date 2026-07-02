package com.kimbopulus.weird.sim;

import java.util.List;

public final class Human extends Organism {
    private boolean bred;

    public Human() {
        super(100);
    }

    @Override
    protected void update(Simulation simulation, Position position) {
        Position current = position;
        if (tryMate(simulation, current)) {
            return;
        }

        Position wolf = first(simulation.neighborsWithKind(position, OrganismKind.WOLF));
        if (wolf != null) {
            simulation.removeOrganism(wolf, DeathCause.HUMAN_ATTACK);
            spendEnergy(4);
            return;
        }

        if (simulation.random().nextDouble() < 0.72) {
            List<Position> passable = simulation.passableNeighbors(position, OrganismKind.RABBIT);
            if (!passable.isEmpty() && simulation.moveAnimal(position, passable.get(0), OrganismKind.RABBIT)) {
                current = passable.get(0);
            }
        }

        tryMate(simulation, current);

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

    private boolean tryMate(Simulation simulation, Position position) {
        if (bred) {
            return false;
        }

        for (Position neighbor : simulation.neighborsWithKind(position, OrganismKind.HUMAN)) {
            if (!(simulation.organismAt(neighbor) instanceof Human partner) || partner.bred) {
                continue;
            }

            List<Position> spots = simulation.emptyNeighbors(position);
            if (spots.isEmpty()) {
                return false;
            }

            bred = true;
            partner.bred = true;
            Human offspring = new Human();
            if (simulation.placeOrganism(spots.get(0), offspring)) {
                simulation.recordBirth(OrganismKind.HUMAN, position);
                return true;
            }
            bred = false;
            partner.bred = false;
        }
        return false;
    }

    private Position first(List<Position> positions) {
        return positions.isEmpty() ? null : positions.get(0);
    }
}
