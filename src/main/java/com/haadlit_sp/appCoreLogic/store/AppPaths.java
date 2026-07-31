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
}
