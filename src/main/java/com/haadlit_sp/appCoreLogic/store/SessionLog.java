package com.haadlit_sp.appCoreLogic.store;

import com.haadlit_sp.appCoreLogic.model.RunSummary;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * A readable record of one apply run: what was searched, what happened to each posting, and notes
 * calling out what worked and what needs attention. Written to
 * {@code diagnostics/sessions/<timestamp>.md} so runs can be compared over time — the point is to
 * see whether a fix actually held in the real world, not just that a run happened.
 *
 * <p>Notes are derived from the run's own events (recurring failures grouped, verified submissions
 * counted) rather than guessed, so two sessions can be read side by side and trusted.
 */
public class SessionLog {

    private static final Logger LOG = System.getLogger(SessionLog.class.getName());
    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter READABLE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /** One posting's outcome within the run. */
    public record Event(Instant at, String postingId, String title, String company,
                        String status, String detail) {}

    /** One fine-grained action inside an application (see {@code ApplyJournal}). */
    public record Step(Instant at, String postingId, String phase, String detail) {}

    private final Path dir;
    private final List<Event> events = new ArrayList<>();
    private final List<Step> steps = new ArrayList<>();
    /** Reset per run: it stamps the filename, so runs in one app session must not collide. */
    private Instant startedAt = Instant.now();
    private String header = "";

    public SessionLog() {
        this(AppPaths.diagnosticsDir().resolve("sessions"));
    }

    public SessionLog(Path dir) {
        this.dir = dir;
    }

    /** Describe the run's setup (search, modes) — recorded verbatim at the top of the file. */
    public void start(String header) {
        this.header = header;
        this.startedAt = Instant.now();
        events.clear();
        steps.clear();
    }

    public void add(Event event) {
        events.add(event);
    }

    /** Record one action inside the application currently being walked. Thread-safe enough: the
     *  browser worker is the only writer. */
    public void addStep(Step step) {
        steps.add(step);
    }

    /** Write the session file. Returns the file, or null when it could not be written. */
    public Path write(RunSummary summary, String endedBecause) {
        StringBuilder out = new StringBuilder();
        out.append("# Apply run — ").append(READABLE.format(startedAt)).append("\n\n")
                .append(header).append("\n\n")
                .append("Ended: ").append(endedBecause)
                .append(" · duration ").append(Duration.between(startedAt, Instant.now()).toMinutes())
                .append(" min\n\n");

        out.append("## Summary\n\n")
                .append("| Found | Skipped | Attempted | Abandoned | Needs you | Finished | Submitted | Verified |\n")
                .append("|---|---|---|---|---|---|---|---|\n")
                .append("| ").append(summary.found())
                .append(" | ").append(summary.skipped())
                .append(" | ").append(summary.attempted())
                .append(" | ").append(summary.abandoned())
                .append(" | ").append(summary.needsInput())
                .append(" | ").append(summary.finished())
                .append(" | ").append(summary.submitted())
                .append(" | ").append(summary.verified())
                .append(" |\n\n");

        out.append("## Timeline\n\n");
        if (events.isEmpty()) {
            out.append("_No postings were attempted._\n\n");
        } else {
            out.append("| Time | Posting | Outcome | Detail |\n|---|---|---|---|\n");
            for (Event e : events) {
                out.append("| ").append(READABLE.format(e.at()).substring(11))
                        .append(" | ").append(cell(e.title()))
                        .append(e.company().isBlank() ? "" : " @ " + cell(e.company()))
                        .append(" | ").append(e.status())
                        .append(" | ").append(cell(e.detail())).append(" |\n");
            }
            out.append('\n');
        }

        out.append(detail()).append("## Notes\n\n").append(notes(summary));
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(FILE_STAMP.format(startedAt) + ".md");
            Files.writeString(file, out.toString());
            return file;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not write the session log", e);
            return null;
        }
    }

    /**
     * Blow-by-blow of each application: modules entered, questions asked, the answer chosen and
     * where it came from, whether it landed in the field, clicks, waits and interstitials. This is
     * the section that explains WHY an application stopped where it did.
     */
    private String detail() {
        if (steps.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder("## What happened, step by step\n\n");
        String currentPosting = null;
        for (Step step : steps) {
            if (!step.postingId().equals(currentPosting)) {
                currentPosting = step.postingId();
                out.append("\n### ").append(titleFor(currentPosting)).append("\n\n");
            }
            out.append("- `").append(READABLE.format(step.at()).substring(11)).append("` **")
                    .append(step.phase()).append("** — ").append(cell(step.detail())).append('\n');
        }
        return out.append('\n').toString();
    }

    /** The posting's title from its outcome event, so the detail section reads in plain language. */
    private String titleFor(String postingId) {
        for (Event event : events) {
            if (event.postingId().equals(postingId)) {
                return event.title() + (event.company().isBlank() ? "" : " @ " + event.company())
                        + " — " + event.status();
            }
        }
        return postingId;
    }

    /** Plain-language read of the run, derived from its own events. */
    private String notes(RunSummary summary) {
        StringBuilder notes = new StringBuilder();

        if (summary.verified() > 0) {
            notes.append("- **Worked as intended:** ").append(summary.verified())
                    .append(" application(s) submitted with a confirmation seen afterwards.\n");
        }
        if (summary.submitted() > summary.verified()) {
            notes.append("- **Unverified:** ").append(summary.submitted() - summary.verified())
                    .append(" submitted without a confirmation screen being identified — either the")
                    .append(" confirmation wording is new, or the submit did not land. Worth a look.\n");
        }
        if (summary.finished() > summary.submitted()) {
            notes.append("- ").append(summary.finished() - summary.submitted())
                    .append(" application(s) reached the end of the form but were not submitted")
                    .append(" (review mode, or the auto-submit gate did not open).\n");
        }

        Map<String, Integer> failures = new LinkedHashMap<>();
        int challenged = 0;
        for (Event e : events) {
            if (e.status().equals("CHALLENGED")) {
                challenged++;
            } else if (e.status().equals("FAILED") || e.status().equals("NEEDS_INPUT")) {
                failures.merge(e.detail(), 1, Integer::sum);
            }
        }
        if (!failures.isEmpty()) {
            notes.append("- **Went wrong** (grouped, most common first):\n");
            failures.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .forEach(entry -> notes.append("    - ").append(entry.getValue())
                            .append("× ").append(entry.getKey()).append('\n'));
        }
        if (challenged > 0) {
            notes.append("- **Cloudflare:** the run hit a human-verification wall (")
                    .append(challenged).append("×). Runs stop there by design — clear the check in")
                    .append(" the browser before starting again, and give the account a rest if it")
                    .append(" keeps happening.\n");
        }
        if (summary.attempted() > 0 && summary.abandoned() == summary.attempted()) {
            notes.append("- **Unknown issue:** every attempt failed the same way. That pattern")
                    .append(" usually means something environmental (signed out, blocked, offline)")
                    .append(" rather than a per-posting problem — check the screenshots.\n");
        }
        if (notes.isEmpty()) {
            notes.append("- Nothing notable recorded.\n");
        }
        notes.append("\n_Screenshots for anything that stopped short are in the parent")
                .append(" `diagnostics/` folder, timestamped to match the timeline._\n");
        return notes.toString();
    }

    private static String cell(String value) {
        return value == null ? "" : value.replace("|", "\\|").replaceAll("[\r\n]", " ").strip();
    }
}
