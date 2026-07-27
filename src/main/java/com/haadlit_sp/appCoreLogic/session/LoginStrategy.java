package com.haadlit_sp.appCoreLogic.session;

import com.haadlit_sp.appCoreLogic.browser.BrowserDriver;
import com.haadlit_sp.appCoreLogic.browser.IndeedSelectors;

import java.util.function.Consumer;


/**
 * Confirms the browser is in a signed-in state. Implementations run on the browser worker
 * thread and block until sign-in is detected or they give up.
 */
public interface LoginStrategy {

    /**
     * @param driver the (already-created) browser driver
     * @param status callback for short human-readable progress messages
     * @return true once a signed-in state is detected
     */
    boolean login(BrowserDriver driver, Consumer<String> status) throws InterruptedException;

    /**
     * Shared detection: the account menu only renders when signed in (verified against the live
     * site). Deliberately a single positive signal — inferring sign-in from the ABSENCE of a
     * sign-out marker would report success whenever that selector merely broke.
     */
    default boolean isSignedIn(BrowserDriver driver) {
        return driver.exists(IndeedSelectors.ACCOUNT_MENU);
    }

    /** A Cloudflare bot check is on screen; the human must clear it, we never bypass it. */
    default boolean isChallenged(BrowserDriver driver) {
        return Boolean.TRUE.equals(driver.evaluate(IndeedSelectors.IS_CHALLENGE_JS));
    }
}
