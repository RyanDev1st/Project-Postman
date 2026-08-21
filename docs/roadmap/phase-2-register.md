# Phase 2 — Register

**Goal:** a receiver registers with a phone number, gets a token, and stays logged in.

**Progress: 0 / 9.** Ticked, that is - not 0 done: P2-01, P2-03, P2-04 and P2-05 all work and were driven end-to-end on an emulator on 2026-08-21. Every `Verify` in this phase says *a real Android phone*, and an emulator is not one, so none of them are ticked. The gap is one session with Ryan's phone, not more building. P2-02 is the real blocker and it is an account setting at SpeedSMS. See `docs/findings/2026-08-21-otp-channel-choice.md`.

The receiver must be registered and logged in before a parcel is ever dropped. Everything later depends on this.

## Tasks

- [ ] **P2-01** — Screen: type a phone number, ask for a one-time code
      - Owner: _unassigned_ · Needs: P1-05 · Blocks: P2-02
      - Verify: a real phone number is entered on the real Android phone and the server log shows the request
      - Notes: P0-04 was dropped from the Needs by [ADR 0009](../adr/0009-start-phase-1-early.md) — we build against our own proposed contract, per [ADR 0005](../adr/0005-we-propose-they-object.md). **The shape of endpoints 1 and 2 must be agreed with the Server team before this is built**, because a shape change here rebuilds the screen. The field names and the path can change cheaply; the call existing at all cannot
      - Status 2026-08-21: **built and working, not ticked.** `Verify` says a real Android phone; this was the emulator `parity` (Android 16) against the real server over TLS, driven through the real screens. Typed `+84 912 340 001`, the server logged the request and generated a code. What is left is one run on Ryan's phone, not code.

- [ ] **P2-02** — The one-time code arrives on the phone
      - Owner: _unassigned_ · Needs: P2-01 · Blocks: P2-03
      - Verify: a real code arrives on a real phone within the agreed time
      - Status 2026-08-21: **blocked, and not on us.** The code reaches a terminal, not a phone. The SpeedSMS request was wrong in three ways and all three are fixed (BUG-016), but the account has no registered sender, so the provider refuses every send. That is a job in SpeedSMS's dashboard. Free alternative if money is the objection: `sms_type` 5, their Android gateway app, which sends from Ryan's own SIM.

- [ ] **P2-03** — Send the code back and get a token
      - Owner: _unassigned_ · Needs: P2-02 · Blocks: P2-04, P2-06
      - Verify: a real phone number and its real code reach the empty parcel list screen
      - Status 2026-08-21: **built and working, not ticked.** Emulator `parity` (Android 16), real server over TLS, real screens: typed the code, reached Home, and the server issued a token. Same as P2-01 - it needs a real phone to close, not work.

- [ ] **P2-04** — Store the token in the phone's secure store
      - Owner: _unassigned_ · Needs: P2-03 · Blocks: P2-05, P5-02
      - Verify: the token is not in plain text anywhere on the device, and never appears in a log line
      - Notes: not a plain file, not app settings. The platform secure store
      - Status 2026-08-21: **verified, and this one was tested properly.** Emulator `parity` (Android 16), real server over TLS, real screens. The stored value was read off the device as root: `shared_prefs/...session.xml` holds a 71-byte blob (12-byte IV, 43-byte ciphertext, 16-byte GCM tag), and hashing that string does not match the server's token hash - so the file does not contain the token. The key is in AndroidKeyStore, so the file alone is useless. Nothing token-shaped in logcat. Still wants one run on a real phone to tick.

- [ ] **P2-05** — Stay logged in after the app is closed and reopened
      - Owner: _unassigned_ · Needs: P2-04 · Blocks: P2-07, P4-02, P4-04
      - Verify: close the app fully, reopen it, and it does not ask to register again
      - Status 2026-08-21: **built and working, not ticked.** Emulator `parity` (Android 16), real server over TLS, real screens: `am force-stop`, relaunch, and the app opened straight to Home without asking to register. Needs a real phone to close.

- [ ] **P2-06** — Plain messages for a wrong code, an expired code, and too many tries
      - Owner: _unassigned_ · Needs: P2-03 · Blocks: —
      - Verify: each of the three cases shows its own plain sentence on a real phone. No code shown raw
      - Status 2026-08-21: **half done.** Two of the sentences were missing entirely - a cooldown and a failed send both showed "Something went wrong. Try again.", which invites the one action that cannot work (BUG-018, fixed, both languages). The three code cases in this task's `Verify` - wrong, expired, too many tries - have still not been walked on a screen.

- [ ] **P2-07** — Log out, and handle an expired token
      - Owner: _unassigned_ · Needs: P2-05 · Blocks: —
      - Verify: log out clears the token; an expired token sends the user back to register once, with no loop
      - Status 2026-08-21: **not started, and it is the one real gap left in this phase.** Nothing logs out, and nothing has been walked with a dead token. The server side exists - it answers `401` on an expired token - but what the app does next has never been seen. The risk this task exists to remove is a loop: the app is sent back to register, registers, gets the same dead token, and goes round again.

- [ ] **P2-08** — Sign in with Google, on top of the phone number
      - Owner: _unassigned_ · Needs: P2-05 · Blocks: —
      - Verify: on a real Android phone, tapping "Continue with Google" on an account that has already registered by phone reaches the parcel list without a code being typed; and a Google account that has never registered is asked for a phone number first
      - Notes: decided in [ADR 0011](../adr/0011-google-sign-in-no-passwords.md). Endpoint 19 in [api-contract.md](../reference/api-contract.md); the reference server already answers it and its checks are tested. **Blocked on an OAuth client id, which only Ryan can create** — see the ADR. No passwords are added, on purpose

- [ ] **P2-09** — Set a password, and sign in with one
      - Owner: _unassigned_ · Needs: P2-05 · Blocks: —
      - Verify: on a real Android phone, set a password after registering by code, close the app fully, sign in with the phone number and that password, and reach the parcel list. Then type it wrong five times and confirm the right password stops working until fifteen minutes have passed
      - Notes: decided in [ADR 0012](../adr/0012-passwords-on-a-phone-account.md), which reverses the no-passwords half of [ADR 0011](../adr/0011-google-sign-in-no-passwords.md). Endpoints 20 and 21. The server side is written and tested; **no email anywhere, and no reset screen** — forgetting a password is the one-time code, then set a new one

## Exit check

- [ ] All nine tasks ticked
- [ ] A real phone number registers on a real Android *(iPhone deferred — [ADR 0007](../adr/0007-android-first.md))*
- [ ] The token is in the secure store and in no log line
- [ ] Every error case shows a plain sentence
- [ ] Counts updated in [README.md](README.md)
