# Phase 2 — Register

**Goal:** a receiver registers with a phone number, gets a token, and stays logged in.

**Progress: 0 / 7.**

The receiver must be registered and logged in before a parcel is ever dropped. Everything later depends on this.

## Tasks

- [ ] **P2-01** — Screen: type a phone number, ask for a one-time code
      - Owner: _unassigned_ · Needs: P0-04, P1-05 · Blocks: P2-02
      - Verify: a real phone number is entered on the real Android phone and the server log shows the request

- [ ] **P2-02** — The one-time code arrives on the phone
      - Owner: _unassigned_ · Needs: P2-01 · Blocks: P2-03
      - Verify: a real code arrives on a real phone within the agreed time

- [ ] **P2-03** — Send the code back and get a token
      - Owner: _unassigned_ · Needs: P2-02 · Blocks: P2-04, P2-06
      - Verify: a real phone number and its real code reach the empty parcel list screen

- [ ] **P2-04** — Store the token in the phone's secure store
      - Owner: _unassigned_ · Needs: P2-03 · Blocks: P2-05, P5-02
      - Verify: the token is not in plain text anywhere on the device, and never appears in a log line
      - Notes: not a plain file, not app settings. The platform secure store

- [ ] **P2-05** — Stay logged in after the app is closed and reopened
      - Owner: _unassigned_ · Needs: P2-04 · Blocks: P2-07, P4-02, P4-04
      - Verify: close the app fully, reopen it, and it does not ask to register again

- [ ] **P2-06** — Plain messages for a wrong code, an expired code, and too many tries
      - Owner: _unassigned_ · Needs: P2-03 · Blocks: —
      - Verify: each of the three cases shows its own plain sentence on a real phone. No code shown raw

- [ ] **P2-07** — Log out, and handle an expired token
      - Owner: _unassigned_ · Needs: P2-05 · Blocks: —
      - Verify: log out clears the token; an expired token sends the user back to register once, with no loop

## Exit check

- [ ] All seven tasks ticked
- [ ] A real phone number registers on a real Android **and** a real iPhone
- [ ] The token is in the secure store and in no log line
- [ ] Every error case shows a plain sentence
- [ ] Counts updated in [README.md](README.md)
