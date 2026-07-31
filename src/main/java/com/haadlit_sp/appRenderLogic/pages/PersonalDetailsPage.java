package com.haadlit_sp.appRenderLogic.pages;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.haadlit_sp.appCoreLogic.model.ContactDetails;
import com.haadlit_sp.appRenderLogic.App;
import com.haadlit_sp.appRenderLogic.components.Header;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import java.awt.BorderLayout;
import java.awt.GridLayout;


/** Collects name / email / phone / address once, so the run fills those apply steps on its own. */
public class PersonalDetailsPage extends JPanel implements LivePage {

    private final App app;
    private final PageUtil pageUtil = new PageUtil();

    private final JTextField firstName = Theme.textField("Jane", 16);
    private final JTextField lastName = Theme.textField("Doe", 16);
    private final JTextField email = Theme.textField("jane@example.com", 22);
    private final JTextField phone = Theme.textField("(416) 555-0123", 16);
    private final JTextField street = Theme.textField("123 Main St", 28);
    private final JTextField city = Theme.textField("Toronto", 16);
    private final JTextField region = Theme.textField("Ontario", 16);
    private final JTextField postal = Theme.textField("M5V 1A1", 12);
    private final JTextField country = Theme.textField("Canada", 16);

    private boolean seeded;

    public PersonalDetailsPage(App app) {
        this.app = app;

        setBackground(Theme.CANVAS);
        setLayout(new BorderLayout());
        setBorder(Theme.pad(Theme.SPACE_XXL));

        add(new Header("Your details",
                "Entered once and reused to fill the name, contact and address questions automatically."),
                BorderLayout.NORTH);
        add(Theme.scroll(buildBody()), BorderLayout.CENTER);
        add(pageUtil.stepNav(app, "Page2", "Continue →", this::saveAndContinue), BorderLayout.SOUTH);
    }

    private JComponent buildBody() {
        JComponent contact = Theme.card(
                Theme.heading("Contact"),
                Theme.vGap(Theme.SPACE_MD),
                grid(Theme.field("First name", firstName), Theme.field("Last name", lastName),
                        Theme.field("Email", email), Theme.field("Phone", phone)));

        JComponent address = Theme.card(
                Theme.heading("Address"),
                Theme.vGap(Theme.SPACE_MD),
                Theme.field("Street address", street),
                Theme.vGap(Theme.SPACE_MD),
                grid(Theme.field("City", city), Theme.field("Province / State", region),
                        Theme.field("Postal / ZIP code", postal), Theme.field("Country", country)));

        JComponent note = Theme.muted("Saved on your machine (~/.indeedapplier) so you only enter it "
                + "once. Seeded from your resume — please check it.");

        return Theme.stack(contact, Theme.vGap(Theme.SPACE_LG), address,
                Theme.vGap(Theme.SPACE_MD), note, Theme.vGlue());
    }

    private JPanel grid(JComponent... fields) {
        JPanel p = new JPanel(new GridLayout(0, 2, Theme.SPACE_LG, Theme.SPACE_MD));
        p.setOpaque(false);
        for (JComponent f : fields) {
            p.add(f);
        }
        return p;
    }

    /** Seed the fields from the resume-derived details once, when they first become available. */
    @Override
    public void refresh() {
        if (seeded) {
            return;
        }
        ContactDetails cd = app.core().contactDetails();
        if (cd.isEmpty()) {
            return;
        }
        setIfBlank(firstName, cd.firstName());
        setIfBlank(lastName, cd.lastName());
        setIfBlank(email, cd.email());
        setIfBlank(phone, cd.phone());
        setIfBlank(street, cd.streetAddress());
        setIfBlank(city, cd.city());
        setIfBlank(region, cd.region());
        setIfBlank(postal, cd.postalCode());
        setIfBlank(country, cd.country());
        seeded = true;
    }

    private static void setIfBlank(JTextField field, String value) {
        if (field.getText().isBlank()) {
            field.setText(value);
        }
    }

    private void saveAndContinue() {
        app.core().setContactDetails(new ContactDetails(
                firstName.getText(), lastName.getText(), email.getText(), phone.getText(),
                street.getText(), city.getText(), region.getText(), postal.getText(), country.getText()));
        app.showPage("Page3");
    }
}
