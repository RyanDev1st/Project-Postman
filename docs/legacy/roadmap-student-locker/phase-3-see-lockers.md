# Phase 3 — See the lockers

**Goal:** the user finds a locker point with free space, near them, and sees what is inside it.

**Progress: 0 / 7.**

## Tasks

- [ ] **P3-01** — Show the list of locker points from the API
      - Owner: _unassigned_ · Needs: P2-02 · Blocks: P3-02, P3-03, P3-05, P6-01
      - Verify: every point on the server appears in the app

- [ ] **P3-02** — Show the free-slot count for each point
      - Owner: _unassigned_ · Needs: P3-01 · Blocks: P3-07
      - Verify: the count in the app matches the server after a refresh
      - Notes: the count comes from the server. The app never works it out itself

- [ ] **P3-03** — Ask for location permission, and work fine if the user says no
      - Owner: _unassigned_ · Needs: P3-01 · Blocks: P3-04
      - Verify: denying location leaves the app fully usable
      - Notes: "no" must not block the app. Show the plain list instead

- [ ] **P3-04** — Sort or mark the nearest locker point
      - Owner: _unassigned_ · Needs: P3-03 · Blocks: —
      - Verify: standing at one point, that point is first in the list or clearly marked
      - Notes: **open question** — does the API send each point's location? Settle in P0-05

- [ ] **P3-05** — Show the locker list inside one point, with each locker's state
      - Owner: _unassigned_ · Needs: P3-01 · Blocks: P3-06, P4-01, P6-03
      - Verify: locker states in the app match the server

- [ ] **P3-06** — Show each locker's size
      - Owner: _unassigned_ · Needs: P3-05 · Blocks: P7-03
      - Verify: each locker shows its size
      - Notes: sizes come from the server table

- [ ] **P3-07** — Refresh the lists — free counts go stale fast
      - Owner: _unassigned_ · Needs: P3-02 · Blocks: P6-04
      - Verify: a change made on the server appears in the app after one refresh
      - Notes: pull to refresh, plus a refresh when the screen opens

## Exit check

- [ ] All seven tasks ticked
- [ ] A user with no training opens the app, finds the nearest point with free space, and sees the lockers in it
- [ ] Counts updated in [README.md](README.md)
