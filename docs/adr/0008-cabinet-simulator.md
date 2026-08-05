# 0008 — A simulated cabinet, and the line it must not cross

- **Status:** accepted
- **Date:** 2026-08-05
- **Deciders:** IT team

## Context

There is no cabinet to work with. There is a design drawing — 1800 × 1600 × 450 mm, 20 compartments, a touchscreen in the middle — and nothing physical.

Almost everything interesting in this project happens at the cabinet. A door opens. A door shuts. A screen shows a QR. A shipper types a number. Phases 3, 5 and 6 are almost entirely cabinet work, and none of it can be developed against a drawing.

Waiting for hardware means waiting to start.

## Decision

**Build a simulated cabinet in Blender**, modelled from the real design drawing, driven by an event API.

It exists to answer one kind of question: *when the server says "open box 04", does the right thing happen?* Doors open and shut, the screen changes, and the whole sequence can be run over and over in seconds.

The build is one Python script, `scripts/cabinet-sim/build_cabinet.py`, so that it can be read, diffed and re-run. Not a `.blend` file, which is a binary nobody can review.

## The line this must not cross

**A simulator is not evidence.** Rule [A5](../reference/working-rules.md) says so directly:

> *"It works on my machine" is not evidence. Evidence for this project means a real phone, a real network, and the real server.*

A simulated cabinet is the purest possible form of "works on my machine". It cannot show a jammed door, a lock that sticks in the cold, a screen washed out by sunlight, a Wi-Fi dead spot behind a concrete pillar, or a shipper holding a parcel in one hand.

**So the rule is:** a task whose `Verify` names the real cabinet is ticked by the real cabinet. Never by the simulator. No exceptions, and no partial credit.

The simulator is for **building**. The cabinet is for **proving**.

## The danger we are walking into on purpose

A good simulator makes hardware feel less urgent. Work flows, screens run, doors open, and the question *"when do we get a real cabinet?"* quietly stops being asked. Then Phase 8 arrives and every physical assumption is tested at once, under pressure — the same failure that [ADR 0007](0007-android-first.md) accepted for iOS.

Two guards, both written into the board rather than left to memory:

1. **No `Verify` line is ever rewritten to accept the simulator.** If a task says real cabinet, it means the real cabinet. Changing that wording to make a tick possible is the failure mode, and it is banned here so it is not a judgement call later.
2. **`P0-16` tracks getting real hardware**, and it stays open and visible for as long as it takes. The simulator does not close it and cannot close it.

## Why Blender rather than a drawing on screen

The simulator has to answer *"did the right door open?"* — a question about which of twenty doors, in which position, at what moment. That is a spatial question, and it is much easier to see than to read from a log.

It is also the only way to catch a whole class of mistakes early: the box numbering being wrong, the panel covering a door, a door too small for a parcel. Those are cheap to find in a model and expensive to find in steel.

## What it will and will not include

| It has | It does not have |
| --- | --- |
| All 20 compartments, numbered as the drawing numbers them | Real locks, real timing, real friction |
| The control panel where the drawing puts it | A real touchscreen, or real touch |
| Doors that open and shut on command | Any claim about how long a real door takes |
| A screen surface that shows the current state | Network behaviour |
| An event API: `open_door(n)`, `close_door(n)`, `set_screen(state)` | Anything that could be mistaken for proof |

## One thing the drawing gets wrong

The specification says **20 compartments**. The drawing shows doors numbered 01 to 20 but **06 is missing** — the control panel occupies that position. Counting the doors in the drawing gives **19**.

The model is built to the drawing: 19 doors plus a panel. The mismatch is recorded as assumption **A-15** and needs an answer from whoever made the drawing, because it changes how many parcels a cabinet holds.

Finding this before anything was fabricated is exactly the point of building the model early.

## Affects

- `scripts/cabinet-sim/` — new folder, added to the Repository map
- **P0-09** — the test-device task now names the simulator, and still names a real cabinet
- **P0-16** — new task: get a real cabinet to test on
- Assumption **A-15** — 19 doors or 20?
