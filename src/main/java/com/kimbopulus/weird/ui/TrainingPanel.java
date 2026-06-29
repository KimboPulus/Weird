package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.PopulationSnapshot;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.training.TrainingSession;

import javax.swing.BorderFactory;
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
    private final JLabel titleLabel = new JLabel("Focus Path");
    private final JLabel levelLabel = new JLabel();
    private final JLabel scoreLabel = new JLabel();
    private final JLabel goalLabel = new JLabel();
    private final JLabel challengeLabel = new JLabel();
    private final JLabel balanceLabel = new JLabel();
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
        setPreferredSize(new Dimension(320, 640));
        setBorder(BorderFactory.createEmptyBorder(14, 18, 12, 18));
        setLayout(new BorderLayout(0, 9));

        JPanel top = new JPanel(new GridLayout(0, 1, 0, 4));
        top.setOpaque(false);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setForeground(TEXT);
        top.add(titleLabel);

        configureLabel(levelLabel, Font.BOLD, 15f, new Color(75, 101, 67));
        configureLabel(scoreLabel, Font.BOLD, 15f, TEXT);
        configureLabel(goalLabel, Font.BOLD, 14f, TEXT);
        configureLabel(challengeLabel, Font.BOLD, 12f, new Color(126, 78, 56));
        configureLabel(balanceLabel, Font.BOLD, 14f, TEXT);
        configureLabel(detailLabel, Font.PLAIN, 12f, MUTED);
        configureLabel(climateLabel, Font.PLAIN, 12f, MUTED);
        configureLabel(eventLabel, Font.BOLD, 12f, new Color(126, 78, 56));
        configureLabel(warningLabel, Font.BOLD, 24f, Color.WHITE);
        configureLabel(feedbackLabel, Font.PLAIN, 13f, MUTED);

        warningLabel.setOpaque(true);
        warningLabel.setBackground(new Color(176, 57, 45));
        warningLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        warningLabel.setVisible(false);

        levelProgress.setStringPainted(true);
        levelProgress.setForeground(new Color(77, 143, 85));
        levelProgress.setBackground(new Color(222, 216, 199));
        levelProgress.setBorderPainted(false);
        levelProgress.setPreferredSize(new Dimension(280, 18));

        top.add(levelLabel);
        top.add(goalLabel);
        top.add(challengeLabel);
        top.add(levelProgress);
        top.add(createEconomyRow());
        top.add(balanceLabel);
        top.add(climateLabel);
        top.add(eventLabel);
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
        goalLabel.setText(training.objective());
        challengeLabel.setText(html(training.challengeText()));
        levelProgress.setMaximum(training.drillTarget());
        levelProgress.setValue(training.drillProgress());
        levelProgress.setString(training.drillProgress() + " / " + training.drillTarget());
        levelProgress.setForeground(training.levelComplete()
                ? new Color(189, 137, 56)
                : new Color(77, 143, 85));
        balanceLabel.setText(training.balanceStatus(snapshot));
        detailLabel.setText(html(training.balanceGuide(snapshot, boardCells)));
        climateLabel.setText(String.format(
                "Water %.0f%%   Soil %.0f%%   %.1f C",
                snapshot.averageMoisture() * 100.0,
                snapshot.averageFertility() * 100.0,
                snapshot.averageTemperature()
        ));
        eventLabel.setText("Weather: " + simulation.currentEvent().title());
        String warning = training.levelFailed() ? "LEVEL LOST" : training.dangerWarning();
        warningLabel.setText(warning == null ? "" : warning.toUpperCase());
        warningLabel.setVisible(warning != null);

        nextLevelButton.setVisible(training.levelComplete());
        restartLevelButton.setVisible(training.levelFailed());
        levelActionsPanel.setVisible(training.levelComplete() || training.levelFailed());
        helpPanel.setVisible(!training.levelComplete() && !training.levelFailed());
        feedbackLabel.setText(html(training.feedback()));
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
        configureLabel(title, Font.BOLD, 14f, TEXT);
        panel.add(title);
        panel.add(help("Low plants: Rain or Compost"));
        panel.add(help("High plants: Drought or Rabbits"));
        panel.add(help("Too many rabbits: Wolves"));
        panel.add(help("Too many humans: Bears"));
        panel.add(help("Rabbits spend energy each tick and can starve"));
        panel.add(help("Lightning: 50 tokens, click one creature"));
        return panel;
    }

    private JLabel help(String text) {
        JLabel label = new JLabel(html(text));
        configureLabel(label, Font.PLAIN, 12f, MUTED);
        return label;
    }

    private void configureLabel(JLabel label, int style, float size, Color color) {
        label.setFont(label.getFont().deriveFont(style, size));
        label.setForeground(color);
    }

    private String html(String text) {
        return "<html><body style='width:275px'>" + text + "</body></html>";
    }
}
