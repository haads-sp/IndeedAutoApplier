# Indeed Auto-Applier

A desktop app that finds Indeed postings matching your resume and walks them through Indeed's
Easy-Apply flow — reading each form, answering the employer's screener questions, and submitting.
It drives a real Chrome window you sign into yourself, and it can answer unfamiliar questions with a
language model that runs entirely on your machine.

## Running it

```
.\gradlew.bat run     # start the app
.\gradlew.bat test    # run the test suite
.\gradlew.bat jar     # build\libs\IndeedAutoApplier-1.0-SNAPSHOT.jar
```

Requires Java 21+ and Google Chrome. Everything else is fetched by Gradle.

## How it works, in one pass

1. **Sign in** — the app opens a normal Chrome window on its own profile. You sign into Indeed by
   hand (once); the session persists, and the app attaches to that same browser afterwards.
2. **Documents** — pick your resume PDF. It is parsed into a fact base (skills, education, years).
3. **Your details / location** — entered once, saved locally, reused to fill contact fields.
4. **Run** — search returns every matching posting across all result pages. Each one is then opened,
   filled, and submitted according to the chosen submit mode.

### Answering modes

Chosen at launch (and rememberable):

- **Standard** — rule-based answers only. No downloads, no model, fully deterministic.
- **AI-enhanced** — the same rules first, then a local language model for anything they can't
  derive. On first launch it downloads a small inference engine and model (~1.1 GB) into
  `~/.indeedapplier/llm/`; after that it starts in a few seconds. Nothing leaves your machine and
  no API key is involved.

AI-enhanced mode also scores every found posting against your resume and shows a **fit percentage**
beside each result.

### Submit modes

- **Review before submit** — fills everything, then stops so you can check and submit yourself.
- **Auto (Easy Apply, all answers known)** — submits only when every question was answered
  confidently.
- **Fully automatic** — walks the whole result list on its own, submitting as it goes.

In the auto modes one click works through every posting found. If a verification check appears, the
app holds the application open and waits for you rather than discarding it.

## What it will not do

- Solve captchas or bot checks. It detects them, pauses, and hands them to you.
- Claim credentials, licences, or experience your resume does not support. When the resume is
  silent, the answer is the conservative one (`0`, "No", "None").
- Invent a salary figure. Free-text pay questions answer "Negotiable"; numeric ones wait for you.

## Where your data lives

Everything is local, under `~/.indeedapplier/`:

| Path | What it holds |
|---|---|
| `chrome-profile/` | The signed-in Chrome session. Delete to sign out. |
| `contact.tsv` | Your name, email, phone, address. |
| `qa-bank.tsv` | Answers learned from past applications, reused instantly. |
| `history.tsv` | Every posting applied to (also the dedup source). |
| `settings.tsv` | Answering mode and other preferences. |
| `llm/` | Local inference engine and model weights (AI mode). |
| `diagnostics/` | Screenshots + `issues.tsv` for anything that stopped short. |
| `diagnostics/sessions/` | One markdown report per run: summary, timeline, step-by-step detail. |

Your password is never seen, typed, or stored by the app — you sign in directly with Indeed.

## Documentation

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — what every component does and how a run flows
  through them.
- [docs/DECISIONS.md](docs/DECISIONS.md) — why the app is built this way, and the measurements
  behind the choices.
- [docs/INDEED.md](docs/INDEED.md) — what we know about the live site: the apply flow, DOM traps,
  interstitials.
- [docs/EXTENDING.md](docs/EXTENDING.md) — how to add or change behaviour, with the conventions
  this codebase follows.
- [ISSUES.md](ISSUES.md) — known issues, their diagnosis, and whether the fix is confirmed.
