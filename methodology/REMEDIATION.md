# Remediation — this repository

Ordered by risk. Evidence for each is in [ANALYSIS.md Part 10](ANALYSIS.md#part-10--critical-review).

**Status key:** ✅ applied in this session · ⏳ needs your decision · 📋 scoped, not yet done

| # | Item | Risk | Status |
|---|---|---|---|
| 1 | Real resume PDF in git | **High — privacy** | ⏳ file deleted, history is your call |
| 2 | Dead skeleton stubs + false Javadoc | Medium — misleads every reader | ✅ |
| 3 | `ISSUES.md` tables don't render | Medium — half the tracker is unreadable | ✅ |
| 4 | `ApplyWalkthrough` has no tests | **High — correctness** | 📋 scoped below |
| 5 | Threading rule violated | Low impact, doc integrity | ✅ |
| 6 | No `LICENSE`, tracked IDE noise | Low | ✅ / ⏳ |

---

## 1 — Real resume PDF committed ⏳

**Evidence.** `src/main/resources/Resume.pdf` — a genuine one-page PDF, added in `f2eef92`
(2026-07-31), **referenced by zero lines of code**. The only two `getResourceAsStream` calls in the
codebase load `cities.tsv` and the app icon. Remote: `github.com/HaadLIT/IndeedAutoApplier`.

**Why it matters.** A resume carries full name, email, phone, and usually a home address and
employment history. It is in git *history*, so deleting it from `HEAD` does not remove it. This is
also the exact failure the app itself guards against at runtime — `ProfileFacts`'s Javadoc says
`rawText` *"must NEVER be written to disk"*, and `DECISIONS.md:116` says raw resume text *"stays in
memory and is never persisted."* The care was applied to the runtime and not to the repository.

**Done in this session:** the file is deleted from the working tree, and `.gitignore` now excludes
documents outside a fixtures directory.

**Your decision — the history.** Three options, and the right one depends on whether the repo is
public. I have not run any of these:

| Option | When it's right | Cost |
|---|---|---|
| **Do nothing further** | The repo is private and will stay private | None. The PDF stays in history |
| **Rewrite history** — `git filter-repo --path src/main/resources/Resume.pdf --invert-paths`, then force-push | The repo is public, and you want the existing repo | Rewrites every commit hash. Any clone/fork must be re-cloned. GitHub may retain unreferenced objects until you ask support to purge them |
| **Fresh repository** | The repo is public and history isn't precious | Cleanest guarantee. Loses the commit history — which, given how much this analysis draws from it, is a real loss |

**Regardless of the option:** treat the personal details in that PDF as exposed if the repo has ever
been public. There is no way to know what was cached.

**Prevention, already applied.** `.gitignore` now has:

```gitignore
### Never commit documents containing personal data
*.pdf
*.docx
!src/test/resources/fixtures/*.pdf
```

The test suite already does this correctly — `new ProfileFacts("Pat Doe", "pat@x.com", …)`. Fixtures
should be synthetic from the first one.

---

## 2 — Dead skeleton stubs and a false Javadoc ✅

**Evidence.** `startRun`, `pauseRun`, `stopRun` logged `"(stub)"` and mutated a `RunStatus` nothing
read. `history()` returned a list nothing added to. `RunStatus`, `HistoryEntry`, `StatusPanel`,
`HistoryPanel` were unreachable from `Main`. `AppCore`'s class Javadoc still described a skeleton
from day 0: *"the automation verbs are logged stubs… The real engines arrive in later slices."*

**Why it mattered.** `AppCore` is the file `ARCHITECTURE.md` points at first and the file an AI
session reads first, and it told a new reader the automation was stubbed. `stopApplying()` (real)
sat 200 lines from `stopRun()` (fake) — a live trap.

**Applied.** Removed the five dead members and their two backing fields from `AppCore`, deleted
`RunStatus`, `HistoryEntry`, `StatusPanel`, `HistoryPanel`, removed `AppliedPosting.toHistoryEntry()`
(its only consumer), dropped the two orphaned imports, and rewrote the class Javadoc to describe what
`AppCore` now is.

**Verified.** `.\gradlew.bat clean test` — BUILD SUCCESSFUL, 47 tests, 0 failures.

---

## 3 — `ISSUES.md` tables don't render ✅

**Evidence.** Blank lines before rows 12, 16, 23, 25, and 26 terminated the GFM table, so rows 12–26
rendered as literal pipe-delimited text.

**Applied.** Removed the five blank lines. The tracker is one table again, rows 1–26.

**Watch out if you edit this file from PowerShell.** `Get-Content` in Windows PowerShell 5.1 reads a
BOM-less UTF-8 file as ANSI, so a read-modify-write round trip turns every em dash into mojibake. Use
`[System.IO.File]::ReadAllLines($path, (New-Object System.Text.UTF8Encoding($false)))`. I did exactly
this wrong on the first attempt and reverted it.

---

## 4 — `ApplyWalkthrough` has no tests 📋

**This is the largest remaining gap and the most valuable work in this list.** Scoped rather than
done, because it is a real piece of engineering and deserves its own session.

**Evidence.** 516 lines, decides whether to click Submit on a real employer's form, zero tests.
`ISSUES.md` rows 3, 5, 6, 11, 12, 13, 14, 23 are all walkthrough control-flow bugs found in
production on live applications. The check *ordering* is load-bearing — issue #13 was caused by
checking "challenged?" *after* "module loaded?" — and nothing asserts that ordering, so a future
tidy-up silently reintroduces it.

**The approach.** `BrowserDriver` is a 13-method interface. A fake returning scripted per-call values
makes every one of those bugs reproducible without a browser.

```java
/** Returns scripted values per selector/script, in call order, so a page can "change". */
final class FakeBrowserDriver implements BrowserDriver {
    private final Deque<String> urls = new ArrayDeque<>();
    private final Map<String, Deque<Object>> evaluations = new HashMap<>();
    private final Set<String> present = new HashSet<>();
    final List<String> clicks = new ArrayList<>();   // assertions read this

    FakeBrowserDriver script(String js, Object... values) { … }
    FakeBrowserDriver urlSequence(String... urls) { … }
    // evaluate() pops the next scripted value, repeating the last one when exhausted
}
```

**The tests, one per `ISSUES.md` row:**

| Test | Row | Asserts |
|---|---|---|
| `cloudflareIsReportedAsChallengedNotAsALoadFailure` | 13 | `MODULE_READY_JS` false + `IS_CHALLENGE_JS` true → `CHALLENGED`. **This is the ordering test** |
| `reviewSpinnerIsWaitedOutBeforeReadingThePage` | 23 | `REVIEW_PREPARING_JS` true for N polls, then Submit appears → waits, reads a real page |
| `continueIsNeverClickedOnTheReviewModule` | 23 | On `review-module`, `clicks` never contains Continue |
| `disabledSubmitMeansStandBy` | 11, 12 | `SUBMIT_STATE_JS` `"disabled"` → `"enabled"` → stands by, then submits |
| `invisibleCaptchaDoesNotBlockSubmit` | 11 | `VISIBLE_CAPTCHA_JS` false after settle → proceeds |
| `errorScreenWithTryAgainRedoesTheModule` | 5 | Try-again clicked, module redone |
| `terminalErrorScreenEndsCleanly` | 14 | "still having trouble" variant with no Try again → `FAILED`, not a button error |
| `applyAnywayIsClickedOnTheRequirementsScreen` | 6 | Advisory screen clicked through |
| `continueIsTriedEvenWithUnansweredRequiredFields` | 3 | Continue-anyway policy |
| `submitIsNotClickedInReviewMode` | — | **The safety test.** `SubmitMode.REVIEW` → `clicks` never contains Submit |
| `submissionCountsAsVerifiedOnlyWithAConfirmation` | 15 | Click without confirmation → `SUBMITTED`, not `SUBMITTED_VERIFIED` |

Plus the gap from [ANALYSIS.md §10.10](ANALYSIS.md#1010-probe-established-facts-are-not-committed--oversight):
commit HTML fixtures under `src/test/resources/` and a test running
`SUBMITTED_CONFIRMATION_JS`'s regex over confirmation pages (must match) and over question, review,
and captcha pages (must not). `INDEED.md:123` claims this was verified; the verification exists
nowhere in the repo, and that regex is the sole basis for the app's most important assertion.

**Estimated size.** ~150 lines of fake + ~250 lines of tests. Say the word and I'll do it.

---

## 5 — Threading rule violated ✅

**Evidence.** `ARCHITECTURE.md:97` states *"`AppCore` state read by the UI is `volatile`."* But
`documents` and `criteria` were plain fields, and `documents` is written on the EDT
(`loadDocuments`) and read on the browser worker (`applyOne` → `documents.resume()`). Separately,
`fitScoresVersion++` was a non-atomic read-modify-write on a `volatile int` from two threads (browser
worker at `scheduleFitScoring`, llm worker in each scoring task).

**Impact.** Low in practice — executor submission supplies an incidental happens-before edge for
`documents`, and a lost `fitScoresVersion` increment costs one missed list repaint. The real harm was
that **a documented invariant was false**, which teaches readers the document is approximate.

**Applied.** `volatile` on `documents` and `criteria`; `fitScoresVersion` is now an `AtomicInteger`.
The document is true again.

---

## 6 — Housekeeping ✅ / ⏳

**Applied:**
- `LICENSE` added (MIT), with third-party attributions: GeoNames CC BY 4.0, llama.cpp MIT,
  Qwen3 Apache 2.0.
- `.gitignore` now excludes `.idea/` consistently (it previously excluded `.vscode/` entirely while
  tracking `.idea/copilot.data.migration.*.xml`).

**Your decision ⏳ — untracking the four IDE files.** They are already committed, so `.gitignore`
alone does not remove them. When you want them gone:

```bash
git rm --cached .idea/copilot.data.migration.agent.xml .idea/copilot.data.migration.ask.xml \
                .idea/copilot.data.migration.ask2agent.xml .idea/copilot.data.migration.edit.xml
```

I have not run this — it stages a change, and staging on your behalf without asking isn't mine to do.

---

## Not fixed, deliberately

These are real observations from the analysis that I have **not** touched, because each is a
judgement call about this project's direction rather than a defect:

| Item | Why left alone |
|---|---|
| `AppCore` at 719 lines ([§10.5](ANALYSIS.md#105-appcore-is-a-facade-with-a-run-engine-hiding-inside-it)) | Extracting `ApplyRunner` is the right move and is best done *with* item 4, since the tests are what make the extraction safe |
| `history.tsv` conflating audit log with retry state ([§10.7](ANALYSIS.md#107-historytsv-carries-three-responsibilities--potential)) | Changing it changes user-visible retry behaviour. Needs your call on what *should* happen to a `NEEDS_INPUT` posting |
| String-keyed page navigation ([§10.8](ANALYSIS.md#108-stringly-typed-navigation--potential)) | ~20 lines to fix, zero realised cost so far. Worth doing next time you touch `App` |
| `Page1`–`Page4` names | Renaming touches every page. Not worth a standalone commit |
| No CI | A real improvement, but it is a new capability rather than a remediation — and it belongs in the new project's scaffold first |
| 16 rows stuck at `FIXED?` ([§10.3](ANALYSIS.md#103-the-verification-loops-last-step-does-not-happen--actual)) | Can only be resolved by real runs. The `predicts:` mechanism in [templates/ISSUES.md](templates/ISSUES.md) is the fix for *next* time |
