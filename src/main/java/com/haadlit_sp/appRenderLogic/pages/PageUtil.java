package com.haadlit_sp.appRenderLogic.pages;

import com.haadlit_sp.appRenderLogic.App;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;


/** Builds the footer navigation shared by the step pages. */
public class PageUtil {

    /**
     * Footer with an optional Back button (left) and an optional forward button (right).
     *
     * @param backPage   page to return to, or {@code null} to omit the Back button
     * @param nextLabel  label for the forward button (ignored when {@code nextAction} is null)
     * @param nextAction what the forward button does, or {@code null} to omit it
     */
    public JPanel stepNav(App app, String backPage, String nextLabel, Runnable nextAction) {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(Theme.pad(Theme.SPACE_XL, 0, 0, 0));

        if (backPage != null) {
            JButton back = Theme.secondaryButton("← Back");
            back.addActionListener(e -> app.showPage(backPage));
            footer.add(back, BorderLayout.WEST);
        }

        if (nextAction != null) {
            JButton next = Theme.primaryButton(nextLabel);
            next.addActionListener(e -> nextAction.run());
            footer.add(next, BorderLayout.EAST);
        }

        return footer;
    }
}
