# Phase 0 — Agree

**Goal:** settle everything that costs the most to change late. No code in this phase.

**Progress: 5 / 15.**

Phase 0 is the cheapest phase and it prevents the most rework. Do all of it before Phase 1.

## Start here

Only **two** tasks can start right now. Everything else is waiting on one of them.

| Start today | Why it is free to start |
| --- | --- |
| **P0-01** — does the cabinet have its own network? | Nothing blocks it, and it blocks four other tasks |
| **P0-09** — list the test devices | Nothing blocks it. The Mac and Apple account question takes the longest to fix, so ask today |

**And immediately after P0-09, without waiting for anyone:** **P0-03**, the phone app stack. Those three are the whole IT-team-only path into Phase 1.

Everything else unlocks in this order:

```
   P0-01 ──┬──> P0-02 ──> P0-14 ──> (cabinet screen, Phase 1)
           │
           └──> P0-04 ──┬──> P0-05, P0-07, P0-08, P0-10
                        │
                        └──> P0-06 ──> P0-11 ──> P0-12 ──> P0-13

   P0-09 ──────> P0-03 ──────────────> (phone app, Phase 1)
```

The bottom line needs nobody outside this team. It is the only way into Phase 1 that we control.

**The longest chain is six deep: P0-01 → P0-04 → P0-06 → P0-11 → P0-12 → P0-13.** That chain is the length of Phase 0. Shortening it means answering P0-01 and P0-04 fast, not working harder later.

## Who actually answers each one

Most of this phase is not work we do. It is answers we need from other people. Grouped that way, it is three conversations, not fourteen tasks.

| Who | Tasks | What to do |
| --- | --- | --- |
| **Hardware team** | P0-01, P0-02, and the sensor half of P0-10 | One message. Four questions. Send it today |
| **Server team** | P0-04, and P0-05 · P0-06 · P0-07 · P0-08 · P0-10 · P0-11 which all hang off it | One meeting. P0-04 is the meeting; the other six are its agenda |
| **Us, the IT team** | P0-03, P0-09, P0-12, P0-13 | Ours to decide. **P0-09 and P0-03 need nobody** — do them now. P0-12 and P0-13 need the other columns first |
| **Hardware, then us** | P0-14 | The cabinet stack, once P0-02 says what the screen runs |

The Google Meet the team asked about covers the Server-team column in one sitting.

## Tasks

- [x] **P0-01** — Ask the hardware team: does the cabinet have its own network connection?
      - Owner: Team · Needs: — · Blocks: P0-02, P0-04, P0-12, P1-03
      - Verify: a written answer from the hardware owner naming SIM or Wi-Fi, in `architecture.md`
      - Done: 2026-08-05 — The team confirmed the cabinet connects to Wi-Fi. That is now written into `architecture.md` as a fact rather than a guess, and the diagram says so. Everything in the plan needed this answer, so it unblocks most of the phase. Worth knowing the answer came from our own team rather than the hardware owner directly — if that turns out to be wrong, this is the first thing to re-check.

- [ ] **P0-02** — Write down what the cabinet screen is: size, touch, what it runs
      - Owner: _unassigned_ · Needs: P0-01 · Blocks: P0-14, P1-03
      - Verify: the screen size, whether it takes touch, and its operating system are written in `architecture.md`
      - Notes: a phone-sized screen and a tablet-sized screen need different layouts

- [ ] **P0-03** — Pick the tech stack for the **phone app**
      - Owner: _unassigned_ · Needs: P0-09 · Blocks: P0-15, P1-01
      - Verify: [ADR 0001](../adr/0001-tech-stack.md) is `accepted` for the phone app, and `CLAUDE.md` names the real stack
      - Notes: nobody outside this team decides this. It needs P0-09 only because no Mac changes the answer. Split from the cabinet choice (P0-14) so the phone app is not held up by hardware questions

- [ ] **P0-04** — Write the whole API contract as our proposal, and send it
      - Owner: _unassigned_ · Needs: P0-01 · Blocks: P0-05, P0-08, P2-01, P3-01
      - Verify: `api-contract.md` has zero `TO AGREE` markers **and** it has been sent to the Server team with a date recorded here
      - Notes: changed from *agree with them* to *propose to them* — see [ADR 0005](../adr/0005-we-propose-they-object.md). They have no API design yet, so a blank form would never come back. The document is written; **it still has to be sent**

- [ ] **P0-05** — Agree how the cabinet proves it is the cabinet
      - Owner: _unassigned_ · Needs: P0-04 · Blocks: P1-06, P3-01
      - Verify: the key type, how it is placed on the cabinet, and how it is replaced are written in `api-contract.md`
      - Notes: get this wrong and a stolen cabinet opens every box. The key never enters this repo

- [x] **P0-06** — Decide how the receiver is told a parcel arrived
      - Owner: Team · Needs: — · Blocks: P0-11, P4-01
      - Verify: one route is chosen and written down, with its cost per message, and it is adjustable without a new release
      - Done: 2026-08-05 — Chose push through the app. It costs nothing per message, and the app is already needed to collect a parcel, so nobody has to install anything extra. SMS would cost money for every single delivery. The choice sits in the settings list, so if push turns out to be unreliable on campus we can switch to Zalo or SMS by editing one line, not by rebuilding the app.

