package com.kimbopulus.weird;

import com.kimbopulus.weird.sim.Simulation;
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
        SwingUtilities.invokeAndWait(() -> render(output));
        System.out.println("Visual check saved " + output.getAbsolutePath());
    }

    private static void render(File output) {
        try {
            Simulation simulation = new Simulation(48, 32, 7L);
            simulation.seedPlants(220);
            simulation.seedRabbits(48);
            simulation.seedWolves(4);

            TrainingSession training = new TrainingSession();
            for (int i = 0; i < 80; i++) {
                simulation.tick();
                training.update(simulation);
            }

            JPanel root = new JPanel(new BorderLayout());
            TerrariumPanel board = new TerrariumPanel(simulation);
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
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
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
