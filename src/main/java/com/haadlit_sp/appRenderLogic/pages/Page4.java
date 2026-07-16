package com.haadlit_sp.appRenderLogic.pages;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.haadlit_sp.appRenderLogic.App;
import com.haadlit_sp.appRenderLogic.components.Header;
import com.haadlit_sp.appRenderLogic.components.HistoryPanel;
import com.haadlit_sp.appRenderLogic.components.StatusPanel;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import java.awt.BorderLayout;


/** Run page — start/pause/stop, live status, and the session history. */
public class Page4 extends JPanel implements LivePage {

    private final App app;
    private final PageUtil pageUtil = new PageUtil();

    private final StatusPanel statusPanel = new StatusPanel();
    private final HistoryPanel historyPanel = new HistoryPanel();
    private final JButton startBtn = Theme.primaryButton("Start applying");
    private final JButton pauseBtn = Theme.secondaryButton("Pause");
    private final JButton stopBtn = Theme.secondaryButton("Stop");

    public Page4(App app) {
        this.app = app;

        setBackground(Theme.CANVAS);
        setLayout(new BorderLayout());
        setBorder(Theme.pad(Theme.SPACE_XXL));

        add(new Header("Run", "Start applying. You can pause or stop at any time."),
                BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(pageUtil.stepNav(app, "Page3", null, null), BorderLayout.SOUTH);

        wireControls();
        refresh();
    }

    private JComponent buildBody() {
        JComponent console = Theme.card(
                Theme.row(startBtn, pauseBtn, stopBtn),
                Theme.vGap(Theme.SPACE_XL),
                statusPanel);

        JPanel body = new JPanel(new BorderLayout(0, Theme.SPACE_LG));
        body.setOpaque(false);
        body.add(console, BorderLayout.NORTH);
        body.add(historyPanel, BorderLayout.CENTER);
        return body;
    }

    private void wireControls() {
        startBtn.addActionListener(e -> app.core().startRun());
        pauseBtn.addActionListener(e -> app.core().pauseRun());
        stopBtn.addActionListener(e -> app.core().stopRun());
    }

    @Override
    public void refresh() {
        statusPanel.setStatus(app.core().status());
        historyPanel.setHistory(app.core().history());
    }
}
