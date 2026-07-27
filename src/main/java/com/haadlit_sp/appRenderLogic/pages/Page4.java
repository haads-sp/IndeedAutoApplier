package com.haadlit_sp.appRenderLogic.pages;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.haadlit_sp.appCoreLogic.model.JobPosting;
import com.haadlit_sp.appRenderLogic.App;
import com.haadlit_sp.appRenderLogic.components.Header;
import com.haadlit_sp.appRenderLogic.components.ResultsPanel;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import java.awt.BorderLayout;
import java.util.List;


/** Run page — for now, find and list matching postings (read-only). Applying comes in a later slice. */
public class Page4 extends JPanel implements LivePage {

    private final App app;
    private final PageUtil pageUtil = new PageUtil();

    private final ResultsPanel resultsPanel = new ResultsPanel();
    private final JButton searchBtn = Theme.primaryButton("Find matching jobs");
    private final JLabel searchStatus = Theme.muted("Not searched yet.");
    private List<JobPosting> shown = List.of();

    public Page4(App app) {
        this.app = app;

        setBackground(Theme.CANVAS);
        setLayout(new BorderLayout());
        setBorder(Theme.pad(Theme.SPACE_XXL));

        add(new Header("Run", "Find postings that match your search. For now the app lists what it "
                + "finds; submitting applications comes next."), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(pageUtil.stepNav(app, "Page3", null, null), BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> app.core().startSearch());
        refresh();
    }

    private JComponent buildBody() {
        JComponent controls = Theme.card(
                Theme.row(searchBtn),
                Theme.vGap(Theme.SPACE_MD),
                Theme.row(searchStatus));

        JPanel body = new JPanel(new BorderLayout(0, Theme.SPACE_LG));
        body.setOpaque(false);
        body.add(controls, BorderLayout.NORTH);
        body.add(resultsPanel, BorderLayout.CENTER);
        return body;
    }

    @Override
    public void refresh() {
        boolean searching = app.core().isSearching();
        searchStatus.setText(app.core().searchMessage());
        searchBtn.setEnabled(!searching);
        searchBtn.setText(searching ? "Searching…" : "Find matching jobs");

        // The found list only changes when a search completes, so rebuild the model only then.
        List<JobPosting> latest = app.core().foundPostings();
        if (latest != shown) {
            resultsPanel.setResults(latest, app.core().appliedPostingIds());
            shown = latest;
        }
    }
}
