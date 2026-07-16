package com.haadlit_sp.appCoreLogic.session;

import com.haadlit_sp.appCoreLogic.browser.BrowserDriver;
import com.haadlit_sp.appCoreLogic.browser.IndeedSelectors;

import java.util.function.Consumer;


/**
 * Opens the persistent profile and confirms it is signed in. The sign-in itself happens
 * beforehand, by the user, in a plain (un-automated) Chrome window on the same profile —
 * that is what keeps Google and Cloudflare treating it as a real browser.
 */
public class ManualLoginStrategy implements LoginStrategy {

    private static final long TIMEOUT_MS = 3 * 60 * 1000;
    private static final long POLL_MS = 2000;

    @Override
    public boolean login(BrowserDriver driver, Consumer<String> status) throws InterruptedException {
        driver.launch();
        status.accept("Checking the saved session…");
        driver.navigate(IndeedSelectors.HOME_URL);

        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        String last = "";
        while (System.currentTimeMillis() < deadline) {
            String message;
            if (isChallenged(driver)) {
                message = "Cloudflare check — please clear it in the Chrome window.";
            } else if (isSignedIn(driver)) {
                status.accept("Signed in.");
                return true;
            } else {
                message = "Not signed in — sign in inside the Chrome window.";
            }
            if (!message.equals(last)) {
                status.accept(message);
                last = message;
            }
            Thread.sleep(POLL_MS);
        }
        status.accept("Timed out — still not signed in.");
        return false;
    }
}
