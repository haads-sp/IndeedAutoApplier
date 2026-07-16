package com.haadlit_sp.appRenderLogic.pages;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.haadlit_sp.appRenderLogic.App;
import com.haadlit_sp.appRenderLogic.components.Header;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import java.awt.BorderLayout;


/** Step 1 — sign in once in a plain Chrome window; the app reuses that saved session. */
public class Page1 extends JPanel implements LivePage {

    private final App app;
    private final PageUtil pageUtil = new PageUtil();
    private final JLabel statusLabel = Theme.muted("Not signed in.");

    public Page1(App app) {
        this.app = app;

        setBackground(Theme.CANVAS);
        setLayout(new BorderLayout());
        setBorder(Theme.pad(Theme.SPACE_XXL));

        add(new Header("Sign in to Indeed",
                "Sign in once by hand; the app reuses that session from then on."), BorderLayout.NORTH);
        add(Theme.scroll(buildBody()), BorderLayout.CENTER);
        add(pageUtil.stepNav(app, null, "Continue →", () -> app.showPage("Page2")), BorderLayout.SOUTH);
    }

    private JComponent buildBody() {
        return Theme.stack(
                buildSignIn(),
                Theme.vGap(Theme.SPACE_LG),
                buildPrivacyNotice(),
                Theme.vGlue());
    }

    private JComponent buildSignIn() {
        JButton open = Theme.primaryButton("Open Chrome to sign in");
        open.addActionListener(e -> app.core().openSignInBrowser());

        JButton verify = Theme.secondaryButton("I've signed in — verify");
        verify.addActionListener(e -> app.core().verifySignIn());

        return Theme.card(
                Theme.heading("Sign in"),
                Theme.vGap(Theme.SPACE_SM),
                Theme.muted("1.  Click below — a normal Chrome window opens. It is not automated, so "
                        + "Google and Cloudflare treat it as the real browser it is."),
                Theme.muted("2.  Sign in however you like (Google, emailed code…), then CLOSE that window."),
                Theme.muted("3.  Click verify. The app reuses that saved session for every run."),
                Theme.vGap(Theme.SPACE_LG),
                Theme.row(open, verify),
                Theme.vGap(Theme.SPACE_MD),
                Theme.row(statusLabel));
    }

    private JComponent buildPrivacyNotice() {
        return Theme.card(
                Theme.eyebrow("Privacy"),
                Theme.vGap(Theme.SPACE_SM),
                Theme.body("<html>This app never sees, types, or stores your password — you sign in "
                        + "directly with Indeed/Google in a normal browser.<br><br>So that you only sign "
                        + "in once, Chrome keeps its own session cookies in a dedicated profile at "
                        + "<b>~/.indeedapplier/chrome-profile</b>. That folder is the only thing "
                        + "persisted; delete it to sign out completely.</html>"));
    }

    /** Reflect the facade's live login state (updated from the browser worker thread). */
    @Override
    public void refresh() {
        statusLabel.setText(app.core().loginMessage());
        statusLabel.setForeground(app.core().isLoggedIn() ? Theme.SUCCESS : Theme.TEXT_MUTED);
    }
}
