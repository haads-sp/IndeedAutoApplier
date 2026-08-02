package com.haadlit_sp.appRenderLogic.theme;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * THE design system: every colour, font, spacing step, radius and styled widget lives here.
 *
 * <p>Direction — "instrument panel": the app is a four-step console that signs you in, learns what
 * you want, then dispatches applications. So the palette is graphite neutrals carrying a single
 * petrol accent, status colours are reserved for real state (running / paused / failed), and
 * anything that is genuinely data (counts, radii, versions) is set in the mono face.
 * The accent is deliberately NOT Indeed's blue — this is the user's tool, not a copy of Indeed.
 */
public final class Theme {

    private Theme() {}

    // ---- Palette: graphite neutrals ----
    public static final Color CANVAS       = new Color(0xF4, 0xF6, 0xF8);
    public static final Color SURFACE      = new Color(0xFF, 0xFF, 0xFF);
    public static final Color SURFACE_SUNK = new Color(0xEC, 0xEF, 0xF3);
    public static final Color LINE         = new Color(0xDC, 0xE2, 0xE9);
    public static final Color LINE_STRONG  = new Color(0xC2, 0xCB, 0xD6);
    public static final Color TEXT         = new Color(0x11, 0x18, 0x21);
    public static final Color TEXT_MUTED   = new Color(0x5B, 0x67, 0x76);
    public static final Color TEXT_FAINT   = new Color(0x8A, 0x96, 0xA3);

    // ---- Palette: petrol accent + status ----
    public static final Color ACCENT       = new Color(0x0B, 0x63, 0x63);
    public static final Color ACCENT_HOVER = new Color(0x08, 0x4C, 0x4C);
    public static final Color ACCENT_SOFT  = new Color(0xE2, 0xF0, 0xF0);
    public static final Color ON_ACCENT    = new Color(0xFF, 0xFF, 0xFF);
    public static final Color SUCCESS      = new Color(0x0E, 0x7A, 0x4F);
    public static final Color WARNING      = new Color(0xB4, 0x53, 0x09);
    public static final Color DANGER       = new Color(0xB4, 0x23, 0x18);

    // ---- Spacing scale (4pt rhythm) ----
    public static final int SPACE_XS  = 4;
    public static final int SPACE_SM  = 8;
    public static final int SPACE_MD  = 12;
    public static final int SPACE_LG  = 16;
    public static final int SPACE_XL  = 24;
    public static final int SPACE_XXL = 32;

    // ---- Radii ----
    public static final int RADIUS_CARD    = 10;
    public static final int RADIUS_CONTROL = 6;

    // ---- Typography ----
    private static final String UI_FAMILY = pickFamily(
            "Segoe UI Variable Text", "Segoe UI", "Inter", "Roboto", Font.SANS_SERIF);
    private static final String DISPLAY_FAMILY = pickFamily(
            "Segoe UI Variable Display", "Segoe UI Semibold", "Segoe UI", "Inter", Font.SANS_SERIF);
    private static final String MONO_FAMILY = pickFamily(
            "Cascadia Mono", "Cascadia Code", "Consolas", "JetBrains Mono", Font.MONOSPACED);

    /** Page titles. Tightened tracking keeps big text from looking loose. */
    public static final Font TITLE   = tracked(new Font(DISPLAY_FAMILY, Font.BOLD, 22), -0.015f);
    /** Card / section headings. */
    public static final Font HEADING = new Font(UI_FAMILY, Font.BOLD, 15);
    /** Small all-caps section markers; the caller supplies uppercase text. */
    public static final Font EYEBROW = tracked(new Font(UI_FAMILY, Font.BOLD, 10), 0.14f);
    public static final Font BODY    = new Font(UI_FAMILY, Font.PLAIN, 13);
    public static final Font BODY_STRONG = new Font(UI_FAMILY, Font.BOLD, 13);
    public static final Font SMALL   = new Font(UI_FAMILY, Font.PLAIN, 12);
    /** For things that genuinely are data: counts, radii, versions, timestamps. */
    public static final Font MONO    = new Font(MONO_FAMILY, Font.PLAIN, 12);

    /** Installs the look and feel. Must run before any component is created. */
    public static void install() {
        FlatLightLaf.setup();

        UIManager.put("defaultFont", new FontUIResource(BODY));
        UIManager.put("Component.arc", RADIUS_CONTROL);
        UIManager.put("Button.arc", RADIUS_CONTROL);
        UIManager.put("TextComponent.arc", RADIUS_CONTROL);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 1);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Component.focusedBorderColor", ACCENT);
        UIManager.put("Component.borderColor", LINE_STRONG);

        UIManager.put("Panel.background", CANVAS);
        UIManager.put("Viewport.background", CANVAS);
        UIManager.put("ScrollPane.background", CANVAS);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("ComboBox.padding", new Insets(SPACE_XS + 1, SPACE_SM, SPACE_XS + 1, SPACE_SM));

