package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.PopulationSnapshot;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.training.FocusRule;
import com.kimbopulus.weird.training.TrainingPrompt;
import com.kimbopulus.weird.training.TrainingSession;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;

public final class TrainingPanel extends JPanel {
    private static final Color BACKGROUND = new Color(247, 243, 232);
    private static final Color TEXT = new Color(38, 42, 38);
    private static final Color MUTED = new Color(92, 96, 88);
    private static final Color GREEN = new Color(71, 139, 84);
    private static final Color RABBIT = new Color(166, 119, 74);
    private static final Color WOLF = new Color(78, 88, 103);
    private static final Color RULE_NORMAL = new Color(75, 80, 112);
    private static final Color RULE_OPPOSITE = new Color(160, 66, 48);

    private final Simulation simulation;
    private final TrainingSession training;
    private final Runnable onProgressionChanged;
    private final JLabel titleLabel = new JLabel("Focus Path");
    private final JLabel levelLabel = new JLabel();
    private final JLabel scoreLabel = new JLabel();
    private final JLabel goalLabel = new JLabel();
    private final JLabel balanceLabel = new JLabel();
    private final JLabel climateLabel = new JLabel();
    private final JLabel eventLabel = new JLabel();
    private final JLabel ruleLabel = new JLabel();
    private final JLabel promptLabel = new JLabel();
    private final JLabel feedbackLabel = new JLabel();
    private final JProgressBar levelProgress = new JProgressBar();
    private final JButton[] answerButtons = new JButton[3];
    private final JButton nextLevelButton = new JButton("Next Level");
    private final TrendPanel trendPanel;

    public TrainingPanel(Simulation simulation, TrainingSession training) {
        this(simulation, training, () -> {
        });
    }

