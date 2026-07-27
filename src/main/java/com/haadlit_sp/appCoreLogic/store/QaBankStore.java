package com.haadlit_sp.appCoreLogic.store;

import com.haadlit_sp.appCoreLogic.model.Answer;
import com.haadlit_sp.appCoreLogic.model.ScreenerQuestion;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;


/**
 * The learned Q&amp;A bank: once the user answers a question the app could not, that answer is saved
 * here keyed by the question's normalised text, so the same question is never asked again — across
 * postings and across runs.
 *
 * <p>Append-only, fail-soft. Line format: {@code normalisedKey \t value1|value2|…}. Answers are
 * plain text the user typed for a form, never anything sensitive.
 */
public class QaBankStore {

    private static final Logger LOG = System.getLogger(QaBankStore.class.getName());
    private static final String SEP = "\t";
    private static final String VALUE_SEP = "|";

    private final Path file;
    private final Map<String, List<String>> byKey = new LinkedHashMap<>();

    public QaBankStore() {
        this(AppPaths.root().resolve("qa-bank.tsv"));
    }

    QaBankStore(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] f = line.split(SEP, 2);
                if (f.length == 2 && !f[0].isBlank()) {
                    byKey.put(f[0], List.of(f[1].split("\\" + VALUE_SEP, -1)));
                }
            }
            LOG.log(Level.INFO, "Loaded {0} learned answers", byKey.size());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not read Q&A bank; starting empty", e);
        }
    }

    /** A previously learned answer for this question, if any. */
    public Optional<Answer> lookup(ScreenerQuestion question) {
        List<String> values = byKey.get(key(question));
        return values == null ? Optional.empty() : Optional.of(Answer.of(values, Answer.Source.BANK));
    }

    /** Remember the user's answer so this question is never asked again. Failure is logged, not thrown. */
    public void remember(ScreenerQuestion question, Answer answer) {
        String key = key(question);
        byKey.put(key, answer.values());
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, key + SEP + join(answer.values()) + "\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not persist learned answer", e);
        }
    }

    private static String join(List<String> values) {
        return String.join(VALUE_SEP, values.stream().map(QaBankStore::clean).toList());
    }

    /** Same question, differently worded/spaced, should map to the same key. */
    private static String key(ScreenerQuestion question) {
        return question.text().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip();
    }

    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("[\t\r\n|]", " ");
    }
}
