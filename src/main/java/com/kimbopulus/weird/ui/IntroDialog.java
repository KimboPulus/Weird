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

Plants feed rabbits. Rabbits feed wolves.
Humans plant and can add 1 more human once.
Bears hunt humans.

Rabbits and wolves can starve.
Rain and Drought hit a 4 x 4 patch.
Rain on active drought soil cools the whole board by 1 C.
Drought twice on one patch heats the whole board by 2 C.
Direct Drought clicks kill animals on that square.
Lightning costs 10 tokens.
Plant warnings allow 60 seconds. Other red warnings allow 30 seconds.

Use the How to play button in the top bar anytime to reopen this guide.
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
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(460, 340));
        pack();
        setLocationRelativeTo(owner);
    }

    public static void show(Window owner) {
        new IntroDialog(owner).setVisible(true);
    }
}
