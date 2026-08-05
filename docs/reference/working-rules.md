# Rules of working

How this team makes decisions and moves work. Built on **empirical development**.

These rules are gathered from senior practice in industry — Google's code review guidance, the DORA delivery research, trunk-based development, contract-first API work, and systematic debugging. Sources are listed at the end. They are cut down to fit a student team building one mobile app.

---

## The one idea

**Empirical means: decide from evidence, not from assumption.**

A plan made in a room is a guess. A real card scanned by a real phone is evidence. When the two disagree, the evidence wins and the plan changes.

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
Turn it into a roadmap task with an ID and an owner. See P0-02, which exists only to answer "what is actually printed on a VGU card?"
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
See task P0-05 and [api-contract.md](api-contract.md).
*Why:* a broken contract is a top cause of mobile regressions. Change the contract after the screens exist and you rebuild the screens. This is the highest-cost mistake available to this project.

**C2. A contract change is a decision by both teams, never by one.**
App team and Server team agree, in writing, before the code changes.

**C3. A mock is allowed in a test. Never in a shipping path.**
If a mock exists, it is labelled as a fixture and it lives in `tests/`.
*Why:* a mock in a shipping path is a lie that passes every test and fails every user.

**C4. The server is the truth. The app never decides.**
The app does not decide who you are, which locker is yours, whether a locker is free, or whether the mode allows an open. It asks.
*Why:* one truth in one place. Also: an app on a phone can be modified. A server cannot.

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

**D4. For anything that opens a locker, the server log is the evidence.**
Not the screen. The screen shows what the app believes. The log shows what happened.

**D5. Unclear is not success.**
If the app cannot tell whether the locker opened, it says so. It never guesses.

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

**F6. Anything touching a locker open, a token, or a password gets a careful review, however small the change.**

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

An open request moves real metal and exposes someone's belongings. These are not style preferences.

1. An open request is fired only by a direct user tap. Never by a retry, a timer, or a screen refresh.
2. One tap = at most one open request. Proven from the server log, not assumed.
3. Unclear result → say it is unclear. Never show a false success.
4. A parcel code works once, and expires.
5. No key, token, password or private address inside the app code. Anything shipped in the app can be read by anyone who has the app.
6. Found a secret in the code or in git history? **Rotate it first**, then remove it. Removing it alone does nothing — it is already public.

---

## Assumption log

Every belief we act on but have not yet checked. Add a row the moment you notice one.

**Status:** `open` → `confirmed` or `wrong`. A `wrong` row is a win — it was found before it cost anything.

| # | Assumption | Made by | Date | How we will check it | Status |
| --- | --- | --- | --- | --- | --- |
| A-01 | All VGU student cards use one barcode format | — | — | Scan several real cards from different years (P0-02) | open |
| A-02 | The API sends each locker point's location, so the app can find the nearest | — | — | Ask the Server team (P0-05, question 1) | open |
| A-03 | Operating mode is per locker point, not one setting for the whole school | — | — | Ask the Server team (P0-05, question 3) | open |
| A-04 | The phone never talks to the locker hardware directly — only the server does | — | — | Confirm with the hardware owner (blocks the ADR 0001 stack choice) | open |
| A-05 | We have a Mac and an Apple developer account available for iOS builds | — | — | Check today. It blocks P1-03 and takes longest to fix | open |

---

## Changing these rules

**This file is the authority on how the team works.** `CLAUDE.md` carries a short summary of it for agents. If the two disagree, this file wins — and the summary is corrected in the same change. Never edit a rule in `CLAUDE.md` alone. See [ADR 0002](../adr/0002-where-rules-live.md).

These rules are subject to their own loop. They are inspected at the end of every phase and adapted.

To change one: say which rule, what happened that shows it is wrong, and what replaces it. Bring evidence, not preference. Then edit this file — it is mutable in place, like everything in `reference/`.

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
