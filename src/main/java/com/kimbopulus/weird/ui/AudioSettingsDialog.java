package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.settings.GameSettings;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;

public final class AudioSettingsDialog extends JDialog {
    private AudioSettingsDialog(Window owner, GameSettings settings, Runnable onChange) {
        super(owner, "Audio Settings", ModalityType.APPLICATION_MODAL);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(new Color(247, 243, 232));
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));

        JLabel title = new JLabel("Audio Settings");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        content.add(title, BorderLayout.NORTH);

        JPanel controls = new JPanel(new GridLayout(0, 1, 0, 8));
        controls.setOpaque(false);
        JCheckBox enabled = new JCheckBox("Music and sounds", settings.audioEnabled());
        enabled.setOpaque(false);
        controls.add(enabled);
        controls.add(new JLabel("Music volume"));
        JSlider music = new JSlider(0, 100, settings.musicVolume());
        music.setOpaque(false);
        controls.add(music);
        controls.add(new JLabel("Effect volume"));
        JSlider effects = new JSlider(0, 100, settings.effectsVolume());
        effects.setOpaque(false);
        controls.add(effects);
        content.add(controls, BorderLayout.CENTER);

        enabled.addActionListener(event -> {
            settings.setAudioEnabled(enabled.isSelected());
            onChange.run();
        });
        music.addChangeListener(event -> {
            settings.setMusicVolume(music.getValue());
            onChange.run();
        });
        effects.addChangeListener(event -> {
            settings.setEffectsVolume(effects.getValue());
            onChange.run();
        });

        JButton close = new JButton("Close");
        close.addActionListener(event -> dispose());
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(close, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);

        setContentPane(content);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(400, 300));
        pack();
        setLocationRelativeTo(owner);
    }

    public static void show(Window owner, GameSettings settings, Runnable onChange) {
        new AudioSettingsDialog(owner, settings, onChange).setVisible(true);
    }

    public static JDialog createForVisualCheck(Window owner, GameSettings settings) {
        return new AudioSettingsDialog(owner, settings, () -> {
        });
    }
}
