package com.haadlit_sp.appCoreLogic.browser;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;


/**
 * Playwright-backed {@link BrowserDriver}. Drives the user's real installed Chrome
 * (channel "chrome") on the persistent {@link ChromeProfile}, so the session the user
 * signed into by hand is reused instead of a pristine throwaway profile.
 *
 * <p>Playwright objects are thread-affine, so every method here must be called from the
 * single browser worker thread that owns this instance.
 */
public class PlaywrightBrowserDriver implements BrowserDriver {

    private static final Logger LOG = System.getLogger(PlaywrightBrowserDriver.class.getName());
    private static final double TIMEOUT_MS = 15_000;

    private Playwright playwright;
    private BrowserContext context;
    private Page page;

    @Override
    public void launch() {
        if (page != null) {
            return;
        }
        Path profile = ChromeProfile.dir();
        try {
            Files.createDirectories(profile);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create Chrome profile directory: " + profile, e);
        }
        // We only ever drive the user's installed Chrome, so skip Playwright's ~500 MB of
        // bundled browser downloads on first run.
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        context = playwright.chromium().launchPersistentContext(profile,
                new BrowserType.LaunchPersistentContextOptions()
                        .setHeadless(false)
                        .setChannel("chrome"));
        page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
        page.setDefaultTimeout(TIMEOUT_MS);
        LOG.log(Level.INFO, "Chrome launched on persistent profile {0}", profile);
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
    public void close() {
        try {
            if (context != null) {
                context.close();
            }
            if (playwright != null) {
                playwright.close();
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Error closing browser", e);
        } finally {
            page = null;
            context = null;
            playwright = null;
        }
    }
}
