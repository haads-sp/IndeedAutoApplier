package com.haadlit_sp.appCoreLogic.store;

import com.haadlit_sp.appCoreLogic.model.AppliedPosting;
import com.haadlit_sp.appCoreLogic.model.AppliedPosting.Outcome;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;


/**
 * The dedup + audit store: an append-only tab-separated log of every posting we processed.
 *
 * <p>It is the source of truth for "have we already applied to this?", so re-running the same
 * search skips postings already handled. Fail-soft throughout — a corrupt line is skipped, a
 * missing file is an empty history, and a write failure never crashes the run.
 *
 * <p>Format per line: {@code jobId \t epochMillis \t outcome \t title \t company}. Tabs inside a
 * field are replaced with spaces so a stray tab can never shift the columns.
 */
public class ApplicationHistoryStore {

    private static final Logger LOG = System.getLogger(ApplicationHistoryStore.class.getName());
    private static final int MAX_ENTRIES = 5000;
    private static final String SEP = "\t";

    private final Path file;
    /** Insertion order = chronological; last write for a job id wins. */
    private final LinkedHashMap<String, AppliedPosting> byId = new LinkedHashMap<>();

    public ApplicationHistoryStore() {
        this(AppPaths.historyFile());
    }

    ApplicationHistoryStore(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                parse(line).ifPresent(p -> byId.put(p.jobId(), p));
            }
            trimToCap();
            LOG.log(Level.INFO, "Loaded {0} history entries", byId.size());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not read history; starting empty", e);
        }
    }

    /** Bound memory if the log has grown huge: keep only the most recent entries. */
    private void trimToCap() {
        while (byId.size() > MAX_ENTRIES) {
            byId.remove(byId.keySet().iterator().next()); // oldest, since insertion-ordered
        }
    }

    private java.util.Optional<AppliedPosting> parse(String line) {
        if (line == null || line.isBlank()) {
            return java.util.Optional.empty();
        }
        String[] f = line.split(SEP, -1);
        if (f.length < 5) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(new AppliedPosting(
                    f[0], Instant.ofEpochMilli(Long.parseLong(f[1])),
                    Outcome.valueOf(f[2]), f[3], f[4]));
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Skipping unreadable history line: {0}", line);
            return java.util.Optional.empty();
        }
    }

    /** Whether this posting has already been processed in any past run. */
    public boolean contains(String jobId) {
        return byId.containsKey(jobId);
    }

    public Set<String> appliedIds() {
        return Set.copyOf(byId.keySet());
    }

    /** Append one processed posting to the log and remember it. Failure is logged, never thrown. */
    public void record(AppliedPosting posting) {
        byId.put(posting.jobId(), posting);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, toLine(posting) + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not persist history entry for " + posting.jobId(), e);
        }
    }

    /** Most recent entries first, capped, for the run-page history list. */
    public List<AppliedPosting> recent(int limit) {
        List<AppliedPosting> all = new ArrayList<>(byId.values());
        List<AppliedPosting> recent = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && recent.size() < limit; i--) {
            recent.add(all.get(i));
        }
        return recent;
    }

    private static String toLine(AppliedPosting p) {
        return String.join(SEP,
                clean(p.jobId()),
                Long.toString(p.appliedAt().toEpochMilli()),
                p.outcome().name(),
                clean(p.title()),
                clean(p.company()));
    }

    /** Keep tabs and newlines out of a field so the columns can never shift. */
    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("[\t\r\n]", " ");
    }
}
