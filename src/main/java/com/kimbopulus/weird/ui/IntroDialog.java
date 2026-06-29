package com.kimbopulus.weird.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;

public final class IntroDialog extends JDialog {
    private IntroDialog(Window owner) {
        super(owner, "How to play", ModalityType.APPLICATION_MODAL);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(new Color(247, 243, 232));
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));

        JLabel title = new JLabel("Weird");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        content.add(title, BorderLayout.NORTH);

        JTextArea body = new JTextArea("""
Keep the board balanced.

Plants feed rabbits. Rabbits feed wolves. Humans plant more life. Bears pressure humans.
Rabbits lose energy every turn, so they can starve if they do not find food in time.

If any group gets too low or too high for too long, the level fails.
Rain and drought change a 4 x 4 patch at a time and also nudge temperature.
Lightning costs 50 tokens and strikes one exact creature.
The right panel shows the exact balance bands for the current level.

Click the Info button again anytime to reopen this guide.
""");
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setOpaque(false);
        body.setFont(body.getFont().deriveFont(Font.PLAIN, 16f));
        content.add(body, BorderLayout.CENTER);

        JButton close = new JButton("Got it");
        close.addActionListener(event -> dispose());
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(close, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);

        setContentPane(content);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(460, 340));
        pack();
        setLocationRelativeTo(owner);
    }

    public static void show(Window owner) {
        new IntroDialog(owner).setVisible(true);
    }
}
