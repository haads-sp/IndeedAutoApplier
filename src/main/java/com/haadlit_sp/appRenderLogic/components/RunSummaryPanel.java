package com.haadlit_sp.appRenderLogic.components;

import com.haadlit_sp.appCoreLogic.model.RunSummary;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;


/**
 * The scoreboard for a run: what was found, what was skipped, and — the number that actually
 * matters — how many applications were confirmed as submitted. Updates live while a run walks the
 * list. Narrow API: {@link #setSummary}.
 */
public class RunSummaryPanel extends JPanel {

    private final Tile found = new Tile("Found", Theme.TEXT);
    private final Tile skipped = new Tile("Skipped", Theme.TEXT_MUTED);
    private final Tile attempted = new Tile("Attempted", Theme.TEXT);
    private final Tile abandoned = new Tile("Abandoned", new Color(0xB3, 0x38, 0x2E));
    private final Tile needsInput = new Tile("Needs you", new Color(0xB8, 0x7A, 0x00));
    private final Tile finished = new Tile("Finished", Theme.TEXT);
    private final Tile submitted = new Tile("Submitted", Theme.ACCENT);
    private final Tile verified = new Tile("Confirmed", Theme.SUCCESS);

    public RunSummaryPanel() {
        setOpaque(false);
        setLayout(new GridLayout(1, 8, Theme.SPACE_SM, 0));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Tile tile : new Tile[]{found, skipped, attempted, abandoned, needsInput,
                finished, submitted, verified}) {
            add(tile);
        }
        setSummary(RunSummary.empty());
    }

    /** Stay one row tall inside a vertical card — BoxLayout would otherwise stretch the grid. */
    @Override
    public java.awt.Dimension getMaximumSize() {
        return new java.awt.Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    public void setSummary(RunSummary summary) {
        found.set(summary.found());
        skipped.set(summary.skipped());
        attempted.set(summary.attempted());
        abandoned.set(summary.abandoned());
        needsInput.set(summary.needsInput());
        finished.set(summary.finished());
        submitted.set(summary.submitted());
        verified.set(summary.verified());
    }

    /** One number over its label — a stat tile, not a table row, so the run reads at a glance. */
    private static final class Tile extends JPanel {

        private final JLabel value = Theme.heading("0");

        Tile(String caption, Color accent) {
            setOpaque(false);
            setLayout(new java.awt.BorderLayout(0, 2));
            value.setForeground(accent);
            add(value, java.awt.BorderLayout.NORTH);
            add(Theme.eyebrow(caption), java.awt.BorderLayout.CENTER);
        }

        void set(int number) {
            value.setText(String.valueOf(number));
        }
    }
}
