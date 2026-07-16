package com.haadlit_sp.appCoreLogic.browser;


/** Selects the browser-driver implementation. One variant today; add a case to switch drivers. */
public final class BrowserDriverFactory {

    private BrowserDriverFactory() {}

    public static BrowserDriver create() {
        return new PlaywrightBrowserDriver();
    }
}
