package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.audio.AudioEngine;
import com.kimbopulus.weird.audio.SoundCue;
import com.kimbopulus.weird.sim.BirthEvent;
import com.kimbopulus.weird.sim.OrganismKind;
import com.kimbopulus.weird.sim.DeathCause;
import com.kimbopulus.weird.sim.DeathEvent;
import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;
import com.kimbopulus.weird.sim.WorldEvent;
import com.kimbopulus.weird.progression.ShopItem;
import com.kimbopulus.weird.settings.GameSettings;
import com.kimbopulus.weird.training.TrainingSession;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.AbstractButton;
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
import java.util.Arrays;
import java.util.Comparator;
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
    private long lastPopupDeathId;
    private long lastPopupBirthId;
    private Position hoveredBoardPosition;
    private WorldEvent lastWorldEvent = WorldEvent.CALM;

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
                String targetDescription = terrariumPanel.describe(position);
                if (toolMode == ToolMode.LIGHTNING) {
                    if (simulation.organismAt(position) == null) {
                        terrariumPanel.showBanner("Lightning needs a target");
                        return;
                    }
                    if (!training.progression().spendTokens(toolMode.tokenCost())) {
                        terrariumPanel.showBanner("Need 10 tokens");
                        return;
                    }
                }
                if (!toolMode.apply(simulation, position)) {
                    return;
                }
                applyPurchasedUpgrade(position);
                playToolSound();
                terrariumPanel.showToolEffect(position, toolMode);
                showToolMechanicTip(toolMode);
                training.noteAction(toolMode.label(), targetDescription);
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
                hoveredBoardPosition = position;
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
        updateToolHint(hoveredBoardPosition);
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
        ToolMode[] modes = orderedToolModes();
        JPanel toolButtons = new JPanel(new GridLayout(1, modes.length, 6, 0));
        toolButtons.setOpaque(false);
        int shortcut = 1;
        for (ToolMode mode : modes) {
            JToggleButton button = new JToggleButton(mode.label());
            button.setFocusable(false);
            button.setToolTipText(shortcutLabel(shortcut) + ": " + mode.description());
            installHoverHint(button);
            button.addActionListener(event -> {
                toolMode = mode;
                updateToolHint(hoveredBoardPosition);
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
        pauseButton.setToolTipText("Pause or resume the simulation.");
        installHoverHint(pauseButton);
        pauseButton.addActionListener(event -> togglePause());
        toolbar.add(pauseButton);

        JButton stepButton = new JButton("Step");
        stepButton.setFocusable(false);
        stepButton.setToolTipText("Advance the simulation by one tick.");
        installHoverHint(stepButton);
        stepButton.addActionListener(event -> stepSimulation());
        toolbar.add(stepButton);

        JButton restartButton = new JButton("Restart");
        restartButton.setFocusable(false);
        restartButton.setToolTipText("Restart the current level from the beginning.");
        installHoverHint(restartButton);
        restartButton.addActionListener(event -> restart());
        toolbar.add(restartButton);

        JButton audioButton = new JButton("Audio");
        audioButton.setFocusable(false);
        audioButton.setToolTipText("Set music and effect volume.");
        installHoverHint(audioButton);
        audioButton.addActionListener(event -> AudioSettingsDialog.show(this, settings, this::applyAudioSettings));
        toolbar.add(audioButton);

        JButton infoButton = new JButton("Info");
        infoButton.setFocusable(false);
        infoButton.setToolTipText("Show the short game guide again.");
        installHoverHint(infoButton);
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
        simulation.tick();
        training.update(simulation);
        terrariumPanel.syncDeathEffects();
        terrariumPanel.syncBirthEffects();
        playDeathSounds();
        showPassiveMechanicTips();
        updateAudioTension();
        if (!wasComplete && training.levelComplete()) {
            terrariumPanel.showBanner("Level complete: +" + training.lastLevelReward());
        } else if (!wasFailed && training.levelFailed()) {
            training.progression().resetPurchases();
            terrariumPanel.showBanner("LEVEL LOST: " + training.failureReason());
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
        training.progression().resetPurchases();
        simulation.restart();
        training.reset();
        terrariumPanel.resetMechanicPopups();
        lastDeathSoundId = 0L;
        lastPopupDeathId = 0L;
        lastPopupBirthId = 0L;
        lastWorldEvent = simulation.currentEvent();
        updateAudioTension();
        terrariumPanel.repaint();
        trainingPanel.refresh();
        updateToolAvailability();
        updateStatus();
        updateToolHint(hoveredBoardPosition);
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

    private void installHoverHint(AbstractButton button) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                String hint = button.getToolTipText();
                toolHintLabel.setText(hint == null || hint.isBlank() ? button.getText() : hint);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                updateToolHint(hoveredBoardPosition);
            }
        });
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
                updateToolHint(hoveredBoardPosition);
            }
        } else {
            sanctuaryButton.setToolTipText(shortcutLabel(toolButtons.size()) + ": " + ToolMode.SANCTUARY.description());
        }
    }

    private void installKeyBindings() {
        JComponent root = getRootPane();
        ToolMode[] modes = orderedToolModes();
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
        updateToolHint(hoveredBoardPosition);
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
        training.progression().resetPurchases();
        simulation.restart();
        terrariumPanel.resetMechanicPopups();
        lastDeathSoundId = 0L;
        lastPopupDeathId = 0L;
        lastPopupBirthId = 0L;
        lastWorldEvent = simulation.currentEvent();
        updateAudioTension();
        terrariumPanel.showBanner("Level restarted");
        terrariumPanel.repaint();
        trainingPanel.refresh();
        updateStatus();
        updateToolAvailability();
    }

    private void playToolSound() {
        if (toolMode == ToolMode.RAIN || toolMode == ToolMode.DROUGHT || toolMode == ToolMode.COMPOST) {
            return;
        }
        if (toolMode == ToolMode.LIGHTNING) {
            audio.play(SoundCue.LIGHTNING);
            return;
        }
        audio.play(SoundCue.PLACE);
    }

    private String shortcutLabel(int index) {
        return index == 10 ? "0" : Integer.toString(index);
    }

    private char shortcutKey(int index) {
        return index == 10 ? '0' : Character.forDigit(index, 10);
    }

    private void celebrateLevel() {
        terrariumPanel.showLevelUp("LEVEL " + training.levelNumber() + "  " + training.levelTitle());
    }

    private void playDeathSounds() {
        boolean humanDeath = false;
        boolean animalDeath = false;
        long newest = lastDeathSoundId;
        for (DeathEvent death : simulation.recentDeathEvents()) {
            if (death.id() <= lastDeathSoundId) {
                continue;
            }
            newest = Math.max(newest, death.id());
            if (death.cause() == DeathCause.LIGHTNING) {
                continue;
            }
            if (death.kind() == OrganismKind.HUMAN) {
                humanDeath = true;
            } else {
                animalDeath = true;
            }
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

    private void showToolMechanicTip(ToolMode mode) {
        switch (mode) {
            case RAIN -> terrariumPanel.showMechanicPopup(
                    "tool-rain",
                    "Rain affects a 4 x 4 patch",
                    "It cools that patch hard right away and keeps cooling it while delayed growth builds."
            );
            case DROUGHT -> terrariumPanel.showMechanicPopup(
                    "tool-drought",
                    "Drought lingers after the click",
                    "It heavily dries and warms a 4 x 4 patch. Clicking directly on a creature also kills it."
            );
            case COMPOST -> terrariumPanel.showMechanicPopup(
                    "tool-compost",
                    "Compost boosts one square",
                    "It raises fertility right away. Overuse can cause a local plant spike."
            );
            case HUMAN -> terrariumPanel.showMechanicPopup(
                    "tool-human",
                    "Humans roam and defend themselves",
                    "They move over plants without crushing them, sometimes plant nearby soil, breed once when they meet, and stab wolves that step next to them."
            );
            case BEAR -> terrariumPanel.showMechanicPopup(
                    "tool-bear",
                    "Bears hunt humans",
                    "A bear keeps chasing humans until it has eaten 2 of them, then it leaves."
            );
            case RABBIT -> terrariumPanel.showMechanicPopup(
                    "tool-rabbit",
                    "This tool places a male rabbit",
                    "Males breed once. When one reaches a female, 3 female rabbits can appear."
            );
            case WOLF -> terrariumPanel.showMechanicPopup(
                    "tool-wolf",
                    "Wolves hunt hard and leave full",
                    "They chase rabbits, can breed once, and leave the map after 3 rabbit kills."
            );
            case LIGHTNING -> terrariumPanel.showMechanicPopup(
                    "tool-lightning",
                    "Lightning hits one exact creature",
                    "It costs 10 tokens and only works if you click directly on a living target."
            );
            case SANCTUARY -> terrariumPanel.showMechanicPopup(
                    "tool-sanctuary",
                    "Sanctuary protects a 2 x 2 patch",
                    "That soil resists seasons and global weather, so it is useful as an anchor area."
            );
        }
    }

    private void showPassiveMechanicTips() {
        WorldEvent event = simulation.currentEvent();
        if (event != lastWorldEvent) {
            lastWorldEvent = event;
            if (event != WorldEvent.CALM) {
                terrariumPanel.showMechanicPopup("event-" + event.name(), event.title(), event.description());
            }
        }

        if (training.dangerWarning() != null) {
            terrariumPanel.showMechanicPopup(
                    "danger-timer",
                    "Red warning means danger is ticking",
                    "If one band stays out of range for 30 seconds, the level is lost."
            );
        }

        for (DeathEvent death : simulation.recentDeathEvents()) {
            if (death.id() <= lastPopupDeathId) {
                continue;
            }
            lastPopupDeathId = Math.max(lastPopupDeathId, death.id());
            maybeShowDeathTip(death);
        }

        for (BirthEvent birth : simulation.recentBirthEvents()) {
            if (birth.id() <= lastPopupBirthId) {
                continue;
            }
            lastPopupBirthId = Math.max(lastPopupBirthId, birth.id());
            maybeShowBirthTip(birth);
        }
    }

    private void maybeShowDeathTip(DeathEvent death) {
        if (death.kind() == OrganismKind.RABBIT && death.cause() == DeathCause.NATURAL) {
            terrariumPanel.showMechanicPopup(
                    "rabbit-starvation",
                    "Rabbits can starve",
                    "Rabbits lose energy every tick. If they do not reach plants in time, they die on their own."
            );
            return;
        }
        if (death.kind() == OrganismKind.WOLF && death.cause() == DeathCause.NATURAL) {
            terrariumPanel.showMechanicPopup(
                    "wolf-starvation",
                    "Wolves can starve too",
                    "Wolves burn energy fast. If rabbits thin out, wolf numbers can collapse."
            );
            return;
        }
        if (death.kind() == OrganismKind.WOLF && death.cause() == DeathCause.HUMAN_ATTACK) {
            terrariumPanel.showMechanicPopup(
                    "human-vs-wolf",
                    "Humans kill nearby wolves",
                    "If a wolf steps next to a human, the human can stab it before moving."
            );
            return;
        }
        if (death.kind() == OrganismKind.HUMAN && death.cause() == DeathCause.NATURAL) {
            terrariumPanel.showMechanicPopup(
                    "bear-kill",
                    "That human was killed by a bear",
                    "Bear kills count as natural deaths here. Bears leave after 2 human kills."
            );
        }
    }

    private void maybeShowBirthTip(BirthEvent birth) {
        if (birth.kind() == OrganismKind.RABBIT) {
            terrariumPanel.showMechanicPopup(
                    "rabbit-birth",
                    "Rabbit breeding spike",
                    "A male rabbit can trigger a one-time burst of 3 female rabbits when he reaches a female."
            );
            return;
        }
        if (birth.kind() == OrganismKind.WOLF) {
            terrariumPanel.showMechanicPopup(
                    "wolf-birth",
                    "Wolves can reproduce",
                    "When two wolves meet and there is room, they can add 1 extra wolf."
            );
            return;
        }
        if (birth.kind() == OrganismKind.HUMAN) {
            terrariumPanel.showMechanicPopup(
                    "human-birth",
                    "Humans can add one more human",
                    "The first time 2 humans meet, that pair can create 1 extra human if a nearby cell is empty."
            );
        }
    }

    private ToolMode[] orderedToolModes() {
        return Arrays.stream(ToolMode.values())
                .sorted(Comparator.comparing(ToolMode::label))
                .toArray(ToolMode[]::new);
    }
}
