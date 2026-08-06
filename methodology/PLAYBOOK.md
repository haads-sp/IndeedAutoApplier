# The Playbook

**"If I started a new project tomorrow, what exact process should I follow?"**

Derived from [ANALYSIS.md](ANALYSIS.md), corrected for what went wrong. Targeted at a solo or small
team building a multi-user web application with AI assistance.

This is not the sequence IndeedAutoApplier followed. It is the sequence that project's evidence
argues for — the two differences that matter are **documentation from commit 1** and **contact with
reality in week 1**, both of which cost that project real time.

---

## The six rules that govern everything

Everything below is machinery for these.

> **1. One boundary rule, stated in one sentence, enforced by a check.**
> If you cannot say it in a sentence, you don't have an architecture — you have folders.

> **2. Every volatile external fact gets exactly one file, with a *last verified* date.**
> Anything that changes on someone else's schedule.

> **3. Constraints are written as prohibitions before features are written as stories.**
> Then the architecture makes violating one require deleting code.

> **4. The system writes its own diagnosis.**
> If you learned about a bug from a human, your instrumentation has a gap. Fix that too.

> **5. Nothing is "fixed" until evidence from reality says so.**
> `FIXED?` is a real state and most of your fixes live there.

> **6. Verify before claiming.**
> Run it, screenshot it, query it. Say plainly when something is unverified.

---

## Stage 0 — Scaffold (hours, not days)

**Do.** Start from a template repo, not an empty directory. If you don't have one, this project *is*
the exercise that produces it — extract it at the end.

The scaffold carries, already working:

- Build, run, test, and format commands — one line each in the README.
- CI running build + test + lint + a secret/PII scan on every push.
- The boundary check (ArchUnit, `no-restricted-imports`, or dependency-cruiser) — wired up and
  passing against an empty ruleset, so adding a rule later is one line.
- `.gitignore` covering every IDE you use, consistently. Plus `*.pdf`, `*.csv`, `*.xlsx` outside a
  designated fixtures directory.
- The seven documents from [templates/](templates/), stubbed.
- `LICENSE`.

**Why.** IndeedAutoApplier's foundation came from `JavaAppBoilerplate`, and that is the single
biggest reason its structure held for 38 commits. It also inherited that scaffold's problems
(`Page1`–`Page4`, tracked `.idea` files) untouched — so the scaffold is worth investing in, and
worth re-examining once.

**Output.** A repo that builds, tests, and deploys nothing, on CI, green.

**Mistakes to avoid.** Don't add CI "later" — you never will, and once the suite is red on main
nobody adds it. Don't skip the boundary check because you have no rules yet; adding the *mechanism*
costs 15 minutes now and never happens later.

**Gate.** `git push` produces a green CI run.

---

## Stage 1 — Problem, constraints, and the irreversible list (half a day)

**Do.** Write three things, in this order:

1. **The problem, in one paragraph**, in the user's words, in `README.md`.
2. **The prohibitions** — what this system must never do. Phrase them as "never," not "should."
   IndeedAutoApplier's are *never claim a credential the resume doesn't support · never solve a
   captcha · never auto-submit by default*, and they show up in four documents and three code
   structures.
3. **The irreversible-action list.** Every action that spends money, sends a message, changes
   someone else's state, or cannot be undone. For a web app that is at minimum: charge a card, send
   an email, delete an account or its data, publish something public, invite a user, change
   permissions.

For each irreversible action, decide the safe default **now**. The safe default is the reversible
branch; the irreversible branch requires an explicit choice, and that choice gets recorded.

**Why.** These are the constraints that survive every refactor. `CLAUDE.md`'s *"A wrong answer is not
a failing test, it is a false statement to an employer"* did more design work than any diagram — once
written, `SubmitMode.REVIEW` as default, the confirmation-verification step, and the conservative
answering rules all follow mechanically.

**Output.** `README.md` §What this will not do. `docs/DECISIONS.md` entry #1: the irreversible list
and each one's safe default.

