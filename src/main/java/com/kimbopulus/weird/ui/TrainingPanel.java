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
    private static final int WARNING_SLOT_HEIGHT = 152;

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
    private final JPanel warningPanel = new JPanel();
    private final JLabel warningTitleLabel = new JLabel();
    private final JLabel warningDetailLabel = new JLabel();
    private final JLabel warningActionLabel = new JLabel();
    private final JLabel warningCountdownLabel = new JLabel();
    private final JPanel warningSlotPanel = new JPanel(new BorderLayout());
    private final JLabel feedbackLabel = new JLabel();
    private final JProgressBar levelProgress = new JProgressBar();
    private final JButton nextLevelButton = new JButton("Next Level");
    private final JButton restartLevelButton = new JButton("Restart Level");
    private final JPanel levelActionsPanel = new JPanel(new GridLayout(0, 1, 0, 5));

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

        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(432, 700));
        setBorder(BorderFactory.createEmptyBorder(14, 18, 12, 18));
        setLayout(new BorderLayout(0, 9));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 25f));
        titleLabel.setForeground(TEXT);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        titleLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        top.add(titleLabel);
        top.add(Box.createVerticalStrut(6));

        configureLabel(levelLabel, Font.BOLD, 20f, new Color(75, 101, 67));
        configureLabel(scoreLabel, Font.BOLD, 19f, TEXT);
        configureLabel(goalLabel, Font.BOLD, 17f, TEXT);
        configureLabel(balanceLabel, Font.BOLD, 19f, TEXT);
        configureLabel(countsLabel, Font.BOLD, 21f, TEXT);
        configureLabel(detailLabel, Font.PLAIN, 16f, MUTED);
        configureLabel(climateLabel, Font.PLAIN, 16f, MUTED);
        configureLabel(eventLabel, Font.BOLD, 16f, new Color(126, 78, 56));
        configureLabel(feedbackLabel, Font.PLAIN, 16f, MUTED);

        warningPanel.setOpaque(true);
        warningPanel.setBackground(new Color(176, 57, 45));
        warningPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        warningPanel.setLayout(new BoxLayout(warningPanel, BoxLayout.Y_AXIS));
        warningPanel.setAlignmentX(LEFT_ALIGNMENT);
        warningPanel.setVisible(false);

        warningSlotPanel.setOpaque(false);
        warningSlotPanel.setAlignmentX(LEFT_ALIGNMENT);
        warningSlotPanel.setMinimumSize(new Dimension(0, WARNING_SLOT_HEIGHT));
        warningSlotPanel.setPreferredSize(new Dimension(360, WARNING_SLOT_HEIGHT));
        warningSlotPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, WARNING_SLOT_HEIGHT));

        configureWarningLine(warningTitleLabel, Font.BOLD, 17f);
        configureWarningLine(warningDetailLabel, Font.BOLD, 15f);
        configureWarningLine(warningActionLabel, Font.BOLD, 14f);
        configureWarningLine(warningCountdownLabel, Font.BOLD, 14f);

        warningPanel.add(warningTitleLabel);
        warningPanel.add(Box.createVerticalStrut(4));
        warningPanel.add(warningDetailLabel);
        warningPanel.add(Box.createVerticalStrut(4));
        warningPanel.add(warningActionLabel);
        warningPanel.add(Box.createVerticalStrut(4));
        warningPanel.add(warningCountdownLabel);
        warningSlotPanel.add(warningPanel, BorderLayout.CENTER);

        levelProgress.setStringPainted(true);
        levelProgress.setForeground(new Color(77, 143, 85));
        levelProgress.setBackground(new Color(222, 216, 199));
        levelProgress.setBorderPainted(false);
        levelProgress.setPreferredSize(new Dimension(330, 20));
        levelProgress.setAlignmentX(LEFT_ALIGNMENT);

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
        top.add(warningSlotPanel);
        add(top, BorderLayout.NORTH);

        JPanel filler = new JPanel();
        filler.setOpaque(false);
        add(filler, BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

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
        balanceLabel.setText(html(training.objectiveStatus(snapshot), 352));
        balanceLabel.setForeground(training.objectiveOnTrack(snapshot) ? new Color(75, 101, 67) : new Color(126, 78, 56));
        countsLabel.setText(html(training.currentSummary(snapshot), 336));
        detailLabel.setText(html(training.balanceGuide(snapshot, boardCells), 336));
        climateLabel.setText(String.format(
                "Moisture %.0f%%   Soil %.0f%%   %.1f C",
                snapshot.averageMoisture() * 100.0,
                snapshot.averageFertility() * 100.0,
                snapshot.averageTemperature()
        ));
        eventLabel.setText("Weather: " + simulation.currentEvent().title());
        refreshWarningPanel();

        nextLevelButton.setVisible(training.levelComplete());
        restartLevelButton.setVisible(training.levelFailed());
        levelActionsPanel.setVisible(training.levelComplete() || training.levelFailed());
        feedbackLabel.setText(html(training.feedback(), 336));
        feedbackLabel.setForeground(MUTED);
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        detailLabel.setAlignmentX(LEFT_ALIGNMENT);
        levelActionsPanel.setAlignmentX(LEFT_ALIGNMENT);
        feedbackLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(detailLabel);
        panel.add(Box.createVerticalStrut(12));
        panel.add(levelActionsPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(feedbackLabel);

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
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
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

    private void configureLabel(JLabel label, int style, float size, Color color) {
        label.setFont(label.getFont().deriveFont(style, size));
        label.setForeground(color);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
    }

    private void configureWarningLine(JLabel label, int style, float size) {
        configureLabel(label, style, size, Color.WHITE);
        label.setAlignmentX(LEFT_ALIGNMENT);
    }

    private String html(String text) {
        return html(text, 300);
    }

    private String html(String text, int width) {
        return "<html><body style='width:" + width + "px'>" + text.replace("\n", "<br>") + "</body></html>";
    }

    private String warningHtml(String text, int maxLineLength) {
        return "<html>" + wrapWarningText(text, maxLineLength).replace("\n", "<br>") + "</html>";
    }

    private String wrapWarningText(String text, int maxLineLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] inputLines = text.split("\\R");
        StringBuilder wrapped = new StringBuilder();
        for (int i = 0; i < inputLines.length; i++) {
            if (i > 0) {
                wrapped.append('\n');
            }
            appendWrappedLine(wrapped, inputLines[i].trim(), maxLineLength);
        }
        return wrapped.toString();
    }

    private void appendWrappedLine(StringBuilder wrapped, String line, int maxLineLength) {
        if (line.isBlank()) {
            return;
        }
        int currentLength = 0;
        for (String word : line.split("\\s+")) {
            if (word.isBlank()) {
                continue;
            }
            int required = currentLength == 0 ? word.length() : currentLength + 1 + word.length();
            if (currentLength > 0 && required > maxLineLength) {
                wrapped.append('\n');
                wrapped.append(word);
                currentLength = word.length();
                continue;
            }
            if (currentLength > 0) {
                wrapped.append(' ');
                currentLength++;
            }
            wrapped.append(word);
            currentLength += word.length();
        }
    }

    private void refreshWarningPanel() {
        WarningCard warning = training.levelFailed()
                ? formatFailureWarning(training.failureReason(), training.failureAction())
                : formatDangerWarning(training.dangerDetail(), training.dangerCountdownLabel(), training.dangerAction());
        if (warning == null) {
            warningPanel.setVisible(false);
            warningTitleLabel.setText("");
            warningDetailLabel.setText("");
            warningActionLabel.setText("");
            warningCountdownLabel.setText("");
            return;
        }

        warningTitleLabel.setText(warningHtml(warning.title(), 22));
        warningDetailLabel.setText(warning.detail() == null ? "" : warningHtml(warning.detail(), 24));
        warningDetailLabel.setVisible(warning.detail() != null && !warning.detail().isBlank());
        warningActionLabel.setText(warning.action() == null ? "" : warningHtml(warning.action(), 24));
        warningActionLabel.setVisible(warning.action() != null && !warning.action().isBlank());
        warningCountdownLabel.setText(warning.countdown() == null ? "" : warningHtml(warning.countdown(), 24));
        warningCountdownLabel.setVisible(warning.countdown() != null && !warning.countdown().isBlank());

        warningPanel.setVisible(true);
        warningPanel.revalidate();
        warningPanel.repaint();
    }

    private WarningCard formatFailureWarning(String reason, String action) {
        return new WarningCard("Level failed", reason, action, null);
    }

    private WarningCard formatDangerWarning(String reason, String countdownText, String action) {
        if (reason == null || countdownText == null) {
            return null;
        }
        int split = reason.indexOf(" (");
        String title = split <= 0 ? reason : reason.substring(0, split);
        String detail = split <= 0 ? "" : reason.substring(split);
        return new WarningCard(
                title,
                detail.isBlank() ? null : detail,
                action,
                countdownText
        );
    }

    private record WarningCard(String title, String detail, String action, String countdown) {
    }
}
