# Decisions and why

The choices that shape this codebase, and the evidence behind them. If a change seems to fight the
design, the reason is probably here — most of these were settled by measurement, not preference.

## Attach to a real Chrome, never launch an automated one

**Decision.** The app launches the user's actual Chrome with
`--remote-debugging-port=9222 --user-data-dir=<profile>`, the human signs in and clears any
Cloudflare check themselves, and the app attaches with `connectOverCDP`.

**Why.** The obvious approach — Playwright's `launchPersistentContext(channel: "chrome")` — was
measured and is unusable: it sets `navigator.webdriver === true`, and Cloudflare then refuses to
issue a clearance token. The "Verify you are human" checkbox **loops forever even when a real human
clicks it**, and a persisted `cf_clearance` cookie doesn't save it. Attaching over CDP gives
`webdriver === false` because the browser genuinely is a normal browser; the same search that was
walled scraped cleanly.

This is not evasion. Nothing is faked — it *is* an ordinary browser, and the human solves anything
meant for humans.

**The line, which does not move:** never patch `navigator.webdriver`, never
`--disable-blink-features=AutomationControlled`, never solve a captcha. On a challenge the app
detects it, disconnects, and hands it to the user.

## Cloudflare is a pacing problem, not just a detection problem

A cold first navigation is often challenged and then self-clears on retry. But volume escalates it:
roughly four rapid multi-page searches plus posting visits within ten minutes turned into *every*
posting page being challenged, which then blocks the user's own real use of the app.

Consequences baked into the code: jittered delays between result pages and between postings; the run
stops on a challenge rather than hammering; and the app **disconnects from Chrome** before asking the
user to clear a check, because clearance can be refused while a debugger is attached.

When testing, prefer local `data:` URLs for page-detection scripts, and space out anything that
touches the live site.

## A local model, not a hosted API

**Decision.** AI-enhanced answering runs `llama-server` (llama.cpp) as a managed subprocess on
localhost with Qwen3-1.7B Q4_K_M.

**Why.** No API key, no account, no network dependency, and the resume never leaves the machine.
The reasoning task is short and simple, so a ~1.1 GB quantized model is enough, and it starts in
seconds once cached.

**Pins live in one place** (`llm/LlmAssets`) with sha256s. Note that Qwen's *official* GGUF repo
publishes no Q4 build — only a 1.8 GB Q8_0 — so the pin is unsloth's Q4_K_M. Prompts are sent with a
JSON schema, which llama.cpp compiles to a grammar: a dropdown answer is then *structurally* incapable
of being off-list. `LlmResponseValidator` re-checks anyway before anything reaches a form.

## Small models need a procedure, not principles

Telling a 1.7B model to "be conservative" does not work — measured. It answered `0` to an
unmentioned-skill question but flagged itself unsure (so the app paused), and it invented
availability.

What works, and why `ScreenerPrompt` is shaped the way it is: a **numbered decision procedure**
(resume answers it → use it; unmentioned experience or qualification → zero/none/"No" *confidently*;
genuinely unknowable personal detail → say unsure), plus concrete few-shot examples, plus a short
reminder appended at the end of the user prompt. That took the same questions from pausing to 6/6
correct.

Keep the candidate/profile block **byte-identical** between calls: that is what lets llama-server's
prefix cache make every question after the first fast (~1.7 s versus ~5 s).

## Answer precedence, and never fabricating

Order: **contact details → Q&A bank → rules → model → leave for the human.** The bank is checked
before the model so a question answered once is instant and free forever after, including in later
runs.

The app is meant to run unattended, so it prefers answering over pausing — but only honestly. When
the resume is silent about a skill, licence, or certification, the correct answer is the conservative
one (`0`, "No", "None") given *confidently*. It must never claim a credential the resume doesn't
support. Free-text pay questions answer "Negotiable"; a numeric pay field is left alone rather than
inventing a figure.

One consequence worth knowing: rules answer only *generic* experience totals. A question about a
specific skill ("years of AZ driving experience") is deliberately declined by the rules, because
answering it from the resume's overall total would fabricate specific experience. Those go to the
model.

## Submitting is irreversible

Auto-submit sends real applications to real employers. Hence: Review mode is the default and never
submits; probes never click a final Submit; and a submission is only *counted* as verified when
Indeed's confirmation is actually detected afterwards, because a click is not proof.

## The UI is a thin layer over one facade

Every screen talks only to `AppCore`. The UI never touches the browser, the model, or the disk, and
never blocks — it reads plain state on a single 500 ms timer. This is what makes the engines
swappable and keeps threading contained to one class.

Every variant sits behind an interface plus a factory (`BrowserDriver`, `QuestionAnswerer`,
`LlmRuntime`, `LocationSuggester`), which is how AI answering was added without the walkthrough
changing at all.

## Design system

FlatLaf, installed before any component is created — Swing's default Metal look was most of why the
app looked dated. Light graphite neutrals with a **petrol accent (#0B6363), deliberately not Indeed
blue**: this is the user's tool, not a fake Indeed. Status colours are reserved for real state (a
zero count renders neutral, never red). The signature element is the custom-painted `StepRail`,
which is honest here because the app genuinely is a sequence.

Filters mirror what Indeed actually supports — `DatePosted` offers 1/3/7/14 days because there is no
"past month" filter to apply. Offering one would promise something the app cannot deliver.

## Storage

Everything is plain TSV under `~/.indeedapplier/`, append-only where it makes sense, and fail-soft
on every path. Credentials are never seen or stored; raw resume text stays in memory and is never
persisted. The one deliberate exception is the Chrome profile directory, which holds the signed-in
session so the user signs in once — anyone with filesystem access is effectively signed in, a
tradeoff the user accepted knowingly.

This is a single-user desktop app with no server, so server-shaped threat models (IDOR, session IDs,
API endpoints) don't apply. The real analogues are handled: sign-in detection fails closed, the
history store is an audit log, and errors say what actually happened.
