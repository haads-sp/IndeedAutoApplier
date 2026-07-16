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

    /** Dedicated profile directory; holds the signed-in Indeed session between runs. */
    public static Path dir() {
        return Paths.get(System.getProperty("user.home"), ".indeedapplier", "chrome-profile");
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
