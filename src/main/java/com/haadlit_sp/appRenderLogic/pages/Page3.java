package com.haadlit_sp.appRenderLogic.pages;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.haadlit_sp.appCoreLogic.model.SearchRadius;
import com.haadlit_sp.appRenderLogic.App;
import com.haadlit_sp.appRenderLogic.components.Header;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import java.awt.BorderLayout;


/** Step 3 — current city plus an Indeed-style search radius. */
public class Page3 extends JPanel {

    private final App app;
    private final PageUtil pageUtil = new PageUtil();

    private final JTextField city = new JTextField(24);
    private final JComboBox<SearchRadius> radius = new JComboBox<>(SearchRadius.values());

    public Page3(App app) {
        this.app = app;

        setBackground(Theme.BG);
        setLayout(new BorderLayout());
        setBorder(Theme.pad(Theme.PAD * 2));
        radius.setSelectedItem(SearchRadius.KM_25);

        add(new Header("Step 3 — Location",
                "Where should we search, and how far out?"), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(pageUtil.stepNav(app, "Page2", "Continue →", this::saveAndContinue), BorderLayout.SOUTH);
    }

    private JComponent buildBody() {
        JComponent card = Theme.card(
                Theme.heading("Current city"),
                Theme.vGap(Theme.GAP),
                Theme.row(city),
                Theme.vGap(Theme.PAD),
                Theme.heading("Search radius"),
                Theme.vGap(Theme.GAP),
                Theme.row(radius));
        return Theme.stack(card, Theme.vGlue());
    }

    private void saveAndContinue() {
        String cityText = city.getText().trim();
        if (cityText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your city.",
                    "City required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SearchRadius selected = (SearchRadius) radius.getSelectedItem();
        app.core().setSearchCriteria(
                app.core().searchCriteria().withLocation(cityText, selected));
        app.showPage("Page4");
    }
}
