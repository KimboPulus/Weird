package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.training.TrainingSession;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
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
    private final Timer timer;
    private ToolMode toolMode = ToolMode.RAIN;
    private JButton pauseButton;

    public TerrariumFrame() {
        super("Weird");

        simulation = Simulation.createDefault();
        training = new TrainingSession();
        terrariumPanel = new TerrariumPanel(simulation);
        trainingPanel = new TrainingPanel(simulation, training);
        statusLabel = new JLabel();
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 13f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        statusLabel.setForeground(new Color(227, 222, 205));

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(new Color(30, 32, 29));
        content.add(createToolbar(), BorderLayout.NORTH);
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
                terrariumPanel.repaint();
                trainingPanel.refresh();
                updateStatus();
            }
        });
        terrariumPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                terrariumPanel.setHoverPosition(terrariumPanel.positionAtPoint(event.getX(), event.getY()));
            }
        });

        timer = new Timer(700, event -> {
            stepSimulation();
        });
        timer.start();
        updateStatus();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1040, 760);
        setLocationRelativeTo(null);
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
            button.addActionListener(event -> toolMode = mode);
            if (mode == toolMode) {
                button.setSelected(true);
            }
            tools.add(button);
            toolButtons.add(button);
        }
        toolbar.add(toolButtons);

        toolbar.addSeparator();

        pauseButton = new JButton("Pause");
        pauseButton.setFocusable(false);
        pauseButton.addActionListener(event -> togglePause());
        toolbar.add(pauseButton);

        JButton stepButton = new JButton("Step");
        stepButton.setFocusable(false);
        stepButton.addActionListener(event -> stepSimulation());
        toolbar.add(stepButton);

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

    private void updateStatus() {
        statusLabel.setText(String.format(
                "Tick %d   Season: %s   Plants: %d   Rabbits: %d   Wolves: %d",
                simulation.tickCount(),
                simulation.season(),
                simulation.count(OrganismKind.PLANT),
                simulation.count(OrganismKind.RABBIT),
                simulation.count(OrganismKind.WOLF)
        ));
    }
}
