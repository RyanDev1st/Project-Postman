# Phase 4 — Tell the receiver

**Goal:** the receiver learns a parcel arrived, without opening the app to check.

**Progress: 0 / 6.** Nothing here is built except the device-token endpoint, and nothing calls it. P4-04's screen is the exception - it is built and drawing real parcels. See P4-01 for what the four missing parts are and which Google credential unblocks them.

A parcel nobody knows about is a parcel nobody collects.

## Tasks

- [ ] **🔴 BLOCKED — P4-01** — The server sends the notice when the door closes
      - Owner: _unassigned_ · Needs: P3-05, P0-06 · Blocks: P4-02, P4-04
      - Verify: closing a real door sends one notice, and the server log shows it going out
      - Notes: **surveyed 2026-08-22. One of four parts exists.** The chain is: the app gets a device token from Firebase → the app sends it to us → the server calls Google when a drop's door closes → Google wakes the phone. Part two is built on both sides - endpoint 8 `POST /devices` stores it in the `devices` table, and `Api.registerDevice` is written. **Nothing calls `registerDevice`**, the same dead wiring P1-08 had, so no token is ever collected and the table stays empty. Parts one, three and four do not exist
      - Notes: 🔴 **blocked on a credential only Ryan can make: a Firebase service-account key** for the FCM HTTP v1 API. `google-services.json` is already in the repo tree (git-ignored) and the Firebase project exists, but it carries **App Distribution only** - sending test builds to testers - not Messaging. FCM itself is free and needs no card, which is why it was chosen over SMS in [ADR 0006](../adr/0006-*.md). Not to be confused with the OAuth **client id** P2-08 needs; two different Google credentials, both Ryan's to create

- [ ] **P4-02** — The notice arrives while the app is open
      - Owner: _unassigned_ · Needs: P4-01, P2-05 · Blocks: P4-03
      - Verify: with the app open on a real phone, the notice appears within the agreed time
      - Notes: needs the `FirebaseMessagingService` subclass, which does two jobs and neither exists yet: it receives the device token when Firebase issues **or rotates** it - so the token cannot be collected once and forgotten - and it receives the message. Also needs the messaging dependency, the manifest entry, and `registerDevice` actually being called after sign-in

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

- [ ] **P4-06** — Tell the receiver their number did not match
      - Owner: _unassigned_ · Needs: P3-09, P3-10 · Blocks: —
      - Verify: a drop resolved by a near miss or by the name sends a notice **naming both numbers** - “the label said …382, you booked …328” - to the receiver and nobody else, by push and by email. A parcel that went to ABO sends its own notice, with the same two numbers
      - Notes: [ADR 0026](../adr/0026-the-booking-makes-the-number-true.md). **The notice names the digits**, because a person cannot correct a number they are only told is wrong, and somebody who mistyped theirs has no other way to find out. Never SMS - there is no SMS in this system any more. P2-17 is the tap that fixes it
## Exit check

- [ ] All five tasks ticked
- [ ] A notice arrived on a real Android with the app closed *(iPhone deferred — [ADR 0007](../adr/0007-android-first.md))*
- [ ] The parcel list matches what the server holds
- [ ] Counts updated in [README.md](README.md)
