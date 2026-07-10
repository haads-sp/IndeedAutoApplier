package com.haadlit_sp.appRenderLogic.pages;

import javax.swing.*;

import com.haadlit_sp.appRenderLogic.App;

import java.awt.*;


public class Page2 extends JPanel {

    public Page2(App app) {

        PageUtil pageUtil = new PageUtil();

        setBackground(Color.CYAN);
        setLayout(new BorderLayout());

        add(pageUtil.navButtons(app, "Page1", "Page2", "Page3"), BorderLayout.SOUTH);


        JLabel label = new JLabel("This is Page 2", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);

    }
}
