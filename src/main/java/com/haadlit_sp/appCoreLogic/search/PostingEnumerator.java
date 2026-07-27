package com.haadlit_sp.appCoreLogic.search;

import com.haadlit_sp.appCoreLogic.browser.BrowserDriver;
import com.haadlit_sp.appCoreLogic.browser.IndeedSelectors;
import com.haadlit_sp.appCoreLogic.model.JobPosting;
import com.haadlit_sp.appCoreLogic.model.SearchCriteria;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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

    private final BrowserDriver driver;

    public PostingEnumerator(BrowserDriver driver) {
        this.driver = driver;
    }

    /**
     * The first page of postings for these criteria. Reports CHALLENGED when Cloudflare is asking
     * the human to verify, so the caller can pause rather than mistake it for empty results.
     */
    public EnumerationResult enumerate(SearchCriteria criteria) throws InterruptedException {
        String url = IndeedSelectors.searchUrl(
                criteria.jobQuery(), criteria.city(),
                criteria.radius().km(), criteria.datePosted().days());
        LOG.log(Level.INFO, "Searching: {0}", url);
        driver.navigate(url);

        for (int i = 0; i < WAIT_ATTEMPTS; i++) {
            if (isChallenged()) {
                LOG.log(Level.INFO, "Cloudflare check is up; waiting for the human to clear it");
                return EnumerationResult.challenged();
            }
            if (resultsReady()) {
                return EnumerationResult.ok(toPostings(driver.evaluate(IndeedSelectors.SCRAPE_POSTINGS_JS)));
            }
            Thread.sleep(WAIT_MS);
        }
        return isChallenged() ? EnumerationResult.challenged() : EnumerationResult.noResults();
    }

    private boolean isChallenged() {
        return Boolean.TRUE.equals(driver.evaluate(IndeedSelectors.IS_CHALLENGE_JS));
    }

    /** True once the first card's title has hydrated, so titles won't scrape blank. */
    private boolean resultsReady() {
        return Boolean.TRUE.equals(driver.evaluate(IndeedSelectors.RESULTS_READY_JS));
    }

    /** The driver hands back a List of Maps (JS array of objects); map each row to a posting. */
    private List<JobPosting> toPostings(Object raw) {
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
                    IndeedSelectors.jobUrl(jk),
                    Boolean.TRUE.equals(map.get("easilyApply"))));
        }
        return postings;
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
