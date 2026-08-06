<!-- GUIDE: HOW to add or change things. Conventions plus recipes. A recipe is only useful if it
     names the FILE and the TEST — "add it to the answerer" is not a recipe; "add a matcher and a
     branch in answer(), plus a case in RuleBasedAnswererTest" is.

     Write a recipe the moment you establish a pattern someone (or an AI session) will repeat.
     Not before — speculative recipes are worse than none. -->

# Extending the app

Recipes for the changes this project actually needs, and the conventions that keep it maintainable.
See [ARCHITECTURE.md](ARCHITECTURE.md) for what each component is.

## Conventions

<!-- GUIDE: Short, imperative, each one traceable to a real problem. Keep to 5–8. If you have 15,
     most of them are style preferences and belong in the linter instead. -->

**Put third-party knowledge in one place.** Every endpoint, payload shape, and quirk for a provider
lives in `integrations/<provider>/`. If you are writing a URL or a response field name anywhere
else, it belongs there instead. The *behaviour* behind it goes in [DOMAIN.md](DOMAIN.md).

**Every query is tenant-scoped, at the repository layer.** Nothing above `<persistence>/` sees a
raw connection or an unscoped id. This is enforced by `<mechanism>` and tested by `<test>`.

**Outcomes are values, not booleans or exceptions.** Any operation with more than two endings
returns an outcome enum. Exceptions are for genuinely unexpected conditions only. Use the same
outcome names in logs, metrics labels, and UI copy.

**Domain data fails loud; telemetry fails soft.** See [DECISIONS.md](DECISIONS.md). No `catch`
around a domain write continues as if it succeeded.

**Verify against reality, don't assume.** Run it, query it, curl it, screenshot it. Before claiming
something works, produce the evidence. Say plainly when something is unverified.

**Comments explain why, not what** — and carry the number that proves it. A line where a
counter-intuitive constraint bit us, with the measurement, not narration of the code.

## Recipes

### Add an endpoint

1. Route in `<http>/<area>` — parsing and the required permission, nothing else.
2. A use case in `<usecases>/` returning an outcome enum.
3. The handler maps every outcome value to a status and a message. **Exhaustively** — if a new
   outcome is added later, this must not compile.
4. Tests: one per outcome, plus one asserting an unauthorised caller gets `<403/404>`.
5. Emit a structured event with the correlation ID.

### Add a background job

1. `jobs/<name>` — **idempotent**; running it twice on the same input must be safe.
2. Register the queue and the retry policy.
3. Record an outcome row on every terminal state, including failure.
4. Tests: happy path, a retry, and a duplicate delivery.

### Add a third-party integration

1. `integrations/<provider>/` — client, types, and a `contract.md` listing endpoints, payloads, auth,
   rate limits, and **the date each was last verified**.
2. Everything the provider does that surprised you goes in [DOMAIN.md](DOMAIN.md), dated.
3. Wrap its failures into your own outcome values at the boundary. Provider error shapes must not
   leak upward.
4. Tests against **recorded real responses**, not hand-written fixtures. Record them once from the
   provider's sandbox and commit them.

### Add a stored field

1. Migration — additive first. Backfill separately. Never a destructive migration in the same deploy
   as the code that needs it.
2. Add it to the PII inventory in [SECURITY.md](SECURITY.md) if it is personal data, and to the
   logged-fields policy if it could appear in a log.
3. Update the data table in [README.md](../README.md) if a user would ask about it.
4. Tests: round-trip, plus tenant scoping.

### Add a permission or role

1. `<authz module>` — the single place permissions are defined.
2. Every affected route declares it.
3. Tests: a user **without** it is refused. This is the test that matters; write it first.

### Add an irreversible action

1. Add it to the table in [DECISIONS.md](DECISIONS.md) with its safe default.
2. The default path is the reversible branch.
3. An audit row is written **before** the action, not after.
4. Tests: the default does not perform it; the explicit path does and audits it.

### Add instrumentation

1. Event name matches the outcome vocabulary.
2. Carries the correlation ID.
3. Contains no field on the never-log list in [SECURITY.md](SECURITY.md).
4. If it is a new failure mode, add it to the digest's grouping.

## Traps

<!-- GUIDE: The equivalent of a project's hard-won gotchas list. Add to it every time something
     behaves unexpectedly and you had to work it out. Each entry: the surprising behaviour, and the
     number or observation that proves it. This section becomes one of the most-read parts of your
     docs. Delete this placeholder once you have two real entries. -->

- <Trap discovered, what it looked like, and what is actually true. Include the measurement.>

## Testing

<!-- GUIDE: Which layer proves what. Split this into docs/TESTING.md once it exceeds ~40 lines. -->

| Layer | Proves | Runs |
|---|---|---|
| Unit | Domain decisions, validators, mappers | Every commit |
| Repository | Tenant scoping, constraints, migrations | Every commit, against a real DB in a container |
| Use case | Each outcome value is reachable and correct | Every commit |
| Contract | Our client matches the provider's recorded responses | Every commit |
| End-to-end | The journeys in [README.md](../README.md) still work | Every deploy |

**Name tests in domain language.** `unauthorisedTenantCannotReadAnotherTenantsOrders`, not
`testGetOrder2`. The test list should read as a specification of your constraints.

**Write the refusal test first** for anything security- or money-related. A test that something does
*not* happen is worth more than three that it does.

**Any fact established by a one-off script graduates into a committed test with a committed
fixture.** A script proves it once; a test proves it forever.

## Keeping the docs honest

When a change lands that affects behaviour:

1. Update the relevant document — [README.md](../README.md) for anything user-visible,
   [ARCHITECTURE.md](ARCHITECTURE.md) for structure, [DECISIONS.md](DECISIONS.md) if you decided
   something, [DOMAIN.md](DOMAIN.md) if reality surprised you, this file for a new pattern.
2. Add or update the [ISSUES.md](../ISSUES.md) row if it was a fix: symptom, diagnosis, fix, status.
   `OPEN` → `FIXED?` (shipped, unproven) → `VERIFIED` (reality showed it gone). **Only evidence
   promotes a row to VERIFIED.**
3. Delete the stub or the dead code your change orphaned, in the same commit.
