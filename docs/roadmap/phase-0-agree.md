# Phase 0 — Agree

**Goal:** settle everything that costs the most to change late. No code in this phase.

**Progress: 0 / 12.**

Phase 0 is the cheapest phase and it prevents the most rework. Do all of it before Phase 1.

## Tasks

- [ ] **P0-01** — Ask the hardware team: does the cabinet have its own network connection?
      - Owner: _unassigned_ · Needs: — · Blocks: P0-02, P0-04, P0-12, P1-03
      - Verify: a written answer from the hardware owner naming SIM or Wi-Fi, in `architecture.md`
      - Notes: **ask this first.** We are already building as if the answer is yes (assumption A-09). A chat message from a teammate is not the hardware owner. Every arrow in the chart dies without this

- [ ] **P0-02** — Write down what the cabinet screen is: size, touch, what it runs
      - Owner: _unassigned_ · Needs: P0-01 · Blocks: P0-03, P1-03
      - Verify: the screen size, whether it takes touch, and its operating system are written in `architecture.md`
      - Notes: a phone-sized screen and a tablet-sized screen need different layouts

- [ ] **P0-03** — Pick the tech stack for both front-ends
      - Owner: _unassigned_ · Needs: P0-02 · Blocks: P1-01, P1-02, P1-03
      - Verify: [ADR 0001](../adr/0001-tech-stack.md) is `accepted`, and `CLAUDE.md` names the real stack
      - Notes: the cabinet screen may use a different stack from the phone app. Say so if it does

- [ ] **P0-04** — Agree the API contract with the Server team
      - Owner: _unassigned_ · Needs: P0-01 · Blocks: P0-05, P0-06, P0-07, P0-08, P0-10, P0-11, P2-01, P3-01
      - Verify: `api-contract.md` has zero `TO AGREE` markers and both teams have signed off in writing
      - Notes: the highest-cost task in the project. Do not start screens before it passes

- [ ] **P0-05** — Agree how the cabinet proves it is the cabinet
      - Owner: _unassigned_ · Needs: P0-04 · Blocks: P1-06, P3-01
      - Verify: the key type, how it is placed on the cabinet, and how it is replaced are written in `api-contract.md`
      - Notes: get this wrong and a stolen cabinet opens every box. The key never enters this repo

- [ ] **P0-06** — Agree how the receiver is told a parcel arrived
      - Owner: _unassigned_ · Needs: P0-04 · Blocks: P0-11, P4-01
      - Verify: one route is chosen — push, Zalo or SMS — and written in `api-contract.md` with its cost per message
      - Notes: push through the app is free. SMS is not

- [ ] **P0-07** — Agree the QR session code: what it holds, how long it lives
      - Owner: _unassigned_ · Needs: P0-04 · Blocks: P5-01
      - Verify: the lifetime in seconds and the refresh rate are written in `api-contract.md`
      - Notes: too short and a slow user fails; too long and a photograph stays useful

- [ ] **P0-08** — Agree what the shipper sees when he looks up a receiver
      - Owner: _unassigned_ · Needs: P0-04 · Blocks: P3-03
      - Verify: the exact masked format is written in `api-contract.md`, with a worked example
      - Notes: never a full name, never a phone number. The cabinet screen is a public terminal

- [ ] **P0-09** — List the test devices: two phones and one cabinet
      - Owner: _unassigned_ · Needs: — · Blocks: P1-01, P1-02
      - Verify: a table in this file names the Android model, the iPhone model, their OS versions, and where the test cabinet is
      - Notes: check today whether a Mac and an Apple developer account exist. It blocks P1-02 and takes longest to fix

- [ ] **P0-10** — Agree what the box sensor actually reports
      - Owner: _unassigned_ · Needs: P0-04 · Blocks: P3-05, P5-04
      - Verify: what the sensor sends, and what counts as "a parcel is in there", are written in `api-contract.md` endpoint 12
      - Notes: a weight, a broken beam, or a plain full/empty are three different answers. It decides what "delivered" means, so it cannot be guessed

- [ ] **P0-11** — Agree the typed pickup code: length, lifetime, and lockout
      - Owner: _unassigned_ · Needs: P0-04, P0-06 · Blocks: P0-12, P5-08
      - Verify: the length, the lifetime in minutes, and the number of wrong tries before a box locks are all written in `api-contract.md`
      - Notes: this code **is** the key — unlike the QR. Too long a life and a forwarded message still opens the box. Not the same question as P0-07

- [ ] **P0-12** — Decide how a pickup works when the cabinet loses the network
      - Owner: _unassigned_ · Needs: P0-01, P0-11 · Blocks: P6-06
      - Verify: [ADR 0004](../adr/0004-offline-pickup.md) is `accepted`, and the chosen way is written in `api-contract.md`
      - Notes: the team has no answer for this yet. Two options are written up in the ADR. A drop can never work offline — only a pickup — and that makes the problem much smaller than it looks

## Test devices

Fill this in during P0-09.

| Device | Model | OS version | Who holds it |
| --- | --- | --- | --- |
| Android phone | | | |
| iPhone | | | |
| Test cabinet | | | |

## Exit check

- [ ] All twelve tasks ticked
- [ ] `api-contract.md` has zero `TO AGREE` markers
- [ ] [ADR 0001](../adr/0001-tech-stack.md) is `accepted`
- [ ] The cabinet is confirmed to have a network connection
- [ ] Counts updated in [README.md](README.md)
