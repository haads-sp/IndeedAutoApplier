# IndeedAutoApplier — reverse-engineering the engineering system

What was actually done, how it was actually done, what it cost, and which parts are worth keeping.

Every claim here cites a file, a line range, or a commit hash. Where the repository does not settle
a question, this document says **"not determinable"** rather than inventing a reason.

**Companion documents:** [PLAYBOOK.md](PLAYBOOK.md) is the process distilled into something you can
follow. [templates/](templates/) are the day-one files for the new project.
[REMEDIATION.md](REMEDIATION.md) is this repo's own fix list.

---

## Contents

| Part | Question it answers |
|---|---|
| [1. What was actually built](#part-1--what-was-actually-built) | The real timeline, from git |
| [2. Decomposition](#part-2--decomposition-how-a-big-idea-became-tasks) | How the idea became tasks |
| [3. Architecture](#part-3--architecture-pattern-by-pattern) | The patterns, and when to reuse each |
| [4. Code organisation](#part-4--code-and-file-organisation) | Where things live and why |
| [5. Workflow](#part-5--the-development-workflow) | How a change got made |
| [6. Documentation](#part-6--documentation-and-knowledge-management) | The four-document system |
| [7. Decisions](#part-7--the-decision-making-system) | How choices were made |
| [8. Testing & debugging](#part-8--testing-validation-and-debugging) | How correctness was established |
| [9. Extensibility](#part-9--extensibility-and-maintainability) | What made change cheap or expensive |
| [10. Critical review](#part-10--critical-review) | What is wrong, ranked |
| [11. What transfers](#part-11--what-transfers-and-what-inverts) | General vs. project-specific |
| [12. Lessons](#part-12--lessons-from-this-project) | Carry forward, avoid, improve |

---

## Part 1 — What was actually built

**Shape of the effort:** 26 days (2026-07-10 → 2026-08-05), 38 commits, ~10,000 lines of Java 21,
85 source files, 5 test classes, 4 runtime dependencies. Solo developer working with an AI coding
agent — evidenced by `CLAUDE.md`, a 74-entry `.claude/settings.local.json` permission allowlist, and
a commit cadence (10 commits between 15:34 and 02:00 on 2026-08-01) that is characteristic of
agent-assisted sessions rather than hand-typing.

### 1.1 The four phases

The commit log divides cleanly. This division was not planned; it emerged, and recognising it is
most of the lesson.

#### Phase 0 — Inherited scaffold (day 0)

`bb51cbe "Initial commit"` is not a fresh start. Its `README.md` is a set of clone-and-rename
instructions for a personal template repo:

```
2. git clone https://github.com/HaadLIT/JavaAppBoilerplate.git LeetCode2
...
Remove-Item -Recurse -Force .git   (Remove current origin + delete git history)
```

And `AppCore.java` at that commit is, in its entirety:

```java
package com.haadlit_sp.appCoreLogic;

public class AppCore {

}
```

An empty class — **already at its final name, in its final package**. The
`appCoreLogic` / `appRenderLogic` split, the Gradle setup, the wrapper, the `com.haadlit_sp` group,
the `Main` → `App` → pages structure: all predate this project.

> **This is the most under-appreciated finding in the repository.** The architecture that the docs
> present as a set of considered decisions was, at its foundation, a *reused personal scaffold*. The
> value came from having made those decisions **once, before**, and never re-litigating them.

#### Phase 1 — UI-first vertical slices (days 0–22, `01dbef2` → `2b1411f`)

`01dbef2 "inital ui skeleton"` (19 files, 826 insertions) built the entire user-facing surface —
`Page1`–`Page4`, `Theme`, `StepRail`, `FileField`, and the model records — against a **stubbed
facade**. The stub's Javadoc, still present at
[AppCore.java:55-62](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L55-L62), is the
project plan for the next 22 days:

> *"For this skeleton slice the automation verbs are logged stubs that mutate in-memory state so the
> UI is demoable. The real engines (browser, PDF, answerers, stores) arrive in later slices behind
> their own interfaces + factories, without changing this API."*

That prediction held for 36 subsequent commits. The `AppCore` API grew, but the *shape* — UI calls
verbs, reads plain state — never changed, and the pages never learned about Playwright, PDFBox, or
llama.cpp.

Each subsequent commit is one capability, threaded through every layer:

| Commit | Capability | Layers touched |
|---|---|---|
| `c321e7d` | Browser login | browser + session + UI |
| `6ae9557` | PDF fact extraction | pdf + model + UI |
| `9d2a081` | Find and list postings | search + browser + model + UI |
| `08080ba` | "Intelligence layer for forms" | answer + model + store |
| `ed56098`…`2b1411f` | Apply walkthrough, module by module | apply + browser + model |
| `c3d98c8`…`2f0ecff` | Local LLM | llm (9 new classes) |

Note the granularity within the walkthrough: `05823ca` click-through drivers → `25f7ca5` walkthrough
drivers → `066ff18` PDF upload → `f2eef92` PDF×Indeed logic → `3a2c140` radio buttons → `2b1411f`
choice filling. **One DOM interaction pattern per commit.** Each was independently observable in a
real browser.

#### Phase 2 — The pivot to reality (2026-08-01)

The app met the live site. The commit character changes completely and permanently:

```
37891d2  cloudflare via indeed verifycation fixies
b26a855  fixes to jobs found results
37b2a1e  fixes to full auto, better answering logic, additonal indeed logic gates…
c8c3b10  updates to captcha/cloudflare verfication functions
```

Every one of `ISSUES.md`'s 26 rows carries a "first seen" date of 2026-08-01 or later. Nothing in
the first 22 days produced a tracked issue, because nothing had been *run against reality*.

> The 22 days of construction produced zero validated knowledge about the actual problem. Four days
> of running produced all of it.

#### Phase 3 — Instrumentation, then documentation (2026-08-02 → 08-05)

Two things happened, in this order, and the order matters:

1. **Instrumentation first.** `b4c3413` added `ISSUES.md` and `DiagnosticsLog` (screenshot +
   `issues.tsv` row per failure). `5d985e6` and `7dc2275` added `SessionLog` — a per-run markdown
   report with a step-by-step journal.
2. **Documentation second.** `9782565` (2026-08-03 01:19) added `CLAUDE.md`, `ARCHITECTURE.md`,
   `EXTENDING.md`. `05e99a4` (01:52) added `DECISIONS.md`, `INDEED.md`.

**All project documentation was written on day 24 of 26.** Before that, the only `.md` in the repo
was the boilerplate's clone instructions.

### 1.2 Why the documentation is good, and why its timing was a mistake

The docs are unusually strong. `DECISIONS.md` opens with *"most of these were settled by
measurement, not preference"* and delivers on it — the Cloudflare decision cites an observed
`navigator.webdriver` value and a checkbox that "loops forever even when a real human clicks it";
the prompt-engineering decision cites "6/6 correct" after restructuring; the prefix-cache decision
cites "~1.7 s versus ~5 s."

None of that could have been written on day 1. It is a **distillation of measurement**, and
measurement requires a running system.

But the cost was real and is visible in the code:

- [AppCore.java:55-62](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L55-L62) still
  describes a skeleton that stopped existing 20 days earlier, because nothing ever prompted a review
  of it.
- `startRun`/`pauseRun`/`stopRun` stubs from day 0 survive to today
  ([AppCore.java:766-791](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L766-L791)),
  along with `RunStatus`, `HistoryEntry`, `StatusPanel`, and `HistoryPanel` — all unreachable.
- `ARCHITECTURE.md:97` states a threading rule (*"AppCore state read by the UI is `volatile`"*) that
  the code violates in two places, because the rule was written **after** the code and never checked
  against it.

**The resolution is not "document on day 1 instead."** It is the split described in
[PLAYBOOK.md](PLAYBOOK.md): `ARCHITECTURE.md` and `DECISIONS.md` exist from commit 1 as *thin*
files that grow by one entry per decision, while `DOMAIN.md`-style knowledge can only be written
after contact with reality — which is an argument for making contact **much earlier than day 22.**

---

## Part 2 — Decomposition: how a big idea became tasks

The repository shows a consistent decomposition, even though no planning document exists. It was
reconstructed from package boundaries, the commit sequence, and the model layer.

### 2.1 The actual hierarchy

```
IDEA          "Apply to Indeed jobs automatically"
   │
   ▼
CONSTRAINTS   Never lie to an employer · never solve a captcha · run unattended
   │          (these are stated as constraints, not features — see §2.4)
   ▼
USER JOURNEY  Sign in → Documents → Details → Location → Run
   │          Became the UI card sequence AND the package boundaries AND the commit order
   ▼
SYSTEMS       browser · search · apply · answer · llm · pdf · location · store · model
   │          Each = one verb the journey needs
   ▼
COMPONENTS    apply/ = ApplyWalkthrough + ApplyFormReader + ApplyFiller + ApplyResult + ApplyJournal
   │          Split by VERB (orchestrate / read / write / report / record), not by noun
   ▼
CLASSES       One responsibility, stated in the first line of its Javadoc
   │
   ▼
TASKS         One commit = one capability, working end-to-end
```

### 2.2 The organising insight: the user journey *is* the architecture

This is the highest-leverage structural decision in the project, and it was almost certainly not
consciously made — it fell out of building UI first.

The five pages (`Page1` sign in, `Page2` documents, `PersonalDetailsPage`, `Page3` location, `Page4`
run) map one-to-one onto:

- the packages that serve them (`session`, `pdf`, `store`, `location`, `search`+`apply`),
- the order capabilities were implemented (`c321e7d` login → `6ae9557` pdf → `9d2a081` search →
  `ed56098` apply),
- and the `completedSteps()` gate in [App.java:117-132](../src/main/java/com/haadlit_sp/appRenderLogic/App.java#L117-L132),
  which defines "done" for each step as *"the thing it asks for actually exists."*

Consequence: **there is never a question about where new code goes.** A change to how the user picks
a location is a `location` + `Page3` change. There is no `utils/`, no `helpers/`, no `common/` —
the two candidate homes for code that has no obvious owner. Their absence is not an accident of a
small project; it is what a journey-derived decomposition produces.

### 2.3 The splitting rule, inferred

Comparing what was split against what was not yields a consistent rule:

**Split when the pieces have different reasons to change.** Not when a file gets long.

| Split | Different reasons to change |
|---|---|
| `ApplyFormReader` / `ApplyFiller` / `ApplyWalkthrough` | Reader changes when Indeed's DOM changes; filler when a new *input type* appears; walkthrough when the *flow* changes. Three independent clocks. |
| `IndeedSelectors` separate from everything | Changes when Indeed ships new markup — on Indeed's schedule, not the developer's. |
| `LlmAssets` separate from `LlamaServerRuntime` | Changes when a model or engine version is bumped, which is a decision, not a code change. |
| `QuestionAnswerer` interface + two impls | Rule-based changes on new question patterns; LLM-based on prompt/model changes. |

**Not split, correctly:** `RuleBasedAnswerer` is 252 lines of pattern-matchers in one class, because
they all change for the same reason — a new screener question wording appears. Splitting it into
`YearsAnswerer` + `EducationAnswerer` + `MoneyAnswerer` would be nine files where one belongs, and
the "add a rule" recipe in `EXTENDING.md:30-37` would become "add a rule, a class, a registration,
and a test file."

**Not split, incorrectly:** `AppCore` is 719 lines and contains at least three reasons to change
(the UI's API surface; the auto-run algorithm; how outcomes are recorded and tallied). See
[§10.5](#105-appcore-is-a-facade-with-a-run-engine-hiding-inside-it).

### 2.4 Constraints declared before features

`DECISIONS.md`, `EXTENDING.md`, `CLAUDE.md`, and `README.md` all state the same three constraints
in near-identical language. `README.md:52-56` has a section titled **"What it will not do."**

These constraints then appear *in the code as structure*, not as comments:

- "Never fabricate" → `RuleBasedAnswerer` returns `Optional.empty()` rather than guessing
  ([RuleBasedAnswerer.java:82-83](../src/main/java/com/haadlit_sp/appCoreLogic/answer/RuleBasedAnswerer.java#L82-L83));
  the LLM's schema physically cannot emit an off-list option; `LlmResponseValidator` re-checks anyway.
- "Never solve a captcha" → `VISIBLE_CAPTCHA_JS` *detects only*, and the walkthrough hands off.
- "Submitting is irreversible" → `SubmitMode.REVIEW` is the default
  ([AppCore.java:153](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L153), with the
  comment `// safe default: never auto-submit`), and a submission only counts as verified when a
  confirmation is actually detected.

> **The transferable move:** write the *non-negotiables* down before the features, phrase them as
> prohibitions, and then make the architecture enforce them structurally — so violating one requires
> deleting code, not just forgetting a rule.

### 2.5 When decomposition would have been over-engineering

The project correctly *did not* build several things it could plausibly have justified:

- No `Repository` interface over the TSV stores (there is one implementation and no plan for two).
- No event bus (the UI polls; there are four threads and one direction of data flow).
- No dependency-injection container (four singletons constructed in one place).
- No config file (`LlmAssets`, `AppPaths`, and `Theme` are compile-time constants; the two genuine
  user choices — answer mode, submit mode — are enums with a store).

Each of these would be *correct* in the web app you are about to build. Their absence here is not
laziness; it is calibration. See [Part 11](#part-11--what-transfers-and-what-inverts).

---

## Part 3 — Architecture, pattern by pattern

Each pattern below: **what was done → why it worked → when to use it → how to reproduce it.**

### 3.1 One boundary rule, stated first

**What.** `ARCHITECTURE.md` opens with a section called *"The one rule that shapes everything"*:
**the UI never touches the browser, the model, or the disk.** Every screen talks to `AppCore`.

**Why it worked.** A single rule that a reader can hold in their head does more than a diagram. It
is falsifiable — you can grep for it. `appRenderLogic` imports from `appCoreLogic.model` and
`appCoreLogic.AppCore` and nothing else, and that invariant survived 38 commits.

It also *paid for itself directly*: AI answering (`c3d98c8`–`2f0ecff`, 9 new classes, ~800 lines)
was added with **zero changes to `ApplyWalkthrough`** and zero changes to any page, because the
seam was already there.

**When to use it.** Always. Every project should be able to state its boundary rule in one sentence.
If you cannot, you do not have one.

**How to reproduce.** Write the sentence in `ARCHITECTURE.md` **before** the second package exists.
Then add a check that fails the build when it is violated — an ArchUnit rule, an ESLint
`no-restricted-imports`, a `depcruise` config. This project relies on discipline; a check is
cheaper.

### 3.2 One file per volatile external fact

**What.** Anything that changes on someone else's schedule gets exactly one file:

| File | Owns | Changes when |
|---|---|---|
| [`IndeedSelectors`](../src/main/java/com/haadlit_sp/appCoreLogic/browser/IndeedSelectors.java) | Every selector, URL shape, and page-detection script | Indeed ships new markup |
| [`LlmAssets`](../src/main/java/com/haadlit_sp/appCoreLogic/llm/LlmAssets.java) | Engine + model URLs, sha256s, sizes | A version is bumped |
| [`AppPaths`](../src/main/java/com/haadlit_sp/appCoreLogic/store/AppPaths.java) | Every on-disk location | The storage layout changes |
| [`Theme`](../src/main/java/com/haadlit_sp/appRenderLogic/theme/Theme.java) | Colours, type, spacing, layout helpers | The design changes |

`IndeedSelectors`' Javadoc states the rule as a promise: *"when automation breaks, fixing a selector
must be a one-file edit here."* `EXTENDING.md:8-11` states the enforcement: *"If you find yourself
writing a CSS selector or a `document.query…` anywhere else, it belongs there instead."*

**Why it worked.** The scraping JS is *also* in this file — not just selector strings, but 45-line
`SCRAPE_MODULE_FIELDS_JS` and `VISIBLE_CAPTCHA_JS` programs. That is the part most projects get
wrong: they centralise the selectors and scatter the DOM logic. Here, when issue #11 turned out to
be an invisible reCAPTCHA that `offsetParent` reported as visible, the fix was **eight lines in one
file**.

**When to use it.** Whenever some knowledge is (a) external, (b) volatile, and (c) discovered rather
than designed. Third-party API response shapes. Payment provider webhook formats. Feature-flag keys.
Regulatory thresholds.

**How to reproduce.** For each external dependency, create `integrations/<name>/contract.<ext>`.
Put in it: endpoints, request/response shapes, quirks, and the *date each was last verified*. Ban
the raw strings everywhere else. Pair it with a `DOMAIN.md` section explaining the traps that are
invisible from the contract itself — see [§6.3](#63-the-fourth-document-external-reality).

### 3.3 Interface + factory at every swap seam

**What.** Five seams, each an interface with a factory:
`BrowserDriver`/`BrowserDriverFactory`, `QuestionAnswerer`/`QuestionAnswererFactory`,
`LlmRuntime`/`LlmRuntimeFactory`, `LocationSuggester`/`LocationSuggesterFactory`,
`LoginStrategy`/`LoginStrategyFactory`.

Four of the five have exactly one implementation.

**Why it worked (and where it did not).** The one that carried its weight is `QuestionAnswerer`. Its
factory is 8 meaningful lines
([QuestionAnswererFactory.java](../src/main/java/com/haadlit_sp/appCoreLogic/answer/QuestionAnswererFactory.java)),
and it is what let AI answering slot in behind the existing rules without the walkthrough noticing.
`BrowserDriver` also earns it: the interface's Javadoc explicitly documents the *thread-affinity
contract* and notes which verbs both Playwright and Selenium support, so the abstraction is
constraining the implementation, not just wrapping it.

`LlmRuntimeFactory`, `LocationSuggesterFactory`, and `LoginStrategyFactory` are three lines each
returning `new X()`. They are speculative. In Java with no DI container the cost is near zero, so
this is a defensible habit rather than a mistake — but it is exactly the habit that becomes
**actively harmful** in a framework with dependency injection, where it duplicates the container.

**When to use it.** When you can name the second implementation *and* a plausible reason it would
exist. "A test double" counts. "Someday we might switch" does not.

**How to reproduce.** In the web app: use the framework's DI. Define the interface where it is
*consumed*, not where it is implemented. Do not write factories.

### 3.4 Outcome enums instead of booleans or exceptions

**What.** `ApplyResult.Status` has seven values:
`SUBMITTED_VERIFIED`, `SUBMITTED`, `REVIEW_READY`, `NEEDS_INPUT`, `SKIPPED`, `CHALLENGED`, `FAILED`.

**Why it worked — this is the quietest, highest-value pattern in the codebase.**

1. **Java's exhaustive `switch` makes it impossible to ignore a case.** `AppCore` switches on it
   four separate times — to decide whether to screenshot, what to record in history, how to tally,
   and what to tell the user
   ([AppCore.java:662-696](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L662-L696)).
   Adding an eighth status breaks compilation in four places, each of which is a real decision.
2. **The taxonomy is the diagnostic vocabulary.** The same names appear in the run summary panel, in
   `issues.tsv`, in the session log's timeline column, and in the grouped notes. When a user says
   "it said NEEDS_INPUT five times," that string traces to one code path.
3. **It encodes the honesty constraint in the type system.** `SUBMITTED` and `SUBMITTED_VERIFIED`
   are different values precisely because *a click is not proof* (`DECISIONS.md:85-89`). A boolean
   `submitted` could not have expressed that, and the distinction is the entire content of issue #15.

**When to use it.** For any operation with more than two meaningful endings — which is almost every
operation that touches a network, a user, or a payment.

**How to reproduce.** Replace `boolean success` / `throw` pairs with a discriminated union
(TypeScript union type, Rust enum, Python `Literal` + dataclass, Kotlin sealed class). Rule: **if
two failures need different handling, they are different values.** Then use those value names
verbatim in your logs, your metrics labels, and your UI copy — one vocabulary, end to end.

### 3.5 Safe default at every irreversible action

**What.** `SubmitMode.REVIEW` is the default and never submits. Probes never click Submit. A
submission is only counted as verified when a confirmation is detected afterwards. On a captcha, the
app disconnects and hands off rather than retrying.

**Why it worked.** `CLAUDE.md` states the stakes plainly: *"A wrong answer is not a failing test, it
is a false statement to an employer."* Once that is written down, the defaults follow mechanically.

**When to use it.** Any action that spends money, sends a message, changes someone else's state, or
cannot be undone.

**How to reproduce.** List your irreversible actions on day 1 — in the web app: charge a card, send
an email, delete an account, publish, invite. For each, the default path must be the reversible one,
and the irreversible one must be reached by an explicit choice that is *recorded*.

### 3.6 Layered defence around an untrusted component

**What.** Four layers between the local LLM and a real employer's form:

1. **Structural** — the JSON schema is compiled by llama.cpp into a grammar, so a dropdown answer is
   *physically incapable* of being off-list
   ([ScreenerPrompt.schemaFor](../src/main/java/com/haadlit_sp/appCoreLogic/llm/ScreenerPrompt.java#L104-L142)).
2. **Semantic** — `LlmResponseValidator` re-checks the option is in the list, the number is in
   range, `unsure` is a real boolean, and the text is TSV-safe. 14 test cases.
3. **Behavioural** — pay questions are refused before the model is even called
   ([LlmQuestionAnswerer.java:62-65](../src/main/java/com/haadlit_sp/appCoreLogic/llm/LlmQuestionAnswerer.java#L62-L65)).
4. **Fallback** — any failure returns `Optional.empty()`, which means "ask the human." AI
   unavailable degrades to exactly Standard mode.

Plus a fifth, subtler one: **transport failures are not cached, but "model said unsure" is**
([LlmQuestionAnswerer.java:69-77](../src/main/java/com/haadlit_sp/appCoreLogic/llm/LlmQuestionAnswerer.java#L69-L77)).
That distinction was a *bug fix* — `ISSUES.md` #4 records a whole module answered blank because
transport failures were negative-cached for the session.

**Why it worked.** Each layer catches a different class of failure. The grammar cannot catch "the
model is confidently wrong"; the validator cannot catch "the server is down."

**When to use it.** Any component whose output you cannot fully trust and whose failure has real
consequences: an LLM, a third-party API, user input, an OCR pipeline.

**How to reproduce.** For each untrusted producer, ask four questions and build a layer per *yes*:
Can I make the bad output structurally impossible? Can I check it? Can I refuse to ask in cases I
know are unsafe? What is the safe behaviour when all of that fails? Then **cache negative results
only when the negative is stable** — a "no" from a reasoning failure is durable; a "no" from a
timeout is not.

### 3.7 Threading contained to one class

**What.** Four threads, all owned by `AppCore`: the Swing EDT, `browser-worker` (single-threaded
because the driver is thread-affine), `doc-worker` (separate so PDF parsing is not stuck behind a
multi-minute sign-in poll — see the comment at
[AppCore.java:75](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L75)), and `llm-worker`.

The UI never blocks: it reads plain fields on a single 500 ms timer
([App.java:106-114](../src/main/java/com/haadlit_sp/appRenderLogic/App.java#L106-L114)), and only
the visible page does work.

**Why it worked.** Concurrency bugs are confined to one file. Everything else — the walkthrough, the
enumerator, the stores — is written as straight-line blocking code and is *much* easier to read for
it. `ApplyWalkthrough` calls `Thread.sleep` freely because it knows it owns its thread.

**Where it broke.** The rule is stated but not enforced, and it is violated —
see [§10.6](#106-the-documented-threading-rule-is-violated).

**When to use it.** Any app with a UI thread plus background work. The pattern generalises as
"designate one place where concurrency is allowed to be complicated."

**How to reproduce in a web app.** The mapping is not literal. The request handler is already your
"never block the UI" boundary. The transferable half is: **background work belongs in a named,
bounded queue with an owner**, and everything downstream of it is written as straight-line code that
does not think about concurrency. The non-transferable half is polling shared mutable fields — see
[Part 11](#part-11--what-transfers-and-what-inverts).

### 3.8 The application generates its own bug reports

**What.** Three layers of runtime recording, all under `~/.indeedapplier/diagnostics/`:

| Artefact | Granularity | Purpose |
|---|---|---|
| `issues.tsv` + PNG | One row per failed application | *That* something failed, with a picture |
| `sessions/<stamp>.md` | One file per run | *What* the run accomplished, and what went wrong, grouped |
| The step journal inside it | Every module, question, click, wait | *Why* it stopped where it did |

The journal line for a question records five things:

```
21:51:49 **question** — As part of the position you may be dealing with disposal of medical
sharps…?  [TEXT]  ->  "Yes" (BANK) — FILL FAILED
```

Question, type, answer, **source**, and **whether the fill landed**. That is what
[ApplyWalkthrough.java:276-281](../src/main/java/com/haadlit_sp/appCoreLogic/apply/ApplyWalkthrough.java#L276-L281)
produces, and its inline comment explains why: *"The one record that explains an empty box
afterwards."*

**Why it worked — with a concrete example.** `ISSUES.md` #23: a run reported 0 submitted despite the
user completing a captcha and seeing a confirmation. The journal showed the walkthrough arrived at
`review-module`, read **0 questions**, found no Submit button, clicked Continue (which that step does
not have), and abandoned the application **one second after arriving**. The cause was Indeed's
"Preparing review" spinner. Without a timestamped step journal, that is indistinguishable from "the
review page is broken."

**The derived-notes layer is the part most worth stealing.**
[`SessionLog.notes()`](../src/main/java/com/haadlit_sp/appCoreLogic/store/SessionLog.java#L159-L211)
does not dump events — it *interprets* them:

- groups repeated failures by message, most common first;
- flags submissions counted but never confirmed (`"Unverified"`);
- and detects an environmental pattern: *"every attempt failed the same way… usually means something
  environmental (signed out, blocked, offline) rather than a per-posting problem."*

That last one is a diagnosis the log writes about itself.

**When to use it.** Any system that runs unattended, or where the user cannot tell you what
happened. Which includes every web app with real users.

**How to reproduce in a web app.** Three tiers, same idea, different mechanism:

1. **Structured events**, one per meaningful step, with a correlation ID threading a whole user
   action. This is the journal.
2. **A per-operation summary** the *system* writes — not a dashboard you have to interpret. "This
   checkout had 4 steps, step 3 retried twice, ended ABANDONED_PAYMENT_DECLINED."
3. **Derived notes**: a scheduled job that reads yesterday's events and writes prose — grouped
   failures, ranked; anomalies called out; "all N failed identically" flagged as environmental.

**Build tier 1 before your third feature, not after your first outage.**

### 3.9 Where the logic lives

| Kind | Home | Rule |
|---|---|---|
| Domain policy | `answer/`, `SubmitMode.autoSubmits()` | The decision itself, with no I/O |
| Orchestration | `ApplyWalkthrough`, `PostingEnumerator` | Sequence and error handling, no site knowledge |
| External knowledge | `IndeedSelectors` | Selectors and DOM scripts only |
| Infrastructure | `browser/`, `store/`, `llm/` | Mechanism, no policy |
| Data | `model/` | Records + enums; validation and normalisation only |
| Presentation | `appRenderLogic` | No decisions at all |

The cleanest illustration:
[`SubmitMode.autoSubmits(easyApply, allAnswered)`](../src/main/java/com/haadlit_sp/appCoreLogic/model/SubmitMode.java)
is business policy living on an enum. It is pure, trivially testable, and the walkthrough consults
it rather than re-deriving the rule. Compare that to how easily the same three-way condition could
have been an `if` chain inside the 516-line walkthrough.

---

## Part 4 — Code and file organisation

### 4.1 The layout

```
src/main/java/com/haadlit_sp/
├── Main.java                    38 lines — entry point only
├── appCoreLogic/
│   ├── AppCore.java            719 lines — the facade  ⚠ see §10.5
│   ├── answer/     3 files      interface + rules + factory
│   ├── apply/      5 files      walkthrough, reader, filler, result, journal
│   ├── browser/    5 files      interface, impl, factory, profile, selectors
│   ├── llm/        9 files      runtime, installer, assets, client, prompt, validator, …
│   ├── location/   3 files      interface, impl, factory
│   ├── model/     16 files      records and enums
│   ├── pdf/        2 files      text extraction, fact extraction
│   ├── search/     2 files      enumerator, result
│   ├── session/    3 files      interface, impl, factory
│   └── store/      7 files      paths + one class per persisted thing
└── appRenderLogic/
    ├── App.java                122 lines — card layout + the one timer
    ├── StartupModeDialog.java
    ├── pages/      7 files
    ├── components/ 10 files
    └── theme/      2 files
```

### 4.2 File-size discipline

Median source file: **~60 lines.** Distribution:

| Size | Count | Character |
|---|---|---|
| < 30 lines | 24 | Records, enums, interfaces, factories |
| 30–100 | 34 | Most real classes |
| 100–200 | 18 | Substantial single-responsibility classes |
| 200–400 | 5 | `RuleBasedAnswerer` 252, `Theme` 352, `IndeedSelectors` 370, `ResultsPanel` 223 |
| > 400 | 2 | `ApplyWalkthrough` 516, `AppCore` 719 |

The two large files are the two places the design is under strain — and they are also the two files
with no tests. That correlation is not a coincidence: **a file gets large because it accumulated
responsibilities, and it accumulates responsibilities because nothing forced it to be
constructible in isolation.** A test is that forcing function.

The three 200–400 files are all *legitimate*: `Theme` and `IndeedSelectors` are single-source-of-truth
registries (large by design — that is the point), and `RuleBasedAnswerer` is cohesive by change-reason
([§2.3](#23-the-splitting-rule-inferred)).

### 4.3 Naming

**Good and consistent:**

- Package names are **verbs or domains**, never layers: `apply`, `answer`, `search` — not
  `services`, `managers`, `helpers`.
- Class names say what the thing *is*: `PostingEnumerator`, `ApplyFormReader`, `LlmResponseValidator`.
- Method names read as questions or commands: `asksYearsOfExperience`, `submitBlocked`, `leftFlow`,
  `waitForReviewReady`.
- Constants are named for the *phenomenon*, not the value: `REVIEW_READY_ATTEMPTS`,
  `HANDOFF_WAIT_MS`, `BETWEEN_PAGES_JITTER_MS`.

**Inconsistent:** `Page1`, `Page2`, `Page3`, `Page4` sit beside `PersonalDetailsPage` and
`LivePage`. Worse, they are addressed by string literals — `showPage("Page1")`, `"Details"` —
in [App.java:69-99](../src/main/java/com/haadlit_sp/appRenderLogic/App.java#L69-L99) and in every
page's navigation call. Renaming a step means editing string constants in three places with no
compiler help. See [§10.8](#108-stringly-typed-navigation).

The tell: the four numbered pages came from the boilerplate (`01dbef2`); every page created *after*
the project had a domain (`PersonalDetailsPage`) is named properly. **Inherited scaffolding carries
inherited naming, and nobody goes back.**

### 4.4 Comment convention

`EXTENDING.md:25-26`: *"Comments explain why, not what. Match the surrounding density: a line where a
non-obvious constraint bit us, not narration of the code."*

The codebase honours this, and the comments are unusually valuable because most of them encode a
*measurement*:

```java
// A challenge page never becomes "module ready", so ask WHY before blaming the
// load — mid-run Cloudflare walls looked like generic failures until now.
```
— [ApplyWalkthrough.java:98-99](../src/main/java/com/haadlit_sp/appCoreLogic/apply/ApplyWalkthrough.java#L98-L99), which is `ISSUES.md` #13 written at the line it protects.

```java
// offsetParent alone is not enough: invisible-mode reCAPTCHA renders its widget
// COLLAPSED (near-zero box) while still "visible" to layout checks. A checkbox the
// human must click is ~300x78; a challenge grid is bigger. So require real size.
```
— [IndeedSelectors.java:305-308](../src/main/java/com/haadlit_sp/appCoreLogic/browser/IndeedSelectors.java#L305-L308), which is `ISSUES.md` #11.

> **This is the single most reusable micro-practice in the repository.** When a bug is caused by
> something counter-intuitive, the fix comment states the counter-intuitive fact *and the number that
> proves it*. Anyone who later "simplifies" that line reads why they shouldn't. It is documentation
> that cannot drift from the code, because it is on the code.

---

## Part 5 — The development workflow

Reconstructed from commit sizes, the `.claude` allowlist, `EXTENDING.md`, and `ISSUES.md`.

### 5.1 Before the pivot: build → compile → eyeball

Commits are 100–500 lines, one capability each, and the allowlist shows the loop was
`gradlew compileJava` → run → look. `EXTENDING.md:99-101` records two hard-won probe rules that
could only have come from being burned:

- *"Call `Theme.install()` in any rendering probe, or you're testing a look-and-feel the app never
  uses."*
- *"Capture with `component.printAll(g)`, not a screen grab — a screen grab catches whatever window
  happens to be in front."*

### 5.2 After the pivot: the improvement loop

`CLAUDE.md` states it as four steps, and `EXTENDING.md:106-116` repeats it. Reconstructed with
actual mechanisms:

```
  ┌─ 1. RUN ────────── the user runs a real session
  │                     app writes sessions/<stamp>.md + issues.tsv + screenshots
  │
  ├─ 2. READ ───────── read the session file, not the developer's memory
  │                     "Diagnose from that evidence, not from assumption"
  │
  ├─ 3. DIAGNOSE ───── ask what the screen WAS before deciding it failed
  │                     (#13: 8 "load failures" were Cloudflare pages)
  │
  ├─ 4. FIX ────────── one file where possible; comment the counter-intuitive fact
  │
  ├─ 5. RECORD ─────── ISSUES.md row: symptom | diagnosis | fix | status
  │                     OPEN → FIXED? → VERIFIED
  │
  └─ 6. CONFIRM ────── only a later real run promotes FIXED? → VERIFIED
                        ⚠ this is the step that did not happen — §10.3
```

**The status ladder is the best idea in the workflow.** `FIXED?` — shipped but unproven — is a state
most trackers lack, and its absence is why teams believe things are fixed that are not. `ISSUES.md`
even distinguishes *"**OPEN** (mitigated, not solved)"* (#8) and *"**OPEN** (instrumented, cause not
yet proven)"* (#19) — the second is a beautiful state to be able to name: *we don't know why, and we
have made the next occurrence explain itself.*

### 5.3 Two verification modes, deliberately separated

`EXTENDING.md:89-105` draws the line:

| | Unit tests (`gradlew test`) | Scratch probes |
|---|---|---|
| Cover | Pure logic: rules, validator, prompt shape, session log | Anything involving a live site, a real process, or a rendered UI |
| Live in | `src/test/` | The scratchpad; compiled against `build/install/…/lib/*` |
| Found | Regressions | An auto-unboxing crash on first health poll; a captcha false-positive; a session file overwriting itself |

*"Probes have caught things tests structurally cannot."* The probe list in the allowlist confirms
the range: an `LlmRuntime` probe, a badge-markup probe rendering 8 HTML variants side by side under
the real theme (`ISSUES.md` #17), a startup-dialog screenshot probe, and one that **deliberately
corrupts `llama-server.exe`** to exercise the failure path.

The discipline that keeps this honest: `EXTENDING.md:103-104` — *"Be sparing with probes that hit
Indeed… space them out and prefer local `data:` URLs when testing page-detection JS."* Probing has a
cost that is borne by the user's real account.

**The gap:** probes are ephemeral. `INDEED.md:123` claims `SUBMITTED_CONFIRMATION_JS` *"is verified
against fake local pages for both hits and non-hits."* That verification happened, but it exists
nowhere in the repository. It is a claim about a past event.

> **Rule for the next project: any probe that establishes a fact you will rely on must graduate into
> a committed test with a committed fixture.** A probe proves it once; a test proves it forever.

---

## Part 6 — Documentation and knowledge management

### 6.1 The system

Six documents, each with a distinct job and near-zero overlap. This is the part of the project most
worth copying verbatim.

| File | Question it answers | Audience | Half-life |
|---|---|---|---|
| `README.md` | What is this and how do I use it? | User | Long |
| `docs/ARCHITECTURE.md` | What are the parts and how does a request flow? | Developer, orienting | Medium |
| `docs/DECISIONS.md` | Why is it like this? | Developer, about to change something | **Permanent** |
| `docs/EXTENDING.md` | How do I add X? | Developer, mid-task | Medium |
| `docs/INDEED.md` | What is true about the outside world? | Developer, debugging | **Short (by design)** |
| `ISSUES.md` | What is broken and is the fix proven? | Developer, triaging | Live |
| `CLAUDE.md` | What must an AI session know first? | AI agent | Medium |

### 6.2 What makes each one work

**`README.md`** answers "what will it *not* do" (`:52-56`) as prominently as what it will, and has a
table of exactly where every piece of user data lives (`:59-72`). Both are trust-building and both
are things users actually ask.

**`ARCHITECTURE.md`** leads with the one rule, then an ASCII diagram, then a package table with a
**"Notes" column that carries the constraint, not just the contents**:

> *"`IndeedSelectors` is the only place Indeed's markup is written down."*
> *"All fail-soft: a store problem must never break a run."*

A table of contents tells you where code is. This table tells you *what you are not allowed to do*.

**`DECISIONS.md`** — the crown jewel. Its format is not the ADR template; it is
**Decision → Why (with the measurement) → The line that does not move.** The Cloudflare section is
the model:

> *"The obvious approach — Playwright's `launchPersistentContext(channel: "chrome")` — was measured
> and is unusable: it sets `navigator.webdriver === true`… The 'Verify you are human' checkbox loops
> forever even when a real human clicks it… Attaching over CDP gives `webdriver === false` because
> the browser genuinely is a normal browser; the same search that was walled scraped cleanly."*
>
> *"**The line, which does not move:** never patch `navigator.webdriver`, never
> `--disable-blink-features=AutomationControlled`, never solve a captcha."*

It records the **rejected alternative and why it failed**, so nobody re-tries it; and it separates
the *decision* (revisable) from the *principle* (not). That last distinction is missing from most
ADR practice and is worth adopting.

**`EXTENDING.md`** is organised as **recipes for changes that actually happen**, not a style guide:
"Add a rule for a screener question" · "Handle a new page or interstitial" · "Add persisted state."
Each names the file, the test, and the trap. It also carries the accumulated Swing gotchas
(`:70-76`), including the one where an em space is built from its codepoint *because a literal one
is invisible in source and has been silently dropped by an edit* — a bug caused by editing, recorded
where the next editor will see it.

**`INDEED.md`** is the one most projects lack and the one this project could least do without. It
holds knowledge that is **discovered, not designed**: that the title lives in the anchor and there
is no `h2`; that `el.required` lies; that a synthetic `element.click()` silently does nothing; that
Indeed *repeats* results past the last page instead of returning none. It opens by declaring its own
perishability — *"Treat this as perishable — Indeed changes"* — and dates its claims
(*"Verified 2026-08-05"*).

**`ISSUES.md`** is a table, not a prose log: `# | First seen | Symptom | Diagnosis | Fix | Status`.
Separating **symptom** from **diagnosis** is what makes it useful — the symptom is what the user
saw, the diagnosis is what was actually true, and the gap between them is where the learning is.
Row #13 is the exemplar: symptom *"8 consecutive postings 'A step did not finish loading'"*,
diagnosis *"Screenshots show these were **Cloudflare** pages, not load failures."*

**`CLAUDE.md`** is short and prioritised: environment first, then *"What matters most here,"* then
the loop. Its "Verify before claiming" section lists four specific past failures. That is far more
effective than an abstract instruction, because it is evidence that the failure mode is real.

### 6.3 The fourth document: external reality

Most projects have README + architecture + some contribution notes. The document that made the
difference here is **`INDEED.md` — a home for facts about the world that you learned the hard way.**

Without it, those facts have three bad homes: a comment on one line (invisible to someone reading a
different file), a commit message (unfindable), or the developer's head (lost).

Your web app has a direct analogue and will need it more, not less: the payment provider's actual
webhook ordering, the identity provider's real token lifetime, the email service's silent
rate-limit, the browser quirk that only appears on iOS Safari. `templates/docs/DOMAIN.md` is this
document, generalised.

### 6.4 Gaps, redundancy, and drift risk

**Missing:**

- **No operations document.** Acceptable here (the "deployment" is `gradlew run`). Fatal in a web
  app — see [templates/docs/OPERATIONS.md](templates/docs/OPERATIONS.md).
- **No test-strategy document.** The probe-vs-test split is real, valuable, and buried in the middle
  of `EXTENDING.md`. It deserves its own heading at minimum.
- **No data-format documentation.** Six TSV files with no schema, no version field, and no note on
  what happens when the format changes.
- **No LICENSE**, despite bundling GeoNames data under CC BY 4.0 and shipping MIT/Apache assets.

**Redundant (mildly):** the "never fabricate" constraint appears in `README.md`, `CLAUDE.md`,
`EXTENDING.md`, and `DECISIONS.md`. For a *safety* constraint this repetition is arguably correct —
it should be unmissable from any entry point — but note that the four wordings already differ
slightly, which is how a canonical rule starts to blur.

**Most likely to go stale:**

1. `INDEED.md` — by design; it declares this and dates its claims, which is the correct mitigation.
2. `ARCHITECTURE.md`'s package table — it lists class names, so every new class is a chance to drift.
   Already at risk: it omits nothing today, but nothing enforces that.
3. `ARCHITECTURE.md:97`'s threading rule — **already false** ([§10.6](#106-the-documented-threading-rule-is-violated)).
4. `AppCore`'s class Javadoc — **already false, and it is the first thing anyone reads.**

**Undocumented decisions I could not find a reason for** (stated as unknowns, not criticisms):

- Why `Page1`–`Page4` were never renamed. *Not determinable — most likely inherited-scaffold inertia.*
- Why `MAX_PAGES = 10` and `MAX_MODULES = 20`. Sensible caps, but the numbers have no recorded basis.
- Why the answer-mode choice is a startup dialog rather than a setting inside the app. Plausibly
  because AI mode needs an eager download decision before the UI is useful — but this is inference.
- Whether `history.tsv` conflating "applied" with "attempted" was deliberate.
  [AppCore.java:660-661](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L660-L661) says
  *"so it isn't re-tried,"* which suggests deliberate, but its interaction with `NEEDS_INPUT`
  (issue #22) suggests the consequence was not foreseen.

---

## Part 7 — The decision-making system

### 7.1 The major decisions

| Decision | Context | Reasoning | Alternative rejected | Trade-off | Result |
|---|---|---|---|---|---|
| Attach to real Chrome over CDP | Cloudflare walls automated browsers | **Measured:** `launchPersistentContext` sets `webdriver === true`; the human-verification checkbox then loops forever even for a real human. CDP attach gives `webdriver === false` because it genuinely is a normal browser | Playwright-launched persistent context; patching `navigator.webdriver` (refused on principle) | User must launch Chrome and sign in by hand | Works. Still the #1 source of friction (`ISSUES.md` #8, OPEN) |
| Local 1.7B model, not an API | Needs to answer novel screener questions | No key, no account, no network, resume never leaves the machine; task is short so a 1.1 GB quantised model suffices | Hosted API | 1.1 GB first-run download; weaker model | Works; ~1.7 s/question warm |
| Numbered decision procedure in the prompt | *Measured:* telling a 1.7B model to "be conservative" **does not work** — it answered `0` then flagged itself unsure, and invented availability | Small models follow procedures, not principles | Abstract instructions | Prompt is long and rigid | 6/6 correct, from pausing |
| Byte-identical profile block | Prompt processing dominates CPU inference | llama-server's prefix cache makes every question after the first fast | Per-question prompt assembly | Any prompt edit must preserve the prefix | ~1.7 s vs ~5 s |
| Review mode as default | Submitting is irreversible and goes to a real employer | The cost of a wrong auto-submit is unbounded; the cost of an extra click is one click | Auto by default | Less automation out of the box | Correct |
| Everything in plain TSV | Single-user desktop app | Human-inspectable, append-only, no dependency, trivially recoverable | SQLite; JSON | No schema, no transactions, no migration path | Correct **here**; wrong for the web app |

### 7.2 The reusable principles behind them

1. **Measure the obvious approach before rejecting it.** The Cloudflare decision is not "CDP is
   better"; it is "we tried the normal thing, here is the specific observation that killed it."
   Rejected-alternative-with-evidence is what makes a decision durable.
2. **Separate the decision from the principle.** *"Attach over CDP"* is revisable. *"Never patch
   `navigator.webdriver`, never solve a captcha"* is not. Write both, and label which is which.
3. **Let the stakes pick the default.** Enumerate irreversible actions; the default is always the
   reversible branch.
4. **Constrain structurally before instructing behaviourally.** A JSON schema compiled to a grammar
   beats a prompt that says "only pick from the list" — then validate anyway, because the structural
   constraint cannot catch confident wrongness.
5. **When a model or a service disappoints, change the procedure, not the adjectives.** "Be
   conservative" failed; a numbered decision procedure with concrete examples worked. This
   generalises well beyond LLMs.
6. **Pin external dependencies with hashes, in one file, with a verification date.** `LlmAssets`
   carries URL + sha256 + byte size + *"Pins verified 2026-08-01."*
7. **Where the honest answer is knowable, give it confidently; where it is not, say so.** Encoded as
   the difference between "unmentioned qualification → `No`, not unsure" and "pay expectation →
   unsure." Most systems collapse these into one "I don't know," which is worse on both sides.

### 7.3 What was decided by default rather than chosen

Being fair about this matters for the new project:

- **Java 21 / Swing / Gradle** — inherited from `JavaAppBoilerplate`. Not evaluated. For a desktop
  app on Windows with a single developer who already knows Java, defensible; but it is a *default*,
  not a decision, and Swing is why `ISSUES.md` #16 and #17 exist.
- **TSV for everything** — a habit, applied consistently. Correct outcome; not visibly reasoned.
- **Package naming and the two-root split** — inherited.
- **No CI** — never considered, as far as the repository shows.

> The lesson is not "inheritance is bad." It is: **inherited defaults should be re-examined once,
> explicitly, at the point the project's real requirements are known** — and the outcome recorded in
> `DECISIONS.md`, even if the outcome is "keeping it." Otherwise you cannot tell later which parts of
> your architecture anyone actually thought about.

---

## Part 8 — Testing, validation, and debugging

### 8.1 What is tested

5 test classes, 47 test methods, covering 5 of ~85 production classes.

| Test | Covers | Quality |
|---|---|---|
| `RuleBasedAnswererTest` | 8 tests on answer rules | **Excellent.** Tests the *safety property*, not just behaviour: `skillSpecificYearsQuestionsAreNotAnsweredWithTheTotal` asserts the answerer **declines** four phrasings — a test that something does *not* happen |
| `LlmResponseValidatorTest` | 14 tests across every question type | **Excellent.** Off-list, malformed, `unsure`-not-boolean, oversize, blank, dedup, TSV-hostile chars |
| `IndeedSelectorsTest` | 7 tests on URL construction | **Good.** Covers the two irregular domains with a comment saying *why* (`// Not us.indeed.com, and not gb.indeed.com`) and the km→mi conversion |
| `ScreenerPromptTest` | 6 tests on schema + prompt shape | **Good.** Asserts the system prompt still contains its safety words |
| `SessionLogTest` | 6 tests | **Good.** `eachRunWritesItsOwnFileStampedWhenTheRunStarted` is a direct regression test for `ISSUES.md` #18 |

Two things stand out. First, **the tests are written in domain language** — `numericPayQuestionsStillPause`,
`unsureTrueIsRejected`, `offListOptionIsRejected` — so the test list reads as a specification of the
honesty constraint. Second, **several are regression tests traceable to a specific `ISSUES.md` row**,
which is exactly the discipline that keeps a suite meaningful.

### 8.2 What is not tested — and why it matters

**Untested:** `ApplyWalkthrough` (516 lines), `ApplyFormReader`, `ApplyFiller`, `PostingEnumerator`,
`ProfileFactsExtractor`, `QaBankStore`, `ApplicationHistoryStore`, `ContactDetailsStore`,
`LlmClient`, `LlamaServerRuntime`, `LlmAssetInstaller`, `BundledCityLocationSuggester`, all of
`appRenderLogic`.

The critical one is `ApplyWalkthrough`: it decides whether to click Submit on a real employer's
form, and it has no tests at all. Eight `ISSUES.md` rows — 3, 5, 6, 11, 12, 13, 14, 23 — are
walkthrough **control-flow** bugs, all found in production on live applications.

Every one of those was testable without a browser. `BrowserDriver` is a 13-method interface; a fake
that returns scripted values per call would have caught:

- **#13** — challenge-check ordering. A fake where `MODULE_READY_JS` returns false and
  `IS_CHALLENGE_JS` returns true must yield `CHALLENGED`, not `FAILED`.
- **#23** — the review spinner. `REVIEW_PREPARING_JS` true for N polls, then a Submit button
  appears; assert the walkthrough waits and never clicks Continue on a review module.
- **#11** — disabled Submit as *state*: `SUBMIT_STATE_JS` returns `"disabled"` then `"enabled"`;
  assert it stands by and then submits.
- **#5/#14** — the error interstitial and its "still having trouble" wording variant.

> **This is the project's largest single gap, and it is a workflow gap, not a skill gap.** The seam
> that makes the code good (`BrowserDriver` as a narrow interface) is exactly the seam that makes it
> testable — it was built for swappability and never used for verification.

### 8.3 Error handling

Consistent and thoughtful:

- **Fail-soft on non-essential paths.** Every store catches `IOException`, logs at `WARNING`, and
  continues. `captureDiagnostics` is wrapped so that failing to *record* a failure cannot cause one
  ([AppCore.java:731-750](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L731-L750)).
- **Errors are translated for the user.** `describeBrowserFailure` turns a connection refusal into
  *"No Chrome to attach to — click 'Open Chrome to sign in' first and leave it open."* Every message
  states the next action.
- **Failure is a value, not an exception**, for expected outcomes (`ApplyResult.Status`,
  `EnumerationResult.Outcome`). Exceptions are reserved for genuinely unexpected conditions.
- **Ask what happened before reporting failure.** The single best line of error handling in the
  codebase is the ordering at
  [ApplyWalkthrough.java:94-114](../src/main/java/com/haadlit_sp/appCoreLogic/apply/ApplyWalkthrough.java#L94-L114):
  on a timeout, check *left the flow?* → *challenged?* → *error screen with retry?* → *error screen
  without retry?* → only then report "a step did not finish loading."

**Weaknesses:** `catch (Exception e)` at the worker-task level in `AppCore` is broad enough to
swallow programming errors as "browser errors." And `catch (RuntimeException)` around fill
operations ([ApplyFiller.java:53-56](../src/main/java/com/haadlit_sp/appCoreLogic/apply/ApplyFiller.java#L53-L56))
means a genuine bug looks identical to "the field wasn't there" — mitigated only because the journal
records `FILL FAILED`.

### 8.4 A reusable debugging process

Formalising what this project actually did:

```
DETECT      The system tells you. Not a user report — a diagnostics row, a screenshot,
            a session file. If you learned about it from a human, your instrumentation
            has a gap; fix that too.

REPRODUCE   Prefer replaying recorded state over re-running reality. Live re-runs cost
            real resources (here: Cloudflare escalation against the user's account).

ISOLATE     Ask what the thing WAS before deciding what it wasn't.
            #13: 8 "load failures" were Cloudflare pages.
            #11: a "captcha" was an invisible widget passing a layout check.
            #23: an "empty page" was a page still rendering.
            The generalisation: a negative check ("X is not ready") tells you nothing
            about why. Always pair it with positive identification.

DIAGNOSE    Write the cause in one sentence, distinct from the symptom. If you cannot,
            you have not diagnosed it. ISSUES.md forces this with two columns.

FIX         One file if the architecture allows. If it does not, note why in the row —
            that is a design signal.

TEST        Write the test that would have caught it, at the level it was caught.
            A probe proves it once; a committed test proves it forever.

VALIDATE    FIXED? until a real run says otherwise. Never mark it done on belief.

DOCUMENT    ISSUES.md row + a comment at the line stating the counter-intuitive fact.
            If the cause was a fact about the outside world, DOMAIN.md too.
```

---

## Part 9 — Extensibility and maintainability

### 9.1 What makes change cheap here

| Change | Cost | Why |
|---|---|---|
| New screener-question rule | 1 file + 1 test | `RuleBasedAnswerer` + `RuleBasedAnswererTest`; recipe in `EXTENDING.md` |
| Selector broke | 1 file | Everything Indeed-specific is in `IndeedSelectors` |
| Model or engine version bump | 1 file | `LlmAssets` |
| New persisted thing | 2 files | `AppPaths` + a store class, following an established shape |
| New UI page | 1 file + 1 registration | `pages/`, implement `LivePage`, register in `App` |
| New browser capability | 2 files | Interface verb + implementation |
| Swap the whole answering strategy | 1 factory line | Proven: AI mode added with no walkthrough change |

The three properties that produce this: **one place per volatile fact**, **interfaces at swap
points**, and **recipes in `EXTENDING.md` that name the file and the test**.

### 9.2 What makes change expensive

| Area | Coupling | Consequence |
|---|---|---|
| `AppCore` | Everything routes through it | Any new capability grows a 719-line file. Its four `switch (result.status())` blocks must all be updated together |
| `ApplyWalkthrough` | Flow + interstitials + submit policy + journalling in one 516-line method-set | The `apply()` loop is ~130 lines with 12 exit points. Adding an interstitial means finding the right place among the ordered checks — and the *order is load-bearing* (#13) but not asserted anywhere |
| Page navigation | String keys in `App` and every page | Renaming a step: 3+ edits, no compiler help |
| TSV formats | No version field | A format change silently misparses old data |
| `history.tsv` | Dedup key + audit log + handled-marker | Cannot change retry policy without changing the audit log's meaning |

### 9.3 What makes it understandable months later

Genuinely strong, and worth enumerating because these are cheap to replicate:

1. **`ARCHITECTURE.md`'s one rule** — the boundary is graspable in one sentence.
2. **Javadoc that states purpose and constraint**, not signature. `QuestionAnswerer`: *"An empty
   result means 'not confident' — the caller pauses and asks the user… Better to ask than to answer a
   real application wrong."* That tells you what to do with the return value *and* why.
3. **`DECISIONS.md` pre-empts the "why is this weird?" question**, which is the question that
   actually blocks people.
4. **Comments carry measurements.** `~300x78`, `~1.7 s versus ~5 s`, `>40×40`. A number is much
   harder to argue with than an assertion.
5. **`ISSUES.md` is a searchable history of every trap**, with the symptom you would actually observe
   as the search key.

The two things working against it: the stale `AppCore` Javadoc (the first file anyone opens), and
the dead stubs that make a reader wonder which parts of the facade are real.

---

## Part 10 — Critical review

Ordered by real risk. Each is Problem → Evidence → Why it matters → Likely consequence → Fix.
Classification per your Section 11 request: **[Actual]** already causing harm · **[Potential]** will
bite as it grows · **[Oversight]** missed consideration · **[Trade-off]** reasonable given
circumstances · **[Improvement]** already fine, could be better.

### 10.1 A real resume PDF is committed to a public repository — **[Actual]**

**Evidence.** `src/main/resources/Resume.pdf` is tracked (`git ls-files`), added in `f2eef92
"additonal .pdf x indeed drivers logic"` (2026-07-31), and **referenced by zero lines of code** — a
grep for `Resume.pdf` across `src/` returns nothing, and the only two `getResourceAsStream` calls
load `cities.tsv` and the app icon. The remote is `https://github.com/HaadLIT/IndeedAutoApplier`.
The file is a genuine one-page PDF (`%PDF-1.7`, embedded fonts, `StructTreeRoot`).

**Why it matters.** A resume contains full name, email, phone number, and typically a home address
and employment history. It is committed to a repository whose visibility I cannot verify from here,
and it is in the git *history* — so deleting the file from `HEAD` does not remove it.

**Likely consequence.** If the repo is public: permanent, indexable exposure of personal contact
details. Also a live example of the exact failure the app itself is careful about — `ProfileFacts`'
Javadoc says `rawText` *"must NEVER be written to disk,"* and `DECISIONS.md:116` says *"raw resume
text stays in memory and is never persisted."* The care was applied to the runtime and not to the
repository.

**Fix.** Delete the file (it is unused). Then decide on history: `git filter-repo` or a fresh repo
if it is public; nothing further if it is private and will stay so. Add a `.gitignore` rule and a
pre-commit check for `*.pdf` outside a fixtures directory. **Test fixtures containing real personal
data should be synthetic from the start** — the tests already do this correctly with
`new ProfileFacts("Pat Doe", "pat@x.com", …)`.

### 10.2 The highest-risk class has no tests — **[Actual]**

**Evidence.** `ApplyWalkthrough` is 516 lines and decides whether to click Submit on a real
employer's form. No test file exists for the `apply` package. `ISSUES.md` rows 3, 5, 6, 11, 12, 13,
14, and 23 are all walkthrough control-flow bugs found in production.

**Why it matters.** `CLAUDE.md`: *"A wrong answer is not a failing test, it is a false statement to
an employer."* The class where that risk concentrates is the one with no automated verification.
Worse, the *ordering* of its checks is load-bearing (issue #13 was caused by checking "is it
challenged?" after "did the module load?") and nothing asserts that ordering — a future reorganisation
silently reintroduces the bug.

**Likely consequence.** Regressions land in production on real applications and are discovered by
reading a session log afterwards. This has already happened at least eight times.

**Fix.** A fake `BrowserDriver` returning scripted per-call values. Detailed in
[REMEDIATION.md](REMEDIATION.md) §4, with one test per `ISSUES.md` row above.

### 10.3 The verification loop's last step does not happen — **[Actual]**

**Evidence.** `ISSUES.md` has 26 rows: 4 `VERIFIED`/`FIXED`, ~16 `FIXED?`, 4 `OPEN`. Rows 3–7 have
been `FIXED?` since 2026-08-01 despite 30 sessions being recorded since. The footnote after the
table shows the discipline is understood: *"issues 3–6 look good but stay FIXED? until attributable
evidence."*

**Why it matters.** The whole point of the three-state ladder is that `FIXED?` is not `FIXED`.
Sixteen unproven fixes is sixteen assumptions, and their interactions are unknown. It also degrades
the tracker's signal: when most rows are `FIXED?`, the state stops distinguishing anything.

**Likely consequence.** A fix that never worked stays believed. Effort goes to new problems while an
old one persists under a different symptom.

**Root cause — and this is the important part.** Promotion requires a human to read a session file
and match its absence-of-a-symptom to a row. That is unpaid, unprompted, boring work. **The loop's
last step is manual in a project that automated everything else.**

**Fix.** Make the system do the matching. Tag each `ISSUES.md` row with the failure string it
predicts (`"A step did not finish loading"`, `"No Continue button on step: review-module"`). Have
`SessionLog` emit a machine-readable summary alongside the markdown, and add a tiny script:
*"rows 3, 5, 13 predicted failures that did not occur in the last N sessions covering M postings →
candidates for VERIFIED."* Judgment stays human; the bookkeeping stops being.

### 10.4 Dead skeleton code and a false Javadoc in the facade — **[Actual]**

**Evidence.**
[AppCore.java:766-791](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L766-L791):
`startRun`, `pauseRun`, `stopRun` log `"(stub)"` and mutate a `RunStatus` nothing reads. `history()`
returns a list nothing ever adds to. `RunStatus`, `HistoryEntry`, `StatusPanel`, and `HistoryPanel`
are unreachable from `Main`. The class Javadoc
([:55-62](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L55-L62)) still says the real
engines *"arrive in later slices."*

**Why it matters.** `AppCore` is the file `ARCHITECTURE.md` points at first, and it is what an AI
session reads first. It currently tells a new reader that the automation is stubbed. Meanwhile
`stopApplying()` (the real stop) sits 200 lines away from `stopRun()` (the fake one), which is an
outright trap.

**Likely consequence.** Wasted orientation time; a plausible future bug where someone wires a Stop
button to `stopRun()`.

**Fix.** Delete all six members and the four dead classes; rewrite the Javadoc to describe what
`AppCore` *is*. Small, safe, high value. See [REMEDIATION.md](REMEDIATION.md) §2.

### 10.5 `AppCore` is a facade with a run engine hiding inside it — **[Potential]**

**Evidence.** 719 lines. It owns: three executors and their lifecycle; the browser driver; eight
engine/store instances; 20+ pieces of `volatile` UI state; seven run counters; the auto-apply loop
(`runAutoLoop`); the post-handoff watcher (`standByForHuman`); outcome recording (`recordAndReport`);
tallying (`countOutcome`); diagnostics capture; session-log lifecycle; and every user-facing status
string.

**Why it matters.** At least three independent reasons to change live in one file: the UI's API
surface, the auto-run algorithm, and how outcomes are recorded. `recordAndReport` alone does four
things (screenshot, persist, tally, compose a message) across three `switch` blocks over the same
enum.

**Likely consequence.** Each new capability adds 50–100 lines. The file becomes the merge-conflict
centre and the place nobody wants to touch. It is already the *only* class that cannot be constructed
in a test without starting three threads.

**Fix.** Extract an `ApplyRunner` owning the loop, the stand-by watcher, and the counters, taking a
`BrowserDriver` supplier and a status callback. `AppCore` then delegates and stays a facade — and
`ApplyRunner` becomes testable, which is also most of [§10.2](#102-the-highest-risk-class-has-no-tests--actual).

**Note:** this is a *[Potential]*, not an *[Actual]*. At 719 lines with one developer it is
uncomfortable, not broken. Listed here because it is exactly the size at which extraction is still
cheap.

### 10.6 The documented threading rule is violated — **[Actual, low impact]**

**Evidence.** `ARCHITECTURE.md:97`: *"`AppCore` state read by the UI is `volatile`."* But
[`documents`](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L154) and
[`criteria`](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L155) are plain fields.
`documents` is written on the EDT (`loadDocuments`) and read on the browser worker (`applyOne` →
`documents.resume()`). Separately, `fitScoresVersion++`
([:432-433](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L432-L433) on the browser
worker, [:449](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L449) on the llm worker) is
a non-atomic read-modify-write on a `volatile int` from two threads.

**Why it matters.** Under the JMM there is no happens-before edge for `documents`; the browser
worker may observe a stale value. In practice the executor submission provides one incidentally, so
this is very unlikely to bite. The larger harm is that **a documented invariant is false**, which
teaches future readers that the document is approximate.

**Likely consequence.** Realistically: a lost `fitScoresVersion` increment causing the results list
to miss a repaint until the next score. Theoretically: a resume path read as null.

**Fix.** `volatile` on both fields; `AtomicInteger` for `fitScoresVersion`. Three lines. Then the
document is true again.

### 10.7 `history.tsv` carries three responsibilities — **[Potential]**

**Evidence.** It is simultaneously the dedup key (`nextUnhandledPosting` skips anything in
`appliedIds()`), the permanent audit log (`ARCHITECTURE.md:117`), and the "already handled" marker.
`recordAndReport` writes `NEEDS_INPUT` outcomes into it
([AppCore.java:669-679](../src/main/java/com/haadlit_sp/appCoreLogic/AppCore.java#L669-L679)).

**Why it matters.** A posting the app *could not finish* is permanently excluded from future runs.
The user is told *"Finish it in the browser"* — and if they don't, that posting is silently never
retried. `ISSUES.md` #22 (two applications the user finished by hand never reaching the Submitted
column) is the same conflation seen from the other side.

**Likely consequence.** Silent loss of retry opportunities, invisible because the audit log says the
posting was "handled."

**Fix.** Separate *attempted* from *completed*. Either a status column consulted by the dedup query,
or two files. In the web app this maps to: **never let one table serve as both audit log and
workflow state.**

### 10.8 Stringly-typed navigation — **[Potential]**

**Evidence.** `showPage("Page1")`, `addPage("Details", …)`, and `stepLabels()` keyed by the same
literals ([App.java:69-99](../src/main/java/com/haadlit_sp/appRenderLogic/App.java#L69-L99)), with
`completedSteps()` returning a `Set<String>` of them.

**Why it matters.** Four coupled string tables with no compiler check. A typo compiles and produces
a page that silently never shows.

**Fix.** An enum with a label. ~20 lines, removes a class of bug entirely.
**[Trade-off note]:** with one developer and five pages, the realised cost so far is zero. It is
listed as *Potential* rather than *Actual* for that reason.

### 10.9 `ISSUES.md` tables are broken markdown — **[Actual]**

**Evidence.** Blank lines at `ISSUES.md:26`, `:31`, `:34`, `:40`, `:43` terminate the GFM table.
Rows 12–26 therefore render as literal pipe-delimited text, not a table.

**Why it matters.** More than half of the project's most-referenced document does not render. It is
also self-inflicted by the append pattern (adding a blank line before each new row).

**Fix.** Delete five blank lines.

### 10.10 Probe-established facts are not committed — **[Oversight]**

**Evidence.** `INDEED.md:123`: *"`SUBMITTED_CONFIRMATION_JS` … is verified against fake local pages
for both hits and non-hits (question, review, and captcha pages must not match)."* No such fixture
or test exists in the repository. Same for the badge-markup finding (#17) and the FlatLaf
whitespace-collapse behaviour.

**Why it matters.** These are claims about a past event, not properties of the codebase. The
confirmation regex is the sole basis for `SUBMITTED_VERIFIED` — the app's most important assertion —
and nothing prevents a future edit from breaking it.

**Fix.** Commit the fixture HTML under `src/test/resources/` and a test that runs the regex over
hits and non-hits. Rule: **any probe that establishes a fact you will rely on graduates to a
committed test.**

### 10.11 Housekeeping — **[Oversight]**

- **No `LICENSE`**, while bundling GeoNames data (CC BY 4.0 — attribution is handled in the UI per
  `EXTENDING.md:81`, so the obligation is met, but the project's own terms are unstated) and shipping
  llama.cpp (MIT) and Qwen3 (Apache 2.0).
- **`.idea/copilot.data.migration.*.xml` are tracked** while `.gitignore` excludes `.vscode/`
  entirely — inconsistent IDE policy, four files of pure noise.
- **No CI.** `gradlew test` runs only when someone remembers.
- **No format version on any TSV.** `SettingsStoreTest.garbledFileMeansDefaults` shows the *values*
  degrade gracefully; a *structural* change would not.

### 10.12 Reasonable trade-offs — explicitly not weaknesses — **[Trade-off]**

Listing these so they are not "fixed" by someone reading only the section above:

| Decision | Why it is fine |
|---|---|
| Manual sign-in + CDP attach | Measured, documented, and ethically motivated. The friction is the cost of not evading detection |
| Plain TSV over a database | Single-user, append-only, human-inspectable, zero dependency, trivially recoverable |
| 1.7B local model over an API | Documented reasoning, no key, no data egress, adequate for the task |
| Chrome profile holds a signed-in session | Explicitly acknowledged in `DECISIONS.md:116-118` as a knowing trade-off |
| `RuleBasedAnswerer` at 252 lines | Cohesive by change-reason; splitting would triple the cost of the most common change |
| `Page1`–`Page4` names | Ugly, zero realised cost at this scale |
| Broad `catch (Exception)` at worker boundaries | A crashed worker thread is worse than a swallowed exception in a desktop app that must keep running |

### 10.13 Already good, could be better — **[Improvement]**

- **`DECISIONS.md` entries could carry dates.** `INDEED.md` dates its claims; `DECISIONS.md` does
  not. A decision made under conditions that have since changed is invisible.
- **`ARCHITECTURE.md`'s package table lists class names**, so it drifts on every new class. Listing
  *responsibilities* would be more durable, with class names only where the name is load-bearing.
- **The probe/test split deserves its own document heading.** It is a genuine methodology buried
  mid-file.
- **`EXTENDING.md`'s Swing-traps list is excellent and mis-filed.** It is external-reality knowledge
  (how FlatLaf actually behaves) and belongs with `INDEED.md`-class material.

---

## Part 11 — What transfers, and what inverts

Your new project is a **multi-user web application** with authentication, accounts, and a database.
That changes several conclusions, and a few of them **reverse**.

### 11.1 Generally useful — take these unchanged

| Pattern | Why it transfers |
|---|---|
| **One boundary rule, stated in one sentence** | Universal. Add an automated import check this time |
| **One file per volatile external fact** | Becomes your integration contracts, one per third party, each with a *last verified* date |
| **Outcome enums over booleans/exceptions** | More valuable in a web app — HTTP status mapping, retry policy, and user messaging all key off the same taxonomy |
| **Safe default at every irreversible action** | Charges, emails, deletions, publishes, invites |
| **Layered defence around untrusted output** | Constrain structurally → validate → refuse known-unsafe cases → safe fallback |
| **The four-document system** | `ARCHITECTURE` / `DECISIONS` / `EXTENDING` / `DOMAIN`, plus `ISSUES.md` |
| **`ISSUES.md` with OPEN → FIXED? → VERIFIED** | The `FIXED?` state is the whole idea. Automate the promotion evidence this time |
| **Symptom separated from diagnosis** | The gap between them is where the learning is |
| **Comments that carry the measurement** | Cheapest high-value practice in the repo |
| **"Verify before claiming"** | Encode it in `CLAUDE.md` with your *own* four past mistakes, once you have them |
| **Constraints written as prohibitions before features** | Then make the architecture enforce them |
| **Domain-language test names** | The test list should read as a specification |
| **The app writes its own diagnosis** | See §11.4 for the web-app form |

### 11.2 Situational — right here, needs re-deciding there

| Pattern | Condition under which it applies |
|---|---|
| Facade over all engines | One UI process, few consumers. In a web app your route handlers are already the boundary; a god-facade behind them is a regression |
| Interface + factory at every seam | Only where no DI container exists. With one, this is duplication |
| UI-first development against a stubbed core | Excellent when the UX is the risk. Bad when the risk is the data model or a third-party integration — build a walking skeleton through the *riskiest* layer instead |
| Threading confined to one class | Maps to "background work goes in one named, owned queue," not to shared mutable fields |
| Scratch probes | Keep for third parties you cannot test hermetically. Replace for everything else with integration tests against a real DB in a container |
| Single markdown file per operation | Right for tens of runs a day. Wrong at thousands; becomes structured events + a derived digest |

### 11.3 The inversions — copying these would hurt

| This project | Your web app | Why |
|---|---|---|
| *"Server-shaped threat models (IDOR, session IDs, API endpoints) don't apply"* — `DECISIONS.md:120` | **They are now the primary threat model** | Authorisation on every **read**, not just every write. Tenancy isolation as a tested invariant. Session fixation, CSRF, rate limiting, enumeration. `docs/SECURITY.md` exists to replace this paragraph |
| **Fail-soft everywhere** — *"a store problem must never break a run"* | **Split the rule.** Telemetry fails soft. Domain data fails **loud**, in a transaction | Silently dropping a diagnostics row costs a screenshot. Silently dropping an order costs money and trust. The current rule applied to a payments table is a data-loss bug |
| Plain TSV, no schema, no version | Real schema, migrations, constraints, transactions | Append-only text works for one user who can open the file. It does not survive concurrent writers or a shape change |
| **Process-global mutable state** (`volatile boolean applying` as a run mutex) | **Per-user / per-request state** | With two users, every global is a bug. `applying` becomes a per-user job row with a status |
| Hand-rolled `XFactory.create()` | Framework DI | Re-implementing the container adds indirection with no seam |
| UI polls plain fields every 500 ms | Server state + explicit invalidation, or SSE/WebSocket | No shared memory across the network. Polling a REST endpoint every 500 ms per user is a load problem, not an architecture |
| *"Credentials are never seen or stored"* — true and easy here | You now **own** authentication | Password hashing, session lifetime, reset flows, MFA, lockout. This becomes a large chunk of `DECISIONS.md` |
| One user's data in `~/.appname/` | Tenancy is a correctness property | Every query needs a tenant predicate, and that needs a test that *fails* when it is missing |
| Diagnostics written to the user's disk, freely | PII in logs is a compliance issue | You need a logged-fields inventory and a redaction policy from day 1 |

### 11.4 The instrumentation pattern, translated

This is the most valuable thing to carry, so here is the explicit mapping rather than the analogy:

| Desktop | Web app |
|---|---|
| `ApplyJournal.step(phase, detail)` | Structured event per step, carrying a **correlation ID** for the whole user action |
| `sessions/<stamp>.md`, one per run | One trace per user operation, plus a durable summary row (`checkout_attempts`: outcome enum, step reached, duration) |
| `SessionLog.notes()` — derived prose | A scheduled job over yesterday's outcomes: failures grouped and ranked; "all N failed identically" flagged as environmental; unverified successes called out |
| Screenshot on failure | Request/response snapshot (redacted) + the state at failure |
| `issues.tsv` | Error tracker, with the outcome enum as the grouping key |
| Reading a session file after a run | A per-user activity view support can open, showing the same journal |

**Build the correlation ID and the outcome enum before your third feature.** In this project both
arrived on day 23, and everything learned about the actual problem was learned after they existed.

### 11.5 Do not replicate

- `Page1`–`Page4` and string-keyed navigation.
- A facade that accumulates orchestration (the `AppCore` shape, not the `AppCore` idea).
- Documentation written at the end.
- Committing anything containing real personal data, ever, including fixtures.
- Factories with one implementation and no named second one.
- One file serving as both audit log and workflow state.

---

## Part 12 — Lessons from this project

### Practices worth carrying forward

1. **State one boundary rule before the second package exists.** *"The UI never touches the browser,
   the model, or the disk."* One sentence, greppable, and it survived 38 commits.
2. **Give every volatile external fact exactly one file.** Including the *logic*, not just the
   constants.
3. **Write the constraints as prohibitions before writing features**, and make the architecture
   enforce them so violating one requires deleting code.
4. **Use an outcome enum wherever there are more than two endings**, and reuse those exact names in
   logs, metrics, and UI copy.
5. **Make the application write its own diagnosis.** Not raw logs — a summary the system interprets,
   with recurring failures grouped and environmental patterns called out.
6. **Record the counter-intuitive fact at the line it protects, with the number that proves it.**
7. **Separate symptom from diagnosis** in every issue record. The gap is the learning.
8. **Use a three-state fix ladder.** `FIXED?` is the state most trackers are missing.
9. **Measure the obvious approach before rejecting it**, and record the measurement.
10. **Separate the revisable decision from the non-negotiable principle**, and label which is which.
11. **Name tests in domain language**, so the test list reads as a specification of your constraints.
12. **Prefer a personal scaffold over greenfield.** Decisions made once and reused are decisions not
    re-litigated. Just re-examine them once, explicitly, when the real requirements are known.

### Mistakes and weaknesses worth avoiding

1. **Documentation on day 24.** *What happened:* 22 days of knowledge lived in one head and a chat
   history; `AppCore`'s Javadoc was never revisited and is still false. *What should have happened:*
   `ARCHITECTURE.md` and `DECISIONS.md` at commit 1, thin, growing one entry per decision.
   *Prevention:* the docs are part of the definition of done for a slice, not a phase after it.
2. **22 days before contact with reality.** *What happened:* every validated fact about the actual
   problem came from four days of running. *Prevention:* a walking skeleton that touches the riskiest
   external dependency **in week one**, even if it does nothing useful.
3. **The riskiest class has no tests.** *What happened:* eight production bugs in `ApplyWalkthrough`,
   all reproducible with a fake driver. *Prevention:* the rule is not "test everything" — it is
   **"the class that can do irreversible harm gets tests first."**
4. **The verification loop's last step was manual.** *Prevention:* if a step in your process is
   boring, unprompted, and skippable, it will be skipped. Automate the *evidence*; keep the judgment.
5. **Real personal data in the repository.** *Prevention:* synthetic fixtures from the first one; a
   pre-commit check.
6. **Dead scaffolding never removed.** *Prevention:* when a stub is replaced, deleting the stub is
   part of the same commit. A grep for `(stub)` in a code review would have caught all six.
7. **A documented invariant that the code violates.** *Prevention:* if a rule is worth writing down,
   it is worth a check that fails the build.
8. **Probe findings not committed.** *Prevention:* a probe that establishes a fact you rely on
   graduates into a test with a fixture, in the same session.

### Improvements for the new project

1. **Enforce the boundary rule automatically** — ArchUnit / `no-restricted-imports` / dependency-cruiser.
2. **CI from commit 1** — build, test, lint, plus a secret/PII scan. It is 20 lines and it removes an
   entire class of "someone forgot."
3. **Correlation ID and outcome enums before feature 3**, not after outage 1.
4. **`docs/SECURITY.md` and `docs/OPERATIONS.md` from day 1** — the two documents this project did
   not need and yours cannot do without.
5. **Date every `DECISIONS.md` entry**, so a decision made under conditions that have changed is
   visible.
6. **Automate the `FIXED?` → `VERIFIED` evidence.** Tag each issue with the failure signature it
   predicts; let the system report which predictions stopped occurring.
7. **A `docs/TESTING.md`** that states which layer proves what, and the rule that anything proved by
   a probe must graduate to a committed test.
8. **Split the fail-soft rule explicitly**, in writing: telemetry degrades, domain data does not.
9. **A tenancy-isolation test that fails when a query lacks its tenant predicate.** Make the
   invariant executable, not documented.
10. **Keep the scaffold.** Once this project's shape works, extract it into a template repo — the way
    `JavaAppBoilerplate` served this one, but this time with the docs, the CI, and the checks already
    in it.
