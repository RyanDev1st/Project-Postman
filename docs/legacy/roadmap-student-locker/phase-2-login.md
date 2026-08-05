# Phase 2 — Login

**Goal:** both account types log in, stay logged in, and get a clear message when something fails.

**Progress: 0 / 8.**

## Tasks

- [ ] **P2-01** — Build the login screen UI
      - Owner: _unassigned_ · Needs: P1-07 · Blocks: P2-02
      - Verify: the screen matches the agreed design on both phones

- [ ] **P2-02** — Log in as student or lecturer against the real API
      - Owner: _unassigned_ · Needs: P2-01, P1-04, P0-06 · Blocks: P2-03, P2-04, P2-07, P2-08, P3-01, P4-07
      - Verify: a real student account reaches the point list

- [ ] **P2-03** — Log in as candidate or parent against the real API
      - Owner: _unassigned_ · Needs: P2-02, P0-07 · Blocks: —
      - Verify: a real candidate account reaches the point list
      - Notes: these accounts arrive by email from the server. The app's first screen must match the email wording

- [ ] **P2-04** — Store the pass token safely on the phone
      - Owner: _unassigned_ · Needs: P2-02 · Blocks: P2-05, P2-06
      - Verify: the token is not found in app logs or in plain app storage
      - Notes: use the phone's secure store. Never plain text, never a log line

- [ ] **P2-05** — Stay logged in after the app closes and reopens
      - Owner: _unassigned_ · Needs: P2-04 · Blocks: —
      - Verify: force-close, reopen → still logged in, no password asked

- [ ] **P2-06** — Send the user back to login when the token expires
      - Owner: _unassigned_ · Needs: P2-04 · Blocks: —
      - Verify: an expired token sends the user to login **once**, with a message
      - Notes: the server answers "not allowed". Do not loop, do not retry

- [ ] **P2-07** — Clear message on a wrong password, in plain words
      - Owner: _unassigned_ · Needs: P2-02 · Blocks: —
      - Verify: a wrong password shows a plain message and the app stays usable
      - Notes: never say which part was wrong

- [ ] **P2-08** — Clear message when there is no network, on every screen
      - Owner: _unassigned_ · Needs: P2-02 · Blocks: P8-04
      - Verify: airplane mode on every screen shows a message — no crash, no blank screen

## Exit check

- [ ] All eight tasks ticked
- [ ] One real account of **each** type logs in on **both** phones
- [ ] Airplane mode is handled on every screen that exists so far
- [ ] Counts updated in [README.md](README.md)

## Note

The two account types see different things. Confirm the exact difference in [api-contract.md](../reference/api-contract.md) under **Account types** before starting P2-03.
