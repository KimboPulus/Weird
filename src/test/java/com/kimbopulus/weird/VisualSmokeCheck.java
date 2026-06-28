package com.kimbopulus.weird;

import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Rabbit;
import com.kimbopulus.weird.sim.RabbitSex;
import com.kimbopulus.weird.progression.ProgressionProfile;
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
import java.util.HashSet;
import java.util.Set;

public final class VisualSmokeCheck {
    private VisualSmokeCheck() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        File output = new File(args.length == 0 ? "out/visual-check.png" : args[0]);
        File failureOutput = new File(output.getParentFile(), "failure-check.png");
        File levelOutput = new File(output.getParentFile(), "level-up-check.png");
        File birthOutput = new File(output.getParentFile(), "birth-check.png");
        SwingUtilities.invokeAndWait(() -> {
            render(output);
            renderFailure(failureOutput);
            renderLevelUp(levelOutput);
            renderBirth(birthOutput);
        });
        System.out.println("Visual check saved " + output.getAbsolutePath());
        System.out.println("Failure check saved " + failureOutput.getAbsolutePath());
        System.out.println("Level-up check saved " + levelOutput.getAbsolutePath());
        System.out.println("Birth check saved " + birthOutput.getAbsolutePath());
    }

    private static void render(File output) {
        try {
            Simulation simulation = new Simulation(38, 26, 7L);
            simulation.seedPlants(140);
            simulation.seedRabbits(32);
            simulation.seedWolves(3);
            simulation.seedHumans(4);

            TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
            for (int i = 0; i < 80; i++) {
                simulation.tick();
                training.update(simulation);
            }
            simulation.clearDeathEvents();

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

            TrainingSession training = new TrainingSession(ProgressionProfile.inMemory());
            for (int i = 0; i < 14; i++) {
                simulation.tick();
                removeSpecies(simulation, OrganismKind.PLANT);
                training.update(simulation);
            }
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

    private static void renderPanels(
            Simulation simulation,
            TrainingSession training,
            File output,
            String levelBanner
    ) throws Exception {
        JPanel root = new JPanel(new BorderLayout());
        TerrariumPanel board = new TerrariumPanel(simulation);
        if (levelBanner != null) {
            board.showLevelUp(levelBanner);
        }
        TrainingPanel panel = new TrainingPanel(simulation, training);
        root.add(board, BorderLayout.CENTER);
        root.add(panel, BorderLayout.EAST);
        root.setSize(1280, 720);
        layoutTree(root);

        BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        root.paint(g);
        g.dispose();

        require(hasEnoughColorVariation(image), "Rendered image looks blank.");
        require(hasReadableBoardArea(image), "Board area did not render expected dark background.");

        output.getParentFile().mkdirs();
        ImageIO.write(image, "png", output);
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
            for (int x = 80; x < image.getWidth() - 360; x += 8) {
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
