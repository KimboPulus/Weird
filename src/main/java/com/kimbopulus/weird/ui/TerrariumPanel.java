package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.Cell;
import com.kimbopulus.weird.sim.Organism;
import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.sim.WorldGrid;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class TerrariumPanel extends JPanel {
    private static final Color GRID_LINE = new Color(46, 38, 32, 40);
    private static final Color PLANT = new Color(54, 145, 66);
    private static final Color PLANT_DARK = new Color(31, 95, 45);
    private static final Color RABBIT = new Color(214, 196, 163);
    private static final Color RABBIT_DARK = new Color(129, 99, 73);
    private static final Color WOLF = new Color(92, 96, 102);
    private static final Color WOLF_DARK = new Color(46, 49, 54);
    private static final Color HOVER = new Color(255, 246, 172, 180);

    private final Simulation simulation;
    private Position hoverPosition;

    public TerrariumPanel(Simulation simulation) {
        this.simulation = simulation;
        setBackground(new Color(24, 26, 23));
        setPreferredSize(new Dimension(960, 640));
    }

    public void setHoverPosition(Position hoverPosition) {
        this.hoverPosition = hoverPosition;
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
                    drawRabbit(g, px, py, cellSize);
                } else if (organism.kind() == OrganismKind.WOLF) {
                    drawWolf(g, px, py, cellSize);
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

    private void drawRabbit(Graphics2D g, int x, int y, int size) {
        int pad = Math.max(2, size / 6);
        int bodyW = size - pad * 2;
        int bodyH = Math.max(4, size / 2);

        g.setColor(RABBIT_DARK);
        g.fillOval(x + pad, y + pad / 2, size / 5, size / 2);
        g.fillOval(x + size - pad - size / 5, y + pad / 2, size / 5, size / 2);
        g.setColor(RABBIT);
        g.fillOval(x + pad, y + size / 3, bodyW, bodyH);
    }

    private void drawWolf(Graphics2D g, int x, int y, int size) {
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

        g.setColor(WOLF_DARK);
        g.fillPolygon(xs, ys, 3);
        g.setColor(WOLF);
        g.fillOval(x + size / 3, y + size / 3, size / 3, size / 3);
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
}
