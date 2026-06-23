package com.kimbopulus.weird.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
        simulation.seedRabbits(34);
        simulation.seedWolves(5);
        return simulation;
    }

    public void tick() {
        tick++;
        if (tick % 160 == 0) {
            season = season.next();
        }

        grid.applySeason(season);
        sproutWildPlants();

        List<Position> positions = occupiedPositions();
        Collections.shuffle(positions, random);
        Set<Organism> acted = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Position position : positions) {
            Organism organism = organismAt(position);
            if (organism == null || !organism.alive() || !acted.add(organism)) {
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

    public boolean moveOrganism(Position from, Position to) {
        if (!grid.contains(from) || !grid.contains(to) || !isEmpty(to)) {
            return false;
        }

        Organism organism = organismAt(from);
        if (organism == null) {
            return false;
        }

        organisms[to.y()][to.x()] = organism;
        organisms[from.y()][from.x()] = null;
        return true;
    }

    public boolean moveAnimal(Position from, Position to, OrganismKind foodKind) {
        Organism target = organismAt(to);
        if (target != null && foodKind != OrganismKind.PLANT && target.kind() == OrganismKind.PLANT) {
            removeOrganism(to);
        }
        return moveOrganism(from, to);
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

    public List<Position> passableNeighbors(Position position, OrganismKind foodKind) {
        List<Position> passable = new ArrayList<>();
        for (Position neighbor : grid.neighbors(position, random)) {
            Organism organism = organismAt(neighbor);
            if (organism == null || (foodKind != OrganismKind.PLANT && organism.kind() == OrganismKind.PLANT)) {
                passable.add(neighbor);
            }
        }
        return passable;
    }

    public List<Position> neighborsWithKind(Position position, OrganismKind kind) {
        List<Position> matches = new ArrayList<>();
        for (Position neighbor : grid.neighbors(position, random)) {
            Organism organism = organismAt(neighbor);
            if (organism != null && organism.kind() == kind) {
                matches.add(neighbor);
            }
        }
        return matches;
    }

    public List<Position> visibleWithKind(Position position, OrganismKind kind, int range) {
        List<Position> matches = new ArrayList<>();
        for (int y = Math.max(0, position.y() - range); y <= Math.min(grid.height() - 1, position.y() + range); y++) {
            for (int x = Math.max(0, position.x() - range); x <= Math.min(grid.width() - 1, position.x() + range); x++) {
                Position candidate = new Position(x, y);
                int distance = Math.abs(position.x() - candidate.x()) + Math.abs(position.y() - candidate.y());
                if (distance == 0 || distance > range) {
                    continue;
                }

                Organism organism = organismAt(candidate);
                if (organism != null && organism.kind() == kind) {
                    matches.add(candidate);
                }
            }
        }
        Collections.shuffle(matches, random);
        return matches;
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

    public void seedRabbits(int amount) {
        for (int i = 0; i < amount; i++) {
            placeRandomly(new Rabbit(), 150);
        }
    }

    public void seedWolves(int amount) {
        for (int i = 0; i < amount; i++) {
            placeRandomly(new Wolf(), 150);
        }
    }

    public void rain(Position center) {
        if (grid.contains(center)) {
            grid.rainAround(center, 2, 0.18);
        }
    }

    public void drought(Position center) {
        if (grid.contains(center)) {
            grid.dryAround(center, 2, 0.16);
        }
    }

    public boolean addPlant(Position position) {
        return placeOrganism(position, new Plant());
    }

    public boolean addRabbit(Position position) {
        return placeOrganism(position, new Rabbit());
    }

    public boolean addWolf(Position position) {
        return placeOrganism(position, new Wolf());
    }

    private void placeRandomly(Organism organism, int attempts) {
        for (int attempt = 0; attempt < attempts; attempt++) {
            Position position = new Position(random.nextInt(grid.width()), random.nextInt(grid.height()));
            if (placeOrganism(position, organism)) {
                return;
            }
        }
    }

    private void sproutWildPlants() {
        int plantLimit = (grid.width() * grid.height()) / 2;
        if (count(OrganismKind.PLANT) >= plantLimit) {
            return;
        }

        int attempts = Math.max(4, (grid.width() * grid.height()) / 80);
        for (int i = 0; i < attempts; i++) {
            Position position = new Position(random.nextInt(grid.width()), random.nextInt(grid.height()));
            if (!isEmpty(position)) {
                continue;
            }

            Cell cell = grid.cellAt(position);
            if (random.nextDouble() < cell.plantGrowthFactor() * 0.08) {
                placeOrganism(position, new Plant());
                cell.spendFertility(0.02);
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
