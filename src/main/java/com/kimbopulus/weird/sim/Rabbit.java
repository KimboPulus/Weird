package com.kimbopulus.weird.sim;

import java.util.List;

public final class Rabbit extends Animal {
    private final RabbitSex sex;
    private boolean bred;

    public Rabbit() {
        this(RabbitSex.random());
    }

    public Rabbit(RabbitSex sex) {
        super(18, 3, 36, 7, 32, 21, 1, 0.09, OrganismKind.PLANT);
        this.sex = sex;
    }

    public RabbitSex sex() {
        return sex;
    }

    public boolean male() {
        return sex == RabbitSex.MALE;
    }

    public boolean female() {
        return sex == RabbitSex.FEMALE;
    }

    @Override
    protected void update(Simulation simulation, Position position) {
        spendEnergy(moveCost());
        if (energy() <= 0) {
            die();
            return;
        }

        Position current = position;
        if (tryMate(simulation, position)) {
            spendEnergy(reproductionCost());
            return;
        }

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
        } else {
            Position target = first(simulation.visibleWithKind(position, foodKind(), sightRange()));
            Position next = target == null
                    ? first(simulation.passableNeighbors(position, foodKind()))
                    : stepToward(simulation, position, target);
            if (next != null && simulation.moveAnimal(position, next, foodKind())) {
                current = next;
            }
        }

        if (age() > 8 && energy() >= reproductionEnergy() && simulation.random().nextDouble() < reproductionChance()) {
            List<Position> emptyNeighbors = simulation.emptyNeighbors(current);
            if (!emptyNeighbors.isEmpty()) {
                simulation.placeOrganism(emptyNeighbors.get(0), createOffspring());
                spendEnergy(reproductionCost());
            }
        }
    }

    @Override
    public OrganismKind kind() {
        return OrganismKind.RABBIT;
    }

    @Override
    protected Animal createOffspring() {
        return new Rabbit(RabbitSex.FEMALE);
    }

    private boolean tryMate(Simulation simulation, Position position) {
        if (!male() || bred) {
            return false;
        }

        for (Position neighbor : simulation.visibleWithKind(position, OrganismKind.RABBIT, 8)) {
            if (!(simulation.organismAt(neighbor) instanceof Rabbit partner)) {
                continue;
            }
            if (!partner.female() || partner.bred) {
                continue;
            }

            partner.bred = true;
            bred = true;
            List<Position> spots = simulation.emptyNeighbors(position);
            int births = Math.min(3, spots.size());
            for (int i = 0; i < births; i++) {
                simulation.placeOrganism(spots.get(i), new Rabbit(RabbitSex.FEMALE));
            }
            if (births > 0) {
                simulation.recordBirth(OrganismKind.RABBIT, position);
            }
            return true;
        }
        return false;
    }
}
