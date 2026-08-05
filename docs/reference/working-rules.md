# Rules of working

How this team makes decisions and moves work. Built on **empirical development**.

These rules are gathered from senior practice in industry — Google's code review guidance, the DORA delivery research, trunk-based development, contract-first API work, and systematic debugging. Sources are listed at the end. They are cut down to fit a student team building one mobile app.

---

## The one idea

**Empirical means: decide from evidence, not from assumption.**

A plan made in a room is a guess. A real parcel collected from a real cabinet is evidence. When the two disagree, the evidence wins and the plan changes.

The loop is three steps, and it never stops:

```
   TRANSPARENCY  ──>  INSPECTION  ──>  ADAPTATION
   make the work         look at             change the work
   visible               what is real        or the plan
        ^                                          │
        └──────────────────────────────────────────┘
```

| Pillar | In this repo |
| --- | --- |
| **Transparency** | The board, the bug log, the API contract, the assumption log. If work is not written down, it does not exist |
| **Inspection** | The Verify line on every task. Real devices. Server logs. Weekly board review |
| **Adaptation** | The Verify failed → change the code, or change the plan, or change these rules |

Empirical process control exists because software is unpredictable. You cannot plan your way out of an unknown. You can only make the unknown small, look at it early, and adjust.

---

## A. Evidence rules

**A1. No claim without a check.**
Never say "done", "fixed", or "works" without running something. State what you ran and what came out.
*Why:* an unchecked claim is an assumption wearing a suit. It fails later, in front of a user.
*Check:* every task's Verify line was actually run.

**A2. Evidence beats opinion, and both beat seniority.**
The server log outranks a strong feeling. Anyone on the team may ask "what is the evidence?" — including of the most senior person in the room.
*Why:* this is the whole point of empirical work. A team where rank decides is guessing with confidence.

