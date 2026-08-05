# 0001 — Tech stack for the phone app

- **Status:** accepted
- **Date:** 2026-08-05
- **Deciders:** IT team

## Context

Two front-ends need a stack. This ADR covers the **phone app** only. The cabinet screen is task **P0-14**.

The app is not heavy. It shows lists, takes taps, reads a QR code, and calls an API. No 3D, no video, no heavy computing.

Two facts decide this:

1. **[ADR 0007](0007-android-first.md) makes the first version Android only.**
2. **The team has not used any of these stacks before.** No Kotlin, no Dart, no React Native. Everything is learned from zero.

That second fact removes the argument that usually settles this question. Nobody ships faster on what they already know, because nobody knows any of it.

## Decision

**Native Android: Kotlin with Jetpack Compose.**

Decided by the IT team, which owns app development for this project.

## Why

**1. One toolchain to learn, not two.**
Android Studio, Kotlin, the Android SDK. A cross-platform framework is that same Android layer *plus* a framework on top — more to learn, not less, for a team starting from zero.

**2. When a beginner hits a bug, the error is the real one.**
This is the strongest reason. In Flutter or React Native, an Android problem arrives translated through the framework, and a beginner cannot tell whether the fault is theirs, the framework's, or the platform's. Native Kotlin gives an Android stack trace and an Android answer on the first search result.

**3. Everything the app needs is first-class on Android.**
QR reading is CameraX with ML Kit. The token goes in EncryptedSharedPreferences over the Android Keystore. Notifications are Firebase Cloud Messaging. All three are the documented default path, not a package wrapping a package.

**4. Learning material.** Android's own tutorials, Google's Compose course, and roughly every Stack Overflow answer ever written about mobile. For a team learning from zero, the size of the haystack matters.

## What we accept

**iOS becomes a second app, not a port.** Every screen written again in Swift. This was raised before the decision and taken anyway.

Three things make it a smaller loss than it looks:

- iOS was already deferred with no date ([ADR 0007](0007-android-first.md)), and for a student project it may never start.
- **The design carries over even though the code does not.** The API contract, the screen flow, the error wording, the settings — all of it is written down and platform-neutral. A second app rebuilds the code, not the thinking.
- If iOS ever does start, a rewrite by a team that has already built the thing once is a very different job from a first build.

**What we do not accept:** letting Android shape the design. Nothing in the API contract, the architecture or the settings may assume Android. Anything named after an Android class in a document, rather than after what it does, is a bug in that document.

## When to revisit

- The team grows and somebody arrives who already knows Flutter or React Native well.
- iOS gains a date **and** a person, before Android work is far along. Once several screens exist, switching costs more than continuing.

Both mean a new ADR. This one stays as the record of what was decided on 2026-08-05 and why.

## Affects

- **P0-03** — ticked by this ADR
- Unblocks **P1-01** (empty app on real Android) and **P1-04** (the one network file)
- **P1-02** and the iOS work behind it — now a rewrite, and that is written down above
- `CLAUDE.md` — the Tech stack section names Kotlin and Compose
- Does **not** decide the cabinet screen. That is **P0-14**
