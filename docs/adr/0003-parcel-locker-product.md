# 0003 — Parcel drop-off product, and the phone scans the cabinet

- **Status:** accepted
- **Date:** 2026-08-05
- **Deciders:** IT team

## Context

The first plan built a **student storage locker**: a student logs in, picks a locker point, and opens a locker with a card barcode. Two account types. An exam-season mode. Many locker points.

A team member then described what the group actually wants, in Vietnamese:

> Register with a phone number. When the shipper arrives, he types the phone number on the screen, picks the name, puts the item in the locker. A notification goes to the phone. Send a QR code so the receiver only has to show the QR, and the locker opens.

That is a **parcel drop-off locker**. It is a different product. Three things change at the root:

1. **There are two front-ends, not one.** The shipper never installs an app. He uses a screen on the cabinet. The IT team builds a phone app *and* a cabinet screen.
2. **Nobody logs in with a student card.** Registration is a phone number. No card barcode. No exam mode. No student or candidate account types.
3. **A QR code opens the door**, not a locker list.

## The question this ADR settles

The Vietnamese said *"đưa QR"* — *show the QR* — which reads as: the phone displays a QR and the cabinet camera reads it. Call that **A**.

The other direction is: the cabinet screen displays a QR and the phone camera reads it. Call that **B**.

| | Who displays | Who scans | Cabinet needs a camera |
| --- | --- | --- | --- |
| **A** | Phone | Cabinet | Yes |
| **B** | Cabinet screen | Phone | No |

The cabinet screen is confirmed. The cabinet camera is not.

## Decision

**B. The cabinet screen shows the QR. The phone scans it.**

The product is a parcel drop-off locker with two front-ends. The old student-locker board moves to `docs/legacy/`.

## Why

**1. It removes a hardware unknown.** Every phone has a camera. The cabinet camera was never confirmed. B does not need one, so the design stops waiting for an answer.

**2. Cheap cameras fail in a way that is worse than no camera.** A camera that works in daylight and fails at night fails *sometimes*. Intermittent failure is the most expensive kind to support — it cannot be reproduced, and users stop trusting the whole system. A camera good enough for mixed lighting costs real money.

**3. The QR stops being the key.** This is the reason that matters most.

In **A**, the QR on the phone *is* the key. A screenshot forwarded to somebody else opens the box. Expiry and one-time use reduce that, but a forwarded screenshot inside the valid window still works.

In **B**, the cabinet's QR only says *"you are standing at this cabinet, right now."* The identity comes from the app login. Anyone can photograph the cabinet screen. Without the receiver's logged-in account, it opens nothing.

```
   A: QR = the key            B: QR = the place
      screenshot = entry         screenshot = useless
```

## What we accept

**The receiver must have the app installed and be logged in before pickup.** In A, a receiver could open a box straight from an SMS with no app. We lose that.

The cost is nothing new. Registration and the notification already live in the app, so the receiver has it before a parcel is ever dropped.

## The board restarts

Rule: *never renumber an ID, never reuse one*. That rule exists so a task ID in a branch name or a commit subject always means one thing.

We reuse the IDs anyway, once, on purpose:

- No task on the old board was ticked. The board stood at `0 / 66`.
- No commit, branch, or line of code refers to any old ID.
- So no history is made ambiguous by the reuse.

The old board moves to `docs/legacy/roadmap-student-locker/` and stays readable. The new board starts at `P0-01`.

**This is a one-time allowance for a product change before any work started.** It is not permission to renumber a live board. From the first ticked task onward, the rule holds with no exception.

## When to revisit

Reverse this with a new ADR if the hardware team confirms a camera good enough for mixed outdoor lighting **and** a receiver without the app has to be supported. Both conditions, not one.

## Affects

- `docs/reference/architecture.md` — rewritten: two front-ends, no card scanner
- `docs/reference/api-contract.md` — rewritten for the parcel flow
- `docs/roadmap/` — new board, 9 phases, 60 tasks
- `docs/legacy/roadmap-student-locker/` — the old board
- `CLAUDE.md` — Mission and Feature list rewritten
- Assumption log — A-06 to A-09 added
