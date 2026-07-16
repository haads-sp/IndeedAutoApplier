package com.haadlit_sp;


import com.haadlit_sp.appRenderLogic.App;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.*;
import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {

        SwingUtilities.invokeLater(() -> {
            Theme.install();   // must run before any component is created
            new App();
        });

    }
}
