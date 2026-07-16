package com.haadlit_sp.appRenderLogic.components;

import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.JPanel;
import java.awt.BorderLayout;


/** A page title with a muted subtitle. The rail already says which step this is. */
public class Header extends JPanel {

    public Header(String title, String subtitle) {
        setOpaque(false);
        setLayout(new BorderLayout(0, Theme.SPACE_XS));
        setBorder(Theme.pad(0, 0, Theme.SPACE_XL, 0));
        add(Theme.title(title), BorderLayout.NORTH);
        add(Theme.muted(subtitle), BorderLayout.SOUTH);
    }
}
