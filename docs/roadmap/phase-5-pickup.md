# Phase 5 — Pick up

**Goal:** the receiver walks to the cabinet, scans the screen with the app, and the right door opens.

**Progress: 0 / 7.**

**This is the product.** Everything in Phases 0 to 4 exists to make this phase possible.

## Tasks

- [ ] **P5-01** — The cabinet screen shows the QR session code
      - Owner: _unassigned_ · Needs: P0-07, P1-06 · Blocks: P5-02
      - Verify: the real cabinet shows a QR, and it changes on the agreed timer
      - Notes: the QR says *which cabinet, at what moment*. It is not a key. See `architecture.md` section 6

- [ ] **P5-02** — The app scans the QR on the cabinet screen
      - Owner: _unassigned_ · Needs: P5-01, P2-04, P4-04 · Blocks: P5-03
      - Verify: a real phone reads the real cabinet screen in daylight **and** in a dark corridor

- [ ] **P5-03** — The server checks the user, then opens their box
      - Owner: _unassigned_ · Needs: P5-02 · Blocks: P5-04, P5-05, P5-06, P5-07, P6-05, P7-03
      - Verify: the correct door physically opens, and the server log shows which user and which box

- [ ] **P5-04** — The receiver closes the door and the parcel is marked collected
      - Owner: _unassigned_ · Needs: P5-03 · Blocks: P6-01, P6-02, P6-03, P7-01, P7-02
      - Verify: closing the real door removes the parcel from "My parcels" and writes one collected row in the server log

- [ ] **P5-05** — Plain messages for "code expired" and "no parcel for you here"
      - Owner: _unassigned_ · Needs: P5-03 · Blocks: —
      - Verify: both cases show their own plain sentence on a real phone
      - Notes: an expired code is normal, not a failure. The message says "scan again", with no alarm

- [ ] **P5-06** — An unclear result never shows success
      - Owner: _unassigned_ · Needs: P5-03 · Blocks: —
      - Verify: cut the network mid-request on a real phone. The app says it could not confirm and tells the user to check the box. It never shows "opened"

- [ ] **P5-07** — One scan opens at most one door
      - Owner: _unassigned_ · Needs: P5-03 · Blocks: —
      - Verify: scan once on a real phone and count the open requests in the server log. There is exactly one
      - Notes: proven from the log, not from the screen

## Safety rules for this phase

Tick these with the phase. A door opening is a physical act.

- [ ] A door opens only from a scan the user made. Never from a retry, a timer, or a screen refresh
- [ ] One scan = at most one open request. Proven from the server log
- [ ] An unclear result is said plainly. The app never shows a false success
- [ ] A photograph of the cabinet screen opens nothing without the receiver's logged-in account

## Exit check

- [ ] All seven tasks ticked
- [ ] All four safety rules ticked
- [ ] Someone who is not on this team collected a real parcel, with no help
- [ ] It worked in daylight and in a dark corridor
- [ ] Counts updated in [README.md](README.md)
