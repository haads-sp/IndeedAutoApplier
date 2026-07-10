package com.haadlit_sp.appRenderLogic.pages;

import javax.swing.*;

import com.haadlit_sp.appRenderLogic.App;

import java.awt.*;


public class Page3 extends JPanel {

    public Page3(App app) {

        PageUtil pageUtil = new PageUtil();

        setBackground(Color.PINK);
        setLayout(new BorderLayout());
        
        add(pageUtil.navButtons(app, "Page1", "Page2", "Page3"), BorderLayout.SOUTH);


        JLabel label = new JLabel("This is Page 3", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }

   
}
