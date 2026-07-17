package com.kimbopulus.weird.ui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

public final class CompletionVideoDialog {
    private static final String VIDEO_RESOURCE = "/com/kimbopulus/weird/media/game-complete.mp4";

    private CompletionVideoDialog() {
    }

    public static void show(Window owner) {
        Path video = extractVideo();
        if (video == null) {
            return;
        }

        JDialog dialog = new JDialog(owner, "Game Complete", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());

        JLabel message = new JLabel(
                "<html><div style='text-align:center;width:620px'>You're a warrior for passing my game!</div></html>",
                SwingConstants.CENTER
        );
        message.setOpaque(true);
        message.setBackground(new Color(28, 31, 27));
        message.setForeground(new Color(245, 228, 178));
        message.setFont(message.getFont().deriveFont(Font.BOLD, 25f));
        message.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
        dialog.add(message, BorderLayout.NORTH);

        JFXPanel videoPanel = new JFXPanel();
        videoPanel.setPreferredSize(new Dimension(960, 540));
        dialog.add(videoPanel, BorderLayout.CENTER);

        AtomicReference<MediaPlayer> playerReference = new AtomicReference<>();
        AtomicReference<javax.swing.Timer> watchdogReference = new AtomicReference<>();
        JButton pauseButton = new JButton("Pause");
        JButton restartButton = new JButton("Restart video");
        JButton closeButton = new JButton("Close");
        pauseButton.setFocusable(false);
        restartButton.setFocusable(false);
        closeButton.setFocusable(false);

        pauseButton.addActionListener(event -> Platform.runLater(() -> {
            MediaPlayer player = playerReference.get();
            if (player == null) {
                return;
            }
            if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                player.pause();
                SwingUtilities.invokeLater(() -> pauseButton.setText("Resume"));
            } else {
                player.play();
                SwingUtilities.invokeLater(() -> pauseButton.setText("Pause"));
            }
        }));
        restartButton.addActionListener(event -> Platform.runLater(() -> {
            MediaPlayer player = playerReference.get();
            if (player != null) {
                player.seek(Duration.ZERO);
                player.play();
                SwingUtilities.invokeLater(() -> pauseButton.setText("Pause"));
            }
        }));
        closeButton.addActionListener(event -> dialog.dispose());

        JPanel controls = new JPanel();
        controls.setBackground(new Color(239, 233, 218));
        controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        controls.add(pauseButton);
        controls.add(restartButton);
        controls.add(closeButton);
        dialog.add(controls, BorderLayout.SOUTH);

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent event) {
                javax.swing.Timer watchdog = watchdogReference.getAndSet(null);
                if (watchdog != null) {
                    watchdog.stop();
                }
                Platform.runLater(() -> {
                    MediaPlayer player = playerReference.getAndSet(null);
                    if (player != null) {
                        player.dispose();
                    }
                });
            }
        });

        Platform.runLater(() -> {
            Media media = new Media(video.toUri().toString());
            MediaPlayer player = new MediaPlayer(media);
            player.setVolume(0.5);
            player.setAutoPlay(true);
            playerReference.set(player);
            MediaView view = new MediaView(player);
            view.setPreserveRatio(true);
            view.setSmooth(true);

            StackPane root = new StackPane(view);
            root.setStyle("-fx-background-color: black;");
            view.fitWidthProperty().bind(root.widthProperty());
            view.fitHeightProperty().bind(root.heightProperty());
            videoPanel.setScene(new Scene(root, 960, 540, javafx.scene.paint.Color.BLACK));
            player.setOnEndOfMedia(() -> SwingUtilities.invokeLater(dialog::dispose));
            player.setOnError(() -> SwingUtilities.invokeLater(dialog::dispose));
            AtomicInteger startupRescues = new AtomicInteger();
            player.setOnStalled(() -> rescuePlayback(player, startupRescues));
            player.setOnReady(() -> {
                startupRescues.set(0);
                player.seek(Duration.millis(900));
                player.play();
            });
            player.play();
            SwingUtilities.invokeLater(() -> startWatchdog(playerReference, watchdogReference));
        });

        dialog.pack();
        dialog.setMinimumSize(new Dimension(720, 480));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static Path extractVideo() {
        try (InputStream source = CompletionVideoDialog.class.getResourceAsStream(VIDEO_RESOURCE)) {
            if (source == null) {
                return null;
            }
            Path directory = Path.of(System.getProperty("java.io.tmpdir"), "Weird");
            Files.createDirectories(directory);
            Path video = Files.createTempFile(directory, "game-complete-", ".mp4");
            Files.copy(source, video, StandardCopyOption.REPLACE_EXISTING);
            video.toFile().deleteOnExit();
            return video;
        } catch (Exception exception) {
            return null;
        }
    }

    private static void startWatchdog(
            AtomicReference<MediaPlayer> playerReference,
            AtomicReference<javax.swing.Timer> watchdogReference
    ) {
        AtomicReference<Duration> lastTime = new AtomicReference<>(Duration.UNKNOWN);
        AtomicInteger stalledChecks = new AtomicInteger();
        AtomicInteger rescueAttempts = new AtomicInteger();
        javax.swing.Timer watchdog = new javax.swing.Timer(1500, event -> Platform.runLater(() -> {
            MediaPlayer player = playerReference.get();
            if (player == null) {
                return;
            }
            MediaPlayer.Status status = player.getStatus();
            if (status == MediaPlayer.Status.STALLED || status == MediaPlayer.Status.READY) {
                rescuePlayback(player, rescueAttempts);
                return;
            }
            if (status != MediaPlayer.Status.PLAYING) {
                stalledChecks.set(0);
                return;
            }
            Duration current = player.getCurrentTime();
            Duration previous = lastTime.getAndSet(current);
            if (previous == null || previous.isUnknown()) {
                return;
            }
            if (Math.abs(current.toMillis() - previous.toMillis()) < 80.0) {
                if (stalledChecks.incrementAndGet() >= 2) {
                    rescuePlayback(player, rescueAttempts);
                    stalledChecks.set(0);
                }
            } else {
                stalledChecks.set(0);
                rescueAttempts.set(0);
            }
        }));
        watchdog.setRepeats(true);
        watchdog.start();
        watchdogReference.set(watchdog);
    }

    private static void rescuePlayback(MediaPlayer player, AtomicInteger attempts) {
        int attempt = attempts.getAndIncrement();
        double offsetMillis = Math.min(6500.0, 900.0 + attempt * 900.0);
        player.stop();
        player.seek(Duration.millis(offsetMillis));
        player.play();
    }
}
