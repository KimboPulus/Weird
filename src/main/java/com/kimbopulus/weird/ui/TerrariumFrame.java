package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.Simulation;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Font;

public final class TerrariumFrame extends JFrame {
    private final Simulation simulation;
    private final TerrariumPanel terrariumPanel;
    private final JLabel statusLabel;
    private final Timer timer;

    public TerrariumFrame() {
        super("Weird");

        simulation = Simulation.createDefault();
        terrariumPanel = new TerrariumPanel(simulation);
        statusLabel = new JLabel();
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 13f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JPanel content = new JPanel(new BorderLayout());
        content.add(terrariumPanel, BorderLayout.CENTER);
        content.add(statusLabel, BorderLayout.SOUTH);
        setContentPane(content);

        timer = new Timer(700, event -> {
            simulation.tick();
            terrariumPanel.repaint();
            updateStatus();
        });
        timer.start();
        updateStatus();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1040, 760);
        setLocationRelativeTo(null);
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

