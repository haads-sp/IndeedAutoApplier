package com.haadlit_sp.appCoreLogic;

import com.haadlit_sp.appCoreLogic.model.HistoryEntry;
import com.haadlit_sp.appCoreLogic.model.RunStatus;
import com.haadlit_sp.appCoreLogic.model.SearchCriteria;
import com.haadlit_sp.appCoreLogic.model.SessionDocuments;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;


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

    private volatile boolean loggedIn = false;
    private SessionDocuments documents = SessionDocuments.empty();
    private SearchCriteria criteria = SearchCriteria.blank();
    private volatile RunStatus status = RunStatus.idle();
    private final List<HistoryEntry> history = new ArrayList<>();

    // ---- Session / login ----

    public void startManualLogin() {
        LOG.log(Level.INFO, "startManualLogin (stub) — will open a headful browser for the user to sign in");
    }

    public void loginWithCredentials(String email, char[] password) {
        LOG.log(Level.INFO, "loginWithCredentials (stub) for {0}", email);
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    // ---- Documents ----

    public void loadDocuments(SessionDocuments docs) {
        this.documents = docs == null ? SessionDocuments.empty() : docs;
        LOG.log(Level.INFO, "loadDocuments (stub): resume={0}", this.documents.resume());
    }

    public SessionDocuments documents() {
        return documents;
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