**Mistakes.** Writing constraints as aspirations ("we should validate input"). A constraint you
cannot test a violation of is a slogan.

**Gate.** For every prohibition, you can name the code structure that will enforce it.

---

## Stage 2 — Walking skeleton through the riskiest thing (week 1)

**Do.** Build the thinnest possible end-to-end path that touches your **highest-risk external
dependency**, and run it against the real thing.

For a multi-user web app, that is almost always one of: the auth provider, the payment provider, or
the database under real concurrency. Not the UI.

The skeleton does nothing useful. It proves: a request arrives → authenticates a real user → writes
one row → reads it back scoped to that user → renders → deploys to a real environment.

**Why — this is the biggest correction to what IndeedAutoApplier did.** It spent 22 days building
before meeting the live site, and *every validated fact about the actual problem came from the four
days after*. Twenty-six `ISSUES.md` rows, all dated after first contact. The construction phase
produced zero knowledge about the problem, because it was building against assumptions.

You are guaranteed to be wrong about your external dependencies in ways you cannot predict. Find out
in week 1, when the cost of being wrong is a rewrite of 200 lines.

**Output.** A deployed skeleton. And your first `docs/DOMAIN.md` entries — the things that turned out
not to work the way the docs said.

**Mistakes.** Building the UI first because it's visible and satisfying. IndeedAutoApplier did this
and got away with it because the UX genuinely was a risk; if your risk is the data model or a
third-party integration, UI-first defers the only learning that matters.

**Gate.** The skeleton runs in a real deployed environment against real third parties, and
`docs/DOMAIN.md` has at least one entry you did not expect to write.

---

## Stage 3 — Decompose (one sitting)

**Do.** Derive the structure from the **user journey**, not from layer names.

```
IDEA
  ▼
CONSTRAINTS        the prohibitions from Stage 1
  ▼
USER JOURNEYS      "sign up → verify → configure → do the thing → see results"
  ▼                 each journey step names a system
SYSTEMS            accounts · billing · <your domain> · notifications · admin
  ▼                 each = one thing the journey needs, named as a domain, not a layer
COMPONENTS         split by VERB within a system: read / decide / write / report
  ▼
MODULES            one responsibility, stated in the first line of its doc comment
  ▼
TASKS              one slice = one journey step working end to end
```

**The splitting rule — the only one you need:**

> **Split when the pieces have different reasons to change. Not when a file gets long.**

Test it by naming the change that would touch each piece independently. If you can't name two
different changes, it's one module. IndeedAutoApplier split `ApplyFormReader` / `ApplyFiller` /
`ApplyWalkthrough` because they change on three independent clocks (Indeed's DOM, new input types,
flow changes) — and correctly left `RuleBasedAnswerer` at 252 lines, because every one of its rules
changes for the same reason.

**Naming.** Packages/directories are **domains or verbs**, never layers. `accounts`, `billing`,
`onboarding` — not `services`, `managers`, `helpers`, `utils`. The absence of a `utils/` directory
is a design achievement, not an oversight: `utils/` is where code goes when nothing owns it, and
code that nothing owns is code nobody maintains.

**Output.** The directory tree, with a one-line responsibility per directory, in
`docs/ARCHITECTURE.md`.

**Mistakes.** Decomposing by technical layer (`controllers/`, `models/`, `views/`) — it scatters
every real change across three directories. Creating a module because a file *might* get big.

**Gate.** For five plausible upcoming changes, you can name the single directory each lands in.

---

## Stage 4 — Architecture, thin and dated (one sitting, then continuous)

**Do.** Write `docs/ARCHITECTURE.md` and `docs/DECISIONS.md` **now**, at commit ~5, deliberately thin.

`ARCHITECTURE.md` needs exactly four things on day 1:

