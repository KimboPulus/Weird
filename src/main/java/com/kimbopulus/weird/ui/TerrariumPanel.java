package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.Cell;
import com.kimbopulus.weird.sim.Organism;
import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.sim.WorldGrid;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TerrariumPanel extends JPanel {
    private static final Color GRID_LINE = new Color(46, 38, 32, 40);
    private static final Color PLANT = new Color(54, 145, 66);
    private static final Color PLANT_DARK = new Color(31, 95, 45);
    private static final Color RABBIT = new Color(214, 196, 163);
    private static final Color RABBIT_DARK = new Color(129, 99, 73);
    private static final Color WOLF = new Color(92, 96, 102);
    private static final Color WOLF_DARK = new Color(46, 49, 54);
    private static final Color HOVER = new Color(255, 246, 172, 180);
    private static final long EFFECT_DURATION_MS = 600;

    private final Simulation simulation;
    private final List<ToolEffect> toolEffects = new ArrayList<>();
    private final Timer effectTimer;
    private Position hoverPosition;

    public TerrariumPanel(Simulation simulation) {
        this.simulation = simulation;
        setBackground(new Color(24, 26, 23));
        setPreferredSize(new Dimension(960, 640));
        effectTimer = new Timer(32, event -> advanceEffects());
        effectTimer.setCoalesce(true);
    }

    public void setHoverPosition(Position hoverPosition) {
        this.hoverPosition = hoverPosition;
        repaint();
    }

    public void showToolEffect(Position position, ToolMode mode) {
        toolEffects.add(new ToolEffect(position, mode, System.currentTimeMillis()));
        if (!effectTimer.isRunning()) {
            effectTimer.start();
        }
        repaint();
    }

    public Position positionAtPoint(int x, int y) {
        WorldGrid grid = simulation.grid();
        BoardMetrics metrics = boardMetrics(grid);
        if (x < metrics.offsetX || y < metrics.offsetY || x >= metrics.offsetX + metrics.width || y >= metrics.offsetY + metrics.height) {
            return null;
        }

        int gridX = (x - metrics.offsetX) / metrics.cellSize;
        int gridY = (y - metrics.offsetY) / metrics.cellSize;
        Position position = new Position(gridX, gridY);
        if (!grid.contains(position)) {
            return null;
        }
        return position;
    }

    public String describe(Position position) {
        if (position == null || !simulation.grid().contains(position)) {
            return "Move over the board to inspect a cell.";
        }

        Cell cell = simulation.grid().cellAt(position);
        Organism organism = simulation.organismAt(position);
        String occupant = "empty";
        if (organism != null) {
            occupant = switch (organism.kind()) {
                case PLANT -> "plant";
                case RABBIT -> "rabbit";
                case WOLF -> "wolf";
            };
            occupant += String.format(
                    "%s, age %d, energy %d",
                    organism.veteran() ? " veteran" : "",
                    organism.age(),
                    organism.energy()
            );
        }

        return String.format(
                "Cell %d,%d: %s | moisture %.0f%% | fertility %.0f%% | temp %.1f C",
                position.x(),
                position.y(),
                occupant,
                cell.moisture() * 100.0,
                cell.fertility() * 100.0,
                cell.temperature()
        );
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        WorldGrid grid = simulation.grid();
        BoardMetrics metrics = boardMetrics(grid);

        drawCells(g, grid, metrics.cellSize, metrics.offsetX, metrics.offsetY);
        drawOrganisms(g, grid, metrics.cellSize, metrics.offsetX, metrics.offsetY);
        drawGridLines(g, grid, metrics.cellSize, metrics.offsetX, metrics.offsetY);
        drawToolEffects(g, metrics);
        drawHover(g, metrics);

        g.dispose();
    }

    private void drawCells(Graphics2D g, WorldGrid grid, int cellSize, int offsetX, int offsetY) {
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                Position position = new Position(x, y);
                Cell cell = grid.cellAt(position);
                g.setColor(soilColor(cell));
                g.fillRect(offsetX + x * cellSize, offsetY + y * cellSize, cellSize, cellSize);
            }
        }
    }

    private void drawOrganisms(Graphics2D g, WorldGrid grid, int cellSize, int offsetX, int offsetY) {
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                Position position = new Position(x, y);
                Organism organism = simulation.organismAt(position);
                if (organism == null) {
                    continue;
                }

                int px = offsetX + x * cellSize;
                int py = offsetY + y * cellSize;
                if (organism.kind() == OrganismKind.PLANT) {
                    drawPlant(g, px, py, cellSize);
                } else if (organism.kind() == OrganismKind.RABBIT) {
                    drawRabbit(g, px, py, cellSize, organism.veteran());
                } else if (organism.kind() == OrganismKind.WOLF) {
                    drawWolf(g, px, py, cellSize, organism.veteran());
                }
            }
        }
    }

    private void drawGridLines(Graphics2D g, WorldGrid grid, int cellSize, int offsetX, int offsetY) {
        g.setColor(GRID_LINE);
        g.setStroke(new BasicStroke(1f));
        int boardWidth = grid.width() * cellSize;
        int boardHeight = grid.height() * cellSize;

        for (int x = 0; x <= grid.width(); x++) {
            int px = offsetX + x * cellSize;
            g.drawLine(px, offsetY, px, offsetY + boardHeight);
        }

        for (int y = 0; y <= grid.height(); y++) {
            int py = offsetY + y * cellSize;
            g.drawLine(offsetX, py, offsetX + boardWidth, py);
        }
    }

    private void drawPlant(Graphics2D g, int x, int y, int size) {
        int pad = Math.max(2, size / 5);
        int stemX = x + size / 2;
        int stemTop = y + pad;
        int stemBottom = y + size - pad;

        g.setColor(PLANT_DARK);
        g.drawLine(stemX, stemTop, stemX, stemBottom);
        g.setColor(PLANT);
        g.fillOval(x + pad, y + pad, size / 2, size / 2);
        g.fillOval(x + size / 2 - pad / 2, y + size / 3, size / 2, size / 2);
    }

    private void drawRabbit(Graphics2D g, int x, int y, int size, boolean veteran) {
        int pad = Math.max(2, size / 6);
        int bodyW = size - pad * 2;
        int bodyH = Math.max(4, size / 2);

        drawVeteranMark(g, x, y, size, veteran);
        g.setColor(RABBIT_DARK);
        g.fillOval(x + pad, y + pad / 2, size / 5, size / 2);
        g.fillOval(x + size - pad - size / 5, y + pad / 2, size / 5, size / 2);
        g.setColor(RABBIT);
        g.fillOval(x + pad, y + size / 3, bodyW, bodyH);
    }

    private void drawWolf(Graphics2D g, int x, int y, int size, boolean veteran) {
        int pad = Math.max(2, size / 6);
        int[] xs = {
                x + size / 2,
                x + size - pad,
                x + pad
        };
        int[] ys = {
                y + pad,
                y + size - pad,
                y + size - pad
        };

        drawVeteranMark(g, x, y, size, veteran);
        g.setColor(WOLF_DARK);
        g.fillPolygon(xs, ys, 3);
        g.setColor(WOLF);
        g.fillOval(x + size / 3, y + size / 3, size / 3, size / 3);
    }

    private void drawVeteranMark(Graphics2D g, int x, int y, int size, boolean veteran) {
        if (!veteran) {
            return;
        }
        g.setColor(new Color(235, 240, 237, 220));
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(x + 1, y + 1, size - 3, size - 3);
    }

    private Color soilColor(Cell cell) {
        double moisture = cell.moisture();
        double fertility = cell.fertility();
        double heat = Math.min(1.0, Math.max(0.0, (cell.temperature() - 8.0) / 28.0));

        int red = (int) (92 + fertility * 48 + heat * 28);
        int green = (int) (68 + fertility * 52 + moisture * 36);
        int blue = (int) (42 + moisture * 58);
        return new Color(clamp(red), clamp(green), clamp(blue));
    }

    private void drawHover(Graphics2D g, BoardMetrics metrics) {
        if (hoverPosition == null) {
            return;
        }

        int x = metrics.offsetX + hoverPosition.x() * metrics.cellSize;
        int y = metrics.offsetY + hoverPosition.y() * metrics.cellSize;
        g.setColor(HOVER);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(x + 1, y + 1, metrics.cellSize - 2, metrics.cellSize - 2);
    }

    private void drawToolEffects(Graphics2D g, BoardMetrics metrics) {
        long now = System.currentTimeMillis();
        for (ToolEffect effect : toolEffects) {
            double progress = Math.min(1.0, (now - effect.startedAt) / (double) EFFECT_DURATION_MS);
            int centerX = metrics.offsetX + effect.position.x() * metrics.cellSize + metrics.cellSize / 2;
            int centerY = metrics.offsetY + effect.position.y() * metrics.cellSize + metrics.cellSize / 2;
            int reach = effect.mode == ToolMode.RAIN || effect.mode == ToolMode.DROUGHT
                    ? metrics.cellSize * 3
                    : metrics.cellSize * 2;
            int radius = Math.max(metrics.cellSize / 2, (int) (reach * progress));
            int alpha = (int) (190 * (1.0 - progress));

            g.setColor(effectColor(effect.mode, alpha));
            g.setStroke(new BasicStroke(Math.max(2f, metrics.cellSize / 5f)));
            g.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            if (effect.mode == ToolMode.RAIN) {
                int dropOffset = (int) (metrics.cellSize * progress);
                for (int offset = -metrics.cellSize; offset <= metrics.cellSize; offset += metrics.cellSize) {
                    g.drawLine(centerX + offset, centerY - metrics.cellSize + dropOffset,
                            centerX + offset - 2, centerY - metrics.cellSize / 2 + dropOffset);
                }
            }
        }
    }

    private Color effectColor(ToolMode mode, int alpha) {
        return switch (mode) {
            case RAIN -> new Color(78, 151, 214, alpha);
            case DROUGHT -> new Color(218, 135, 65, alpha);
            case COMPOST -> new Color(104, 157, 78, alpha);
            case CLEAR -> new Color(231, 220, 179, alpha);
            case PLANT -> new Color(73, 184, 89, alpha);
            case RABBIT -> new Color(235, 211, 171, alpha);
            case WOLF -> new Color(157, 164, 177, alpha);
        };
    }

    private void advanceEffects() {
        long now = System.currentTimeMillis();
        Iterator<ToolEffect> iterator = toolEffects.iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().startedAt > EFFECT_DURATION_MS) {
                iterator.remove();
            }
        }
        repaint();
        if (toolEffects.isEmpty()) {
            effectTimer.stop();
        }
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private BoardMetrics boardMetrics(WorldGrid grid) {
        int cellSize = Math.max(4, Math.min(getWidth() / grid.width(), getHeight() / grid.height()));
        int boardWidth = cellSize * grid.width();
        int boardHeight = cellSize * grid.height();
        int offsetX = (getWidth() - boardWidth) / 2;
        int offsetY = (getHeight() - boardHeight) / 2;
        return new BoardMetrics(cellSize, boardWidth, boardHeight, offsetX, offsetY);
    }

    private record BoardMetrics(int cellSize, int width, int height, int offsetX, int offsetY) {
    }

    private record ToolEffect(Position position, ToolMode mode, long startedAt) {
    }
}
