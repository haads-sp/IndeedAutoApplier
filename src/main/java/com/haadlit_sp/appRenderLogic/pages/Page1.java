package com.haadlit_sp.appRenderLogic.pages;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.haadlit_sp.appRenderLogic.App;
import com.haadlit_sp.appRenderLogic.components.Header;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import java.awt.BorderLayout;


/** Step 1 — sign in once in a plain Chrome window; the app reuses that saved session. */
public class Page1 extends JPanel {

    private final App app;
    private final PageUtil pageUtil = new PageUtil();
    private final JLabel statusLabel = Theme.muted("Not signed in.");

    public Page1(App app) {
        this.app = app;

        setBackground(Theme.BG);
        setLayout(new BorderLayout());
        setBorder(Theme.pad(Theme.PAD * 2));

        add(new Header("Step 1 — Sign in to Indeed",
                "Sign in once by hand; the app reuses that session from then on."), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(pageUtil.stepNav(app, null, "Continue →", () -> app.showPage("Page2")), BorderLayout.SOUTH);

        startStatusTimer();
    }

    private JComponent buildBody() {
        return Theme.stack(
                buildSignIn(),
                Theme.vGap(Theme.PAD),
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
                Theme.muted("1. Click below — a normal Chrome window opens. It is not automated, so Google "
                        + "and Cloudflare treat it as the real browser it is."),
                Theme.muted("2. Sign in however you like (Google, emailed code…), then CLOSE that window."),
                Theme.muted("3. Click Verify. The app reuses that saved session for every run."),
                Theme.vGap(Theme.GAP),
                Theme.row(open, verify),
                Theme.vGap(Theme.GAP),
                Theme.row(statusLabel));
    }

    private JComponent buildPrivacyNotice() {
        return Theme.card(
                Theme.heading("Privacy"),
                Theme.body("<html>This app never sees, types, or stores your password — you sign in directly "
                        + "with Indeed/Google in a normal browser.<br><br>So that you only sign in once, Chrome "
                        + "keeps its own session cookies in a dedicated profile at "
                        + "<b>~/.indeedapplier/chrome-profile</b>. That folder is the only thing persisted; "
                        + "delete it to sign out completely.</html>"));
    }

    /** Reflect the facade's live login state (updated from the browser worker thread). */
    private void startStatusTimer() {
        Timer timer = new Timer(750, e -> {
            statusLabel.setText(app.core().loginMessage());
            statusLabel.setForeground(app.core().isLoggedIn() ? Theme.SUCCESS : Theme.MUTED);
        });
        timer.start();
    }
}
