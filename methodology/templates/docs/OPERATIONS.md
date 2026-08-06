<!-- GUIDE: New for a web app — the desktop project this methodology came from did not need it
     ("deployment" was `gradlew run`), and its absence would be fatal here.

     This is a RUNBOOK, written for you at 3am when something is broken and you are not thinking
     clearly. Optimise for "can be followed while panicking": exact commands, no prose, no
     ambiguity about what is safe.

     Write the rollback section BEFORE your first deploy. -->

# Operations

How this runs in production, and what to do when it doesn't.

## Environments

| Environment | URL | Database | Deploys from | Data |
|---|---|---|---|---|
| Local | `localhost:<port>` | Container | — | Seeded, synthetic |
| Staging | `<url>` | `<instance>` | `main`, automatically | **Synthetic only** — never a copy of production |
| Production | `<url>` | `<instance>` | Tagged release, manual approval | Real |

<!-- GUIDE: "Never a copy of production" is worth stating explicitly. Copying production data to
     staging is the most common way personal data ends up somewhere it should not be. If you must,
     document the anonymisation step here and make it a script, not a habit. -->

## Secrets

| Secret | Where it lives | Rotation | Who can read it |
|---|---|---|---|
| `<NAME>` | `<manager>` | <cadence> | <role> |

**Rules.** Secrets are never in the repo, never in an environment file that is committed, never in a
log, never in an error message shown to a user. A leaked secret is rotated first and investigated
second.

**If a secret leaks:** 1. rotate it · 2. invalidate anything issued with it · 3. check access logs
for the exposure window · 4. record it in [SECURITY.md](SECURITY.md) under incidents.

## Deploy

```bash
<command>          # 1. build and test
<command>          # 2. apply migrations  (see the rule below)
<command>          # 3. deploy
<command>          # 4. verify
```

**Migration rule.** Migrations are **additive first** and deploy **separately from** the code that
depends on them:

1. Deploy the migration that adds the column/table. Old code ignores it.
2. Deploy the code that writes it.
3. Backfill.
4. Deploy the code that reads it.
5. Only later, and separately, remove anything.

A destructive migration in the same deploy as the code that needs it is unrollbackable. This is the
rule that makes step-by-step deploys feel slow and makes 3am survivable.

## Rollback

<!-- GUIDE: Write this before your first production deploy, and TEST it once, deliberately, on a
     quiet day. An untested rollback procedure is a hope. -->

```bash
<command>          # revert to the previous release
```

**Time to roll back:** <n> minutes.

**What rollback does not fix:** applied migrations, sent emails, captured payments, and anything a
third party has already acted on. For each of these, the recovery is <procedure>.

**Last rollback rehearsal:** YYYY-MM-DD.

## Monitoring

| Signal | Where | Alerts when | Goes to |
|---|---|---|---|
| Error rate | `<tool>` | <threshold> | <who> |
| `<operation>` outcome distribution | `<tool>` | <e.g. "`PAYMENT_DECLINED` > n% over 15 min"> | <who> |
| Job queue depth / DLQ | `<tool>` | <threshold> | <who> |
| p95 latency | `<tool>` | <threshold> | <who> |

<!-- GUIDE: Alert on your OUTCOME ENUM distribution, not just on 500s. A spike in a specific outcome
     value is a far more precise signal than "errors are up", and it maps directly to a code path. -->

## When something is wrong — first five minutes

1. **What is the blast radius?** One user, one tenant, or everyone? `<query/dashboard>`.
2. **What changed?** Last deploy: `<command>`. Last migration: `<command>`. Third-party status:
   `<links>`.
3. **Is it us or them?** Check provider status pages before debugging your own code.
4. **Stop the bleeding.** Roll back, or disable the feature: `<how>`.
5. **Then diagnose.** Read the digest and the traces, not the raw logs. Correlation ID from any
   affected request: `<how to search>`.

**Only after it is stable:** write the [ISSUES.md](../ISSUES.md) row — symptom, diagnosis, fix,
status `FIXED?`.

## Backups and restore

| What | Frequency | Retention | Restore time |
|---|---|---|---|
| `<database>` | <…> | <…> | <…> |
| `<object storage>` | <…> | <…> | <…> |

**Restore procedure:** `<exact commands>`

**Last restore rehearsal:** YYYY-MM-DD.

<!-- GUIDE: A backup you have never restored is not a backup. Rehearse once per quarter and put the
     date here — the date being old is itself the alert. -->

## Routine maintenance

| Task | Cadence | Command | Last done |
|---|---|---|---|
| Dependency updates | <…> | <…> | YYYY-MM-DD |
| Restore rehearsal | Quarterly | <…> | YYYY-MM-DD |
| Rollback rehearsal | Quarterly | <…> | YYYY-MM-DD |
| Secret rotation | <…> | <…> | YYYY-MM-DD |
| Provider contract re-verification | <…> | See [DOMAIN.md](DOMAIN.md) | YYYY-MM-DD |
| `ISSUES.md` `FIXED?` → `VERIFIED` review | <…> | <…> | YYYY-MM-DD |
