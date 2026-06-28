package com.kimbopulus.weird;

import com.kimbopulus.weird.ui.TerrariumFrame;
import com.kimbopulus.weird.support.ErrorReporter;

import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        ErrorReporter.install();
        SwingUtilities.invokeLater(() -> new TerrariumFrame().setVisible(true));
    }
}
