# Architecture

What each part of the codebase does, and how a run flows through it. Read this before changing
anything; read [EXTENDING.md](EXTENDING.md) before adding anything.

## The one rule that shapes everything

**The UI never touches the browser, the model, or the disk.** Every screen talks to a single facade,
[`AppCore`](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java), which owns the workers and
the engines. Swap Playwright for Selenium, or the local model for a hosted one, and no page changes.

```
appRenderLogic  (Swing: pages, components, theme)
      │  verbs in, plain state out — never blocks
      ▼
   AppCore      (facade: owns workers, engines, stores)
      │
      ├── browser/   drive Chrome            ├── llm/     local model
      ├── search/    find postings           ├── answer/  decide answers
      ├── apply/     walk one application    ├── pdf/     read the resume
      └── store/     persist + record        └── model/   plain data records
```

## Package map

### `appCoreLogic` — the engine room

| Package | Contains | Notes |
|---|---|---|
| *(root)* | `AppCore` | The facade. Owns three worker threads, all engines, all stores, and the run counters. Everything the UI can do is a method here. |
| `browser` | `BrowserDriver`, `PlaywrightBrowserDriver`, `BrowserDriverFactory`, `ChromeProfile`, `IndeedSelectors` | `BrowserDriver` is the swappable verb set (navigate, click, fill, evaluate, screenshot…). **`IndeedSelectors` is the only place Indeed's markup is written down** — selectors and page-detection JS live there, nowhere else. |
| `session` | `LoginStrategy`, `ManualLoginStrategy`, `LoginStrategyFactory` | Sign-in check. Manual is the only strategy: the human signs in, the app verifies. |
| `search` | `PostingEnumerator`, `EnumerationResult` | Runs a search and scrapes result cards, walking every result page (`&start=`), deduping by job key. Read-only — it never applies. |
| `apply` | `ApplyWalkthrough`, `ApplyFormReader`, `ApplyFiller`, `ApplyResult`, `ApplyJournal` | Walks one posting through Easy Apply. The reader scrapes whatever fields a step presents; the filler puts values in them; the walkthrough orchestrates and decides the outcome. |
| `answer` | `QuestionAnswerer`, `RuleBasedAnswerer`, `QuestionAnswererFactory` | Deciding what to answer. The interface is the seam between rule-based and AI answering. |
| `llm` | `LlmRuntime`, `LlamaServerRuntime`, `LlmAssetInstaller`, `LlmAssets`, `LlmClient`, `ScreenerPrompt`, `LlmResponseValidator`, `LlmQuestionAnswerer`, `JobFitScorer` | Everything about the local model. Nothing outside this package knows the model exists. |
| `pdf` | `PdfTextExtractor`, `ProfileFactsExtractor` | Resume PDF → text → a `ProfileFacts` fact base. |
| `location` | `LocationSuggester`, `BundledCityLocationSuggester` | City autocomplete from a bundled GeoNames list. |
| `store` | `AppPaths`, `ContactDetailsStore`, `QaBankStore`, `ApplicationHistoryStore`, `SettingsStore`, `DiagnosticsLog`, `SessionLog` | Everything persisted. All fail-soft: a store problem must never break a run. `AppPaths` is the single source of truth for file locations. |
| `model` | `JobPosting`, `ScreenerQuestion`, `Answer`, `ProfileFacts`, `ContactDetails`, `SearchCriteria`, `RunSummary`, `SubmitMode`, `AnswerMode`, … | Plain records and enums. No behaviour beyond validation/normalisation. |

### `appRenderLogic` — the Swing UI

| Package | Contains | Notes |
|---|---|---|
| *(root)* | `App`, `StartupModeDialog` | `App` owns the card layout and the **single 500 ms timer** that refreshes whichever page is visible. `StartupModeDialog` asks Standard vs AI-enhanced at launch. |
| `pages` | `Page1`…`Page4`, `PersonalDetailsPage`, `LivePage`, `PageUtil` | One class per step: sign in → documents → details → location → run. A page implementing `LivePage` gets `refresh()` called on every tick. |
| `components` | `ResultsPanel`, `RunSummaryPanel`, `StatusPanel`, `HistoryPanel`, `ProfileFactsPanel`, `SubmitModeSelector`, `AutocompleteField`, `FileField`, `StepRail`, `Header` | Reusable widgets, each with a narrow API (typically one setter). |
| `theme` | `Theme`, `AppInfo` | Colours, type, spacing, and layout helpers (`card`, `stack`, `row`, `scroll`). All styling goes through here. |

