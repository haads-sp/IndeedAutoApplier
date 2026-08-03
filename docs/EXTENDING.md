# Extending the app

Recipes for the changes this project actually needs, and the conventions that keep it maintainable.
See [ARCHITECTURE.md](ARCHITECTURE.md) for what each component is.

## Conventions

**Put site knowledge in one place.** Every Indeed selector, URL shape, and page-detection script
lives in `browser/IndeedSelectors`. If you find yourself writing a CSS selector or a `document.query…`
anywhere else, it belongs there instead.

**Verify against reality, don't assume.** This codebase has been wrong about the live site many
times — a "load failure" that was a Cloudflare page, a captcha that was invisible, an HTML gap that
only collapses under FlatLaf. Before claiming something works, run it: a scratch probe against the
real page, a rendering probe against the real theme, or a test. Screenshots beat reasoning.

**Fail soft, and say why.** A store that can't write, a model that won't start, a screenshot that
fails — none of these may break a run. Degrade to the simpler behaviour and report it in plain
language the user can act on.

**Answer rather than pause, but never fabricate.** The app is meant to run unattended. When the
resume is silent about a qualification, the honest answer is `0` / "No" / "None" — give it
confidently. Never assert a credential, licence, or experience the resume doesn't support.

**Comments explain why, not what.** Match the surrounding density: a line where a non-obvious
constraint bit us, not narration of the code.

## Recipes

### Add a rule for a screener question

`answer/RuleBasedAnswerer` — add a matcher and a branch in `answer()`, plus tests in
`RuleBasedAnswererTest`. Keep it conservative: answer only when a fact clearly covers the question,
and prefer returning empty over guessing (the AI or the user takes it from there).

Rules run before the model in both modes, so a rule is the right home for anything deterministic —
it's instant and free.

### Change how the model answers

- **Prompt / safety rules** → `llm/ScreenerPrompt`. Small models follow a numbered decision
  procedure plus concrete examples far better than abstract instructions; the current prompt is
  shaped that way for a reason. Re-probe after editing — a plausible-looking prompt change can flip
  a confident `0` into "unsure".
- **Output shape** → `schemaFor()` in the same class, and `LlmResponseValidator` for the checks that
  run before a value can reach a form. Both must agree.
- **A new model capability** (like fit scoring) → a new class in `llm/`, reusing
  `ScreenerPrompt.profileBlock()` so the prompt prefix stays byte-identical and the server's prefix
  cache keeps it fast.

### Add a browser capability

Add the verb to the `BrowserDriver` interface, implement it in `PlaywrightBrowserDriver`, and keep
it generic — verbs describe browsing ("click the first visible match"), not Indeed.

### Handle a new page or interstitial

Add its detection JS and selector to `IndeedSelectors`, then handle it in `ApplyWalkthrough`'s module
loop. Ask what the screen *is* before deciding it's a failure: the loop checks for challenges and
error screens before reporting a generic problem, and that ordering matters.

Journal it (`journal.step("interstitial", …)`) so the session file shows it happened.

### Add a UI page or widget

Pages live in `appRenderLogic/pages`, implement `LivePage` if they show live state, and are
registered in `App`. Widgets go in `components` with a narrow API. All styling comes from `Theme` —
no raw colours or fonts.

Swing traps that have bitten this project: `Theme.row` freezes its height at build time (seed a
label that starts empty with `" "`); `BorderLayout.NORTH` starves `CENTER` (give lists a
`setVisibleRowCount` and wrap pages in `Theme.scroll`); FlatLaf collapses whitespace between two
styled HTML runs (use an em space or a visible separator).

### Regenerate the city list

`src/main/resources/cities.tsv` comes from GeoNames `cities15000.zip` + `admin1CodesASCII.txt`,
filtered to population ≥15k for US/CA and ≥50k elsewhere, and written in population order (file
order *is* the relevance ranking). Keep the CC BY 4.0 attribution shown in the UI.

### Add persisted state

Add the path to `store/AppPaths`, then a store class beside the others. Follow the existing shape:
plain TSV, append-only where possible, load on construction, fail-soft on every I/O path, and never
persist anything sensitive.

## Testing and probing

`.\gradlew.bat test` runs the suite. Unit tests cover the logic that can be tested without a
browser: answer rules, the LLM response validator, prompt construction, the session log.

Anything involving the live site or a rendered UI is verified with a **scratch probe** — a small
`main` compiled against `build/install/IndeedAutoApplier/lib/*` and run directly. Probes have caught
things tests structurally cannot: an auto-unboxing crash on the first health poll, a captcha
false-positive, a session file overwriting itself. Two rules learned the hard way:

- Call `Theme.install()` in any rendering probe, or you're testing a look-and-feel the app never uses.
- Capture with `component.printAll(g)`, not a screen grab — a screen grab catches whatever window
  happens to be in front.

Be sparing with probes that hit Indeed. Automated volume escalates Cloudflare challenges for the
real account; space them out and prefer local `data:` URLs when testing page-detection JS.

## Keeping the docs honest

When a change lands that affects behaviour:

1. Update the relevant `.md` — [README.md](../README.md) for anything user-visible,
   [ARCHITECTURE.md](ARCHITECTURE.md) for structure, this file for new conventions.
2. Add or update the [ISSUES.md](../ISSUES.md) row: symptom, diagnosis, fix, status. Statuses are
   **OPEN** → **FIXED?** (shipped, unproven) → **VERIFIED** (a later real run showed it gone).
   Only real evidence promotes a row to VERIFIED.
3. After a user's run, read `~/.indeedapplier/diagnostics/sessions/` and triage new failures into
   `ISSUES.md`. That loop — run, capture, diagnose, fix, confirm — is how this app improves.
