# Roadmap — master board

The plan for the app side of VGU Smart Locker. One file per phase. Every task is a checkbox with an ID.

Product: a **parcel drop-off locker**, with two front-ends — the phone app and the cabinet screen. See [ADR 0003](../adr/0003-parcel-locker-product.md).

> ### ⚠️ Phase 1.5 — four of ten closed, and three of the rest need a phone
>
> **Ryan put the app design first, as a "Phase 1.5", ahead of Phase 2.** It has a file, ten task IDs and a `Verify` line each, and the totals count it. This board no longer points at Phase 2.
>
> **The tester build is current.** Release `6i35gjjd975go`, version 0.10.2, 2026-08-13 — sign-in can be typed into, and the crash that closed the app on the way to Home is fixed (BUG-006, BUG-007). The old warning here said App Distribution was still serving the Phase 1 skeleton and that a tester was not looking at the current design — that stopped being true at 0.8.0 and this note was left standing. Six design builds have gone out since.
>
> **The loop works. The proving has not been done.** P1.5-01 is closed: one command draws five of the app's components to PNG on this machine, and one draws the same five from the mock-up's own stylesheets in Chrome, with no phone involved. An earlier version of this note said the JVM half had never rendered a component — that was read off a stale line in the phase file rather than checked, and it was wrong.
>
> **The comparison has started, and it is finding things.** P1.5-02, P1.5-03 and P1.5-06 are closed. Checking all 46 colour tokens against the designer's own file by number rather than by eye found two typed in wrong — a dark-mode outline drawn teal instead of pale blue on every card and box in the app, and a washed-out light-mode card shadow. Both fixed, logged as BUG-005. That is a fault six builds of eye-checking never caught, found in one pass on this machine.
>
> **Three blocks cannot be closed here at all, and now that is proved rather than assumed** — P1.5-04, P1.5-05 and P1.5-07. All three are glass, and glass reads by its backdrop blur, which needs a GPU that a JVM render does not have. `build/parity/beads.png` shows the beads as dark blobs, which is exactly what P1.5-07's own `Verify` forbids. Their numbers are all right — a bead is 30dp with a 14dp icon, matching the design exactly — so what is left is a look on Ryan's phone, and nothing else.
>
> **Two blocks are `🟡 DOING` and could still close here** — P1.5-09, the cabinet camera, and P1.5-10, the map card. Both are motion and framing rather than a still component, so neither fits the five-component test as it stands.
>
> One task is `🔴 BLOCKED` — P1.5-08, the theme switch, on BUG-003.
>
> ⚠️ **This phase and the design language now pull against each other, and Ryan has not been asked yet.** Matching the mock-up and sitting on one grid cannot both be satisfied: the mock-up's card radius is 18 and its padding 14, neither of which the new four-radius set or the 4dp grid names. Four values differ, all four moved on purpose, all four are listed in the phase file. It is resolved there in favour of the scales, on the grounds that the mock-up was eyeballed — **that call needs Ryan's word, and reversing it is a small change.**
>
> Server work has run in parallel — endpoints 19, 20 and 21 are built and tested — because it blocks nothing on the app side and needs nothing from a phone.

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
- `⏸️ LATER —` we chose not to do it yet. Say which ADR decided that.

Nothing else. No half-ticks.

**`BLOCKED` and `LATER` are not the same.** Blocked means something is in the way. Later means we decided to wait, on purpose, and can start whenever we choose. A `LATER` task still counts in the total — hiding deferred work makes progress look better than it is.

## How to tick a task

1. Run the Verify check. Actually run it.
2. It passes → change `- [ ]` to `- [x]`.
3. **Add the `Done:` line.** The date, then what you did and what you saw, in plain words.
4. Update the phase file's **Progress** line.
5. Update this file's count in the table below, and the **Total** line.
6. Run `python scripts/checkboard.py`. It must print `board is consistent`.
7. Commit with the ID: `feat(P2-03): send the one-time code`.

All in the same change. A tick with a stale count is a broken board. A tick with no `Done` line is worse — nobody can tell what was proved.

**The board is never left stale.** Whoever does the work updates the board in the same change as the work. Not at the end of the day. Not at the end of the week.

## Phases

- [ ] **Phase 0 — Agree** · [phase-0-agree.md](phase-0-agree.md) · `12/17`
      Settle the stack, the cabinet hardware, the sensor, the codes and the API contract. No code.
      **5 deferred `⏸️ LATER`** — [ADR 0009](../adr/0009-start-phase-1-early.md). **P0-04 is the critical path for the entire board** — send the contract, get an address.
- [ ] **Phase 1 — Skeleton** · [phase-1-skeleton.md](phase-1-skeleton.md) · `5/9`
      Empty app on the Android phone, empty cabinet screen in a browser, both reach the server.
- [ ] **Phase 1.5 — Design parity** · [phase-1.5-design-parity.md](phase-1.5-design-parity.md) · `4/10` · **CURRENT**
      Make the app look like the mock-up, and build the loop that can prove it does. Ryan put this ahead of Phase 2. Ten blocks, in order, each done only when its two pictures match.
- [ ] **Phase 2 — Register** · [phase-2-register.md](phase-2-register.md) · `1/9`
      A receiver registers with a phone number and stays logged in.
- [ ] **Phase 3 — Shipper drop** · [phase-3-shipper-drop.md](phase-3-shipper-drop.md) · `0/7`
      The cabinet screen. A shipper finds the receiver and a box opens.
