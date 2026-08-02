package com.haadlit_sp.appCoreLogic.store;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


/**
 * Developer diagnostics for the prototype: every application that gets stuck, fails, or hits a
 * challenge is appended to {@code diagnostics/issues.tsv} with a screenshot of the page it died on.
 * The log is what turns a vague "it got stuck on some page" into an exact question, module URL and
 * picture — each entry is raw material for a fix, and re-running shows whether the fix held.
 * Append-only, fail-soft: diagnostics must never break an actual run.
 */
public class DiagnosticsLog {

    private static final Logger LOG = System.getLogger(DiagnosticsLog.class.getName());
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    private final Path dir;

    public DiagnosticsLog() {
        this(AppPaths.diagnosticsDir());
    }

    public DiagnosticsLog(Path dir) {
        this.dir = dir;
    }

    /** Where the screenshot for this incident should be written (parent dirs created). */
    public Path screenshotFile(String postingId) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not create the diagnostics directory", e);
        }
        return dir.resolve(STAMP.format(Instant.now()) + "-" + postingId + ".png");
    }

    /** Append one incident line: when, posting, status, detail, page URL, screenshot file. */
    public void record(String postingId, String title, String status, String detail,
                       String pageUrl, Path screenshot) {
        String line = String.join("\t",
                Instant.now().toString(),
                postingId,
                clean(title),
                status,
                clean(detail),
                clean(pageUrl),
                screenshot == null ? "" : screenshot.getFileName().toString()) + "\n";
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("issues.tsv"), line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not write the diagnostics log", e);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("[\t\r\n]", " ").strip();
    }
}
