# 0001 — Tech stack for the phone app

- **Status:** proposed — **not decided**
- **Date:** —
- **Deciders:** IT team

## Context

Two front-ends need a stack. This ADR covers the **phone app** only. The cabinet screen is a separate decision — task **P0-14** — because it cannot be answered until we know what the screen is and what it runs.

The app is not heavy. It shows lists, takes taps, reads a QR code, and calls an API. No 3D, no video, no heavy computing. The camera is the only demanding part, and every option below handles it.

## The question changed on 2026-08-05

It used to be *"one code base or two?"*, weighing half the work against better hardware control.

[ADR 0007](0007-android-first.md) makes the MVP **Android only**. So the trade is no longer about today's effort — every option ships one Android app. The real question is now:

> **When iOS finally starts, how much of the work is already done?**

| Option | Android MVP effort | Cost of adding iOS later |
| --- | --- | --- |
| **A. One code base** — Flutter or React Native | Same as native, roughly | **Small.** Mostly building, testing and fixing platform quirks. The screens already exist |
| **B. Native Kotlin** | Same, and the most direct access to the camera and secure store | **A second app from scratch.** Every screen written twice, in a language nobody on the team has used yet |

This reframing is the whole point. Android-first only stays cheap if the stack keeps iOS cheap. **Choosing native Kotlin now converts a deferred phase into a rewrite** — and that would make ADR 0007 a mistake in hindsight rather than a sensible deferral.

## Options

| Option | Good | Bad |
| --- | --- | --- |
| **A1. Flutter** | One code base. Screens look the same on both platforms. Strong QR and secure-store packages. Fast to build and change | Dart is a new language for most people. The app file is larger |
| **A2. React Native** | One code base. JavaScript, which more students already know. Huge package ecosystem | More moving parts to set up. Platform quirks surface more often than in Flutter |
| **B. Native Kotlin** | Best possible camera and hardware access. No extra layer. Standard Android tooling | iOS later means writing the whole app again in Swift. Two code bases forever |

## Decision

**Not decided.** Task **P0-03** settles it. Set the status to `accepted` and fill in the sections below.

**Leaning toward A**, on the reasoning above. B is defensible only if something about the cabinet hardware demands direct Android access that a cross-platform layer cannot give — and nothing currently does, because the phone never talks to the hardware. The server does. See [architecture.md](../reference/architecture.md).

## What to check before deciding

1. **What does the team already know?** A team that knows JavaScript ships faster on React Native than on anything else, whatever a comparison chart says. This usually decides it.
2. **How many people are on the app side?** Fewer than three makes two native apps impossible to sustain, which rules out B.
3. **Does the phone ever talk to the cabinet hardware directly?** Currently no — the server commands the locks, and the phone only calls the API. That removes the main argument for native. Assumption **A-04**, still open.
4. **Can the chosen stack read a QR code and use the phone's secure store?** All three can. Confirm rather than assume, because both are load-bearing: P5-02 and P2-04.

No longer on this list: *"do you have a Mac and an Apple developer account?"* [ADR 0007](0007-android-first.md) moved that to the rest of the team, and it no longer gates this decision.

## Why

Fill in after deciding. Two or three sentences.

## What we accept

Fill in after deciding. The known downside taken on purpose.

## Affects

- Blocks **P1-01** (empty app on real Android), **P1-04** (the one network file), and through them every later phase
- **P1-02** and the iOS work it leads to — how expensive that becomes is decided here
- Does **not** block the cabinet screen. That is **P0-14**
