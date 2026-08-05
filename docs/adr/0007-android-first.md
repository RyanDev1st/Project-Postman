# 0007 — Android first. iOS is deferred, not cancelled

- **Status:** accepted
- **Date:** 2026-08-05
- **Deciders:** IT team

## Context

The plan was both platforms from the start. Every phase ended with a test on a real Android **and** a real iPhone.

Two things make that expensive right now:

- **An iOS build needs a Mac and an Apple developer account.** Neither is confirmed. It was the longest-lead item in the whole project, and it sat in front of Phase 1.
- **Nothing is proven yet.** No screen has been drawn, no locker has opened. Paying an iOS tax on a product that has not been shown to work is paying twice for a guess.

## Decision

**Build an Android MVP first.** One platform, end to end, until a real parcel goes into a real cabinet and a real student takes it out.

iOS is **deferred, not cancelled**. Every iOS task keeps its ID and stays on the board, marked `⏸️ LATER`. The Apple account and Mac question moves to the rest of the team and stops blocking us.

## What this costs — and it is not nothing

**This accepts the exact risk rule [D3](../reference/working-rules.md) was written to prevent.**

D3 says: test on a real Android **and** a real iPhone at the end of every phase, *"because half of mobile bugs live on one device only. Finding them at the end means fixing them all at once, under pressure."*

Android-first is that pattern. Six phases of Android-only work, then iOS arrives and every one-platform bug shows up in the same week.

We are choosing it anyway, because a working Android app is worth more than two half-built ones, and because an MVP that nobody has used may not deserve a second platform at all. But the debt is real and it is written here so nobody is surprised by it later.

**D3 is not being changed.** The rule stands as written. We are knowingly deferring part of it, which is a different thing from deciding it was wrong. When iOS work starts, D3 applies in full from that day.

## How we keep the debt small

1. **iOS is its own phase when it comes, not a bolt-on to Phase 8.** It gets its own device-test task and its own bug budget. Squeezing it into "harden and ship" is how the pile becomes a crisis.
2. **The stack choice is made with iOS in mind.** See P0-03. A cross-platform framework makes the later iOS phase small. Native Kotlin makes it a rewrite. That trade is now the main question in [ADR 0001](0001-tech-stack.md), and it is the one place where Android-first must not be shortsighted.
3. **No Android-only shape in the design.** Nothing in the API contract, the architecture or the settings assumes Android. The QR reader, the secure store and the notification route all have iOS equivalents, and that is the reason those things are named by what they do rather than by which library provides them.

## What becomes easier

- **P0-09 stops being urgent.** The Apple account was its long pole. It is now the rest of the team's item, on their timeline.
- **P1-02 is off the critical path**, so Phase 1 finishes on one device.
- **P0-03 has fewer constraints**, since "do we have a Mac" no longer gates the answer — though point 2 above means it must still be answered with iOS in view.

## When to revisit

Start the iOS phase when **both** are true:

1. A real student has collected a real parcel on Android, with no help.
2. Somebody actually has a Mac and an Apple developer account.

Not before the first. There is no point porting a product that has not been shown to work.

## Affects

- `docs/roadmap/phase-1-skeleton.md` — P1-02 marked `⏸️ LATER`
- `docs/roadmap/phase-8-ship.md` — P8-02 marked `⏸️ LATER`
- Every phase Exit check that named an iPhone
- `docs/roadmap/README.md` — the `⏸️ LATER` marker is added to the task format
- [ADR 0001](0001-tech-stack.md) — the stack question is now framed as *"how cheap does this make the later iOS phase?"*
