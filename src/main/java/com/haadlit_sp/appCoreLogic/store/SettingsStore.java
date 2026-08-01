package com.haadlit_sp.appCoreLogic.store;

import com.haadlit_sp.appCoreLogic.model.AnswerMode;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


/**
 * Persists small app settings as {@code key\tvalue} lines — currently the answer mode and whether
 * to skip the startup chooser. Fail-soft: a missing, unreadable or garbled file means defaults.
 * Never credentials, never PII.
 */
public class SettingsStore {

    private static final Logger LOG = System.getLogger(SettingsStore.class.getName());
    private static final String SEP = "\t";
    private static final String KEY_ANSWER_MODE = "answerMode";
    private static final String KEY_REMEMBER = "rememberAnswerMode";

    private final Path file;

    public SettingsStore() {
        this(AppPaths.settingsFile());
    }

    SettingsStore(Path file) {
        this.file = file;
    }

    /** The saved answer mode; an unknown or missing value is the safe {@link AnswerMode#STANDARD}. */
    public AnswerMode answerMode() {
        try {
            return AnswerMode.valueOf(load().getOrDefault(KEY_ANSWER_MODE, ""));
        } catch (IllegalArgumentException e) {
            return AnswerMode.STANDARD;
        }
    }

    /** Whether the user asked to skip the startup chooser and reuse the saved mode. */
    public boolean rememberAnswerMode() {
        return Boolean.parseBoolean(load().getOrDefault(KEY_REMEMBER, "false"));
    }

    /** Save the chosen mode and whether to skip the chooser next launch. Failure is logged, not thrown. */
    public void saveAnswerMode(AnswerMode mode, boolean remember) {
        String out = KEY_ANSWER_MODE + SEP + mode.name() + "\n"
                + KEY_REMEMBER + SEP + remember + "\n";
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not save settings", e);
        }
    }

    private Map<String, String> load() {
        Map<String, String> values = new HashMap<>();
        if (!Files.exists(file)) {
            return values;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] parts = line.split(SEP, 2);
                if (parts.length == 2) {
                    values.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not read settings; using defaults", e);
        }
        return values;
    }
}
