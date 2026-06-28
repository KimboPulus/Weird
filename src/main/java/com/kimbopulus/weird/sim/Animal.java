package com.kimbopulus.weird.sim;

import java.util.List;

public abstract class Animal extends Organism {
    private final int moveCost;
    private final int maxEnergy;
    private final int foodEnergy;
    private final int reproductionEnergy;
    private final int reproductionCost;
    private final int sightRange;
    private final double reproductionChance;
    private final OrganismKind foodKind;

    protected Animal(
            int startingEnergy,
            int moveCost,
            int maxEnergy,
            int foodEnergy,
            int reproductionEnergy,
            int reproductionCost,
            int sightRange,
            double reproductionChance,
            OrganismKind foodKind
    ) {
        super(startingEnergy);
        this.moveCost = moveCost;
        this.maxEnergy = maxEnergy;
        this.foodEnergy = foodEnergy;
        this.reproductionEnergy = reproductionEnergy;
        this.reproductionCost = reproductionCost;
        this.sightRange = sightRange;
        this.reproductionChance = reproductionChance;
        this.foodKind = foodKind;
    }

    @Override
    protected void update(Simulation simulation, Position position) {
        spendEnergy(moveCost());
        if (energy() <= 0) {
            die();
            return;
        }

        Position current = position;
        Position food = first(simulation.neighborsWithKind(position, foodKind()));
        if (food != null) {
            simulation.removeOrganism(food);
            if (simulation.moveOrganism(position, food)) {
                current = food;
            }
            gainEnergy(foodEnergy());
            if (energy() > maxEnergy()) {
                setEnergy(maxEnergy());
            }
            onFoodConsumed(simulation, current, food);
            if (shouldDepartAfterFood(simulation, current, food)) {
                simulation.removeOrganismQuietly(current);
                return;
            }
        } else {
            Position target = first(simulation.visibleWithKind(position, foodKind(), sightRange()));
            Position next = target == null ? first(simulation.passableNeighbors(position, foodKind())) : stepToward(simulation, position, target);
            if (next != null && simulation.moveAnimal(position, next, foodKind())) {
                current = next;
            }
        }

        if (age() > 8 && energy() >= reproductionEnergy() && simulation.random().nextDouble() < reproductionChance()) {
            List<Position> emptyNeighbors = simulation.emptyNeighbors(current);
            if (!emptyNeighbors.isEmpty()) {
                Animal offspring = createOffspring();
                if (simulation.placeOrganism(emptyNeighbors.get(0), offspring)) {
                    simulation.recordBirth(offspring.kind(), current);
                    spendEnergy(reproductionCost());
                }
            }
        }
    }

    protected abstract Animal createOffspring();

    protected void onFoodConsumed(Simulation simulation, Position position, Position food) {
    }

    protected boolean shouldDepartAfterFood(Simulation simulation, Position position, Position food) {
        return false;
    }

    protected int moveCost() {
        return moveCost;
    }

    protected int maxEnergy() {
        return maxEnergy;
    }

    protected int foodEnergy() {
        return foodEnergy;
    }

    protected int reproductionEnergy() {
        return reproductionEnergy;
    }

    protected int reproductionCost() {
        return reproductionCost;
    }

    protected int sightRange() {
        return sightRange;
    }

    protected double reproductionChance() {
        return reproductionChance;
    }

    protected OrganismKind foodKind() {
        return foodKind;
    }

    protected Position first(List<Position> positions) {
        if (positions.isEmpty()) {
            return null;
        }
        return positions.get(0);
    }

    protected Position stepToward(Simulation simulation, Position position, Position target) {
        Position best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Position neighbor : simulation.passableNeighbors(position, foodKind())) {
            int distance = Math.abs(neighbor.x() - target.x()) + Math.abs(neighbor.y() - target.y());
            if (distance < bestDistance) {
                best = neighbor;
                bestDistance = distance;
            }
        }

        return best;
    }
}
