package com.haadlit_sp.appRenderLogic.theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;


/**
 * Single source of truth for colors, fonts, spacing and small styled-widget factories.
 * Light, professional palette with an Indeed-blue accent.
 */
public final class Theme {

    private Theme() {}

    // ---- Palette ----
    public static final Color BG          = new Color(0xF3, 0xF4, 0xF6);
    public static final Color SURFACE     = new Color(0xFF, 0xFF, 0xFF);
    public static final Color TEXT        = new Color(0x1F, 0x29, 0x33);
    public static final Color MUTED       = new Color(0x6B, 0x72, 0x80);
    public static final Color BORDER      = new Color(0xD1, 0xD5, 0xDB);
    public static final Color ACCENT      = new Color(0x25, 0x57, 0xA7); // Indeed blue
    public static final Color ACCENT_TEXT = Color.WHITE;
    public static final Color SUCCESS     = new Color(0x12, 0x80, 0x5C);
    public static final Color WARN        = new Color(0xB4, 0x53, 0x09);
    public static final Color ERROR       = new Color(0xB9, 0x1C, 0x1C);

    // ---- Fonts ----
    private static final String FAMILY = "Segoe UI";
    public static final Font TITLE   = new Font(FAMILY, Font.BOLD, 22);
    public static final Font HEADING = new Font(FAMILY, Font.BOLD, 15);
    public static final Font BODY    = new Font(FAMILY, Font.PLAIN, 13);
    public static final Font SMALL   = new Font(FAMILY, Font.PLAIN, 12);
    public static final Font MONO    = new Font("Consolas", Font.PLAIN, 12);

    // ---- Spacing ----
    public static final int PAD = 16;
    public static final int GAP = 8;

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
    public static JLabel muted(String text)   { return label(text, SMALL, MUTED); }

    private static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    // ---- Buttons ----
    public static JButton primaryButton(String text) {
        JButton b = baseButton(text, HEADING, ACCENT, ACCENT_TEXT);
        b.setBorder(pad(8, 18, 8, 18));
        return b;
    }

    public static JButton secondaryButton(String text) {
        JButton b = baseButton(text, BODY, SURFACE, TEXT);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                pad(7, 16, 7, 16)));
        return b;
    }

    private static JButton baseButton(String text, Font font, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(font);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ---- Layout helpers ----

    /** Vertical box; children are left-aligned as added. */
    public static JPanel stack(Component... items) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        addLeft(p, items);
        return p;
    }

    /** A white, bordered "card" laying its children out vertically. */
    public static JPanel card(Component... items) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(SURFACE);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), pad(PAD)));
        addLeft(p, items);
        return p;
    }

    /** A left-aligned horizontal row that keeps its preferred height inside a vertical box. */
    public static JPanel row(Component... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, GAP, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component c : items) {
            p.add(c);
        }
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

    public static Component vGap(int height) {
        return Box.createVerticalStrut(height);
    }

    public static Component vGlue() {
        return Box.createVerticalGlue();
    }
}
