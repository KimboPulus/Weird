package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.audio.AudioEngine;
import com.kimbopulus.weird.audio.SoundCue;
import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.DeathEvent;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.progression.ShopItem;
import com.kimbopulus.weird.settings.GameSettings;
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
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumMap;
import java.util.Map;

public final class TerrariumFrame extends JFrame {
    private final Simulation simulation;
    private final TrainingSession training;
    private final TerrariumPanel terrariumPanel;
    private final TrainingPanel trainingPanel;
    private final JLabel statusLabel;
    private final JLabel toolHintLabel;
    private final Timer timer;
    private final AudioEngine audio;
    private final GameSettings settings;
    private final Map<ToolMode, JToggleButton> toolButtons = new EnumMap<>(ToolMode.class);
    private ToolMode toolMode = ToolMode.RAIN;
    private JButton pauseButton;
    private long lastDeathSoundId;

    public TerrariumFrame() {
        super("Weird");

        simulation = Simulation.createDefault();
        training = new TrainingSession();
        settings = GameSettings.loadDefault();
        audio = new AudioEngine();
        audio.applySettings(settings);
        terrariumPanel = new TerrariumPanel(simulation);
        trainingPanel = new TrainingPanel(
                simulation,
                training,
                this::updateToolAvailability,
                this::restartFailedLevel,
                this::celebrateLevel
        );
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
        installKeyBindings();

        terrariumPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                Position position = terrariumPanel.positionAtPoint(event.getX(), event.getY());
                if (position == null) {
                    return;
                }
                toolMode.apply(simulation, position);
                applyPurchasedUpgrade(position);
                playToolSound();
                terrariumPanel.showToolEffect(position, toolMode);
                training.noteAction(toolMode.label(), terrariumPanel.describe(position));
                terrariumPanel.repaint();
                trainingPanel.refresh();
                updateToolAvailability();
                updateStatus();
            }
        });
        terrariumPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                Position position = terrariumPanel.positionAtPoint(event.getX(), event.getY());
                if (terrariumPanel.setHoverPosition(position)) {
                    updateToolHint(position);
                }
            }
        });

        timer = new Timer(700, event -> {
            stepSimulation();
        });
        timer.setCoalesce(true);
        timer.start();
        updateToolAvailability();
        updateStatus();
        updateToolHint(null);
        SwingUtilities.invokeLater(this::showIntroIfNeeded);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                audio.close();
            }
        });
        setMinimumSize(new java.awt.Dimension(1360, 900));
        setSize(1520, 980);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
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
        int shortcut = 1;
        for (ToolMode mode : ToolMode.values()) {
            JToggleButton button = new JToggleButton(mode.label());
            button.setFocusable(false);
            button.setToolTipText(shortcutLabel(shortcut) + ": " + mode.description());
            button.addActionListener(event -> {
                toolMode = mode;
                updateToolHint(null);
            });
            if (mode == toolMode) {
                button.setSelected(true);
            }
            tools.add(button);
            toolButtons.add(button);
            this.toolButtons.put(mode, button);
            shortcut++;
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

        JButton audioButton = new JButton("Audio");
        audioButton.setFocusable(false);
        audioButton.setToolTipText("Set music and effect volume.");
        audioButton.addActionListener(event -> AudioSettingsDialog.show(this, settings, this::applyAudioSettings));
        toolbar.add(audioButton);

        JButton infoButton = new JButton("Info");
        infoButton.setFocusable(false);
        infoButton.setToolTipText("Show the short game guide again.");
        infoButton.addActionListener(event -> IntroDialog.show(this));
        toolbar.add(infoButton);

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
        boolean wasComplete = training.levelComplete();
        boolean wasFailed = training.levelFailed();
        boolean hadWarning = training.dangerWarning() != null;
        simulation.tick();
        training.update(simulation);
        terrariumPanel.syncDeathEffects();
        terrariumPanel.syncBirthEffects();
        playDeathSounds();
        updateAudioTension();
        if (!wasComplete && training.levelComplete()) {
            terrariumPanel.showBanner("Level complete: +" + training.lastLevelReward());
            audio.play(SoundCue.COMPLETE);
        } else if (!wasFailed && training.levelFailed()) {
            terrariumPanel.showBanner("LEVEL LOST");
            audio.play(SoundCue.FAILURE);
        } else if (!hadWarning && training.dangerWarning() != null) {
            audio.play(SoundCue.WARNING);
        }
        terrariumPanel.repaint();
        trainingPanel.refresh();
        updateToolAvailability();
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
        int answer = JOptionPane.showConfirmDialog(
                this,
                "Restart the whole run? Current level progress and run score will be lost.",
                "Restart run",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        simulation.restart();
        training.reset();
        updateAudioTension();
        audio.play(SoundCue.RESTART);
        terrariumPanel.repaint();
        trainingPanel.refresh();
        updateToolAvailability();
        updateStatus();
        updateToolHint(null);
    }

    private void updateStatus() {
        statusLabel.setText(String.format(
                "Tick %d   Season: %s   Event: %s   Plants: %d   Rabbits: %d   Wolves: %d   Humans: %d   Bears: %d",
                simulation.tickCount(),
                simulation.season(),
                simulation.currentEvent().title(),
                simulation.count(OrganismKind.PLANT),
                simulation.count(OrganismKind.RABBIT),
                simulation.count(OrganismKind.WOLF),
                simulation.count(OrganismKind.HUMAN),
                simulation.count(OrganismKind.BEAR)
        ));
    }

    private void updateToolHint(Position position) {
        if (position == null) {
            toolHintLabel.setText(toolMode.label() + ": " + toolMode.description());
            return;
        }
        toolHintLabel.setText(toolMode.label() + ": " + terrariumPanel.describe(position));
    }

    private void updateToolAvailability() {
        JToggleButton sanctuaryButton = toolButtons.get(ToolMode.SANCTUARY);
        if (sanctuaryButton == null) {
            return;
        }

        boolean unlocked = training.progression().sanctuaryUnlocked();
        sanctuaryButton.setEnabled(unlocked && !simulation.sanctuaryPlaced());
        if (!unlocked) {
            sanctuaryButton.setToolTipText("Buy the Sanctuary Permit in the shop.");
        } else if (simulation.sanctuaryPlaced()) {
            sanctuaryButton.setToolTipText("The one sanctuary for this terrarium has been placed.");
            if (toolMode == ToolMode.SANCTUARY) {
                toolMode = ToolMode.RAIN;
                toolButtons.get(ToolMode.RAIN).setSelected(true);
                updateToolHint(null);
            }
        } else {
            sanctuaryButton.setToolTipText("9: " + ToolMode.SANCTUARY.description());
        }
    }

    private void installKeyBindings() {
        JComponent root = getRootPane();
        ToolMode[] modes = ToolMode.values();
        for (int i = 0; i < modes.length; i++) {
            ToolMode mode = modes[i];
            String actionName = "tool-" + mode.name();
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(shortcutKey(i + 1)), actionName);
            root.getActionMap().put(actionName, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    selectTool(mode);
                }
            });
        }

        bindKey(root, "SPACE", "toggle-pause", this::togglePause);
        bindKey(root, "N", "step", this::stepSimulation);
        bindKey(root, "R", "restart", this::restart);
    }

    private void bindKey(JComponent root, String key, String actionName, Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), actionName);
        root.getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    private void selectTool(ToolMode mode) {
        JToggleButton button = toolButtons.get(mode);
        if (button == null || !button.isEnabled()) {
            return;
        }
        toolMode = mode;
        button.setSelected(true);
        updateToolHint(null);
    }

    private void applyPurchasedUpgrade(Position position) {
        if (toolMode == ToolMode.RAIN && training.progression().owns(ShopItem.RAIN_BARREL)) {
            simulation.rainBoost(position);
        } else if (toolMode == ToolMode.COMPOST && training.progression().owns(ShopItem.RICH_COMPOST)) {
            simulation.compostBoost(position);
        }
    }

    private void restartFailedLevel() {
        if (!training.restartLevel()) {
            return;
        }
        simulation.restart();
        updateAudioTension();
        audio.play(SoundCue.RESTART);
        terrariumPanel.showBanner("Level restarted");
        terrariumPanel.repaint();
        trainingPanel.refresh();
        updateStatus();
        updateToolAvailability();
    }

    private void playToolSound() {
        SoundCue cue = switch (toolMode) {
            case RAIN -> SoundCue.WATER;
            case DROUGHT -> SoundCue.DRY;
            case COMPOST, PLANT, SANCTUARY -> SoundCue.GROW;
            case HUMAN -> SoundCue.GROW;
            case BEAR, RABBIT, WOLF -> SoundCue.PLACE;
        };
        audio.play(cue);
    }

    private String shortcutLabel(int index) {
        return index == 10 ? "0" : Integer.toString(index);
    }

    private char shortcutKey(int index) {
        return index == 10 ? '0' : Character.forDigit(index, 10);
    }

    private void celebrateLevel() {
        terrariumPanel.showLevelUp("LEVEL " + training.levelNumber() + "  " + training.levelTitle());
        audio.play(SoundCue.LEVEL_UP);
    }

    private void playDeathSounds() {
        boolean animalDeath = false;
        boolean humanDeath = false;
        long newest = lastDeathSoundId;
        for (DeathEvent death : simulation.recentDeathEvents()) {
            if (death.id() <= lastDeathSoundId) {
                continue;
            }
            newest = Math.max(newest, death.id());
            humanDeath |= death.kind() == OrganismKind.HUMAN;
            animalDeath = true;
        }
        lastDeathSoundId = newest;
        if (humanDeath) {
            audio.play(SoundCue.HUMAN_DEATH);
        } else if (animalDeath) {
            audio.play(SoundCue.ANIMAL_DEATH);
        }
    }

    private void applyAudioSettings() {
        audio.applySettings(settings);
    }

    private void showIntroIfNeeded() {
        if (settings.introSeen()) {
            return;
        }
        settings.setIntroSeen(true);
        IntroDialog.show(this);
    }

    private void updateAudioTension() {
        if (training.levelFailed()) {
            audio.setTension(1.0);
        } else if (training.dangerWarning() != null) {
            audio.setTension(0.7);
        } else {
            audio.setTension(0.0);
        }
    }
}
