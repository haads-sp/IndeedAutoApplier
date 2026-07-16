package com.haadlit_sp.appCoreLogic.browser;


/**
 * THE single source of truth for every Indeed URL and CSS selector.
 *
 * <p>Indeed changes its markup often; when automation breaks, fixing a selector must be a
 * one-file edit here. These are best-effort guesses and will need real-world tuning.
 */
public final class IndeedSelectors {

    private IndeedSelectors() {}

    // ---- URLs ----
    public static final String LOGIN_URL = "https://secure.indeed.com/account/login";
    /** Indeed redirects this to the visitor's country domain (e.g. ca.indeed.com). */
    public static final String HOME_URL  = "https://www.indeed.com/";

    // ---- Auth state ----
    /** Present only when signed IN (account/avatar button). Verified against the live site. */
    public static final String ACCOUNT_MENU = "[data-gnav-element-name='AccountMenu']";

    /**
     * Cloudflare bot check / "Additional Verification Required". When this is on screen the app
     * pauses so the human can clear it — we never try to solve or bypass it ourselves.
     */
    public static final String CLOUDFLARE_CHALLENGE =
            "iframe[src*='challenges.cloudflare.com'], #challenge-running, #cf-challenge-running, "
            + ".cf-turnstile, #challenge-form";
}
