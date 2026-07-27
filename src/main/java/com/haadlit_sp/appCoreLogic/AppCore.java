package com.haadlit_sp.appCoreLogic;

import com.haadlit_sp.appCoreLogic.browser.BrowserDriver;
import com.haadlit_sp.appCoreLogic.browser.BrowserDriverFactory;
import com.haadlit_sp.appCoreLogic.browser.ChromeProfile;
import com.haadlit_sp.appCoreLogic.browser.IndeedSelectors;
import com.haadlit_sp.appCoreLogic.location.LocationSuggester;
import com.haadlit_sp.appCoreLogic.location.LocationSuggesterFactory;
import com.haadlit_sp.appCoreLogic.model.CityLocation;
import com.haadlit_sp.appCoreLogic.model.HistoryEntry;
import com.haadlit_sp.appCoreLogic.model.JobPosting;
import com.haadlit_sp.appCoreLogic.model.ProfileFacts;
import com.haadlit_sp.appCoreLogic.model.RunStatus;
import com.haadlit_sp.appCoreLogic.model.SearchCriteria;
import com.haadlit_sp.appCoreLogic.model.SessionDocuments;
import com.haadlit_sp.appCoreLogic.model.SubmitMode;
import com.haadlit_sp.appCoreLogic.pdf.PdfTextExtractor;
import com.haadlit_sp.appCoreLogic.pdf.ProfileFactsExtractor;
import com.haadlit_sp.appCoreLogic.search.EnumerationResult;
import com.haadlit_sp.appCoreLogic.search.PostingEnumerator;
import com.haadlit_sp.appCoreLogic.session.LoginStrategy;
import com.haadlit_sp.appCoreLogic.session.LoginStrategyFactory;
import com.haadlit_sp.appCoreLogic.store.ApplicationHistoryStore;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Facade: the single class the UI talks to. It owns (will own) the browser driver,
 * PDF parser, answerers and stores, and exposes a small verb API.
 *
 * <p>For this skeleton slice the automation verbs are logged stubs that mutate in-memory
 * state so the UI is demoable. The real engines (browser, PDF, answerers, stores) arrive
 * in later slices behind their own interfaces + factories, without changing this API.
 */
public class AppCore {

    private static final Logger LOG = System.getLogger(AppCore.class.getName());

