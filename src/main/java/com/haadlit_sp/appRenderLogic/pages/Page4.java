package com.haadlit_sp.appRenderLogic.pages;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.haadlit_sp.appCoreLogic.model.AnswerMode;
import com.haadlit_sp.appCoreLogic.model.JobPosting;
import com.haadlit_sp.appCoreLogic.model.SubmitMode;
import com.haadlit_sp.appRenderLogic.App;
import com.haadlit_sp.appRenderLogic.components.Header;
import com.haadlit_sp.appRenderLogic.components.ResultsPanel;
import com.haadlit_sp.appRenderLogic.components.SubmitModeSelector;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import java.awt.BorderLayout;
import java.util.List;


/** Run page — search for matching postings, then apply to them one at a time in the chosen mode. */
public class Page4 extends JPanel implements LivePage {

    private final App app;
    private final PageUtil pageUtil = new PageUtil();

    private final ResultsPanel resultsPanel = new ResultsPanel();
    private final SubmitModeSelector modeSelector;
    private final JButton searchBtn = Theme.primaryButton("Find matching jobs");
    private final JLabel searchStatus = Theme.muted("Not searched yet.");
    private final JButton applyBtn = Theme.primaryButton("Apply to next posting");
    private final JLabel applyStatus = Theme.muted("Search first, then apply one posting at a time.");
    // Single space, not "": Theme.row freezes its height at build time, and an empty label
    // measures 0 tall — the row would clip the text forever once it appears.
    private final JLabel aiStatus = Theme.muted(" ");
    private List<JobPosting> shown = List.of();

    public Page4(App app) {
        this.app = app;
        this.modeSelector = new SubmitModeSelector(app.core().submitMode());

        setBackground(Theme.CANVAS);
        setLayout(new BorderLayout());
        setBorder(Theme.pad(Theme.SPACE_XXL));

        add(new Header("Run", "Search for matching postings, then apply to them one at a time — "
                + "the app fills what it can and pauses in the browser when it needs you."), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(pageUtil.stepNav(app, "Page3", null, null), BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> app.core().startSearch());
        applyBtn.addActionListener(e -> {
            if (app.core().isApplying()) {
                app.core().stopApplying();
            } else {
                app.core().applyToNextPosting();
            }
        });
        modeSelector.onChange(app.core()::setSubmitMode);
        refresh();
    }

    private JComponent buildBody() {
        JComponent search = Theme.card(
                Theme.heading("Find jobs"),
                Theme.vGap(Theme.SPACE_MD),
                Theme.row(searchBtn),
                Theme.vGap(Theme.SPACE_MD),
                Theme.row(searchStatus));

        JComponent applyMode = Theme.card(
                Theme.heading("Apply mode"),
                Theme.vGap(Theme.SPACE_XS),
                Theme.muted("How each application should be submitted when applying is run."),
                Theme.vGap(Theme.SPACE_MD),
                modeSelector);

        JComponent apply = Theme.card(
                Theme.heading("Apply"),
                Theme.vGap(Theme.SPACE_XS),
                Theme.muted("Opens each posting, fills what it can, and pauses in the browser when it "
                        + "needs you (a question, a review, or a check to clear)."),
                Theme.vGap(Theme.SPACE_MD),
                Theme.row(applyBtn),
                Theme.vGap(Theme.SPACE_MD),
                Theme.row(applyStatus),
                Theme.row(aiStatus));

        JComponent top = Theme.stack(search, Theme.vGap(Theme.SPACE_LG), applyMode,
                Theme.vGap(Theme.SPACE_LG), apply);

        JPanel body = new JPanel(new BorderLayout(0, Theme.SPACE_LG));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(resultsPanel, BorderLayout.CENTER);
        return body;
    }

    @Override
    public void refresh() {
        boolean searching = app.core().isSearching();
        searchStatus.setText(app.core().searchMessage());
        searchBtn.setEnabled(!searching);
        searchBtn.setText(searching ? "Searching…" : "Find matching jobs");

        boolean applying = app.core().isApplying();
        boolean auto = app.core().submitMode() != SubmitMode.REVIEW;
        applyStatus.setText(app.core().applyMessage());
        // In the auto modes the run is hands-off, so the button becomes its own Stop.
        applyBtn.setEnabled(!applying || auto);
        applyBtn.setText(applying ? (auto ? "Stop" : "Applying…")
                : (auto ? "Apply to all found jobs" : "Apply to next posting"));

        String ai = app.core().aiStatus();
        boolean showAi = app.core().answerMode() == AnswerMode.AI_ENHANCED && !ai.isBlank();
        aiStatus.setVisible(showAi);
        if (showAi) {
            aiStatus.setText(ai);
        }

        // The found list only changes when a search completes, so rebuild the model only then.
        List<JobPosting> latest = app.core().foundPostings();
        if (latest != shown) {
            resultsPanel.setResults(latest, app.core().appliedPostingIds());
            shown = latest;
        }
    }
}
