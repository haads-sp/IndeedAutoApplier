<!-- GUIDE: The document most projects don't have and can least do without. It holds facts about the
     OUTSIDE WORLD that you learned the hard way — third-party behaviour that contradicts its own
     docs, business rules that come from outside engineering, browser/platform quirks.

     Three rules:
       1. Only observed facts. If you assumed it, it does not go here.
       2. Every claim carries a verified-on date.
       3. It is perishable by design. When something turns out to be stale, RE-VERIFY and update it
          — do not work around it.

     Without this document these facts live in a comment on one line (invisible from anywhere else),
     a commit message (unfindable), or someone's head (lost). -->

# What we know about the systems we depend on

Facts verified against reality, not assumed from documentation. The code that acts on them lives in
`integrations/<provider>/`; this is the context that explains it and the traps that are invisible
from the API reference.

**Treat this as perishable.** Providers change. When something here turns out to be stale,
re-verify and update it rather than working around it. Every claim is dated.

---

## <Provider A — e.g. the payment provider>

**Last full re-verification: YYYY-MM-DD**

<!-- GUIDE: Structure each provider as: what their docs say → what is actually true → what it means
     for us. The gap between the first two is the entire value of this document. -->

### <Behaviour that differs from the documentation>

**Verified YYYY-MM-DD.** <What you observed, concretely — the actual response, the actual ordering,
the actual timing. Include the specific values.>

**Consequence for us.** <What the code does about it, and where.>

### Webhooks

<!-- GUIDE: Webhook ordering and duplication assumptions are where most integrations break. Write
     down what you have actually observed, not what the provider promises. -->

- **Ordering:** <observed — e.g. "`payment.succeeded` has arrived BEFORE `charge.created` in <n>
  observed cases">. **Verified YYYY-MM-DD.**
- **Duplicates:** <observed rate and handling>.
- **Retries:** <observed schedule>.
- **Signature verification:** <the exact scheme, and any gotcha — e.g. raw-body requirement>.

### Rate limits

<!-- GUIDE: The documented limit and the observed limit are frequently different. Record both. -->

| Endpoint | Documented | **Observed** | Verified |
|---|---|---|---|
| `<endpoint>` | <n>/min | <what actually happened> | YYYY-MM-DD |

### Not yet verified

<!-- GUIDE: This section is as valuable as the rest. Naming what you have NOT confirmed prevents it
     being treated as known. The source project's honesty here — "Not yet verified: the United
     States case specifically… Worth a probe before trusting US searches" — is exactly right. -->

- <Thing we are relying on but have not observed. What would confirm it.>

---

## <Provider B — e.g. the identity provider>

**Last full re-verification: YYYY-MM-DD**

### Token lifetimes

| Token | Documented | **Observed** | Verified |
|---|---|---|---|
| Access | <…> | <…> | YYYY-MM-DD |
| Refresh | <…> | <…> | YYYY-MM-DD |

### <Trap>

**Verified YYYY-MM-DD.** <…>

---

## Business rules from outside engineering

<!-- GUIDE: Rules that came from legal, finance, a regulator, or a contract — things engineering
     cannot derive and must not "simplify". Record the SOURCE, because that is what lets a future
     developer tell a real constraint from an old guess. -->

| Rule | Source | Effective | Notes |
|---|---|---|---|
| <e.g. "Refunds are permitted within 14 days of delivery, not of purchase"> | <who said so, and where it is written> | YYYY-MM-DD | <edge cases> |

---

## Platform and client quirks

<!-- GUIDE: Browser, device, and runtime behaviours that only show up in reality. The source project
     had a whole class of these (a UI framework collapsing whitespace between styled runs, but only
     under the real theme — so an isolated test passed and the app was broken). -->

- **<Quirk>.** **Verified YYYY-MM-DD.** <What happens, on what platform, and the workaround. Note if
  it reproduces only under specific conditions — that detail is what stops someone "proving" it
  isn't real with a test that doesn't reproduce it.>

---

## Re-verification schedule

<!-- GUIDE: Perishable knowledge without a re-check date silently becomes wrong. Keep this small
     enough that it actually happens. -->

| What | Cadence | How | Last done |
|---|---|---|---|
| <Provider A> contract tests against sandbox | <cadence> | `<command>` | YYYY-MM-DD |
| <Provider B> token lifetimes | <cadence> | <how> | YYYY-MM-DD |
