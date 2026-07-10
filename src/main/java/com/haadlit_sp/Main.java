package com.haadlit_sp;


import com.haadlit_sp.appRenderLogic.App;

import javax.swing.*;
import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {

        SwingUtilities.invokeLater(App::new);

    }
}