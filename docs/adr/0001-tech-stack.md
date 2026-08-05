# 0001 — App tech stack

- **Status:** proposed — **not decided**
- **Date:** —
- **Deciders:** IT team

## Context

The app must run on Android **and** iOS. The team requirements say so directly. The choice made here blocks every app task in the repo. See task P0-01.

The app is not heavy. It shows lists, takes taps, scans a barcode, and calls an API. It has no 3D, no video, no heavy computing. The camera is the only demanding part, and every option handles it.

## Options

| Option | Good | Bad |
| --- | --- | --- |
| **A. One code base** (React Native or Flutter) | One team, one code, both phones. About half the work. Fastest to a demo | One extra layer between you and the camera. Some device bugs need platform-specific fixes |
| **B. Two native apps** (Kotlin for Android, Swift for iOS) | Best camera and hardware control. No extra layer. Every phone feature available at once | Two code bases, two builds, two sets of bugs. Needs people for both. Roughly double the work |
| **C. One code base now, native later if needed** | Start fast, keep the door open | Two rewrites if you get it wrong. Rarely worth planning for |

## Decision

**Not decided.** Fill this in during Phase 0, then set the status to `accepted`.

## What to check before deciding

1. **What does the team already know?** A team that knows JavaScript ships faster on React Native than on anything else, whatever the comparison chart says.
2. **Do you have a Mac and an Apple developer account?** An iOS build needs both, whichever option you pick. Missing either one blocks task P1-03. Check this first — it takes the longest to fix.
3. **Does the locker hardware need Bluetooth or NFC from the phone?** If the phone talks to the locker directly, that pushes toward native. If the **server** commands the locker and the phone only calls the API, that pushes toward one code base. Confirm with the Server team.
4. **How many people are on the app side?** Fewer than three people makes two native apps hard to sustain.

## Why

Fill in after deciding. Two or three sentences.

## What we accept

Fill in after deciding. The known downside you take on purpose.

## Affects

Blocks: P1-01, P1-02, P1-03, P0-09, and through them every later phase.
