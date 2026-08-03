# Working on this project

Java 21 Swing desktop app that applies to Indeed postings automatically, driving a real Chrome
window and answering screener questions with rules plus a locally-run language model.

**Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) first** for the component map, then
[docs/EXTENDING.md](docs/EXTENDING.md) for conventions and recipes.
[docs/DECISIONS.md](docs/DECISIONS.md) explains why things are the way they are — check it before
"fixing" something that looks odd, since most of it was settled by measurement.
[docs/INDEED.md](docs/INDEED.md) holds the live-site knowledge (apply flow, DOM traps,
interstitials). [ISSUES.md](ISSUES.md) is the live record of what is broken, what was fixed, and
what is confirmed.

## Environment

- **Use PowerShell.** The Bash tool does not work in this environment.
- `.\gradlew.bat run` · `.\gradlew.bat test` · `.\gradlew.bat installDist` (the last one gives you
  `build/install/IndeedAutoApplier/lib/*`, the classpath for scratch probes).
- User data and all diagnostics live under `~/.indeedapplier/`.

## What matters most here

**This app submits real job applications on the user's behalf.** A wrong answer is not a failing
test, it is a false statement to an employer. Never claim a credential, licence, or experience the
resume doesn't support; when the resume is silent, the conservative answer (`0`, "No", "None") is
the correct one and should be given confidently. The app is meant to run unattended, so prefer
answering over pausing — but never by inventing.

**Verify before claiming.** Repeated lesson on this project: things that looked obviously true were
wrong — 8 "load failures" were Cloudflare pages, a captcha guard fired on an invisible widget, HTML
spacing collapsed only under the real theme, a session log silently overwrote itself. Run a probe,
take a screenshot, read the actual page. Say plainly when something is unverified.

**Be careful with live-site traffic.** Automated searching and applying escalates Cloudflare
challenges against the user's real account, which then blocks their actual use of the app. Space out
probes, prefer local `data:` URLs for testing detection scripts, and stop rather than hammer.

## The improvement loop

The user runs real sessions; the app records them. After a run:

1. Read `~/.indeedapplier/diagnostics/sessions/*.md` (summary, timeline, step-by-step journal) and
   `diagnostics/issues.tsv` plus its screenshots.
2. Diagnose from that evidence, not from assumption.
3. Fix, then update `ISSUES.md` — promote a row to **VERIFIED** only when a later real run shows the
   problem gone.
4. Update the docs when behaviour changes.
