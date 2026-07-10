package com.kimbopulus.weird.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class Simulation {
    public static final int DEFAULT_WIDTH = 38;
    public static final int DEFAULT_HEIGHT = 26;
    private static final double DROUGHT_RAIN_GLOBAL_COOLING = 2.5;
    private static final double DROUGHT_STACK_GLOBAL_HEATING = 2.0;
    private static final double DROUGHT_INITIAL_DRYING = 1.64;
    private static final double DROUGHT_LINGER_DRYING = 0.60;

    private final Random random;
    private final WorldGrid grid;
    private final Organism[][] organisms;
    private final Plant[][] coveredPlants;
    private final List<PopulationSnapshot> history = new ArrayList<>();
    private final List<DeathEvent> deathEvents = new ArrayList<>();
    private final List<BirthEvent> birthEvents = new ArrayList<>();
    private final List<AreaEffect> areaEffects = new ArrayList<>();
    private int tick;
    private Season season = Season.SPRING;
    private WorldEvent currentEvent = WorldEvent.CALM;
    private boolean sanctuaryPlaced;
    private int plantCount;
    private int rabbitCount;
    private int wolfCount;
    private int humanCount;
    private int bearCount;
    private long deathSequence;
    private long birthSequence;

    public Simulation(int width, int height, long seed) {
        this.random = new Random(seed);
        this.grid = new WorldGrid(width, height, random);
        this.organisms = new Organism[height][width];
        this.coveredPlants = new Plant[height][width];
    }

    public static Simulation createDefault() {
        Simulation simulation = new Simulation(DEFAULT_WIDTH, DEFAULT_HEIGHT, System.nanoTime());
        simulation.seedPlants(95);
        simulation.seedRabbits(12);
        simulation.seedRabbits(2, RabbitSex.MALE);
        simulation.seedWolves(5);
        simulation.seedHumans(4);
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
        applyAreaEffects();
        sproutWildPlants();
        migrateWildlife();
        visitBears();

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
                coveredPlants[y][x] = null;
            }
        }
        plantCount = 0;
        rabbitCount = 0;
        wolfCount = 0;
        humanCount = 0;
        bearCount = 0;
        grid.reset(random);
        history.clear();
        tick = 0;
        season = Season.SPRING;
        currentEvent = WorldEvent.CALM;
        sanctuaryPlaced = false;
        deathEvents.clear();
        birthEvents.clear();
        areaEffects.clear();
        seedPlants(140);
        seedRabbits(15);
        seedRabbits(3, RabbitSex.MALE);
        seedWolves(5);
        seedHumans(5);
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
        restoreCoveredPlant(position);
        if (organism != null) {
            adjustCount(organism.kind(), -1);
            if (organism.kind() != OrganismKind.PLANT) {
                recordDeath(organism.kind(), position);
            }
        }
        return organism;
    }

    public Organism removeOrganismQuietly(Position position) {
        if (!grid.contains(position)) {
            return null;
        }
        Organism organism = organismAt(position);
        restoreCoveredPlant(position);
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

        Plant underneath = coveredPlants[from.y()][from.x()];
        coveredPlants[from.y()][from.x()] = null;
        organisms[from.y()][from.x()] = underneath;
        organisms[to.y()][to.x()] = organism;
        return true;
    }

    public boolean moveAnimal(Position from, Position to, OrganismKind foodKind) {
        Organism mover = organismAt(from);
        Organism target = organismAt(to);
        if (target != null && target.kind() == OrganismKind.PLANT) {
            if (mover != null && preservesPlantsWhileMoving(mover.kind())) {
                return moveOntoPlantPreserving(from, to, mover, (Plant) target);
            }
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
            if (organism == null || organism.kind() == OrganismKind.PLANT) {
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
            case HUMAN -> humanCount;
            case BEAR -> bearCount;
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
        seedRabbits(amount, RabbitSex.FEMALE);
    }

    public void seedRabbits(int amount, RabbitSex sex) {
        for (int i = 0; i < amount; i++) {
            placeRandomly(new Rabbit(sex), 150);
        }
    }

    public void seedWolves(int amount) {
        for (int i = 0; i < amount; i++) {
            placeRandomly(new Wolf(), 150);
        }
    }

    public void seedHumans(int amount) {
        for (int i = 0; i < amount; i++) {
            placeRandomly(new Human(), 150);
        }
    }

    public List<DeathEvent> recentDeathEvents() {
        return List.copyOf(deathEvents);
    }

    public List<BirthEvent> recentBirthEvents() {
        return List.copyOf(birthEvents);
    }

    public void clearDeathEvents() {
        deathEvents.clear();
    }

    public boolean rain(Position center) {
        if (grid.contains(center)) {
            Position origin = patchOrigin(center, 4, 4);
            if (overlapsActiveDrought(origin, 4, 4)) {
                grid.coolAll(DROUGHT_RAIN_GLOBAL_COOLING);
            }
            grid.rainPatch(origin, 4, 4, 0.18);
            grid.coolPatch(origin, 4, 4, 11.2);
            grid.fertilizePatch(origin, 4, 4, 0.16);
            areaEffects.add(new AreaEffect(EffectKind.RAIN, origin, 4, 4, 4, 0, 10, 0.08, 0.08, 2, 0.85));
            return true;
        }
        return false;
    }

    public boolean rainWillCoolWorld(Position center) {
        if (!grid.contains(center)) {
            return false;
        }
        return overlapsActiveDrought(patchOrigin(center, 4, 4), 4, 4);
    }

    public boolean droughtWillHeatWorld(Position center) {
        if (!grid.contains(center)) {
            return false;
        }
        Position origin = patchOrigin(center, 4, 4);
        for (AreaEffect effect : areaEffects) {
            if (effect.kind() == EffectKind.DROUGHT
                    && effect.origin().equals(origin)
                    && effect.width() == 4
                    && effect.height() == 4) {
                return true;
            }
        }
        return false;
    }

    public boolean rainBoost(Position center) {
        if (grid.contains(center)) {
            Position origin = patchOrigin(center, 4, 4);
            if (overlapsActiveDrought(origin, 4, 4)) {
                grid.coolAll(DROUGHT_RAIN_GLOBAL_COOLING);
            }
            grid.rainPatch(origin, 4, 4, 0.14);
            grid.coolPatch(origin, 4, 4, 13.6);
            grid.fertilizePatch(origin, 4, 4, 0.22);
            areaEffects.add(new AreaEffect(EffectKind.RAIN, origin, 4, 4, 2, 0, 12, 0.10, 0.10, 3, 1.05));
            return true;
        }
        return false;
    }

    public boolean drought(Position center) {
        if (grid.contains(center)) {
            if (droughtWillHeatWorld(center)) {
                grid.warmAll(DROUGHT_STACK_GLOBAL_HEATING);
            }
            grid.dryAll(0.01);
            Organism target = organismAt(center);
            if (target != null && target.kind() != OrganismKind.PLANT) {
                removeOrganism(center, DeathCause.NATURAL);
                grid.cellAt(center).addFertility(0.08);
            }
            Position origin = patchOrigin(center, 4, 4);
            grid.dryPatch(origin, 4, 4, DROUGHT_INITIAL_DRYING);
            grid.warmPatch(origin, 4, 4, 3.2);
            grid.spendFertilityPatch(origin, 4, 4, 0.18);
            areaEffects.add(new AreaEffect(
                    EffectKind.DROUGHT,
                    origin,
                    4,
                    4,
                    0,
                    0,
                    24,
                    DROUGHT_LINGER_DRYING,
                    0.10,
                    0,
                    0.24
            ));
            return true;
        }
        return false;
    }

    public boolean compost(Position center) {
        if (grid.contains(center)) {
            Position origin = patchOrigin(center, 4, 4);
            grid.fertilizePatch(origin, 4, 4, 0.30);
            grid.rainPatch(origin, 4, 4, 0.08);
            return true;
        }
        return false;
    }

    public boolean compostBoost(Position center) {
        if (grid.contains(center)) {
            Position origin = patchOrigin(center, 4, 4);
            grid.fertilizePatch(origin, 4, 4, 0.15);
            return true;
        }
        return false;
    }

    public boolean lightning(Position position) {
        if (!grid.contains(position) || organismAt(position) == null) {
            return false;
        }
        removeOrganism(position, DeathCause.LIGHTNING);
        return true;
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

    public boolean addHuman(Position position) {
        return placeOrganism(position, new Human());
    }

    public boolean addBear(Position position) {
        return placeOrganism(position, new Bear());
    }

    public boolean addRabbit(Position position) {
        return addRabbit(position, RabbitSex.FEMALE);
    }

    public boolean addRabbit(Position position, RabbitSex sex) {
        return placeOrganism(position, new Rabbit(sex));
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

        int attempts = Math.max(3, (grid.width() * grid.height()) / 100);
        for (int i = 0; i < attempts; i++) {
            Position position = new Position(random.nextInt(grid.width()), random.nextInt(grid.height()));
            if (!isEmpty(position)) {
                continue;
            }

            Cell cell = grid.cellAt(position);
            if (random.nextDouble() < cell.plantGrowthFactor() * 0.022) {
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

        int rabbitFloor = Math.max(8, cells / 80);
        if (plants > cells / 7 && rabbits < rabbitFloor && random.nextDouble() < 0.42) {
            placeRandomly(new Rabbit(random.nextBoolean() ? RabbitSex.MALE : RabbitSex.FEMALE), 60);
        }

        if (rabbits > cells / 34 && wolves < 5 && random.nextDouble() < 0.12) {
            placeRandomly(new Wolf(), 60);
        }
    }

    private void visitBears() {
        if (bearCount > 0 || humanCount == 0) {
            return;
        }
        double arrivalChance = Math.min(0.14, humanCount * 0.0025);
        if (random.nextDouble() >= arrivalChance) {
            return;
        }

        int side = random.nextInt(4);
        Position edge = switch (side) {
            case 0 -> new Position(random.nextInt(grid.width()), 0);
            case 1 -> new Position(grid.width() - 1, random.nextInt(grid.height()));
            case 2 -> new Position(random.nextInt(grid.width()), grid.height() - 1);
            default -> new Position(0, random.nextInt(grid.height()));
        };
        if (!placeOrganism(edge, new Bear())) {
            placeRandomly(new Bear(), 40);
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
            case WILD_BLOOM -> seedPlants(Math.max(12, grid.width() * grid.height() / 70));
            case RABBIT_ARRIVAL -> {
                seedRabbits(Math.max(3, grid.width() * grid.height() / 380));
                seedRabbits(1, RabbitSex.MALE);
            }
            case WOLF_ARRIVAL -> seedWolves(Math.max(1, grid.width() * grid.height() / 900));
            case CALM -> {
            }
        }
    }

    private int plantLimit() {
        return (int) (grid.width() * grid.height() * 0.52);
    }

    private void applyAreaEffects() {
        if (areaEffects.isEmpty()) {
            return;
        }

        List<AreaEffect> remaining = new ArrayList<>(areaEffects.size());
        for (AreaEffect effect : areaEffects) {
            int age = effect.age() + 1;
            if (age < effect.delayTicks()) {
                remaining.add(effect.withAge(age));
                continue;
            }

            if (effect.kind() == EffectKind.RAIN) {
                grid.rainPatch(effect.origin(), effect.width(), effect.height(), effect.magnitude());
                grid.coolPatch(effect.origin(), effect.width(), effect.height(), effect.temperatureDelta());
                grid.fertilizePatch(effect.origin(), effect.width(), effect.height(), effect.fertilityBoost());
                if (age == effect.delayTicks()) {
                    burstPlants(effect.origin(), effect.width(), effect.height(), effect.burstPlants());
                }
            } else {
                grid.dryPatch(effect.origin(), effect.width(), effect.height(), effect.magnitude());
                grid.warmPatch(effect.origin(), effect.width(), effect.height(), effect.temperatureDelta());
                grid.spendFertilityPatch(effect.origin(), effect.width(), effect.height(), effect.fertilityBoost());
            }

            if (age < effect.delayTicks() + effect.durationTicks()) {
                remaining.add(effect.withAge(age));
            }
        }
        areaEffects.clear();
        areaEffects.addAll(remaining);
    }

    private void burstPlants(Position origin, int width, int height, int amount) {
        for (int i = 0; i < amount; i++) {
            int x = Math.max(0, Math.min(grid.width() - 1, origin.x() + random.nextInt(Math.max(1, width))));
            int y = Math.max(0, Math.min(grid.height() - 1, origin.y() + random.nextInt(Math.max(1, height))));
            Position position = new Position(x, y);
            if (!isEmpty(position)) {
                continue;
            }
            placeOrganism(position, new Plant());
        }
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
                humanCount,
                bearCount,
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
        List<Position> positions = new ArrayList<>(plantCount + rabbitCount + wolfCount + humanCount + bearCount);
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
            case HUMAN -> humanCount += amount;
            case BEAR -> bearCount += amount;
        }
    }

    private void recordDeath(OrganismKind kind, Position position) {
        deathEvents.add(new DeathEvent(++deathSequence, kind, DeathCause.NATURAL, position, System.currentTimeMillis()));
        if (deathEvents.size() > 80) {
            deathEvents.remove(0);
        }
    }

    public void recordBirth(OrganismKind kind, Position position) {
        birthEvents.add(new BirthEvent(++birthSequence, kind, position, System.currentTimeMillis()));
        if (birthEvents.size() > 80) {
            birthEvents.remove(0);
        }
    }

    public void removeOrganism(Position position, DeathCause cause) {
        if (!grid.contains(position)) {
            return;
        }
        Organism organism = organismAt(position);
        restoreCoveredPlant(position);
        if (organism != null) {
            adjustCount(organism.kind(), -1);
            if (organism.kind() != OrganismKind.PLANT || cause == DeathCause.LIGHTNING) {
                deathEvents.add(new DeathEvent(++deathSequence, organism.kind(), cause, position, System.currentTimeMillis()));
                if (deathEvents.size() > 80) {
                    deathEvents.remove(0);
                }
            }
        }
    }

    private Position patchOrigin(Position center, int patchWidth, int patchHeight) {
        int startX = Math.max(0, Math.min(center.x() - 1, grid.width() - patchWidth));
        int startY = Math.max(0, Math.min(center.y() - 1, grid.height() - patchHeight));
        return new Position(startX, startY);
    }

    private boolean preservesPlantsWhileMoving(OrganismKind kind) {
        return kind == OrganismKind.HUMAN || kind == OrganismKind.WOLF || kind == OrganismKind.BEAR;
    }

    private boolean moveOntoPlantPreserving(Position from, Position to, Organism mover, Plant plant) {
        if (!grid.contains(from) || !grid.contains(to)) {
            return false;
        }
        Plant underneath = coveredPlants[from.y()][from.x()];
        coveredPlants[from.y()][from.x()] = null;
        organisms[from.y()][from.x()] = underneath;
        coveredPlants[to.y()][to.x()] = plant;
        organisms[to.y()][to.x()] = mover;
        return true;
    }

    private void restoreCoveredPlant(Position position) {
        Plant underneath = coveredPlants[position.y()][position.x()];
        coveredPlants[position.y()][position.x()] = null;
        organisms[position.y()][position.x()] = underneath;
    }

    private boolean overlapsActiveDrought(Position rainOrigin, int rainWidth, int rainHeight) {
        for (AreaEffect effect : areaEffects) {
            if (effect.kind() != EffectKind.DROUGHT) {
                continue;
            }
            if (patchesOverlap(rainOrigin, rainWidth, rainHeight, effect.origin(), effect.width(), effect.height())) {
                return true;
            }
        }
        return false;
    }

    private boolean patchesOverlap(
            Position firstOrigin,
            int firstWidth,
            int firstHeight,
            Position secondOrigin,
            int secondWidth,
            int secondHeight
    ) {
        int firstRight = firstOrigin.x() + firstWidth - 1;
        int firstBottom = firstOrigin.y() + firstHeight - 1;
        int secondRight = secondOrigin.x() + secondWidth - 1;
        int secondBottom = secondOrigin.y() + secondHeight - 1;

        return firstOrigin.x() <= secondRight
                && firstRight >= secondOrigin.x()
                && firstOrigin.y() <= secondBottom
                && firstBottom >= secondOrigin.y();
    }

    private record AreaEffect(
            EffectKind kind,
            Position origin,
            int width,
            int height,
            int delayTicks,
            int age,
            int durationTicks,
            double magnitude,
            double fertilityBoost,
            int burstPlants,
            double temperatureDelta) {
        private AreaEffect withAge(int age) {
            return new AreaEffect(kind, origin, width, height, delayTicks, age, durationTicks,
                    magnitude, fertilityBoost, burstPlants, temperatureDelta);
        }
    }

    private enum EffectKind {
        RAIN,
        DROUGHT
    }
}
