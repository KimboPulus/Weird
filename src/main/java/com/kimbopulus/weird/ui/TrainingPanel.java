package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.PopulationSnapshot;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.training.TrainingSession;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

public final class TrainingPanel extends JPanel {
    private static final Color BACKGROUND = new Color(247, 243, 232);
    private static final Color TEXT = new Color(38, 42, 38);
    private static final Color MUTED = new Color(92, 96, 88);

    private final Simulation simulation;
    private final TrainingSession training;
    private final Runnable onProgressionChanged;
    private final Runnable onRestartLevel;
    private final Runnable onLevelAdvanced;
    private final JLabel titleLabel = new JLabel("OBJECTIVE");
    private final JLabel levelLabel = new JLabel();
    private final JLabel scoreLabel = new JLabel();
    private final JLabel goalLabel = new JLabel();
    private final JLabel balanceLabel = new JLabel();
    private final JLabel countsLabel = new JLabel();
    private final JLabel detailLabel = new JLabel();
    private final JLabel climateLabel = new JLabel();
    private final JLabel eventLabel = new JLabel();
    private final JLabel warningLabel = new JLabel();
    private final JLabel feedbackLabel = new JLabel();
    private final JProgressBar levelProgress = new JProgressBar();
    private final JButton nextLevelButton = new JButton("Next Level");
    private final JButton restartLevelButton = new JButton("Restart Level");
    private final JPanel levelActionsPanel = new JPanel(new GridLayout(0, 1, 0, 5));
    private final JPanel helpPanel;

    public TrainingPanel(Simulation simulation, TrainingSession training) {
        this(simulation, training, () -> {
        }, () -> {
        }, () -> {
        });
    }

    public TrainingPanel(Simulation simulation, TrainingSession training, Runnable onProgressionChanged) {
        this(simulation, training, onProgressionChanged, () -> {
        }, () -> {
        });
    }