1. **The one boundary rule**, as a sentence, at the top.
2. An ASCII diagram of the layers and the direction data flows.
3. A table: directory → what it contains → **the constraint that applies to it**. That third column
   is the one that matters — *"all fail-soft"*, *"the only place third-party markup is written
   down"*. A contents column tells you where code is; a constraint column tells you what you may not
   do.
4. How one request flows through, end to end.

`DECISIONS.md` gets **one entry per decision, dated, as you make it**. Format:

```markdown
## <Decision>            (YYYY-MM-DD)

**Decision.** What we do.
**Why.** The measurement or observation. Numbers if you have them.
**Rejected.** The obvious alternative, and the specific thing that killed it.
**The line that does not move.** The non-negotiable principle, if any.
```

The *Rejected* line is what stops someone re-trying it in six months. The *line that does not move*
separates the revisable decision from the principle — a distinction most ADR practice lacks.

**Why.** IndeedAutoApplier wrote both on day 24. The documents are excellent because they distil
real measurement — but the cost is visible: `AppCore`'s class comment still describes a skeleton
from day 0, and `ARCHITECTURE.md:97` states a threading rule the code violates, because the rule was
written after the code and never checked against it.

Thin-and-growing gets both: the discipline of writing decisions down while the reasoning is fresh,
without pretending to know things you can only learn by running.

**Output.** Two short files. `ARCHITECTURE.md` under 100 lines. `DECISIONS.md` with 3–5 entries.

**Mistakes.** Big design up front — a 2,000-word architecture document at commit 5 is fiction, and
you will not update fiction. Writing decisions without the rejected alternative.

**Gate.** Someone who has never seen the repo can state the boundary rule after two minutes of
reading.

---

## Stage 5 — The slice loop (the main loop)

This is where most of the project happens. One slice = one journey step working end to end.

```
   ┌──────────────────────────────────────────────────────────┐
   │  1. PICK       one journey step, vertical through         │
   │                every layer, ending runnable               │
   │                                                            │
   │  2. WRITE      the test that defines "done" — in domain    │
   │                language. For anything irreversible or      │
   │                security-relevant, write it first           │
   │                                                            │
   │  3. BUILD      smallest code that passes. Outcome enums    │
   │                for anything with >2 endings                │
   │                                                            │
   │  4. INSTRUMENT structured events for each meaningful step, │
   │                carrying the correlation ID                 │
   │                                                            │
   │  5. VERIFY     run it. In the real environment where that  │
   │                is possible. Screenshot / query / curl.     │
   │                Never claim it works from reading the diff  │
   │                                                            │
   │  6. RECORD     DECISIONS.md if you decided something.      │
   │                DOMAIN.md if reality surprised you.         │
   │                EXTENDING.md if you established a pattern   │
   │                someone will need to repeat                 │
   │                                                            │
   │  7. CLEAN      delete the stub you replaced, in this same  │
   │                commit. Remove imports your change orphaned │
   └──────────────────────────────────────────────────────────┘
```

### Rules inside the loop

**Every slice is vertical.** It touches storage, logic, and presentation, and ends with something
runnable. IndeedAutoApplier's commits are exactly this shape — `9d2a081 "added the ability to find
and list potential jobs"` touched search, browser, model, and UI in one commit.

**Instrument before you need it.** Per-step structured events with a correlation ID, and an outcome
enum for every operation that can end more than two ways. IndeedAutoApplier added these on day 23,
and *everything it learned about its actual problem was learned after they existed*. Build them by
slice three.

**Outcome enums, not booleans.** The moment an operation has three endings, name them. Then use
those exact names in your logs, your metrics labels, your error tracker's grouping key, and your UI
copy. One vocabulary end to end is what makes "it said `PAYMENT_DECLINED` four times" a one-hop
investigation.

**Step 7 is the one that gets skipped.** IndeedAutoApplier still carries six dead stub methods and
four unreachable classes from its day-0 skeleton, plus a class comment that describes a system that
stopped existing 20 days later. Deleting the stub belongs in the commit that replaces it — a week
later it looks like it might be load-bearing.

