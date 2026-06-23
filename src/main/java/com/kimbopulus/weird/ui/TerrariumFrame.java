package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.training.TrainingSession;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public final class TerrariumFrame extends JFrame {
    private final Simulation simulation;
    private final TrainingSession training;
    private final TerrariumPanel terrariumPanel;
    private final TrainingPanel trainingPanel;
    private final JLabel statusLabel;
    private final JLabel toolHintLabel;
    private final Timer timer;
    private ToolMode toolMode = ToolMode.RAIN;
    private JButton pauseButton;

    public TerrariumFrame() {
        super("Weird");

        simulation = Simulation.createDefault();
        training = new TrainingSession();
        terrariumPanel = new TerrariumPanel(simulation);
        trainingPanel = new TrainingPanel(simulation, training);
        toolHintLabel = new JLabel();
        toolHintLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        toolHintLabel.setFont(toolHintLabel.getFont().deriveFont(Font.PLAIN, 13f));
        toolHintLabel.setForeground(new Color(52, 55, 49));
        statusLabel = new JLabel();
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 13f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        statusLabel.setForeground(new Color(227, 222, 205));

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(new Color(30, 32, 29));
        content.add(createHeader(), BorderLayout.NORTH);
        content.add(terrariumPanel, BorderLayout.CENTER);
        content.add(trainingPanel, BorderLayout.EAST);
        content.add(statusLabel, BorderLayout.SOUTH);
        setContentPane(content);

        terrariumPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                Position position = terrariumPanel.positionAtPoint(event.getX(), event.getY());
                if (position == null) {
                    return;
                }
                toolMode.apply(simulation, position);
                terrariumPanel.showToolEffect(position, toolMode);
                training.noteAction(toolMode.label(), terrariumPanel.describe(position));
                terrariumPanel.repaint();
                trainingPanel.refresh();
                updateStatus();
            }
        });
        terrariumPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                Position position = terrariumPanel.positionAtPoint(event.getX(), event.getY());
                terrariumPanel.setHoverPosition(position);
                updateToolHint(position);
            }
        });

        timer = new Timer(700, event -> {
            stepSimulation();
        });
        timer.start();
        updateStatus();
        updateToolHint(null);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1100, 720));
        setSize(1240, 820);
        setLocationRelativeTo(null);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(239, 233, 218));
        header.add(createToolbar(), BorderLayout.NORTH);

        toolHintLabel.setOpaque(true);
        toolHintLabel.setBackground(new Color(228, 221, 204));
        toolHintLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        header.add(toolHintLabel, BorderLayout.SOUTH);
        return header;
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(new Color(239, 233, 218));
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        ButtonGroup tools = new ButtonGroup();
        JPanel toolButtons = new JPanel(new GridLayout(1, ToolMode.values().length, 6, 0));
        toolButtons.setOpaque(false);
        for (ToolMode mode : ToolMode.values()) {
            JToggleButton button = new JToggleButton(mode.label());
            button.setFocusable(false);
            button.setToolTipText(mode.description());
            button.addActionListener(event -> {
                toolMode = mode;
                updateToolHint(null);
            });
            if (mode == toolMode) {
                button.setSelected(true);
            }
            tools.add(button);
            toolButtons.add(button);
        }
        toolbar.add(toolButtons);
        toolbar.add(Box.createHorizontalGlue());

        toolbar.add(new JLabel("Speed "));
        JComboBox<String> speedBox = new JComboBox<>(new String[]{"Slow", "Normal", "Fast"});
        speedBox.setSelectedItem("Normal");
        speedBox.setFocusable(false);
        speedBox.addActionListener(event -> setSpeed((String) speedBox.getSelectedItem()));
        toolbar.add(speedBox);
        toolbar.addSeparator();

        pauseButton = new JButton("Pause");
        pauseButton.setFocusable(false);
        pauseButton.addActionListener(event -> togglePause());
        toolbar.add(pauseButton);

        JButton stepButton = new JButton("Step");
        stepButton.setFocusable(false);
        stepButton.addActionListener(event -> stepSimulation());
        toolbar.add(stepButton);

        JButton restartButton = new JButton("Restart");
        restartButton.setFocusable(false);
        restartButton.addActionListener(event -> restart());
        toolbar.add(restartButton);

        return toolbar;
    }

    private void togglePause() {
        if (timer.isRunning()) {
            timer.stop();
            pauseButton.setText("Resume");
        } else {
            timer.start();
            pauseButton.setText("Pause");
        }
    }

    private void stepSimulation() {
        simulation.tick();
        training.update(simulation);
        terrariumPanel.repaint();
        trainingPanel.refresh();
        updateStatus();
    }

    private void setSpeed(String speed) {
        int delay = switch (speed) {
            case "Slow" -> 1200;
            case "Fast" -> 300;
            default -> 700;
        };
        timer.setDelay(delay);
    }

    private void restart() {
        simulation.restart();
        training.reset();
        terrariumPanel.repaint();
        trainingPanel.refresh();
        updateStatus();
        updateToolHint(null);
    }

    private void updateStatus() {
        statusLabel.setText(String.format(
                "Tick %d   Season: %s   Event: %s   Plants: %d   Rabbits: %d   Wolves: %d",
                simulation.tickCount(),
                simulation.season(),
                simulation.currentEvent().title(),
                simulation.count(OrganismKind.PLANT),
                simulation.count(OrganismKind.RABBIT),
                simulation.count(OrganismKind.WOLF)
        ));
    }

    private void updateToolHint(Position position) {
        if (position == null) {
            toolHintLabel.setText(toolMode.label() + ": " + toolMode.description());
            return;
        }
        toolHintLabel.setText(toolMode.label() + ": " + terrariumPanel.describe(position));
    }
}
