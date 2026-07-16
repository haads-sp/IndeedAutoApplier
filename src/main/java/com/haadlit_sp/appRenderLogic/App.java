package com.haadlit_sp.appRenderLogic;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.haadlit_sp.appCoreLogic.AppCore;
import com.haadlit_sp.appRenderLogic.components.StepRail;
import com.haadlit_sp.appRenderLogic.pages.LivePage;
import com.haadlit_sp.appRenderLogic.pages.Page1;
import com.haadlit_sp.appRenderLogic.pages.Page2;
import com.haadlit_sp.appRenderLogic.pages.Page3;
import com.haadlit_sp.appRenderLogic.pages.Page4;
import com.haadlit_sp.appRenderLogic.theme.AppInfo;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class App {
    private static final int UI_REFRESH_MS = 500;

    private final JFrame frame;
    private final JPanel cardPanel;   // Holds all "pages"
    private final CardLayout cardLayout;
    private final AppCore core = new AppCore();   // The one facade the UI talks to
    private final Map<String, JPanel> pages = new LinkedHashMap<>();
    private final StepRail rail;
    private String currentPage = "";

    public App() {

        frame = new JFrame(AppInfo.NAME);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1120, 780);
        frame.setMinimumSize(new java.awt.Dimension(920, 640));
        frame.setLocationRelativeTo(null);

        // Close the browser and void the session when the window closes.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                core.shutdown();
            }
        });

        // Create CardLayout manager
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(Theme.CANVAS);

        // Instantiate each page (separate classes)
        addPage("Page1", new Page1(this));   // Sign in
        addPage("Page2", new Page2(this));   // Documents & job target
        addPage("Page3", new Page3(this));   // Location
        addPage("Page4", new Page4(this));   // Run

        rail = new StepRail(stepLabels(), this::showPage);

        // Start on Page1
        showPage("Page1");

        startUiTimer();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CANVAS);
        root.add(rail, BorderLayout.WEST);
        root.add(cardPanel, BorderLayout.CENTER);
        frame.setContentPane(root);
        frame.setVisible(true);

    }

    private static LinkedHashMap<String, String> stepLabels() {
        LinkedHashMap<String, String> steps = new LinkedHashMap<>();
        steps.put("Page1", "Sign in");
        steps.put("Page2", "Documents");
        steps.put("Page3", "Location");
        steps.put("Page4", "Run");
        return steps;
    }

    private void addPage(String name, JPanel page) {
        pages.put(name, page);
        cardPanel.add(page, name);
    }

    /** The single timer driving every live UI update; only the visible page does work. */
    private void startUiTimer() {
        new Timer(UI_REFRESH_MS, e -> {
            if (pages.get(currentPage) instanceof LivePage live) {
                live.refresh();
            }
            rail.setState(currentPage, completedSteps());
        }).start();
    }

    /** A step is done once the thing it asks for actually exists. */
    private Set<String> completedSteps() {
        Set<String> done = new LinkedHashSet<>();
        if (core.isLoggedIn()) {
            done.add("Page1");
        }
        if (core.documents().hasResume() && !core.searchCriteria().jobQuery().isBlank()) {
            done.add("Page2");
        }
        if (core.searchCriteria().isReady()) {
            done.add("Page3");
        }
        return done;
    }

    /** The single core facade the UI is allowed to call. */
    public AppCore core() {
        return core;
    }

    // Method for switching pages
    public void showPage(String pageName) {
        currentPage = pageName;
        cardLayout.show(cardPanel, pageName);
    }
}
