package com.haadlit_sp.appCoreLogic.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;


/**
 * Playwright-backed {@link BrowserDriver} that ATTACHES over CDP to the real Chrome the user
 * launched (with a debugging port) and signed into by hand. It never launches an automation-flagged
 * browser — that is what keeps Cloudflare treating the session as the real browser it is
 * ({@code navigator.webdriver === false}); a Playwright-launched Chrome is walled and cannot be
 * cleared even by a human click.
 *
 * <p>Playwright objects are thread-affine, so every method here must be called from the single
 * browser worker thread that owns this instance.
 */
public class PlaywrightBrowserDriver implements BrowserDriver {

    private static final Logger LOG = System.getLogger(PlaywrightBrowserDriver.class.getName());
    private static final double TIMEOUT_MS = 15_000;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    @Override
    public void launch() {
        if (page != null) {
            return;
        }
        // We only ever attach to the user's installed Chrome, so skip Playwright's ~500 MB of
        // bundled browser downloads.
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        browser = playwright.chromium().connectOverCDP(ChromeProfile.cdpEndpoint());
        context = browser.contexts().isEmpty() ? browser.newContext() : browser.contexts().get(0);
        page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
        page.setDefaultTimeout(TIMEOUT_MS);
        LOG.log(Level.INFO, "Attached to Chrome over CDP at {0}", ChromeProfile.cdpEndpoint());
    }

    @Override
    public void navigate(String url) {
        page.navigate(url);
    }

    @Override
    public boolean exists(String selector) {
        if (page == null) {
            return false;
        }
        try {
            return page.locator(selector).count() > 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public String currentUrl() {
        return page == null ? "" : page.url();
    }

    @Override
    public Object evaluate(String script) {
        return page.evaluate(script);
    }

    @Override
    public void fill(String selector, String text) {
        page.fill(selector, text);
    }

    @Override
    public void click(String selector) {
        page.click(selector);
    }

    @Override
    public void clickFirstVisible(String selector) {
        for (com.microsoft.playwright.Locator candidate : page.locator(selector).all()) {
            if (candidate.isVisible()) {
                candidate.click(); // native = trusted pointer event; a synthetic click won't advance the form
                return;
            }
        }
        throw new IllegalStateException("No visible element for selector: " + selector);
    }

    @Override
    public void selectOption(String selector, String value) {
        page.selectOption(selector, value);
    }

    @Override
    public void close() {
        try {
            if (playwright != null) {
                playwright.close(); // disconnects; does NOT kill the user's Chrome, which we did not launch
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Error disconnecting from browser", e);
        } finally {
            page = null;
            context = null;
            browser = null;
            playwright = null;
        }
    }
}
