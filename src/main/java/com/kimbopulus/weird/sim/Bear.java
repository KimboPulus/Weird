package com.kimbopulus.weird.sim;

import java.util.List;

public final class Bear extends Organism {
    private int humansEaten;

    public Bear() {
        super(140);
    }

    @Override
    protected void update(Simulation simulation, Position position) {
        if (humansEaten >= 2) {
            simulation.removeOrganismQuietly(position);
            return;
        }
        if (age() > 70 && simulation.random().nextDouble() < 0.08) {
            simulation.removeOrganismQuietly(position);
            return;
        }
        if (simulation.count(OrganismKind.HUMAN) == 0 && simulation.random().nextDouble() < 0.18) {
            simulation.removeOrganismQuietly(position);
            return;
        }

        Position human = first(simulation.neighborsWithKind(position, OrganismKind.HUMAN));
        if (human != null) {
            simulation.removeOrganism(human);
            humansEaten++;
            if (humansEaten >= 2) {
                simulation.removeOrganismQuietly(position);
                return;
            }
            simulation.moveOrganism(position, human);
            return;
        }

        Position target = first(simulation.visibleWithKind(position, OrganismKind.HUMAN, 10));
        Position next = target == null
                ? first(simulation.passableNeighbors(position, OrganismKind.HUMAN))
                : stepToward(simulation, position, target);
        if (next != null) {
            simulation.moveAnimal(position, next, OrganismKind.HUMAN);
        }
    }

    @Override
    public OrganismKind kind() {
        return OrganismKind.BEAR;
    }

    private Position stepToward(Simulation simulation, Position position, Position target) {
        Position best = null;
        int distance = Integer.MAX_VALUE;
        for (Position neighbor : simulation.passableNeighbors(position, OrganismKind.HUMAN)) {
            int candidate = Math.abs(neighbor.x() - target.x()) + Math.abs(neighbor.y() - target.y());
            if (candidate < distance) {
                best = neighbor;
                distance = candidate;
            }
        }
        return best;
    }

    private Position first(List<Position> positions) {
        return positions.isEmpty() ? null : positions.get(0);
    }
}
