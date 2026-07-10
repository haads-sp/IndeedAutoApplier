package com.haadlit_sp.appRenderLogic.pages;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.haadlit_sp.appRenderLogic.App;
import com.haadlit_sp.appRenderLogic.components.Header;
import com.haadlit_sp.appRenderLogic.theme.Theme;

import java.awt.BorderLayout;


/** Step 1 — sign in to Indeed (manual browser login, with a credential fallback). */
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
                "Sign in so the app can apply on your behalf."), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(pageUtil.stepNav(app, null, "Continue →", () -> app.showPage("Page2")), BorderLayout.SOUTH);
    }

    private JComponent buildBody() {
        return Theme.stack(
                buildManualLogin(),
                Theme.vGap(Theme.PAD),
                buildCredentialLogin(),
                Theme.vGap(Theme.PAD),
                buildPrivacyNotice(),
                Theme.vGlue());
    }

    private JComponent buildManualLogin() {
        JButton open = Theme.primaryButton("Open browser & sign in");
        open.addActionListener(e -> {
            app.core().startManualLogin();
            statusLabel.setText("Browser launch requested (stub).");
        });
        return Theme.card(
                Theme.heading("Recommended — sign in manually"),
                Theme.muted("Opens a real browser window; this survives CAPTCHAs and 2FA."),
                Theme.vGap(Theme.GAP),
                Theme.row(open, statusLabel));
    }

    private JComponent buildCredentialLogin() {
        JTextField email = new JTextField(18);
        JPasswordField password = new JPasswordField(14);
        JButton signIn = Theme.secondaryButton("Sign in");
        signIn.addActionListener(e -> {
            app.core().loginWithCredentials(email.getText(), password.getPassword());
            statusLabel.setText("Credential sign-in requested (stub).");
        });
        return Theme.card(
                Theme.heading("Fallback — sign in with credentials"),
                Theme.muted("The app types these into Indeed's login form. Held in memory only."),
                Theme.vGap(Theme.GAP),
                Theme.row(Theme.body("Email"), email, Theme.body("Password"), password, signIn));
    }

    private JComponent buildPrivacyNotice() {
        return Theme.card(
                Theme.heading("Privacy"),
                Theme.body("<html>Credentials and session data live only in memory for this session, "
                        + "are never written to disk, and are voided when the app exits.</html>"));
    }
}
