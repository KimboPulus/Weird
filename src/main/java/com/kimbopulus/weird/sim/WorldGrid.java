package com.kimbopulus.weird.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class WorldGrid {
    private final int width;
    private final int height;
    private final Cell[][] cells;

    public WorldGrid(int width, int height, Random random) {
        this.width = width;
        this.height = height;
        this.cells = new Cell[height][width];
        reset(random);
    }

    public void reset(Random random) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double moisture = 0.35 + random.nextDouble() * 0.4;
                double temperature = 16.0 + random.nextDouble() * 10.0;
                double fertility = 0.35 + random.nextDouble() * 0.35;
                if (cells[y][x] == null) {
                    cells[y][x] = new Cell(moisture, temperature, fertility);
                } else {
                    cells[y][x].reset(moisture, temperature, fertility);
                }
            }
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean contains(Position position) {
        return position.x() >= 0 && position.y() >= 0 && position.x() < width && position.y() < height;
    }

    public Cell cellAt(Position position) {
        if (!contains(position)) {
            throw new IllegalArgumentException("Position outside grid: " + position);
        }
        return cells[position.y()][position.x()];
    }

    public Cell cellAt(int x, int y) {
        return cells[y][x];
    }

    public List<Position> neighbors(Position position, Random random) {
        List<Position> result = new ArrayList<>(4);
        for (Direction direction : Direction.values()) {
            Position next = position.move(direction);
            if (contains(next)) {
                result.add(next);
            }
        }
        Collections.shuffle(result, random);
        return result;
    }

    public void applySeason(Season season) {
        forEachCell(cell -> {
            if (cell.sanctuary()) {
                cell.stabilizeSanctuary();
                return;
            }
            if (season.moistureShift() >= 0.0) {
                cell.addRain(season.moistureShift() * 0.02);
            } else {
                cell.dry(Math.abs(season.moistureShift()) * 0.02);
            }

            if (season.temperatureShift() >= 0.0) {
                cell.warm(season.temperatureShift() * 0.8);
            } else {
                cell.cool(Math.abs(season.temperatureShift()) * 0.8);
            }
        });
    }

    public void rainAround(Position center, int radius, double amount) {
        affectAround(center, radius, cell -> cell.addRain(amount));
    }

    public void dryAround(Position center, int radius, double amount) {
        affectAround(center, radius, cell -> cell.dry(amount));
    }

    public void fertilizeAround(Position center, int radius, double amount) {
        affectAround(center, radius, cell -> cell.addFertility(amount));
    }

    public void rainAll(double amount) {
        forEachCell(cell -> {
            if (!cell.sanctuary()) {
                cell.addRain(amount);
            }
        });
    }

    public void dryAndWarmAll(double dryAmount, double heatAmount) {
        forEachCell(cell -> {
            if (cell.sanctuary()) {
                return;
            }
            cell.dry(dryAmount);
            cell.warm(heatAmount);
        });
    }

    public void createSanctuary(Position corner) {
        for (int y = corner.y(); y <= Math.min(height - 1, corner.y() + 1); y++) {
            for (int x = corner.x(); x <= Math.min(width - 1, corner.x() + 1); x++) {
                cells[y][x].makeSanctuary();
            }
        }
    }

    private void affectAround(Position center, int radius, CellAction action) {
        for (int y = Math.max(0, center.y() - radius); y <= Math.min(height - 1, center.y() + radius); y++) {
            for (int x = Math.max(0, center.x() - radius); x <= Math.min(width - 1, center.x() + radius); x++) {
                action.apply(cells[y][x]);
            }
        }
    }

    private void forEachCell(CellAction action) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                action.apply(cells[y][x]);
            }
        }
    }

    private interface CellAction {
        void apply(Cell cell);
    }
}