- [x] **P0-07** — Decide the QR session code: what it holds, how long it lives
      - Owner: Team · Needs: — · Blocks: P5-01
      - Verify: the lifetime in seconds and the refresh rate are written down, and both are adjustable without a new release
      - Done: 2026-08-05 — Set the code on the cabinet screen to last 60 seconds, with the screen drawing a new one every 30 seconds. Both numbers are guesses, so both went into the settings list where anyone can change them. If people find it expires while they are still walking up, the number goes up; nothing else has to change. The code itself only says which cabinet and when, so a longer life is not dangerous the way the typed code would be.

- [ ] **P0-08** — Agree what the shipper sees when he looks up a receiver
      - Owner: _unassigned_ · Needs: P0-04 · Blocks: P3-03
      - Verify: the exact masked format is written in `api-contract.md` with a worked example, **and the Server team has confirmed they can produce it**
      - Notes: never a full name, never a phone number. The cabinet screen is a public terminal. The Verify used to stop at "written down", which our own proposal already satisfied — a task called *Agree* needs the other side to agree

- [ ] **P0-09** — List the test devices: two phones and one cabinet
      - Owner: _unassigned_ · Needs: — · Blocks: P0-03, P1-01
      - Verify: a table in this file names the Android model and its OS version, and where the test cabinet is
      - Notes: Android only for now — [ADR 0007](../adr/0007-android-first.md). The Mac and Apple developer account question moves to the rest of the team and no longer blocks us

- [x] **P0-10** — Decide what records a delivery, now that there is no sensor
      - Owner: Team · Needs: — · Blocks: P3-05, P5-04
      - Verify: [ADR 0006](../adr/0006-no-sensor.md) is `accepted`, and `api-contract.md` endpoint 12 names the event rather than what causes it
      - Done: 2026-08-05 — The team confirmed there is no sensor in the boxes, which reverses an earlier plan. So the door closing is what records a delivery. That means a driver could open a door, walk off still holding the parcel, and the system would think it was delivered. We looked at three ways to stop that — a confirm button, a camera, a weight check — and all three were either the same trust problem or a sensor by another name. We accept the risk: it leaves a trail naming the driver, the box and the time, so it is traceable afterwards even though it is not preventable beforehand. The call is named "a door closed" so that if a sensor is ever fitted, it fires the same call and nothing else changes.

- [x] **P0-11** — Decide the typed pickup code: length, lifetime, and lockout
      - Owner: Team · Needs: P0-06 · Blocks: P0-12, P5-08
      - Verify: the length, the lifetime, and the number of wrong tries before a box locks are all written in `api-contract.md`, and all three are adjustable
      - Done: 2026-08-05 — Settled on a 6-digit code that lasts 48 hours, with the box locking for 15 minutes after 5 wrong tries. 48 hours because a parcel usually sits a day or two and nobody should be punished for being busy. The lockout matters more than it looks: 6 digits is one chance in a million per try, which is only safe while somebody cannot sit there guessing all night. All four numbers are in the settings list. What is **not** adjustable is that the code works once and never repeats — that is a design decision, not a number.

- [ ] **P0-12** — Decide how a pickup works when the cabinet loses the network
      - Owner: _unassigned_ · Needs: P0-01, P0-11 · Blocks: P0-13, P6-06, P6-07
      - Verify: [ADR 0004](../adr/0004-offline-pickup.md) is `accepted`, and the chosen way is written in `api-contract.md`
      - Notes: the team has no answer for this yet. Two options are written up in the ADR, which recommends the challenge-and-response one. Both a drop and a pickup can work offline — an earlier draft of the ADR said otherwise and was wrong

- [ ] **P0-13** — Settle whether rule C4 gains an offline exception
      - Owner: _unassigned_ · Needs: P0-12 · Blocks: —
      - Verify: rule **C4** in `working-rules.md` either carries a written exception, or ADR 0004 is marked rejected. One or the other, decided by the team, not by whoever writes the code
      - Notes: C4 says the front-end never decides. ADR 0004 has the cabinet opening a door with no server involved. Both cannot be true. **Nothing offline gets built until this is settled**

- [ ] **P0-14** — Pick the tech stack for the **cabinet screen**
      - Owner: _unassigned_ · Needs: P0-02 · Blocks: P1-03
      - Verify: [ADR 0001](../adr/0001-tech-stack.md) names the cabinet stack, and says whether it is the same as the phone app or different
      - Notes: cannot be decided before P0-02 says what the screen is and what it runs. A different stack from the phone app is allowed — say so if it is

- [ ] **P0-15** — Build the settings file, before any code is written
      - Owner: _unassigned_ · Needs: P0-03 · Blocks: —
      - Verify: every number in the settings table in `architecture.md` is in one file, a person can edit it without touching code, and changing one takes effect without a new release
      - Notes: this is what makes guessing safe — see [ADR 0005](../adr/0005-we-propose-they-object.md). It exists **before** Phase 1 so that no guessed number ever gets typed into a source file. An admin web page can come later; a plain config file is enough to start

## Test devices

Fill this in during P0-09.

| Device | Model | OS version | Who holds it |
| --- | --- | --- | --- |
| Android phone | | | |
| iPhone | *(later — [ADR 0007](../adr/0007-android-first.md))* | | |
| Test cabinet | | | |

## Exit check

- [ ] All fifteen tasks ticked
- [x] `api-contract.md` has zero `TO AGREE` markers — done 2026-08-05
- [ ] [ADR 0001](../adr/0001-tech-stack.md) is `accepted`
- [x] The cabinet is confirmed to have a network connection — Wi-Fi, done 2026-08-05
- [ ] Counts updated in [README.md](README.md)
