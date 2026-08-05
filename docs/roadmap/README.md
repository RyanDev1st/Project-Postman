# Roadmap — master board

The plan for the app side of VGU Smart Locker. One file per phase. Every task is a checkbox with an ID.

Product: a **parcel drop-off locker**, with two front-ends — the phone app and the cabinet screen. See [ADR 0003](../adr/0003-parcel-locker-product.md).

## Task format

Every task looks exactly like this. Do not change the shape — people and agents both read it.

**While it is open:**

```markdown
- [ ] **P2-03** — Send the one-time code back and get a token
      - Owner: _unassigned_ · Needs: P2-02 · Blocks: P2-04
      - Verify: a real phone number gets a real code and reaches the parcel list
      - Notes: optional, one line
```

**After it is ticked** — one line is added, and nothing is removed:

```markdown
- [x] **P2-03** — Send the one-time code back and get a token
      - Owner: Minh · Needs: P2-02 · Blocks: P2-04
      - Verify: a real phone number gets a real code and reaches the parcel list
      - Done: 2026-08-12 — Typed a real number into the app. The code came by SMS in
        about 4 seconds. Typed it in and the parcel list opened. Watched it happen on
        Minh's own phone, not a simulator.
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
| **Done** | **Added when you tick.** The date, then what you did and what you saw, in plain words |

### How to write the Done line

It is written for a reader with no technical background. A team member, a lecturer, or you in three months.

- Say **what you did** and **what you saw**. Not what you built.
- Name the real thing: which phone, which cabinet, which person.
- Two or three sentences. No jargon. No tool names unless they matter.
- If something was odd but you ticked anyway, say that too.

| Good | Bad |
| --- | --- |
| "Dropped a real parcel at the cabinet by the library. Box 4 opened, I put a book in, closed it. The phone buzzed 2 seconds later." | "Implemented drop flow, endpoint 11 wired, tests green." |
| "Scanned the cabinet screen in a dark corridor. It read on the second try, so we made the screen brighter." | "QR scanning works." |

A `Done` line that a non-technical reader cannot follow is not finished. Rewrite it.

**Extra state markers** — put at the front of the title:

- `🔴 BLOCKED —` cannot move. Say why in Notes.
- `🟡 DOING —` someone is on it now. Owner must be a name.

Nothing else. No half-ticks.

## How to tick a task

1. Run the Verify check. Actually run it.
2. It passes → change `- [ ]` to `- [x]`.
3. **Add the `Done:` line.** The date, then what you did and what you saw, in plain words.
4. Update the phase file's **Progress** line.
5. Update this file's count in the table below.
6. Commit with the ID: `feat(P2-03): send the one-time code`.

All in the same change. A tick with a stale count is a broken board. A tick with no `Done` line is worse — nobody can tell what was proved.

**The board is never left stale.** Whoever does the work updates the board in the same change as the work. Not at the end of the day. Not at the end of the week.

## Phases

- [ ] **Phase 0 — Agree** · [phase-0-agree.md](phase-0-agree.md) · `0/14` · **CURRENT**
      Settle the stack, the cabinet hardware, the sensor, the codes and the API contract. No code.
- [ ] **Phase 1 — Skeleton** · [phase-1-skeleton.md](phase-1-skeleton.md) · `0/7`
      Empty app on both phones, empty screen on the cabinet, both reach the server.
- [ ] **Phase 2 — Register** · [phase-2-register.md](phase-2-register.md) · `0/7`
      A receiver registers with a phone number and stays logged in.
- [ ] **Phase 3 — Shipper drop** · [phase-3-shipper-drop.md](phase-3-shipper-drop.md) · `0/7`
      The cabinet screen. A shipper finds the receiver and a box opens.
- [ ] **Phase 4 — Tell the receiver** · [phase-4-notify.md](phase-4-notify.md) · `0/5`
      The notification arrives and the parcel shows in the app.
- [ ] **Phase 5 — Pick up** · [phase-5-pickup.md](phase-5-pickup.md) · `0/9`
      Scan the cabinet QR — or type the code — and the right box opens. The core feature.
- [ ] **Phase 6 — When it goes wrong** · [phase-6-faults.md](phase-6-faults.md) · `0/8`
      Faulty box, two parcels, nobody collects, shipper walks away, network drops.
- [ ] **Phase 7 — History** · [phase-7-history.md](phase-7-history.md) · `0/3`
      What happened, for the receiver and for staff.
- [ ] **Phase 8 — Ship** · [phase-8-ship.md](phase-8-ship.md) · `0/10`
      Device tests, bug fixing, release build, rollback page.

**Total: 0 / 70.**

Tick a phase box only when every task inside it is ticked **and** its Exit check passes.

## Current phase

**Phase 0 — Agree.** Nothing here needs a tech stack, so nothing here is wasted.

Do every Phase 0 task before any Phase 1 task. Four items cost the most if they change late:

1. **Does the cabinet have its own network?** We are building as if it does, but nobody from the hardware team has said so in writing. Ask today.
2. **The API contract.** Change it after the screens exist → rebuild the screens.
3. **The cabinet key.** How the cabinet proves it is the cabinet. Get it wrong and anyone can open every box.
4. **What the sensor reports.** It decides what "delivered" means, so it cannot be guessed.

## Order of work

Phases 1 → 5 run in order. Each leans on the one before. Phase 5 is the product — everything before it exists to make Phase 5 possible.

Phase 6 needs Phase 5 working, because a fault is a fault in a flow that already runs.

Phase 8 runs at the end, but its device testing starts early — test on a real Android, a real iPhone, **and** the real cabinet at the end of every phase.

## Two front-ends, one team

Phases 1, 3, 5 and 8 touch **both** the phone app and the cabinet screen. When a task names one of them, it means only that one.

The cabinet screen is a smaller job than the app, but it is a real second front-end. It is the reason this board is 70 tasks and not 45.

## Working rules

1. One owner per task. No task is `🟡 DOING` with `_unassigned_`.
2. A task bigger than one day is two tasks. Split it, give the new one the next free number.
3. Branch name = task ID: `P2-03-one-time-code`.
4. New task found mid-phase → add it to the phase file with the next free number, raise the total here, in the same change.
5. Bugs are not roadmap tasks. They go to [../reference/bug-log.md](../reference/bug-log.md) as `BUG-nnn`.