**Output per slice.** A commit that builds, tests green, deployed, with docs updated if anything
changed.

**Gate.** Tests pass, CI green, and you have *run it* — not read it.

---

## Stage 6 — The improvement loop (once real users exist)

The moment the system is used by someone who is not you, this replaces Stage 5 as the primary loop.

```
  1. THE SYSTEM REPORTS
     Structured events → a durable summary row per operation → a derived digest.
     If a user told you about it before your system did, that's a second bug: fix
     the instrumentation gap in the same session.

  2. READ THE EVIDENCE
     The trace, the summary, the digest. Not your memory of how the code works.

  3. IDENTIFY BEFORE YOU BLAME
     Ask what the thing WAS before deciding what it wasn't.
     IndeedAutoApplier's three worst bugs were all this failure:
       · 8 "load failures" were third-party challenge pages
       · a "captcha" was an invisible widget passing a layout check
       · an "empty page" was a page still rendering
     Generalisation: a negative check ("X is not ready") tells you nothing about
     why. Always pair it with positive identification, and check the positive FIRST.

  4. DIAGNOSE IN ONE SENTENCE
     Distinct from the symptom. If you can't, you haven't diagnosed it.

  5. FIX
     One file if the architecture allows. If it doesn't, note that in the issue row —
     it is a design signal, and it is how you find your next refactor.

  6. TEST + COMMENT
     The test that would have caught it. Plus a comment at the line stating the
     counter-intuitive fact AND the number that proves it.

  7. RECORD
     ISSUES.md row: # | first seen | symptom | diagnosis | fix | status.
     Status starts at FIXED?.

  8. CONFIRM  ← the step that fails
     FIXED? → VERIFIED only when reality shows the symptom gone.
```

### Making step 8 actually happen

This is the step IndeedAutoApplier documented, understood, and did not do: 26 issues, 4 confirmed.
The reason is structural — promotion requires a human to read a log and notice an *absence*, which
is unpaid, unprompted, and skippable.

**Automate the evidence, keep the judgment.** Tag each issue row with the failure signature it
predicts:

```markdown
| 13 | 2026-08-03 | 8 postings "step did not finish loading" | … | … | FIXED? |
<!-- predicts: outcome=FAILED detail~"did not finish loading" -->
```

Then a small scheduled job reads recent outcomes and reports: *"Rows 3, 5, 13 predicted failures
that have not occurred in the last 40 operations. Candidates for VERIFIED."* You still decide; you
no longer have to notice.

**Gate for VERIFIED.** Real usage, in the real environment, covering the path, with the symptom
absent. Not "the test passes." Not "I think so."

---

## Stage 7 — Maintenance and extension

**Adding a feature to a mature system.** Work through, in order:

