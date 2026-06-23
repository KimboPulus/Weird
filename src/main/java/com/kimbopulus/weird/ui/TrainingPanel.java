package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.PopulationSnapshot;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.training.TrainingPrompt;
import com.kimbopulus.weird.training.TrainingSession;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

    private final Simulation simulation;
    private final TrainingSession training;
    private final JLabel titleLabel = new JLabel("Focus Grove");
    private final JLabel scoreLabel = new JLabel();
    private final JLabel goalLabel = new JLabel();
    private final JLabel balanceLabel = new JLabel();
    private final JLabel promptLabel = new JLabel();
    private final JLabel feedbackLabel = new JLabel();
    private final TrendPanel trendPanel;

    public TrainingPanel(Simulation simulation, TrainingSession training) {
        this.simulation = simulation;
        this.training = training;
        this.trendPanel = new TrendPanel(simulation);

        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(300, 640));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setLayout(new BorderLayout(0, 14));

        JPanel top = new JPanel(new GridLayout(0, 1, 0, 8));
        top.setOpaque(false);

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setForeground(TEXT);
        top.add(titleLabel);

        configureLabel(scoreLabel, Font.BOLD, 15f, TEXT);
        configureLabel(goalLabel, Font.PLAIN, 13f, MUTED);
        configureLabel(balanceLabel, Font.BOLD, 14f, TEXT);
        top.add(scoreLabel);
        top.add(goalLabel);
        top.add(balanceLabel);

        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(trendPanel, BorderLayout.NORTH);
        center.add(createPromptPanel(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        configureLabel(feedbackLabel, Font.PLAIN, 13f, MUTED);
        add(feedbackLabel, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        PopulationSnapshot snapshot = simulation.currentSnapshot();
        scoreLabel.setText("Score " + training.score() + "   Streak " + training.streak());
        goalLabel.setText(html(training.focusGoal()));
        balanceLabel.setText(training.balanceStatus(snapshot));

        TrainingPrompt prompt = training.prompt();
        if (prompt == null) {
            promptLabel.setText(html("Scan the board. A recall prompt will appear soon."));
        } else {
            promptLabel.setText(html(prompt.question()));
        }
        feedbackLabel.setText(html(training.feedback()));
        trendPanel.repaint();
    }

    private JPanel createPromptPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        configureLabel(promptLabel, Font.BOLD, 15f, TEXT);
        panel.add(promptLabel, BorderLayout.NORTH);

        JPanel answers = new JPanel(new GridLayout(1, 3, 8, 0));
        answers.setOpaque(false);
        answers.add(answerButton("Plants", OrganismKind.PLANT));
        answers.add(answerButton("Rabbits", OrganismKind.RABBIT));
        answers.add(answerButton("Wolves", OrganismKind.WOLF));
        panel.add(answers, BorderLayout.CENTER);
        return panel;
    }

    private JButton answerButton(String text, OrganismKind kind) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.addActionListener(event -> {
            training.answer(kind);
            refresh();
        });
        return button;
    }

    private void configureLabel(JLabel label, int style, float size, Color color) {
        label.setFont(label.getFont().deriveFont(style, size));
        label.setForeground(color);
    }

    private String html(String text) {
        return "<html><body style='width:245px'>" + text + "</body></html>";
    }

    private static final class TrendPanel extends JPanel {
        private final Simulation simulation;

        private TrendPanel(Simulation simulation) {
            this.simulation = simulation;
            setOpaque(false);
            setPreferredSize(new Dimension(260, 150));
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
