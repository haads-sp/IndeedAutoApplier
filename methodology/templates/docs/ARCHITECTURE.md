<!-- GUIDE: WHAT the parts are and HOW a request flows. Not WHY (→ DECISIONS.md) and not HOW TO ADD
     (→ EXTENDING.md). Write this at commit ~5, deliberately thin, and grow it. Target: under 150
     lines even at maturity. If it is longer, you are describing classes instead of boundaries. -->

# Architecture

What each part does and how a request flows through it. Read this before changing anything; read
[EXTENDING.md](EXTENDING.md) before adding anything.

## The one rule that shapes everything

<!-- GUIDE: ONE sentence. It must be falsifiable — you should be able to write a lint rule for it.
     Write this before the second directory exists. Examples:
       "HTTP handlers contain no business logic and no SQL — they parse, call a use case, and render."
       "No module outside `billing/` may construct a money value."
       "Every query is scoped by tenant at the repository layer; nothing above it may pass a raw id."
     Then add the check. An unenforced rule becomes false — that happened in the project this
     methodology came from. -->

**<The rule, in one sentence.>**

<Two or three sentences on what this buys you and what it prevents.>

**Enforced by:** `<the lint rule / ArchUnit test / dependency-cruiser config>`.

```
   <client / UI>
        │  <what crosses this boundary — verbs in, plain data out>
        ▼
   <HTTP layer>          routing, authn, request validation
        │
        ▼
   <use cases>           the operations your journeys need
        │
        ├── <domain>/        the decisions      ├── integrations/   third parties
        └── <persistence>/   the storage        └── jobs/           background work
```

## Directory map

<!-- GUIDE: The third column is the one that matters. "Contains" tells a reader where code is;
     "Constraint" tells them what they are not allowed to do. Prefer stating responsibilities over
     listing file names — file lists drift on every commit. -->

| Directory | Contains | **Constraint** |
|---|---|---|
| `<http>/` | Routing, request parsing, response shaping | No business logic, no SQL. Every route declares its required permission |
| `<usecases>/` | One module per journey operation | No framework types. Returns an outcome value, never throws for expected endings |
| `<domain>/` | The rules and the decisions | Pure. No I/O, no clock, no randomness — pass those in |
| `<persistence>/` | Repositories, migrations | **Every query is tenant-scoped here.** Nothing above this layer sees a raw connection |
| `integrations/<name>/` | One directory per third party | **The only place that party's endpoints, payloads, and quirks are written down.** See [DOMAIN.md](DOMAIN.md) for the behaviour behind them |
| `jobs/` | Background work | Idempotent. Every job is retry-safe and records an outcome |
| `observability/` | Events, correlation IDs, digests | Telemetry failures are swallowed; they must never fail a request |
| `<ui>/` | Presentation | No decisions. Reads state, sends verbs |

## How a request flows

<!-- GUIDE: One worked example, end to end, naming the real modules. Pick the most representative
     journey. Add a second only for a genuinely different shape (e.g. a webhook, a background job). -->

### <The main journey — e.g. "Submitting an order">

`<route>` → authn middleware resolves the user and tenant → `<UseCase>` →

1. <Step, and what it validates>
2. <Step, and where the decision is made>
3. <The irreversible action, and the gate in front of it>
4. Returns `<OutcomeEnum>`; the handler maps it to a status code and a message.

### Background work

<!-- GUIDE: A web app's version of "threading". Name the queue, who owns it, and the guarantees. -->

| Queue | Owns | Guarantees |
|---|---|---|
| `<name>` | <what it processes> | At-least-once; jobs are idempotent; failures retry <n> times with backoff, then land in `<dead letter>` |

**The rule:** background work is enqueued with a correlation ID, is idempotent, and records an
outcome. Nothing else in the codebase spawns concurrency.

## Outcome types

<!-- GUIDE: List every outcome enum and where its values are consumed. This is the shared vocabulary
     — the same names must appear in logs, metrics labels, error grouping, and UI copy. -->

| Operation | Outcomes | Consumed by |
|---|---|---|
| `<Operation>` | `<A>` · `<B>` · `<C>` · `<D>` | HTTP status mapping, metrics label `outcome`, the run digest, UI copy |

## State and storage

| Store | Holds | Failure policy |
|---|---|---|
| `<primary DB>` | Domain data | **Fail loud.** Transactional. A failed write is a failed request |
| `<cache>` | <what> | Fail soft — a miss is a slow path, never an error |
| `<object storage>` | <what> | <policy> |
| `<events / logs>` | Telemetry | **Fail soft.** Instrumentation must never break the thing it observes |

<!-- GUIDE: This split is essential and is the single biggest inversion from a single-user desktop
     app, where "fail soft everywhere" is correct. Here, fail-soft on domain data is a data-loss bug. -->

## What is recorded

- **Structured events** — one per meaningful step, carrying a correlation ID for the whole user
  action. See [EXTENDING.md](EXTENDING.md) for how to add one.
- **Operation summaries** — one durable row per `<operation>`: outcome, step reached, duration.
- **Derived digest** — `<schedule>`, grouping failures and flagging patterns. This is what you read
  after an incident, not raw logs.
- **[ISSUES.md](../ISSUES.md)** — the human tracker: symptom, diagnosis, fix, and whether reality
  has confirmed it.
