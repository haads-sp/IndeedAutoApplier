# Known issues & fixes — developer log

Field problems seen in real runs, what caused them, and whether the fix is confirmed.

**How this works with the runtime log:** every application that ends stuck/failed/challenged writes
a line to `~/.indeedapplier/diagnostics/issues.tsv` plus a screenshot of the page it died on
(same folder). After a test session: triage new lines into this table, fix, then on the next run
check whether the same stuck-point reappears — only then flip the status to VERIFIED.

Statuses: **OPEN** (not fixed) · **FIXED?** (fix shipped, not yet confirmed in a live run) ·
**VERIFIED** (confirmed fixed in a later run).

| # | First seen | Symptom | Diagnosis | Fix | Status |
|---|---|---|---|---|---|
| 1 | 2026-08-01 | Search only returned the first page of results (16) | Enumerator never paginated | Walk `&start=` pages until nothing new (cap 10), dedupe by job key | **VERIFIED** — 156 vs 16 live |
| 2 | 2026-08-01 | Phone country-code dropdown never filled ("all it had to do was click +1") | `<select>` fields were routed to the radio-clicking code path; the select verb also matched by value attribute, not visible label | Filler detects `select[name=…]` → `selectOption` by label with value fallback | **FIXED?** |
| 3 | 2026-08-01 | Stuck on skippable pages (work-experience job title / company) | Paused on any unanswerable "required" field without trying Continue; the form's required flags lie | Continue-anyway policy: fill what we can, click Continue, pause only if the form refuses to advance | **FIXED?** |
| 4 | 2026-08-01 | One employer-questions module answered NOTHING (all yes/no blank); not reproducible every time | Posting ran while the local AI was still loading; resume states no work-auth so rules couldn't cover; transport failures were also negative-cached for the whole session | Apply waits ≤90 s for AI readiness; negative cache now only stores model-said-unsure, never transport failures | **FIXED?** |
| 5 | 2026-08-01 | "Something went wrong / Our systems are having some trouble" screen appears often, run dies | Indeed's own sporadic smartapply error interstitial | Detect + click "Try again", redo the module (answers already banked) | **FIXED?** |
| 6 | 2026-08-01 | "You don't meet these employer requirements" stop screen ends the run | Advisory screen; has an "Apply anyway" button | Detect + click "Apply anyway", continue the flow | **FIXED?** |
| 7 | 2026-08-01 | Salary/pay expectation questions pause the run | Money questions were deliberately excluded from AI answering | Free-text pay questions answered "Negotiable" (rule); numeric pay fields still pause on purpose — inventing a figure is worse than asking | **FIXED?** |
| 8 | 2026-08-01 | Cloudflare "verify you are human" loops — clicking it just re-asks | Clearance can be refused while the debugger is attached; heavy automated volume escalates challenge frequency | App disconnects from Chrome on every challenge before asking the human; page-walk pacing jittered. Ritual: clear only after the app says it disconnected; if it still loops, close the Chrome window, reopen from Sign-in, cool down | **OPEN** (mitigated, not solved) |
| 9 | 2026-08-01 | Final Submit never clicked even in Auto modes | Review mode never submits by design; Auto path existed but the Submit button's exact text was never verified live (review step unreachable before AI answering) | Submit branch hardened (visible-captcha guard, post-click verification); selector pin still needs one live run that reaches the final step | **OPEN** (code ready, selector unverified) |
| 10 | 2026-08-01 | User must click "Apply to next" for every posting | Deliberate sequencing during development | Auto modes now run the whole found list hands-off with Stop button, pacing, per-run failure skip | **FIXED?** |
