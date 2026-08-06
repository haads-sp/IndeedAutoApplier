# The documentation system

Drop-in starters for a new multi-user web application. Copy `project-README.md` → `README.md`,
`CLAUDE.md` → repo root, `ISSUES.md` → repo root, and `docs/` → `docs/`.

Each template contains `<!-- GUIDE: … -->` comments telling you what belongs and what does not.
**Delete the guide comments once the section has real content** — a template that still looks like a
template three months in is a template nobody is filling in.

---

## The eight documents

| File | Purpose | Belongs inside | Does **not** belong | Update when |
|---|---|---|---|---|
| **`README.md`** | What this is, how to run it, what it will not do, where data lives | Setup commands, the one-pass explanation, the prohibitions, a data-location table | Architecture, rationale, troubleshooting | User-visible behaviour changes |
| **`docs/ARCHITECTURE.md`** | What the parts are and how a request flows | The boundary rule, a diagram, a directory table with a **constraint** column, request flow, background-job model | *Why* (→ DECISIONS), *how to add* (→ EXTENDING) | A component, boundary, or flow changes |
| **`docs/DECISIONS.md`** | Why it is like this | Dated entries: decision · why (with the measurement) · rejected alternative · the line that does not move | Anything not actually decided; obvious choices | Immediately when you decide something non-obvious. **Never delete an entry** — supersede it |
| **`docs/EXTENDING.md`** | How to add or change things | Conventions, recipes naming file + test, accumulated traps | Component descriptions, rationale | You establish a pattern someone will repeat |
| **`docs/DOMAIN.md`** | What is actually true about the outside world | Third-party behaviour you learned the hard way, business rules from outside engineering, each with a **verified-on date** | Your own code's behaviour; anything you assumed rather than observed | You discover reality differs from the docs. Re-verify on a schedule |
| **`docs/OPERATIONS.md`** | How it runs in production | Environments, secrets, migrations, deploy, rollback, on-call first moves, backup/restore | Application design | Deploy, env, or runbook changes |
| **`docs/SECURITY.md`** | The security model as an engineering artefact | Authn/authz model, tenancy boundary, PII inventory, logged-fields policy, threat notes, known accepted risks | Vague assurances; a compliance checklist you don't act on | Auth changes, a new data class is stored, a new integration |
| **`ISSUES.md`** | What is broken and whether the fix is proven | Table: # · first seen · symptom · diagnosis · fix · status (`OPEN`→`FIXED?`→`VERIFIED`) | Feature requests, roadmap | A real failure is seen; a fix ships; reality confirms it |
| **`CLAUDE.md`** | What an AI session must know before touching anything | Environment + commands, pointers to the docs, the stakes, verify-before-claiming with *your* examples, the improvement loop | Anything duplicated from the other docs | Environment, workflow, or stakes change |

---

## Deliberately not included, and why

| Document | Why not |
|---|---|
| `CONTRIBUTING.md` | Meaningless below ~3 contributors. `EXTENDING.md` already carries the conventions. Add it when someone outside the team sends a PR |
| `CHANGELOG.md` | Add it when you have releases users track. Before then it duplicates git and rots |
| `PROJECT_PLAN.md` / `ROADMAP.md` | Plan documents die within weeks and then actively mislead. `ISSUES.md` plus your issue tracker is the living record. If you need a roadmap, it belongs where it's read — the tracker, not the repo |
| `API.md` | Generate it from the code (OpenAPI, typedoc). A hand-maintained API doc is wrong within a month |
| `TESTING.md` | Only once your test strategy has more than one layer worth explaining. Until then it's a section of `EXTENDING.md`. Split it out when that section exceeds ~40 lines |

---

## Rules for keeping this honest

1. **One fact, one home.** If it's in two documents, one will be wrong within a month. Cross-link
   instead of copying. The single exception is a *safety prohibition*, which should be unmissable
   from any entry point — but even then, keep the wording identical.

2. **Date anything perishable.** `DOMAIN.md` entries and `DECISIONS.md` entries both carry dates.
   A decision made under conditions that have since changed is invisible without one.

3. **A rule you write down should be a rule something checks.** IndeedAutoApplier's
   `ARCHITECTURE.md` states a threading invariant its code violates, because nothing enforced it. If
   you can't check it, write it as guidance, not as a rule.

4. **Documentation is part of "done" for a slice, not a phase after it.** IndeedAutoApplier wrote
   everything on day 24 of 26; the docs are excellent and the code still carries a class comment
   describing a system that stopped existing 20 days earlier.

5. **Never delete a `DECISIONS.md` entry.** Supersede it:
   `~~Superseded 2027-01-14 by "…" — <one line on what changed>~~`. The record of a decision you
   reversed is more valuable than the decision itself.

6. **Write the rejected alternative.** The most useful sentence in a decision record is the one that
   stops someone re-trying the obvious thing in six months.
