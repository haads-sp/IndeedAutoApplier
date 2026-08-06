<!-- GUIDE: The most valuable document you will write, and the cheapest to maintain — one entry at
     the moment you decide, then never touch it again. It exists to stop the same debate being had
     three times and to stop someone "fixing" something that was settled by measurement.

     Rules:
       · Date every entry.
       · Record the REJECTED alternative and the specific thing that killed it. This is the sentence
         that stops someone re-trying it in six months.
       · Separate the revisable DECISION from the non-negotiable PRINCIPLE ("the line that does not
         move"). Most ADR practice misses this and it matters.
       · Never delete an entry. Supersede it, in place, with a strikethrough and a date.
       · Do not record obvious choices. "We used the framework's router" is not a decision.
     -->

# Decisions and why

The choices that shape this codebase, and the evidence behind them. If a change seems to fight the
design, the reason is probably here.

**Entry format:** Decision · Why (with the measurement) · Rejected · The line that does not move.

---

## Irreversible actions and their safe defaults — YYYY-MM-DD

<!-- GUIDE: This should be your first entry, written before feature work (PLAYBOOK Stage 1). It is
     the entry that does the most design work for you. -->

**Decision.** These actions cannot be undone, and each defaults to its reversible branch:

| Action | Safe default | Irreversible branch requires |
|---|---|---|
| Charge a payment method | Authorise only, capture on confirmation | Explicit user confirmation, recorded with a timestamp |
| Send email to a user | Queue with a <n>-minute cancel window | — |
| Delete an account | Soft delete, <n>-day recovery window | Explicit typed confirmation |
| Publish publicly | Draft | Explicit action |
| Change permissions | — | Confirmation + audit row |

**Why.** The cost of a wrong irreversible action is unbounded and lands on someone who is not us.
The cost of an extra confirmation is one click.

**The line that does not move.** No code path performs an irreversible action as a side effect of
something else, and every one writes an audit row **before** it acts, not after.

---

## <Authentication approach> — YYYY-MM-DD

**Decision.** <What we do.>

**Why.** <The reasoning, with numbers or observations where you have them.>

**Rejected.** <The obvious alternative, and the *specific* thing that ruled it out.>

**The line that does not move.** <e.g. "Passwords are never logged, never in an error message, and
never in a support tool. Sessions are invalidated server-side on password change.">

---

## <Tenancy / data isolation model> — YYYY-MM-DD

**Decision.** <Row-level scoping at the repository layer / schema per tenant / separate databases.>

**Why.** <…>

**Rejected.** <…>

**The line that does not move.** <e.g. "No query reaches the database without a tenant predicate.
This is enforced by <mechanism> and tested by <test>, not by review.">

---

## <Data store choice> — YYYY-MM-DD

**Decision.** <…>

**Why.** <…>

**Rejected.** <…>

---

## <Background job strategy> — YYYY-MM-DD

**Decision.** <…>

**Why.** <…>

**Rejected.** <…>

**The line that does not move.** <e.g. "Every job is idempotent. At-least-once delivery is assumed,
never worked around.">

---

## Failure policy: fail loud vs. fail soft — YYYY-MM-DD

<!-- GUIDE: Include this entry. It is the single most common place a habit carried from smaller
     projects becomes a data-loss bug. -->

**Decision.** Two policies, applied by data class:

- **Domain data fails loud.** A failed write is a failed request, inside a transaction, surfaced to
  the caller. Never swallowed, never best-effort.
- **Telemetry fails soft.** Events, metrics, and audit-adjacent logging degrade silently rather than
  breaking the operation they observe.

**Why.** Silently dropping an event costs a line in a digest. Silently dropping an order costs money
and trust. A blanket "never let storage break the flow" rule is correct for local diagnostics and is
a data-loss bug for domain writes.

**The line that does not move.** No `catch` around a domain write may continue as if it succeeded.

---

<!-- GUIDE: Superseding an entry — keep the original, mark it, and link forward:

## <Old decision> — 2026-08-01

~~**Superseded 2027-01-14** by "<New decision>" — <one line on what changed in the world>.~~

<original text left intact below>
-->
