package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.progression.ProgressionProfile;
import com.kimbopulus.weird.progression.ShopItem;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.Window;

public final class ShopDialog extends JDialog {
    private static final Color BACKGROUND = new Color(247, 243, 232);
    private static final Color TEXT = new Color(38, 42, 38);
    private static final Color MUTED = new Color(92, 96, 88);

    private final ProgressionProfile profile;
    private final Runnable onPurchase;
    private final JLabel tokensLabel = new JLabel();
    private final JPanel itemsPanel = new JPanel();

    private ShopDialog(Window owner, ProgressionProfile profile, Runnable onPurchase) {
        super(owner, "Terrarium Shop", ModalityType.APPLICATION_MODAL);
        this.profile = profile;
        this.onPurchase = onPurchase;

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));

        JPanel header = new JPanel(new GridLayout(0, 1, 0, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("Terrarium Shop");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(TEXT);
        tokensLabel.setFont(tokensLabel.getFont().deriveFont(Font.BOLD, 14f));
        tokensLabel.setForeground(new Color(75, 101, 67));
        header.add(title);
        header.add(tokensLabel);
        content.add(header, BorderLayout.NORTH);

        itemsPanel.setOpaque(false);
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        content.add(itemsPanel, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(event -> dispose());
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(close, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);

        setContentPane(content);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(480, 330));
        refresh();
        pack();
        setLocationRelativeTo(owner);
    }

    public static void show(Window owner, ProgressionProfile profile, Runnable onPurchase) {
        new ShopDialog(owner, profile, onPurchase).setVisible(true);
    }

    public static JDialog createForVisualCheck(Window owner, ProgressionProfile profile) {
        return new ShopDialog(owner, profile, () -> {
        });
    }

    private void refresh() {
        tokensLabel.setText("Tokens: " + profile.tokens());
        itemsPanel.removeAll();
        for (ShopItem item : ShopItem.values()) {
            itemsPanel.add(createItemRow(item));
        }
        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    private JPanel createItemRow(ShopItem item) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 208, 190)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        row.setPreferredSize(new Dimension(430, 70));

        JPanel text = new JPanel(new GridLayout(0, 1, 0, 2));
        text.setOpaque(false);
        JLabel name = new JLabel(item.title());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 14f));
        name.setForeground(TEXT);
        JLabel description = new JLabel(item.description());
        description.setForeground(MUTED);
        text.add(name);
        text.add(description);
        row.add(text, BorderLayout.CENTER);

        boolean owned = profile.owns(item);
        JButton buy = new JButton(owned ? "Owned" : item.cost() + " tokens");
        buy.setEnabled(!owned && profile.tokens() >= item.cost());
        buy.addActionListener(event -> {
            if (profile.buy(item)) {
                onPurchase.run();
                refresh();
            }
        });
        JPanel buttonWrap = new JPanel(new GridBagLayout());
        buttonWrap.setOpaque(false);
        buy.setPreferredSize(new Dimension(100, 34));
        buttonWrap.add(buy);
        row.add(buttonWrap, BorderLayout.EAST);
        return row;
    }
}
