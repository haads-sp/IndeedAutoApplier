<!-- GUIDE: What an AI session must know before touching anything. Copy to repo root.

     Order it by what breaks first. Environment problems waste a tool call every session; a wrong
     assumption about the stakes wastes a whole session and can cause real harm.

     Keep it SHORT and POINT AT the other documents rather than duplicating them — duplicated
     content drifts, and a long CLAUDE.md gets skimmed.

     The two highest-value sections are "What matters most here" and "Verify before claiming" with
     YOUR OWN past mistakes in it. Abstract instructions do far less than four concrete cases where
     the obvious answer turned out to be wrong in this specific codebase. -->

# Working on this project

<!-- GUIDE: One sentence. What the system is and what makes it non-trivial. -->

<A multi-user web app that …>

**Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) first** for the component map, then
[docs/EXTENDING.md](docs/EXTENDING.md) for conventions and recipes.
[docs/DECISIONS.md](docs/DECISIONS.md) explains why things are the way they are — check it before
"fixing" something that looks odd. [docs/DOMAIN.md](docs/DOMAIN.md) holds what we know is actually
true about the systems we depend on. [docs/SECURITY.md](docs/SECURITY.md) is required reading before
touching auth, tenancy, or personal data. [ISSUES.md](ISSUES.md) is the live record of what is
broken and what is confirmed.

## Environment

<!-- GUIDE: Exact commands, plus anything that does NOT work here. One line about a broken tool
     saves a failed call every session. -->

- <Shell / platform note, and anything that does not work in this environment.>
- `<install>` · `<dev>` · `<test>` · `<lint>` · `<migrate>`
- <Where a local database comes from, and how to reset it.>
- <Where logs and events go locally.>

## What matters most here

<!-- GUIDE: The stakes, in one paragraph, concretely. This is what makes an agent pick the
     conservative path when a situation is ambiguous — which is most situations. State the worst
     realistic consequence of getting it wrong, in plain terms. -->

**<This app handles real users' <money / personal data / communications>.>** <The worst realistic
consequence of a mistake, stated plainly.> When in doubt, the conservative path is the correct one:
<the specific conservative behaviour — e.g. "refuse and surface an error rather than proceeding on a
guess">.

**Multi-tenant means every query is scoped.** A missing tenant predicate is not a bug that shows up
in testing — it shows up as one customer seeing another customer's data. Before writing a query, read
the authorisation invariant in [docs/SECURITY.md](docs/SECURITY.md).

**Domain data fails loud; telemetry fails soft.** Never wrap a domain write in a `catch` that
continues as if it succeeded.

**Verify before claiming.**

<!-- GUIDE: Replace these with YOUR OWN cases as you accumulate them. Four specific past mistakes
     from this codebase are worth more than any general instruction, because they are evidence that
     the failure mode is real here. Until you have any, keep the general form and say so. -->

Run it, query it, curl it, screenshot it. Reasoning about what the code should do is not evidence.
Say plainly when something is unverified. Cases where the obvious answer was wrong here:

- <case>
- <case>

## The improvement loop

<!-- GUIDE: Where the evidence lives and what to do with it. This is what makes a session able to
     diagnose rather than guess. -->

Real usage produces evidence; the system records it. After a period of usage or an incident:

1. Read `<where the digest lives>` and the traces for the affected correlation ids — not the raw
   logs, and not an assumption about how the code behaves.
2. Diagnose from that evidence. Ask what the thing *was* before deciding what it wasn't: a negative
   check ("X is not ready") tells you nothing about why.
3. Fix, then update [ISSUES.md](ISSUES.md) — promote a row to **VERIFIED** only when real usage shows
   the symptom gone.
4. Update the docs when behaviour changes. If reality surprised you, that goes in
   [docs/DOMAIN.md](docs/DOMAIN.md), dated.

## Conventions worth stating up front

- **Outcomes are values, not booleans.** Any operation with more than two endings returns an outcome
  enum, and those exact names appear in logs, metrics, and UI copy.
- **Third-party knowledge lives in one place** — `integrations/<provider>/`. Never a raw endpoint or
  response field name anywhere else.
- **Delete the stub you replaced, in the same commit.**
- **Comments explain why, not what** — and carry the number that proves it.
- **Never commit real personal data**, including in test fixtures. Fixtures are synthetic.
