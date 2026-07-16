package com.haadlit_sp.appRenderLogic.components;

import com.haadlit_sp.appCoreLogic.model.ProfileFacts;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;


/** Shows what was pulled out of the user's PDFs. Narrow API: {@link #setFacts}. */
public class ProfileFactsPanel extends JPanel {

    private static final int SKILLS_SHOWN = 12;
    private static final String MUTED_HEX = "#5B6776";

    private final JLabel status = Theme.body("No resume selected.");
    private final JLabel details = Theme.muted(" ");

    public ProfileFactsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, Theme.SPACE_SM));
        add(status, BorderLayout.NORTH);
        add(details, BorderLayout.CENTER);
    }

    public void setFacts(String message, ProfileFacts facts) {
        status.setText(message);
        if (facts.isEmpty()) {
            details.setText(" ");
            return;
        }
        List<String> rows = new ArrayList<>();
        addRow(rows, "Name", facts.fullName());
        addRow(rows, "Email", facts.email());
        addRow(rows, "Phone", facts.phone());
        addRow(rows, "Experience", facts.yearsOfExperience() == null
                ? null : facts.yearsOfExperience() + " years");
        addRow(rows, "Education", facts.educationLevel());
        addRow(rows, "Work authorization", facts.workAuthorization());
        addRow(rows, "Skills (" + facts.skills().size() + ")", preview(facts.skills()));
        addRow(rows, "Certifications (" + facts.certifications().size() + ")",
                preview(facts.certifications()));

        details.setText(rows.isEmpty()
                ? "Nothing recognisable found — you'll be asked questions as they come up."
                : "<html>" + String.join("<br>", rows) + "</html>");
    }

    private static void addRow(List<String> rows, String label, String value) {
        if (value != null && !value.isBlank()) {
            rows.add("<font color='" + MUTED_HEX + "'>" + label + "</font>&nbsp;&nbsp;" + escape(value));
        }
    }

    private static String preview(List<String> items) {
        if (items.isEmpty()) {
            return null;
        }
        String shown = String.join(", ", items.subList(0, Math.min(items.size(), SKILLS_SHOWN)));
        return items.size() > SKILLS_SHOWN ? shown + "…" : shown;
    }

    /** The rows are rendered as HTML, so raw PDF text must not be able to inject markup. */
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
