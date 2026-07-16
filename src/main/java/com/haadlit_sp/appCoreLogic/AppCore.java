package com.haadlit_sp.appCoreLogic;

import com.haadlit_sp.appCoreLogic.browser.BrowserDriver;
import com.haadlit_sp.appCoreLogic.browser.BrowserDriverFactory;
import com.haadlit_sp.appCoreLogic.browser.ChromeProfile;
import com.haadlit_sp.appCoreLogic.browser.IndeedSelectors;
import com.haadlit_sp.appCoreLogic.model.HistoryEntry;
import com.haadlit_sp.appCoreLogic.model.ProfileFacts;
import com.haadlit_sp.appCoreLogic.model.RunStatus;
import com.haadlit_sp.appCoreLogic.model.SearchCriteria;
import com.haadlit_sp.appCoreLogic.model.SessionDocuments;
import com.haadlit_sp.appCoreLogic.pdf.PdfTextExtractor;
import com.haadlit_sp.appCoreLogic.pdf.ProfileFactsExtractor;
import com.haadlit_sp.appCoreLogic.session.LoginStrategy;
import com.haadlit_sp.appCoreLogic.session.LoginStrategyFactory;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

    private volatile boolean loggedIn = false;
    private volatile String loginMessage = "Not signed in.";
    private volatile ProfileFacts profileFacts = ProfileFacts.empty();
    private volatile String documentsMessage = "No resume selected.";
    private SessionDocuments documents = SessionDocuments.empty();
    private SearchCriteria criteria = SearchCriteria.blank();
    private volatile RunStatus status = RunStatus.idle();
    private final List<HistoryEntry> history = new ArrayList<>();

    // ---- Session / login ----

    /**
     * Open a PLAIN Chrome window (no Playwright, no automation flags) on the dedicated profile so
     * the user can sign in like a normal person. This is what keeps Google and Cloudflare from
     * rejecting the sign-in. They must close that window before {@link #verifySignIn()} can drive it.
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
                    "--user-data-dir=" + ChromeProfile.dir(),
                    IndeedSelectors.LOGIN_URL).start();
            loginMessage = "Sign in, then CLOSE that Chrome window and click Verify.";
        } catch (IOException e) {
            LOG.log(Level.ERROR, "Could not launch Chrome", e);
            loginMessage = "Could not launch Chrome: " + e.getMessage();
        }
    }

    /** Reopen the saved profile under automation and confirm it is still signed in. */
    public void verifySignIn() {
        runLogin(LoginStrategyFactory.manual());
    }

    /** Runs a login strategy on the browser worker; UI polls {@link #isLoggedIn()} / {@link #loginMessage()}. */
    private void runLogin(LoginStrategy strategy) {
        loggedIn = false;
        loginMessage = "Opening the saved Chrome profile…";
        browserWorker.submit(() -> {
            try {
                if (driver == null) {
                    driver = BrowserDriverFactory.create();
                }
                loggedIn = strategy.login(driver, msg -> loginMessage = msg);
            } catch (Exception e) {
                LOG.log(Level.ERROR, "Sign-in check failed", e);
                loginMessage = describeLoginFailure(e);
            }
        });
    }

    /** Chrome locks its profile directory, which is the failure users will hit most often. */
    private static String describeLoginFailure(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (message.contains("user data directory") || message.contains("ProcessSingleton")) {
            return "Close the Chrome window you signed in with, then click Verify again.";
        }
        return "Sign-in check failed: " + message;
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