    public TrainingPanel(
            Simulation simulation,
            TrainingSession training,
            Runnable onProgressionChanged,
            Runnable onRestartLevel,
            Runnable onLevelAdvanced
    ) {
        this.simulation = simulation;
        this.training = training;
        this.onProgressionChanged = onProgressionChanged;
        this.onRestartLevel = onRestartLevel;
        this.onLevelAdvanced = onLevelAdvanced;
        this.helpPanel = createControlsPanel();

        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(432, 700));
        setBorder(BorderFactory.createEmptyBorder(14, 18, 12, 18));
        setLayout(new BorderLayout(0, 9));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 25f));
        titleLabel.setForeground(TEXT);
        top.add(titleLabel);
        top.add(Box.createVerticalStrut(6));

        configureLabel(levelLabel, Font.BOLD, 18f, new Color(75, 101, 67));
        configureLabel(scoreLabel, Font.BOLD, 18f, TEXT);
        configureLabel(goalLabel, Font.BOLD, 15f, TEXT);
        configureLabel(balanceLabel, Font.BOLD, 17f, TEXT);
        configureLabel(countsLabel, Font.BOLD, 18f, TEXT);
        configureLabel(detailLabel, Font.PLAIN, 14f, MUTED);
        configureLabel(climateLabel, Font.PLAIN, 14f, MUTED);
        configureLabel(eventLabel, Font.BOLD, 14f, new Color(126, 78, 56));
        configureLabel(warningLabel, Font.BOLD, 20f, Color.WHITE);
        configureLabel(feedbackLabel, Font.PLAIN, 14f, MUTED);

        warningLabel.setOpaque(true);
        warningLabel.setBackground(new Color(176, 57, 45));
        warningLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        warningLabel.setAlignmentX(LEFT_ALIGNMENT);
        warningLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        warningLabel.setVisible(false);

        levelProgress.setStringPainted(true);
        levelProgress.setForeground(new Color(77, 143, 85));
        levelProgress.setBackground(new Color(222, 216, 199));
        levelProgress.setBorderPainted(false);
        levelProgress.setPreferredSize(new Dimension(330, 20));

        top.add(levelLabel);
        top.add(Box.createVerticalStrut(4));
        top.add(goalLabel);
        top.add(Box.createVerticalStrut(6));
        top.add(levelProgress);
        top.add(Box.createVerticalStrut(8));
        top.add(createEconomyRow());
        top.add(Box.createVerticalStrut(10));
        top.add(balanceLabel);
        top.add(Box.createVerticalStrut(4));
        top.add(countsLabel);
        top.add(Box.createVerticalStrut(4));
        top.add(climateLabel);
        top.add(Box.createVerticalStrut(2));
        top.add(eventLabel);
        top.add(Box.createVerticalStrut(8));
        top.add(warningLabel);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(createDetailPanel(), BorderLayout.NORTH);
        center.add(helpPanel, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        add(feedbackLabel, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        PopulationSnapshot snapshot = simulation.currentSnapshot();
        int boardCells = simulation.grid().width() * simulation.grid().height();

        scoreLabel.setText("Run " + training.score() + "   Total " + training.progression().totalScore()
                + "   Tokens " + training.progression().tokens());
        levelLabel.setText("Level " + training.levelNumber() + "/" + training.levelCount()
                + "   " + training.levelTitle());
        goalLabel.setText(html(training.objective(), 352));
        levelProgress.setMaximum(training.drillTarget());
        levelProgress.setValue(training.drillProgress());
        levelProgress.setString(training.drillProgress() + " / " + training.drillTarget());
        levelProgress.setForeground(training.levelComplete()
                ? new Color(189, 137, 56)
                : new Color(77, 143, 85));
        balanceLabel.setText(training.objectiveStatus(snapshot));
        balanceLabel.setForeground(training.objectiveOnTrack(snapshot) ? new Color(75, 101, 67) : new Color(126, 78, 56));
        countsLabel.setText(html(training.currentSummary(snapshot), 336));
        detailLabel.setText(html(training.balanceGuide(snapshot, boardCells), 336));
        climateLabel.setText(String.format(
                "Water %.0f%%   Soil %.0f%%   %.1f C",
                snapshot.averageMoisture() * 100.0,
                snapshot.averageFertility() * 100.0,
                snapshot.averageTemperature()
        ));
        eventLabel.setText("Weather: " + simulation.currentEvent().title());
        String warning = training.levelFailed()
                ? formatFailureWarning(training.failureReason())
                : formatDangerWarning(training.dangerDetail(), training.dangerCountdownLabel());
        warningLabel.setText(warning == null ? "" : warning);
        warningLabel.setVisible(warning != null);

        nextLevelButton.setVisible(training.levelComplete());
        restartLevelButton.setVisible(training.levelFailed());
        levelActionsPanel.setVisible(training.levelComplete() || training.levelFailed());
        helpPanel.setVisible(!training.levelComplete() && !training.levelFailed() && warning == null);
        feedbackLabel.setText(html(training.feedback(), 336));
        feedbackLabel.setForeground(MUTED);
    }

    private JPanel createDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 7));
        panel.setOpaque(false);
        panel.add(detailLabel, BorderLayout.CENTER);
        panel.add(levelActionsPanel, BorderLayout.SOUTH);

        nextLevelButton.setFocusable(false);
        nextLevelButton.setPreferredSize(new Dimension(280, 38));
        nextLevelButton.addActionListener(event -> {
            if (training.advanceLevel()) {
                onLevelAdvanced.run();
                onProgressionChanged.run();
                refresh();
            }
        });

        restartLevelButton.setFocusable(false);
        restartLevelButton.setPreferredSize(new Dimension(280, 38));
        restartLevelButton.addActionListener(event -> onRestartLevel.run());

        levelActionsPanel.setOpaque(false);
        levelActionsPanel.add(nextLevelButton);
        levelActionsPanel.add(restartLevelButton);
        return panel;
    }

    private JPanel createEconomyRow() {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JButton shopButton = new JButton("Shop");
        shopButton.setFocusable(false);
        shopButton.addActionListener(event -> ShopDialog.show(
                SwingUtilities.getWindowAncestor(this),
                training.progression(),
                () -> {
                    onProgressionChanged.run();
                    refresh();
                }
        ));
        row.add(scoreLabel, BorderLayout.CENTER);
        row.add(shopButton, BorderLayout.EAST);
        return row;
    }

    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.setOpaque(false);

        JLabel title = new JLabel("Quick help");
        configureLabel(title, Font.BOLD, 15f, TEXT);
        panel.add(title);
        panel.add(help("Low plants: Rain or Compost"));
        panel.add(help("High plants: Drought or Rabbits"));
        panel.add(help("Too many rabbits: Wolves"));
        panel.add(help("Too many humans: Bears"));
        return panel;
    }

    private JLabel help(String text) {
        JLabel label = new JLabel(html(text, 336));
        configureLabel(label, Font.PLAIN, 14f, MUTED);
        return label;
    }

    private void configureLabel(JLabel label, int style, float size, Color color) {
        label.setFont(label.getFont().deriveFont(style, size));
        label.setForeground(color);
    }

    private String html(String text) {
        return html(text, 300);
    }

    private String html(String text, int width) {
        return "<html><body style='width:" + width + "px'>" + text.replace("\n", "<br>") + "</body></html>";
    }

    private String formatFailureWarning(String reason) {
        return "<html><body style='width:328px'><span style='font-size:17px;font-weight:bold;'>Level failed</span>"
                + "<br><span style='font-size:13px;'>" + reason + "</span></body></html>";
    }

    private String formatDangerWarning(String reason, String countdownText) {
        if (reason == null || countdownText == null) {
            return null;
        }
        int split = reason.indexOf(" (");
        String title = split <= 0 ? reason : reason.substring(0, split);
        String detail = split <= 0 ? "" : reason.substring(split);
        return "<html><body style='width:328px'><span style='font-size:17px;font-weight:bold;'>"
                + title
                + "</span>"
                + (detail.isBlank() ? "" : "<br><span style='font-size:13px;'>" + detail + "</span>")
                + "<br><span style='font-size:12px;color:#f6dfca;'>" + countdownText + "</span>"
                + "</body></html>";
    }
}
