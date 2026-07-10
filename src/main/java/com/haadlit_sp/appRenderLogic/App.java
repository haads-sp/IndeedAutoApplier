package com.haadlit_sp.appRenderLogic;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.haadlit_sp.appCoreLogic.AppCore;
import com.haadlit_sp.appRenderLogic.pages.Page1;
import com.haadlit_sp.appRenderLogic.pages.Page2;
import com.haadlit_sp.appRenderLogic.pages.Page3;
import com.haadlit_sp.appRenderLogic.pages.Page4;
import com.haadlit_sp.appRenderLogic.theme.AppInfo;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import java.awt.CardLayout;

public class App {
    private final JFrame frame;
    private final JPanel cardPanel;   // Holds all "pages"
    private final CardLayout cardLayout;
    private final AppCore core = new AppCore();   // The one facade the UI talks to

    public App() {

        frame = new JFrame(AppInfo.NAME + "  v" + AppInfo.VERSION);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 640);
        frame.setLocationRelativeTo(null);

        // Create CardLayout manager
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(Theme.BG);

        // Instantiate each page (separate classes)
        cardPanel.add(new Page1(this), "Page1");   // Sign in
        cardPanel.add(new Page2(this), "Page2");   // Documents & job target
        cardPanel.add(new Page3(this), "Page3");   // Location
        cardPanel.add(new Page4(this), "Page4");   // Run

        // Start on Page1
        showPage("Page1");

        frame.setContentPane(cardPanel);
        frame.setVisible(true);

    }

    /** The single core facade the UI is allowed to call. */
    public AppCore core() {
        return core;
    }

    // Method for switching pages
    public void showPage(String pageName) {
        cardLayout.show(cardPanel, pageName);
    }
}
