package com.haadlit_sp.appCoreLogic.store;

import java.nio.file.Path;
import java.nio.file.Paths;


/** Single source of truth for the per-user data directory everything persists under. */
public final class AppPaths {

    private AppPaths() {}

    /** The stable per-user home, e.g. C:\Users\me\.indeedapplier — NOT the working directory. */
    public static Path root() {
        return Paths.get(System.getProperty("user.home"), ".indeedapplier");
    }

    /** Append-only log of every posting we applied to; the dedup + audit source of truth. */
    public static Path historyFile() {
        return root().resolve("history.tsv");
    }

    /** The user's personal details, entered once and reused to fill applications. */
    public static Path contactFile() {
        return root().resolve("contact.tsv");
    }

    /** Learned screener answers, keyed by normalised question text. */
    public static Path qaBankFile() {
        return root().resolve("qa-bank.tsv");
    }

    /** App settings (answer mode etc.) — small key/value pairs, never credentials. */
    public static Path settingsFile() {
        return root().resolve("settings.tsv");
    }

    /** Local AI assets: the inference engine under bin/ and model weights under models/. */
    public static Path llmDir() {
        return root().resolve("llm");
    }

    /** Developer diagnostics: issues.tsv plus a screenshot per stuck/failed application step. */
    public static Path diagnosticsDir() {
        return root().resolve("diagnostics");
    }
}
