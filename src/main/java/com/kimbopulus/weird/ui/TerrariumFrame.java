package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;

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
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class TerrariumFrame extends JFrame {
    private final Simulation simulation;
    private final TerrariumPanel terrariumPanel;
    private final JLabel statusLabel;
    private final Timer timer;
    private ToolMode toolMode = ToolMode.RAIN;
    private JButton pauseButton;

    public TerrariumFrame() {
        super("Weird");

        simulation = Simulation.createDefault();
        terrariumPanel = new TerrariumPanel(simulation);
        statusLabel = new JLabel();
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 13f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JPanel content = new JPanel(new BorderLayout());
        content.add(createToolbar(), BorderLayout.NORTH);
        content.add(terrariumPanel, BorderLayout.CENTER);
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
                updateStatus();
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
        toolbar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        ButtonGroup tools = new ButtonGroup();
        for (ToolMode mode : ToolMode.values()) {
            JToggleButton button = new JToggleButton(mode.label());
            button.setFocusable(false);
            button.addActionListener(event -> toolMode = mode);
            if (mode == toolMode) {
                button.setSelected(true);
            }
            tools.add(button);
            toolbar.add(button);
        }

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
        terrariumPanel.repaint();
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
