# Phase 4 — Tell the receiver

**Goal:** the receiver learns a parcel arrived, without opening the app to check.

**Progress: 0 / 5.**

A parcel nobody knows about is a parcel nobody collects.

## Tasks

- [ ] **P4-01** — The server sends the notice when the door closes
      - Owner: _unassigned_ · Needs: P3-05, P0-06 · Blocks: P4-02, P4-04
      - Verify: closing a real door sends one notice, and the server log shows it going out

- [ ] **P4-02** — The notice arrives while the app is open
      - Owner: _unassigned_ · Needs: P4-01, P2-05 · Blocks: P4-03
      - Verify: with the app open on a real phone, the notice appears within the agreed time

- [ ] **P4-03** — The notice arrives while the app is closed
      - Owner: _unassigned_ · Needs: P4-02 · Blocks: P4-05
      - Verify: with the app fully closed on a real Android, the notice still arrives
      - Notes: this is where the two platforms differ most, so it is also where the deferred iOS work will bite hardest — [ADR 0007](../adr/0007-android-first.md)

- [ ] **P4-04** — "My parcels" screen lists what is waiting
      - Owner: _unassigned_ · Needs: P2-05, P4-01 · Blocks: P4-05, P5-02
      - Verify: a real waiting parcel shows the cabinet name, the box number, and when it arrived

- [ ] **P4-05** — Tapping the notice opens the parcel screen
      - Owner: _unassigned_ · Needs: P4-03, P4-04 · Blocks: —
      - Verify: tapping a real notice on a real phone lands on the parcel, not on the home screen

## Exit check

- [ ] All five tasks ticked
- [ ] A notice arrived on a real Android with the app closed *(iPhone deferred — [ADR 0007](../adr/0007-android-first.md))*
- [ ] The parcel list matches what the server holds
- [ ] Counts updated in [README.md](README.md)
