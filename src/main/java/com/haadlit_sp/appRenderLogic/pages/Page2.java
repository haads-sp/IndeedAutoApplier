package com.haadlit_sp.appRenderLogic.pages;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.haadlit_sp.appCoreLogic.model.SessionDocuments;
import com.haadlit_sp.appRenderLogic.App;
import com.haadlit_sp.appRenderLogic.components.FileField;
import com.haadlit_sp.appRenderLogic.components.Header;
import com.haadlit_sp.appRenderLogic.components.ProfileFactsPanel;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import java.awt.BorderLayout;


/** Step 2 — job target plus resume / cover letter / supporting PDF uploads. */
public class Page2 extends JPanel implements LivePage {

    private final App app;
    private final PageUtil pageUtil = new PageUtil();

    private final JTextField jobTarget = new JTextField(28);
    private final FileField resume = new FileField("Resume (PDF)", false);
    private final FileField coverLetter = new FileField("Cover letter", false);
    private final FileField supporting = new FileField("Supporting docs", true);
    private final ProfileFactsPanel factsPanel = new ProfileFactsPanel();

    public Page2(App app) {
        this.app = app;

        setBackground(Theme.BG);
        setLayout(new BorderLayout());
        setBorder(Theme.pad(Theme.PAD * 2));

        add(new Header("Step 2 — Documents & job target",
                "Tell us what you're looking for and attach your PDFs."), BorderLayout.NORTH);
        add(Theme.scroll(buildBody()), BorderLayout.CENTER);
        add(pageUtil.stepNav(app, "Page1", "Continue →", this::saveAndContinue), BorderLayout.SOUTH);

        wireDocumentChanges();
    }

    private JComponent buildBody() {
        JComponent target = Theme.card(
                Theme.heading("Job or field you're looking for"),
                Theme.muted("e.g. \"junior java developer\", \"warehouse associate\""),
                Theme.vGap(Theme.GAP),
                Theme.row(jobTarget));
        JComponent docs = Theme.card(
                Theme.heading("Documents"),
                Theme.muted("Resume is required. Cover letter and supporting docs are optional."),
                Theme.vGap(Theme.GAP),
                resume, coverLetter, supporting);
        JComponent facts = Theme.card(
                Theme.heading("What we found"),
                Theme.muted("Read from your PDFs and used to answer screener questions."),
                Theme.vGap(Theme.GAP),
                factsPanel);
        return Theme.stack(target, Theme.vGap(Theme.PAD), docs, Theme.vGap(Theme.PAD), facts, Theme.vGlue());
    }

    /** Re-read the PDFs as soon as a selection changes, so the facts show before moving on. */
    private void wireDocumentChanges() {
        Runnable reload = () -> app.core().loadDocuments(new SessionDocuments(
                resume.selectedFile(), coverLetter.selectedFile(), supporting.selectedFiles()));
        resume.onChange(reload);
        coverLetter.onChange(reload);
        supporting.onChange(reload);
    }

    @Override
    public void refresh() {
        factsPanel.setFacts(app.core().documentsMessage(), app.core().profileFacts());
    }

    private void saveAndContinue() {
        if (resume.selectedFile() == null) {
            JOptionPane.showMessageDialog(this, "A resume PDF is required to continue.",
                    "Resume required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        app.core().setSearchCriteria(
                app.core().searchCriteria().withJobQuery(jobTarget.getText().trim()));
        app.showPage("Page3");
    }
}
