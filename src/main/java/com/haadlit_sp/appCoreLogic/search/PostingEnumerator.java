package com.haadlit_sp.appCoreLogic.search;

import com.haadlit_sp.appCoreLogic.browser.BrowserDriver;
import com.haadlit_sp.appCoreLogic.browser.IndeedSelectors;
import com.haadlit_sp.appCoreLogic.model.JobPosting;
import com.haadlit_sp.appCoreLogic.model.SearchCriteria;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Runs a search and reads the result cards into {@link JobPosting}s. READ-ONLY — it navigates and
 * scrapes, it never applies. Indeed specifics live in {@link IndeedSelectors}; this only orchestrates.
 *
 * <p>Runs on the browser worker thread (the driver is thread-affine), so it may block and sleep.
 */
public class PostingEnumerator {

    private static final Logger LOG = System.getLogger(PostingEnumerator.class.getName());
    private static final int WAIT_ATTEMPTS = 16;
    private static final long WAIT_MS = 500;
    /** Indeed pages results in steps of 10; cap the walk so a huge query can't run away. */
    private static final int PAGE_STEP = 10;
    private static final int MAX_PAGES = 10;
    private static final long BETWEEN_PAGES_MIN_MS = 1200;   // human-ish pacing between page loads,
    private static final long BETWEEN_PAGES_JITTER_MS = 1800; // randomized so it isn't metronomic

    private final BrowserDriver driver;

    public PostingEnumerator(BrowserDriver driver) {
        this.driver = driver;
    }

    /**
     * All postings for these criteria, walking every result page (deduped by job key) until a page
     * adds nothing new or the page cap is hit. Reports CHALLENGED when Cloudflare is asking the
     * human to verify, so the caller can pause rather than mistake it for empty results.
     */
    public EnumerationResult enumerate(SearchCriteria criteria) throws InterruptedException {
        List<JobPosting> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int page = 0; page < MAX_PAGES; page++) {
            String url = IndeedSelectors.searchUrl(
                    criteria.jobQuery(), criteria.city(), criteria.country(),
                    criteria.radius().km(), criteria.datePosted().days(), page * PAGE_STEP);
            LOG.log(Level.INFO, "Searching page {0}: {1}", page + 1, url);
            driver.navigate(url);

            List<JobPosting> pagePostings = null;
            for (int i = 0; i < WAIT_ATTEMPTS && pagePostings == null; i++) {
                if (isChallenged()) {
                    LOG.log(Level.INFO, "Cloudflare check is up; waiting for the human to clear it");
                    // Keep anything already collected rather than throwing the search away.
                    return all.isEmpty() ? EnumerationResult.challenged() : EnumerationResult.ok(all);
                }
                if (resultsReady()) {
                    pagePostings = toPostings(driver.evaluate(IndeedSelectors.SCRAPE_POSTINGS_JS),
                            criteria.country());
                    break;
                }
                Thread.sleep(WAIT_MS);
            }
            if (pagePostings == null) {
                // No cards rendered: past the last page (or truly no results on page one).
                break;
            }
            int newOnPage = 0;
            for (JobPosting posting : pagePostings) {
                if (seen.add(posting.id())) {
                    all.add(posting);
                    newOnPage++;
                }
            }
            LOG.log(Level.INFO, "Page {0}: {1} card(s), {2} new", page + 1, pagePostings.size(), newOnPage);
            if (newOnPage == 0) {
                break;   // Indeed repeats results past the end instead of going empty
            }
            Thread.sleep(BETWEEN_PAGES_MIN_MS
                    + java.util.concurrent.ThreadLocalRandom.current().nextLong(BETWEEN_PAGES_JITTER_MS));
        }
        if (all.isEmpty()) {
            return isChallenged() ? EnumerationResult.challenged() : EnumerationResult.noResults();
        }
        return EnumerationResult.ok(all);
    }

    private boolean isChallenged() {
        return Boolean.TRUE.equals(driver.evaluate(IndeedSelectors.IS_CHALLENGE_JS));
    }

    /** True once the first card's title has hydrated, so titles won't scrape blank. */
    private boolean resultsReady() {
        return Boolean.TRUE.equals(driver.evaluate(IndeedSelectors.RESULTS_READY_JS));
    }

    /**
     * The driver hands back a List of Maps (JS array of objects); map each row to a posting.
     * Posting URLs are built on the search's own regional domain, so opening one later stays in
     * the same region.
     */
    private List<JobPosting> toPostings(Object raw, String country) {
        if (!(raw instanceof List<?> rows)) {
            return List.of();
        }
        List<JobPosting> postings = new ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> map)) {
                continue;
            }
            String jk = str(map.get("jk"));
            if (jk.isBlank()) {
                continue;
            }
            postings.add(new JobPosting(
                    jk,
                    str(map.get("title")),
                    str(map.get("company")),
                    str(map.get("location")),
                    IndeedSelectors.jobUrl(jk, country),
                    Boolean.TRUE.equals(map.get("easilyApply")),
                    str(map.get("snippet"))));
        }
        return postings;
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
