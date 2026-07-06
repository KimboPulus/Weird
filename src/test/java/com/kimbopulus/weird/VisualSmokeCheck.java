package com.kimbopulus.weird;

import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Plant;
import com.kimbopulus.weird.sim.Rabbit;
import com.kimbopulus.weird.sim.RabbitSex;
import com.kimbopulus.weird.sim.Human;
import com.kimbopulus.weird.sim.Wolf;
import com.kimbopulus.weird.sim.Bear;
import com.kimbopulus.weird.progression.ProgressionProfile;
import com.kimbopulus.weird.training.TrainingLevel;
import com.kimbopulus.weird.training.TrainingSession;
import com.kimbopulus.weird.ui.TerrariumPanel;
import com.kimbopulus.weird.ui.TrainingPanel;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class VisualSmokeCheck {
    private VisualSmokeCheck() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        File output = new File(args.length == 0 ? "out/visual-check.png" : args[0]);
        File failureOutput = new File(output.getParentFile(), "failure-check.png");
        File completeOutput = new File(output.getParentFile(), "complete-check.png");
        File gameCompleteOutput = new File(output.getParentFile(), "game-complete-check.png");
        File levelOutput = new File(output.getParentFile(), "level-up-check.png");
        File birthOutput = new File(output.getParentFile(), "birth-check.png");
        File sexOutput = new File(output.getParentFile(), "rabbit-sex-check.png");
        File animalPackOutput = new File(output.getParentFile(), "animal-pack-check.png");
        File warningOutput = new File(output.getParentFile(), "warning-check.png");
        File popupOutput = new File(output.getParentFile(), "mechanic-popup-check.png");
        File levelScreensDir = new File(output.getParentFile(), "levels");
        File levelOverview = new File(output.getParentFile(), "levels-overview.png");
        SwingUtilities.invokeAndWait(() -> {
            render(output);
            renderFailure(failureOutput);
            renderCompleteOverlay(completeOutput);
            renderGameComplete(gameCompleteOutput);
            renderLevelUp(levelOutput);
            renderBirth(birthOutput);
            renderRabbitSexes(sexOutput);
            renderAnimalPack(animalPackOutput);
            renderWarning(warningOutput);
            renderMechanicPopups(popupOutput);
            renderLevelScreens(levelScreensDir);
            renderLevelOverview(levelOverview);
        });
        System.out.println("Visual check saved " + output.getAbsolutePath());
        System.out.println("Failure check saved " + failureOutput.getAbsolutePath());
        System.out.println("Complete check saved " + completeOutput.getAbsolutePath());
        System.out.println("Game complete check saved " + gameCompleteOutput.getAbsolutePath());
        System.out.println("Level-up check saved " + levelOutput.getAbsolutePath());
        System.out.println("Birth check saved " + birthOutput.getAbsolutePath());
        System.out.println("Rabbit sex check saved " + sexOutput.getAbsolutePath());
        System.out.println("Animal pack check saved " + animalPackOutput.getAbsolutePath());
        System.out.println("Warning check saved " + warningOutput.getAbsolutePath());
        System.out.println("Mechanic popup check saved " + popupOutput.getAbsolutePath());
        System.out.println("Level screenshots saved " + levelScreensDir.getAbsolutePath());
        System.out.println("Level overview saved " + levelOverview.getAbsolutePath());
    }

    private static void render(File output) {
        try {
            Simulation simulation = balancedLevelSimulation(7L);
            TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
            renderPanels(simulation, training, output);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void renderFailure(File output) {
        try {
            Simulation simulation = new Simulation(38, 26, 17L);
            simulation.seedPlants(140);
            simulation.seedRabbits(32);
            simulation.seedWolves(3);
            simulation.seedHumans(4);
            simulation.clearDeathEvents();
            for (int y = 0; y < simulation.grid().height(); y++) {
                for (int x = 0; x < simulation.grid().width(); x++) {
                    if (simulation.organismAt(x, y) != null
                            && simulation.organismAt(x, y).kind() == OrganismKind.WOLF) {
                        simulation.removeOrganism(new Position(x, y));
                    }
                }
            }
            removeSpecies(simulation, OrganismKind.PLANT);
            removeSpecies(simulation, OrganismKind.RABBIT);
            removeSpecies(simulation, OrganismKind.HUMAN);

            AtomicLong now = new AtomicLong();
            TrainingSession training = new TrainingSession(ProgressionProfile.inMemory(), now::get);
            for (int i = 0; i < 2; i++) {
                simulation.tick();
                removeSpecies(simulation, OrganismKind.PLANT);
                training.update(simulation);
            }
            now.addAndGet(60_000L);
            training.update(simulation);
            require(training.levelFailed(), "Failure scenario did not lose the level.");
            renderPanels(simulation, training, output);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void renderPanels(Simulation simulation, TrainingSession training, File output) throws Exception {
        renderPanels(simulation, training, output, null);
    }

    private static void renderLevelUp(File output) {
        try {
            Simulation simulation = new Simulation(38, 26, 27L);
            simulation.seedPlants(140);
            simulation.seedRabbits(32);
            simulation.seedWolves(3);
            simulation.seedHumans(4);
            TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
            for (int i = 0; i < 8; i++) {
                simulation.tick();
                training.update(simulation);
            }
            renderPanels(simulation, training, output, "LEVEL 2  GREEN RHYTHM");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void renderCompleteOverlay(File output) {
        try {
            Simulation simulation = balancedLevelSimulation(23L);
            TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
            renderPanels(simulation, training, output, null, board -> board.showLevelCompleteOverlay(
                    "Level complete",
                    "+45 tokens. Click Next Level on the right to continue."
            ));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void renderGameComplete(File output) {
        try {
            Simulation simulation = balancedLevelSimulation(29L);
            TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
            forceLevel(training, TrainingLevel.FLEX_SHIFT);
            setField(training, "levelComplete", true);
            renderPanels(simulation, training, output, null, board -> board.showLevelCompleteOverlay(
                    "You're a warrior for passing my game!",
                    "Final level complete. Use Restart Game on the right to play again."
            ));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void renderBirth(File output) {
        try {
            Simulation simulation = new Simulation(38, 26, 37L);
            simulation.seedPlants(120);
            Position center = new Position(14, 12);
            simulation.placeOrganism(new Position(13, 12), new Rabbit(RabbitSex.FEMALE));
            simulation.placeOrganism(new Position(15, 12), new Rabbit(RabbitSex.MALE));
            simulation.recordBirth(OrganismKind.RABBIT, center);
            TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
            renderPanels(simulation, training, output);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void renderRabbitSexes(File output) {
        try {
            Simulation simulation = new Simulation(38, 26, 47L);
            simulation.seedPlants(100);
            simulation.placeOrganism(new Position(12, 12), new Rabbit(RabbitSex.FEMALE));
            simulation.placeOrganism(new Position(16, 12), new Rabbit(RabbitSex.MALE));
            TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
            renderPanels(simulation, training, output);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void renderAnimalPack(File output) {
        try {
            Simulation simulation = new Simulation(38, 26, 57L);
            simulation.seedPlants(70);
            simulation.placeOrganism(new Position(10, 10), new com.kimbopulus.weird.sim.Human());
            simulation.placeOrganism(new Position(24, 12), new com.kimbopulus.weird.sim.Bear());
            simulation.placeOrganism(new Position(16, 8), new Rabbit(RabbitSex.FEMALE));
            simulation.placeOrganism(new Position(18, 8), new Rabbit(RabbitSex.MALE));
            simulation.placeOrganism(new Position(21, 15), new com.kimbopulus.weird.sim.Wolf());
            TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
            renderPanels(simulation, training, output);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void renderWarning(File output) {
        try {
            Simulation simulation = balancedLevelSimulation(67L);
            removeSpecies(simulation, OrganismKind.WOLF);
            AtomicLong now = new AtomicLong();
            TrainingSession training = new TrainingSession(ProgressionProfile.inMemory(), now::get);
            forceLevel(training, TrainingLevel.PREDATOR_CHECK);
            training.update(simulation);
            require(training.dangerWarning() != null, "Warning scenario did not trigger danger text.");
            renderPanels(simulation, training, output);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void renderMechanicPopups(File output) {
        try {
            Simulation simulation = balancedLevelSimulation(77L);
            TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
            renderPanels(simulation, training, output, null, board -> {
                board.showMechanicPopup("rain-tip", "Rain hit drought soil",
                        "That combo cools the whole board by 2.5 C.");
                board.showMechanicPopup("drought-tip", "Drought hit dry soil",
                        "That combo heats the whole board by 2 C.");
                board.showMechanicPopup("rabbit-tip", "Rabbits can starve",
                        "No plant in time means death.");
            });
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void renderLevelScreens(File directory) {
        try {
            directory.mkdirs();
            for (TrainingLevel level : TrainingLevel.values()) {
                File output = new File(directory, String.format("level-%d-%s.png", level.ordinal() + 1, slug(level.title())));
                renderLevelScreen(level, output);
            }
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void renderLevelOverview(File output) {
        try {
            TrainingLevel[] levels = TrainingLevel.values();
            int columns = 2;
            int rows = (levels.length + columns - 1) / columns;
            int tileWidth = 720;
            int tileHeight = 410;
            BufferedImage sheet = new BufferedImage(columns * tileWidth, rows * tileHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sheet.createGraphics();
            g.setColor(new Color(20, 22, 19));
            g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            for (int i = 0; i < levels.length; i++) {
                TrainingLevel level = levels[i];
                Simulation simulation = balancedLevelSimulation(101L + level.ordinal() * 17L);
                TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
                forceLevel(training, level);
                BufferedImage panel = renderPanelsImage(simulation, training, null, null);
                int x = (i % columns) * tileWidth;
                int y = (i / columns) * tileHeight;
                g.drawImage(panel, x, y, tileWidth, tileHeight, null);
            }
            g.dispose();
            output.getParentFile().mkdirs();
            ImageIO.write(sheet, "png", output);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void renderLevelScreen(TrainingLevel level, File output) throws Exception {
        Simulation simulation = balancedLevelSimulation(101L + level.ordinal() * 17L);
        TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
        forceLevel(training, level);
        renderPanels(simulation, training, output);
    }

    private static Simulation balancedLevelSimulation(long seed) {
        Simulation simulation = new Simulation(38, 26, seed);
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                double moisture = clamp(0.52 + ((x % 7) - 3) * 0.018 + ((y % 5) - 2) * 0.012, 0.36, 0.68);
                double fertility = clamp(0.55 + ((x % 6) - 2.5) * 0.022 + ((y % 4) - 1.5) * 0.018, 0.34, 0.72);
                double temperature = clamp(21.0 + ((x % 5) - 2) * 0.65 + ((y % 6) - 2.5) * 0.28, 17.0, 25.5);
                simulation.grid().cellAt(x, y).reset(moisture, temperature, fertility);
            }
        }

        List<Position> positions = new ArrayList<>();
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                positions.add(new Position(x, y));
            }
        }
        Collections.shuffle(positions, new java.util.Random(seed));

        place(simulation, positions, 320, Plant::new);
        place(simulation, positions, 24, () -> new Rabbit(RabbitSex.FEMALE));
        place(simulation, positions, 5, Wolf::new);
        place(simulation, positions, 4, Human::new);
        place(simulation, positions, 1, Bear::new);
        return simulation;
    }

    private static void place(Simulation simulation, List<Position> positions, int count, Supplier<? extends com.kimbopulus.weird.sim.Organism> factory) {
        for (int i = 0; i < count && !positions.isEmpty(); i++) {
            simulation.placeOrganism(positions.remove(0), factory.get());
        }
    }

    private static void forceLevel(TrainingSession training, TrainingLevel level) throws ReflectiveOperationException {
        setField(training, "level", level);
        setField(training, "levelComplete", false);
        setField(training, "levelFailed", false);
        setField(training, "stableTicks", 0);
        setField(training, "levelProgress", 0);
        setField(training, "lastLevelReward", 0);
        setField(training, "dangerTicks", 0);
        setField(training, "dangerReason", null);
        setField(training, "dangerDetail", null);
        setField(training, "dangerStartedAtMillis", -1L);
        setField(training, "failureDetail", null);
        setField(training, "gardenerActions", 0);
        setField(training, "feedback", "Hold the ecosystem steady.");
    }

    private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setField(Object target, String name, int value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setInt(target, value);
    }

    private static String slug(String text) {
        return text.toLowerCase().replace(' ', '-');
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void renderPanels(
            Simulation simulation,
            TrainingSession training,
            File output,
            String levelBanner
    ) throws Exception {
        renderPanels(simulation, training, output, levelBanner, null);
    }

    private static void renderPanels(
            Simulation simulation,
            TrainingSession training,
            File output,
            String levelBanner,
            Consumer<TerrariumPanel> boardSetup
    ) throws Exception {
        BufferedImage image = renderPanelsImage(simulation, training, levelBanner, boardSetup);
        output.getParentFile().mkdirs();
        ImageIO.write(image, "png", output);
    }

    private static BufferedImage renderPanelsImage(
            Simulation simulation,
            TrainingSession training,
            String levelBanner,
            Consumer<TerrariumPanel> boardSetup
    ) throws Exception {
        JPanel root = new JPanel(new BorderLayout());
        TerrariumPanel board = new TerrariumPanel(simulation);
        if (levelBanner != null) {
            board.showLevelUp(levelBanner);
        }
        if (boardSetup != null) {
            boardSetup.accept(board);
        }
        TrainingPanel panel = new TrainingPanel(simulation, training);
        root.add(board, BorderLayout.CENTER);
        root.add(panel, BorderLayout.EAST);
        root.setSize(1440, 820);
        layoutTree(root);

        BufferedImage image = new BufferedImage(1440, 820, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        root.paint(g);
        g.dispose();

        require(hasEnoughColorVariation(image), "Rendered image looks blank.");
        require(hasReadableBoardArea(image), "Board area did not render expected dark background.");
        return image;
    }

    private static void removeSpecies(Simulation simulation, OrganismKind kind) {
        for (int y = 0; y < simulation.grid().height(); y++) {
            for (int x = 0; x < simulation.grid().width(); x++) {
                if (simulation.organismAt(x, y) != null && simulation.organismAt(x, y).kind() == kind) {
                    simulation.removeOrganism(new Position(x, y));
                }
            }
        }
    }

    private static boolean hasEnoughColorVariation(BufferedImage image) {
        Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y += 8) {
            for (int x = 0; x < image.getWidth(); x += 8) {
                colors.add(image.getRGB(x, y));
                if (colors.size() > 48) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container child) {
                layoutTree(child);
            }
        }
    }

    private static boolean hasReadableBoardArea(BufferedImage image) {
        int darkPixels = 0;
        int sampled = 0;
        for (int y = 80; y < image.getHeight() - 80; y += 8) {
            for (int x = 80; x < image.getWidth() - 440; x += 8) {
                Color color = new Color(image.getRGB(x, y), true);
                if (color.getRed() < 150 && color.getGreen() < 150 && color.getBlue() < 150) {
                    darkPixels++;
                }
                sampled++;
            }
        }
        return sampled > 0 && darkPixels > sampled / 3;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
