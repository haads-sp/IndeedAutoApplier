package com.haadlit_sp.appRenderLogic;

import javax.swing.*;

import com.haadlit_sp.appRenderLogic.pages.Page1;
import com.haadlit_sp.appRenderLogic.pages.Page2;
import com.haadlit_sp.appRenderLogic.pages.Page3;

import java.awt.*;

public class App {
    private JFrame frame;
    private JPanel cardPanel;   // Holds all "pages"
    private CardLayout cardLayout;

    public App() {

        frame = new JFrame("Multi-Page Swing App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        // Create CardLayout manager
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // Instantiate each page (separate classes)
        Page1 page1 = new Page1(this);
        Page2 page2 = new Page2(this);
        Page3 page3 = new Page3(this);

        // Register pages with unique names
        cardPanel.add(page1, "Page1");
        cardPanel.add(page2, "Page2");
        cardPanel.add(page3, "Page3");

        // Start on Page1
        showPage("Page1");

        frame.add(cardPanel);
        frame.setVisible(true);

    }

    // Method for switching pages
    public void showPage(String pageName) {
        cardLayout.show(cardPanel, pageName);
    }
}
