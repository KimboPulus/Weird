package com.kimbopulus.weird.support;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public final class ErrorReporter {
    private ErrorReporter() {
    }

    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            writeLog(thread, error);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    "Weird encountered an error. Details were saved to data/error.log.",
                    "Unexpected error",
                    JOptionPane.ERROR_MESSAGE
            ));
        });
    }

    private static void writeLog(Thread thread, Throwable error) {
        try {
            Path path = Path.of("data", "error.log");
            Files.createDirectories(path.getParent());
            StringWriter stackTrace = new StringWriter();
            error.printStackTrace(new PrintWriter(stackTrace));
            String entry = System.lineSeparator()
                    + LocalDateTime.now() + " [" + thread.getName() + "]"
                    + System.lineSeparator() + stackTrace;
            Files.writeString(
                    path,
                    entry,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            // There is no safe recovery path if even the error log cannot be written.
        }
    }
}
