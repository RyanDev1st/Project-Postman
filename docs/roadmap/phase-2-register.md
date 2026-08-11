# Phase 2 — Register

**Goal:** a receiver registers with a phone number, gets a token, and stays logged in.

**Progress: 0 / 8.**

The receiver must be registered and logged in before a parcel is ever dropped. Everything later depends on this.

## Tasks

- [ ] **P2-01** — Screen: type a phone number, ask for a one-time code
      - Owner: _unassigned_ · Needs: P1-05 · Blocks: P2-02
      - Verify: a real phone number is entered on the real Android phone and the server log shows the request
      - Notes: P0-04 was dropped from the Needs by [ADR 0009](../adr/0009-start-phase-1-early.md) — we build against our own proposed contract, per [ADR 0005](../adr/0005-we-propose-they-object.md). **The shape of endpoints 1 and 2 must be agreed with the Server team before this is built**, because a shape change here rebuilds the screen. The field names and the path can change cheaply; the call existing at all cannot

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

- [ ] **P2-08** — Sign in with Google, on top of the phone number
      - Owner: _unassigned_ · Needs: P2-05 · Blocks: —
      - Verify: on a real Android phone, tapping "Continue with Google" on an account that has already registered by phone reaches the parcel list without a code being typed; and a Google account that has never registered is asked for a phone number first
      - Notes: decided in [ADR 0011](../adr/0011-google-sign-in-no-passwords.md). Endpoint 19 in [api-contract.md](../reference/api-contract.md); the reference server already answers it and its checks are tested. **Blocked on an OAuth client id, which only Ryan can create** — see the ADR. No passwords are added, on purpose

## Exit check

- [ ] All eight tasks ticked
- [ ] A real phone number registers on a real Android *(iPhone deferred — [ADR 0007](../adr/0007-android-first.md))*
- [ ] The token is in the secure store and in no log line
- [ ] Every error case shows a plain sentence
- [ ] Counts updated in [README.md](README.md)
