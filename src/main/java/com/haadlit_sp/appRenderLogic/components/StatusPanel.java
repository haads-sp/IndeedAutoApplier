package com.haadlit_sp.appRenderLogic.components;

import com.haadlit_sp.appCoreLogic.model.RunStatus;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;


/** Live run status. Narrow API: {@link #setStatus(RunStatus)}. */
public class StatusPanel extends JPanel {

    private final JLabel state = Theme.heading("Idle");
    private final JLabel current = Theme.body("—");
    private final JLabel message = Theme.muted(" ");
    private final Counter submitted = new Counter("Submitted", Theme.SUCCESS);
    private final Counter skipped = new Counter("Skipped", Theme.TEXT_MUTED);
    private final Counter failed = new Counter("Failed", Theme.DANGER);

    public StatusPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, Theme.SPACE_LG));
        add(buildNow(), BorderLayout.NORTH);
        add(buildCounters(), BorderLayout.CENTER);
    }

    private JPanel buildNow() {
        JPanel p = new JPanel(new BorderLayout(0, Theme.SPACE_XS));
        p.setOpaque(false);
        p.add(state, BorderLayout.NORTH);
        p.add(current, BorderLayout.CENTER);
        p.add(message, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildCounters() {
        JPanel p = new JPanel(new GridLayout(1, 3, Theme.SPACE_MD, 0));
        p.setOpaque(false);
        p.add(submitted);
        p.add(skipped);
        p.add(failed);
        return p;
    }

    public void setStatus(RunStatus s) {
        state.setText(!s.running() ? "Idle" : s.paused() ? "Paused" : "Running");
        state.setForeground(!s.running() ? Theme.TEXT
                : s.paused() ? Theme.WARNING : Theme.SUCCESS);
        current.setText(s.currentPosting());
        message.setText(s.lastMessage().isEmpty() ? " " : s.lastMessage());
        submitted.set(s.submitted());
        skipped.set(s.skipped());
        failed.set(s.failed());
    }

    /** A count is data, so it gets room to be read at a glance. */
    private static final class Counter extends JPanel {

        private final JLabel value = Theme.title("0");
        private final java.awt.Color activeColor;

        Counter(String caption, java.awt.Color activeColor) {
            this.activeColor = activeColor;
            setOpaque(true);
            setBackground(Theme.SURFACE_SUNK);
            setLayout(new BorderLayout(0, Theme.SPACE_XS));
            setBorder(Theme.pad(Theme.SPACE_MD, Theme.SPACE_LG, Theme.SPACE_MD, Theme.SPACE_LG));
            value.setForeground(Theme.TEXT_FAINT);
            add(value, BorderLayout.NORTH);
            add(Theme.eyebrow(caption), BorderLayout.SOUTH);
        }

        /** Colour is a signal, so a count only takes its colour once there is something to report. */
        void set(int count) {
            value.setText(String.valueOf(count));
            value.setForeground(count > 0 ? activeColor : Theme.TEXT_FAINT);
        }
    }
}
