package com.haadlit_sp.appRenderLogic.pages;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.haadlit_sp.appCoreLogic.model.CityLocation;
import com.haadlit_sp.appCoreLogic.model.DatePosted;
import com.haadlit_sp.appCoreLogic.model.SearchRadius;
import com.haadlit_sp.appRenderLogic.App;
import com.haadlit_sp.appRenderLogic.components.AutocompleteField;
import com.haadlit_sp.appRenderLogic.components.Header;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import java.awt.BorderLayout;
import java.awt.GridLayout;


/** Step 3 — where to search, how far out, and how fresh the postings must be. */
public class Page3 extends JPanel {

    private static final int MAX_SUGGESTIONS = 8;

    private final App app;
    private final PageUtil pageUtil = new PageUtil();

    private final AutocompleteField<CityLocation> city;
    private final JComboBox<SearchRadius> radius = Theme.comboBox(SearchRadius.values());
    private final JComboBox<DatePosted> datePosted = Theme.comboBox(DatePosted.values());

    public Page3(App app) {
        this.app = app;
        this.city = new AutocompleteField<>("Start typing a city…", 26,
                typed -> app.core().suggestLocations(typed, MAX_SUGGESTIONS),
                CityLocation::displayName);

        setBackground(Theme.CANVAS);
        setLayout(new BorderLayout());
        setBorder(Theme.pad(Theme.SPACE_XXL));
        radius.setSelectedItem(SearchRadius.KM_25);
        datePosted.setSelectedItem(DatePosted.ANY_TIME);

        add(new Header("Location", "Where should we search, and how far out?"), BorderLayout.NORTH);
        add(Theme.scroll(buildBody()), BorderLayout.CENTER);
        add(pageUtil.stepNav(app, "Details", "Continue →", this::saveAndContinue), BorderLayout.SOUTH);
    }

    private JComponent buildBody() {
        JComponent place = Theme.card(
                Theme.heading("Where"),
                Theme.muted("Pick a suggestion to be precise — the region and country tell "
                        + "same-named cities apart. Any typed text is accepted."),
                Theme.vGap(Theme.SPACE_LG),
                Theme.field("City", city),
                Theme.vGap(Theme.SPACE_XS),
                Theme.mono("City data © GeoNames, CC BY 4.0"));

        JPanel filters = new JPanel(new GridLayout(1, 2, Theme.SPACE_LG, 0));
        filters.setOpaque(false);
        filters.add(Theme.field("Radius from work", radius));
        filters.add(Theme.field("Date posted", datePosted));

        JComponent narrow = Theme.card(
                Theme.heading("Narrow it down"),
                Theme.muted("Both mirror Indeed's own filters."),
                Theme.vGap(Theme.SPACE_LG),
                filters);

        return Theme.stack(place, Theme.vGap(Theme.SPACE_LG), narrow, Theme.vGlue());
    }

    private void saveAndContinue() {
        String cityText = city.getText();
        if (cityText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your city.",
                    "City required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // The country routes the search to the right Indeed domain, so resolve it now while we
        // still have the text the user picked ("Dubai, Dubai, United Arab Emirates").
        app.core().setSearchCriteria(app.core().searchCriteria()
                .withLocation(cityText, app.core().countryFor(cityText),
                        (SearchRadius) radius.getSelectedItem())
                .withDatePosted((DatePosted) datePosted.getSelectedItem()));
        app.showPage("Page4");
    }
}
