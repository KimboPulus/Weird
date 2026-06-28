package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.Cell;
import com.kimbopulus.weird.sim.BirthEvent;
import com.kimbopulus.weird.sim.DeathCause;
import com.kimbopulus.weird.sim.DeathEvent;
import com.kimbopulus.weird.sim.Organism;
import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Rabbit;
import com.kimbopulus.weird.sim.RabbitSex;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.sim.WorldGrid;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TerrariumPanel extends JPanel {
    private static final Color GRID_LINE = new Color(39, 33, 27, 28);
    private static final Color PLANT = new Color(66, 164, 77);
    private static final Color PLANT_LIGHT = new Color(111, 196, 98);
    private static final Color PLANT_DARK = new Color(31, 91, 42);
    private static final Color RABBIT = new Color(222, 204, 170);
    private static final Color RABBIT_LIGHT = new Color(244, 232, 205);
    private static final Color RABBIT_DARK = new Color(112, 82, 61);
    private static final Color RABBIT_PINK = new Color(210, 145, 140);
    private static final Color WOLF = new Color(111, 119, 128);
    private static final Color WOLF_LIGHT = new Color(164, 172, 178);
    private static final Color WOLF_DARK = new Color(44, 48, 53);
    private static final Color HUMAN = new Color(70, 112, 151);
    private static final Color HUMAN_SKIN = new Color(229, 184, 142);
    private static final Color BEAR = new Color(112, 72, 43);
    private static final Color BEAR_LIGHT = new Color(169, 116, 69);
    private static final Color SHADOW = new Color(24, 25, 22, 85);
    private static final Color WET_SOIL_DETAIL = new Color(129, 169, 170, 75);
    private static final Color DRY_SOIL_DETAIL = new Color(74, 55, 35, 80);
    private static final Color SOIL_SPECK = new Color(49, 45, 34, 48);
    private static final Color SANCTUARY_EDGE = new Color(221, 215, 128, 185);
    private static final Color VETERAN_EDGE = new Color(235, 240, 237, 220);
    private static final Color HOVER = new Color(255, 246, 172, 180);
    private static final BasicStroke GRID_STROKE = new BasicStroke(1f);
    private static final BasicStroke PLANT_STROKE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke VETERAN_STROKE = new BasicStroke(1.5f);
    private static final BasicStroke HOVER_STROKE = new BasicStroke(2f);
    private static final long EFFECT_DURATION_MS = 600;
    private static final long BANNER_DURATION_MS = 1800;
    private static final long LEVEL_UP_DURATION_MS = 2600;
    private static final long DEATH_DURATION_MS = 2800;
    private static final long BIRTH_DURATION_MS = 1500;
    private static final Color[][][] SOIL_PALETTE = createSoilPalette();

    private final Simulation simulation;
    private final List<ToolEffect> toolEffects = new ArrayList<>();
    private final Timer effectTimer;
    private Position hoverPosition;
    private String bannerText;
    private long bannerStartedAt;
    private boolean levelUpBanner;
    private long lastDeathId;
    private long lastBirthId;

    public TerrariumPanel(Simulation simulation) {
        this.simulation = simulation;
        setBackground(new Color(24, 26, 23));
        setPreferredSize(new Dimension(960, 640));
        effectTimer = new Timer(20, event -> advanceEffects());
        effectTimer.setCoalesce(true);
    }

    public boolean setHoverPosition(Position hoverPosition) {
        if (positionsEqual(this.hoverPosition, hoverPosition)) {
            return false;
        }
        Position previous = this.hoverPosition;
        this.hoverPosition = hoverPosition;
        repaintCell(previous);
        repaintCell(hoverPosition);
        return true;
    }

    public void showToolEffect(Position position, ToolMode mode) {
        toolEffects.add(new ToolEffect(position, mode, System.currentTimeMillis()));
        if (!effectTimer.isRunning()) {
            effectTimer.start();
        }
        repaint();
    }

    public void showBanner(String text) {
        bannerText = text;
        bannerStartedAt = System.currentTimeMillis();
        levelUpBanner = false;
        if (!effectTimer.isRunning()) {
            effectTimer.start();
        }
        repaint();
    }

    public void showLevelUp(String text) {
        bannerText = text;
        bannerStartedAt = System.currentTimeMillis();
        levelUpBanner = true;
        if (!effectTimer.isRunning()) {
            effectTimer.start();
        }
        repaint();
    }

    public void syncDeathEffects() {
        List<DeathEvent> deaths = simulation.recentDeathEvents();
        if (!deaths.isEmpty() && deaths.get(deaths.size() - 1).id() > lastDeathId) {
            lastDeathId = deaths.get(deaths.size() - 1).id();
            if (!effectTimer.isRunning()) {
                effectTimer.start();
            }
        }
    }

    public void syncBirthEffects() {
        List<BirthEvent> births = simulation.recentBirthEvents();
        if (!births.isEmpty() && births.get(births.size() - 1).id() > lastBirthId) {
            lastBirthId = births.get(births.size() - 1).id();
            if (!effectTimer.isRunning()) {
                effectTimer.start();
            }
        }
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
                case RABBIT -> organism instanceof Rabbit rabbit ? "rabbit " + rabbit.sex().name().toLowerCase() : "rabbit";
                case WOLF -> "wolf";
                case HUMAN -> "human";
                case BEAR -> "bear";
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
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        WorldGrid grid = simulation.grid();
        BoardMetrics metrics = boardMetrics(grid);

        drawCells(g, grid, metrics.cellSize, metrics.offsetX, metrics.offsetY);
        drawOrganisms(g, grid, metrics.cellSize, metrics.offsetX, metrics.offsetY);
        drawDeathEffects(g, metrics);
        drawGridLines(g, grid, metrics.cellSize, metrics.offsetX, metrics.offsetY);
        drawToolEffects(g, metrics);
        drawBirthEffects(g, metrics);
        drawHover(g, metrics);
        drawCrisisEdge(g, metrics);
        drawBanner(g, metrics);

        g.dispose();
    }

    private void drawCells(Graphics2D g, WorldGrid grid, int cellSize, int offsetX, int offsetY) {
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                Cell cell = grid.cellAt(x, y);
                g.setColor(soilColor(cell));
                int px = offsetX + x * cellSize;
                int py = offsetY + y * cellSize;
                g.fillRect(px, py, cellSize, cellSize);
                drawSoilTexture(g, cell, x, y, px, py, cellSize);
                if (cell.sanctuary()) {
                    g.setColor(SANCTUARY_EDGE);
                    g.setStroke(HOVER_STROKE);
                    g.drawRect(px + 1, py + 1, cellSize - 3, cellSize - 3);
                }
            }
        }
    }

    private void drawOrganisms(Graphics2D g, WorldGrid grid, int cellSize, int offsetX, int offsetY) {
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                Organism organism = simulation.organismAt(x, y);
                if (organism == null) {
                    continue;
                }

                int px = offsetX + x * cellSize;
                int py = offsetY + y * cellSize;
                if (organism.kind() == OrganismKind.PLANT) {
                    drawPlant(g, px, py, cellSize, x + y);
                } else if (organism.kind() == OrganismKind.RABBIT) {
                    Rabbit rabbit = organism instanceof Rabbit value ? value : null;
                    drawRabbit(g, px, py, cellSize, organism.veteran(), (x + y) % 2 == 0, rabbit);
                } else if (organism.kind() == OrganismKind.WOLF) {
                    drawWolf(g, px, py, cellSize, organism.veteran(), (x + y) % 2 == 0);
                } else if (organism.kind() == OrganismKind.HUMAN) {
                    drawHuman(g, px, py, cellSize);
                } else if (organism.kind() == OrganismKind.BEAR) {
                    drawBear(g, px, py, cellSize, (x + y) % 2 == 0);
                }
            }
        }
    }

    private void drawGridLines(Graphics2D g, WorldGrid grid, int cellSize, int offsetX, int offsetY) {
        g.setColor(GRID_LINE);
        g.setStroke(GRID_STROKE);
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

    private void drawPlant(Graphics2D g, int x, int y, int size, int variation) {
        int pad = Math.max(2, size / 6);
        int stemX = x + size / 2;
        int stemTop = y + pad + variation % 2;
        int stemBottom = y + size - 2;

        g.setColor(SHADOW);
        g.fillOval(x + size / 4, y + size - 5, size / 2, 4);
        g.setColor(PLANT_DARK);
        g.setStroke(PLANT_STROKE);
        g.drawLine(stemX, stemTop, stemX, stemBottom);
        g.setColor(PLANT);
        g.fillOval(x + pad, y + size / 3, size / 2, size / 3);
        g.fillOval(x + size / 2 - 1, y + size / 4, size / 3, size / 2);
        g.setColor(PLANT_LIGHT);
        g.fillOval(x + size / 2 - 2, y + pad, Math.max(3, size / 3), Math.max(3, size / 3));
        g.setColor(PLANT_DARK);
        g.drawLine(stemX, y + size / 2, x + pad + size / 4, y + size / 2);
    }

    private void drawRabbit(Graphics2D g, int x, int y, int size, boolean veteran, boolean facesRight, Rabbit rabbit) {
        int headX = facesRight ? x + size - 8 : x + 1;
        drawVeteranMark(g, x, y, size, veteran);
        g.setColor(SHADOW);
        g.fillOval(x + 2, y + size - 5, size - 3, 4);
        g.setColor(rabbit != null && rabbit.sex() == RabbitSex.FEMALE ? new Color(236, 209, 198) : RABBIT);
        g.fillOval(x + 2, y + size / 2 - 2, size - 8, size / 2 + 1);
        g.fillOval(headX, y + size / 3, 8, 8);
        g.fillOval(headX + (facesRight ? 1 : 5), y, 3, size / 2 + 1);
        g.fillOval(headX + (facesRight ? 4 : 2), y + 1, 3, size / 2);
        if (rabbit != null && rabbit.sex() == RabbitSex.FEMALE) {
            g.setColor(new Color(212, 125, 154));
            g.fillOval(x + size / 2 - 2, y + 2, 4, 3);
        }
        g.setColor(RABBIT_PINK);
        g.fillOval(headX + (facesRight ? 2 : 5), y + 1, 1, size / 2 - 2);
        g.fillOval(headX + (facesRight ? 5 : 3), y + 2, 1, size / 2 - 3);
        g.setColor(RABBIT_LIGHT);
        g.fillOval(x + (facesRight ? 1 : size - 6), y + size / 2 - 1, 5, 5);
        g.fillOval(x + (facesRight ? 7 : 3), y + size - 6, 7, 4);
        g.setColor(RABBIT_DARK);
        g.fillOval(headX + (facesRight ? 6 : 1), y + size / 3 + 2, 2, 2);
        g.setColor(RABBIT_PINK);
        g.fillOval(headX + (facesRight ? 7 : 0), y + size / 3 + 5, 2, 2);
    }

    private void drawWolf(Graphics2D g, int x, int y, int size, boolean veteran, boolean facesRight) {
        int headX = facesRight ? x + size - 8 : x + 1;
        drawVeteranMark(g, x, y, size, veteran);
        g.setColor(SHADOW);
        g.fillOval(x + 2, y + size - 5, size - 3, 4);
        g.setColor(WOLF_DARK);
        int tailBase = facesRight ? x + 4 : x + size - 4;
        int tailTip = facesRight ? x : x + size;
        g.fillPolygon(
                new int[]{tailBase, tailTip, tailBase + (facesRight ? 2 : -2)},
                new int[]{y + size / 2, y + size / 3, y + size - 4},
                3
        );
        g.setColor(WOLF);
        g.fillRoundRect(x + 3, y + size / 2 - 2, size - 7, size / 2, 5, 5);
        g.fillOval(headX, y + size / 3 - 1, 8, 8);
        g.setColor(WOLF_DARK);
        g.fillPolygon(
                new int[]{headX + 1, headX + 3, headX + 4},
                new int[]{y + size / 3, y + 1, y + size / 3 + 1},
                3
        );
        g.fillPolygon(
                new int[]{headX + 4, headX + 6, headX + 7},
                new int[]{y + size / 3, y + 2, y + size / 3 + 2},
                3
        );
        g.setColor(WOLF_LIGHT);
        g.fillOval(headX + (facesRight ? 4 : 1), y + size / 3 + 1, 3, 3);
        g.setColor(WOLF_DARK);
        g.fillOval(headX + (facesRight ? 6 : 1), y + size / 3 + 2, 2, 2);
        g.fillRect(x + 5, y + size - 5, 2, 4);
        g.fillRect(x + size - 6, y + size - 5, 2, 4);
    }

    private void drawHuman(Graphics2D g, int x, int y, int size) {
        int center = x + size / 2;
        int width = Math.min(size - 1, Math.max(9, (int) (size * 0.9)));
        int left = x + (size - width) / 2;
        int right = left + width;
        g.setColor(SHADOW);
        g.fillOval(x + 3, y + size - 4, size - 6, 3);
        g.setColor(HUMAN_SKIN);
        g.fillOval(center - 4, y + 1, 8, 8);
        g.setColor(HUMAN);
        g.fillRoundRect(center - 4, y + 9, 8, 8, 3, 3);
        g.setStroke(GRID_STROKE);
        g.drawLine(center - 2, y + 16, left + 1, y + size - 2);
        g.drawLine(center + 2, y + 16, right - 1, y + size - 2);
        g.drawLine(center - 4, y + 10, left - 1, y + 14);
        g.drawLine(center + 4, y + 10, right + 1, y + 14);
    }

    private void drawBear(Graphics2D g, int x, int y, int size, boolean facesRight) {
        int headX = facesRight ? x + size - 9 : x + 1;
        g.setColor(SHADOW);
        g.fillOval(x + 1, y + size - 5, size - 2, 4);
        g.setColor(BEAR);
        g.fillOval(x + 2, y + size / 3, size - 7, size / 2 + 2);
        g.fillOval(headX, y + size / 3 - 2, 9, 9);
        g.fillOval(headX + 1, y + 2, 4, 4);
        g.fillOval(headX + 5, y + 2, 4, 4);
        g.setColor(BEAR_LIGHT);
        g.fillOval(headX + (facesRight ? 5 : 0), y + size / 3 + 3, 5, 4);
        g.setColor(Color.BLACK);
        g.fillOval(headX + (facesRight ? 6 : 2), y + size / 3 + 1, 2, 2);
    }

    private void drawVeteranMark(Graphics2D g, int x, int y, int size, boolean veteran) {
        if (!veteran) {
            return;
        }
        g.setColor(VETERAN_EDGE);
        g.setStroke(VETERAN_STROKE);
        g.drawOval(x + 1, y + 1, size - 3, size - 3);
    }

    private Color soilColor(Cell cell) {
        int moisture = clampLevel((int) Math.round(cell.moisture() * 7));
        int fertility = clampLevel((int) Math.round(cell.fertility() * 7));
        int heat = clampLevel((int) Math.round((cell.temperature() - 8.0) / 28.0 * 7));
        return SOIL_PALETTE[moisture][fertility][heat];
    }

    private void drawSoilTexture(Graphics2D g, Cell cell, int gridX, int gridY, int x, int y, int size) {
        int hash = gridX * 31 + gridY * 17;
        int dotX = x + 3 + Math.floorMod(hash, Math.max(1, size - 6));
        int dotY = y + 3 + Math.floorMod(hash / 7, Math.max(1, size - 6));
        if (cell.moisture() > 0.68) {
            g.setColor(WET_SOIL_DETAIL);
            g.fillOval(dotX, dotY, Math.max(2, size / 6), Math.max(2, size / 7));
        } else if (cell.moisture() < 0.28) {
            g.setColor(DRY_SOIL_DETAIL);
            g.drawLine(x + size / 3, y + size / 2, x + size / 2, y + size / 3);
            g.drawLine(x + size / 2, y + size / 3, x + size * 2 / 3, y + size / 2);
        } else if ((hash & 3) == 0) {
            g.setColor(SOIL_SPECK);
            g.fillOval(dotX, dotY, 2, 2);
        }
    }

    private void drawHover(Graphics2D g, BoardMetrics metrics) {
        if (hoverPosition == null) {
            return;
        }

        int x = metrics.offsetX + hoverPosition.x() * metrics.cellSize;
        int y = metrics.offsetY + hoverPosition.y() * metrics.cellSize;
        g.setColor(HOVER);
        g.setStroke(HOVER_STROKE);
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

    private void drawCrisisEdge(Graphics2D g, BoardMetrics metrics) {
        String crisis = null;
        Color color = null;
        if (simulation.count(OrganismKind.RABBIT) < 12) {
            crisis = "RABBITS LOW";
            color = new Color(212, 153, 67, 210);
        } else if (simulation.count(OrganismKind.WOLF) < 2) {
            crisis = "WOLVES LOW";
            color = new Color(194, 83, 66, 210);
        } else if (simulation.count(OrganismKind.RABBIT) > 105) {
            crisis = "RABBITS HIGH";
            color = new Color(194, 83, 66, 210);
        } else if (simulation.count(OrganismKind.PLANT) > 1100) {
            crisis = "PLANTS HIGH";
            color = new Color(212, 153, 67, 210);
        } else if (simulation.count(OrganismKind.HUMAN) < 3) {
            crisis = "HUMANS LOW";
            color = new Color(194, 83, 66, 210);
        }
        if (crisis == null) {
            return;
        }

        g.setColor(color);
        g.setStroke(new BasicStroke(4f));
        g.drawRect(metrics.offsetX + 2, metrics.offsetY + 2, metrics.width - 5, metrics.height - 5);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
        FontMetrics fontMetrics = g.getFontMetrics();
        int labelWidth = fontMetrics.stringWidth(crisis) + 16;
        g.fillRoundRect(metrics.offsetX + 8, metrics.offsetY + 8, labelWidth, 24, 5, 5);
        g.setColor(Color.WHITE);
        g.drawString(crisis, metrics.offsetX + 16, metrics.offsetY + 25);
    }

    private void drawDeathEffects(Graphics2D g, BoardMetrics metrics) {
        long now = System.currentTimeMillis();
        for (DeathEvent death : simulation.recentDeathEvents()) {
            double progress = (now - death.createdAtMillis()) / (double) DEATH_DURATION_MS;
            if (progress < 0.0 || progress >= 1.0) {
                continue;
            }
            int alpha = (int) (210 * (1.0 - progress));
            int size = Math.max(5, (int) (metrics.cellSize * (1.0 - progress * 0.35)));
            int x = metrics.offsetX + death.position().x() * metrics.cellSize
                    + (metrics.cellSize - size) / 2;
            int y = metrics.offsetY + death.position().y() * metrics.cellSize
                    + (metrics.cellSize - size) / 2 - (int) (progress * metrics.cellSize * 0.6);
            drawDeathShape(g, death.kind(), death.cause(), x, y, size, alpha, progress);
        }
    }

    private void drawBirthEffects(Graphics2D g, BoardMetrics metrics) {
        long now = System.currentTimeMillis();
        for (BirthEvent birth : simulation.recentBirthEvents()) {
            double progress = (now - birth.createdAtMillis()) / (double) BIRTH_DURATION_MS;
            if (progress < 0.0 || progress >= 1.0) {
                continue;
            }
            int alpha = (int) (220 * (1.0 - progress));
            int size = Math.max(6, (int) (metrics.cellSize * (0.75 + progress * 0.8)));
            int x = metrics.offsetX + birth.position().x() * metrics.cellSize
                    + (metrics.cellSize - size) / 2;
            int y = metrics.offsetY + birth.position().y() * metrics.cellSize
                    + (metrics.cellSize - size) / 2 - (int) (progress * metrics.cellSize * 0.1);
            drawBirthShape(g, birth.kind(), x, y, size, alpha, progress);
        }
    }

    private void drawDeathShape(Graphics2D g, OrganismKind kind, DeathCause cause, int x, int y, int size, int alpha, double progress) {
        Color color = switch (kind) {
            case RABBIT -> new Color(188, 48, 52, alpha);
            case WOLF -> new Color(145, 153, 164, alpha);
            case HUMAN -> new Color(176, 48, 56, alpha);
            case BEAR -> new Color(151, 96, 58, alpha);
            case PLANT -> new Color(96, 175, 91, alpha);
        };
        g.setColor(color);
        if (kind == OrganismKind.HUMAN || kind == OrganismKind.RABBIT) {
            int center = x + size / 2;
            int splatter = Math.max(2, size / 5);
            g.fillOval(center - 3, y, 6, 6);
            g.fillOval(x + 1, y + size / 2, 4, 4);
            g.fillOval(x + size - 5, y + size / 2, 4, 4);
            g.setStroke(new BasicStroke(kind == OrganismKind.HUMAN ? 3f : 2.4f));
            g.drawLine(center, y + 5, center, y + size - 1);
            g.drawLine(center, y + size / 2, x, y + size - 1);
            g.drawLine(center, y + size / 2, x + size - 1, y + size - 1);
            g.setColor(new Color(120, 10, 18, alpha));
            g.fillOval(center - 1, y + size / 2, 2, 2);
            g.fillOval(x + size / 3, y + size / 2, splatter, splatter);
            g.fillOval(x + size / 2, y + size / 3, splatter, splatter);
            g.fillOval(x + size / 4, y + size / 2 + 1, splatter + 1, splatter);
        } else if (kind == OrganismKind.WOLF && cause == DeathCause.HUMAN_ATTACK) {
            drawKnifeKill(g, x, y, size, alpha, progress);
        } else if (kind == OrganismKind.WOLF) {
            g.fillPolygon(
                    new int[]{x + size / 2, x + size, x},
                    new int[]{y, y + size, y + size},
                    3
            );
        } else {
            g.fillOval(x, y + size / 3, size, size * 2 / 3);
            g.fillOval(x + size / 2, y, size / 2, size / 2);
        }
    }

    private void drawKnifeKill(Graphics2D g, int x, int y, int size, int alpha, double progress) {
        int bladeX = x + size / 2 + (int) ((progress - 0.5) * size * 0.25);
        int bladeTop = y - (int) (progress * size * 0.4);
        int bladeBottom = y + size - (int) (progress * size * 0.08);

        g.setColor(new Color(176, 58, 56, alpha));
        g.fillOval(x + 1, y + size / 3, size - 2, size / 2);
        g.fillOval(x + size / 3, y + 1, size / 3, size / 3);
        g.setColor(new Color(72, 52, 43, alpha));
        g.fillRect(bladeX - 1, bladeTop + 4, 2, Math.max(4, bladeBottom - bladeTop - 8));
        g.setColor(new Color(220, 214, 196, alpha));
        g.fillPolygon(
                new int[]{bladeX - 4, bladeX + 4, bladeX - 1},
                new int[]{bladeTop + 1, bladeTop + 1, bladeTop + 10},
                3
        );
        g.setColor(new Color(120, 10, 18, alpha));
        g.drawLine(bladeX, bladeTop + 10, bladeX, bladeBottom);
        g.drawLine(bladeX - 1, bladeTop + 14, bladeX - 4, bladeTop + 6);
    }

    private void drawBirthShape(Graphics2D g, OrganismKind kind, int x, int y, int size, int alpha, double progress) {
        Color glow = switch (kind) {
            case RABBIT -> new Color(171, 210, 129, alpha);
            case HUMAN -> new Color(152, 196, 234, alpha);
            case WOLF -> new Color(198, 205, 214, alpha);
            case BEAR -> new Color(204, 171, 128, alpha);
            case PLANT -> new Color(124, 196, 109, alpha);
        };
        int ring = Math.max(4, (int) (size * (0.95 + progress * 0.7)));
        int center = x + size / 2;
        int centerY = y + size / 2;
        g.setColor(glow);
        g.fillOval(center - size / 4, centerY - size / 4, Math.max(4, size / 2), Math.max(4, size / 2));
        g.setStroke(new BasicStroke(Math.max(2f, size / 8f)));
        g.drawOval(center - ring / 2, centerY - ring / 2, ring, ring);
        g.setColor(new Color(248, 244, 222, alpha));
        g.fillOval(center - 3, centerY - 3, 6, 6);
        g.fillOval(center - ring / 3, centerY - 2, 4, 4);
        g.fillOval(center + ring / 3 - 4, centerY - 2, 4, 4);
        g.fillOval(center - 2, centerY - ring / 3, 4, 4);
        g.fillOval(center - 2, centerY + ring / 3 - 4, 4, 4);
    }

    private void drawBanner(Graphics2D g, BoardMetrics metrics) {
        if (bannerText == null) {
            return;
        }
        double progress = (System.currentTimeMillis() - bannerStartedAt) / (double) bannerDuration();
        if (progress >= 1.0) {
            return;
        }

        int alpha = progress < 0.72 ? 225 : (int) (225 * (1.0 - progress) / 0.28);
        if (levelUpBanner) {
            drawCelebration(g, metrics, progress, alpha);
        }
        g.setFont(g.getFont().deriveFont(Font.BOLD, levelUpBanner ? 30f : 22f));
        FontMetrics fontMetrics = g.getFontMetrics();
        int width = fontMetrics.stringWidth(bannerText) + 42;
        int height = levelUpBanner ? 72 : 54;
        int x = metrics.offsetX + (metrics.width - width) / 2;
        int y = metrics.offsetY + (levelUpBanner ? metrics.height / 3 : 28);
        g.setColor(new Color(levelUpBanner ? 46 : 32, levelUpBanner ? 66 : 38, 32, alpha));
        g.fillRoundRect(x, y, width, height, 8, 8);
        g.setColor(new Color(239, 235, 211, alpha));
        g.drawString(bannerText, x + 21, y + (levelUpBanner ? 47 : 35));
    }

    private void drawCelebration(Graphics2D g, BoardMetrics metrics, double progress, int alpha) {
        g.setColor(new Color(236, 211, 88, Math.max(0, alpha / 4)));
        g.fillRect(metrics.offsetX, metrics.offsetY, metrics.width, metrics.height);
        Color[] colors = {
                new Color(240, 200, 72, alpha),
                new Color(91, 177, 103, alpha),
                new Color(89, 145, 204, alpha),
                new Color(220, 106, 90, alpha)
        };
        for (int i = 0; i < 42; i++) {
            int startX = Math.floorMod(i * 97, metrics.width);
            int x = metrics.offsetX + startX + (int) (Math.sin(progress * 8 + i) * 12);
            int y = metrics.offsetY + (int) ((progress * (metrics.height + 100) + i * 43) % (metrics.height + 100)) - 40;
            g.setColor(colors[i % colors.length]);
            g.fillRect(x, y, 5 + i % 4, 9 + i % 5);
        }
    }

    private Color effectColor(ToolMode mode, int alpha) {
        return switch (mode) {
            case RAIN -> new Color(78, 151, 214, alpha);
            case DROUGHT -> new Color(218, 135, 65, alpha);
            case COMPOST -> new Color(104, 157, 78, alpha);
            case PLANT -> new Color(73, 184, 89, alpha);
            case HUMAN -> new Color(183, 73, 84, alpha);
            case BEAR -> new Color(157, 106, 73, alpha);
            case RABBIT_FEMALE, RABBIT_MALE -> new Color(235, 211, 171, alpha);
            case WOLF -> new Color(157, 164, 177, alpha);
            case SANCTUARY -> new Color(232, 218, 112, alpha);
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
        boolean bannerActive = bannerText != null
                && now - bannerStartedAt <= bannerDuration();
        if (!bannerActive) {
            bannerText = null;
        }
        boolean birthsActive = hasActiveBirths(now);
        boolean deathsActive = hasActiveDeaths(now);
        if (toolEffects.isEmpty() && !bannerActive && !birthsActive && !deathsActive) {
            effectTimer.stop();
        }
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private int clampLevel(int value) {
        return Math.max(0, Math.min(7, value));
    }

    private boolean hasActiveDeaths(long now) {
        for (DeathEvent death : simulation.recentDeathEvents()) {
            if (now - death.createdAtMillis() <= DEATH_DURATION_MS) {
                return true;
            }
        }
        return false;
    }

    private boolean hasActiveBirths(long now) {
        for (BirthEvent birth : simulation.recentBirthEvents()) {
            if (now - birth.createdAtMillis() <= BIRTH_DURATION_MS) {
                return true;
            }
        }
        return false;
    }

    private long bannerDuration() {
        return levelUpBanner ? LEVEL_UP_DURATION_MS : BANNER_DURATION_MS;
    }

    private boolean positionsEqual(Position first, Position second) {
        return first == second || (first != null && first.equals(second));
    }

    private void repaintCell(Position position) {
        if (position == null) {
            return;
        }
        BoardMetrics metrics = boardMetrics(simulation.grid());
        int x = metrics.offsetX + position.x() * metrics.cellSize;
        int y = metrics.offsetY + position.y() * metrics.cellSize;
        repaint(x - 3, y - 3, metrics.cellSize + 6, metrics.cellSize + 6);
    }

    private static Color[][][] createSoilPalette() {
        Color[][][] palette = new Color[8][8][8];
        for (int moisture = 0; moisture < 8; moisture++) {
            for (int fertility = 0; fertility < 8; fertility++) {
                for (int heat = 0; heat < 8; heat++) {
                    double wet = moisture / 7.0;
                    double rich = fertility / 7.0;
                    double warm = heat / 7.0;
                    int red = (int) (83 + rich * 45 + warm * 29 - wet * 10);
                    int green = (int) (62 + rich * 54 + wet * 29 - warm * 6);
                    int blue = (int) (39 + wet * 61 + rich * 4);
                    palette[moisture][fertility][heat] = new Color(
                            Math.max(0, Math.min(255, red)),
                            Math.max(0, Math.min(255, green)),
                            Math.max(0, Math.min(255, blue))
                    );
                }
            }
        }
        return palette;
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