1. Which journey step does this belong to? → that's the directory.
2. Does it touch a volatile external fact? → that goes in the integration contract, nowhere else.
3. Does it have more than two endings? → outcome enum, and add the new values to every exhaustive
   switch (your compiler should force this — if it doesn't, your language needs a lint rule).
4. Is it irreversible? → safe default, explicit opt-in, recorded.
5. Is there an `EXTENDING.md` recipe? → follow it. If there isn't and you just invented a pattern
   someone will repeat, write one.
6. Does it change behaviour a user sees? → `README.md`.
7. Did you decide something non-obvious? → `DECISIONS.md`, dated.

**When to refactor.** Three signals, in order of reliability:

- **A fix couldn't be one file.** Recorded in the issue row. This is the strongest signal you have.
- **A file has three reasons to change.** Not "a file is long" — `IndeedSelectors` is 370 lines and
  correct; `AppCore` is 719 and has three.
- **You can't construct it in a test without starting infrastructure.** That is coupling, measured.

**When not to refactor.** Because it's long. Because it isn't how you'd write it. Because a pattern
is unfamiliar. IndeedAutoApplier's `DECISIONS.md` exists precisely to stop this: *"If a change seems
to fight the design, the reason is probably here."*

---

## Working with AI assistants

Every practice above helps a human and helps an AI session more, because a session starts with no
memory. IndeedAutoApplier's `CLAUDE.md` is a good model; here is what makes it work.

**`CLAUDE.md` should be short and ordered by what breaks first:**

1. **Environment** — the exact commands, and any that *don't work here*. IndeedAutoApplier's says
   *"Use PowerShell. The Bash tool does not work in this environment."* One line, saves a failed
   tool call every session.
2. **Pointers, not content** — "read `docs/ARCHITECTURE.md` first, then `EXTENDING.md`." Don't
   duplicate; duplication drifts.
3. **What matters most here** — the stakes, in one paragraph. This is what makes an agent choose the
   conservative path when it's ambiguous.
4. **Verify before claiming — with your own examples.** IndeedAutoApplier lists four specific past
   mistakes. That is far more effective than an abstract instruction, because it is evidence that
   the failure mode is real *in this codebase*.
5. **The improvement loop**, so a session knows where the evidence lives.

**Give the model evidence, not descriptions.** The reason IndeedAutoApplier's later sessions were
productive is that a session could read `sessions/<stamp>.md` and see the actual step-by-step of a
failed run. An agent reasoning from a diff guesses; an agent reading a trace diagnoses.

**Every decision recorded is a decision not re-litigated.** Without `DECISIONS.md`, each session
re-derives — or worse, "fixes" — the Cloudflare approach, the conservative-answer rule, the review
default. `DECISIONS.md` is as much an AI-context document as a human one.

**Land patterns in `EXTENDING.md` as recipes that name the file and the test.** *"Add a rule for a
screener question → `answer/RuleBasedAnswerer`, add a matcher and a branch in `answer()`, plus tests
in `RuleBasedAnswererTest`."* That is a complete, unambiguous instruction for a session that has
never seen the repo.

**Ask for verification, and check it.** "Run it and show me the output" is the whole practice. The
four documented cases where this project's assumptions were wrong — Cloudflare pages read as load
failures, an invisible captcha, HTML spacing that only collapses under the real theme, a session log
overwriting itself — were all cases where the reasoning was plausible and the reality was different.

---

## Stage sequence at a glance

| Stage | Duration | Output | Gate |
|---|---|---|---|
| 0 · Scaffold | Hours | Repo building on green CI | `git push` → green |
| 1 · Constraints | Half a day | Prohibitions + irreversible list | Each prohibition has a named enforcer |
| 2 · Walking skeleton | Week 1 | Deployed, touching real third parties | ≥1 surprising `DOMAIN.md` entry |
| 3 · Decompose | One sitting | Directory tree + responsibilities | 5 changes → 5 obvious homes |
| 4 · Architecture | One sitting, then continuous | Thin `ARCHITECTURE` + dated `DECISIONS` | Boundary rule stateable in 2 min |
| 5 · Slice loop | Most of the project | Vertical slices, instrumented | Tests green + you ran it |
| 6 · Improvement loop | Once users exist | Issues diagnosed from evidence | `VERIFIED` requires reality |
| 7 · Maintain | Ongoing | Recipes, refactors on signal | Fix fits in one file |

---

## The anti-pattern checklist

Read this before you commit. Every item is a mistake this project actually made.

- [ ] Am I claiming this works without having run it?
- [ ] Did I replace a stub and leave the stub?
- [ ] Does a comment or doc now say something false because of this change?
- [ ] Is there real personal data in anything I'm about to commit?
- [ ] Does this operation have three endings and a boolean return type?
- [ ] Did I put a third-party selector/endpoint/format anywhere but its one file?
- [ ] Is this an irreversible action whose default is the irreversible branch?
- [ ] Did I write a rule in a document that nothing checks?
- [ ] Am I marking something fixed on belief rather than evidence?
- [ ] Did I add an abstraction whose second implementation I can't name?
- [ ] Is one table/file now serving as both audit log and workflow state?
- [ ] Is a global holding state that should be per-user?
