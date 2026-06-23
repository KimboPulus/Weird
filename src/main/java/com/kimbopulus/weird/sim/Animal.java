package com.kimbopulus.weird.sim;

import java.util.List;

public abstract class Animal extends Organism {
    private final int moveCost;
    private final int maxEnergy;
    private final int foodEnergy;
    private final int reproductionEnergy;
    private final int reproductionCost;
    private final OrganismKind foodKind;

    protected Animal(
            int startingEnergy,
            int moveCost,
            int maxEnergy,
            int foodEnergy,
            int reproductionEnergy,
            int reproductionCost,
            OrganismKind foodKind
    ) {
        super(startingEnergy);
        this.moveCost = moveCost;
        this.maxEnergy = maxEnergy;
        this.foodEnergy = foodEnergy;
        this.reproductionEnergy = reproductionEnergy;
        this.reproductionCost = reproductionCost;
        this.foodKind = foodKind;
    }

    @Override
    protected void update(Simulation simulation, Position position) {
        spendEnergy(moveCost);
        if (energy() <= 0) {
            die();
            return;
        }

        Position current = position;
        Position food = first(simulation.neighborsWithKind(position, foodKind));
        if (food != null) {
            simulation.removeOrganism(food);
            if (simulation.moveOrganism(position, food)) {
                current = food;
            }
            gainEnergy(foodEnergy);
            if (energy() > maxEnergy) {
                setEnergy(maxEnergy);
            }
        } else {
            Position empty = first(simulation.emptyNeighbors(position));
            if (empty != null && simulation.moveOrganism(position, empty)) {
                current = empty;
            }
        }

        if (energy() >= reproductionEnergy) {
            List<Position> emptyNeighbors = simulation.emptyNeighbors(current);
            if (!emptyNeighbors.isEmpty()) {
                simulation.placeOrganism(emptyNeighbors.get(0), createOffspring());
                spendEnergy(reproductionCost);
            }
        }
    }

    protected abstract Animal createOffspring();

    private Position first(List<Position> positions) {
        if (positions.isEmpty()) {
            return null;
        }
        return positions.get(0);
    }
}

