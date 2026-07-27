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

    /**
     * Run {@code script} in the current page and return its result. Values map to plain Java:
     * a JS array becomes a {@code List}, an object a {@code Map}, plus String/Boolean/Number.
     * Both Playwright and Selenium support this, so it stays swappable.
     */
    Object evaluate(String script);

    /** Close the browser and release all resources. Never throws. */
    @Override
    void close();
}
