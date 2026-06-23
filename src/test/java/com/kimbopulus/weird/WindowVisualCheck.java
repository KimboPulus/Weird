package com.kimbopulus.weird;

import com.kimbopulus.weird.ui.TerrariumFrame;

import javax.imageio.ImageIO;
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
            frame.setVisible(true);
            frameRef.set(frame);
        });

        Thread.sleep(1200);
        TerrariumFrame frame = frameRef.get();
        Rectangle bounds = frame.getBounds();
        BufferedImage image = new Robot().createScreenCapture(bounds);
        output.getParentFile().mkdirs();
        ImageIO.write(image, "png", output);

        SwingUtilities.invokeAndWait(frame::dispose);
        System.out.println("Window check saved " + output.getAbsolutePath());
        System.exit(0);
    }
}