- [ ] **Phase 4 — Tell the receiver** · [phase-4-notify.md](phase-4-notify.md) · `0/5`
      The notification arrives and the parcel shows in the app.
- [ ] **Phase 5 — Pick up** · [phase-5-pickup.md](phase-5-pickup.md) · `0/10`
      Scan the cabinet QR — or type the code — and the right box opens. The core feature.
- [ ] **Phase 6 — When it goes wrong** · [phase-6-faults.md](phase-6-faults.md) · `0/8`
      Faulty box, two parcels, nobody collects, shipper walks away, network drops.
- [ ] **Phase 7 — History** · [phase-7-history.md](phase-7-history.md) · `0/3`
      What happened, for the receiver and for staff.
- [ ] **Phase 8 — Ship** · [phase-8-ship.md](phase-8-ship.md) · `0/13`
      Device tests, bug fixing, release build, rollback page, and the Play listing.

**Total: 22 / 91.**  ·  5 marked `⏸️ LATER` — 2 for iOS ([ADR 0007](../adr/0007-android-first.md)), 3 deferred from Phase 0 ([ADR 0009](../adr/0009-start-phase-1-early.md))

Tick a phase box only when every task inside it is ticked **and** its Exit check passes.

## Current phase

**Phase 1 — Skeleton.** The first code.

> ### The whole board is waiting on two people, not on code
>
> Checked on 2026-08-07 by walking every open task's dependencies to the root. **Every one of them ends at P1-05 or P1-06, and both need a server address.** Nothing else is in the way.
>
> | # | What | Who | Unblocks |
> | --- | --- | --- | --- |
> | 1 | **Send `api-contract.md` to the Server team** — P0-04. It is finished: 17 endpoints, every value chosen, no blanks. It needs sending, and an address back | Somebody with their contact | ~60 tasks. Phases 2 to 8 |
> | 2 | **Install the app on the Vivo and look at it** — P1-01. The APK is built and waiting | Somebody holding that phone | Closes Phase 1's last human step |
>
> Both front-ends already have everything that does not need a server: one network door each, the token store, the settings file, and seven screens.

**Phase 0 no longer gates Phase 1** — [ADR 0009](../adr/0009-start-phase-1-early.md) reversed that on 2026-08-06. Every open Phase 0 task waited on somebody outside this team, none of them moved, and the app they exist to serve had not been started. Six are now `⏸️ LATER`. Phase 0 stays open and its deferred tasks still count against the total.

**One thing still gates the work: the shape of the API.** Which calls exist, what each is for, and which caller may make it. Agree that with the Server team before Phase 2. Everything else in the contract — paths, field names, numbers, error codes — changes one file, because every call goes through one network file (P1-04 ✅) and every guessed number lives in one settings file (P0-15 ✅).

**Phase 0 is ours again, because the server is.** On 2026-08-15 the Server team handed over what they had — a FastAPI app whose schema could not say who a parcel belonged to, and no authentication anywhere — and the split moved to where the skill is: [ADR 0019](../adr/0019-we-own-the-server.md). P0-04 was rewritten in the same change, because *send the contract and wait* could never have closed. P0-08 closed, because the masked-name format was only ever waiting on a team that was not going to answer. What is still open in Phase 0 waits on the hardware team or the group chat.

> ⚠️ **P0-13 was decided under delegation, not at a team sitting.** The board asked for a team decision; the CSE lead made the call on 2026-08-07 so the work could move. The team still needs telling, and if they disagree, reversing it is one new ADR and no code — ADR 0004's Option A is a day's work.

## Order of work

Phases 1 → 5 run in order. Each leans on the one before. Phase 5 is the product — everything before it exists to make Phase 5 possible.

Phase 6 needs Phase 5 working, because a fault is a fault in a flow that already runs.

Phase 8 runs at the end, but its device testing starts early — test on a real Android at the end of every phase, and on the real cabinet as soon as there is one.

**There is no cabinet, and we are not waiting for one** — [ADR 0009](../adr/0009-start-phase-1-early.md). The cabinet screen is a web page built in a browser. That unblocks writing the code, not ticking the box: every Verify line that names a door opening still names a door opening, and stays unticked until P0-16 delivers real metal. The simulator can drive those tasks; it cannot close them — [ADR 0008](../adr/0008-cabinet-simulator.md).

**Android first.** iOS is deferred, not cancelled — see [ADR 0007](../adr/0007-android-first.md). Every iOS task keeps its ID and is marked `⏸️ LATER`. Rule D3 still says test on both platforms; we are knowingly deferring half of it, and ADR 0007 records what that will cost when iOS starts.

## Two front-ends, one team

Phases 1, 3, 5 and 8 touch **both** the phone app and the cabinet screen. When a task names one of them, it means only that one.

The cabinet screen is a smaller job than the app, but it is a real second front-end. It is the reason this board is 74 tasks and not 45.

## Working rules

1. One owner per task. No task is `🟡 DOING` with `_unassigned_`.
2. A task bigger than one day is two tasks. Split it, give the new one the next free number.
3. Branch name = task ID: `P2-03-one-time-code`.
4. New task found mid-phase → add it to the phase file with the next free number, raise the total here, in the same change.
5. Bugs are not roadmap tasks. They go to [../reference/bug-log.md](../reference/bug-log.md) as `BUG-nnn`.