    /** Single thread that owns the (thread-affine) browser driver and all its calls. */
    private final ExecutorService browserWorker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "browser-worker");
        t.setDaemon(true);
        return t;
    });
    private BrowserDriver driver;

    /** Separate from the browser worker, which can sit in a multi-minute sign-in poll. */
    private final ExecutorService docWorker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "doc-worker");
        t.setDaemon(true);
        return t;
    });
    private final PdfTextExtractor pdfText = new PdfTextExtractor();
    private final ProfileFactsExtractor factsExtractor = new ProfileFactsExtractor();
    private final LocationSuggester locationSuggester = LocationSuggesterFactory.create();
    private final ApplicationHistoryStore applicationStore = new ApplicationHistoryStore();

    private volatile boolean loggedIn = false;
    private volatile String loginMessage = "Not signed in.";
    private volatile ProfileFacts profileFacts = ProfileFacts.empty();
    private volatile String documentsMessage = "No resume selected.";
    private volatile List<JobPosting> foundPostings = List.of();
    private volatile String searchMessage = "Not searched yet.";
    private volatile boolean searching = false;
    private volatile SubmitMode submitMode = SubmitMode.REVIEW; // safe default: never auto-submit
    private SessionDocuments documents = SessionDocuments.empty();
    private SearchCriteria criteria = SearchCriteria.blank();
    private volatile RunStatus status = RunStatus.idle();
    private final List<HistoryEntry> history = new ArrayList<>();

    // ---- Session / login ----

    /**
     * Open the user's REAL Chrome (no automation flags, {@code webdriver === false}) on the
     * dedicated profile, exposing a debugging port. The user signs in and clears any Cloudflare
     * check as a human — which works precisely because this is a real browser — and LEAVES IT OPEN.
     * The app then attaches to that same window over CDP; it never launches its own, because a
     * Playwright-launched browser is walled and cannot be cleared even by a human click.
     */
    public void openSignInBrowser() {
        String chrome = ChromeProfile.executable();
        if (chrome == null) {
            loginMessage = "Google Chrome not found — install Chrome to continue.";
            return;
        }
        try {
            Files.createDirectories(ChromeProfile.dir());
            new ProcessBuilder(chrome,
                    "--remote-debugging-port=" + ChromeProfile.DEBUG_PORT,
                    "--user-data-dir=" + ChromeProfile.dir(),
                    IndeedSelectors.LOGIN_URL).start();
            loginMessage = "Sign in and clear any check, then LEAVE this window open and click Verify.";
        } catch (IOException e) {
            LOG.log(Level.ERROR, "Could not launch Chrome", e);
            loginMessage = "Could not launch Chrome: " + e.getMessage();
        }
    }

    /** Attach to the open Chrome and confirm it is signed in. */
    public void verifySignIn() {
        runLogin(LoginStrategyFactory.manual());
    }

    /** Runs a login strategy on the browser worker; UI polls {@link #isLoggedIn()} / {@link #loginMessage()}. */
    private void runLogin(LoginStrategy strategy) {
        loggedIn = false;
        loginMessage = "Attaching to your Chrome…";
        browserWorker.submit(() -> {
            try {
                loggedIn = strategy.login(connectedDriver(), msg -> loginMessage = msg);
            } catch (Exception e) {
                LOG.log(Level.ERROR, "Sign-in check failed", e);
                closeDriverQuietly();
                loginMessage = describeBrowserFailure(e);
            }
        });
    }

    /** The (lazily created) driver, attached over CDP. Must be called on the browser worker. */
    private BrowserDriver connectedDriver() {
        if (driver == null) {
            driver = BrowserDriverFactory.create();
        }
        driver.launch(); // connectOverCDP; idempotent once attached
        return driver;
    }

    /** Drop the driver so the next attempt reconnects to a fresh session. */
    private void closeDriverQuietly() {
        if (driver != null) {
            try {
                driver.close();
            } catch (RuntimeException ignored) {
                // best-effort
            }
            driver = null;
        }
    }

    /** The most common failure is that no Chrome is up to attach to yet. */
    private static String describeBrowserFailure(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        String lower = message.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("connect") || lower.contains("refused") || lower.contains("econnrefused")) {
            return "No Chrome to attach to — click \"Open Chrome to sign in\" first and leave it open.";
        }
        return "Browser error: " + message;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String loginMessage() {
        return loginMessage;
    }

    /** Close the browser and stop the workers. Called when the app window closes. */
    public void shutdown() {
        browserWorker.submit(() -> {
            if (driver != null) {
                driver.close();
            }
        });
        browserWorker.shutdown();
        docWorker.shutdown();
    }

    // ---- Documents ----

    /** Accepts the chosen PDFs and extracts the profile fact base off the EDT. */
    public void loadDocuments(SessionDocuments docs) {
        this.documents = docs == null ? SessionDocuments.empty() : docs;
        if (!this.documents.hasResume()) {
            profileFacts = ProfileFacts.empty();
            documentsMessage = "No resume selected.";
            return;
        }
        documentsMessage = "Reading documents…";
        SessionDocuments snapshot = this.documents;
        docWorker.submit(() -> extractFacts(snapshot));
    }

    private void extractFacts(SessionDocuments docs) {
        try {
            List<Path> files = new ArrayList<>();
            files.add(docs.resume());
            if (docs.coverLetter() != null) {
                files.add(docs.coverLetter());
            }
            files.addAll(docs.supporting());

            StringBuilder combined = new StringBuilder();
            List<String> unreadable = new ArrayList<>();
            for (Path file : files) {
                String text = pdfText.extract(file);
                if (text.isBlank()) {
                    unreadable.add(file.getFileName().toString());
                } else {
                    combined.append(text).append('\n');
                }
            }
            if (combined.isEmpty()) {
                profileFacts = ProfileFacts.empty();
                documentsMessage = "No readable text found — is it a scanned/image-only PDF?";
                return;
            }
            profileFacts = factsExtractor.extract(combined.toString());
            documentsMessage = unreadable.isEmpty()
                    ? "Documents read."
                    : "Read, but skipped: " + String.join(", ", unreadable);
        } catch (Exception e) {
            LOG.log(Level.ERROR, "Document extraction failed", e);
            profileFacts = ProfileFacts.empty();
            documentsMessage = "Could not read documents: " + e.getMessage();
        }
    }

    public SessionDocuments documents() {
        return documents;
    }

    public ProfileFacts profileFacts() {
        return profileFacts;
    }

    public String documentsMessage() {
        return documentsMessage;
    }

    // ---- Search criteria ----

    public void setSearchCriteria(SearchCriteria c) {
        this.criteria = c == null ? SearchCriteria.blank() : c;
    }

    public SearchCriteria searchCriteria() {
        return criteria;
    }

    /** Places matching what the user has typed so far. Cheap enough to call on every keystroke. */
    public List<CityLocation> suggestLocations(String typed, int limit) {
        return locationSuggester.suggest(typed, limit);
    }

    // ---- Search (read-only: find & list postings; applying comes in a later slice) ----

    /** Run one search on the browser worker; the UI polls {@link #searchMessage()} / {@link #foundPostings()}. */
    public void startSearch() {
        if (searching) {
            return;
        }
        if (!criteria.isReady()) {
            searchMessage = "Add a job and a location first (steps 2 and 3).";
            return;
        }
        searching = true;
        searchMessage = "Searching…";
        SearchCriteria snapshot = criteria;
        browserWorker.submit(() -> {
            try {
                EnumerationResult result = new PostingEnumerator(connectedDriver()).enumerate(snapshot);
                applySearchResult(result);
            } catch (Exception e) {
                LOG.log(Level.ERROR, "Search failed", e);
                closeDriverQuietly();
                searchMessage = describeBrowserFailure(e);
            } finally {
                searching = false;
            }
        });
    }

    private void applySearchResult(EnumerationResult result) {
        switch (result.outcome()) {
            case OK -> {
                foundPostings = result.postings();
                long fresh = result.postings().stream().filter(p -> !applicationStore.contains(p.id())).count();
                searchMessage = "Found " + result.postings().size() + " postings (" + fresh + " new).";
            }
            case CHALLENGED -> searchMessage =
                    "Cloudflare check — solve it in the Chrome window, then search again.";
            case NO_RESULTS -> {
                foundPostings = List.of();
                searchMessage = "No matching postings found — try a broader search.";
            }
        }
    }

    public List<JobPosting> foundPostings() {
        return foundPostings;
    }

    public String searchMessage() {
        return searchMessage;
    }

    public boolean isSearching() {
        return searching;
    }

    /** How far the app should go on each application. Consumed by the apply walkthrough. */
    public void setSubmitMode(SubmitMode mode) {
        this.submitMode = mode == null ? SubmitMode.REVIEW : mode;
    }

    public SubmitMode submitMode() {
        return submitMode;
    }

    /** Whether a posting has already been applied to in a past run (for the New/Applied tag). */
    public Set<String> appliedPostingIds() {
        return applicationStore.appliedIds();
    }

    // ---- Run control ----

    public void startRun() {
        status = new RunStatus(true, false, "—",
                status.submitted(), status.skipped(), status.failed(),
                "Run started (stub) — the automation engine arrives in a later slice");
        LOG.log(Level.INFO, "startRun (stub)");
    }

    public void pauseRun() {
        status = new RunStatus(status.running(), true, status.currentPosting(),
                status.submitted(), status.skipped(), status.failed(), "Paused");
        LOG.log(Level.INFO, "pauseRun (stub)");
    }

    public void stopRun() {
        status = new RunStatus(false, false, "—",
                status.submitted(), status.skipped(), status.failed(), "Stopped");
        LOG.log(Level.INFO, "stopRun (stub)");
    }

    public RunStatus status() {
        return status;
    }

    public List<HistoryEntry> history() {
        return List.copyOf(history);
    }
}