## How a run flows

### Search
`Page4` → `AppCore.startSearch()` → browser worker → `PostingEnumerator.enumerate()` → navigates
each result page, evaluates `IndeedSelectors.SCRAPE_POSTINGS_JS`, dedupes → `foundPostings`. In AI
mode this also queues a `JobFitScorer` task per posting on the LLM worker, so scores appear
progressively.

### Applying to one posting
`AppCore.applyToNextPosting()` → browser worker → `ApplyWalkthrough.apply()`:

1. Open the posting; bail if challenged or not Easy Apply.
2. Click Apply, wait for the smartapply flow.
3. Loop, up to `MAX_MODULES` steps:
   - Wait for the module to render. If it never does, work out **why** — Cloudflare challenge?
     Indeed error screen? — before reporting a generic failure.
   - Handle interstitials: "Try again" error screens, "Apply anyway" requirement screens.
   - Read the module's questions (`ApplyFormReader`), resolve an answer for each, fill it
     (`ApplyFiller`), and journal what happened.
   - On the final step: submit (auto modes), waiting for the human if a verification blocks it, then
     **verify** a confirmation actually appeared.
   - Otherwise click Continue and move on. The form's own validation is the authority on whether a
     field was really required.
4. Return an `ApplyResult`; `AppCore` records it, counts it, and screenshots anything that stopped
   short.

### Answering one question
`ApplyWalkthrough.resolveAnswer()` asks, in order:

1. **Contact details** — name/email/phone/address fields map straight from what the user entered.
2. **`QuestionAnswerer`** — in Standard mode `RuleBasedAnswerer`; in AI mode `LlmQuestionAnswerer`,
   which itself tries the Q&A bank and the rules *before* the model.
3. Nothing → the question is left for the human.

A validated model answer is written back to `QaBankStore`, so the same question is instant and free
next time — including in future runs.

## Threading

| Thread | Owns | Rule |
|---|---|---|
| Swing EDT | All UI | Never blocks. Reads plain fields off `AppCore` on a 500 ms timer. |
| `browser-worker` | The `BrowserDriver` | Single-threaded: the driver is thread-affine. All navigation, scraping, and applying happens here. |
| `doc-worker` | PDF parsing | Keeps resume parsing off the EDT. |
| `llm-worker` | Model setup + fit scoring | First-run download and server start; then one fit score at a time. Yields while an application is running so screener answers get the model. |

`AppCore` state read by the UI is `volatile`; collections shared across threads are concurrent.

## Local AI

`LlamaServerRuntime` owns a llama.cpp `llama-server` process on localhost: it installs the assets
(resumable download, sha256-verified), starts the server, adopts one that is already running, and
shuts it down on exit. `LlmClient` talks to it over HTTP with a **JSON schema**, so a dropdown answer
is physically constrained to the real options. `LlmResponseValidator` then re-checks everything
before it can reach a form. Every failure path falls back to rules — AI unavailable means the app
behaves exactly like Standard mode.

Model and engine versions are pinned in one place: `LlmAssets`.

## Recording what happened

- `DiagnosticsLog` — a screenshot plus a line in `diagnostics/issues.tsv` for every application that
  stopped short.
- `SessionLog` — one markdown file per run in `diagnostics/sessions/`: setup, summary counters,
  timeline, a **step-by-step journal** (every module, question, answer, source, fill result, click,
  wait), and derived notes grouping recurring failures.
- `ApplicationHistoryStore` — the permanent record of postings applied to, and the dedup source.
- [`ISSUES.md`](../ISSUES.md) — the human-maintained tracker: symptom, diagnosis, fix, and whether a
  later run confirmed it.
