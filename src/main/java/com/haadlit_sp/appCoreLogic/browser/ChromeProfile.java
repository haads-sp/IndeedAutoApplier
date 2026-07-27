package com.haadlit_sp.appCoreLogic.browser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * The dedicated Chrome profile the app drives, and where Chrome itself is installed.
 *
 * <p>The user signs in once in a plain Chrome window on this profile; the session cookies
 * persist here so later automated runs start already signed in. This is the one place that
 * knowingly persists session state to disk — deleting the directory signs the user out.
 */
public final class ChromeProfile {

    private ChromeProfile() {}

    private static final String CHROME_SUFFIX = "\\Google\\Chrome\\Application\\chrome.exe";
    private static final String[] CHROME_ENV_ROOTS = {"ProgramFiles", "ProgramFiles(x86)", "LOCALAPPDATA"};

    /**
     * Port the app-launched Chrome exposes for automation. The app attaches to that real,
     * human-controlled browser over CDP — it never launches an automation-flagged one, which is
     * what keeps Cloudflare and Google treating the session as the real browser it is.
     */
    public static final int DEBUG_PORT = 9222;

    /** Dedicated profile directory; holds the signed-in Indeed session between runs. */
    public static Path dir() {
        return Paths.get(System.getProperty("user.home"), ".indeedapplier", "chrome-profile");
    }

    /** CDP endpoint of the app-launched Chrome. */
    public static String cdpEndpoint() {
        return "http://127.0.0.1:" + DEBUG_PORT;
    }

    /** Installed Chrome executable, or {@code null} when Chrome is not present. */
    public static String executable() {
        for (String env : CHROME_ENV_ROOTS) {
            String root = System.getenv(env);
            if (root == null) {
                continue;
            }
            Path candidate = Paths.get(root + CHROME_SUFFIX);
            if (Files.isRegularFile(candidate)) {
                return candidate.toString();
            }
        }
        return null;
    }
}
