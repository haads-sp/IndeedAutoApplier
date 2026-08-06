<!-- GUIDE: The live record of what is broken and whether fixes are proven. Copy to repo root.

     Two things make this document work, and both are easy to lose:

     1. SYMPTOM and DIAGNOSIS are separate columns. The symptom is what was observed; the diagnosis
        is what was actually true. The gap between them is the entire learning, and forcing yourself
        to fill both is what stops "fixed the thing that looked wrong".

     2. The THREE-STATE ladder. FIXED? — shipped but unproven — is the state most trackers lack, and
        its absence is why teams believe things are fixed that are not.

     WATCH OUT: do not put a blank line between rows. A blank line ends a Markdown table, and
     everything after it renders as literal text. This happened in the project this came from and
     half the tracker stopped rendering. -->

# Known issues & fixes — developer log

Real failures seen in production, what actually caused them, and whether the fix is confirmed.

**How this works with the runtime record:** every `<operation>` that ends in a failure outcome
writes an event with its correlation id. After a period of real usage: triage new failure signatures
into this table, fix, then check whether the same signature reappears — only then flip the status to
`VERIFIED`.

**Statuses:** **OPEN** (not fixed) · **FIXED?** (fix shipped, not yet confirmed in real usage) ·
**VERIFIED** (confirmed gone in real usage).

Qualified statuses are encouraged and useful: **OPEN** *(mitigated, not solved)* ·
**OPEN** *(instrumented, cause not yet proven)* — the second is a real and valuable state: *we don't
know why, and we have made the next occurrence explain itself.*

| # | First seen | Symptom | Diagnosis | Fix | Status |
|---|---|---|---|---|---|
| 1 | YYYY-MM-DD | <What was observed — in the words of whoever observed it> | <What was actually true. If it differs from the symptom, that difference is the point> | <What changed> | **FIXED?** |
<!-- predicts: outcome=<OUTCOME_ENUM> detail~"<failure string this fix should eliminate>" -->

---

## Promoting FIXED? → VERIFIED

<!-- GUIDE: This is the step that fails in practice. In the source project: 26 issues, 4 confirmed.
     The reason is structural — promotion requires a human to notice an ABSENCE, which is unpaid,
     unprompted, and skippable.

     The `predicts:` comment under each row exists so a script can do the noticing for you. -->

Each row carries a `<!-- predicts: … -->` comment naming the failure signature the fix should
eliminate. `<script/command>` reads recent outcomes and reports which predictions have stopped
occurring:

```
$ <command>
Row 1  predicted outcome=PAYMENT_DECLINED detail~"…"
       0 occurrences in the last 340 operations (7 days)  → candidate for VERIFIED
Row 4  predicted outcome=SYNC_FAILED detail~"…"
       12 occurrences in the last 340 operations          → still OPEN, not fixed
```

You still decide. You no longer have to notice.

**A row is only VERIFIED when:** real usage, in production, covering the affected path, with the
symptom absent. Not "the test passes." Not "I think so."

---

## Periodic review

**Last review:** YYYY-MM-DD

At each review:

1. Run the promotion check above.
2. Promote what the evidence supports.
3. For anything `FIXED?` for more than <n> weeks with no covering traffic, ask whether the path is
   actually being exercised — an unexercised fix is not a fix, it is an untested change.
4. For anything still `OPEN`, restate the diagnosis. If you cannot, it was never diagnosed.
