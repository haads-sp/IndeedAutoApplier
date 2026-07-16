package com.haadlit_sp.appRenderLogic.components;

import com.haadlit_sp.appRenderLogic.theme.AppInfo;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


/**
 * The persistent rail down the left: brand, then the four steps with live state.
 *
 * <p>The app genuinely is a sequence — sign in, then documents, then location, then run — so the
 * order carries real information and is worth showing. Painted rather than composed from widgets:
 * the connector between dots is the whole point, and Swing has no component for it.
 */
public class StepRail extends JPanel {

    public enum State { TODO, CURRENT, DONE }

    private static final int WIDTH = 216;
    private static final int ROW_HEIGHT = 44;
    private static final int DOT_X = 28;
    private static final int DOT_R = 5;
    private static final int LABEL_X = 48;
    private static final int FIRST_ROW_Y = 132;

    private final LinkedHashMap<String, String> steps;
    private final Map<String, State> states = new LinkedHashMap<>();

    /**
     * @param steps    ordered map of page key to label
     * @param onSelect called with the page key when a step is clicked
     */
    public StepRail(LinkedHashMap<String, String> steps, Consumer<String> onSelect) {
        this.steps = steps;
        steps.keySet().forEach(key -> states.put(key, State.TODO));

        setOpaque(true);
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(WIDTH, 0));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String key = stepAt(e.getY());
                if (key != null) {
                    onSelect.accept(key);
                }
            }
        });
    }

    /** Narrow API: which step is open, and which are finished. */
    public void setState(String currentKey, Set<String> doneKeys) {
        Map<String, State> next = new LinkedHashMap<>();
        for (String key : steps.keySet()) {
            next.put(key, key.equals(currentKey) ? State.CURRENT
                    : doneKeys.contains(key) ? State.DONE : State.TODO);
        }
        if (!next.equals(states)) { // repaint only on real change
            states.putAll(next);
            repaint();
        }
    }

    private String stepAt(int y) {
        List<String> keys = List.copyOf(steps.keySet());
        int index = (y - FIRST_ROW_Y + ROW_HEIGHT / 2) / ROW_HEIGHT;
        return index >= 0 && index < keys.size() ? keys.get(index) : null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        paintBrand(g2);
        paintSteps(g2);

        g2.setColor(Theme.LINE);
        g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
        g2.dispose();
    }

    private void paintBrand(Graphics2D g2) {
        g2.setFont(Theme.TITLE);
        g2.setColor(Theme.TEXT);
        g2.drawString("Indeed", DOT_X, 56);
        g2.setColor(Theme.ACCENT);
        g2.drawString("Auto-Applier", DOT_X, 82);

        g2.setFont(Theme.MONO);
        g2.setColor(Theme.TEXT_FAINT);
        g2.drawString("v" + AppInfo.VERSION, DOT_X, 102);
    }

    private void paintSteps(Graphics2D g2) {
        List<String> keys = List.copyOf(steps.keySet());
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            State state = states.getOrDefault(key, State.TODO);
            int y = FIRST_ROW_Y + i * ROW_HEIGHT;

            if (i < keys.size() - 1) {
                g2.setColor(state == State.DONE ? Theme.ACCENT : Theme.LINE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(DOT_X, y + DOT_R + 4, DOT_X, y + ROW_HEIGHT - DOT_R - 4);
            }
            paintDot(g2, y, state);

            g2.setFont(state == State.CURRENT ? Theme.BODY_STRONG : Theme.BODY);
            g2.setColor(switch (state) {
                case CURRENT -> Theme.TEXT;
                case DONE -> Theme.TEXT_MUTED;
                case TODO -> Theme.TEXT_FAINT;
            });
            g2.drawString(steps.get(key), LABEL_X, y + 5);
        }
    }

    private void paintDot(Graphics2D g2, int y, State state) {
        int d = DOT_R * 2;
        switch (state) {
            case DONE -> {
                g2.setColor(Theme.ACCENT);
                g2.fillOval(DOT_X - DOT_R, y - DOT_R, d, d);
                g2.setColor(Theme.ON_ACCENT);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(DOT_X - 2, y, DOT_X - 1, y + 2);
                g2.drawLine(DOT_X - 1, y + 2, DOT_X + 3, y - 3);
            }
            case CURRENT -> {
                g2.setColor(Theme.ACCENT_SOFT);
                g2.fillOval(DOT_X - DOT_R - 3, y - DOT_R - 3, d + 6, d + 6);
                g2.setColor(Theme.ACCENT);
                g2.fillOval(DOT_X - DOT_R, y - DOT_R, d, d);
            }
            case TODO -> {
                g2.setColor(Theme.SURFACE);
                g2.fillOval(DOT_X - DOT_R, y - DOT_R, d, d);
                g2.setColor(Theme.LINE_STRONG);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(DOT_X - DOT_R, y - DOT_R, d, d);
            }
        }
    }
}
