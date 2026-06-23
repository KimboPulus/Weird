package com.kimbopulus.weird.sim;

import java.util.List;

public final class Plant extends Organism {
    private static final int STARTING_ENERGY = 6;
    private static final int MAX_ENERGY = 28;
    private static final int SEED_COST = 14;

    public Plant() {
        super(STARTING_ENERGY);
    }

    @Override
    protected void update(Simulation simulation, Position position) {
        Cell cell = simulation.grid().cellAt(position);
        double growth = cell.plantGrowthFactor();

        gainEnergy(1 + (int) Math.round(growth * 4.0));
        if (energy() > MAX_ENERGY) {
            setEnergy(MAX_ENERGY);
        }

        cell.spendFertility(0.004 + growth * 0.006);
        if (growth < 0.2 && simulation.random().nextDouble() < 0.05) {
            die();
            return;
        }

        if (energy() >= SEED_COST && simulation.random().nextDouble() < growth * 0.28) {
            List<Position> emptyNeighbors = simulation.emptyNeighbors(position);
            if (!emptyNeighbors.isEmpty()) {
                simulation.placeOrganism(emptyNeighbors.get(0), new Plant());
                spendEnergy(SEED_COST / 2);
            }
        }
    }

    @Override
    public OrganismKind kind() {
        return OrganismKind.PLANT;
    }
}

