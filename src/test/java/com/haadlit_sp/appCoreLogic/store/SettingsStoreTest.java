package com.haadlit_sp.appCoreLogic.store;

import com.haadlit_sp.appCoreLogic.model.AnswerMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsStoreTest {

    @TempDir
    Path dir;

    private SettingsStore store() {
        return new SettingsStore(dir.resolve("settings.tsv"));
    }

    @Test
    void missingFileMeansDefaults() {
        SettingsStore store = store();
        assertEquals(AnswerMode.STANDARD, store.answerMode());
        assertFalse(store.rememberAnswerMode());
    }

    @Test
    void savedChoiceRoundTrips() {
        SettingsStore store = store();
        store.saveAnswerMode(AnswerMode.AI_ENHANCED, true);
        assertEquals(AnswerMode.AI_ENHANCED, store.answerMode());
        assertTrue(store.rememberAnswerMode());
    }

    @Test
    void resavingOverwritesPreviousChoice() {
        SettingsStore store = store();
        store.saveAnswerMode(AnswerMode.AI_ENHANCED, true);
        store.saveAnswerMode(AnswerMode.STANDARD, false);
        assertEquals(AnswerMode.STANDARD, store.answerMode());
        assertFalse(store.rememberAnswerMode());
    }

    @Test
    void garbledFileMeansDefaults() throws IOException {
        Path file = dir.resolve("settings.tsv");
        Files.writeString(file, "answerMode\tTURBO_MODE\nrememberAnswerMode\tmaybe\nnonsense line\n",
                StandardCharsets.UTF_8);
        SettingsStore store = new SettingsStore(file);
        assertEquals(AnswerMode.STANDARD, store.answerMode());
        assertFalse(store.rememberAnswerMode());
    }
}
