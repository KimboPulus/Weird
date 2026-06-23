package com.kimbopulus.weird.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class Simulation {
    public static final int DEFAULT_WIDTH = 48;
    public static final int DEFAULT_HEIGHT = 32;

    private final Random random;
    private final WorldGrid grid;
    private final Organism[][] organisms;
    private int tick;
    private Season season = Season.SPRING;

    public Simulation(int width, int height, long seed) {
        this.random = new Random(seed);
        this.grid = new WorldGrid(width, height, random);
        this.organisms = new Organism[height][width];
    }

    public static Simulation createDefault() {
        Simulation simulation = new Simulation(DEFAULT_WIDTH, DEFAULT_HEIGHT, System.nanoTime());
        simulation.seedPlants(220);
        return simulation;
    }

    public void tick() {
        tick++;
        if (tick % 160 == 0) {
            season = season.next();
        }

        grid.applySeason(season);

        List<Position> positions = occupiedPositions();
        Collections.shuffle(positions, random);
        for (Position position : positions) {
            Organism organism = organismAt(position);
            if (organism == null || !organism.alive()) {
                continue;
            }

            organism.tick(this, position);
            if (!organism.alive() && organismAt(position) == organism) {
                removeOrganism(position);
                grid.cellAt(position).addFertility(0.08);
            }
        }
    }

    public WorldGrid grid() {
        return grid;
    }

    public Random random() {
        return random;
    }

    public int tickCount() {
        return tick;
    }

    public Season season() {
        return season;
    }

    public Organism organismAt(Position position) {
        if (!grid.contains(position)) {
            return null;
        }
        return organisms[position.y()][position.x()];
    }

    public boolean isEmpty(Position position) {
        return grid.contains(position) && organismAt(position) == null;
    }

    public boolean placeOrganism(Position position, Organism organism) {
        if (!isEmpty(position)) {
            return false;
        }
        organisms[position.y()][position.x()] = organism;
        return true;
    }

    public Organism removeOrganism(Position position) {
        if (!grid.contains(position)) {
            return null;
        }
        Organism organism = organismAt(position);
        organisms[position.y()][position.x()] = null;
        return organism;
    }

    public List<Position> emptyNeighbors(Position position) {
        List<Position> empty = new ArrayList<>();
        for (Position neighbor : grid.neighbors(position, random)) {
            if (isEmpty(neighbor)) {
                empty.add(neighbor);
            }
        }
        return empty;
    }

    public int count(OrganismKind kind) {
        int total = 0;
        for (Position position : occupiedPositions()) {
            Organism organism = organismAt(position);
            if (organism != null && organism.kind() == kind) {
                total++;
            }
        }
        return total;
    }

    public void seedPlants(int amount) {
        for (int i = 0; i < amount; i++) {
            placeRandomly(new Plant(), 150);
        }
    }

    private void placeRandomly(Organism organism, int attempts) {
        for (int attempt = 0; attempt < attempts; attempt++) {
            Position position = new Position(random.nextInt(grid.width()), random.nextInt(grid.height()));
            if (placeOrganism(position, organism)) {
                return;
            }
        }
    }

    private List<Position> occupiedPositions() {
        List<Position> positions = new ArrayList<>();
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (organisms[y][x] != null) {
                    positions.add(new Position(x, y));
                }
            }
        }
        return positions;
    }
}
