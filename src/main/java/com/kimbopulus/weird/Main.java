package com.kimbopulus.weird;

import com.kimbopulus.weird.ui.TerrariumFrame;

import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TerrariumFrame().setVisible(true));
    }
}
