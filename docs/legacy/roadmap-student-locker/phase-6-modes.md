# Phase 6 — Modes

**Goal:** the app follows the school's two operating modes without a reinstall.

**Progress: 0 / 5.**

Two modes exist: **normal** and **exam season**. In exam season some lockers are reserved for candidates. The admin switches the mode on the server. The app only follows.

## Tasks

- [ ] **P6-01** — Read the current mode of each point from the API
      - Owner: _unassigned_ · Needs: P3-01 · Blocks: P6-02, P6-03, P6-04
      - Verify: the mode shown in the app matches the server for every point
      - Notes: the mode is **per point**, not one global mode. Confirm in P0-05

- [ ] **P6-02** — Show the point's mode on screen in plain words
      - Owner: _unassigned_ · Needs: P6-01 · Blocks: —
      - Verify: a user reads the screen and knows which mode the point is in
      - Notes: "Exam season — some lockers are reserved"

- [ ] **P6-03** — Show reserved lockers correctly in exam mode, and say who they are for
      - Owner: _unassigned_ · Needs: P6-01, P3-05 · Blocks: P6-05
      - Verify: in exam mode, reserved lockers look different from shared lockers
      - Notes: the server decides what is reserved. The app never guesses

- [ ] **P6-04** — Follow a mode change made by the admin, with no app update
      - Owner: _unassigned_ · Needs: P6-01, P3-07 · Blocks: —
      - Verify: the admin flips the mode; the app shows the new mode after one refresh, no reinstall

- [ ] **P6-05** — Refuse an open on a locker the current mode does not allow, with a plain reason
      - Owner: _unassigned_ · Needs: P6-03, P4-08 · Blocks: —
      - Verify: tapping open on a not-allowed locker gives a plain reason, not an error code
      - Notes: the server refuses. The app explains

## Exit check

- [ ] All five tasks ticked
- [ ] The admin switches a point from normal to exam season, and the app follows on **both** phones with no reinstall
- [ ] Counts updated in [README.md](README.md)
