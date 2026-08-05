# Phase 7 — Parcel handoff

**Goal:** a parcel is dropped in a locker, and the receiver gets a code to open it.

**Progress: 0 / 6.**

**Marked *future* in the team requirements. Do it last.** Start only when Phases 1–6 are ticked and the Server team has the order API ready.

## Tasks

- [ ] **P7-01** — Agree the order data with the Server team: order code, locker, user, send time
      - Owner: _unassigned_ · Needs: P0-05 · Blocks: P7-02
      - Verify: the order fields are in [api-contract.md](../reference/api-contract.md) and both teams agreed
      - Notes: extend the contract **before** any code, exactly as in Phase 0

- [ ] **P7-02** — Read the scanned locker or order data
      - Owner: _unassigned_ · Needs: P7-01, P5-05 · Blocks: P7-03
      - Verify: scanned order data appears correctly in the app

- [ ] **P7-03** — Ask the API for a suitable free locker, by parcel size
      - Owner: _unassigned_ · Needs: P7-02, P3-06 · Blocks: P7-04
      - Verify: a large parcel never gets a small locker
      - Notes: the **server** picks the locker. The app sends the size and asks

- [ ] **P7-04** — Confirm the drop and record the order
      - Owner: _unassigned_ · Needs: P7-03 · Blocks: P7-05
      - Verify: the order row exists on the server with the right locker and time

- [ ] **P7-05** — Trigger the open code being sent to the receiver
      - Owner: _unassigned_ · Needs: P7-04 · Blocks: P7-06
      - Verify: the receiver actually receives the code
      - Notes: the server sends it. **Open question** — email, SMS, or in-app? Settle in P7-01

- [ ] **P7-06** — The receiver opens the locker with that code
      - Owner: _unassigned_ · Needs: P7-05, P4-02 · Blocks: —
      - Verify: the code opens the real locker **once**, and fails cleanly the second time

## Safety rules for this phase

This phase gives one person a code to open a locker holding another person's parcel.

- [ ] Every Phase 4 safety rule still holds
- [ ] Confirmed: a parcel code works exactly once
- [ ] Confirmed: a parcel code expires
- [ ] Confirmed: an expired or used code shows a plain message, and opens nothing

## Exit check

- [ ] All six tasks ticked
- [ ] All four safety rules ticked
- [ ] One parcel goes end to end: dropped by a sender, collected by a **different** person with a code
- [ ] Counts updated in [README.md](README.md)
