package com.haadlit_sp.appRenderLogic.components;

import com.haadlit_sp.appCoreLogic.model.RunStatus;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridLayout;


/** Live run status. Narrow API: {@link #setStatus(RunStatus)}. */
public class StatusPanel extends JPanel {

    private final JLabel state   = Theme.heading("Idle");
    private final JLabel current = Theme.body("Current: —");
    private final JLabel counts  = Theme.body("Submitted 0   ·   Skipped 0   ·   Failed 0");
    private final JLabel message = Theme.muted(" ");

    public StatusPanel() {
        setOpaque(true);
        setBackground(Theme.SURFACE);
        setLayout(new GridLayout(0, 1, 0, Theme.GAP / 2));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER), Theme.pad(Theme.PAD)));
        add(state);
        add(current);
        add(counts);
        add(message);
    }

    public void setStatus(RunStatus s) {
        state.setText(!s.running() ? "Idle" : s.paused() ? "Paused" : "Running");
        state.setForeground(s.running() && !s.paused() ? Theme.SUCCESS : Theme.TEXT);
        current.setText("Current: " + s.currentPosting());
        counts.setText(String.format("Submitted %d   ·   Skipped %d   ·   Failed %d",
                s.submitted(), s.skipped(), s.failed()));
        message.setText(s.lastMessage().isEmpty() ? " " : s.lastMessage());
    }
}