**A3. Write down every assumption you cannot check yet.**
It goes in the [Assumption log](#assumption-log) below, with a date and an owner.
*Why:* an assumption you forgot you made is the most expensive kind. Written down, it is cheap.
*Check:* the log has no row older than two weeks with status `open`.

**A4. An unknown that blocks work is a task, not a worry.**
Turn it into a roadmap task with an ID and an owner. See P0-01, which exists only to answer "does the cabinet have its own network connection?"
*Why:* worries get discussed forever. Tasks get done.

**A5. "It works on my machine" is not evidence.**
Evidence for this project means a real phone, a real network, and the real server.
*Why:* simulators hide memory limits, slow CPUs, bad networks and OS-specific bugs. Real devices are the standard for mobile testing.

---

## B. Small batch rules

**B1. Make the change small.**
One task, one change, one reason. About 200 changed lines is comfortable. 1000 lines is too big.
*Why:* Google's guidance is direct — small changes are reviewed faster, reviewed better, and are less likely to break something. Reviewers rarely complain that a change is too small.

**B2. Merge to the main branch often — at least once a day.**
Do not sit on a branch for a week.
*Why:* trunk-based development avoids "integration hell", where a week of work meets a week of someone else's work and neither survives. Small merges keep main working.

**B3. Main branch must always work.**
If main is broken, fixing it is the team's top job. Nothing else moves first.
*Why:* main is where everyone starts. A broken main multiplies across the whole team.

**B4. Half-finished work ships hidden, not on a long branch.**
Put the unfinished part behind a switch that is off, and merge it.
*Why:* this decouples "merged" from "released". It is how teams keep small batches without shipping broken screens.

**B5. A task bigger than one day is two tasks.**
Split it. Give the new one the next free ID.
*Why:* long tasks hide their trouble until the end. Short tasks show it on day one.

---

## C. Contract-first rules

**C1. Agree the API contract before building the screen.**
See task P0-04 and [api-contract.md](api-contract.md).
*Why:* a broken contract is a top cause of mobile regressions. Change the contract after the screens exist and you rebuild the screens. This is the highest-cost mistake available to this project.

**C2. A contract change is a decision by both teams, never by one.**
App team and Server team agree, in writing, before the code changes.

**C3. A mock is allowed in a test. Never in a shipping path.**
If a mock exists, it is labelled as a fixture and it lives in `tests/`.
*Why:* a mock in a shipping path is a lie that passes every test and fails every user.

**C4. The server is the truth. The app never decides.**
The app does not decide who you are, which box is yours, or whether a box is free. It asks.
*Why:* one truth in one place. Also: an app on a phone can be modified. A server cannot.

> ⚠️ **This rule is under challenge, and the challenge is not yet decided.** [ADR 0004](../adr/0004-offline-pickup.md) proposes that when the network is gone, the cabinet checks an answer by itself and opens a door with no server involved. That is a front-end deciding. Either C4 gains a written exception, or ADR 0004 does not get built. **Task P0-13 settles it.** Until then C4 stands as written, and nothing offline gets built.

---

## D. Verify rules

**D1. Every task carries its own Verify line.**
No Verify line → the task is not ready to start. Write the check before you write the code.
*Why:* a check written afterwards is written to pass.

**D2. `done` means the Verify passed. Not "code written".**
*Check:* a ticked box with no evidence is a broken board. Untick it.

**D3. Test on a real Android **and** a real iPhone at the end of every phase.**
Not only in Phase 8.
*Why:* half of mobile bugs live on one device only. Finding them at the end means fixing them all at once, under pressure.

**D4. For anything that opens a box, the server log is the evidence.**
Not the screen. The screen shows what the app believes. The log shows what happened.

**D5. Unclear is not success.**
If the app cannot tell whether the box opened, it says so. It never guesses.

**D6. A tick carries a `Done` line, written for someone with no technical background.**
Say what you did and what you saw. Name the real phone, the real cabinet, the real person. Two or three sentences, plain words.
*Why:* "done" is a claim. The `Done` line is the evidence, in a form the whole team can check — not only the person who wrote the code. A team member who cannot follow it cannot inspect it, and [transparency](#the-one-idea) stops working.
*Check:* read it to somebody outside the team. They cannot follow it → rewrite it.

**D7. The board is updated in the same change as the work.**
Not at the end of the day. Not at the end of the week.
*Why:* a board that lags is a board that lies. People trust it and plan around it. See [H1](#h-cadence--when-inspection-happens) — a board that lies is worse than no board.

---

## E. Debugging rules

Follow this order. Do not skip a step because it feels slow.

1. **Reproduce it.** Smallest steps that make it happen every time. Cannot reproduce → log it anyway, write "seen once".
2. **Describe the failure exactly.** What you saw, not what you think caused it. Record the device and the OS version.
3. **List at least three possible causes.** One cause is not debugging. It is guessing.
4. **Pick the one you can disprove fastest**, and say what result would prove it wrong.
5. **Change one thing at a time.** Two changes and a fix teaches you nothing.
6. **Fix the cause, not the symptom.**
7. **Verify on the device that found it**, then write the row in the bug log.

**E1. If you are changing code before you can explain why it failed, you are gambling, not debugging.**

**E2. Never fix by retrying.** Especially never retry an open request. See the Phase 4 safety rules.

---

## F. Review rules

**F1. Every change is reviewed by one other person before it reaches main.**
*Why:* the second pair of eyes is the cheapest inspection available.

**F2. Review the change, not the person.** Talk about the code. Never "you always…".

**F3. The standard is: does this improve the codebase?** Not "is this how I would have written it?"
*Why:* Google's review standard is exactly this. Perfect is the enemy of merged. Nothing gets in that makes the code worse; nothing is blocked for being merely different.

**F4. The reviewer explains **why**.** "Rename this" is an order. "Rename this — `data` does not say what is in it" teaches.

**F5. Review within one working day**, or say when you can.
*Why:* a change waiting for review is work that is finished and helping nobody.

**F6. Anything touching a door opening, a token, or a code gets a careful review, however small the change.**

---

## G. Measure rules

Measure the delivery process, not the people. Four numbers, checked weekly. They are the small-team version of the DORA delivery metrics — two for speed, two for stability.

| # | Number | How we get it | Good direction |
| --- | --- | --- | --- |
| 1 | **How often do we put a build on a real phone?** | Count per week | More often. Aim for at least weekly, then daily |
| 2 | **How long from starting a task to ticking it?** | Task start date → tick date | Shorter. A task over a week is a task that was too big |
| 3 | **How many changes broke something?** | Bugs caused by our own last change ÷ changes | Lower |
| 4 | **How fast do we fix a broken main branch?** | Time from break to green | Faster. Same day |

**G1. Never use these numbers to rank people.**
*Why:* the moment a number judges a person, the number becomes fiction. People optimise the measure and stop reporting reality — and then you have lost the transparency the whole loop depends on.

**G2. A number that never changes a decision is not worth collecting.** Drop it.

---

## H. Cadence — when inspection happens

| When | What | Output |
| --- | --- | --- |
| Every change | Review, plus the task's Verify | Merged, or sent back |
| End of each day | Merge to main. Main stays working | Green main |
| Once a week | **Board review.** Walk the current phase. Every task: still true? still needed? still blocked? | Board matches reality |
| End of each phase | **Exit check** + real-device test on both phones | Phase box ticked |
| End of each phase | **Rules review.** Which rule helped? Which got ignored, and why? | This file changes |

**H1. The weekly board review is the heartbeat.** Skip it and the board drifts from reality. A board that lies is worse than no board — people trust it and act on it.

**H2. A rule that everyone ignores is a broken rule, not a broken team.** Fix the rule or delete it.

---

## I. Safety rules — this project specifically

An open request moves real metal and exposes someone's parcel. These are not style preferences.

1. An open request is fired only by a direct user tap. Never by a retry, a timer, or a screen refresh.
2. One tap = at most one open request. Proven from the server log, not assumed.
3. Unclear result → say it is unclear. Never show a false success.
4. A parcel code works once, and expires.
5. No key, token, password or private address inside the app code. Anything shipped in the app can be read by anyone who has the app.
6. Found a secret in the code or in git history? **Rotate it first**, then remove it. Removing it alone does nothing — it is already public.

---

## J. Restraint rules — writing the change

Groups A–I say how the team decides. This group says what the change itself should look like. It comes from the `andrej-karpathy-skills:karpathy-guidelines` skill — a list of the mistakes AI coding assistants make most, and human juniors make the same ones. Agents load that skill for the full text instead of searching the web for it.

**J1. Say your assumptions out loud before you build.**
Two ways to read the task → show both and ask. Never pick one silently and build it.
*Why:* a silent choice looks like agreement. Four days later it turns out to be a rebuild. Cheaper to ask for one minute.
*Related:* [A3](#a-evidence-rules) logs assumptions about the **project**. J1 is about assumptions inside **one task**.

**J2. The simplest thing that solves the problem. Nothing more.**
No feature nobody asked for. No setting nobody asked for. No wrapper around a thing used once. No handling for a case that cannot happen.
*Why:* every extra line is a line to read, test, fix and carry. Code written "in case we need it later" is almost never the code needed later.
*Check:* if it is 200 lines and it could be 50, it is not done. Rewrite it.

**J3. Touch only what the task needs.**
Do not tidy nearby code. Do not rename things you did not come to change. Do not reformat. Match the style already in the file, even where you would have done it differently.
*Why:* a change that touches ten files for one reason cannot be reviewed, and cannot be undone cleanly when it breaks.
*Check:* every changed line traces back to the task. One that does not, comes out.

**J4. Clean up your own mess, not other people's.**
Your change left an unused import or variable → delete it, same change. You spotted dead code you did not create → say so, leave it.
*Why:* the first is finishing your work. The second is a different task, and it hides inside yours where nobody reviews it.

**J5. Push back when the ask is more complicated than the problem.**
Say the simpler version, say why, then do what is decided.
*Why:* [A2](#a-evidence-rules) — evidence beats opinion, and both beat seniority. That runs in both directions. Silent agreement is not agreement.

**Deliberately not copied.** Karpathy's fourth rule is "define success criteria, loop until verified." That is already [D1](#d-verify-rules) and [D2](#d-verify-rules). Copying it would put two rules in this file on the same subject, and the day they drift, neither is the authority. If you came here to add it, it is above.

---

## Assumption log

Every belief we act on but have not yet checked. Add a row the moment you notice one.

**Status:** `open` → `confirmed` or `wrong`. A `wrong` row is a win — it was found before it cost anything.

A fourth status, `dropped`, is for a row the product change made meaningless. Never delete a row. A reader must be able to see what we once believed.

| # | Assumption | Made by | Date | How we will check it | Status |
| --- | --- | --- | --- | --- | --- |
| A-01 | All VGU student cards use one barcode format | — | 2026-08-05 | No longer asked. There is no card scan in the parcel product ([ADR 0003](../adr/0003-parcel-locker-product.md)) | dropped |
| A-02 | The API sends each locker point's location, so the app can find the nearest | — | 2026-08-05 | No longer asked. One cabinet, no "nearest point" ([ADR 0003](../adr/0003-parcel-locker-product.md)) | dropped |
| A-03 | Operating mode is per locker point, not one setting for the whole school | — | 2026-08-05 | No longer asked. There is no exam mode in the parcel product ([ADR 0003](../adr/0003-parcel-locker-product.md)) | dropped |
| A-04 | The phone never talks to the cabinet hardware directly — only the server does | — | — | Confirm with the hardware owner (blocks the ADR 0001 stack choice, P0-03) | open |
| A-05 | We have a Mac and an Apple developer account available for iOS builds | — | — | Check today. It blocks P1-02 and takes longest to fix | open |
| A-06 | The cabinet has a screen the shipper can type on | — | 2026-08-05 | **Confirmed by the team** (P0-02 records the size and type) | confirmed |
| A-07 | The design needs no camera on the cabinet | — | 2026-08-05 | True by design — the phone scans the cabinet, not the reverse. See [ADR 0003](../adr/0003-parcel-locker-product.md) | confirmed |
| A-08 | The receiver has the app installed and is logged in before a parcel is dropped | — | — | True by design — registration lives in the app. Watch for a real receiver who has not registered (P3-06) | open |
| A-09 | The cabinet has its own network connection (SIM or Wi-Fi) | — | 2026-08-05 | **Ask the hardware team (P0-01).** We are building as if it does, on the team's instruction — but nobody has said so in writing. Nothing in the plan works without one | open |
| A-10 | The sensor can tell "a parcel is in here" reliably enough to record a delivery on | — | 2026-08-05 | The team said a sensor will exist. Nobody has said what it reports. Ask (P0-10) | open |
| A-11 | A receiver who types the backup code is the person the code was sent to | — | 2026-08-05 | Cannot be checked, only limited. One use, an expiry, a lockout after wrong tries (P0-11, P5-08) | open |
| A-13 | The cabinet can remember a wrong-try count across a power cut | — | 2026-08-05 | Ask the hardware team with P0-01. Without it, the attack is guess, unplug, repeat (P6-08) | open |
| A-14 | Campus Wi-Fi drops rarely enough that a late notice is acceptable during an outage | — | 2026-08-05 | Log the cabinet's connection for one week (P0-12 open question 1) | open |
| A-15 | The cabinet holds 19 parcels, not the 20 the spec claims | — | 2026-08-05 | The drawing numbers doors 01-20 but 06 is the control panel, so counting gives 19. Ask whoever drew it (P0-02). It changes how many parcels a cabinet holds | open |
| A-12 | One cabinet is enough for the first release | — | 2026-08-05 | Ask the team. Two cabinets means the notice must say *which* one, and the pickup must check it (P6-05 already assumes this) | open |

---

## Changing these rules

**This file is the authority on how the team works.** `CLAUDE.md` carries a short summary of it for agents. If the two disagree, this file wins — and the summary is corrected in the same change. Never edit a rule in `CLAUDE.md` alone. See [ADR 0002](../adr/0002-where-rules-live.md).

These rules are subject to their own loop. They are inspected at the end of every phase and adapted.

To change one: say which rule, what happened that shows it is wrong, and what replaces it. Bring evidence, not preference. Then edit this file — it is mutable in place, like everything in `reference/`.

### A rule does not change because the product changed

This is the line, and it is drawn because it was crossed on 2026-08-05.

The product moved from a student storage locker to a parcel locker. In the same change as that work, this file was edited. Most of it was harmless — but one edit quietly gave **C4** an exception it had never been granted. Nobody decided that. It arrived as a side-effect.

| What it is | May it change with the product? |
| --- | --- |
| **The rule** — what it permits, forbids, or requires. Its *why* | **No.** Only through the process above: which rule, what evidence, what replaces it |
| **The words naming a project thing** — locker, box, card, parcel | Yes, but in **its own change**, never carried along with other work |
| **A pointer to a task ID** | Yes, same condition. A pointer to a task that has been renumbered is worse than none — it sends the reader somewhere wrong |
| **The assumption log** | Yes, freely. It is a log. It is meant to grow |

The test: **would this edit change what somebody is allowed to do?** Yes → it is a rule change, and it needs the process. No → it is wording, and it still gets its own commit.

"The product changed" is never on its own a reason to weaken a rule. If a rule genuinely blocks the new product, that is a real conflict and it deserves a real decision — written down, argued, and decided by the team. Not a quiet edit inside a commit about something else.

---

## Sources

Industry practice these rules are drawn from:

- [Empirical Process Control in Scrum — Scrum Alliance](https://resources.scrumalliance.org/Article/empirical-process-control-scrum) — transparency, inspection, adaptation
- [Definitive Guide to Empirical Process — Universal Agile](https://universalagile.com/definitive-guide-to-empirical-process/)
- [Small CLs — Google Engineering Practices](https://google.github.io/eng-practices/review/developer/small-cls.html) — change size guidance
- [The Standard of Code Review — Google Engineering Practices](https://google.github.io/eng-practices/review/reviewer/standard.html) — "does it improve the codebase?"
- [Trunk-Based Development — Atlassian](https://www.atlassian.com/continuous-delivery/continuous-integration/trunk-based-development) — daily merges, small batches
- [Implement trunk-based development using feature flags — Unleash](https://docs.getunleash.io/guides/trunk-based-development) — hiding unfinished work
- [Using the Four Keys to measure DevOps performance — Google Cloud](https://cloud.google.com/blog/products/devops-sre/using-the-four-keys-to-measure-your-devops-performance) — the four delivery numbers
- [Understanding the 4 DORA metrics — Octopus Deploy](https://octopus.com/devops/metrics/dora-metrics/) — speed vs stability balance
- [Mobile App Testing: Best Practices and Strategy — Applause](https://www.applause.com/blog/mobile-app-testing-best-practices-and-strategy/) — real devices over simulators
- [API Testing for Mobile Apps — Quash](https://quashbugs.com/blog/api-testing-for-mobile-apps) — contract-first, broken contracts as a top regression cause
- [Debugging like a senior: a step-by-step mental model — Lauren M.](https://medium.com/@lauren.m45/debugging-like-a-senior-a-step-by-step-mental-model-59a1fd4dbc7d) — hypothesis before change, one variable at a time
- **Karpathy guidelines** — the installed skill `andrej-karpathy-skills:karpathy-guidelines`, not a web page. Source for group J: state assumptions, simplest thing that works, surgical changes. Agents invoke the skill; they do not search for it.
