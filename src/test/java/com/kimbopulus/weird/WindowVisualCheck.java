package com.kimbopulus.weird;

import com.kimbopulus.weird.ui.TerrariumFrame;
import com.kimbopulus.weird.ui.ShopDialog;
import com.kimbopulus.weird.ui.AudioSettingsDialog;
import com.kimbopulus.weird.ui.CompletionVideoDialog;
import com.kimbopulus.weird.progression.ProgressionProfile;
import com.kimbopulus.weird.settings.GameSettings;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Arrays;

public final class WindowVisualCheck {
    private WindowVisualCheck() {
    }

    public static void main(String[] args) throws Exception {
        File output = new File(args.length == 0 ? "out/window-check.png" : args[0]);
        AtomicReference<TerrariumFrame> frameRef = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            TerrariumFrame frame = new TerrariumFrame();
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            frame.toFront();
            frame.requestFocus();
            frameRef.set(frame);
        });

        Thread.sleep(1200);
        TerrariumFrame frame = frameRef.get();
        Rectangle bounds = frame.getBounds();
        BufferedImage image = new Robot().createScreenCapture(bounds);
        output.getParentFile().mkdirs();
        ImageIO.write(image, "png", output);

        File shopOutput = new File(output.getParentFile(), "shop-check.png");
        JButton realShopButton = findButton(frame, "Shop");
        if (realShopButton == null) {
            throw new IllegalStateException("Real Shop button was not found.");
        }
        SwingUtilities.invokeLater(realShopButton::doClick);
        Thread.sleep(800);
        JDialog realShop = visibleDialog("Terrarium Shop");
        if (realShop == null) {
            throw new IllegalStateException("Real Shop button did not open the shop.");
        }
        BufferedImage realShopImage = new Robot().createScreenCapture(realShop.getBounds());
        ImageIO.write(realShopImage, "png", shopOutput);
        SwingUtilities.invokeAndWait(realShop::dispose);

        File shopPurchaseOutput = new File(output.getParentFile(), "shop-purchase-check.png");
        AtomicReference<JDialog> shopRef = new AtomicReference<>();
        SwingUtilities.invokeLater(() -> {
            ProgressionProfile profile = ProgressionProfile.inMemory();
            profile.addFocusXp(600);
            JDialog shop = ShopDialog.createForVisualCheck(frame, profile);
            shop.setAlwaysOnTop(true);
            shopRef.set(shop);
            shop.setVisible(true);
        });
        Thread.sleep(600);
        JDialog shop = shopRef.get();
        if (shop == null) {
            throw new IllegalStateException("Shop window did not open.");
        }
        JButton buyButton = findEnabledTokenButton(shop);
        if (buyButton == null) {
            throw new IllegalStateException("Shop buy button was not enabled.");
        }
        SwingUtilities.invokeAndWait(buyButton::doClick);
        Thread.sleep(200);
        BufferedImage shopImage = new Robot().createScreenCapture(shop.getBounds());
        ImageIO.write(shopImage, "png", shopPurchaseOutput);
        SwingUtilities.invokeAndWait(shop::dispose);

        File audioOutput = new File(output.getParentFile(), "audio-settings-check.png");
        AtomicReference<JDialog> audioRef = new AtomicReference<>();
        SwingUtilities.invokeLater(() -> {
            JDialog audioDialog = AudioSettingsDialog.createForVisualCheck(frame, GameSettings.inMemory());
            audioDialog.setAlwaysOnTop(true);
            audioRef.set(audioDialog);
            audioDialog.setVisible(true);
        });
        Thread.sleep(500);
        JDialog audioDialog = audioRef.get();
        if (audioDialog == null) {
            throw new IllegalStateException("Audio settings window did not open.");
        }
        BufferedImage audioImage = new Robot().createScreenCapture(audioDialog.getBounds());
        ImageIO.write(audioImage, "png", audioOutput);
        SwingUtilities.invokeAndWait(audioDialog::dispose);

        File videoOutput = new File(output.getParentFile(), "completion-video-check.png");
        File videoMotionOutput = new File(output.getParentFile(), "completion-video-motion-check.png");
        SwingUtilities.invokeLater(() -> CompletionVideoDialog.show(frame));
        Thread.sleep(2200);
        JDialog videoDialog = Arrays.stream(Window.getWindows())
                .filter(window -> window instanceof JDialog)
                .map(window -> (JDialog) window)
                .filter(window -> "Game Complete".equals(window.getTitle()) && window.isShowing())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Completion video window did not open."));
        BufferedImage videoImage = new Robot().createScreenCapture(videoDialog.getBounds());
        ImageIO.write(videoImage, "png", videoOutput);
        Thread.sleep(2500);
        BufferedImage videoMotionImage = new Robot().createScreenCapture(videoDialog.getBounds());
        ImageIO.write(videoMotionImage, "png", videoMotionOutput);
        if (frameDifference(videoImage, videoMotionImage) < 2.0) {
            throw new IllegalStateException("Completion video appears frozen.");
        }
        SwingUtilities.invokeAndWait(videoDialog::dispose);

        SwingUtilities.invokeAndWait(() -> {
            frame.setAlwaysOnTop(false);
            frame.dispose();
        });
        System.out.println("Window check saved " + output.getAbsolutePath());
        System.out.println("Shop check saved " + shopOutput.getAbsolutePath());
        System.out.println("Shop purchase check saved " + shopPurchaseOutput.getAbsolutePath());
        System.out.println("Audio settings check saved " + audioOutput.getAbsolutePath());
        System.out.println("Completion video check saved " + videoOutput.getAbsolutePath());
        System.out.println("Completion video motion check saved " + videoMotionOutput.getAbsolutePath());
        System.exit(0);
    }

    private static JDialog visibleDialog(String title) {
        return Arrays.stream(Window.getWindows())
                .filter(window -> window instanceof JDialog)
                .map(window -> (JDialog) window)
                .filter(window -> title.equals(window.getTitle()) && window.isShowing())
                .findFirst()
                .orElse(null);
    }

    private static JButton findButton(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container child) {
                JButton found = findButton(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton findEnabledTokenButton(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button
                    && button.isEnabled()
                    && button.getText() != null
                    && button.getText().contains("tokens")) {
                return button;
            }
            if (component instanceof Container child) {
                JButton found = findEnabledTokenButton(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static double frameDifference(BufferedImage first, BufferedImage second) {
        int width = Math.min(first.getWidth(), second.getWidth());
        int height = Math.min(first.getHeight(), second.getHeight());
        long total = 0;
        long samples = 0;
        for (int y = height / 8; y < height * 7 / 8; y += 4) {
            for (int x = width / 8; x < width * 7 / 8; x += 4) {
                int a = first.getRGB(x, y);
                int b = second.getRGB(x, y);
                total += Math.abs(((a >>> 16) & 0xff) - ((b >>> 16) & 0xff));
                total += Math.abs(((a >>> 8) & 0xff) - ((b >>> 8) & 0xff));
                total += Math.abs((a & 0xff) - (b & 0xff));
                samples += 3;
            }
        }
        return samples == 0 ? 0.0 : total / (double) samples;
    }
}
