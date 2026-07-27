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

    /**
     * Host the search runs on. Indeed serves results per country domain; this user's account
     * resolves to ca.indeed.com (radii are km). If we later support other regions this moves to a
     * setting — it is the one place the domain is written.
     */
    public static final String SEARCH_HOST = "https://ca.indeed.com";

    /**
     * Builds a job-search URL from the user's criteria.
     *
     * @param fromageDays days-back filter, or null for no date filter
     */
    public static String searchUrl(String query, String city, int radiusKm, Integer fromageDays) {
        StringBuilder url = new StringBuilder(SEARCH_HOST).append("/jobs?q=").append(encode(query));
        if (city != null && !city.isBlank()) {
            url.append("&l=").append(encode(city));
        }
        if (radiusKm > 0) {
            url.append("&radius=").append(radiusKm);
        }
        if (fromageDays != null) {
            url.append("&fromage=").append(fromageDays);
        }
        return url.toString();
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value,
                java.nio.charset.StandardCharsets.UTF_8);
    }

    // ---- Auth state ----
    /** Present only when signed IN (account/avatar button). Verified against the live site. */
    public static final String ACCOUNT_MENU = "[data-gnav-element-name='AccountMenu']";

    /**
     * JS predicate: is a Cloudflare bot check on screen? Verified against the live interstitial —
     * it is a full-page "Just a moment…" / "Additional Verification Required" page whose title and
     * body identify it; the turnstile-widget selectors alone do NOT match it. We only DETECT it so
     * the app can pause for the human; we never solve or bypass it.
     */
    public static final String IS_CHALLENGE_JS = """
        () => {
          const title = document.title || '';
          const body = (document.body && document.body.innerText) || '';
          return /just a moment|attention required|verifying you are human/i.test(title)
              || /additional verification required|verify you are (a |an )?human|ray id|checking your browser/i.test(body)
              || !!document.querySelector('#challenge-running, #challenge-stage, #cf-challenge-running, .cf-turnstile, iframe[src*="challenges.cloudflare.com"]');
        }
        """;

    // ---- Search results (verified against live ca.indeed.com) ----
    /** One result card. Present once results have rendered. */
    public static final String RESULT_CARD = ".job_seen_beacon";

    /**
     * JS predicate: are results actually READY to scrape? A card's skeleton (its job key and
     * company) renders before its title text hydrates, so waiting for the element alone scrapes
     * blank titles. This waits until the first card's title text has filled in.
     */
    public static final String RESULTS_READY_JS = """
        () => {
          const cards = document.querySelectorAll('.job_seen_beacon');
          if (!cards.length) return false;
          const a = cards[0].querySelector('a[data-jk]');
          return !!(a && a.textContent.trim().length > 0);
        }
        """;

    /**
     * The clean posting URL, built from the job key. The card's own href is a /pagead/clk ad
     * redirect for sponsored results, so we never navigate to it — we go straight to the job.
     */
    public static String jobUrl(String jobKey) {
        return SEARCH_HOST + "/viewjob?jk=" + jobKey;
    }

    /**
     * Scrapes the visible result cards into plain objects {jk, title, company, location,
     * easilyApply}. Returned to Java as a List of Maps by the browser driver's evaluate().
     */
    public static final String SCRAPE_POSTINGS_JS = """
        () => [...document.querySelectorAll('.job_seen_beacon')].map(card => {
          const a = card.querySelector('a[data-jk]') || card.querySelector('[data-jk]');
          const company = card.querySelector('[data-testid="company-name"]');
          const location = card.querySelector('[data-testid="text-location"]');
          // The title lives in the job-key anchor; a heading only exists in some renders.
          let title = a ? a.textContent.trim() : '';
          if (!title) { const h2 = card.querySelector('h2'); title = h2 ? h2.textContent.trim() : ''; }
          title = title.replace(/^full details of\\s+/i, '');
          return {
            jk: a ? a.getAttribute('data-jk') : '',
            title,
            company: company ? company.textContent.trim() : '',
            location: location ? location.textContent.trim() : '',
            easilyApply: /easily apply/i.test(card.textContent || '')
          };
        }).filter(p => p.jk)
        """;
}
