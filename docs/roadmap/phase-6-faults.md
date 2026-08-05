# Phase 6 — When it goes wrong

**Goal:** the awkward cases have an answer, and none of them loses a parcel.

**Progress: 0 / 5.**

Every task here needs Phase 5 working first. A fault is a fault in a flow that already runs.

## Tasks

- [ ] **P6-01** — A box will not open, or will not close
      - Owner: _unassigned_ · Needs: P5-04 · Blocks: —
      - Verify: with a box jammed on purpose, both front-ends say so plainly, and the server marks that box out of use

- [ ] **P6-02** — One person has two parcels at one cabinet
      - Owner: _unassigned_ · Needs: P5-04 · Blocks: —
      - Verify: after two real drops for one person, one scan opens one box, and the app says a second parcel is still waiting

- [ ] **P6-03** — Nobody collects a parcel
      - Owner: _unassigned_ · Needs: P5-04 · Blocks: —
      - Verify: after the agreed number of days, the receiver is reminded and staff can see the parcel is stuck
      - Notes: agree the number of days in P0-04. A full cabinet blocks every later delivery

- [ ] **P6-04** — The shipper walks away with the door open
      - Owner: _unassigned_ · Needs: P3-05 · Blocks: —
      - Verify: leave a real door open past the agreed time. The cabinet warns, and the server does not record a parcel that is not there

- [ ] **P6-05** — The receiver scans at the wrong cabinet
      - Owner: _unassigned_ · Needs: P5-03 · Blocks: —
      - Verify: scanning a cabinet that holds nothing for you says which cabinet does hold your parcel. No door opens

## Exit check

- [ ] All five tasks ticked
- [ ] No case above lost a parcel or opened a wrong door
- [ ] Every new message was read by someone outside the team and understood
- [ ] Counts updated in [README.md](README.md)
