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

    /** Finds the radio in a group whose label best matches the wanted text and returns a CSS selector
     *  for it (exact match, then prefix, then substring). Generic HTML — no site specifics. */
    private static final String FIND_OPTION_SELECTOR_JS = """
        (arg) => {
          const name = arg[0];
          const want = (arg[1] || '').toLowerCase().replace(/\\s+/g, ' ').trim();
          const norm = s => (s || '').toLowerCase().replace(/\\s+/g, ' ').trim();
          const radios = [...document.querySelectorAll('input[type=radio][name="' + name + '"]')];
          const sel = r => {
            const t = r.getAttribute('data-testid'); if (t) return '[data-testid="' + t + '"]';
            if (r.id) return '[id="' + r.id + '"]';
            return null;
          };
          for (const mode of [0, 1, 2]) {
            for (const r of radios) {
              const label = document.querySelector('label[for="' + CSS.escape(r.id) + '"]') || r.closest('label');
              const text = norm(label ? label.textContent : r.value);
              if ((mode === 0 && text === want) || (mode === 1 && text.startsWith(want))
                  || (mode === 2 && (text.includes(want) || want.includes(text)))) {
                return sel(r);
              }
            }
          }
          return null;
        }""";

    @Override
    public void chooseOption(String groupName, String optionText) {
        Object selector = page.evaluate(FIND_OPTION_SELECTOR_JS, java.util.List.of(groupName, optionText));
        if (selector == null) {
            throw new IllegalStateException("No option matching '" + optionText + "' in group " + groupName);
        }
        page.locator((String) selector).click();
    }

    @Override
    public void uploadFile(String selector, java.nio.file.Path file) {
        page.setInputFiles(selector, file);
    }

    @Override
    public void uploadViaChooser(String trigger, java.nio.file.Path file) {
        com.microsoft.playwright.FileChooser chooser = page.waitForFileChooser(
                new Page.WaitForFileChooserOptions().setTimeout(TIMEOUT_MS),
                () -> page.click(trigger));
        chooser.setFiles(file);
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