    public TrainingPanel(Simulation simulation, TrainingSession training, Runnable onProgressionChanged) {
        this.simulation = simulation;
        this.training = training;
        this.onProgressionChanged = onProgressionChanged;
        this.trendPanel = new TrendPanel(simulation);

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
        configureLabel(balanceLabel, Font.BOLD, 14f, TEXT);
        configureLabel(climateLabel, Font.PLAIN, 12f, MUTED);
        configureLabel(eventLabel, Font.BOLD, 12f, new Color(126, 78, 56));
        configureLabel(ruleLabel, Font.BOLD, 13f, RULE_NORMAL);
        levelProgress.setStringPainted(true);
        levelProgress.setForeground(new Color(77, 143, 85));
        levelProgress.setBackground(new Color(222, 216, 199));
        levelProgress.setBorderPainted(false);
        levelProgress.setPreferredSize(new Dimension(280, 18));
        top.add(levelLabel);
        top.add(goalLabel);
        top.add(levelProgress);
        top.add(createEconomyRow());
        top.add(balanceLabel);
        top.add(climateLabel);
        top.add(eventLabel);
        top.add(ruleLabel);

        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 9));
        center.setOpaque(false);
        center.add(trendPanel, BorderLayout.NORTH);
        JPanel stack = new JPanel(new BorderLayout(0, 8));
        stack.setOpaque(false);
        stack.add(createPromptPanel(), BorderLayout.NORTH);
        stack.add(createControlsPanel(), BorderLayout.CENTER);
        center.add(stack, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        configureLabel(feedbackLabel, Font.PLAIN, 13f, MUTED);
        add(feedbackLabel, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        PopulationSnapshot snapshot = simulation.currentSnapshot();
        scoreLabel.setText("Run " + training.score() + "   Total " + training.progression().totalScore()
                + "   Tokens " + training.progression().tokens());
        levelLabel.setText("Level " + training.levelNumber() + "/" + training.levelCount()
                + "   " + training.levelTitle());
        goalLabel.setText(training.objective());
        levelProgress.setMaximum(training.drillTarget());
        levelProgress.setValue(training.drillProgress());
        levelProgress.setString(training.drillProgress() + " / " + training.drillTarget());
        levelProgress.setForeground(training.levelComplete()
                ? new Color(189, 137, 56)
                : new Color(77, 143, 85));
        balanceLabel.setText(training.balanceStatus(snapshot));
        climateLabel.setText(String.format(
                "Water %.0f%%   Soil %.0f%%   %.0f C",
                snapshot.averageMoisture() * 100.0,
                snapshot.averageFertility() * 100.0,
                snapshot.averageTemperature()
        ));
        eventLabel.setText("Weather: " + simulation.currentEvent().title());
        ruleLabel.setText("Rule: " + training.focusRule().instruction()
                + "   Memory " + training.memorySpan());
        ruleLabel.setForeground(training.focusRule() == FocusRule.OPPOSITE
                ? RULE_OPPOSITE
                : RULE_NORMAL);

        TrainingPrompt prompt = training.prompt();
        if (training.levelComplete()) {
            promptLabel.setText("Level complete. +" + training.lastLevelReward() + " tokens");
            setAnswerChoices(List.of("Rising", "Stable", "Falling"), false);
        } else if (prompt == null) {
            promptLabel.setText("Watch for the next recall.");
            setAnswerChoices(List.of("Rising", "Stable", "Falling"), false);
        } else {
            promptLabel.setText(html(prompt.question()));
            setAnswerChoices(prompt.choices(), true);
        }
        feedbackLabel.setText(html(training.feedback()));
        nextLevelButton.setVisible(training.levelComplete());
        trendPanel.repaint();
    }

    private JPanel createPromptPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 7));
        panel.setOpaque(false);

        configureLabel(promptLabel, Font.BOLD, 15f, TEXT);
        panel.add(promptLabel, BorderLayout.NORTH);

        JPanel answers = new JPanel(new GridLayout(1, 3, 8, 0));
        answers.setOpaque(false);
        answers.setPreferredSize(new Dimension(280, 38));
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i] = answerButton(i);
            answers.add(answerButtons[i]);
        }

        JPanel answerWrap = new JPanel(new BorderLayout());
        answerWrap.setOpaque(false);
        answerWrap.add(answers, BorderLayout.NORTH);
        panel.add(answerWrap, BorderLayout.CENTER);
        nextLevelButton.setFocusable(false);
        nextLevelButton.setPreferredSize(new Dimension(280, 38));
        nextLevelButton.addActionListener(event -> {
            if (training.advanceLevel()) {
                onProgressionChanged.run();
                refresh();
            }
        });
        panel.add(nextLevelButton, BorderLayout.SOUTH);
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

    private JButton answerButton(int answerIndex) {
        JButton button = new JButton();
        button.setFocusable(false);
        button.addActionListener(event -> {
            training.answer(answerIndex);
            refresh();
        });
        return button;
    }

    private void setAnswerChoices(List<String> choices, boolean enabled) {
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setText(choices.get(i));
            answerButtons[i].setEnabled(enabled);
        }
    }

    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.setOpaque(false);

        JLabel title = new JLabel("Quick help");
        configureLabel(title, Font.BOLD, 14f, TEXT);
        panel.add(title);
        panel.add(help("Low plants: Rain or Compost"));
        panel.add(help("Too many plants: Rabbit or Trim"));
        panel.add(help("Too many rabbits: Wolf"));
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

    private static final class TrendPanel extends JPanel {
        private final Simulation simulation;

        private TrendPanel(Simulation simulation) {
            this.simulation = simulation;
            setOpaque(false);
            setPreferredSize(new Dimension(280, 125));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            int left = 12;
            int top = 12;
            int chartWidth = width - 24;
            int chartHeight = height - 30;

            g.setColor(new Color(229, 222, 206));
            g.fillRoundRect(0, 0, width, height, 8, 8);
            g.setColor(new Color(206, 197, 178));
            g.drawRoundRect(0, 0, width - 1, height - 1, 8, 8);

            List<PopulationSnapshot> history = simulation.recentHistory(72);
            if (history.size() < 2) {
                g.dispose();
                return;
            }

            int max = 1;
            for (PopulationSnapshot snapshot : history) {
                max = Math.max(max, Math.max(snapshot.plants(), Math.max(snapshot.rabbits(), snapshot.wolves())));
            }

            drawLine(g, history, max, left, top, chartWidth, chartHeight, GREEN, PopulationSnapshot::plants);
            drawLine(g, history, max, left, top, chartWidth, chartHeight, RABBIT, PopulationSnapshot::rabbits);
            drawLine(g, history, max, left, top, chartWidth, chartHeight, WOLF, PopulationSnapshot::wolves);

            g.setColor(MUTED);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 11f));
            g.drawString("Plants", left, height - 8);
            g.drawString("Rabbits", left + 68, height - 8);
            g.drawString("Wolves", left + 144, height - 8);
            g.dispose();
        }

        private void drawLine(
                Graphics2D g,
                List<PopulationSnapshot> history,
                int max,
                int left,
                int top,
                int width,
                int height,
                Color color,
                Metric metric
        ) {
            g.setColor(color);
            g.setStroke(new BasicStroke(2f));
            int previousX = left;
            int previousY = top + height - (metric.value(history.get(0)) * height / max);
            for (int i = 1; i < history.size(); i++) {
                int x = left + i * width / (history.size() - 1);
                int y = top + height - (metric.value(history.get(i)) * height / max);
                g.drawLine(previousX, previousY, x, y);
                previousX = x;
                previousY = y;
            }
        }

        private interface Metric {
            int value(PopulationSnapshot snapshot);
        }
    }
}
