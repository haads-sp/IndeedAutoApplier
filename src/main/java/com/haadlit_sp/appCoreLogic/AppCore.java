package com.haadlit_sp.appCoreLogic;

import com.haadlit_sp.appCoreLogic.answer.QuestionAnswerer;
import com.haadlit_sp.appCoreLogic.answer.QuestionAnswererFactory;
import com.haadlit_sp.appCoreLogic.apply.ApplyResult;
import com.haadlit_sp.appCoreLogic.apply.ApplyWalkthrough;
import com.haadlit_sp.appCoreLogic.browser.BrowserDriver;
import com.haadlit_sp.appCoreLogic.browser.BrowserDriverFactory;
import com.haadlit_sp.appCoreLogic.browser.ChromeProfile;
import com.haadlit_sp.appCoreLogic.browser.IndeedSelectors;
import com.haadlit_sp.appCoreLogic.llm.LlmRuntime;
import com.haadlit_sp.appCoreLogic.llm.LlmRuntimeFactory;
import com.haadlit_sp.appCoreLogic.location.LocationSuggester;
import com.haadlit_sp.appCoreLogic.location.LocationSuggesterFactory;
import com.haadlit_sp.appCoreLogic.model.AnswerMode;
import com.haadlit_sp.appCoreLogic.model.AppliedPosting;
import com.haadlit_sp.appCoreLogic.model.CityLocation;
import com.haadlit_sp.appCoreLogic.model.ContactDetails;
import com.haadlit_sp.appCoreLogic.model.HistoryEntry;
import com.haadlit_sp.appCoreLogic.model.JobPosting;
import com.haadlit_sp.appCoreLogic.model.ProfileFacts;
import com.haadlit_sp.appCoreLogic.model.RunStatus;
import com.haadlit_sp.appCoreLogic.model.SearchCriteria;
import com.haadlit_sp.appCoreLogic.model.SessionDocuments;
import com.haadlit_sp.appCoreLogic.store.QaBankStore;
import com.haadlit_sp.appCoreLogic.model.SubmitMode;
import com.haadlit_sp.appCoreLogic.pdf.PdfTextExtractor;
import com.haadlit_sp.appCoreLogic.pdf.ProfileFactsExtractor;
import com.haadlit_sp.appCoreLogic.search.EnumerationResult;
import com.haadlit_sp.appCoreLogic.search.PostingEnumerator;
import com.haadlit_sp.appCoreLogic.session.LoginStrategy;
import com.haadlit_sp.appCoreLogic.session.LoginStrategyFactory;
import com.haadlit_sp.appCoreLogic.store.ApplicationHistoryStore;
import com.haadlit_sp.appCoreLogic.store.ContactDetailsStore;

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
    /** Owns the local AI download + server startup, which can take minutes on first run. */
    private final ExecutorService llmWorker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "llm-worker");
        t.setDaemon(true);
        return t;
    });
    private final LlmRuntime llmRuntime;   // null in STANDARD mode

    private final PdfTextExtractor pdfText = new PdfTextExtractor();
    private final ProfileFactsExtractor factsExtractor = new ProfileFactsExtractor();
    private final LocationSuggester locationSuggester = LocationSuggesterFactory.create();
    private final ApplicationHistoryStore applicationStore = new ApplicationHistoryStore();
    private final ContactDetailsStore contactStore = new ContactDetailsStore();
    private final QuestionAnswerer answerer;
    private final AnswerMode answerMode;
    private final QaBankStore qaBank = new QaBankStore();

    public AppCore() {
        this(AnswerMode.STANDARD);
    }

    public AppCore(AnswerMode mode) {
        this.answerMode = mode;
        this.llmRuntime = mode == AnswerMode.AI_ENHANCED ? LlmRuntimeFactory.create() : null;
        this.answerer = QuestionAnswererFactory.create(mode, llmRuntime, this::contactDetails);
        if (llmRuntime != null) {
            // Eager: the one-time download and model load overlap sign-in and document picking,
            // instead of stalling the first application. The answerer never blocks on this.
            llmWorker.submit(() -> llmRuntime.ensureReady(msg -> aiStatus = msg));
        }
    }

    /** The answering mode chosen at launch. */
    public AnswerMode answerMode() {
        return answerMode;
    }

    /** Setup/health of the local AI, for the UI to show; blank when there is nothing to report. */
    public String aiStatus() {
        return aiStatus;
    }

    private volatile String aiStatus = "";
    private volatile boolean loggedIn = false;
    private volatile String loginMessage = "Not signed in.";
    private volatile ProfileFacts profileFacts = ProfileFacts.empty();
    private volatile ContactDetails contactDetails = contactStore.load();
    private volatile String documentsMessage = "No resume selected.";
    private volatile List<JobPosting> foundPostings = List.of();
    private volatile String searchMessage = "Not searched yet.";
    private volatile boolean searching = false;
    private volatile boolean applying = false;
    private volatile String applyMessage = "Search first, then apply one posting at a time.";
    private volatile int submittedCount = 0;
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
        if (llmRuntime != null) {
            llmRuntime.shutdown();
        }
        llmWorker.shutdownNow();   // a first-run download in flight is safely resumable
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
            // Seed contact details from the resume the first time, so the details page starts filled.
            if (contactDetails.isEmpty()) {
                contactDetails = ContactDetails.fromFacts(profileFacts);
            }
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

    // ---- Contact details (entered once; fills the contact/location apply steps) ----

    /** Current details — the saved ones, or the resume-seeded guess for the user to confirm. */
    public ContactDetails contactDetails() {
        return contactDetails;
    }

    /** Save the user's confirmed details so they persist and fill future applications. */
    public void setContactDetails(ContactDetails details) {
        this.contactDetails = details == null ? ContactDetails.empty() : details;
        contactStore.save(this.contactDetails);
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
            case CHALLENGED -> {
                // Detach so the human clears the check in a genuinely plain browser — clearance
                // can be refused while a debugger is attached. Next search re-attaches itself.
                closeDriverQuietly();
                searchMessage = "Cloudflare check — the app has disconnected from Chrome. "
                        + "Clear the check in the Chrome window, then search again.";
            }
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

    // ---- Apply (walk the next found posting through the application) ----

    /** Apply to the next not-yet-handled found posting, on the browser worker. One at a time. */
    public void applyToNextPosting() {
        if (applying) {
            return;
        }
        JobPosting next = nextUnhandledPosting();
        if (next == null) {
            applyMessage = foundPostings.isEmpty()
                    ? "Search for jobs first, then apply." : "All found postings have been handled.";
            return;
        }
        applying = true;
        applyMessage = "Opening: " + next.title() + " @ " + next.company() + "…";
        browserWorker.submit(() -> {
            try {
                ApplyResult result = new ApplyWalkthrough(connectedDriver(), answerer, qaBank)
                        .apply(next, contactDetails, profileFacts, documents.resume(), submitMode);
                recordAndReport(next, result);
            } catch (Exception e) {
                LOG.log(Level.ERROR, "Apply failed", e);
                closeDriverQuietly();
                applyMessage = describeBrowserFailure(e);
            } finally {
                applying = false;
            }
        });
    }

    private JobPosting nextUnhandledPosting() {
        Set<String> handled = applicationStore.appliedIds();
        for (JobPosting posting : foundPostings) {
            if (!handled.contains(posting.id())) {
                return posting;
            }
        }
        return null;
    }

    /** Record the posting (so it isn't re-tried) and set a plain-language status. Transient
     * failures (a challenge, an error) are left unrecorded so the user can retry them. */
    private void recordAndReport(JobPosting posting, ApplyResult result) {
        AppliedPosting.Outcome outcome = switch (result.status()) {
            case SUBMITTED -> AppliedPosting.Outcome.SUBMITTED;
            case REVIEW_READY -> AppliedPosting.Outcome.REVIEW_READY;
            case NEEDS_INPUT -> AppliedPosting.Outcome.NEEDS_INPUT;
            case SKIPPED -> AppliedPosting.Outcome.SKIPPED;
            case CHALLENGED, FAILED -> null;
        };
        if (outcome != null) {
            applicationStore.record(new AppliedPosting(posting.id(), java.time.Instant.now(),
                    outcome, posting.title(), posting.company()));
        }
        if (result.status() == ApplyResult.Status.SUBMITTED) {
            submittedCount++;
        }
        if (result.status() == ApplyResult.Status.CHALLENGED) {
            // Same as search: detach so the human clears the check unobserved; next Apply re-attaches.
            closeDriverQuietly();
        }
        applyMessage = switch (result.status()) {
            case SUBMITTED -> "Submitted: " + posting.title() + ". Click Apply next for the next one.";
            case REVIEW_READY -> "Filled and ready — review and Submit \"" + posting.title()
                    + "\" in the browser, then Apply next.";
            case NEEDS_INPUT -> "Needs you: " + result.detail() + " Finish it in the browser, then Apply next.";
            case CHALLENGED -> "Cloudflare check — the app has disconnected from Chrome. "
                    + "Clear it in the Chrome window, then Apply next.";
            case SKIPPED -> "Skipped (not Easy Apply): " + posting.title() + ". Apply next.";
            case FAILED -> "Couldn't apply to \"" + posting.title() + "\": " + result.detail();
        };
    }

    public boolean isApplying() {
        return applying;
    }

    public String applyMessage() {
        return applyMessage;
    }

    public int submittedCount() {
        return submittedCount;
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
