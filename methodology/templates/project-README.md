<!-- GUIDE: Copy to repo root as README.md. This is the USER-facing document (and the first thing
     a new developer or AI session reads). It answers: what is this, how do I run it, what will it
     refuse to do, where does my data live. Architecture goes in docs/ARCHITECTURE.md; reasoning
     goes in docs/DECISIONS.md. Keep this under ~120 lines. -->

# <Project name>

<!-- GUIDE: One paragraph, in the user's words, not the architecture's. What problem does this
     solve for whom. No stack names here — those go in "Running it". -->

<One-paragraph description: who it is for and what it does for them.>

## Running it

```bash
<install>          # install dependencies
<dev>              # start locally
<test>             # run the test suite
<lint>             # lint + format check
<migrate>          # apply database migrations
```

Requires <runtime version>, <database>, and <anything else>. See
[docs/OPERATIONS.md](docs/OPERATIONS.md) for environments and deployment.

## How it works, in one pass

<!-- GUIDE: The user journey as numbered steps. This should match your directory structure — if it
     doesn't, one of them is wrong (see PLAYBOOK Stage 3). -->

1. **Sign up** — <what happens, including what the user has to do themselves>
2. **<Configure / connect / import>** — <…>
3. **<The core thing>** — <…>
4. **<See results>** — <…>

### <Modes / plans / roles, if any>

<!-- GUIDE: Anything the user chooses that materially changes behaviour. State the safe default and
     say it is the default. -->

- **<Mode A>** — <what it does>. *Default.*
- **<Mode B>** — <what it does, and what it requires>.

## What it will not do

<!-- GUIDE: The prohibitions from PLAYBOOK Stage 1, in user-facing language. Keep this section —
     it is trust-building, and it is the thing that keeps the constraints alive in everyone's head.
     Every line here should trace to a structure in the code, not just an intention. -->

- <Never do X.> <What it does instead.>
- <Never do Y without explicit confirmation.>
- <Never store Z.>

## Your data

<!-- GUIDE: A table. Users and auditors both ask this, and having it written down forces you to
     actually know. Keep it in sync with docs/SECURITY.md's PII inventory — that one is the
     engineering source of truth; this one is its plain-language summary. -->

| What | Where it lives | Retention | Notes |
|---|---|---|---|
| Account (email, hashed password) | `users` | Until deletion | Passwords are hashed with <algorithm>, never stored or logged in plain text |
| <Domain data> | `<table>` | <policy> | <notes> |
| Session tokens | <store> | <lifetime> | <notes> |
| Activity log | `<table>` | <policy> | Used for support and abuse investigation |

<What deleting an account actually removes, and what it does not.>

## Documentation

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — the parts, the boundaries, and how a request flows.
- [docs/DECISIONS.md](docs/DECISIONS.md) — why it is built this way, and what was rejected.
- [docs/DOMAIN.md](docs/DOMAIN.md) — what we know is actually true about the systems we depend on.
- [docs/EXTENDING.md](docs/EXTENDING.md) — how to add or change behaviour.
- [docs/OPERATIONS.md](docs/OPERATIONS.md) — environments, deploy, rollback, on-call.
- [docs/SECURITY.md](docs/SECURITY.md) — the authn/authz model and data handling.
- [ISSUES.md](ISSUES.md) — known issues and whether their fixes are confirmed.
