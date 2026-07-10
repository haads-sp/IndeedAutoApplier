package com.haadlit_sp.appRenderLogic.components;

import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.JPanel;
import java.awt.BorderLayout;


/** A step title with a muted subtitle, used at the top of every page. */
public class Header extends JPanel {

    public Header(String title, String subtitle) {
        setOpaque(false);
        setLayout(new BorderLayout(0, Theme.GAP / 2));
        setBorder(Theme.pad(0, 0, Theme.PAD, 0));
        add(Theme.title(title), BorderLayout.NORTH);
        add(Theme.muted(subtitle), BorderLayout.SOUTH);
    }
}
