# Roadmap — master board

The plan for the app side of VGU Smart Locker. One file per phase. Every task has an ID. Use the ID in branch names, commits, and chat.

## How to use this board

**Task ID:** `P<phase>-<number>` — for example `P2-03`.

**Status:** exactly one of:

| Status | Meaning |
| --- | --- |
| `todo` | Not started |
| `doing` | Someone is on it right now. Put their name in **Owner** |
| `blocked` | Cannot move. Say what blocks it in **Notes** |
| `done` | Finished **and** verified. See the phase's Exit check |

**Rules:**
1. One owner per task. No task sits in `doing` with no owner.
2. A task is `done` only when its **Verify** line passes. Not when the code is written.
3. Branch name: `P2-03-login-screen`. Commit subject: `feat(P2-03): add login screen`.
4. Change a status → change it in the phase file, then update the count below.
5. A task that grows past one day of work is two tasks. Split it.

## Phases

| Phase | Goal | File | Done / Total |
| --- | --- | --- | --- |
| **0** | Agree. Decide the stack, the barcode, the contract | [phase-0-agree.md](phase-0-agree.md) | 0 / 9 |
| 1 | Skeleton. Empty app runs on both phones, reaches server | [phase-1-skeleton.md](phase-1-skeleton.md) | 0 / 7 |
| 2 | Login. Both account types, plus error states | [phase-2-login.md](phase-2-login.md) | 0 / 8 |
| 3 | See the lockers. Points, free counts, nearest, list | [phase-3-see-lockers.md](phase-3-see-lockers.md) | 0 / 7 |
| 4 | Open the locker. The core feature | [phase-4-open-locker.md](phase-4-open-locker.md) | 0 / 8 |
| 5 | Barcode. Card scan plus fallback | [phase-5-barcode.md](phase-5-barcode.md) | 0 / 7 |
| 6 | Modes. Normal and exam season | [phase-6-modes.md](phase-6-modes.md) | 0 / 5 |
| 7 | Handoff. Marked future. Do last | [phase-7-handoff.md](phase-7-handoff.md) | 0 / 6 |
| 8 | Ship. Device tests, bug fixing, release | [phase-8-ship.md](phase-8-ship.md) | 0 / 9 |

**Total: 0 / 66.**

## Current phase

**Phase 0 — Agree.**

Do every Phase 0 task before any Phase 1 task. Phase 0 costs the least and prevents the most rework. The two items that cost the most if they change late:

1. **The API contract.** Change it after the screens exist → rebuild the screens.
2. **The barcode format.** Design around a guess → the app fails on the first real card.

## Order of work

Phases 1 → 6 run in order. Each one leans on the one before it.

Phase 7 is marked *future* in the team requirements. Start it only when Phases 1–6 are `done` and the Server team has the order API ready.

Phase 8 runs at the end, but its device testing starts early: test on a real Android and a real iPhone at the end of **every** phase, not only at the end.