        UIManager.put("List.selectionBackground", ACCENT_SOFT);
        UIManager.put("List.selectionForeground", TEXT);
        UIManager.put("List.selectionInactiveBackground", ACCENT_SOFT);
        UIManager.put("List.selectionInactiveForeground", TEXT);
        UIManager.put("ComboBox.selectionBackground", ACCENT_SOFT);
        UIManager.put("ComboBox.selectionForeground", TEXT);

        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.track", CANVAS);
    }

    private static String pickFamily(String... candidates) {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidate : candidates) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.SANS_SERIF;
    }

    private static Font tracked(Font base, float tracking) {
        return base.deriveFont(Map.of(TextAttribute.TRACKING, tracking));
    }

    // ---- Borders ----
    public static Border pad(int all) {
        return BorderFactory.createEmptyBorder(all, all, all, all);
    }

    public static Border pad(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    // ---- Labels ----
    public static JLabel title(String text)   { return label(text, TITLE, TEXT); }
    public static JLabel heading(String text) { return label(text, HEADING, TEXT); }
    public static JLabel body(String text)    { return label(text, BODY, TEXT); }
    public static JLabel muted(String text)   { return label(text, SMALL, TEXT_MUTED); }

    /**
     * Animates a busy message's trailing ellipsis ("Searching…" → "Searching." / ".." / "...")
     * so it visibly moves. Call from a LivePage refresh — the UI timer provides the ticks.
     * Messages not ending in "…" pass through unchanged.
     */
    public static String animate(String message) {
        if (message == null || !message.endsWith("…")) {
            return message;
        }
        int dots = 1 + (int) ((System.currentTimeMillis() / 400) % 3);
        return message.substring(0, message.length() - 1) + ".".repeat(dots);
    }
    public static JLabel mono(String text)    { return label(text, MONO, TEXT_MUTED); }

    /** A small all-caps marker above a field or section. */
    public static JLabel eyebrow(String text) {
        return label(text.toUpperCase(java.util.Locale.ROOT), EYEBROW, TEXT_FAINT);
    }

    private static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    // ---- Buttons ----
    public static JButton primaryButton(String text) {
        JButton b = baseButton(text, BODY_STRONG, ACCENT, ON_ACCENT);
        b.setBorder(pad(SPACE_SM + 1, SPACE_LG, SPACE_SM + 1, SPACE_LG));
        hoverFill(b, ACCENT, ACCENT_HOVER);
        return b;
    }

    public static JButton secondaryButton(String text) {
        JButton b = baseButton(text, BODY, SURFACE, TEXT);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE_STRONG),
                pad(SPACE_SM, SPACE_MD + 2, SPACE_SM, SPACE_MD + 2)));
        hoverFill(b, SURFACE, SURFACE_SUNK);
        return b;
    }

    /** Lowest-emphasis action: no fill, no border. */
    public static JButton ghostButton(String text) {
        JButton b = baseButton(text, BODY, CANVAS, ACCENT);
        b.setBorder(pad(SPACE_SM, SPACE_MD, SPACE_SM, SPACE_MD));
        b.setContentAreaFilled(false);
        return b;
    }

    private static JButton baseButton(String text, Font font, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(font);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    /** FlatLaf derives no hover colour from a custom background, so drive it explicitly. */
    private static void hoverFill(JButton button, Color normal, Color hover) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(normal);
            }
        });
    }

    // ---- Inputs ----
    public static JTextField textField(String placeholder, int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(BODY);
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.setBorder(BorderFactory.createCompoundBorder(
                field.getBorder(), pad(SPACE_XS, SPACE_SM, SPACE_XS, SPACE_SM)));
        return field;
    }

    public static <T> JComboBox<T> comboBox(T[] items) {
        JComboBox<T> combo = new JComboBox<>(items);
        combo.setFont(BODY);
        combo.setBackground(SURFACE);
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return combo;
    }

    // ---- Layout ----

    /** Vertical box; children are left-aligned as added. */
    public static JPanel stack(Component... items) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        addLeft(p, items);
        return p;
    }

    /** A rounded surface panel laying its children out vertically. */
    public static JPanel card(Component... items) {
        JPanel p = new CardPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(pad(SPACE_LG + 2));
        addLeft(p, items);
        return p;
    }

    /** A left-aligned horizontal row that keeps its preferred height inside a vertical box. */
    public static JPanel row(Component... items) {
        return row(SPACE_SM, items);
    }

    public static JPanel row(int gap, Component... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component c : items) {
            p.add(c);
        }
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    /** A labelled field: small caps marker above its control. */
    public static JPanel field(String label, Component control) {
        JPanel p = new JPanel(new BorderLayout(0, SPACE_XS + 2));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(eyebrow(label), BorderLayout.NORTH);
        p.add(control, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    private static void addLeft(JPanel p, Component... items) {
        for (Component c : items) {
            if (c instanceof JComponent jc) {
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            p.add(c);
        }
    }

    /**
     * Wraps page content so it stays reachable when it outgrows the window — Swing labels cannot
     * shrink below their preferred size, so without this they silently overflow the bottom edge.
     */
    public static JScrollPane scroll(Component content) {
        JScrollPane pane = new JScrollPane(new ScrollableView(content));
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.getVerticalScrollBar().setUnitIncrement(SPACE_LG);
        return pane;
    }

    public static Component vGap(int height) {
        return Box.createVerticalStrut(height);
    }

    public static Component vGlue() {
        return Box.createVerticalGlue();
    }

    /** Rounded, hairline-bordered surface. Swing has no border-radius, so it is painted. */
    private static final class CardPanel extends JPanel {

        CardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            g2.setColor(SURFACE);
            g2.fillRoundRect(0, 0, w, h, RADIUS_CARD, RADIUS_CARD);
            g2.setColor(LINE);
            g2.drawRoundRect(0, 0, w, h, RADIUS_CARD, RADIUS_CARD);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Viewport view that keeps content full-width and top-aligned, scrolling only vertically. */
    private static final class ScrollableView extends JPanel implements Scrollable {

        ScrollableView(Component content) {
            setLayout(new BorderLayout());
            setOpaque(false);
            add(content, BorderLayout.NORTH); // NORTH => natural height, pinned to the top
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return SPACE_LG;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return visible.height;
        }

        /** True so cards stretch to the window width instead of collapsing to preferred width. */
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
