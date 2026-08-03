# What we know about the live site

Hard-won facts about Indeed's real behaviour, all of it verified against the live site rather than
assumed. Selectors themselves live in `browser/IndeedSelectors`; this is the context that explains
them and the traps that are invisible from the markup.

Treat this as perishable — Indeed changes. When something here turns out to be stale, re-verify and
update it rather than working around it.

## Domains and sign-in

Indeed redirects to a country domain; this account resolves to **`ca.indeed.com`** (hence kilometre
radii). `SEARCH_HOST` is the single place that is written down; other regions would make it a
setting.

Signed-in detection uses the account menu `[data-gnav-element-name='AccountMenu']`, which renders
**only when signed in**. It is deliberately a single *positive* check so it fails closed. Never
reintroduce "signed in == the sign-in link is absent" — that reports success whenever the selector
merely breaks.

Email+password login no longer exists on Indeed; manual sign-in is the only strategy.

## Search results

- Cards are `.job_seen_beacon`; the job key comes from `a[data-jk]`.
- **The title lives in that anchor's text.** There is no `h2` on the real render — an `h2`-based
  scrape returns blank. (This differs from what Playwright's own rendering suggests.)
- **Hydration race:** the card skeleton (job key, company) renders *before* the title text. Wait for
  `RESULTS_READY_JS` or titles scrape empty.
- The card's own href is a `/pagead/clk` **ad redirect** — never navigate it. Build the posting URL
  from the job key instead.
- Pagination is `&start=N` in steps of 10, but a page renders ~16 cards, so pages overlap. Past the
  last page Indeed **repeats results instead of returning nothing**, so "this page added nothing new"
  is the reliable stop signal.

## The Easy-Apply flow

Clicking Apply is a **same-tab navigation** (not an iframe or popup) to
`smartapply.indeed.com/beta/indeedapply/form/<module>`. The flow is modular: each step is its own URL
slug, and the current step is identified from that slug.

Observed modules, in order:

1. `contact-info-module` — `names-first-name`, `names-last-name`, `phone`, `age`
2. `profile-location` — `location-postal-code`, `location-locality`, `location-address`
3. `resume-selection-module` — pick a saved resume or upload one
4. `resume-module` — a two-field resume *preview*, a **different** module despite the similar name
5. `questions-module` — the employer's screener questions
6. `review-module` — the final review and Submit

Contact and location modules are often **skipped entirely** once Indeed knows those details, so the
walkthrough must handle a flow starting at any module — it does, via a generic loop.

Re-clicking Apply on a posting already started **resumes the existing draft** mid-flow, which can
look like "the form did not open". Test on fresh postings.

## DOM traps

- **Field `id`s are dynamic React** (`ifl-InputFormField-:r7:`) and change between renders. The
  `name` attribute is stable — key everything on `name`.
- **`el.required` lies** on this form. Real signals are `aria-required` or a trailing `*` in the
  label. Even then the flag is unreliable, which is why the walkthrough clicks Continue anyway and
  lets the form's own validation decide what was truly required.
- **A synthetic `element.click()` does not advance the form** — no error, just nothing. It needs a
  native click. The form also renders several *hidden* duplicate Continue buttons, so the click must
  target the first visible one.
- **Fill → Continue is a race.** React needs a moment to register a filled value; clicking Continue
  immediately silently fails to advance.
- **`page.url()` lags the SPA route change.** Advancing is therefore also detected by the fields we
  just filled disappearing, not by URL alone.
- Radio options are visible inputs grouped by `name` (which is the question id), with the label
  either wrapping the input or carrying the value. Dropdowns are ordinary `<select>`s — but our
  answers are the *visible labels*, so selection must match by label, with the value attribute as a
  fallback.
- The resume step has **two different DOMs**: the upload card flow (which uploads asynchronously —
  wait for the filename to appear before continuing, and a tiny placeholder PDF gets rejected) and
  the saved-resume card, which is the common case once a resume exists on the account.

## Interstitials

Screens that interrupt the flow and must be recognised for what they are:

| Screen | Behaviour | What the app does |
|---|---|---|
| Cloudflare "Additional Verification Required" / "Just a moment…" | Full-page interstitial identified by title and body text — the turnstile widget selector alone misses it | Detect, disconnect, hand to the user. **Check for this before blaming a load failure**: a challenge page never becomes "module ready", so it used to be misreported as a generic error |
| "Our systems are having some trouble" | Sporadic; sometimes offers **Try again**, sometimes only "Save job and exit". The wording varies — it can read "our systems are **still** having some trouble" | Click Try again and redo the module; treat the no-retry variant as a clean end |
| "It looks like you don't meet these employer requirements" | Advisory, with an **Apply anyway** button | Click through and continue |
| Invisible reCAPTCHA | Present on the review module. Its widget is layout-"visible" but renders collapsed | Ignore it — only a widget with real rendered size (>40×40) counts as something a human must solve |
| Visible verification on the final step | A genuine human check | Stand by with a countdown while the user clears it, then submit; never solve it |

## Confirmation of submission

A Submit click is not proof. Confirmations seen live include *"Your application was submitted to
&lt;employer&gt;"* alongside *"You will get an email confirmation at …"*, plus variants like
"Application submitted" and "Thanks for applying"; the post-apply URL is another signal.

`SUBMITTED_CONFIRMATION_JS` covers these and is verified against fake local pages for both hits and
non-hits (question, review, and captcha pages must **not** match).

## City data

Location autocomplete reads `src/main/resources/cities.tsv` — 8,487 cities across 33 countries from
GeoNames (CC BY 4.0, attributed in the UI). The file is **population-ordered**, so file order is the
relevance ranking, and names are de-accented at load so "montreal" matches "Montréal".

To regenerate: GeoNames `cities15000.zip` plus `admin1CodesASCII.txt`, with a floor of population
≥15k for US/CA and ≥50k elsewhere.
