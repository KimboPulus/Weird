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
    private final List<PopulationSnapshot> history = new ArrayList<>();
    private int tick;
    private Season season = Season.SPRING;
    private WorldEvent currentEvent = WorldEvent.CALM;
    private boolean sanctuaryPlaced;
    private int plantCount;
    private int rabbitCount;
    private int wolfCount;

    public Simulation(int width, int height, long seed) {
        this.random = new Random(seed);
        this.grid = new WorldGrid(width, height, random);
        this.organisms = new Organism[height][width];
    }

    public static Simulation createDefault() {
        Simulation simulation = new Simulation(DEFAULT_WIDTH, DEFAULT_HEIGHT, System.nanoTime());
        simulation.seedPlants(220);
        simulation.seedRabbits(48);
        simulation.seedWolves(4);
        simulation.recordSnapshot();
        return simulation;
    }

    public void tick() {
        tick++;
        if (tick % 160 == 0) {
            season = season.next();
        }
        if (tick % 90 == 0) {
            triggerWorldEvent();
        }

        grid.applySeason(season);
        sproutWildPlants();
        migrateWildlife();

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
        recordSnapshot();
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

    public WorldEvent currentEvent() {
        return currentEvent;
    }

    public void restart() {
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                organisms[y][x] = null;
            }
        }
        plantCount = 0;
        rabbitCount = 0;
        wolfCount = 0;
        grid.reset(random);
        history.clear();
        tick = 0;
        season = Season.SPRING;
        currentEvent = WorldEvent.CALM;
        sanctuaryPlaced = false;
        seedPlants(220);
        seedRabbits(48);
        seedWolves(4);
        recordSnapshot();
    }

    public PopulationSnapshot currentSnapshot() {
        if (history.isEmpty()) {
            recordSnapshot();
        }
        return history.get(history.size() - 1);
    }

    public List<PopulationSnapshot> recentHistory(int limit) {
        int from = Math.max(0, history.size() - limit);
        return List.copyOf(history.subList(from, history.size()));
    }

    public Organism organismAt(Position position) {
        if (!grid.contains(position)) {
            return null;
        }
        return organisms[position.y()][position.x()];
    }

    public Organism organismAt(int x, int y) {
        return organisms[y][x];
    }

    public boolean isEmpty(Position position) {
        return grid.contains(position) && organismAt(position) == null;
    }

    public boolean placeOrganism(Position position, Organism organism) {
        if (!isEmpty(position)) {
            return false;
        }
        organisms[position.y()][position.x()] = organism;
        adjustCount(organism.kind(), 1);
        return true;
    }

    public Organism removeOrganism(Position position) {
        if (!grid.contains(position)) {
            return null;
        }
        Organism organism = organismAt(position);
        organisms[position.y()][position.x()] = null;
        if (organism != null) {
            adjustCount(organism.kind(), -1);
        }
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
        return switch (kind) {
            case PLANT -> plantCount;
            case RABBIT -> rabbitCount;
            case WOLF -> wolfCount;
        };
    }

    public NotableAnimal oldestAnimal() {
        NotableAnimal oldest = null;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                Organism organism = organisms[y][x];
                if (organism == null || organism.kind() == OrganismKind.PLANT) {
                    continue;
                }
                if (oldest == null || organism.age() > oldest.age()) {
                    oldest = new NotableAnimal(
                            organism.kind(),
                            organism.age(),
                            organism.energy(),
                            new Position(x, y)
                    );
                }
            }
        }
        return oldest;
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
            grid.rainAround(center, 2, 0.28);
        }
    }

    public void rainBoost(Position center) {
        if (grid.contains(center)) {
            grid.rainAround(center, 2, 0.14);
        }
    }

    public void drought(Position center) {
        if (grid.contains(center)) {
            grid.dryAround(center, 2, 0.28);
        }
    }

    public void compost(Position center) {
        if (grid.contains(center)) {
            grid.fertilizeAround(center, 1, 0.30);
            grid.rainAround(center, 1, 0.08);
        }
    }

    public void compostBoost(Position center) {
        if (grid.contains(center)) {
            grid.fertilizeAround(center, 1, 0.15);
        }
    }

    public boolean addSanctuary(Position position) {
        if (sanctuaryPlaced || !grid.contains(position)) {
            return false;
        }
        grid.createSanctuary(position);
        sanctuaryPlaced = true;
        return true;
    }

    public boolean sanctuaryPlaced() {
        return sanctuaryPlaced;
    }

    public int clearPatch(Position center) {
        if (!grid.contains(center)) {
            return 0;
        }

        int removed = 0;
        for (int y = Math.max(0, center.y() - 1); y <= Math.min(grid.height() - 1, center.y() + 1); y++) {
            for (int x = Math.max(0, center.x() - 1); x <= Math.min(grid.width() - 1, center.x() + 1); x++) {
                Position position = new Position(x, y);
                Organism organism = organismAt(position);
                if (organism != null && organism.kind() == OrganismKind.PLANT) {
                    removeOrganism(position);
                    grid.cellAt(position).addFertility(0.03);
                    removed++;
                }
            }
        }
        return removed;
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

    public boolean canPlantsSpread() {
        return count(OrganismKind.PLANT) < plantLimit();
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
        if (!canPlantsSpread()) {
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

    private void migrateWildlife() {
        int cells = grid.width() * grid.height();
        int plants = count(OrganismKind.PLANT);
        int rabbits = count(OrganismKind.RABBIT);
        int wolves = count(OrganismKind.WOLF);

        int rabbitFloor = Math.max(6, cells / 96);
        if (plants > cells / 5 && rabbits < rabbitFloor && random.nextDouble() < 0.2) {
            placeRandomly(new Rabbit(), 60);
        }

        if (rabbits > cells / 26 && wolves < 2 && random.nextDouble() < 0.04) {
            placeRandomly(new Wolf(), 60);
        }
    }

    private void triggerWorldEvent() {
        WorldEvent[] events = {
                WorldEvent.RAIN_FRONT,
                WorldEvent.HEAT_WAVE,
                WorldEvent.WILD_BLOOM,
                WorldEvent.RABBIT_ARRIVAL,
                WorldEvent.WOLF_ARRIVAL
        };
        currentEvent = events[random.nextInt(events.length)];

        switch (currentEvent) {
            case RAIN_FRONT -> grid.rainAll(0.12);
            case HEAT_WAVE -> grid.dryAndWarmAll(0.1, 2.5);
            case WILD_BLOOM -> seedPlants(Math.max(18, grid.width() * grid.height() / 45));
            case RABBIT_ARRIVAL -> seedRabbits(Math.max(3, grid.width() * grid.height() / 380));
            case WOLF_ARRIVAL -> seedWolves(Math.max(1, grid.width() * grid.height() / 900));
            case CALM -> {
            }
        }
    }

    private int plantLimit() {
        return (int) (grid.width() * grid.height() * 0.72);
    }

    private void recordSnapshot() {
        double moisture = 0.0;
        double fertility = 0.0;
        double temperature = 0.0;
        int cells = grid.width() * grid.height();

        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                Cell cell = grid.cellAt(x, y);
                moisture += cell.moisture();
                fertility += cell.fertility();
                temperature += cell.temperature();
            }
        }

        history.add(new PopulationSnapshot(
                tick,
                season,
                plantCount,
                rabbitCount,
                wolfCount,
                moisture / cells,
                fertility / cells,
                temperature / cells
        ));

        int maxHistory = 320;
        if (history.size() > maxHistory) {
            history.remove(0);
        }
    }

    private List<Position> occupiedPositions() {
        List<Position> positions = new ArrayList<>(plantCount + rabbitCount + wolfCount);
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (organisms[y][x] != null) {
                    positions.add(new Position(x, y));
                }
            }
        }
        return positions;
    }

    private void adjustCount(OrganismKind kind, int amount) {
        switch (kind) {
            case PLANT -> plantCount += amount;
            case RABBIT -> rabbitCount += amount;
            case WOLF -> wolfCount += amount;
        }
    }
}
