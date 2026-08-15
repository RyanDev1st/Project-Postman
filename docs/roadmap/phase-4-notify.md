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
      - Verify: with the app fully closed on a real Android, the notice arrives — **and it still arrives after the phone has sat untouched overnight**, with battery saving left at the factory setting
      - Notes: the overnight half is the real test. Vivo, Oppo, Xiaomi and Huawei stop background apps hard to save battery, and our main test phone is a Vivo (A-16). Closing the app and sending at once will pass on any phone and prove nothing. Do not fix a failure by telling users to change a battery setting — most never will

- [ ] **P4-04** — "My parcels" screen lists what is waiting
      - Owner: _unassigned_ · Needs: P2-05, P4-01 · Blocks: P4-05, P5-02
      - Verify: a real waiting parcel shows the cabinet name, the box number, and when it arrived
      - Notes: **Built and checked by machine; not ticked.** Home reads endpoints 5 and 7 through `rememberHome`, and the sample parcel it used to draw — Back Gate, box 04 — is gone. The lists are required parameters now, so no screen can quietly fall back to an invented parcel again.
        The countdown is worked out from `arrived_at` plus `pickup_code_hours`, because endpoint 5 sends no deadline. `WaitingTest` holds the arithmetic to the design's own two samples, and `python scripts/checkserver.py` proves the server sends a cabinet name on every history line.
        What is missing is the Verify itself: **no phone has ever seen it.** `server_base_url` is blank, so this has never drawn a parcel that came down a wire

- [ ] **P4-05** — Tapping the notice opens the parcel screen
      - Owner: _unassigned_ · Needs: P4-03, P4-04 · Blocks: —
      - Verify: tapping a real notice on a real phone lands on the parcel, not on the home screen

## Exit check

- [ ] All five tasks ticked
- [ ] A notice arrived on a real Android with the app closed *(iPhone deferred — [ADR 0007](../adr/0007-android-first.md))*
- [ ] The parcel list matches what the server holds
- [ ] Counts updated in [README.md](README.md)
