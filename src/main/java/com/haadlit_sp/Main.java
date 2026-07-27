package com.haadlit_sp;


import com.haadlit_sp.appRenderLogic.App;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.*;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;


public class Main {

    private static final Logger LOG = System.getLogger(Main.class.getName());

    public static void main(String[] args) throws IOException {

        // Crash net: an unexpected throw on any thread is logged and surfaced once, not swallowed
        // to the console. Boundaries still catch their own errors; this only backstops the misses.
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            LOG.log(Level.ERROR, "Uncaught error on " + thread.getName(), error);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Something went wrong: " + shortMessage(error)
                            + "\nThe app is still running; you can keep going or restart.",
                    "Unexpected error", JOptionPane.ERROR_MESSAGE));
        });

        SwingUtilities.invokeLater(() -> {
            Theme.install();   // must run before any component is created
            new App();
        });

    }

    private static String shortMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
