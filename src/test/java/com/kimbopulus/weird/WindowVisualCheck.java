package com.kimbopulus.weird;

import com.kimbopulus.weird.ui.TerrariumFrame;
import com.kimbopulus.weird.ui.ShopDialog;
import com.kimbopulus.weird.progression.ProgressionProfile;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

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
        AtomicReference<JDialog> shopRef = new AtomicReference<>();
        SwingUtilities.invokeLater(() -> {
            ProgressionProfile profile = ProgressionProfile.inMemory();
            profile.addFocusXp(120);
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
        BufferedImage shopImage = new Robot().createScreenCapture(shop.getBounds());
        ImageIO.write(shopImage, "png", shopOutput);
        SwingUtilities.invokeAndWait(shop::dispose);

        SwingUtilities.invokeAndWait(() -> {
            frame.setAlwaysOnTop(false);
            frame.dispose();
        });
        System.out.println("Window check saved " + output.getAbsolutePath());
        System.out.println("Shop check saved " + shopOutput.getAbsolutePath());
        System.exit(0);
    }
}
