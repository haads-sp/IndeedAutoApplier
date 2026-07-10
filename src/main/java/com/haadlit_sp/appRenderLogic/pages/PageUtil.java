package com.haadlit_sp.appRenderLogic.pages;

import javax.swing.*;

import com.haadlit_sp.appRenderLogic.App;

public class PageUtil {


    // Navigation buttons | 2 button implementation
    public JPanel navButtons(App app, String pageX, String pageY) {

        JPanel buttons = new JPanel();

        JButton backButton = new JButton("Go to " + pageX);
        backButton.addActionListener(e -> app.showPage(pageX));

        JButton nextButton = new JButton("Go to " + pageY);
        nextButton.addActionListener(e -> app.showPage(pageY));

        buttons.add(backButton);
        buttons.add(nextButton);

        return buttons;
    }


    // Navigation buttons | 3 button implementation
    public JPanel navButtons(App app, String pageX, String pageY, String pageZ) {

        JPanel buttons = new JPanel();

        JButton xButton = new JButton("Go to " + pageX);
        xButton.addActionListener(e -> app.showPage(pageX));

        JButton yButton = new JButton("Go to " + pageY);
        yButton.addActionListener(e -> app.showPage(pageY));

        JButton zButton = new JButton("Go to " + pageZ);
        zButton.addActionListener(e -> app.showPage(pageZ));

        buttons.add(xButton);
        buttons.add(yButton);
        buttons.add(zButton);

        return buttons;
    }




}
