<!-- GUIDE: This document exists because of the single biggest inversion between a single-user
     desktop app and a multi-user web app.

     The source project's DECISIONS.md correctly said: "This is a single-user desktop app with no
     server, so server-shaped threat models (IDOR, session IDs, API endpoints) don't apply."
     For your project that sentence is exactly backwards. Those threat models are now primary.

     This is an ENGINEERING artefact, not a compliance checklist. Every claim in it should be
     testable, and the tests should exist. A security document nothing checks is worse than none,
     because it creates confidence without safety. -->

# Security model

How identity, authorisation, and data handling actually work here. Every invariant below names the
mechanism that enforces it and the test that proves it.

## Authentication

| Aspect | Decision | Enforced by |
|---|---|---|
| Credential storage | <algorithm, parameters> | `<module>` |
| Session mechanism | <cookie/JWT; flags; lifetime> | `<module>` |
| Session invalidation | <on password change, on logout, on role change> | `<module>` |
| Password reset | <single-use, expiring, invalidates sessions> | `<module>` |
| MFA | <status> | `<module>` |
| Lockout / throttling | <policy> | `<module>` |

**Never:** log a password, a token, a reset link, or a session id — including in error messages,
error trackers, and support tooling. See the never-log list below.

**Fail closed.** Every authentication check is a *positive* assertion of identity. Never infer
"authenticated" from the absence of something — an absent marker means a broken selector or a
missing header, not a valid user.

<!-- GUIDE: That last rule is directly transferred from the source project, which used a single
     positive check for sign-in state and documented why: "It is deliberately a single positive check
     so it fails closed. Never reintroduce 'signed in == the sign-in link is absent' — that reports
     success whenever the selector merely breaks." The same reasoning applies to every auth check
     you will write. -->

## Authorisation

**The model:** <RBAC / ABAC / ownership-based — state it in one sentence.>

**The invariant:**

> Every read and every write is scoped to the acting user's tenant, at the `<persistence>` layer.
> No layer above it may pass an unscoped identifier.

| Enforced by | Tested by |
|---|---|
| `<mechanism — e.g. a repository base class that requires a tenant context>` | `<test that FAILS when a query lacks its tenant predicate>` |

**Authorisation is on reads, not just writes.** The most common vulnerability in this shape of app is
an object reference that returns another tenant's data because only writes were checked.

**The test that matters:** for every resource, a test where user B requests user A's object and gets
`<404, not 403>`. Write it before the happy path.

<!-- GUIDE: Prefer 404 over 403 for cross-tenant access — 403 confirms the object exists, which is an
     enumeration oracle. If you choose 403 anyway, record why in DECISIONS.md. -->

## Data inventory

<!-- GUIDE: You cannot protect what you have not listed. This table is also what makes a deletion
     request answerable, and it is the thing you will most regret not having. Keep it in sync with
     the plain-language table in README.md — this one is the engineering source of truth. -->

| Data | Class | Stored where | Encrypted | Retention | Deleted on account deletion |
|---|---|---|---|---|---|
| Email | PII | `users.email` | At rest | Until deletion | Yes |
| Password hash | Credential | `users.password_hash` | At rest | Until deletion | Yes |
| <Domain data> | <class> | `<table>` | <…> | <…> | <yes / anonymised / retained because <reason>> |
| Session tokens | Credential | `<store>` | <…> | <lifetime> | Yes |
| Activity events | Operational + partial PII | `<store>` | <…> | <policy> | <…> |
| Backups | Mixed | `<store>` | <…> | <policy> | **After <n> days** — state this honestly |

## Logging policy

**Never logged, anywhere, in any form:**

- Passwords, tokens, session ids, reset links, API keys
- Full payment card numbers, CVVs
- <domain-specific sensitive fields>

**Logged in redacted form:** <e.g. email → first character + domain; card → last 4>.

**Enforced by:** `<redaction middleware / structured-logging schema / lint rule>`.
**Tested by:** `<a test that asserts a log line containing a sensitive field is rejected or redacted>`.

<!-- GUIDE: This section is the one that most often exists as an intention and not a mechanism. If
     there is no enforcement, say so plainly here rather than implying there is. -->

## Input and output

| Concern | Handling |
|---|---|
| Request validation | <schema validation at the boundary; reject unknown fields> |
| SQL | <parameterised only; the mechanism that makes raw SQL hard to write by accident> |
| Output encoding / XSS | <framework default; where it is bypassed and why> |
| CSRF | <mechanism, and which routes are exempt and why> |
| File uploads | <type allow-list, size cap, storage location, whether they are ever served from your origin> |
| Redirects | <allow-list; no open redirects> |
| Rate limiting | <per IP, per user, per endpoint — with the numbers> |

## Third parties

| Provider | Data shared | Purpose | Their access to our data |
|---|---|---|---|
| `<provider>` | <fields> | <why> | <none / scoped / full> |

**Rule.** No third party receives more than the operation requires. Where a provider's SDK collects
more than that by default, the disabling of it is recorded here.

## Known accepted risks

<!-- GUIDE: Be honest here. A security document that lists no accepted risks is either a very small
     system or an incomplete document. The source project did this well, stating plainly that its
     on-disk browser profile meant "anyone with filesystem access is effectively signed in — a
     tradeoff the user accepted knowingly." That honesty is the point. -->

| Risk | Why accepted | Mitigation | Revisit when |
|---|---|---|---|
| <…> | <…> | <…> | <condition> |

## Incidents

| Date | What | Blast radius | Resolution | Follow-up |
|---|---|---|---|---|
| | | | | |

<!-- GUIDE: An empty table is fine and honest. Filling it in during an incident is how the follow-up
     actually happens. -->

## Review checklist

Before merging anything that touches auth, tenancy, or personal data:

- [ ] Does every new query carry its tenant scope?
- [ ] Is there a test where the *wrong* user is refused?
- [ ] Does any new field belong in the data inventory above?
- [ ] Could any new log line contain something on the never-log list?
- [ ] Does any new error message leak whether a resource exists?
- [ ] Is a new third party receiving more than the operation requires?
- [ ] Does a new irreversible action write its audit row before acting?
