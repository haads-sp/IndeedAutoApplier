package com.haadlit_sp.appCoreLogic.browser;


/**
 * Thin wrapper around the browser-automation library so it can be swapped.
 *
 * <p>Implementations are NOT required to be thread-safe: all calls must come from a
 * single owning thread (the browser worker in {@code AppCore}). Grows additively as
 * later slices need more verbs.
 */
public interface BrowserDriver extends AutoCloseable {

    /**
     * Open a visible browser on the persistent profile. Idempotent.
     *
     * @throws RuntimeException if the profile is already in use by another Chrome window
     */
    void launch();

    /** Navigate the current page to {@code url}. */
    void navigate(String url);

    /** Whether at least one element matching {@code selector} is present right now. Never throws. */
    boolean exists(String selector);

    /** The current page URL, or "" if the browser is not open. Used to tell which apply step we are on. */
    String currentUrl();

    /**
     * Run {@code script} in the current page and return its result. Values map to plain Java:
     * a JS array becomes a {@code List}, an object a {@code Map}, plus String/Boolean/Number.
     * Both Playwright and Selenium support this, so it stays swappable.
     */
    Object evaluate(String script);

    /** Type {@code text} into the first element matching {@code selector} (waits for it; fires real input events). */
    void fill(String selector, String text);

    /** Click the first element matching {@code selector} (waits for it to be actionable). */
    void click(String selector);

    /**
     * Click the first VISIBLE element matching {@code selector}. Indeed's apply form renders several
     * hidden duplicates of buttons like "Continue"; a plain click would hit a hidden one or fail
     * strict-match. Throws if nothing visible matches.
     */
    void clickFirstVisible(String selector);

    /** Choose {@code value} (by value or visible label) in the {@code <select>} matching {@code selector}. */
    void selectOption(String selector, String value);

    /** Select the radio option whose visible label matches {@code optionText}, within group {@code groupName}. */
    void chooseOption(String groupName, String optionText);

    /** Attach {@code file} to the {@code <input type=file>} matching {@code selector} (works even if hidden). */
    void uploadFile(String selector, java.nio.file.Path file);

    /**
     * Click {@code trigger} and supply {@code file} to the file chooser it opens. For upload buttons
     * that open a picker rather than exposing an {@code <input type=file>}. Throws if no chooser opens.
     */
    void uploadViaChooser(String trigger, java.nio.file.Path file);

    /** Close the browser and release all resources. Never throws. */
    @Override
    void close();
}
