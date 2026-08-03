package com.haadlit_sp.appCoreLogic.store;

import com.haadlit_sp.appCoreLogic.model.RunSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionLogTest {

    @TempDir
    Path dir;

    private static SessionLog.Event event(String status, String detail) {
        return new SessionLog.Event(Instant.now(), "jk1", "IT Support", "Acme", status, detail);
    }

    @Test
    void writesSummaryTimelineAndNotes() throws IOException {
        SessionLog log = new SessionLog(dir);
        log.start("Search: **it support** in Toronto");
        log.add(event("SUBMITTED_VERIFIED", "Submitted — confirmed."));
        log.add(event("FAILED", "A step did not finish loading."));
        log.add(event("FAILED", "A step did not finish loading."));

        Path file = log.write(new RunSummary(20, 2, 3, 2, 0, 1, 1, 1), "finished");
        assertNotNull(file);
        String text = Files.readString(file);

        assertTrue(text.contains("Search: **it support** in Toronto"), "header");
        assertTrue(text.contains("| 20 | 2 | 3 | 2 | 0 | 1 | 1 | 1 |"), "summary row");
        assertTrue(text.contains("IT Support @ Acme"), "timeline entry");
        assertTrue(text.contains("Worked as intended"), "positive note");
        assertTrue(text.contains("2× A step did not finish loading."), "grouped failure note");
    }

    @Test
    void notesCallOutUnverifiedSubmissions() throws IOException {
        SessionLog log = new SessionLog(dir);
        log.start("run");
        log.add(event("SUBMITTED", "Submitted (no confirmation screen seen)."));
        Path file = log.write(new RunSummary(5, 0, 1, 0, 0, 1, 1, 0), "finished");
        assertTrue(Files.readString(file).contains("Unverified"));
    }

    @Test
    void notesFlagAnEnvironmentalPatternWhenEveryAttemptFailed() throws IOException {
        SessionLog log = new SessionLog(dir);
        log.start("run");
        log.add(event("CHALLENGED", "Cloudflare check during the application."));
        Path file = log.write(new RunSummary(9, 0, 1, 1, 0, 0, 0, 0), "finished");
        String text = Files.readString(file);
        assertTrue(text.contains("Cloudflare"), "challenge note");
        assertTrue(text.contains("Unknown issue"), "all-failed pattern note");
    }

    @Test
    void handlesARunThatAttemptedNothing() throws IOException {
        SessionLog log = new SessionLog(dir);
        log.start("run");
        Path file = log.write(RunSummary.empty(), "stopped by the user");
        assertTrue(Files.readString(file).contains("No postings were attempted"));
    }
}
