# Roadmap — master board

The plan for the app side of VGU Smart Locker. One file per phase. Every task is a checkbox with an ID.

## Task format

Every task looks exactly like this. Do not change the shape — humans and agents both read it.

```markdown
- [ ] **P2-03** — Log in as candidate or parent against the real API
      - Owner: _unassigned_ · Needs: P2-02 · Blocks: —
      - Verify: a real candidate account reaches the point list
      - Notes: these accounts come by email from the server
```

| Field | Rule |
| --- | --- |
| `- [ ]` / `- [x]` | Unticked / done. Tick **only** when the Verify line passes |
| **ID** | `P<phase>-<nn>`. Never reused, never renumbered |
| Owner | A name, or `_unassigned_`. A task being worked on has a name |
| Needs | Task IDs that must be ticked first, or `—` |
| Blocks | Task IDs waiting on this one, or `—` |
| Verify | The one check that makes this task done. Not "code written" |
| Notes | Optional. One line |

**Extra state markers** — put at the front of the title:

- `🔴 BLOCKED —` cannot move. Say why in Notes.
- `🟡 DOING —` someone is on it now. Owner must be a name.

Nothing else. No half-ticks.

## How to tick a task

1. Run the Verify check. Actually run it.
2. It passes → change `- [ ]` to `- [x]`.
3. Update the phase file's **Progress** line.
4. Update this file's count in the table below.
5. Commit with the ID: `feat(P2-03): add candidate login`.

All in the same change. A tick with a stale count is a broken board.

## Phases

- [ ] **Phase 0 — Agree** · [phase-0-agree.md](phase-0-agree.md) · `0/9` · **CURRENT**
      Decide the stack, the barcode format, the API contract. No code.
- [ ] **Phase 1 — Skeleton** · [phase-1-skeleton.md](phase-1-skeleton.md) · `0/7`
      Empty app runs on both real phones and reaches the real server.
- [ ] **Phase 2 — Login** · [phase-2-login.md](phase-2-login.md) · `0/8`
      Both account types log in and stay logged in. Errors are plain.
- [ ] **Phase 3 — See the lockers** · [phase-3-see-lockers.md](phase-3-see-lockers.md) · `0/7`
      Points, free counts, nearest point, locker list and size.
- [ ] **Phase 4 — Open the locker** · [phase-4-open-locker.md](phase-4-open-locker.md) · `0/8`
      The core. Open, lock, history, and the safety rules around them.
- [ ] **Phase 5 — Barcode** · [phase-5-barcode.md](phase-5-barcode.md) · `0/7`
      Card scan, library-app code, and the ID + password fallback.
- [ ] **Phase 6 — Modes** · [phase-6-modes.md](phase-6-modes.md) · `0/5`
      Normal mode and exam season. The app follows, never decides.
- [ ] **Phase 7 — Handoff** · [phase-7-handoff.md](phase-7-handoff.md) · `0/6`
      Parcel drop and collect. Marked future. Do last.
- [ ] **Phase 8 — Ship** · [phase-8-ship.md](phase-8-ship.md) · `0/9`
      Device tests, bug fixing, release build, rollback page.

**Total: 0 / 66.**

Tick a phase box only when every task inside it is ticked **and** its Exit check passes.

## Current phase

**Phase 0 — Agree.** Nothing here needs a tech stack, so nothing here is wasted.

Do every Phase 0 task before any Phase 1 task. Two items cost the most if they change late:

1. **The API contract.** Change it after the screens exist → rebuild the screens.
2. **The barcode format.** Build against a guess → the app fails on the first real card.

## Order of work

Phases 1 → 6 run in order. Each leans on the one before.

Phase 7 is marked *future* in the team requirements. Start it only when Phases 1–6 are ticked and the Server team has the order API ready.

Phase 8 runs at the end, but its device testing starts early — test on a real Android and a real iPhone at the end of **every** phase.

## Working rules

1. One owner per task. No task is `🟡 DOING` with `_unassigned_`.
2. A task bigger than one day is two tasks. Split it, give the new one the next free number.
3. Branch name = task ID: `P2-03-candidate-login`.
4. New task discovered mid-phase → add it to the phase file with the next free number, raise the total here, in the same change.
5. Bugs are not roadmap tasks. They go to [../reference/bug-log.md](../reference/bug-log.md) as `BUG-nnn`.
