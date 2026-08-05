# Phase 5 — Barcode

**Goal:** the user opens a locker by scanning their card, and still gets in when the scan fails.

**Progress: 0 / 7.**

## Tasks

- [ ] **P5-01** — Build the camera scan screen
      - Owner: _unassigned_ · Needs: P4-02 · Blocks: P5-02, P5-03, P5-04
      - Verify: the camera view opens on both phones

- [ ] **P5-02** — Ask for camera permission, and explain why before asking
      - Owner: _unassigned_ · Needs: P5-01 · Blocks: P5-06
      - Verify: denying the camera still leads to a working fallback
      - Notes: a denial must never end at a dead screen

- [ ] **P5-03** — Read the student and lecturer card barcode
      - Owner: _unassigned_ · Needs: P5-01, P0-02 · Blocks: P5-05, P5-07
      - Verify: a real student card scans on the first or second try
      - Notes: use the real format recorded in P0-02. Never build against a guess

- [ ] **P5-04** — Read the code from the VGU Library app
      - Owner: _unassigned_ · Needs: P5-01, P0-03 · Blocks: —
      - Verify: a real library-app code scans
      - Notes: same screen as P5-03 if the format allows it

- [ ] **P5-05** — Send the scanned code to the API and open the matching locker
      - Owner: _unassigned_ · Needs: P5-03, P4-02 · Blocks: P5-06, P7-02
      - Verify: scanning a real card opens the real locker
      - Notes: the **server** matches code to user. The app only forwards the code

- [ ] **P5-06** — Fallback: open with ID and password when the scan fails
      - Owner: _unassigned_ · Needs: P5-05, P5-02 · Blocks: —
      - Verify: with the camera covered, a user still opens their locker
      - Notes: always reachable from the scan screen, not buried in a menu

- [ ] **P5-07** — Handle a bad scan: dirty card, dark room, unknown code
      - Owner: _unassigned_ · Needs: P5-03 · Blocks: —
      - Verify: a random unknown barcode shows a plain message and no crash
      - Notes: offer the fallback in the same message

## Exit check

- [ ] All seven tasks ticked
- [ ] A real card opens a real locker on **both** phones
- [ ] The fallback works with the camera physically blocked
- [ ] Counts updated in [README.md](README.md)

## Warning

Do not start P5-03 before P0-02 is ticked. Building against a guessed barcode format means building it twice.
